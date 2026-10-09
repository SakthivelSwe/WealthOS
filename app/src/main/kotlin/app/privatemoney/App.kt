package app.privatemoney

import android.app.Application
import app.privatemoney.data.AccountRepository
import app.privatemoney.data.AppSettings
import app.privatemoney.data.CategoryRepository
import app.privatemoney.data.LedgerRepository
import app.privatemoney.data.local.AppDatabase
import app.privatemoney.data.local.DatabaseFactory
import app.privatemoney.security.AppLock
import app.privatemoney.security.DatabaseKeyProvider
import app.privatemoney.work.RecurringWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Manual dependency graph: small enough that a DI framework would only add weight. */
class App : Application() {
    val settings: AppSettings by lazy { AppSettings(this) }
    val lock: AppLock by lazy { AppLock(settings) }
    val keyProvider: DatabaseKeyProvider by lazy { DatabaseKeyProvider(this) }
    val database: AppDatabase by lazy { DatabaseFactory.create(this, keyProvider) }
    val ledger: LedgerRepository by lazy { LedgerRepository(database) }
    val accounts: AccountRepository by lazy { AccountRepository(database) }
    val categories: CategoryRepository by lazy { CategoryRepository(database) }
    val planning: app.privatemoney.data.PlanningRepository by lazy { app.privatemoney.data.PlanningRepository(database) }

    override fun onCreate() {
        super.onCreate()
        scheduleRecurringWorker()
    }

    private fun scheduleRecurringWorker() {
        val workRequest = PeriodicWorkRequestBuilder<RecurringWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "recurring_txn_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
