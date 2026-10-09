package app.privatemoney.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.privatemoney.data.AppSettings
import app.privatemoney.data.CategoryRepository
import app.privatemoney.data.LedgerRepository
import app.privatemoney.domain.model.Currency
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class InsightCategoryUi(val name: String, val amountMinor: Long, val color: Long)

data class InsightMonthTrendUi(val month: String, val income: Long, val expense: Long)

data class InsightsUiState(
    val currency: Currency,
    val topCategories: List<InsightCategoryUi> = emptyList(),
    val trend: List<InsightMonthTrendUi> = emptyList(),
)

class InsightsViewModel(
    ledger: LedgerRepository,
    categories: CategoryRepository,
    settings: AppSettings,
    clock: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val today = clock()
    
    // 6 months trend
    private val startTrend = YearMonth.from(today).minusMonths(5).atDay(1).toString()
    
    // Top categories for current month
    private val startMonth = YearMonth.from(today).atDay(1).toString()
    private val endMonth = YearMonth.from(today).plusMonths(1).atDay(1).toString()

    val uiState: StateFlow<InsightsUiState> = combine(
        ledger.observeMonthlyTotals(startTrend),
        ledger.observeTopCategories(startMonth, endMonth, 5),
        categories.observeActive(),
    ) { monthlyRows, topCatRows, cats ->
        val catMap = cats.associateBy { it.id }
        
        val topCategories = topCatRows.map { row ->
            val cat = catMap[row.categoryId]
            InsightCategoryUi(
                name = cat?.name ?: "Unknown",
                amountMinor = row.total,
                color = cat?.color ?: 0xFF8E8E96
            )
        }
        
        val trendMap = mutableMapOf<String, InsightMonthTrendUi>()
        monthlyRows.forEach { row ->
            val existing = trendMap[row.month] ?: InsightMonthTrendUi(row.month, 0L, 0L)
            trendMap[row.month] = if (row.type == "INCOME") {
                existing.copy(income = row.total)
            } else {
                existing.copy(expense = row.total)
            }
        }

        InsightsUiState(
            currency = settings.currency,
            topCategories = topCategories,
            trend = trendMap.values.toList().sortedBy { it.month }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        InsightsUiState(settings.currency)
    )
}
