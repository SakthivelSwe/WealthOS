package app.privatemoney.ui.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.privatemoney.data.local.ChitFundEntity
import app.privatemoney.data.local.DebtEntity
import app.privatemoney.data.local.GoalEntity
import app.privatemoney.data.local.RecurringTxnEntity
import app.privatemoney.domain.model.Currency
import app.privatemoney.domain.planning.BudgetSplit
import app.privatemoney.domain.planning.Lane
import app.privatemoney.domain.planning.LaneStatus
import app.privatemoney.ui.MoneyFormat
import app.privatemoney.ui.components.GlassCard
import app.privatemoney.ui.components.Hairline
import app.privatemoney.ui.components.LinearProgressModern
import app.privatemoney.ui.components.MoneyText
import app.privatemoney.ui.components.MoneyTint
import app.privatemoney.ui.components.SectionLabel
import app.privatemoney.ui.theme.LocalFinanceColors
import app.privatemoney.ui.theme.MoneyStyles

@Composable
fun PlansRoute(viewModel: PlansViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (!uiState.isConfigured) {
        PlansSetupScreen(
            initialIncome = uiState.basisIncomeMinor,
            initialSplit = uiState.split,
            currency = uiState.currency,
            onSave = { income, split -> viewModel.updateConfig(income, split) },
        )
    } else {
        PlansScreen(
            uiState = uiState,
            onEdit = { viewModel.updateConfig(0L, uiState.split) },
            viewModel = viewModel,
        )
    }
}

