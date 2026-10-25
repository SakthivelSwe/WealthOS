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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    val isCloudLoggedIn: Boolean = false,
    val cloudUserEmail: String? = null,
    val activeCycle: app.privatemoney.data.local.SalaryCycleEntity? = null,
    val isSurvivalMode: Boolean = false,
    val survivalDailyCapPaise: Long? = null,
    val isSalaryWindowActive: Boolean = false,
    val dailyUpiMeterGuiltFreePaise: Long = 0L,
    val dailyUpiMeterNeedsPaise: Long = 0L,
    val dailyUpiMeterTotalPaise: Long = 0L,
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
        val cyclesFlow = planning?.observeSalaryCycles() ?: flowOf(emptyList())

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
            cyclesFlow,
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
            @Suppress("UNCHECKED_CAST")
            val cyclesList = args[10] as List<app.privatemoney.data.local.SalaryCycleEntity>

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

            val latestCycle = cyclesList.maxByOrNull { it.cycleStartDate }
            val guiltFreeDaily = latestCycle?.dailySafeUpiPaise ?: (safe?.safeTodayMinor ?: 0L)
            val needsAllowanceDaily = if (latestCycle != null && latestCycle.daysInCycle > 0) {
                settings.upiNeedsAllowancePaise / latestCycle.daysInCycle
            } else 0L
            val totalDailyUpi = guiltFreeDaily + needsAllowanceDaily

            // Survival mode (§5.5)
            val vaultAndUpiBalance = acctBalances
                .filter { it.account.accountRole == "DAILY_UPI_SPEND" || it.account.accountRole == "INCOME_VAULT" }
                .sumOf { maxOf(0L, it.currentBalanceMinor) }
            val effectiveSpendable = if (vaultAndUpiBalance > 0L) vaultAndUpiBalance else liquid
            val defaultThreshold = minOf(200000L, if (settings.fixedBaselinePaise != null) settings.fixedBaselinePaise!! / 10L else 200000L)
            val survivalThreshold = settings.survivalThresholdPaise ?: defaultThreshold
            val isSurvival = effectiveSpendable in 1..survivalThreshold

            val daysUntilNextPayday = if (latestCycle != null && latestCycle.daysInCycle > 0) {
                try {
                    val start = LocalDate.parse(latestCycle.cycleStartDate)
                    val passed = java.time.temporal.ChronoUnit.DAYS.between(start, today).toInt()
                    maxOf(1, latestCycle.daysInCycle - passed)
                } catch (_: Exception) {
                    30
                }
            } else 30
            val survivalDailyCap = if (isSurvival) effectiveSpendable / daysUntilNextPayday else null

            // Payday window (§5.4)
            val schedType = runCatching {
                app.privatemoney.domain.planning.IncomeScheduleType.valueOf(settings.incomeScheduleType)
            }.getOrDefault(app.privatemoney.domain.planning.IncomeScheduleType.VARIABLE_WINDOW)
            val cycleDef = app.privatemoney.domain.planning.CycleDefinition(
                scheduleType = schedType,
                paydayWindowStartDay = settings.paydayWindowStartDay,
                paydayWindowEndDay = settings.paydayWindowEndDay,
            )
            val isWindowActive = app.privatemoney.domain.planning.CycleEngine.isWithinPaydayWindow(today, cycleDef)

            HomeUiState(
                isLoading = false,
                currency = currency,
                availableMinor = liquid,
                safe = safe,
                incomeMinor = income,
                expenseMinor = expense,
                savedMinor = income - expense,
                recent = recent.map { it.toRowUi(names) },
                isCloudLoggedIn = !settings.supabaseUserId.isNullOrBlank() && !settings.supabaseAccessToken.isNullOrBlank(),
                cloudUserEmail = settings.supabaseUserEmail,
                activeCycle = latestCycle,
                isSurvivalMode = isSurvival,
                survivalDailyCapPaise = survivalDailyCap,
                isSalaryWindowActive = isWindowActive,
                dailyUpiMeterGuiltFreePaise = guiltFreeDaily,
                dailyUpiMeterNeedsPaise = needsAllowanceDaily,
                dailyUpiMeterTotalPaise = totalDailyUpi,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(currency = currency))
    }

    private companion object {
        const val RECENT_COUNT = 6
    }
}
