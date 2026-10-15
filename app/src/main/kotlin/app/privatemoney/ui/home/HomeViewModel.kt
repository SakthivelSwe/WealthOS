package app.privatemoney.ui.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.privatemoney.data.AccountRepository
import app.privatemoney.data.AppSettings
import app.privatemoney.data.CategoryRepository
import app.privatemoney.data.LedgerRepository
import app.privatemoney.data.PlanningRepository
import app.privatemoney.domain.ledger.TransactionType
import app.privatemoney.domain.model.Currency
import app.privatemoney.domain.planning.SafeToSpendBreakdown
import app.privatemoney.domain.planning.SafeToSpendEngine
import app.privatemoney.domain.planning.SafeToSpendInput
import app.privatemoney.ui.ledger.TransactionRowUi
import app.privatemoney.ui.ledger.toRowUi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val currency: Currency = Currency.INR,
    val availableMinor: Long = 0,
    val safe: SafeToSpendBreakdown? = null,
    val incomeMinor: Long = 0,
    val expenseMinor: Long = 0,
    val savedMinor: Long = 0,
    val recent: List<TransactionRowUi> = emptyList(),
)

class HomeViewModel(
    ledger: LedgerRepository,
    categories: CategoryRepository,
    settings: AppSettings,
    planning: PlanningRepository? = null,
    accounts: AccountRepository? = null,
    zone: ZoneId = ZoneId.systemDefault(),
    private val clock: () -> LocalDate = { LocalDate.now(zone) },
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)

    fun refresh() {
        refreshTrigger.update { it + 1 }
    }

    val state: StateFlow<HomeUiState> = run {
        val initialToday = clock()
        val from = initialToday.withDayOfMonth(1)
        val to = from.plusMonths(1)
        val currency = settings.currency

        val recurringFlow = planning?.observeRecurringTxns() ?: flowOf(emptyList())
        val debtsFlow = planning?.observeDebts() ?: flowOf(emptyList())
        val goalsFlow = planning?.observeGoals() ?: flowOf(emptyList())
        val accountsFlow = accounts?.observeActiveWithBalances() ?: flowOf(emptyList())

        combine(
            refreshTrigger,
            ledger.observeLiquidBalance(currency.code),
            ledger.observeTotal(TransactionType.INCOME, from.toString(), to.toString(), currency.code),
            ledger.observeTotal(TransactionType.EXPENSE, from.toString(), to.toString(), currency.code),
            ledger.observeRecent(RECENT_COUNT),
            categories.observeActive(),
            recurringFlow,
            debtsFlow,
            goalsFlow,
            accountsFlow,
        ) { args ->
            val liquid = args[1] as Long
            val income = args[2] as Long
            val expense = args[3] as Long
            @Suppress("UNCHECKED_CAST")
            val recent = args[4] as List<app.privatemoney.data.local.TxnEntity>
            @Suppress("UNCHECKED_CAST")
            val cats = args[5] as List<app.privatemoney.data.local.CategoryEntity>
            @Suppress("UNCHECKED_CAST")
            val recurringList = args[6] as List<app.privatemoney.data.local.RecurringTxnEntity>
            @Suppress("UNCHECKED_CAST")
            val debtsList = args[7] as List<app.privatemoney.data.local.DebtEntity>
            @Suppress("UNCHECKED_CAST")
            val goalsList = args[8] as List<app.privatemoney.data.local.GoalEntity>
            @Suppress("UNCHECKED_CAST")
            val acctBalances = args[9] as List<app.privatemoney.data.local.AccountWithBalance>

            val names = cats.associate { it.id to it.name }

            val today = clock()
            val userCurrency = currency.code
            val currentMonthPrefix = today.toString().take(7)
            val upcomingBills = recurringList
                .filter { it.active && it.type == "EXPENSE" && it.currency == userCurrency && it.nextRunDate.startsWith(currentMonthPrefix) }
                .sumOf { it.amountMinor }
            val debtPayments = debtsList
                .filter { !it.archived && it.currency == userCurrency && it.currentBalanceMinor > 0 }
                .sumOf { minOf(it.currentBalanceMinor, it.expectedMonthlyPaymentMinor) }
            val goalContributions = goalsList
                .filter { !it.archived && it.currency == userCurrency && it.targetAmountMinor > it.currentAmountMinor }
                .sumOf { minOf(it.targetAmountMinor - it.currentAmountMinor, it.plannedMonthlyContributionMinor) }
            val ccOutstanding = acctBalances
                .filter { it.account.type == "CREDIT_CARD" && it.account.currency == userCurrency && it.currentBalanceMinor < 0 }
                .sumOf { kotlin.math.abs(it.currentBalanceMinor) }
            val buffer = if (income > 0) (income * 5) / 100 else 0L

            val safeInput = SafeToSpendInput(
                liquidBalanceMinor = liquid,
                upcomingMandatoryBillsMinor = upcomingBills,
                expectedDebtPaymentsMinor = debtPayments,
                plannedGoalContributionsMinor = goalContributions,
                creditCardDueMinor = ccOutstanding,
                safetyBufferMinor = buffer,
            )

            val safe = runCatching {
                SafeToSpendEngine.calculate(safeInput, today)
            }.getOrNull()

            HomeUiState(
                isLoading = false,
                currency = currency,
                availableMinor = liquid,
                safe = safe,
                incomeMinor = income,
                expenseMinor = expense,
                savedMinor = income - expense,
                recent = recent.map { it.toRowUi(names) },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(currency = currency))
    }

    private companion object {
        const val RECENT_COUNT = 6
    }
}
