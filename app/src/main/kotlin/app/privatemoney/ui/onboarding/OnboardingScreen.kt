package app.privatemoney.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.privatemoney.R
import app.privatemoney.domain.model.AccountType
import app.privatemoney.domain.model.Currency
import app.privatemoney.security.BiometricAuth
import app.privatemoney.ui.components.SectionLabel
import app.privatemoney.ui.theme.LocalFinanceColors

private val StartTypes = listOf(AccountType.BANK, AccountType.CASH, AccountType.UPI_WALLET, AccountType.SAVINGS)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel, onDone: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lockAvailable = BiometricAuth.isAvailable(context)

    var currencyCode by rememberSaveable { mutableStateOf(Currency.INR.code) }
    var name by rememberSaveable { mutableStateOf("") }
    var typeName by rememberSaveable { mutableStateOf(AccountType.BANK.name) }
    var balance by rememberSaveable { mutableStateOf("") }
    var enableLock by rememberSaveable { mutableStateOf(lockAvailable) }

    LaunchedEffect(state.done) { if (state.done) onDone() }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.privacy_headline), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.privacy_body), style = MaterialTheme.typography.bodyMedium, color = LocalFinanceColors.current.muted)

        SectionLabel(stringResource(R.string.currency))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Currency.SUPPORTED.forEach { c ->
                FilterChip(selected = c.code == currencyCode, onClick = { currencyCode = c.code }, label = { Text(c.code) })
            }
        }

        SectionLabel(stringResource(R.string.first_account))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(60) },
            label = { Text(stringResource(R.string.account_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StartTypes.forEach { t ->
                FilterChip(selected = t.name == typeName, onClick = { typeName = t.name }, label = { Text(stringResource(accountTypeLabel(t))) })
            }
        }
        OutlinedTextField(
            value = balance,
            onValueChange = { balance = it.filter { ch -> ch.isDigit() || ch == '.' }.take(16) },
            label = { Text(stringResource(R.string.starting_balance)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.app_lock), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(if (lockAvailable) R.string.app_lock_hint else R.string.app_lock_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalFinanceColors.current.muted,
                )
            }
            Switch(checked = enableLock && lockAvailable, onCheckedChange = { enableLock = it }, enabled = lockAvailable)
        }

        state.error?.let {
            Text(
                stringResource(
                    when (it) {
                        OnboardingError.NAME_REQUIRED -> R.string.err_name_required
                        OnboardingError.BALANCE_INVALID -> R.string.err_balance_invalid
                        OnboardingError.SAVE_FAILED -> R.string.err_save_failed
                    },
                ),
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = {
                val currency = Currency.fromCode(currencyCode) ?: Currency.INR
                viewModel.submit(name, AccountType.valueOf(typeName), balance, currency, enableLock && lockAvailable)
            },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(stringResource(R.string.start)) }
    }
}

fun accountTypeLabel(type: AccountType): Int = when (type) {
    AccountType.BANK -> R.string.acct_bank
    AccountType.CASH -> R.string.acct_cash
    AccountType.UPI_WALLET -> R.string.acct_upi
    AccountType.SAVINGS -> R.string.acct_savings
    AccountType.CREDIT_CARD -> R.string.acct_card
    AccountType.INVESTMENT -> R.string.acct_investment
    AccountType.LOAN -> R.string.acct_loan
    AccountType.OTHER_ASSET -> R.string.acct_other_asset
    AccountType.OTHER_LIABILITY -> R.string.acct_other_liability
}
