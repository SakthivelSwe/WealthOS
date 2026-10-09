package app.privatemoney.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.privatemoney.App
import java.time.LocalDate

import app.privatemoney.domain.ledger.TransactionDraft
import app.privatemoney.domain.ledger.TransactionType
import app.privatemoney.data.TransactionDetails
import app.privatemoney.domain.model.Currency
import kotlinx.coroutines.flow.first
import java.util.UUID
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class RecurringWorker(
    appContext: Context,
    workerParams: WorkerParameters
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
            val type = try { TransactionType.valueOf(recurring.type) } catch (e: Exception) { TransactionType.EXPENSE }
            
            val draft = TransactionDraft(
                id = "txn_recur_" + UUID.randomUUID().toString().replace("-", ""),
                type = type,
                amountMinor = recurring.amountMinor,
                currency = Currency.fromCode(recurring.currency) ?: Currency.INR,
                accountId = recurring.accountId,
                toAccountId = null, // Recurring transfers not fully supported yet in this simple model
                categoryId = recurring.categoryId
            )
            
            val details = TransactionDetails(
                localDate = todayStr,
                localTime = "00:00:00", // Start of day for recurring
                description = recurring.description ?: "Recurring Transaction",
                source = "RECURRING"
            )
            
            try {
                ledger.record(draft, details)
                
                // Update nextRunDate
                val currentRunDate = LocalDate.parse(recurring.nextRunDate)
                val nextRunDate = when (recurring.frequency) {
                    "WEEKLY" -> currentRunDate.plusWeeks(1)
                    "MONTHLY" -> currentRunDate.plusMonths(1)
                    "YEARLY" -> currentRunDate.plusYears(1)
                    else -> currentRunDate.plusMonths(1)
                }.toString()
                
                db.recurringTxnDao().insert(recurring.copy(nextRunDate = nextRunDate, updatedAt = System.currentTimeMillis()))
                processedCount++
            } catch (e: Exception) {
                // Ignore failure for one, continue with others
                e.printStackTrace()
            }
        }

        if (processedCount > 0) {
            sendNotification(processedCount)
        }

        return Result.success()
    }

    private fun sendNotification(count: Int) {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val channelId = "recurring_txns"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Recurring Transactions", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("Recurring Transactions Processed")
            .setContentText("Successfully recorded $count due transaction(s).")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(1001, notification)
    }
}
