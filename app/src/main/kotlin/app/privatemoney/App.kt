package app.privatemoney

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import app.privatemoney.data.AccountRepository
import app.privatemoney.data.AppSettings
import app.privatemoney.data.CategoryRepository
import app.privatemoney.data.LedgerRepository
import app.privatemoney.data.NotificationRepository
import app.privatemoney.data.PlanningRepository
import app.privatemoney.data.local.AppDatabase
import app.privatemoney.data.local.DatabaseFactory
import app.privatemoney.notification.NotificationHub
import app.privatemoney.security.AppLock
import app.privatemoney.security.DatabaseKeyProvider
import app.privatemoney.work.DailyCheckInWorker
import app.privatemoney.work.RecurringWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Manual dependency graph: clean, fast, lightweight. */
class App : Application(), androidx.work.Configuration.Provider {
    val settings: AppSettings by lazy { AppSettings(this) }
    val lock: AppLock by lazy { AppLock(settings) }
    val keyProvider: DatabaseKeyProvider by lazy { DatabaseKeyProvider(this) }
    val database: AppDatabase by lazy { DatabaseFactory.create(this, keyProvider) }
    val ledger: LedgerRepository by lazy { LedgerRepository(database) }
    val accounts: AccountRepository by lazy { AccountRepository(database) }
    val categories: CategoryRepository by lazy { CategoryRepository(database) }
    val planning: PlanningRepository by lazy { PlanningRepository(database) }
    val notifications: NotificationRepository by lazy { NotificationRepository(database) }

    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationHub.initChannels(this)
        scheduleWorkers()

        // Background data health repair & initialization
        CoroutineScope(Dispatchers.IO).launch {
            try {
                categories.seedDefaultsIfEmpty()
                database.ledgerDao().backfillNeedWant()
            } catch (_: Exception) {}
        }
    }

    private fun scheduleWorkers() {
        try {
            val workManager = WorkManager.getInstance(this)

            // 1. Recurring Bills Worker
            val recurringRequest = PeriodicWorkRequestBuilder<RecurringWorker>(1, TimeUnit.DAYS).build()
            workManager.enqueueUniquePeriodicWork(
                "recurring_txn_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                recurringRequest,
            )

            // 2. Daily Check-in Evening Worker (target 8:30 PM)
            val now = LocalDateTime.now()
            val targetTime = LocalTime.of(settings.dailyReminderHour, settings.dailyReminderMinute)
            var targetDateTime = now.with(targetTime)
            if (now.isAfter(targetDateTime)) {
                targetDateTime = targetDateTime.plusDays(1)
            }
            val initialDelay = Duration.between(now, targetDateTime).toMillis()

            val checkInRequest = PeriodicWorkRequestBuilder<DailyCheckInWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                "daily_checkin_worker",
                ExistingPeriodicWorkPolicy.UPDATE,
                checkInRequest,
            )
        } catch (e: Throwable) {
            android.util.Log.e("WealthOS", "Failed to schedule background workers", e)
        }
    }
}
