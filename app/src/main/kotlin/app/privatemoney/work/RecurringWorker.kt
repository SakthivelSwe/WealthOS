package app.privatemoney.work

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.privatemoney.App
import app.privatemoney.MainActivity
import app.privatemoney.data.TransactionDetails
import app.privatemoney.domain.ledger.TransactionDraft
import app.privatemoney.domain.ledger.TransactionType
import app.privatemoney.domain.model.Currency
import app.privatemoney.notification.NotificationHub
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.UUID

class RecurringWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as App
        val db = app.database
        val ledger = app.ledger

        val today = LocalDate.now()
        val todayStr = today.toString()
        val dueTxns = db.recurringTxnDao().observeActive().first().filter { it.nextRunDate <= todayStr }

        var processedCount = 0

        for (recurring in dueTxns) {
            val type = try {
                TransactionType.valueOf(recurring.type)
            } catch (e: Exception) {
                TransactionType.EXPENSE
            }

            val category = recurring.categoryId?.let { catId ->
                db.categoryDao().observeActive().first().find { it.id == catId }
            }

            val draft = TransactionDraft(
                id = "txn_recur_" + UUID.randomUUID().toString().replace("-", ""),
                type = type,
                amountMinor = recurring.amountMinor,
                currency = Currency.fromCode(recurring.currency) ?: Currency.INR,
                accountId = recurring.accountId,
                toAccountId = null,
                categoryId = recurring.categoryId,
            )

            val details = TransactionDetails(
                localDate = todayStr,
                localTime = "09:00:00",
                description = recurring.description ?: "Scheduled Bill",
                merchant = recurring.description,
                source = "RECURRING_BILL",
                needWant = category?.needWant,
            )

            try {
                ledger.record(draft, details)

                val currentRunDate = LocalDate.parse(recurring.nextRunDate)
                val nextRunDate = when (recurring.frequency) {
                    "WEEKLY" -> currentRunDate.plusWeeks(1)
                    "MONTHLY" -> currentRunDate.plusMonths(1)
                    "YEARLY" -> currentRunDate.plusYears(1)
                    else -> currentRunDate.plusMonths(1)
                }.toString()

                db.recurringTxnDao().insert(
                    recurring.copy(
                        nextRunDate = nextRunDate,
                        updatedAt = System.currentTimeMillis(),
                    )
                )
                processedCount++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (processedCount > 0) {
            sendNotification(processedCount)
            try {
                app.notifications.postNotification(
                    title = "Scheduled Bills Recorded",
                    message = "Recorded $processedCount due recurring bill(s).",
                    category = "BILL",
                    actionType = "VIEW_PLANS",
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

        val notification = NotificationCompat.Builder(applicationContext, NotificationHub.CHANNEL_BILLS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Scheduled Bills Processed")
            .setContentText("Successfully recorded $count due bill(s) in your ledger.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(1001, notification)
    }
}
