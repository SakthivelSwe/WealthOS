package app.privatemoney.work

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.privatemoney.App
import app.privatemoney.MainActivity
import app.privatemoney.data.TransactionDetails
import app.privatemoney.domain.ledger.TransactionDraft
import app.privatemoney.domain.ledger.TransactionType
import app.privatemoney.domain.model.Currency
import app.privatemoney.domain.planning.RecurrenceEngine
import app.privatemoney.domain.planning.RecurrenceFrequency
import app.privatemoney.notification.NotificationHub
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class RecurringWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as App
        val db = app.database
        val ledger = app.ledger
        val settings = app.settings

        val today = LocalDate.now()
        val todayStr = today.toString()
        val dueTxns = db.recurringTxnDao().observeActive().first().filter { it.nextRunDate <= todayStr }

        var autoPostedCount = 0
        var reminderCount = 0

        for (recurring in dueTxns) {
            val freq = RecurrenceFrequency.fromString(recurring.frequency)
            val currentRunDate = try {
                LocalDate.parse(recurring.nextRunDate)
            } catch (_: Exception) {
                today
            }
            val anchorDay = currentRunDate.dayOfMonth

            if (recurring.autoPost) {
                // Auto-post mode: record deterministic bookkeeping in local ledger
                val deterministicTxnId = RecurrenceEngine.occurrenceKey(recurring.id, currentRunDate)
                val existing = ledger.get(deterministicTxnId)

                if (existing == null) {
                    val type = try {
                        TransactionType.valueOf(recurring.type)
                    } catch (e: Exception) {
                        TransactionType.EXPENSE
                    }

                    val category = recurring.categoryId?.let { catId ->
                        db.categoryDao().observeActive().first().find { it.id == catId }
                    }

                    val draft = TransactionDraft(
                        id = deterministicTxnId,
                        type = type,
                        amountMinor = recurring.amountMinor,
                        currency = Currency.fromCode(recurring.currency) ?: Currency.INR,
                        accountId = recurring.accountId,
                        toAccountId = null,
                        categoryId = recurring.categoryId,
                    )

                    val details = TransactionDetails(
                        localDate = currentRunDate.toString(),
                        localTime = "09:00:00",
                        description = recurring.description ?: "Scheduled Bill",
                        merchant = recurring.description,
                        source = "RECURRING_BILL",
                        needWant = category?.needWant,
                    )

                    try {
                        db.withTransaction {
                            ledger.record(draft, details)

                            val nextRunDate = RecurrenceEngine.nextOccurrenceDate(
                                currentDate = currentRunDate,
                                frequency = freq,
                                anchorDayOfMonth = anchorDay,
                            ).toString()

                            db.recurringTxnDao().insert(
                                recurring.copy(
                                    nextRunDate = nextRunDate,
                                    updatedAt = System.currentTimeMillis(),
                                )
                            )
                        }
                        autoPostedCount++
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    // Already posted on a prior run or manual entry: advance nextRunDate to prevent getting stuck
                    val nextRunDate = RecurrenceEngine.nextOccurrenceDate(
                        currentDate = currentRunDate,
                        frequency = freq,
                        anchorDayOfMonth = anchorDay,
                    ).toString()

                    db.recurringTxnDao().insert(
                        recurring.copy(
                            nextRunDate = nextRunDate,
                            updatedAt = System.currentTimeMillis(),
                        )
                    )
                }
            } else {
                // Reminder-only mode: do not automatically book transactions into the ledger
                NotificationHub.sendBillDueReminder(
                    context = applicationContext,
                    title = "Scheduled Bill Due",
                    description = recurring.description ?: "Scheduled Bill",
                    amountMinor = recurring.amountMinor,
                    currencyCode = recurring.currency,
                    hideSensitive = settings.hideSensitiveNotificationDetails,
                )

                try {
                    app.notifications.postNotification(
                        title = "Bill Reminder Due",
                        message = "${recurring.description ?: "Scheduled Bill"} is due today (${recurring.nextRunDate}).",
                        category = "BILL",
                        actionType = NotificationHub.ACTION_VIEW_PLANS,
                    )
                } catch (_: Exception) {}

                // Advance nextRunDate to the next scheduled interval
                val nextRunDate = RecurrenceEngine.nextOccurrenceDate(
                    currentDate = currentRunDate,
                    frequency = freq,
                    anchorDayOfMonth = anchorDay,
                ).toString()

                db.recurringTxnDao().insert(
                    recurring.copy(
                        nextRunDate = nextRunDate,
                        updatedAt = System.currentTimeMillis(),
                    )
                )
                reminderCount++
            }
        }

        if (autoPostedCount > 0) {
            sendNotification(autoPostedCount)
            try {
                app.notifications.postNotification(
                    title = "Scheduled Bills Recorded",
                    message = "Auto-posted $autoPostedCount due recurring bill(s) to ledger.",
                    category = "BILL",
                    actionType = NotificationHub.ACTION_VIEW_PLANS,
                )
            } catch (_: Exception) {}
        }

        return Result.success()
    }

    private fun sendNotification(count: Int) {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra(NotificationHub.EXTRA_ACTION, NotificationHub.ACTION_VIEW_PLANS)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 105, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val publicNotification = NotificationCompat.Builder(applicationContext, NotificationHub.CHANNEL_BILLS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Scheduled Bills Processed")
            .setContentText("Recorded due bill(s) in your ledger.")
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val notification = NotificationCompat.Builder(applicationContext, NotificationHub.CHANNEL_BILLS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Scheduled Bills Processed")
            .setContentText("Successfully recorded $count due bill(s) in your ledger.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(1001, notification)
    }
}
