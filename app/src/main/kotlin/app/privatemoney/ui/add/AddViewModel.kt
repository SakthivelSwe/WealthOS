package app.privatemoney.ui.add

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.privatemoney.data.AccountRepository
import app.privatemoney.data.AppSettings
import app.privatemoney.data.CategoryRepository
import app.privatemoney.data.LedgerRepository
import app.privatemoney.data.TransactionDetails
import app.privatemoney.data.local.CategoryEntity
import app.privatemoney.domain.ledger.TransactionDraft
import app.privatemoney.domain.ledger.TransactionType
import app.privatemoney.domain.model.AmountInput
import app.privatemoney.domain.model.CategoryKind
import app.privatemoney.domain.model.Money
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

enum class AddError { AMOUNT_REQUIRED, AMOUNT_INVALID, CATEGORY_REQUIRED, ACCOUNT_REQUIRED, DESTINATION_REQUIRED, SAME_ACCOUNT, SAVE_FAILED }

@Immutable
data class Option(val id: String, val name: String)

@Immutable
data class AddUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val categories: List<Option> = emptyList(),
    val accounts: List<Option> = emptyList(),
    val categoryId: String? = null,
    val accountId: String? = null,
    val toAccountId: String? = null,
    val merchant: String = "",
    val note: String = "",
    val error: AddError? = null,
    val customDate: String? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
)

