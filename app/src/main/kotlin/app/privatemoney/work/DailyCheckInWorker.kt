package app.privatemoney.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.privatemoney.App
import app.privatemoney.notification.NotificationHub
import java.time.LocalDate

class DailyCheckInWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as App
        val settings = app.settings
        if (!settings.dailyReminderEnabled) return Result.success()

        val today = LocalDate.now().toString()
        val hasTxn = app.database.ledgerDao().hasTransactionOnDate(today) != null

        if (settings.remindOnlyWhenNoTxnLogged && hasTxn) {
            // Already logged today's spending, no need to interrupt the user
            return Result.success()
        }

        NotificationHub.sendDailyCheckIn(applicationContext, settings)

        // Log in in-app notification center inbox
        try {
            app.notifications.postNotification(
                title = "Daily Money Check-in",
                message = "Have you recorded today's expenses? Keep your money tracker up to date.",
                category = "CHECK_IN",
                actionType = "ADD_EXPENSE",
            )
        } catch (_: Exception) {}

        return Result.success()
    }
}
