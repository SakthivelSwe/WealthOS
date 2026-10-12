package app.privatemoney.ui.insights

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.privatemoney.domain.model.Currency
import app.privatemoney.ui.MoneyFormat
import app.privatemoney.ui.components.CategoryGlyph
import app.privatemoney.ui.components.EmptyState
import app.privatemoney.ui.components.GlassCard
import app.privatemoney.ui.components.Hairline
import app.privatemoney.ui.components.LinearProgressModern
import app.privatemoney.ui.components.MoneyText
import app.privatemoney.ui.components.MoneyTint
import app.privatemoney.ui.components.SectionLabel
import app.privatemoney.ui.theme.LocalFinanceColors
import app.privatemoney.ui.theme.MoneyStyles
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun InsightsRoute(viewModel: InsightsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val finance = LocalFinanceColors.current

    val totalMonthExpense = state.topCategories.sumOf { it.amountMinor }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Financial Insights",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Cashflow Trends & Spending Analytics",
                    style = MaterialTheme.typography.bodySmall,
                    color = finance.muted,
                )
            }
        }

        // Financial Health Scorecard
        item {
            HealthScorecard(state = state)
        }

        // 6-Month Income vs Expense Visual Bar Chart
        if (state.trend.isNotEmpty()) {
            item {
                SectionLabel("6-MONTH CASHFLOW TREND")
            }
            item {
                CashflowTrendChart(trend = state.trend, currency = state.currency)
            }
        }

        // Category Breakdown
        if (state.topCategories.isNotEmpty()) {
            item {
                SectionLabel("TOP EXPENSES THIS MONTH")
            }

            // Category Segmented Bar
            item {
                CategorySegmentedBar(categories = state.topCategories, total = totalMonthExpense)
            }

            items(state.topCategories.size) { i ->
                val cat = state.topCategories[i]
                val pct = if (totalMonthExpense > 0) ((cat.amountMinor.toDouble() / totalMonthExpense) * 100).toInt() else 0
                CategorySpendCard(
                    rank = i + 1,
                    cat = cat,
                    percent = pct,
                    currency = state.currency,
                )
            }
        }

        // Smart Advice Pills
        item {
            SectionLabel("WEALTH STRATEGY")
        }
        item {
            WealthStrategyCards()
        }

        if (state.topCategories.isEmpty() && state.trend.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = "No analytics data yet",
                        body = "Record expenses and income to view beautiful monthly trends and spending intelligence.",
                        icon = Icons.Default.PieChart,
                    )
                }
            }
        }
    }
}

/** Scorecard rating financial discipline and wealth metrics. */
@Composable
private fun HealthScorecard(state: InsightsUiState) {
    val finance = LocalFinanceColors.current
    val totalIncome = state.trend.sumOf { it.income }
    val totalExpense = state.trend.sumOf { it.expense }
    val savingsRatio = if (totalIncome > 0) (((totalIncome - totalExpense).toDouble() / totalIncome) * 100).toInt() else 0

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(finance.primaryGlow),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text("SAVINGS RATE", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = finance.muted, letterSpacing = 1.sp))
                        Text(
                            if (savingsRatio >= 20) "Prime Health" else "Fair Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Text(
                    text = "$savingsRatio%",
                    style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = if (savingsRatio >= 20) finance.positive else finance.warning),
                )
            }

            LinearProgressModern(
                progress = (savingsRatio.toFloat() / 100f).coerceIn(0f, 1f),
                brush = Brush.horizontalGradient(listOf(finance.positive, finance.info)),
                height = 8.dp,
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Target: 20%+ recommended",
                    style = MaterialTheme.typography.bodySmall,
                    color = finance.muted,
                )
                Text(
                    text = if (savingsRatio >= 20) "✓ On Track" else "Needs Attention",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (savingsRatio >= 20) finance.positive else finance.warning),
                )
            }
        }
    }
}

/** 6-Month Income vs Expense Bar Chart. */
@Composable
private fun CashflowTrendChart(trend: List<InsightMonthTrendUi>, currency: Currency) {
    val finance = LocalFinanceColors.current
    val maxVal = trend.maxOfOrNull { maxOf(it.income, it.expense) }?.takeIf { it > 0 } ?: 1L

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Chart Legend
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(finance.positive))
                    Text("Income", style = TextStyle(fontSize = 11.sp, color = finance.muted))
                }
                Spacer(Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(finance.negative))
                    Text("Expense", style = TextStyle(fontSize = 11.sp, color = finance.muted))
                }
            }

            // Bars Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                trend.forEach { item ->
                    val incomeFrac = (item.income.toFloat() / maxVal.toFloat()).coerceIn(0.04f, 1f)
                    val expenseFrac = (item.expense.toFloat() / maxVal.toFloat()).coerceIn(0.04f, 1f)
                    val monthLabel = runCatching {
                        YearMonth.parse(item.month).format(DateTimeFormatter.ofPattern("MMM"))
                    }.getOrDefault(item.month.takeLast(2))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.weight(1f, fill = false),
                        ) {
                            // Income Bar
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(incomeFrac)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(finance.positive),
                            )
                            // Expense Bar
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(expenseFrac)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(finance.negative),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = monthLabel,
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = finance.muted),
                        )
                    }
                }
            }
        }
    }
}

/** Category Segmented Visual Bar. */
@Composable
private fun CategorySegmentedBar(categories: List<InsightCategoryUi>, total: Long) {
    if (total <= 0) return
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
            ) {
                categories.forEach { cat ->
                    val weight = (cat.amountMinor.toFloat() / total.toFloat()).coerceAtLeast(0.02f)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxSize()
                            .background(Color(cat.color)),
                    )
                    Spacer(Modifier.width(2.dp))
                }
            }
        }
    }
}

/** Individual Category Spend Card. */
@Composable
private fun CategorySpendCard(
    rank: Int,
    cat: InsightCategoryUi,
    percent: Int,
    currency: Currency,
) {
    val finance = LocalFinanceColors.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryGlyph(label = cat.name, customColor = Color(cat.color))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = cat.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "$percent% of month's expenses",
                    style = MaterialTheme.typography.bodySmall,
                    color = finance.muted,
                )
            }
            MoneyText(
                minor = cat.amountMinor,
                currency = currency,
                style = MoneyStyles.row,
                tint = MoneyTint.NEUTRAL,
            )
        }
    }
}

/** Pro-active wealth strategy tips. */
@Composable
private fun WealthStrategyCards() {
    val finance = LocalFinanceColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(finance.primaryGlow),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Cashflow Velocity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Maintain at least a 20% liquid savings rate to buffer unexpected expenses before investing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = finance.muted,
                    )
                }
            }
        }
    }
}
