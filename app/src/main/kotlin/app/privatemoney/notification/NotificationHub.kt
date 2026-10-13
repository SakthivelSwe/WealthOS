package app.privatemoney.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.privatemoney.MainActivity
import app.privatemoney.R
import app.privatemoney.data.AppSettings
import java.time.LocalTime

object NotificationHub {

    const val CHANNEL_DAILY_CHECKIN = "daily_checkin"
    const val CHANNEL_BILLS = "bills_payments"
    const val CHANNEL_BUDGET = "budget_alerts"
    const val CHANNEL_SAVINGS = "savings_goals"
    const val CHANNEL_BACKUP = "backup_sync"
    const val CHANNEL_SECURITY = "security_vault"

    const val EXTRA_ACTION = "extra_notification_action"
    const val ACTION_ADD_EXPENSE = "action_add_expense"
    const val ACTION_REVIEW_DAY = "action_review_day"
    const val ACTION_VIEW_PLANS = "action_view_plans"
    const val ACTION_VIEW_BACKUP = "action_view_backup"
    const val ACTION_CATCH_UP = "action_catch_up"

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channels = listOf(
                NotificationChannel(CHANNEL_DAILY_CHECKIN, "Daily Money Check-in", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Evening reminders to log expenses and stay financially mindful"
                },
                NotificationChannel(CHANNEL_BILLS, "Bills & Payments", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Upcoming subscription, EMI, and credit card due dates"
                },
                NotificationChannel(CHANNEL_BUDGET, "Budget & Spending Alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Alerts when reaching 80% or 100% of budget limits"
                },
                NotificationChannel(CHANNEL_SAVINGS, "Savings Goals & Chit Funds", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Progress updates and milestones on your savings goals"
                },
                NotificationChannel(CHANNEL_BACKUP, "Cloud Backup & Sync", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Sync status and attention notices for your encrypted backups"
                },
                NotificationChannel(CHANNEL_SECURITY, "Security & Vault Lock", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "App lock and security notifications"
                },
            )

            channels.forEach { manager.createNotificationChannel(it) }
        }
    }

    fun isQuietHours(settings: AppSettings): Boolean {
        if (!settings.quietHoursEnabled) return false
        val nowHour = LocalTime.now().hour
        val start = settings.quietHoursStartHour // e.g. 22 (10 PM)
        val end = settings.quietHoursEndHour     // e.g. 7 (7 AM)

        return if (start > end) {
            nowHour >= start || nowHour < end
        } else {
            nowHour in start until end
        }
    }

    fun sendDailyCheckIn(context: Context, settings: AppSettings) {
        if (isQuietHours(settings)) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val addIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_ACTION, ACTION_ADD_EXPENSE)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val addPendingIntent = PendingIntent.getActivity(
            context, 101, addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val reviewIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_ACTION, ACTION_REVIEW_DAY)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val reviewPendingIntent = PendingIntent.getActivity(
            context, 102, reviewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val catchUpIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_ACTION, ACTION_CATCH_UP)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val catchUpPendingIntent = PendingIntent.getActivity(
            context, 103, catchUpIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_CHECKIN)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Your daily money check-in")
            .setContentText("Have you recorded today's expenses? Take a minute to keep your tracker up to date.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Keep your financial clarity intact. Record what you spent today or review unlogged days."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(reviewPendingIntent)
            .addAction(android.R.drawable.ic_input_add, "Add Expense", addPendingIntent)
            .addAction(android.R.drawable.ic_menu_agenda, "Review Today", reviewPendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Catch Up", catchUpPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(2001, notification)
    }

    fun sendBackupAlert(context: Context, errorMsg: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_ACTION, ACTION_VIEW_BACKUP)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 104, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_BACKUP)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Encrypted backup needs attention")
            .setContentText(errorMsg)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_rotate, "Open Settings", pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(2002, notification)
    }
}
