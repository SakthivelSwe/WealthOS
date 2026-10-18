package app.privatemoney.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.privatemoney.cloud.GeminiAIService
import app.privatemoney.data.AppSettings
import app.privatemoney.data.CategoryRepository
import app.privatemoney.data.LedgerRepository
import app.privatemoney.data.PlanningRepository
import app.privatemoney.data.local.CategoryEntity
import app.privatemoney.data.local.CategorySpendRow
import app.privatemoney.data.local.MonthlyTotalRow
import app.privatemoney.domain.ledger.TransactionType
import app.privatemoney.domain.model.Currency
import app.privatemoney.domain.model.Money
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

enum class InsightsPeriod(val label: String) {
    THIS_MONTH("This Month"),
    PREVIOUS_MONTH("Last Month"),
    LAST_3_MONTHS("Last 3M"),
    LAST_6_MONTHS("Last 6M"),
    LAST_12_MONTHS("Last 12M"),
}

data class InsightCategoryUi(val name: String, val amountMinor: Long, val color: Long)

data class InsightMonthTrendUi(val month: String, val income: Long, val expense: Long)

data class WeeklyPacingUi(
    val spentThisWeekMinor: Long = 0L,
    val spentLastWeekMinor: Long = 0L,
    val dailyAvgMinor: Long = 0L,
    val topCategoryName: String = "None",
    val pacePercentChange: Int = 0,
    val isOverExpense: Boolean = false,
)

data class CategoryBreakdownDonutUi(
    val expensesMinor: Long = 0L,
    val savingsMinor: Long = 0L,
    val emisMinor: Long = 0L,
)

data class BookWisdomItem(
    val title: String,
    val author: String,
    val quote: String,
    val actionableRule: String,
)

data class InsightsUiState(
    val currency: Currency,
    val selectedPeriod: InsightsPeriod = InsightsPeriod.THIS_MONTH,
    val topCategories: List<InsightCategoryUi> = emptyList(),
    val trend: List<InsightMonthTrendUi> = emptyList(),
    val weeklyPacing: WeeklyPacingUi = WeeklyPacingUi(),
    val donutBreakdown: CategoryBreakdownDonutUi = CategoryBreakdownDonutUi(),
    val aiAdvice: String? = null,
    val isAiLoading: Boolean = false,
    val wisdomQuotes: List<BookWisdomItem> = emptyList(),
)

