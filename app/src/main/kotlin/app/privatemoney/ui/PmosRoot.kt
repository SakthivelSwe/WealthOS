package app.privatemoney.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import app.privatemoney.cloud.CloudSyncService
import app.privatemoney.data.local.AppDatabase
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.privatemoney.App
import app.privatemoney.R
import app.privatemoney.security.BiometricAuth
import app.privatemoney.ui.add.AddTransactionScreen
import app.privatemoney.ui.add.AddViewModel
import app.privatemoney.ui.components.EmptyState
import app.privatemoney.ui.components.Hairline
import app.privatemoney.ui.components.SectionLabel
import app.privatemoney.ui.home.HomeRoute
import app.privatemoney.ui.home.HomeViewModel
import app.privatemoney.ui.ledger.LedgerRoute
import app.privatemoney.ui.ledger.LedgerViewModel
import app.privatemoney.ui.onboarding.OnboardingScreen
import app.privatemoney.ui.onboarding.OnboardingViewModel
import app.privatemoney.ui.theme.LocalFinanceColors
import app.privatemoney.ui.theme.PmosTheme
import app.privatemoney.ui.imports.ImportScreen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import app.privatemoney.ui.accounts.AccountsRoute
import app.privatemoney.ui.accounts.AccountsViewModel
import app.privatemoney.ui.insights.InsightsRoute
import app.privatemoney.ui.insights.InsightsViewModel
import app.privatemoney.ui.plans.PlansRoute
import app.privatemoney.ui.plans.PlansViewModel

fun appViewModelFactory(app: App): ViewModelProvider.Factory = viewModelFactory {
    initializer { HomeViewModel(app.ledger, app.categories, app.settings) }
    initializer { LedgerViewModel(app.ledger, app.categories) }
    initializer { AddViewModel(app.ledger, app.accounts, app.categories, app.settings) }
    initializer { OnboardingViewModel(app.accounts, app.categories, app.settings, app.lock) }
    initializer { PlansViewModel(app.ledger, app.settings, app.planning) }
    initializer { AccountsViewModel(app.accounts) }
    initializer { InsightsViewModel(app.ledger, app.categories, app.settings) }
}

private enum class Tab(val label: Int, val icon: ImageVector) {
    HOME(R.string.tab_home, Icons.Filled.Home),
    LEDGER(R.string.tab_ledger, Icons.Filled.Receipt),
    PLANS(R.string.tab_plans, Icons.Filled.AccountBalanceWallet),
    INSIGHTS(R.string.tab_insights, Icons.Filled.PieChart),
    MORE(R.string.tab_more, Icons.Filled.MoreHoriz),
}

@Composable
fun PmosRoot(app: App, locked: Boolean, onUnlock: () -> Unit) {
    val factory = remember(app) { appViewModelFactory(app) }
    var onboarded by remember { mutableStateOf(app.settings.onboarded) }
    PmosTheme {
        androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when {
                locked -> LockScreen(onUnlock)
                !onboarded -> OnboardingScreen(viewModel(factory = factory), onDone = { onboarded = true })
                else -> MainShell(app, factory)
            }
        }
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.locked_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.locked_body), color = LocalFinanceColors.current.muted)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onUnlock) { Text(stringResource(R.string.unlock)) }
    }
}

@Composable
private fun MainShell(app: App, factory: ViewModelProvider.Factory) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var managingAccounts by rememberSaveable { mutableStateOf(false) }
    var importing by rememberSaveable { mutableStateOf(false) }
    
    BackHandler(enabled = adding || managingAccounts || importing) {
        adding = false
        managingAccounts = false
        importing = false
    }

    if (adding) {
        AddTransactionScreen(viewModel(factory = factory), app.settings.currency, onClose = { adding = false })
        return
    }
    
    if (managingAccounts) {
        AccountsRoute(viewModel(factory = factory), app.settings.currency, onClose = { managingAccounts = false })
        return
    }

    if (importing) {
        ImportScreen(onBack = { importing = false })
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { BottomNav(tab) { tab = it } },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) { Text(stringResource(R.string.add_fab)) }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).statusBarsPadding()) {
            when (tab) {
                Tab.HOME -> HomeRoute(viewModel(factory = factory), onOpenLedger = { tab = Tab.LEDGER })
                Tab.LEDGER -> LedgerRoute(viewModel(factory = factory))
                Tab.PLANS -> PlansRoute(viewModel(factory = factory))
                Tab.INSIGHTS -> InsightsRoute(viewModel(factory = factory))
                Tab.MORE -> MoreScreen(app, onManageAccounts = { managingAccounts = true }, onImport = { importing = true })
            }
        }
    }
}

@Composable
private fun NotYetAvailable(title: Int) {
    Column(Modifier.padding(20.dp)) {
        EmptyState(stringResource(title), stringResource(R.string.not_yet_available))
    }
}

@Composable
private fun BottomNav(selected: Tab, onSelect: (Tab) -> Unit) {
    val finance = LocalFinanceColors.current
    Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
        Hairline()
        Row(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Tab.entries.forEach { tab ->
                val active = tab == selected
                Column(
                    Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clickable(role = Role.Tab) { onSelect(tab) }
                        .semantics { this.selected = active },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.height(2.dp).fillMaxWidth().background(
                            if (active) MaterialTheme.colorScheme.onBackground else androidx.compose.ui.graphics.Color.Transparent,
                        ),
                    )
                    Spacer(Modifier.height(8.dp))
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = stringResource(tab.label),
                        tint = if (active) MaterialTheme.colorScheme.onBackground else finance.muted,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        stringResource(tab.label),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) MaterialTheme.colorScheme.onBackground else finance.muted,
                    )
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoreScreen(app: App, onManageAccounts: () -> Unit, onImport: () -> Unit) {
    val context = LocalContext.current
    val available = BiometricAuth.isAvailable(context)
    var lockEnabled by remember { mutableStateOf(app.settings.lockEnabled) }
    var timeout by remember { mutableStateOf(app.settings.lockTimeoutMs) }
    val muted = LocalFinanceColors.current.muted

    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionLabel("General")
        Button(onClick = onManageAccounts, modifier = Modifier.fillMaxWidth()) {
            Text("Manage Accounts")
        }
        Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
            Text("Import Statement")
        }

        SectionLabel("Cloud Services")
        Button(onClick = {
            android.widget.Toast.makeText(context, "Cloud Backup is starting...", android.widget.Toast.LENGTH_SHORT).show()
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val db = AppDatabase.getDatabase(context)
                    CloudSyncService(context, db).backupDatabase("0000")
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Backup successful!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Backup failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Backup to Cloud")
        }

        Hairline()
        SectionLabel(stringResource(R.string.security))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.app_lock), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(if (available) R.string.app_lock_hint else R.string.app_lock_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
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
            Text(stringResource(R.string.lock_after), style = MaterialTheme.typography.bodyMedium, color = muted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0L to R.string.lock_immediately, 60_000L to R.string.lock_1_min, 300_000L to R.string.lock_5_min).forEach { (ms, label) ->
                    FilterChip(
                        selected = timeout == ms,
                        onClick = { timeout = ms; app.settings.lockTimeoutMs = ms },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
        }
        Hairline()
        SectionLabel(stringResource(R.string.about))
        Text(stringResource(R.string.about_body), style = MaterialTheme.typography.bodyMedium, color = muted)
    }
}