@Composable
private fun PlansSetupScreen(
    initialIncome: Long,
    initialSplit: BudgetSplit,
    currency: Currency,
    onSave: (Long, BudgetSplit) -> Unit,
) {
    var incomeStr by remember { mutableStateOf(if (initialIncome > 0) (initialIncome / 100).toString() else "") }
    var needsStr by remember { mutableStateOf(initialSplit.needsPct.toString()) }
    var wantsStr by remember { mutableStateOf(initialSplit.wantsPct.toString()) }
    var savingsStr by remember { mutableStateOf(initialSplit.savingsPct.toString()) }
    val finance = LocalFinanceColors.current

    val n = needsStr.toIntOrNull() ?: 0
    val w = wantsStr.toIntOrNull() ?: 0
    val s = savingsStr.toIntOrNull() ?: 0
    val total = n + w + s

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text("Setup Budget & Plans", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Allocate your monthly income into Needs (Essentials), Wants (Lifestyle), and Savings (Future Wealth).",
            style = MaterialTheme.typography.bodyMedium,
            color = finance.muted,
        )

        // Preset Chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BudgetPresetChip("50 / 30 / 20", n == 50 && w == 30 && s == 20) {
                needsStr = "50"; wantsStr = "30"; savingsStr = "20"
            }
            BudgetPresetChip("60 / 20 / 20", n == 60 && w == 20 && s == 20) {
                needsStr = "60"; wantsStr = "20"; savingsStr = "20"
            }
            BudgetPresetChip("70 / 20 / 10", n == 70 && w == 20 && s == 10) {
                needsStr = "70"; wantsStr = "20"; savingsStr = "10"
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = incomeStr,
                    onValueChange = { incomeStr = it },
                    label = { Text("Monthly Basis Income (${currency.code})") },
                    placeholder = { Text("e.g. 75000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = needsStr,
                        onValueChange = { needsStr = it },
                        label = { Text("Needs %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = wantsStr,
                        onValueChange = { wantsStr = it },
                        label = { Text("Wants %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = savingsStr,
                        onValueChange = { savingsStr = it },
                        label = { Text("Savings %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    )
                }

                if (total != 100) {
                    Text("Total must equal 100% (currently $total%)", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                val inc = (incomeStr.toDoubleOrNull() ?: 0.0) * 100
                if (total == 100 && inc > 0) {
                    onSave(inc.toLong(), BudgetSplit(n, w, s))
                }
            },
            enabled = total == 100 && (incomeStr.toDoubleOrNull() ?: 0.0) > 0,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("Save & Activate Budget", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BudgetPresetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val finance = LocalFinanceColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else finance.glassBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (selected) MaterialTheme.colorScheme.primary else finance.muted),
        )
    }
}

@Composable
private fun PlansScreen(
    uiState: PlansUiState,
    onEdit: () -> Unit,
    viewModel: PlansViewModel,
) {
    var expandedMenu by remember { mutableStateOf(false) }
    var showAddGoal by remember { mutableStateOf(false) }
    var showAddDebt by remember { mutableStateOf(false) }
    var showAddChitFund by remember { mutableStateOf(false) }
    var showAddRecurring by remember { mutableStateOf(false) }
    val finance = LocalFinanceColors.current

    Scaffold(
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { expandedMenu = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Plan Item")
                }
                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("🎯 Add Financial Goal") },
                        onClick = { expandedMenu = false; showAddGoal = true },
                    )
                    DropdownMenuItem(
                        text = { Text("💳 Add Debt / Loan") },
                        onClick = { expandedMenu = false; showAddDebt = true },
                    )
                    DropdownMenuItem(
                        text = { Text("🪙 Add Chit Fund") },
                        onClick = { expandedMenu = false; showAddChitFund = true },
                    )
                    DropdownMenuItem(
                        text = { Text("🔄 Add Recurring Subscription") },
                        onClick = { expandedMenu = false; showAddRecurring = true },
                    )
                }
            }
        },
    ) { paddingVals ->
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
            modifier = Modifier.padding(paddingVals),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Budget & Plans", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Monthly Allocation & Wealth Commitments", style = MaterialTheme.typography.bodySmall, color = finance.muted)
                    }
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Adjust")
                    }
                }
            }

            // Budget Split Multi-Bar Summary Card
            item {
                BudgetSplitHeroCard(uiState = uiState)
            }

            // Budget Lanes (Needs, Wants, Savings)
            item {
                SectionLabel("LANE TRACKING")
            }

            items(uiState.lanes.size) { index ->
                LaneCard(lane = uiState.lanes[index], currency = uiState.currency)
            }

            // Goals Section
            if (uiState.goals.isNotEmpty()) {
                item {
                    SectionLabel("FINANCIAL GOALS (${uiState.goals.size})")
                }
                items(uiState.goals.size) { i ->
                    GoalCard(goal = uiState.goals[i], currency = uiState.currency)
                }
            }

            // Debts Section
            if (uiState.debts.isNotEmpty()) {
                item {
                    SectionLabel("DEBTS & LIABILITIES (${uiState.debts.size})")
                }
                items(uiState.debts.size) { i ->
                    DebtCard(debt = uiState.debts[i], currency = uiState.currency)
                }
            }

            // Chit Funds Section
            if (uiState.chitFunds.isNotEmpty()) {
                item {
                    SectionLabel("CHIT FUNDS (${uiState.chitFunds.size})")
                }
                items(uiState.chitFunds.size) { i ->
                    ChitFundCard(fund = uiState.chitFunds[i], currency = uiState.currency)
                }
            }

            // Recurring Transactions Section
            if (uiState.recurringTxns.isNotEmpty()) {
                item {
                    SectionLabel("RECURRING SUBSCRIPTIONS (${uiState.recurringTxns.size})")
                }
                items(uiState.recurringTxns.size) { i ->
                    RecurringTxnCard(txn = uiState.recurringTxns[i], currency = uiState.currency)
                }
            }
        }
    }

    if (showAddGoal) {
        AddGoalDialog(onDismiss = { showAddGoal = false }, onSave = viewModel::createGoal)
    }
    if (showAddDebt) {
        AddDebtDialog(onDismiss = { showAddDebt = false }, onSave = viewModel::createDebt)
    }
    if (showAddChitFund) {
        AddChitFundDialog(onDismiss = { showAddChitFund = false }, onSave = viewModel::createChitFund)
    }
    if (showAddRecurring) {
        AddRecurringTxnDialog(onDismiss = { showAddRecurring = false }, onSave = viewModel::createRecurringTxn)
    }
}

/** Visual 50/30/20 multi-color segmented hero card. */
@Composable
private fun BudgetSplitHeroCard(uiState: PlansUiState) {
    val finance = LocalFinanceColors.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("MONTHLY BASIS INCOME", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = finance.muted, letterSpacing = 1.sp))
                MoneyText(uiState.basisIncomeMinor, uiState.currency, style = MoneyStyles.large, tint = MoneyTint.PRIMARY)
            }

            // Segmented Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .weight(uiState.split.needsPct.toFloat().coerceAtLeast(1f))
                        .fillMaxSize()
                        .background(Color(0xFF6366F1)),
                )
                Spacer(Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .weight(uiState.split.wantsPct.toFloat().coerceAtLeast(1f))
                        .fillMaxSize()
                        .background(Color(0xFFEC4899)),
                )
                Spacer(Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .weight(uiState.split.savingsPct.toFloat().coerceAtLeast(1f))
                        .fillMaxSize()
                        .background(Color(0xFF10B981)),
                )
            }

            // Legend Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SplitLegendItem(color = Color(0xFF6366F1), label = "Needs (${uiState.split.needsPct}%)")
                SplitLegendItem(color = Color(0xFFEC4899), label = "Wants (${uiState.split.wantsPct}%)")
                SplitLegendItem(color = Color(0xFF10B981), label = "Savings (${uiState.split.savingsPct}%)")
            }
        }
    }
}

