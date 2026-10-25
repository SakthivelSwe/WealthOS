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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import app.privatemoney.domain.model.Money
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
                val inc = Money.parseIndian(incomeStr).amountMinor
                if (total == 100 && inc > 0) {
                    onSave(inc, BudgetSplit(n, w, s))
                }
            },
            enabled = total == 100 && Money.parseIndian(incomeStr).amountMinor > 0,
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 140.dp),
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

            if (uiState.unclassifiedSpendMinor > 0) {
                item {
                    GlassCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text("⚠️ Unclassified Outflows", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFFFBBF24))
                                Spacer(Modifier.height(4.dp))
                                Text("Expenses missing Need / Want tags excluded from 50/30/20 comparison.", fontSize = 12.sp, color = finance.muted)
                            }
                            MoneyText(
                                minor = uiState.unclassifiedSpendMinor,
                                currency = uiState.currency,
                                style = MoneyStyles.row,
                                tint = MoneyTint.NEGATIVE,
                            )
                        }
                    }
                }
            }

            // Goals Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SectionLabel("SAVINGS GOALS (${uiState.goals.size})")
                    Text(
                        text = "+ NEW GOAL",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8)),
                        modifier = Modifier.clickable { showAddGoal = true },
                    )
                }
            }

            if (uiState.goals.isNotEmpty()) {
                items(uiState.goals.size) { i ->
                    GoalCard(
                        goal = uiState.goals[i],
                        currency = uiState.currency,
                        onContribute = { amount -> viewModel.contributeToGoal(uiState.goals[i].id, amount) },
                    )
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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SectionLabel("CHIT FUNDS (${uiState.chitFunds.size})")
                    Text(
                        text = "+ NEW CHIT",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B)),
                        modifier = Modifier.clickable { showAddChitFund = true },
                    )
                }
            }

            // Chit Funds Explainer Banner matching Screenshot 4
            item {
                ChitFundExplainerBanner()
            }

            if (uiState.chitFunds.isNotEmpty()) {
                items(uiState.chitFunds.size) { i ->
                    ChitFundCard(
                        fund = uiState.chitFunds[i],
                        currency = uiState.currency,
                        onToggleStatus = { active -> viewModel.toggleChitFundStatus(uiState.chitFunds[i].id, active) },
                    )
                }
            } else {
                item {
                    ChitFundEmptyCard(onAddChit = { showAddChitFund = true })
                }
            }

            // Recurring Transactions Section
            if (uiState.recurringTxns.isNotEmpty()) {
                item {
                    SectionLabel("RECURRING SUBSCRIPTIONS (${uiState.recurringTxns.size})")
                }
                items(uiState.recurringTxns.size) { i ->
                    val txn = uiState.recurringTxns[i]
                    RecurringTxnCard(
                        txn = txn,
                        currency = uiState.currency,
                        onToggleActive = { viewModel.toggleRecurringActive(txn.id, !txn.active) },
                        onSkipOnce = { viewModel.skipRecurringOnce(txn.id) },
                        onCancel = { viewModel.cancelRecurring(txn.id) },
                    )
                }
            }
        }
    }

    if (showAddGoal) {
        AddGoalDialog(currency = uiState.currency, onDismiss = { showAddGoal = false }, onSave = viewModel::createGoal)
    }
    if (showAddDebt) {
        AddDebtDialog(currency = uiState.currency, onDismiss = { showAddDebt = false }, onSave = viewModel::createDebt)
    }
    if (showAddChitFund) {
        AddChitFundDialog(currency = uiState.currency, onDismiss = { showAddChitFund = false }, onSave = viewModel::createChitFund)
    }
    if (showAddRecurring) {
        AddRecurringTxnDialog(currency = uiState.currency, onDismiss = { showAddRecurring = false }, onSave = viewModel::createRecurringTxn)
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

            if (uiState.actualIncomeMinor > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("ACTUAL RECEIVED THIS MONTH", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, color = finance.muted, letterSpacing = 0.5.sp))
                    MoneyText(uiState.actualIncomeMinor, uiState.currency, style = MoneyStyles.small, tint = MoneyTint.POSITIVE)
                }
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
private fun GoalCard(
    goal: GoalEntity,
    currency: Currency,
    onContribute: (Long) -> Unit,
) {
    val finance = LocalFinanceColors.current
    val pctInt = Money.calculatePercentage(goal.currentAmountMinor, goal.targetAmountMinor)
    val pct = if (goal.targetAmountMinor > 0) {
        (goal.currentAmountMinor.toFloat() / goal.targetAmountMinor.toFloat()).coerceIn(0f, 1f)
    } else 0f

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Circular Progress Badge matching Screenshot 3
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x2210B981))
                        .border(2.dp, Color(0xFF10B981).copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$pctInt%",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                        ),
                    )
                }

                // Title and Amounts
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = "${MoneyFormat.format(goal.currentAmountMinor, currency)} / ${MoneyFormat.format(goal.targetAmountMinor, currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8),
                    )
                    Text(
                        text = "Due: ${goal.targetDate}",
                        style = TextStyle(fontSize = 11.sp, color = finance.muted),
                    )
                }
            }

            // Linear Progress Track
            LinearProgressModern(
                progress = pct,
                brush = Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
                height = 5.dp,
            )

            // Quick Deposit Chips: +₹500, +₹1K, +₹5K matching Screenshot 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Quick Save:",
                    style = TextStyle(fontSize = 11.sp, color = finance.muted, fontWeight = FontWeight.Medium),
                )
                val sym = MoneyFormat.symbol(currency)
                QuickDepositChip(
                    label = "+${sym}500",
                    onClick = { onContribute(50_000L) },
                )
                QuickDepositChip(
                    label = "+${sym}1K",
                    onClick = { onContribute(100_000L) },
                )
                QuickDepositChip(
                    label = "+${sym}5K",
                    onClick = { onContribute(500_000L) },
                )
            }
        }
    }
}