class InsightsViewModel(
    private val ledger: LedgerRepository,
    private val categories: CategoryRepository,
    private val planning: PlanningRepository,
    private val settings: AppSettings,
    private val geminiAi: GeminiAIService = GeminiAIService(),
    private val clock: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(InsightsPeriod.THIS_MONTH)
    private val _refreshTrigger = MutableStateFlow(0)
    private val _aiAdvice = MutableStateFlow<String?>(null)
    private val _isAiLoading = MutableStateFlow(false)

    fun selectPeriod(period: InsightsPeriod) {
        _selectedPeriod.value = period
    }

    fun refresh() {
        _refreshTrigger.update { it + 1 }
    }

    private val wisdomList = listOf(
        BookWisdomItem(
            title = "The Psychology of Money",
            author = "Morgan Housel",
            quote = "“True wealth is what you don't see. Spending money to show people how much money you have is the fastest way to have less money.”",
            actionableRule = "Freedom and peace of mind trump status signaling. Wealth is options, flexibility, and growth.",
        ),
        BookWisdomItem(
            title = "The Richest Man in Babylon",
            author = "George S. Clason",
            quote = "“A part of all you earn is yours to keep. It should be not less than a tenth no matter how little you earn.”",
            actionableRule = "Pay yourself first: Automatically set aside at least 10% before paying any monthly bills or discretionary expenses.",
        ),
        BookWisdomItem(
            title = "I Will Teach You to Be Rich",
            author = "Ramit Sethi",
            quote = "“Spend extravagantly on the things you love, and cut costs mercilessly on the things you don't.”",
            actionableRule = "Conscious Spending Plan: Stop feeling guilty about small pleasures; cut recurring leaks that provide zero joy.",
        ),
        BookWisdomItem(
            title = "All Your Worth",
            author = "Elizabeth Warren & Amelia Warren Tyagi",
            quote = "“The Balanced Money Formula: 50% Must-Haves, 30% Wants, 20% Savings & Debt Payoff.”",
            actionableRule = "Keep essential living costs strictly under 50% so you never experience financial claustrophobia.",
        ),
    )

    private var cachedWeeklySpend: Long = 0L
    private var cachedLastWeekSpend: Long = 0L
    private var cachedTopCatName: String = "None"
    private var cachedTopCatSpend: Long = 0L
    private var cachedMonthlySpend: Long = 0L

    private data class MonthlySpendData(
        val monthlyRows: List<MonthlyTotalRow>,
        val topCatRows: List<CategorySpendRow>,
        val cats: List<CategoryEntity>,
        val monthExpense: Long,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<InsightsUiState> = combine(_selectedPeriod, _refreshTrigger) { period, _ ->
        period
    }.flatMapLatest { period ->
        val currentDate = clock()
        val userCurrency = settings.currency.code

        val (fromDate, toDate, trendMonths) = when (period) {
            InsightsPeriod.THIS_MONTH -> Triple(
                YearMonth.from(currentDate).atDay(1).toString(),
                YearMonth.from(currentDate).plusMonths(1).atDay(1).toString(),
                6
            )
            InsightsPeriod.PREVIOUS_MONTH -> Triple(
                YearMonth.from(currentDate).minusMonths(1).atDay(1).toString(),
                YearMonth.from(currentDate).atDay(1).toString(),
                6
            )
            InsightsPeriod.LAST_3_MONTHS -> Triple(
                YearMonth.from(currentDate).minusMonths(2).atDay(1).toString(),
                YearMonth.from(currentDate).plusMonths(1).atDay(1).toString(),
                3
            )
            InsightsPeriod.LAST_6_MONTHS -> Triple(
                YearMonth.from(currentDate).minusMonths(5).atDay(1).toString(),
                YearMonth.from(currentDate).plusMonths(1).atDay(1).toString(),
                6
            )
            InsightsPeriod.LAST_12_MONTHS -> Triple(
                YearMonth.from(currentDate).minusMonths(11).atDay(1).toString(),
                YearMonth.from(currentDate).plusMonths(1).atDay(1).toString(),
                12
            )
        }

        val startTrend = YearMonth.from(currentDate).minusMonths((trendMonths - 1).toLong()).atDay(1).toString()

        val mondayThisWeek = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val nextMonday = mondayThisWeek.plusDays(7)
        val mondayLastWeek = mondayThisWeek.minusDays(7)

        val thisWeekStart = mondayThisWeek.toString()
        val thisWeekEnd = nextMonday.toString()
        val lastWeekStart = mondayLastWeek.toString()
        val lastWeekEnd = mondayThisWeek.toString()

        combine(
            combine(
                ledger.observeMonthlyTotals(startTrend, userCurrency),
                ledger.observeTopCategories(fromDate, toDate, 6, userCurrency),
                categories.observeActive(),
                ledger.observeTotal(TransactionType.EXPENSE, fromDate, toDate, userCurrency),
            ) { monthlyRows, topCatRows, cats, monthExpense ->
                MonthlySpendData(monthlyRows, topCatRows, cats, monthExpense)
            },
            combine(
                ledger.observeTotal(TransactionType.EXPENSE, thisWeekStart, thisWeekEnd, userCurrency),
                ledger.observeTotal(TransactionType.EXPENSE, lastWeekStart, lastWeekEnd, userCurrency),
            ) { thisWeek, lastWeek ->
                Pair(thisWeek, lastWeek)
            },
            combine(
                planning.observeGoals(),
                planning.observeDebts(),
            ) { goals, debts ->
                Pair(goals, debts)
            },
            combine(_aiAdvice, _isAiLoading) { advice, loading ->
                Pair(advice, loading)
            },
        ) { spendData, (spentThisWeek, spentLastWeek), (goals, debts), (aiAdvice, isAiLoading) ->
            val catMap = spendData.cats.associateBy { it.id }

            val topCategories = spendData.topCatRows.map { row ->
                val cat = catMap[row.categoryId]
                InsightCategoryUi(
                    name = cat?.name ?: "Unknown",
                    amountMinor = row.total,
                    color = cat?.color ?: 0xFF8E8E96,
                )
            }

            val totalMonthExpense = spendData.monthExpense
            val daysElapsed = currentDate.dayOfWeek.value.coerceIn(1, 7)
            val dailyAvg = spentThisWeek / daysElapsed
            val topCat = topCategories.firstOrNull()
            val topCatName = topCat?.name ?: "None"
            val topCatSpend = topCat?.amountMinor ?: 0L

            cachedWeeklySpend = spentThisWeek
            cachedLastWeekSpend = spentLastWeek
            cachedTopCatName = topCatName
            cachedTopCatSpend = topCatSpend
            cachedMonthlySpend = totalMonthExpense

            val pacePercentChange = when {
                spentLastWeek > 0L -> {
                    val diff = spentThisWeek - spentLastWeek
                    if (diff >= 0L) {
                        Money.calculatePercentage(diff, spentLastWeek)
                    } else {
                        -Money.calculatePercentage(-diff, spentLastWeek)
                    }
                }
                spentThisWeek > 0L -> 100
                else -> 0
            }

            val isOverExpense = (spentLastWeek > 0L && spentThisWeek > spentLastWeek) ||
                (spentLastWeek > 0L && dailyAvg * 7 > (spentLastWeek * 1.15).toLong())

            val trendMap = mutableMapOf<String, InsightMonthTrendUi>()
            spendData.monthlyRows.forEach { row ->
                val existing = trendMap[row.month] ?: InsightMonthTrendUi(row.month, 0L, 0L)
                trendMap[row.month] = if (row.type == "INCOME") {
                    existing.copy(income = row.total)
                } else {
                    existing.copy(expense = row.total)
                }
            }

            // Planned monthly savings contribution aligns with monthly cash flow breakdown,
            // avoiding distortion from lifetime accumulated savings balances.
            val totalSavingsMinor = goals
                .filter { it.currentAmountMinor < it.targetAmountMinor }
                .sumOf { minOf(it.targetAmountMinor - it.currentAmountMinor, it.plannedMonthlyContributionMinor) }
            val totalEmisMinor = debts.sumOf { it.expectedMonthlyPaymentMinor }

            InsightsUiState(
                currency = settings.currency,
                selectedPeriod = period,
                topCategories = topCategories,
                trend = trendMap.values.toList().sortedBy { it.month },
                weeklyPacing = WeeklyPacingUi(
                    spentThisWeekMinor = spentThisWeek,
                    spentLastWeekMinor = spentLastWeek,
                    dailyAvgMinor = dailyAvg,
                    topCategoryName = topCatName,
                    pacePercentChange = pacePercentChange,
                    isOverExpense = isOverExpense,
                ),
                donutBreakdown = CategoryBreakdownDonutUi(
                    expensesMinor = totalMonthExpense,
                    savingsMinor = totalSavingsMinor,
                    emisMinor = totalEmisMinor,
                ),
                aiAdvice = aiAdvice,
                isAiLoading = isAiLoading,
                wisdomQuotes = wisdomList,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        InsightsUiState(settings.currency, selectedPeriod = InsightsPeriod.THIS_MONTH, wisdomQuotes = wisdomList),
    )

    fun requestAiAdvice() {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val symbol = app.privatemoney.ui.MoneyFormat.symbol(settings.currency)
                val advice = geminiAi.generateFinancialAdvice(
                    currencySymbol = symbol,
                    weeklySpend = cachedWeeklySpend,
                    lastWeekSpend = cachedLastWeekSpend,
                    topCategory = cachedTopCatName,
                    topCategorySpend = cachedTopCatSpend,
                    totalMonthlyExpense = cachedMonthlySpend,
                    basisIncome = settings.basisIncomeMinor,
                )
                _aiAdvice.value = advice
            } catch (_: Exception) {
                val symbol = app.privatemoney.ui.MoneyFormat.symbol(settings.currency)
                _aiAdvice.value = geminiAi.getFallbackAdvice(symbol, cachedWeeklySpend, cachedTopCatName)
            } finally {
                _isAiLoading.value = false
            }
        }
    }
}
