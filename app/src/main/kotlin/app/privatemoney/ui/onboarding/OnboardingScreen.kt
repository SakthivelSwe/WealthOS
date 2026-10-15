package app.privatemoney.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.vector.ImageVector
import app.privatemoney.ui.MoneyFormat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.privatemoney.R
import app.privatemoney.domain.model.AccountType
import app.privatemoney.domain.model.Currency
import app.privatemoney.security.BiometricAuth
import app.privatemoney.ui.components.GlassCard
import app.privatemoney.ui.components.Hairline
import app.privatemoney.ui.components.SectionLabel
import app.privatemoney.ui.theme.LocalFinanceColors

private val StartTypes = listOf(AccountType.BANK, AccountType.CASH, AccountType.UPI_WALLET, AccountType.SAVINGS)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel, onDone: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lockAvailable = BiometricAuth.isAvailable(context)
    val finance = LocalFinanceColors.current

    var currencyCode by rememberSaveable { mutableStateOf(Currency.INR.code) }
    var name by rememberSaveable { mutableStateOf("") }
    var typeName by rememberSaveable { mutableStateOf(AccountType.BANK.name) }
    var balance by rememberSaveable { mutableStateOf("") }
    var enableLock by rememberSaveable { mutableStateOf(lockAvailable) }

    LaunchedEffect(state.done) { if (state.done) onDone() }

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        // Hero Emblem & Welcome
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF00E599), Color(0xFF38BDF8), Color(0xFFF59E0B)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp),
                )
            }

            Text(
                text = "Welcome to WealthOS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Your sovereign, zero-knowledge personal finance operating system. Encrypted entirely on-device.",
                style = MaterialTheme.typography.bodyMedium,
                color = finance.muted,
                lineHeight = 22.sp,
            )
        }

        // Trust Pillars
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TrustPill("Encrypted", Icons.Default.Lock, Modifier.weight(1f))
            TrustPill("Offline 1st", Icons.Default.WifiOff, Modifier.weight(1f))
            TrustPill("Biometric", Icons.Default.Fingerprint, Modifier.weight(1f))
        }

        // Currency Selector
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(stringResource(R.string.currency))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Currency.SUPPORTED.forEach { c ->
                    val isSelected = c.code == currencyCode
                    FilterChip(
                        selected = isSelected,
                        onClick = { currencyCode = c.code },
                        label = { Text("${MoneyFormat.symbol(c)} ${c.code}") },
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        }

        // First Account Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionLabel("SETUP YOUR PRIMARY ACCOUNT")

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text(stringResource(R.string.account_name)) },
                    placeholder = { Text("e.g. Axis Bank, HDFC Salary, Cash in Wallet") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StartTypes.forEach { t ->
                        val isSelected = t.name == typeName
                        FilterChip(
                            selected = isSelected,
                            onClick = { typeName = t.name },
                            label = { Text(stringResource(accountTypeLabel(t))) },
                            shape = RoundedCornerShape(14.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }

                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it.filter { ch -> ch.isDigit() || ch == '.' }.take(16) },
                    label = { Text(stringResource(R.string.starting_balance)) },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Biometrics Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.app_lock), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(if (lockAvailable) R.string.app_lock_hint else R.string.app_lock_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = finance.muted,
                    )
                }
                Switch(checked = enableLock && lockAvailable, onCheckedChange = { enableLock = it }, enabled = lockAvailable)
            }
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
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // CTA Button
        Button(
            onClick = {
                val currency = Currency.fromCode(currencyCode) ?: Currency.INR
                viewModel.submit(name, AccountType.valueOf(typeName), balance, currency, enableLock && lockAvailable)
            },
            enabled = !state.saving,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
        ) {
            Text(
                "Initialize Vault & Get Started",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TrustPill(title: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val finance = LocalFinanceColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, finance.glassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(title, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = finance.muted))
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
