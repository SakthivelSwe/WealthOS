package app.privatemoney.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.privatemoney.R
import app.privatemoney.domain.ledger.TransactionType
import app.privatemoney.domain.model.Currency
import app.privatemoney.domain.planning.SafeToSpendBreakdown
import app.privatemoney.ui.components.EmptyState
import app.privatemoney.ui.components.GlassCard
import app.privatemoney.ui.components.Hairline
import app.privatemoney.ui.components.LinearProgressModern
import app.privatemoney.ui.components.MoneyText
import app.privatemoney.ui.components.MoneyTint
import app.privatemoney.ui.components.SectionLabel
import app.privatemoney.ui.components.TransactionRow
import androidx.compose.ui.text.TextStyle
import app.privatemoney.ui.theme.LocalFinanceColors
import app.privatemoney.ui.theme.LocalPrivacyMask
import app.privatemoney.ui.theme.MoneyStyles
import java.time.LocalDate
import java.time.LocalTime

import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onOpenLedger: () -> Unit,
    onOpenPlans: (() -> Unit)? = null,
    onOpenInsights: (() -> Unit)? = null,
    onOpenAccounts: (() -> Unit)? = null,
    onOpenNotifications: (() -> Unit)? = null,
    onOpenCatchUp: (() -> Unit)? = null,
    onOpenSync: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onOpenLedger = onOpenLedger,
        onOpenPlans = onOpenPlans,
        onOpenInsights = onOpenInsights,
        onOpenAccounts = onOpenAccounts,
        onOpenNotifications = onOpenNotifications,
        onOpenCatchUp = onOpenCatchUp,
        onOpenSync = onOpenSync,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenLedger: () -> Unit,
    onOpenPlans: (() -> Unit)? = null,
    onOpenInsights: (() -> Unit)? = null,
    onOpenAccounts: (() -> Unit)? = null,
    onOpenNotifications: (() -> Unit)? = null,
    onOpenCatchUp: (() -> Unit)? = null,
    onOpenSync: (() -> Unit)? = null,
) {
    var showSafeSheet by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<TransactionType?>(null) }
    val finance = LocalFinanceColors.current

    val greeting = remember {
        val hour = LocalTime.now().hour
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Header Greeting & Security Tag
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = finance.muted,
                    )
                    Text(
                        text = "WealthOS Vault",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (onOpenNotifications != null) {
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notification Center",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    // Cloud Sync Status Pill
                    if (onOpenSync != null) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (state.isCloudLoggedIn) finance.positive.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    1.dp,
                                    if (state.isCloudLoggedIn) finance.positive.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable(onClick = onOpenSync)
                                .padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = if (state.isCloudLoggedIn) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                contentDescription = "Cloud Sync",
                                tint = if (state.isCloudLoggedIn) finance.positive else finance.muted,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = if (state.isCloudLoggedIn) "SYNCED" else "SYNC WITH WEB",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.isCloudLoggedIn) finance.positive else finance.muted,
                                    letterSpacing = 0.6.sp
                                ),
                            )
                        }
                    }

                    // Security Shield Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(finance.primaryGlow)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Encrypted Vault",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "ENCRYPTED",
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.8.sp),
                        )
                    }
                }
            }
        }

        // Daily Financial Check-in Banner
        if (onOpenCatchUp != null) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenCatchUp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(finance.primaryGlow),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventRepeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Financial Check-in",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Review unlogged days or check today's status",
                                style = MaterialTheme.typography.bodySmall,
                                color = finance.muted,
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = finance.muted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        // Hero Titanium Wealth Card
        item {
            HeroWealthCard(
                availableMinor = state.availableMinor,
                currency = state.currency,
                savedMinor = state.savedMinor,
            )
        }

        // Quick Shortcuts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickActionButton(
                    icon = Icons.Default.ReceiptLong,
                    label = "Ledger",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenLedger,
                )
                QuickActionButton(
                    icon = Icons.Default.AccountBalanceWallet,
                    label = "Plans",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenPlans?.invoke() ?: onOpenLedger() },
                )
                QuickActionButton(
                    icon = Icons.Default.PieChart,
                    label = "Insights",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenInsights?.invoke() ?: onOpenLedger() },
                )
            }
        }

        // Safe To Spend Today Card
        state.safe?.let { safe ->
            item {
                SafeToSpendCard(
                    safe = safe,
                    currency = state.currency,
                    onInfoClick = { showSafeSheet = true },
                )
            }
        }

        // Monthly Cashflow Stats (Income vs Expense vs Saved)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("THIS MONTH'S CASHFLOW")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CashflowStatCard(
                        title = "Income",
                        minor = state.incomeMinor,
                        currency = state.currency,
                        tint = MoneyTint.POSITIVE,
                        icon = Icons.Default.ArrowDownward,
                        modifier = Modifier.weight(1f),
                    )
                    CashflowStatCard(
                        title = "Expenses",
                        minor = state.expenseMinor,
                        currency = state.currency,
                        tint = MoneyTint.NEGATIVE,
                        icon = Icons.Default.ArrowUpward,
                        modifier = Modifier.weight(1f),
                    )
                    CashflowStatCard(
                        title = "Net Saved",
                        minor = state.savedMinor,
                        currency = state.currency,
                        tint = if (state.savedMinor >= 0) MoneyTint.PRIMARY else MoneyTint.NEGATIVE,
                        icon = Icons.Default.AccountBalanceWallet,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Recent Activity Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SectionLabel(stringResource(R.string.recent))
                if (state.recent.isNotEmpty()) {
                    TextButton(onClick = onOpenLedger) {
                        Text(
                            text = stringResource(R.string.see_all),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        val filteredRecent = when (selectedFilter) {
            null -> state.recent
            else -> state.recent.filter { it.type == selectedFilter }
        }

        if (filteredRecent.isEmpty() && !state.isLoading) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = stringResource(R.string.no_transactions_title),
                        body = stringResource(R.string.no_transactions_body),
                        icon = Icons.Default.ReceiptLong,
                    )
                }
            }
        } else {
            items(filteredRecent, key = { it.id }) { row ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenLedger,
                ) {
                    TransactionRow(
                        row = row,
                        onClick = onOpenLedger,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }

    val currentSafe = state.safe
    if (showSafeSheet && currentSafe != null) {
        ModalBottomSheet(
            onDismissRequest = { showSafeSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            SafeToSpendSheetContent(
                safe = currentSafe,
                currency = state.currency,
                onClose = { showSafeSheet = false },
            )
        }
    }
}

/** Luxury titanium/emerald gradient hero wealth card. */
@Composable
private fun HeroWealthCard(
    availableMinor: Long,
    currency: Currency,
    savedMinor: Long,
) {
    val finance = LocalFinanceColors.current
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF131D33),
            Color(0xFF0F1729),
            Color(0xFF0A101C),
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(gradientBrush)
            .border(1.2.dp, finance.glassBorder, RoundedCornerShape(26.dp))
            .padding(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Card Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "TOTAL LIQUID BALANCE",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = finance.muted,
                        letterSpacing = 1.2.sp,
                    ),
                )

                // Virtual chip icon & logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Text(
                        text = "LIVE",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }

            // Big Balance
            MoneyText(
                minor = availableMinor,
                currency = currency,
                style = MoneyStyles.hero,
                tint = MoneyTint.NEUTRAL,
            )

            Hairline(modifier = Modifier.padding(vertical = 2.dp))

            // Card Bottom Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "This Month:",
                        style = MaterialTheme.typography.bodySmall,
                        color = finance.muted,
                    )
                    MoneyText(
                        minor = savedMinor,
                        currency = currency,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        tint = if (savedMinor >= 0) MoneyTint.POSITIVE else MoneyTint.NEGATIVE,
                        showPlus = true,
                    )
                }

                Text(
                    text = "WEALTHOS PRIME",
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0x66FFFFFF),
                        letterSpacing = 1.sp,
                    ),
                )
            }
        }
    }
}