class AddViewModel(
    private val ledger: LedgerRepository,
    accounts: AccountRepository,
    categories: CategoryRepository,
    private val settings: AppSettings,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val _state = MutableStateFlow(AddUiState())
    val state: StateFlow<AddUiState> = _state.asStateFlow()
    private var allCategories: List<CategoryEntity> = emptyList()

    init {
        viewModelScope.launch {
            accounts.observeActive().collect { list ->
                val options = list.map { Option(it.id, it.name) }
                _state.update { s ->
                    val keep = s.accountId?.takeIf { id -> options.any { it.id == id } }
                    val remembered = settings.lastAccountId?.takeIf { id -> options.any { it.id == id } }
                    s.copy(accounts = options, accountId = keep ?: remembered ?: options.firstOrNull()?.id)
                }
            }
        }
        viewModelScope.launch {
            categories.observeActive().collect { list ->
                allCategories = list
                _state.update { s -> s.copy(categories = optionsFor(s.type)) }
            }
        }
    }

    private fun optionsFor(type: TransactionType): List<Option> {
        val kind = when (type) {
            TransactionType.EXPENSE -> CategoryKind.EXPENSE
            TransactionType.INCOME -> CategoryKind.INCOME
            TransactionType.TRANSFER -> return emptyList()
        }
        return allCategories.filter { it.kind == kind.name && it.parentId == null }.map { Option(it.id, it.name) }
    }

    /** Clears the form when the screen is opened, keeping loaded lists and the chosen account. */
    fun reset() {
        _state.update { s ->
            AddUiState(accounts = s.accounts, accountId = s.accountId, categories = optionsFor(TransactionType.EXPENSE))
        }
    }

    fun setType(type: TransactionType) {
        _state.update { it.copy(type = type, categories = optionsFor(type), categoryId = null, toAccountId = null, error = null) }
    }

    fun onKey(key: Char) {
        val decimals = settings.currency.minorUnit
        _state.update { it.copy(amountText = AmountInput.append(it.amountText, key, decimals), error = null) }
    }

    fun onBackspace() {
        _state.update { it.copy(amountText = AmountInput.backspace(it.amountText), error = null) }
    }

    fun setCategory(id: String) = _state.update { it.copy(categoryId = id, error = null) }
    fun setAccount(id: String) = _state.update { it.copy(accountId = id, error = null) }
    fun setToAccount(id: String) = _state.update { it.copy(toAccountId = id, error = null) }
    fun setCustomDate(date: String?) = _state.update { it.copy(customDate = date) }

    fun setMerchant(value: String) {
        val trimmed = value.take(MAX_MERCHANT)
        _state.update { s ->
            val suggested = if (s.categoryId == null && s.type == TransactionType.EXPENSE) {
                suggestCategory(trimmed, allCategories)
            } else null
            s.copy(merchant = trimmed, categoryId = suggested ?: s.categoryId)
        }
    }

    fun setNote(value: String) = _state.update { it.copy(note = value.take(MAX_NOTE)) }

    fun save() {
        val s = _state.value
        if (s.saving) return
        val currency = settings.currency
        val amount = runCatching { Money.parse(s.amountText.ifEmpty { "0" }, currency) }.getOrNull()
        val error = when {
            s.amountText.isEmpty() || amount?.isZero == true -> AddError.AMOUNT_REQUIRED
            amount == null || !amount.isPositive -> AddError.AMOUNT_INVALID
            s.accountId == null -> AddError.ACCOUNT_REQUIRED
            s.type != TransactionType.TRANSFER && s.categoryId == null -> AddError.CATEGORY_REQUIRED
            s.type == TransactionType.TRANSFER && s.toAccountId == null -> AddError.DESTINATION_REQUIRED
            s.type == TransactionType.TRANSFER && s.toAccountId == s.accountId -> AddError.SAME_ACCOUNT
            else -> null
        }
        if (error != null || amount == null || s.accountId == null) {
            _state.update { it.copy(error = error) }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        val dateStr = s.customDate ?: LocalDate.now(zone).toString()
        val timeStr = LocalTime.now(zone).truncatedTo(ChronoUnit.MINUTES).toString()
        val catNeedWant = if (s.type == TransactionType.EXPENSE) {
            allCategories.find { it.id == s.categoryId }?.needWant
        } else null

        viewModelScope.launch {
            try {
                ledger.record(
                    TransactionDraft(
                        id = UUID.randomUUID().toString(),
                        type = s.type,
                        amountMinor = amount.amountMinor,
                        currency = currency,
                        accountId = s.accountId,
                        categoryId = if (s.type == TransactionType.TRANSFER) null else s.categoryId,
                        toAccountId = if (s.type == TransactionType.TRANSFER) s.toAccountId else null,
                    ),
                    TransactionDetails(
                        localDate = dateStr,
                        localTime = timeStr,
                        merchant = s.merchant.trim().ifEmpty { null },
                        notes = s.note.trim().ifEmpty { null },
                        needWant = catNeedWant,
                    ),
                )
                settings.lastAccountId = s.accountId
                _state.update { it.copy(saving = false, saved = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Never surface raw exception text; it may contain sensitive data.
                _state.update { it.copy(saving = false, error = AddError.SAVE_FAILED) }
            }
        }
    }

    private fun suggestCategory(merchant: String, categories: List<CategoryEntity>): String? {
        val lower = merchant.lowercase()
        return when {
            listOf("swiggy", "zomato", "blinkit", "zepto", "food", "grocery", "restaurant", "cafe", "coffee", "mart", "supermarket").any { lower.contains(it) } ->
                categories.find { it.id == "cat_food" }?.id
            listOf("uber", "ola", "rapido", "metro", "fuel", "petrol", "diesel", "shell", "hp", "fastag").any { lower.contains(it) } ->
                categories.find { it.id == "cat_transport" }?.id
            listOf("amazon", "flipkart", "myntra", "shopping", "clothes", "zara", "nike").any { lower.contains(it) } ->
                categories.find { it.id == "cat_shopping" }?.id
            listOf("rent", "electricity", "water", "airtel", "jio", "wifi", "broadband", "bill", "recharge").any { lower.contains(it) } ->
                categories.find { it.id == "cat_bills" }?.id
            listOf("doctor", "hospital", "pharmacy", "medicine", "apollo", "medplus", "clinic").any { lower.contains(it) } ->
                categories.find { it.id == "cat_health" }?.id
            listOf("netflix", "prime", "spotify", "hotstar", "movie", "cinema", "pvr", "game").any { lower.contains(it) } ->
                categories.find { it.id == "cat_entertainment" }?.id
            listOf("flight", "hotel", "travel", "irctc", "makemytrip", "airbnb", "train").any { lower.contains(it) } ->
                categories.find { it.id == "cat_travel" }?.id
            listOf("sip", "mutual fund", "zerodha", "groww", "stocks").any { lower.contains(it) } ->
                categories.find { it.id == "cat_investment" }?.id
            else -> null
        }
    }


    private companion object {
        const val MAX_MERCHANT = 80
        const val MAX_NOTE = 300
    }
}
