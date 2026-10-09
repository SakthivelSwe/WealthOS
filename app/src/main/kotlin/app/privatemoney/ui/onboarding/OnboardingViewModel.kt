package app.privatemoney.ui.onboarding

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.privatemoney.data.AccountRepository
import app.privatemoney.data.AppSettings
import app.privatemoney.data.CategoryRepository
import app.privatemoney.domain.model.AccountType
import app.privatemoney.domain.model.Currency
import app.privatemoney.domain.model.Money
import app.privatemoney.security.AppLock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingError { NAME_REQUIRED, BALANCE_INVALID, SAVE_FAILED }

@Immutable
data class OnboardingUiState(
    val saving: Boolean = false,
    val error: OnboardingError? = null,
    val done: Boolean = false,
)

class OnboardingViewModel(
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val settings: AppSettings,
    private val lock: AppLock,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun submit(name: String, type: AccountType, balanceText: String, currency: Currency, enableLock: Boolean) {
        if (_state.value.saving) return
        if (name.isBlank()) {
            _state.update { it.copy(error = OnboardingError.NAME_REQUIRED) }
            return
        }
        val opening = runCatching { Money.parse(balanceText.ifBlank { "0" }, currency) }.getOrNull()
        if (opening == null || opening.isNegative) {
            _state.update { it.copy(error = OnboardingError.BALANCE_INVALID) }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                settings.currency = currency
                categories.seedDefaultsIfEmpty()
                val id = accounts.create(name, type, opening.amountMinor, currency)
                settings.lastAccountId = id
                settings.lockEnabled = enableLock
                lock.refresh()
                settings.onboarded = true
                _state.update { it.copy(saving = false, done = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, error = OnboardingError.SAVE_FAILED) }
            }
        }
    }
}
