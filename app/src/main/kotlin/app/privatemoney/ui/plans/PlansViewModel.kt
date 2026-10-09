package app.privatemoney.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.privatemoney.data.AppSettings
import app.privatemoney.data.LedgerRepository
import app.privatemoney.data.PlanningRepository
import app.privatemoney.data.local.ChitFundEntity
import app.privatemoney.data.local.DebtEntity
import app.privatemoney.data.local.GoalEntity
import app.privatemoney.data.local.RecurringTxnEntity
import app.privatemoney.domain.model.Currency
import app.privatemoney.domain.planning.BudgetSplit
import app.privatemoney.domain.planning.Lane
import app.privatemoney.domain.planning.LaneStatus
import app.privatemoney.domain.planning.PlanReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class PlansUiState(
    val basisIncomeMinor: Long,
    val split: BudgetSplit,
    val currency: Currency,
    val isConfigured: Boolean,
    val lanes: List<LaneStatus> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val debts: List<DebtEntity> = emptyList(),
    val chitFunds: List<ChitFundEntity> = emptyList(),
    val recurringTxns: List<RecurringTxnEntity> = emptyList(),
)

data class PlanningData(
    val goals: List<GoalEntity>,
    val debts: List<DebtEntity>,
    val chitFunds: List<ChitFundEntity>,
    val recurringTxns: List<RecurringTxnEntity>
)

class PlansViewModel(
    private val ledger: LedgerRepository,
    private val settings: AppSettings,
    private val planning: PlanningRepository,
    private val clock: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val forceRefresh = MutableStateFlow(0)

    private val planningDataFlow = combine(
        planning.observeGoals(),
        planning.observeDebts(),
        planning.observeChitFunds(),
        planning.observeRecurringTxns()
    ) { goals, debts, chitFunds, recurringTxns ->
        PlanningData(goals, debts, chitFunds, recurringTxns)
    }

    val uiState: StateFlow<PlansUiState> = combine(
        forceRefresh,
        ledger.observeLaneTotals(
            fromDate = YearMonth.from(clock()).atDay(1).toString(),
            toDate = YearMonth.from(clock()).plusMonths(1).atDay(1).toString(),
        ),
        planningDataFlow
    ) { _, rows, pData ->
        val income = settings.basisIncomeMinor
        val split = BudgetSplit(settings.needsPct, settings.wantsPct, settings.savingsPct)
        
        val spentByLane = rows.mapNotNull { row -> 
            Lane.fromName(row.needWant)?.let { it to row.total }
        }.toMap()

        val today = clock()
        val yearMonth = YearMonth.from(today)
        val elapsedDays = today.dayOfMonth
        val totalDays = yearMonth.lengthOfMonth()

        val lanes = if (income > 0) {
            PlanReport.build(
                basisIncomeMinor = income,
                split = split,
                spentByLane = spentByLane,
                elapsedDays = elapsedDays,
                totalDays = totalDays,
            )
        } else emptyList()

        PlansUiState(
            basisIncomeMinor = income,
            split = split,
            currency = settings.currency,
            isConfigured = income > 0,
            lanes = lanes,
            goals = pData.goals,
            debts = pData.debts,
            chitFunds = pData.chitFunds,
            recurringTxns = pData.recurringTxns,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlansUiState(0L, BudgetSplit.DEFAULT, settings.currency, false))

    fun updateConfig(incomeMinor: Long, split: BudgetSplit) {
        settings.basisIncomeMinor = incomeMinor
        settings.needsPct = split.needsPct
        settings.wantsPct = split.wantsPct
        settings.savingsPct = split.savingsPct
        forceRefresh.update { it + 1 }
    }

    fun createGoal(name: String, targetMinor: Long, currentMinor: Long, monthlyMinor: Long, currency: String, date: String) {
        viewModelScope.launch {
            planning.createGoal(name, targetMinor, currentMinor, monthlyMinor, currency, date)
        }
    }

    fun createDebt(name: String, principalMinor: Long, balanceMinor: Long, apr: String, expectedMonthlyMinor: Long, currency: String) {
        viewModelScope.launch {
            planning.createDebt(name, principalMinor, balanceMinor, apr, expectedMonthlyMinor, currency)
        }
    }

    fun createChitFund(name: String, months: Int, startMonth: String, monthlyMinor: Long, currency: String) {
        viewModelScope.launch {
            planning.createChitFund(name, months, startMonth, monthlyMinor, currency)
        }
    }

    fun createRecurringTxn(type: String, amountMinor: Long, currency: String, accountId: String, categoryId: String?, desc: String?, freq: String, nextRun: String) {
        viewModelScope.launch {
            planning.createRecurringTxn(type, amountMinor, currency, accountId, categoryId, desc, freq, nextRun)
        }
    }
}
