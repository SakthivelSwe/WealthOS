package app.privatemoney.ui.insights

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

    val totalMonthExpense = state.donutBreakdown.expensesMinor

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 140.dp),
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
                    text = "Cashflow Trends, Weekly Pacing & AI Wealth Intelligence",
                    style = MaterialTheme.typography.bodySmall,
                    color = finance.muted,
                )
            }
        }

        // Category Breakdown with Donut Chart
        item {
            CategoryBreakdownCard(state = state)
        }

        // Weekly Pacing Card
        item {
            WeeklyPacingCard(state = state)
        }

        // AI Wealth Advisor & Classic Literature Rules
        item {
            AiWealthAdvisorCard(
                state = state,
                onRequestAi = viewModel::requestAiAdvice,
            )
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

        // Individual Category Outflow List
        if (state.topCategories.isNotEmpty()) {
            item {
                SectionLabel("TOP EXPENSES BREAKDOWN")
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

        if (state.topCategories.isEmpty() && state.trend.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = "No analytics data yet",
                        body = "Record expenses and income to view beautiful monthly trends, weekly pacing, and AI wealth intelligence.",
                        icon = Icons.Default.PieChart,
                    )
                }
            }
        }
    }
}

/** Category Breakdown with Donut Chart matching Screenshot 2. */
@Composable
private fun CategoryBreakdownCard(state: InsightsUiState) {
    val finance = LocalFinanceColors.current
    val totalExpense = state.donutBreakdown.expensesMinor

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "❖ CATEGORY BREAKDOWN",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        letterSpacing = 1.sp,
                    ),
                )
            }

            // Donut Chart Centered
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                CategoryDonutChart(
                    expenses = state.donutBreakdown.expensesMinor,
                    savings = state.donutBreakdown.savingsMinor,
                    emis = state.donutBreakdown.emisMinor,
                )
            }

            // Donut Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(modifier = Modifier.size(10.dp, 6.dp).background(Color(0xFFEF4444), RoundedCornerShape(2.dp)))
                    Text("Expenses", style = TextStyle(fontSize = 11.sp, color = finance.muted))
                }
                Spacer(Modifier.width(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(modifier = Modifier.size(10.dp, 6.dp).background(Color(0xFF10B981), RoundedCornerShape(2.dp)))
                    Text("Savings", style = TextStyle(fontSize = 11.sp, color = finance.muted))
                }
                Spacer(Modifier.width(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(modifier = Modifier.size(10.dp, 6.dp).background(Color(0xFFF59E0B), RoundedCornerShape(2.dp)))
                    Text("EMIs", style = TextStyle(fontSize = 11.sp, color = finance.muted))
                }
            }

            // Horizontal Category Progress Bars
            if (state.topCategories.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.topCategories.take(4).forEach { cat ->
                        val maxMinor = state.topCategories.maxOfOrNull { it.amountMinor }?.takeIf { it > 0 } ?: 1L
                        val frac = (cat.amountMinor.toFloat() / maxMinor.toFloat()).coerceIn(0.04f, 1f)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Category Bullet & Label
                            Row(
                                modifier = Modifier.width(90.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF818CF8)),
                                )
                                Text(
                                    text = cat.name.uppercase(),
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        letterSpacing = 0.5.sp,
                                    ),
                                    maxLines = 1,
                                )
                            }

                            // Progress Bar
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .padding(horizontal = 8.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0x22FFFFFF)),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(frac)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF818CF8), Color(0xFFA78BFA)),
                                            ),
                                        ),
                                )
                            }

                            // Amount
                            Text(
                                text = MoneyFormat.format(cat.amountMinor, state.currency),
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Modern Canvas Donut Chart for Category Breakdown. */
@Composable
private fun CategoryDonutChart(
    expenses: Long,
    savings: Long,
    emis: Long,
    modifier: Modifier = Modifier,
) {
    val total = expenses + savings + emis
    Canvas(modifier = modifier.size(140.dp)) {
        val strokeWidth = 24.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        if (total <= 0) {
            drawCircle(
                color = Color(0xFF2A2D3A),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth),
            )
            return@Canvas
        }

        var currentAngle = -90f

        val expenseSweep = (expenses.toFloat() / total) * 360f
        val savingsSweep = (savings.toFloat() / total) * 360f
        val emisSweep = (emis.toFloat() / total) * 360f

        if (expenseSweep > 0) {
            drawArc(
                color = Color(0xFFEF4444),
                startAngle = currentAngle,
                sweepAngle = expenseSweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
            )
            currentAngle += expenseSweep
        }

        if (savingsSweep > 0) {
            drawArc(
                color = Color(0xFF10B981),
                startAngle = currentAngle,
                sweepAngle = savingsSweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
            )
            currentAngle += savingsSweep
        }

        if (emisSweep > 0) {
            drawArc(
                color = Color(0xFFF59E0B),
                startAngle = currentAngle,
                sweepAngle = emisSweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
            )
        }
    }
}