/** Quick shortcut button for navigation. */
@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val finance = LocalFinanceColors.current
    Surface(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(1.dp, finance.glassBorder),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

/** Modern Safe to Spend Today Card. */
@Composable
private fun SafeToSpendCard(
    safe: SafeToSpendBreakdown,
    currency: Currency,
    onInfoClick: () -> Unit,
) {
    val finance = LocalFinanceColors.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
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
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(finance.positive.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = finance.positive,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.safe_today),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Details",
                        tint = finance.muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MoneyText(
                    minor = safe.safeTodayMinor,
                    currency = currency,
                    style = MoneyStyles.large,
                    tint = MoneyTint.POSITIVE,
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = pluralStringResource(R.plurals.days_left, safe.daysRemainingInMonth, safe.daysRemainingInMonth),
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = finance.muted),
                    )
                }
            }

            // Linear Progress indicating days remaining in month
            val totalDays = (LocalDate.now().lengthOfMonth()).toFloat()
            val passedDays = totalDays - safe.daysRemainingInMonth
            LinearProgressModern(
                progress = (passedDays / totalDays).coerceIn(0f, 1f),
                brush = Brush.horizontalGradient(listOf(finance.positive, finance.info)),
                height = 5.dp,
            )
        }
    }
}

/** Cashflow metric card for Income, Expense, or Saved. */
@Composable
private fun CashflowStatCard(
    title: String,
    minor: Long,
    currency: Currency,
    tint: MoneyTint,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val finance = LocalFinanceColors.current
    GlassCard(
        modifier = modifier,
        cornerRadius = 16.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = when (tint) {
                        MoneyTint.POSITIVE -> finance.positive
                        MoneyTint.NEGATIVE -> finance.negative
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = title,
                    style = TextStyle(fontSize = 12.sp, color = finance.muted, fontWeight = FontWeight.Medium),
                )
            }
            MoneyText(
                minor = minor,
                currency = currency,
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                tint = tint,
            )
        }
    }
}

