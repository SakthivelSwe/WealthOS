package app.privatemoney.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.privatemoney.App
import app.privatemoney.R
import app.privatemoney.cloud.CloudSyncService
import app.privatemoney.security.BiometricAuth
import app.privatemoney.ui.accounts.AccountsRoute
import app.privatemoney.ui.accounts.AccountsViewModel
import app.privatemoney.ui.add.AddTransactionScreen
import app.privatemoney.ui.add.AddViewModel
import app.privatemoney.ui.components.GlassCard
import app.privatemoney.ui.components.Hairline
import app.privatemoney.ui.components.SectionLabel
import app.privatemoney.ui.home.HomeRoute
import app.privatemoney.ui.home.HomeViewModel
import app.privatemoney.ui.imports.ImportScreen
import app.privatemoney.ui.insights.InsightsRoute
import app.privatemoney.ui.insights.InsightsViewModel
import app.privatemoney.ui.ledger.LedgerRoute
import app.privatemoney.ui.ledger.LedgerViewModel
import app.privatemoney.ui.onboarding.OnboardingScreen
import app.privatemoney.ui.onboarding.OnboardingViewModel
import app.privatemoney.ui.plans.PlansRoute
import app.privatemoney.ui.plans.PlansViewModel
import app.privatemoney.ui.theme.LocalFinanceColors
import app.privatemoney.ui.theme.PmosTheme
import app.privatemoney.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.LaunchedEffect
import app.privatemoney.notification.NotificationHub
import app.privatemoney.ui.cloud.CloudBackupSheet
import app.privatemoney.ui.imports.ImportViewModel
import app.privatemoney.ui.notifications.CatchUpSheet
import app.privatemoney.ui.notifications.NotificationCenterSheet

fun appViewModelFactory(app: App): ViewModelProvider.Factory = viewModelFactory {
    initializer { HomeViewModel(app.ledger, app.categories, app.settings, app.planning, app.accounts) }
    initializer { LedgerViewModel(app.ledger, app.categories) }
    initializer { AddViewModel(app.ledger, app.accounts, app.categories, app.settings) }
    initializer { OnboardingViewModel(app.accounts, app.categories, app.settings, app.lock) }
    initializer { PlansViewModel(app.ledger, app.settings, app.planning) }
    initializer { AccountsViewModel(app.accounts) }
    initializer { InsightsViewModel(app.ledger, app.categories, app.settings) }
    initializer { ImportViewModel(app) }
}

private enum class Tab(val label: Int, val icon: ImageVector) {
    HOME(R.string.tab_home, Icons.Filled.Home),
    LEDGER(R.string.tab_ledger, Icons.Filled.Receipt),
    PLANS(R.string.tab_plans, Icons.Filled.AccountBalanceWallet),
    INSIGHTS(R.string.tab_insights, Icons.Filled.PieChart),
    MORE(R.string.tab_more, Icons.Filled.MoreHoriz),
}

@Composable
fun PmosRoot(
    app: App,
    locked: Boolean,
    onUnlock: () -> Unit,
    onSecureFlagChange: () -> Unit = {},
    initialAction: String? = null,
    onActionConsumed: () -> Unit = {},
) {
    val factory = remember(app) { appViewModelFactory(app) }
    var onboarded by remember { mutableStateOf(app.settings.onboarded) }
    var currentThemeMode by remember {
        mutableStateOf(runCatching { ThemeMode.valueOf(app.settings.themeModeStr) }.getOrDefault(ThemeMode.DARK))
    }
    var isMasked by remember { mutableStateOf(app.settings.maskAmounts) }

    PmosTheme(mode = currentThemeMode, isMasked = isMasked) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when {
                locked -> ModernLockScreen(onUnlock)
                !onboarded -> OnboardingScreen(viewModel(factory = factory), onDone = { onboarded = true })
                else -> MainShell(
                    app = app,
                    factory = factory,
                    currentThemeMode = currentThemeMode,
                    onThemeChange = {
                        currentThemeMode = it
                        app.settings.themeModeStr = it.name
                    },
                    isMasked = isMasked,
                    onToggleMask = {
                        isMasked = !isMasked
                        app.settings.maskAmounts = isMasked
                    },
                    onSecureFlagChange = onSecureFlagChange,
                    initialAction = initialAction,
                    onActionConsumed = onActionConsumed,
                )
            }
        }
    }
}

