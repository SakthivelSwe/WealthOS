package app.privatemoney.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.privatemoney.R
import app.privatemoney.domain.ledger.TransactionType
import app.privatemoney.domain.model.Currency
import app.privatemoney.ui.MoneyFormat
import app.privatemoney.ui.components.SectionLabel
import app.privatemoney.ui.theme.LocalFinanceColors
import app.privatemoney.ui.theme.MoneyStyles

@Composable
fun AddTransactionScreen(viewModel: AddViewModel, currency: Currency, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) { viewModel.reset() }
    LaunchedEffect(state.saved) {
        if (state.saved) {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            onClose()
        }
    }

    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) { Text(stringResource(R.string.cancel)) }
            Spacer(Modifier.weight(1f))
        }
        TypeTabs(state.type, viewModel::setType)

        Text(
            text = MoneyFormat.symbol(currency) + state.amountText.ifEmpty { "0" },
            style = MoneyStyles.input,
            color = if (state.amountText.isEmpty()) LocalFinanceColors.current.muted else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            maxLines = 1,
        )

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.type != TransactionType.TRANSFER) {
                SectionLabel(stringResource(R.string.category))
                OptionChips(state.categories, state.categoryId, viewModel::setCategory)
            }
            SectionLabel(stringResource(if (state.type == TransactionType.TRANSFER) R.string.from_account else R.string.account))
            OptionChips(state.accounts, state.accountId, viewModel::setAccount)
            if (state.type == TransactionType.TRANSFER) {
                SectionLabel(stringResource(R.string.to_account))
                OptionChips(state.accounts, state.toAccountId, viewModel::setToAccount)
            }
            OutlinedTextField(
                value = state.merchant,
                onValueChange = viewModel::setMerchant,
                label = { Text(stringResource(R.string.merchant_optional)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text(stringResource(R.string.note_optional)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.error?.let {
            Text(
                stringResource(errorText(it)),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        Keypad(currency.minorUnit > 0, viewModel::onKey, viewModel::onBackspace)
        Button(
            onClick = viewModel::save,
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).height(52.dp),
        ) {
            Text(
                stringResource(
                    when (state.type) {
                        TransactionType.EXPENSE -> R.string.save_expense
                        TransactionType.INCOME -> R.string.save_income
                        TransactionType.TRANSFER -> R.string.save_transfer
                    },
                ),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun errorText(error: AddError): Int = when (error) {
    AddError.AMOUNT_REQUIRED -> R.string.err_amount_required
    AddError.AMOUNT_INVALID -> R.string.err_amount_invalid
    AddError.CATEGORY_REQUIRED -> R.string.err_category_required
    AddError.ACCOUNT_REQUIRED -> R.string.err_account_required
    AddError.DESTINATION_REQUIRED -> R.string.err_destination_required
    AddError.SAME_ACCOUNT -> R.string.err_same_account
    AddError.SAVE_FAILED -> R.string.err_save_failed
}

@Composable
private fun TypeTabs(selected: TransactionType, onSelect: (TransactionType) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        TransactionType.entries.forEach { type ->
            val active = type == selected
            val label = stringResource(
                when (type) {
                    TransactionType.EXPENSE -> R.string.type_expense
                    TransactionType.INCOME -> R.string.type_income
                    TransactionType.TRANSFER -> R.string.type_transfer
                },
            )
            Column(
                Modifier.clickable(role = Role.Tab) { onSelect(type) }.padding(vertical = 8.dp).semantics { contentDescription = label },
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (active) MaterialTheme.colorScheme.onBackground else LocalFinanceColors.current.muted,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.height(2.dp).fillMaxWidth().background(
                        if (active) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionChips(options: List<Option>, selectedId: String?, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option.id == selectedId,
                onClick = { onSelect(option.id) },
                label = { Text(option.name) },
            )
        }
    }
}

@Composable
private fun Keypad(decimalEnabled: Boolean, onKey: (Char) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf("123", "456", "789", ".0<")
    Column(Modifier.padding(horizontal = 12.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { ch ->
                    when {
                        ch == '<' -> KeyCell(stringResource(R.string.backspace), stringResource(R.string.backspace)) { onBackspace() }
                        ch == '.' && !decimalEnabled -> Box(Modifier.weight(1f).height(52.dp))
                        else -> KeyCell(ch.toString(), ch.toString()) { onKey(ch) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.KeyCell(label: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier.weight(1f).height(52.dp).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}