@Composable
private fun QuickDepositChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x2238BDF8))
            .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8),
            ),
        )
    }
}

@Composable
private fun DebtCard(debt: DebtEntity, currency: Currency) {
    val finance = LocalFinanceColors.current
    val payoffMonths = remember(debt.currentBalanceMinor, debt.annualPercent, debt.expectedMonthlyPaymentMinor) {
        val apr = runCatching { java.math.BigDecimal(debt.annualPercent) }.getOrNull()
        if (apr != null && debt.currentBalanceMinor > 0 && debt.expectedMonthlyPaymentMinor > 0) {
            app.privatemoney.domain.planning.LoanEngine.payoff(debt.currentBalanceMinor, apr, debt.expectedMonthlyPaymentMinor)?.months
        } else null
    }

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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Expected Payment", style = MaterialTheme.typography.bodySmall, color = finance.muted)
                Text(
                    "${MoneyFormat.format(debt.expectedMonthlyPaymentMinor, currency)} / mo",
                    style = TextStyle(fontSize = 12.sp, color = finance.muted),
                )
            }
            if (payoffMonths != null) {
                Text(
                    text = "Projected payoff: ~$payoffMonths months (Assumes fixed rate & payments; projection only)",
                    style = TextStyle(fontSize = 10.sp, color = finance.muted),
                )
            }
        }
    }
}

@Composable
private fun ChitFundExplainerBanner() {
    val finance = LocalFinanceColors.current
    GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = Color(0x66F59E0B)) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x33F59E0B)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Savings, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "CHIT FUND (CHEETU)",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), letterSpacing = 1.sp),
                )
                Text(
                    text = "Tamil traditional rotating savings scheme",
                    style = TextStyle(fontSize = 11.sp, color = finance.muted),
                )
                Text(
                    text = "A group of members contribute monthly. Total amount = members × monthly. Each month one member wins the prize (auction price = total - discount). Jewel Chit = prize is gold grams instead of cash.",
                    style = TextStyle(fontSize = 11.sp, color = Color(0xFFE2E8F0), lineHeight = 15.sp),
                )
                Text(
                    text = "⚠️ Disclaimer: Chit funds carry counterparty and credit risk. Track personal contributions and payouts here; this app does not guarantee returns or legal underwriting.",
                    style = TextStyle(fontSize = 10.sp, color = Color(0xFFF59E0B), lineHeight = 14.sp),
                )
            }
        }
    }
}

@Composable
private fun ChitFundEmptyCard(onAddChit: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x33F59E0B)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Savings, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
            }
            Text("No chit funds tracked yet.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Add your Shriram / Kapil / local chit fund to track installments",
                style = MaterialTheme.typography.bodySmall,
                color = LocalFinanceColors.current.muted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ChitFundCard(
    fund: ChitFundEntity,
    currency: Currency,
    onToggleStatus: (Boolean) -> Unit,
) {
    val finance = LocalFinanceColors.current
    val isJewel = fund.name.contains("Jewel", ignoreCase = true) || fund.name.contains("Gold", ignoreCase = true)
    val poolMinor = fund.totalMonths * fund.monthlyContributionMinor

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (isJewel) Color(0x66F59E0B) else Color(0x5538BDF8),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = if (isJewel) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp),
                    )
                    Text(fund.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isJewel) Color(0x22F59E0B) else Color(0x2238BDF8))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = if (isJewel) "💎 Jewel Chit" else "💰 Cash Chit",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isJewel) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Monthly Installment", style = MaterialTheme.typography.bodySmall, color = finance.muted)
                    Text(
                        "${MoneyFormat.format(fund.monthlyContributionMinor, currency)} / mo",
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Prize Pool (${fund.totalMonths} Mos)", style = MaterialTheme.typography.bodySmall, color = finance.muted)
                    Text(
                        MoneyFormat.format(poolMinor, currency),
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isJewel) Color(0xFFF59E0B) else finance.positive),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Started: ${fund.startMonth}",
                    style = TextStyle(fontSize = 11.sp, color = finance.muted),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (fund.active) Color(0x2210B981) else Color(0x22888888))
                        .clickable { onToggleStatus(!fund.active) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = if (fund.active) "ACTIVE" else "COMPLETED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (fund.active) finance.positive else finance.muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurringTxnCard(
    txn: RecurringTxnEntity,
    currency: Currency,
    onToggleActive: () -> Unit = {},
    onSkipOnce: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
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
                    if (!txn.active) {
                        Text("(Paused)", style = MaterialTheme.typography.bodySmall, color = finance.negative)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (txn.autoPost) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = if (txn.autoPost) "AUTO-POST" else "REMINDER",
                            style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Text(txn.frequency, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = finance.muted))
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Next: ${txn.nextRunDate}", style = MaterialTheme.typography.bodySmall, color = finance.muted)
                MoneyText(txn.amountMinor, currency, style = MoneyStyles.row)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onSkipOnce) {
                    Text("Skip Once", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onToggleActive) {
                    Text(if (txn.active) "Pause" else "Resume", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onCancel) {
                    Text("Cancel", style = MaterialTheme.typography.bodySmall, color = finance.negative)
                }
            }
        }
    }
}
