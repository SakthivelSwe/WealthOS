package app.privatemoney.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.privatemoney.data.local.AccountEntity
import app.privatemoney.domain.model.AccountType
import app.privatemoney.domain.model.Currency
import app.privatemoney.domain.model.Money
import app.privatemoney.ui.MoneyFormat
import app.privatemoney.ui.components.EmptyState
import app.privatemoney.ui.components.GlassCard
import app.privatemoney.ui.components.Hairline
import app.privatemoney.ui.components.MoneyText
import app.privatemoney.ui.components.MoneyTint
import app.privatemoney.ui.components.SectionLabel
import app.privatemoney.ui.theme.LocalFinanceColors
import app.privatemoney.ui.theme.MoneyStyles

private val AccountEntity.isLiability: Boolean
    get() = runCatching { AccountType.valueOf(type).isLiability }.getOrDefault(false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsRoute(viewModel: AccountsViewModel, currency: Currency, onClose: () -> Unit) {
    val accountsWithBalances by viewModel.accountsWithBalances.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    val finance = LocalFinanceColors.current

    if (showAddDialog) {
        AddAccountScreen(
            currency = currency,
            onSave = { name, type, openingBalance, inst ->
                viewModel.addAccount(name, type, openingBalance, currency, inst)
                showAddDialog = false
            },
            onCancel = { showAddDialog = false },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Accounts & Wallets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${accountsWithBalances.size} active vaults", style = MaterialTheme.typography.bodySmall, color = finance.muted)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val totalAssets = accountsWithBalances.filter { !it.account.isLiability }.sumOf { maxOf(0L, it.currentBalanceMinor) }
        val totalLiabilities = accountsWithBalances.filter { it.account.isLiability }.sumOf { kotlin.math.abs(minOf(0L, it.currentBalanceMinor)) }

        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 140.dp),
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Portfolio Summary Card
            item {
                PortfolioSummaryCard(
                    totalAssets = totalAssets,
                    totalLiabilities = totalLiabilities,
                    currency = currency,
                )
            }

            item {
                SectionLabel("YOUR ACCOUNTS (${accountsWithBalances.size})")
            }

            if (accountsWithBalances.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            title = "No accounts configured",
                            body = "Add bank accounts, cash, or UPI wallets to track all balances.",
                            icon = Icons.Default.Wallet,
                        )
                    }
                }
            } else {
                items(accountsWithBalances, key = { it.account.id }) { item ->
                    AccountWalletCard(item = item, currency = currency)
                }
            }
        }
    }
}


/** Virtual Wallet Card aesthetic for accounts. */
@Composable
private fun AccountWalletCard(item: app.privatemoney.data.local.AccountWithBalance, currency: Currency) {
    val account = item.account
    val currentBalance = item.currentBalanceMinor
    val finance = LocalFinanceColors.current
    val acctType = runCatching { AccountType.valueOf(account.type) }.getOrDefault(AccountType.BANK)

    val isAxisBank = account.institution?.contains("Axis", ignoreCase = true) == true || account.name.contains("Axis", ignoreCase = true)
    val isHdfcBank = account.institution?.contains("HDFC", ignoreCase = true) == true || account.name.contains("HDFC", ignoreCase = true)
    val isIciciBank = account.institution?.contains("ICICI", ignoreCase = true) == true || account.name.contains("ICICI", ignoreCase = true)
    val isSbi = account.institution?.contains("SBI", ignoreCase = true) == true || account.name.contains("SBI", ignoreCase = true) || account.institution?.contains("State Bank", ignoreCase = true) == true
    val isKotak = account.institution?.contains("Kotak", ignoreCase = true) == true || account.name.contains("Kotak", ignoreCase = true)

    val (cardBrush, icon) = when {
        isAxisBank -> Brush.linearGradient(listOf(Color(0xFF5A0C2D), Color(0xFF260513))) to Icons.Default.AccountBalance
        isHdfcBank -> Brush.linearGradient(listOf(Color(0xFF003865), Color(0xFF061A2C))) to Icons.Default.AccountBalance
        isIciciBank -> Brush.linearGradient(listOf(Color(0xFF6B2B0A), Color(0xFF260E03))) to Icons.Default.AccountBalance
        isSbi -> Brush.linearGradient(listOf(Color(0xFF240B5C), Color(0xFF0F0429))) to Icons.Default.AccountBalance
        isKotak -> Brush.linearGradient(listOf(Color(0xFF610B0E), Color(0xFF290406))) to Icons.Default.AccountBalance
        acctType == AccountType.BANK -> Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))) to Icons.Default.AccountBalance
        acctType == AccountType.UPI_WALLET -> Brush.linearGradient(listOf(Color(0xFF3B0764), Color(0xFF1E1B4B))) to Icons.Default.QrCode2
        acctType == AccountType.CASH -> Brush.linearGradient(listOf(Color(0xFF064E3B), Color(0xFF022C22))) to Icons.Default.Payments
        acctType == AccountType.SAVINGS -> Brush.linearGradient(listOf(Color(0xFF134E4A), Color(0xFF042F2E))) to Icons.Default.Savings
        acctType == AccountType.CREDIT_CARD -> Brush.linearGradient(listOf(Color(0xFF4C0519), Color(0xFF1C1917))) to Icons.Default.CreditCard
        acctType == AccountType.INVESTMENT -> Brush.linearGradient(listOf(Color(0xFF451A03), Color(0xFF1C1917))) to Icons.Default.TrendingUp
        else -> Brush.linearGradient(listOf(Color(0xFF1F2937), Color(0xFF111827))) to Icons.Default.Wallet
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(cardBrush)
            .border(1.dp, finance.glassBorder, RoundedCornerShape(22.dp))
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x2EFFFFFF)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(account.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(account.type.replace('_', ' '), style = TextStyle(fontSize = 11.sp, color = Color(0xB3FFFFFF), fontWeight = FontWeight.Medium))
                            if (!account.institution.isNullOrBlank()) {
                                Text("•", style = TextStyle(fontSize = 11.sp, color = Color(0x80FFFFFF)))
                                Text(account.institution, style = TextStyle(fontSize = 11.sp, color = Color(0xEEFFFFFF), fontWeight = FontWeight.SemiBold))
                            }
                        }
                    }
                }

                // Status Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x26FFFFFF))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        if (account.isLiability) "LIABILITY" else "ASSET",
                        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (account.isLiability) Color(0xFFFDA4AF) else Color(0xFF6EE7B7)),
                    )
                }
            }

            Hairline(modifier = Modifier.padding(vertical = 2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Current Balance", style = TextStyle(fontSize = 11.sp, color = Color(0xB3FFFFFF), fontWeight = FontWeight.Medium))
                    Text(
                        "Opening: ${MoneyFormat.format(account.openingBalanceMinor, currency)}",
                        style = TextStyle(fontSize = 10.sp, color = Color(0x80FFFFFF)),
                    )
                }
                MoneyText(
                    minor = currentBalance,
                    currency = currency,
                    style = MoneyStyles.large,
                    tint = if (account.isLiability) (if (currentBalance < 0) MoneyTint.NEGATIVE else MoneyTint.POSITIVE) else (if (currentBalance >= 0) MoneyTint.POSITIVE else MoneyTint.NEGATIVE),
                )
            }
        }
    }
}