@Composable
private fun SplitLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(text = label, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LocalFinanceColors.current.muted))
    }
}

@Composable
private fun LaneCard(lane: LaneStatus, currency: Currency) {
    val progress = lane.progress
    val isSavings = lane.lane == Lane.SAVINGS
    val percent = if (progress.plannedMinor > 0) {
        (progress.spentMinor.toDouble() / progress.plannedMinor.toDouble()).coerceIn(0.0, 1.0)
    } else 0.0

    val overBudget = progress.spentMinor > progress.plannedMinor
    val finance = LocalFinanceColors.current
    val barColor = when {
        isSavings -> if (progress.spentMinor >= progress.plannedMinor) finance.positive else finance.info
        overBudget -> finance.negative
        else -> finance.positive
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(lane.lane.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (overBudget && !isSavings) {
                        Text(
                            text = "OVER LIMIT",
                            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = finance.negative),
                        )
                    }
                }
                Text(
                    "${MoneyFormat.format(progress.spentMinor, currency)} / ${MoneyFormat.format(progress.plannedMinor, currency)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            LinearProgressModern(
                progress = percent.toFloat(),
                brush = Brush.horizontalGradient(listOf(barColor.copy(alpha = 0.8f), barColor)),
                height = 8.dp,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(if (isSavings) "Current Saved" else "Current Spent", style = MaterialTheme.typography.bodySmall, color = finance.muted)
                val varianceText = if (progress.varianceMinor > 0) "+${MoneyFormat.format(progress.varianceMinor, currency)}" else MoneyFormat.format(progress.varianceMinor, currency)
                Text(
                    "Projected: $varianceText",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overBudget && !isSavings) finance.negative else finance.positive,
                )
            }
        }
    }
}

@Composable
private fun GoalCard(goal: GoalEntity, currency: Currency) {
    val finance = LocalFinanceColors.current
    val pct = if (goal.targetAmountMinor > 0) {
        (goal.currentAmountMinor.toDouble() / goal.targetAmountMinor.toDouble()).coerceIn(0.0, 1.0)
    } else 0.0

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.TrackChanges, contentDescription = null, tint = finance.positive, modifier = Modifier.size(18.dp))
                    Text(goal.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Text("${(pct * 100).toInt()}%", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = finance.positive))
            }

            LinearProgressModern(
                progress = pct.toFloat(),
                brush = Brush.horizontalGradient(listOf(Color(0xFF00E599), Color(0xFF38BDF8))),
                height = 6.dp,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Target Date: ${goal.targetDate}", style = MaterialTheme.typography.bodySmall, color = finance.muted)
                Text(
                    "${MoneyFormat.format(goal.currentAmountMinor, currency)} / ${MoneyFormat.format(goal.targetAmountMinor, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun DebtCard(debt: DebtEntity, currency: Currency) {
    val finance = LocalFinanceColors.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = finance.negative, modifier = Modifier.size(18.dp))
                    Text(debt.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Text("${debt.annualPercent}% APR", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = finance.warning))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Balance Remaining", style = MaterialTheme.typography.bodySmall, color = finance.muted)
                MoneyText(debt.currentBalanceMinor, currency, style = MoneyStyles.row, tint = MoneyTint.NEGATIVE)
            }
        }
    }
}

@Composable
private fun ChitFundCard(fund: ChitFundEntity, currency: Currency) {
    val finance = LocalFinanceColors.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Savings, contentDescription = null, tint = finance.info, modifier = Modifier.size(18.dp))
                    Text(fund.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Text("${fund.totalMonths} Mos", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = finance.info))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Monthly Contribution", style = MaterialTheme.typography.bodySmall, color = finance.muted)
                MoneyText(fund.monthlyContributionMinor, currency, style = MoneyStyles.row)
            }
        }
    }
}

@Composable
private fun RecurringTxnCard(txn: RecurringTxnEntity, currency: Currency) {
    val finance = LocalFinanceColors.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = finance.muted, modifier = Modifier.size(18.dp))
                    Text(txn.description ?: txn.type, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Text(txn.frequency, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = finance.muted))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Next: ${txn.nextRunDate}", style = MaterialTheme.typography.bodySmall, color = finance.muted)
                MoneyText(txn.amountMinor, currency, style = MoneyStyles.row)
            }
        }
    }
}