/** BottomSheet showing breakdown for Safe-To-Spend. */
@Composable
private fun SafeToSpendSheetContent(
    safe: SafeToSpendBreakdown,
    currency: Currency,
    onClose: () -> Unit,
) {
    val finance = LocalFinanceColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.safe_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.close), color = MaterialTheme.colorScheme.primary)
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BreakdownRow(stringResource(R.string.liquid_balance), safe.input.liquidBalanceMinor, currency)
                BreakdownRow(stringResource(R.string.upcoming_bills), -safe.input.upcomingMandatoryBillsMinor, currency)
                BreakdownRow(stringResource(R.string.debt_payments), -safe.input.expectedDebtPaymentsMinor, currency)
                BreakdownRow(stringResource(R.string.goal_contributions), -safe.input.plannedGoalContributionsMinor, currency)
                BreakdownRow(stringResource(R.string.card_due), -safe.input.creditCardDueMinor, currency)
                BreakdownRow(stringResource(R.string.safety_buffer), -safe.input.safetyBufferMinor, currency)
                Hairline()
                BreakdownRow(stringResource(R.string.discretionary), safe.discretionaryMinor, currency, isBold = true)
            }
        }

        Text(
            text = pluralStringResource(R.plurals.days_left, safe.daysRemainingInMonth, safe.daysRemainingInMonth),
            style = MaterialTheme.typography.bodySmall,
            color = finance.muted,
        )
        Text(
            text = stringResource(R.string.safe_note),
            style = MaterialTheme.typography.bodySmall,
            color = finance.muted,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    minor: Long,
    currency: Currency,
    isBold: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
        )
        MoneyText(
            minor = minor,
            currency = currency,
            style = MoneyStyles.row,
            tint = if (minor < 0) MoneyTint.NEGATIVE else MoneyTint.NEUTRAL,
        )
    }
}
