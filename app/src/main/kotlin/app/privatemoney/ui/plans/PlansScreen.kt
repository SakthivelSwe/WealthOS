package app.privatemoney.ui.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.privatemoney.domain.planning.BudgetSplit
import app.privatemoney.domain.planning.Lane
import app.privatemoney.domain.planning.LaneStatus
import app.privatemoney.ui.MoneyFormat
import app.privatemoney.ui.theme.LocalFinanceColors

@Composable
fun PlansRoute(viewModel: PlansViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (!uiState.isConfigured) {
        PlansSetupScreen(
            initialIncome = uiState.basisIncomeMinor,
            initialSplit = uiState.split,
            onSave = { income, split -> viewModel.updateConfig(income, split) },
        )
    } else {
        PlansScreen(
            uiState = uiState,
            onEdit = {
                viewModel.updateConfig(0L, uiState.split) // Temporarily unset to show setup
            },
            viewModel = viewModel
        )
    }
}

@Composable
private fun PlansSetupScreen(
    initialIncome: Long,
    initialSplit: BudgetSplit,
    onSave: (Long, BudgetSplit) -> Unit,
) {
    var incomeStr by remember { mutableStateOf(if (initialIncome > 0) (initialIncome / 100).toString() else "") }
    var needsStr by remember { mutableStateOf(initialSplit.needsPct.toString()) }
    var wantsStr by remember { mutableStateOf(initialSplit.wantsPct.toString()) }
    var savingsStr by remember { mutableStateOf(initialSplit.savingsPct.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Setup Your Budget", style = MaterialTheme.typography.headlineMedium)
        Text("We use the 50/30/20 rule by default, but you can adjust it.", color = LocalFinanceColors.current.muted)
        
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = incomeStr,
            onValueChange = { incomeStr = it },
            label = { Text("Monthly Basis Income") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = needsStr,
                onValueChange = { needsStr = it },
                label = { Text("Needs %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = wantsStr,
                onValueChange = { wantsStr = it },
                label = { Text("Wants %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = savingsStr,
                onValueChange = { savingsStr = it },
                label = { Text("Savings %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        val n = needsStr.toIntOrNull() ?: 0
        val w = wantsStr.toIntOrNull() ?: 0
        val s = savingsStr.toIntOrNull() ?: 0
        val total = n + w + s

        if (total != 100) {
            Text("Total must be 100% (currently $total%)", color = MaterialTheme.colorScheme.error)
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
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Budget")
        }
    }
}

@Composable
private fun PlansScreen(uiState: PlansUiState, onEdit: () -> Unit, viewModel: PlansViewModel) {
    var expandedMenu by remember { mutableStateOf(false) }
    
    var showAddGoal by remember { mutableStateOf(false) }
    var showAddDebt by remember { mutableStateOf(false) }
    var showAddChitFund by remember { mutableStateOf(false) }
    var showAddRecurring by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { expandedMenu = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Plan Item")
                }
                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false }
                ) {
                    DropdownMenuItem(text = { Text("Add Goal") }, onClick = { expandedMenu = false; showAddGoal = true })
                    DropdownMenuItem(text = { Text("Add Debt") }, onClick = { expandedMenu = false; showAddDebt = true })
                    DropdownMenuItem(text = { Text("Add Chit Fund") }, onClick = { expandedMenu = false; showAddChitFund = true })
                    DropdownMenuItem(text = { Text("Add Recurring Txn") }, onClick = { expandedMenu = false; showAddRecurring = true })
                }
            }
        }
    ) { paddingVals ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Budget Plans", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                Button(onClick = onEdit) { Text("Edit") }
            }

            uiState.lanes.forEach { lane ->
                LaneCard(lane = lane, currency = uiState.currency)
            }

            if (uiState.goals.isNotEmpty()) {
                Text("Goals", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                uiState.goals.forEach { goal ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(goal.name, style = MaterialTheme.typography.titleSmall)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Target: ${goal.targetDate}", style = MaterialTheme.typography.bodySmall, color = LocalFinanceColors.current.muted)
                                Text("${app.privatemoney.ui.MoneyFormat.format(goal.currentAmountMinor, uiState.currency)} / ${app.privatemoney.ui.MoneyFormat.format(goal.targetAmountMinor, uiState.currency)}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            if (uiState.debts.isNotEmpty()) {
                Text("Debts", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                uiState.debts.forEach { debt ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(debt.name, style = MaterialTheme.typography.titleSmall)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("APR: ${debt.annualPercent}%", style = MaterialTheme.typography.bodySmall, color = LocalFinanceColors.current.muted)
                                Text(app.privatemoney.ui.MoneyFormat.format(debt.currentBalanceMinor, uiState.currency), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            if (uiState.chitFunds.isNotEmpty()) {
                Text("Chit Funds", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                uiState.chitFunds.forEach { fund ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(fund.name, style = MaterialTheme.typography.titleSmall)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Months: ${fund.totalMonths}", style = MaterialTheme.typography.bodySmall, color = LocalFinanceColors.current.muted)
                                Text(app.privatemoney.ui.MoneyFormat.format(fund.monthlyContributionMinor, uiState.currency) + "/mo", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            if (uiState.recurringTxns.isNotEmpty()) {
                Text("Recurring Transactions", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                uiState.recurringTxns.forEach { txn ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(txn.description ?: txn.type, style = MaterialTheme.typography.titleSmall)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${txn.frequency} • Next: ${txn.nextRunDate}", style = MaterialTheme.typography.bodySmall, color = LocalFinanceColors.current.muted)
                                Text(app.privatemoney.ui.MoneyFormat.format(txn.amountMinor, uiState.currency), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(80.dp)) // space for FAB
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

@Composable
private fun LaneCard(lane: LaneStatus, currency: app.privatemoney.domain.model.Currency) {
    val progress = lane.progress
    val isSavings = lane.lane == Lane.SAVINGS
    val percent = if (progress.plannedMinor > 0) {
        (progress.spentMinor.toDouble() / progress.plannedMinor.toDouble()).coerceIn(0.0, 1.0)
    } else 0.0

    val overBudget = progress.spentMinor > progress.plannedMinor
    val warningColor = MaterialTheme.colorScheme.error
    val goodColor = Color(0xFF4CAF50)
    val neutralColor = MaterialTheme.colorScheme.primary

    val barColor = if (isSavings) {
        if (progress.spentMinor >= progress.plannedMinor) goodColor else neutralColor
    } else {
        if (overBudget) warningColor else goodColor
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row {
                Text(lane.lane.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    MoneyFormat.format(progress.spentMinor, currency) + " / " + MoneyFormat.format(progress.plannedMinor, currency),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((percent).toFloat())
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(barColor)
                )

                if (lane.expectedByNowMinor > 0 && progress.plannedMinor > 0) {
                    val expectedPercent = (lane.expectedByNowMinor.toDouble() / progress.plannedMinor.toDouble()).coerceIn(0.0, 1.0)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((expectedPercent).toFloat())
                            .height(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(8.dp)
                                .background(MaterialTheme.colorScheme.onSurface)
                                .align(Alignment.CenterEnd)
                        )
                    }
                }
            }

            Row {
                Text(
                    if (isSavings) "Saved" else "Spent",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalFinanceColors.current.muted,
                    modifier = Modifier.weight(1f)
                )
                
                val varianceText = if (progress.varianceMinor > 0) {
                    "+" + MoneyFormat.format(progress.varianceMinor, currency)
                } else {
                    MoneyFormat.format(progress.varianceMinor, currency)
                }
                Text(
                    "Projected variance: $varianceText",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSavings && progress.varianceMinor >= 0) goodColor else if (!isSavings && progress.varianceMinor <= 0) goodColor else warningColor
                )
            }
        }
    }
}
