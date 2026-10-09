package app.privatemoney.ui.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.privatemoney.data.AppSettings
import app.privatemoney.data.CategoryRepository
import app.privatemoney.data.LedgerRepository
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
    zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    val state: StateFlow<HomeUiState> = run {
        val today = LocalDate.now(zone)
        val from = today.withDayOfMonth(1)
        val to = from.plusMonths(1)
        val currency = settings.currency
        combine(
            ledger.observeLiquidBalance(),
            ledger.observeTotal(TransactionType.INCOME, from.toString(), to.toString()),
            ledger.observeTotal(TransactionType.EXPENSE, from.toString(), to.toString()),
            ledger.observeRecent(RECENT_COUNT),
            categories.observeActive(),
        ) { liquid, income, expense, recent, cats ->
            val names = cats.associate { it.id to it.name }
            // Commitments and buffer are not configurable yet, so they are zero rather than invented.
            val safe = SafeToSpendEngine.calculate(SafeToSpendInput(liquid, 0, 0, 0, 0, 0), today)
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