@Composable
private fun ModernLockScreen(onUnlock: () -> Unit) {
    val finance = LocalFinanceColors.current
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(finance.primaryGlow),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "WealthOS Vault",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Biometric & SQLCipher AES-256 Protected",
            style = MaterialTheme.typography.bodyMedium,
            color = finance.muted,
        )

        Spacer(Modifier.height(36.dp))

        Button(
            onClick = onUnlock,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(52.dp),
        ) {
            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.unlock), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun MainShell(
    app: App,
    factory: ViewModelProvider.Factory,
    currentThemeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    isMasked: Boolean,
    onToggleMask: () -> Unit,
    onSecureFlagChange: () -> Unit = {},
    initialAction: String? = null,
    onActionConsumed: () -> Unit = {},
) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var addInitialDate by rememberSaveable { mutableStateOf<String?>(null) }
    var managingAccounts by rememberSaveable { mutableStateOf(false) }
    var importing by rememberSaveable { mutableStateOf(false) }
    var showBackupSheet by rememberSaveable { mutableStateOf(false) }
    var showNotificationCenter by rememberSaveable { mutableStateOf(false) }
    var showCatchUpSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialAction) {
        when (initialAction) {
            NotificationHub.ACTION_ADD_EXPENSE -> {
                adding = true
                onActionConsumed()
            }
            NotificationHub.ACTION_REVIEW_DAY, NotificationHub.ACTION_CATCH_UP -> {
                showCatchUpSheet = true
                onActionConsumed()
            }
            NotificationHub.ACTION_VIEW_PLANS -> {
                tab = Tab.PLANS
                onActionConsumed()
            }
            NotificationHub.ACTION_VIEW_BACKUP -> {
                showBackupSheet = true
                onActionConsumed()
            }
            else -> Unit
        }
    }

    BackHandler(enabled = adding || managingAccounts || importing) {
        adding = false
        addInitialDate = null
        managingAccounts = false
        importing = false
    }

    if (adding) {
        AddTransactionScreen(
            viewModel = viewModel(factory = factory),
            currency = app.settings.currency,
            initialDate = addInitialDate,
            onClose = {
                adding = false
                addInitialDate = null
            },
        )
        return
    }

    if (managingAccounts) {
        AccountsRoute(viewModel(factory = factory), app.settings.currency, onClose = { managingAccounts = false })
        return
    }

    if (importing) {
        ImportScreen(
            viewModel = viewModel(factory = factory),
            currency = app.settings.currency,
            onBack = { importing = false },
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            ModernFloatingNav(selected = tab, onSelect = { tab = it })
        },
        floatingActionButton = {
            if (tab != Tab.MORE) {
                ModernRadiantFab(onClick = { adding = true })
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
        ) {
            when (tab) {
                Tab.HOME -> HomeRoute(
                    viewModel = viewModel(factory = factory),
                    onOpenLedger = { tab = Tab.LEDGER },
                    onOpenPlans = { tab = Tab.PLANS },
                    onOpenInsights = { tab = Tab.INSIGHTS },
                    onOpenAccounts = { managingAccounts = true },
                    onOpenNotifications = { showNotificationCenter = true },
                    onOpenCatchUp = { showCatchUpSheet = true },
                )
                Tab.LEDGER -> LedgerRoute(viewModel(factory = factory))
                Tab.PLANS -> PlansRoute(viewModel(factory = factory))
                Tab.INSIGHTS -> InsightsRoute(viewModel(factory = factory))
                Tab.MORE -> MoreHubScreen(
                    app = app,
                    currentThemeMode = currentThemeMode,
                    onThemeChange = onThemeChange,
                    isMasked = isMasked,
                    onToggleMask = onToggleMask,
                    onManageAccounts = { managingAccounts = true },
                    onImport = { importing = true },
                    onOpenBackup = { showBackupSheet = true },
                    onOpenNotifications = { showNotificationCenter = true },
                    onOpenCatchUp = { showCatchUpSheet = true },
                    onSecureFlagChange = onSecureFlagChange,
                )
            }
        }
    }

    if (showBackupSheet) {
        CloudBackupSheet(app = app, onDismiss = { showBackupSheet = false })
    }
    if (showNotificationCenter) {
        NotificationCenterSheet(
            repository = app.notifications,
            onDismiss = { showNotificationCenter = false },
            onActionClick = { action, _ ->
                showNotificationCenter = false
                when (action) {
                    NotificationHub.ACTION_ADD_EXPENSE -> adding = true
                    NotificationHub.ACTION_REVIEW_DAY, NotificationHub.ACTION_CATCH_UP -> showCatchUpSheet = true
                    NotificationHub.ACTION_VIEW_PLANS -> tab = Tab.PLANS
                    NotificationHub.ACTION_VIEW_BACKUP -> showBackupSheet = true
                }
            },
        )
    }
    if (showCatchUpSheet) {
        CatchUpSheet(
            app = app,
            onDismiss = { showCatchUpSheet = false },
            onAddExpenseForDate = { dateStr ->
                showCatchUpSheet = false
                addInitialDate = dateStr
                adding = true
            },
        )
    }
}