/** Weekly Pacing Card matching Screenshot 2 with Over-Expense Diagnostics. */
@Composable
private fun WeeklyPacingCard(state: InsightsUiState) {
    val finance = LocalFinanceColors.current
    val pacing = state.weeklyPacing

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (pacing.isOverExpense) Color(0x66F59E0B) else finance.glassBorder,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "❖ WEEKLY PACING",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B),
                        letterSpacing = 1.sp,
                    ),
                )
            }

            // Spent This Week Hero
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "SPENT THIS WEEK",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = finance.muted,
                        letterSpacing = 1.sp,
                    ),
                )
                Text(
                    text = MoneyFormat.format(pacing.spentThisWeekMinor, state.currency),
                    style = TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    ),
                )

                // Comparison Badge
                val isPacingDown = pacing.pacePercentChange <= 0
                val badgeColor = if (isPacingDown) Color(0xFF10B981) else Color(0xFFEF4444)
                val badgeBg = if (isPacingDown) Color(0x2210B981) else Color(0x22EF4444)
                val arrow = if (isPacingDown) "↓" else "↑"
                val prefix = if (pacing.pacePercentChange > 0) "+" else ""

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "$arrow $prefix${pacing.pacePercentChange}% vs last week",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                        ),
                    )
                }
            }

            // Over-Expense Warning Banner if pacing is high
            if (pacing.isOverExpense) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x22F59E0B))
                        .border(1.dp, Color(0x44F59E0B), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Over-Expense Alert: Outflows pacing higher than last week. Restrict discretionary spending in ${pacing.topCategoryName}.",
                            style = TextStyle(fontSize = 11.sp, color = Color(0xFFFDE68A), lineHeight = 15.sp),
                        )
                    }
                }
            }

            // 3-Column Stats Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x15FFFFFF))
                    .padding(vertical = 12.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                // Daily Avg
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DAILY AVG", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = finance.muted))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        MoneyFormat.format(pacing.dailyAvgMinor, state.currency),
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White),
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0x22FFFFFF)))

                // Top Category
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOP CATEGORY", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = finance.muted))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        pacing.topCategoryName,
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B)),
                        maxLines = 1,
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0x22FFFFFF)))

                // Last Week
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LAST WEEK", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = finance.muted))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        MoneyFormat.format(pacing.spentLastWeekMinor, state.currency),
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White),
                    )
                }
            }
        }
    }
}

/** AI Wealth Advisor Card with Timeless Money Literature Wisdom. */
@Composable
private fun AiWealthAdvisorCard(
    state: InsightsUiState,
    onRequestAi: () -> Unit,
) {
    val finance = LocalFinanceColors.current

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = Color(0x55818CF8),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0x33818CF8)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "AI WEALTH ADVISOR",
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA78BFA), letterSpacing = 1.sp),
                        )
                        Text(
                            text = "Gemini AI & Classic Money Literature",
                            style = TextStyle(fontSize = 10.sp, color = finance.muted),
                        )
                    }
                }

                Button(
                    onClick = onRequestAi,
                    enabled = !state.isAiLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1),
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    if (state.isAiLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("⚡ Ask AI", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            // AI Advice Output
            if (state.aiAdvice != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x22818CF8))
                        .border(1.dp, Color(0x44818CF8), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                ) {
                    Text(
                        text = state.aiAdvice,
                        style = TextStyle(fontSize = 12.sp, color = Color.White, lineHeight = 18.sp),
                    )
                }
            }

            // Classic Books Rules
            Text(
                text = "GLOBAL MONEY PRINCIPLES",
                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = finance.muted, letterSpacing = 1.sp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.wisdomQuotes.forEach { book ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x11FFFFFF))
                            .padding(12.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = book.title,
                                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8)),
                                )
                                Text(
                                    text = book.author,
                                    style = TextStyle(fontSize = 10.sp, color = finance.muted),
                                )
                            }
                            Text(
                                text = book.quote,
                                style = TextStyle(fontSize = 11.sp, color = Color(0xFFE2E8F0), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                            )
                            Text(
                                text = "Rule: ${book.actionableRule}",
                                style = TextStyle(fontSize = 10.sp, color = Color(0xFF34D399), fontWeight = FontWeight.Medium),
                            )
                        }
                    }
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
                    text = "Target: 20%+ (Warren's 50/30/20 Rule)",
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