/** Portfolio summary showing Assets vs Liabilities. */
@Composable
private fun PortfolioSummaryCard(totalAssets: Long, totalLiabilities: Long, currency: Currency) {
    val finance = LocalFinanceColors.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("NET PORTFOLIO VALUE", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = finance.muted, letterSpacing = 1.sp))
            MoneyText(
                minor = totalAssets - totalLiabilities,
                currency = currency,
                style = MoneyStyles.hero,
                tint = MoneyTint.NEUTRAL,
            )

            Hairline()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total Assets", style = TextStyle(fontSize = 11.sp, color = finance.muted))
                    MoneyText(totalAssets, currency, style = MoneyStyles.row, tint = MoneyTint.POSITIVE)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Liabilities", style = TextStyle(fontSize = 11.sp, color = finance.muted))
                    MoneyText(totalLiabilities, currency, style = MoneyStyles.row, tint = MoneyTint.NEGATIVE)
                }
            }
        }
    }
}

private data class BankPreset(val name: String, val institution: String, val type: AccountType, val color: Color)

private val BANK_PRESETS = listOf(
    BankPreset("Axis Bank", "Axis Bank", AccountType.BANK, Color(0xFF97144D)),
    BankPreset("HDFC Bank", "HDFC Bank", AccountType.BANK, Color(0xFF004C8F)),
    BankPreset("ICICI Bank", "ICICI Bank", AccountType.BANK, Color(0xFFF37021)),
    BankPreset("State Bank of India", "SBI", AccountType.BANK, Color(0xFF280071)),
    BankPreset("Kotak Bank", "Kotak Mahindra Bank", AccountType.BANK, Color(0xFFED1C24)),
    BankPreset("Cash in Wallet", "Cash", AccountType.CASH, Color(0xFF10B981)),
    BankPreset("UPI Wallet", "UPI", AccountType.UPI_WALLET, Color(0xFF6366F1)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAccountScreen(
    currency: Currency,
    onSave: (String, AccountType, Long, String?) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.BANK) }
    var institution by remember { mutableStateOf("") }
    var balanceStr by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Account", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Quick Bank Presets
            Text(
                "POPULAR INSTITUTION PRESETS",
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BANK_PRESETS.forEach { preset ->
                    val isSelected = institution.equals(preset.institution, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            institution = preset.institution
                            name = preset.name
                            type = preset.type
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(preset.color))
                                Text(preset.name)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = preset.color.copy(alpha = 0.25f),
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Account Name *") },
                placeholder = { Text("e.g. Axis Bank, HDFC Main, Cash, UPI") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = institution,
                onValueChange = { institution = it },
                label = { Text("Financial Institution (Optional)") },
                placeholder = { Text("e.g. Axis Bank, HDFC, SBI") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value = type.name.replace('_', ' '),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Account Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    AccountType.entries.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t.name.replace('_', ' ')) },
                            onClick = {
                                type = t
                                expanded = false
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = balanceStr,
                onValueChange = { balanceStr = it },
                label = { Text("Opening Balance (${currency.code})") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    val balanceMinor = runCatching { Money.parse(balanceStr.ifBlank { "0" }, currency).amountMinor }.getOrDefault(0L)
                    if (name.isNotBlank()) {
                        onSave(name, type, balanceMinor, institution.ifBlank { null })
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Create Account Vault", fontWeight = FontWeight.Bold)
            }
        }
    }
}