/** Modern Radiant "+ Add" FAB */
@Composable
private fun ModernRadiantFab(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    ExtendedFloatingActionButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(18.dp),
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.add_fab), fontWeight = FontWeight.Bold)
    }
}

/** Modern Floating Dock Bottom Navigation with animated pill indicator. */
@Composable
private fun ModernFloatingNav(selected: Tab, onSelect: (Tab) -> Unit) {
    val finance = LocalFinanceColors.current
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, finance.glassBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { tabItem ->
                val active = tabItem == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(role = Role.Tab) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(tabItem)
                        }
                        .semantics { this.selected = active },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) finance.primaryGlow else Color.Transparent)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = tabItem.icon,
                            contentDescription = stringResource(tabItem.label),
                            tint = if (active) MaterialTheme.colorScheme.primary else finance.muted,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Text(
                        text = stringResource(tabItem.label),
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) MaterialTheme.colorScheme.primary else finance.muted,
                        ),
                    )
                }
            }
        }
    }
}

/** Settings & Sovereign Vault Hub Screen */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoreHubScreen(
    app: App,
    currentThemeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    isMasked: Boolean,
    onToggleMask: () -> Unit,
    onManageAccounts: () -> Unit,
    onImport: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenCatchUp: () -> Unit,
    onSecureFlagChange: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val available = BiometricAuth.isAvailable(context)
    var lockEnabled by remember { mutableStateOf(app.settings.lockEnabled) }
    var timeout by remember { mutableStateOf(app.settings.lockTimeoutMs) }
    val finance = LocalFinanceColors.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Vault Profile Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(finance.primaryGlow),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp),
                    )
                }

                Column(Modifier.weight(1f)) {
                    Text(app.settings.userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("WealthOS Vault • Sovereign & Offline", style = MaterialTheme.typography.bodySmall, color = finance.muted)
                }
            }
        }

        // Privacy & Theme Controls
        SectionLabel("DISPLAY & PRIVACY")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Privacy Mask Balances Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Privacy Shield", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Mask amounts with •••• when viewing in public", style = MaterialTheme.typography.bodySmall, color = finance.muted)
                    }
                    Switch(checked = isMasked, onCheckedChange = { onToggleMask() })
                }

                Hairline()

                // Theme Mode Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Theme Appearance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            ThemeMode.DARK to "Dark Obsidian",
                            ThemeMode.MIDNIGHT to "Midnight OLED",
                            ThemeMode.LIGHT to "Light Apple",
                            ThemeMode.SYSTEM to "System Default",
                        ).forEach { (mode, title) ->
                            FilterChip(
                                selected = currentThemeMode == mode,
                                onClick = { onThemeChange(mode) },
                                label = { Text(title) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                    }
                }
            }
        }

        // Data & Management Actions
        SectionLabel("FINANCIAL DATA")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsActionRow(
                    icon = Icons.Default.Wallet,
                    title = "Manage Accounts & Wallets",
                    subtitle = "Configure bank accounts, credit cards, cash, and UPI",
                    onClick = onManageAccounts,
                )

                Hairline()

                SettingsActionRow(
                    icon = Icons.Default.UploadFile,
                    title = "Import Bank Statement",
                    subtitle = "Import CSV, XLSX, or PDF bank statements safely",
                    onClick = onImport,
                )

                Hairline()

                SettingsActionRow(
                    icon = Icons.Default.CloudSync,
                    title = "Cloud Backup & Health",
                    subtitle = "PBKDF2 encrypted off-device backup and Supabase sync",
                    onClick = onOpenBackup,
                )

                Hairline()

                SettingsActionRow(
                    icon = Icons.Default.Notifications,
                    title = "Notification Center",
                    subtitle = "Activity inbox, bill reminders, and preferences",
                    onClick = onOpenNotifications,
                )

                Hairline()

                SettingsActionRow(
                    icon = Icons.Default.EventRepeat,
                    title = "Missed-Entry Catch Up",
                    subtitle = "Review and log unrecorded expenses from past days",
                    onClick = onOpenCatchUp,
                )
            }
        }

        // Security Settings
        SectionLabel("SECURITY & VAULT LOCK")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Screenshot Protection Toggle
                var blockScreenshots by remember { mutableStateOf(app.settings.blockScreenshots) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Block Screenshots", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (blockScreenshots) "Screenshots & screen recording are blocked" else "Screenshots allowed across all screens",
                            style = MaterialTheme.typography.bodySmall,
                            color = finance.muted,
                        )
                    }
                    Switch(
                        checked = blockScreenshots,
                        onCheckedChange = {
                            blockScreenshots = it
                            app.settings.blockScreenshots = it
                            onSecureFlagChange()
                        },
                    )
                }

                Hairline()

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.app_lock), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(if (available) R.string.app_lock_hint else R.string.app_lock_unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            color = finance.muted,
                        )
                    }
                    Switch(
                        checked = lockEnabled && available,
                        enabled = available,
                        onCheckedChange = {
                            lockEnabled = it
                            app.settings.lockEnabled = it
                            app.lock.refresh()
                        },
                    )
                }

                if (lockEnabled && available) {
                    Hairline()
                    Text(stringResource(R.string.lock_after), style = MaterialTheme.typography.bodyMedium, color = finance.muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            0L to R.string.lock_immediately,
                            60_000L to R.string.lock_1_min,
                            300_000L to R.string.lock_5_min,
                        ).forEach { (ms, label) ->
                            FilterChip(
                                selected = timeout == ms,
                                onClick = {
                                    timeout = ms
                                    app.settings.lockTimeoutMs = ms
                                },
                                label = { Text(stringResource(label)) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                    }
                }
            }
        }

        // About WealthOS
        SectionLabel("ABOUT WEALTHOS")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("WealthOS v0.2.0 Prime", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Encrypted with SQLCipher AES-256 and wrapped with Android Keystore. Screenshot capture is permitted by default across all screens for your records.",
                    style = MaterialTheme.typography.bodySmall,
                    color = finance.muted,
                    lineHeight = 18.sp,
                )
            }
        }

        Spacer(Modifier.height(140.dp))
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val finance = LocalFinanceColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }

        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = finance.muted)
        }

        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = finance.muted, modifier = Modifier.size(16.dp))
    }
}
