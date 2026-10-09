package app.privatemoney.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.privatemoney.R
import app.privatemoney.domain.model.Currency
import app.privatemoney.domain.planning.SafeToSpendBreakdown
import app.privatemoney.ui.components.EmptyState
import app.privatemoney.ui.components.Hairline
import app.privatemoney.ui.components.MoneyText
import app.privatemoney.ui.components.MoneyTint
import app.privatemoney.ui.components.SectionLabel
import app.privatemoney.ui.components.TransactionRow
import app.privatemoney.ui.theme.LocalFinanceColors
import app.privatemoney.ui.theme.MoneyStyles

@Composable
fun HomeRoute(viewModel: HomeViewModel, onOpenLedger: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(state, onOpenLedger)
}

@Composable
fun HomeScreen(state: HomeUiState, onOpenLedger: () -> Unit) {
    var showWhy by remember { mutableStateOf(false) }
    val muted = LocalFinanceColors.current.muted

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            SectionLabel(stringResource(R.string.available_now))
            Spacer(Modifier.height(4.dp))
            MoneyText(state.availableMinor, state.currency, style = MoneyStyles.hero)
            state.safe?.let { safe ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.safe_today), style = MaterialTheme.typography.bodyMedium, color = muted)
                    Spacer(Modifier.padding(horizontal = 6.dp))
                    MoneyText(safe.safeTodayMinor, state.currency, style = MoneyStyles.row)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showWhy = true }) { Text(stringResource(R.string.why)) }
                }
            }
            Spacer(Modifier.height(20.dp))
            Hairline()
            Spacer(Modifier.height(20.dp))
            SectionLabel(stringResource(R.string.this_month))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat(stringResource(R.string.income), state.incomeMinor, state.currency, MoneyTint.NEUTRAL, Modifier.weight(1f))
                Stat(stringResource(R.string.expense), state.expenseMinor, state.currency, MoneyTint.NEUTRAL, Modifier.weight(1f))
                Stat(
                    stringResource(R.string.saved),
                    state.savedMinor,
                    state.currency,
                    if (state.savedMinor < 0) MoneyTint.NEGATIVE else MoneyTint.NEUTRAL,
                    Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(20.dp))
            Hairline()
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(stringResource(R.string.recent))
                Spacer(Modifier.weight(1f))
                if (state.recent.isNotEmpty()) TextButton(onClick = onOpenLedger) { Text(stringResource(R.string.see_all)) }
            }
        }
        if (state.recent.isEmpty() && !state.isLoading) {
            item { EmptyState(stringResource(R.string.no_transactions_title), stringResource(R.string.no_transactions_body)) }
        }
        items(state.recent, key = { it.id }) { row ->
            TransactionRow(row, onClick = onOpenLedger)
            Hairline()
        }
    }

    val safe = state.safe
    if (showWhy && safe != null) {
        SafeToSpendDialog(safe, state.currency) { showWhy = false }
    }
}

@Composable
private fun Stat(label: String, minor: Long, currency: Currency, tint: MoneyTint, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = LocalFinanceColors.current.muted)
        MoneyText(minor, currency, style = MoneyStyles.large, tint = tint)
    }
}

@Composable
private fun SafeToSpendDialog(safe: SafeToSpendBreakdown, currency: Currency, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        title = { Text(stringResource(R.string.safe_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BreakdownLine(stringResource(R.string.liquid_balance), safe.input.liquidBalanceMinor, currency)
                BreakdownLine(stringResource(R.string.upcoming_bills), -safe.input.upcomingMandatoryBillsMinor, currency)
                BreakdownLine(stringResource(R.string.debt_payments), -safe.input.expectedDebtPaymentsMinor, currency)
                BreakdownLine(stringResource(R.string.goal_contributions), -safe.input.plannedGoalContributionsMinor, currency)
                BreakdownLine(stringResource(R.string.card_due), -safe.input.creditCardDueMinor, currency)
                BreakdownLine(stringResource(R.string.safety_buffer), -safe.input.safetyBufferMinor, currency)
                Hairline()
                BreakdownLine(stringResource(R.string.discretionary), safe.discretionaryMinor, currency)
                Text(
                    pluralStringResource(R.plurals.days_left, safe.daysRemainingInMonth, safe.daysRemainingInMonth),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalFinanceColors.current.muted,
                )
                Text(stringResource(R.string.safe_note), style = MaterialTheme.typography.bodySmall, color = LocalFinanceColors.current.muted)
            }
        },
    )
}

@Composable
private fun BreakdownLine(label: String, minor: Long, currency: Currency) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        MoneyText(minor, currency, style = MoneyStyles.row, tint = if (minor < 0) MoneyTint.NEGATIVE else MoneyTint.NEUTRAL)
    }
}
