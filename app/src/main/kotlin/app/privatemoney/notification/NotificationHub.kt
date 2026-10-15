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
                NotificationChannel(CHANNEL_DAILY_CHECKIN, "Daily Money Check-in", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Evening reminders (9:00 PM & 10:00 PM) to log expenses and stay financially mindful"
                    enableVibration(true)
                    enableLights(true)
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
        val start = settings.quietHoursStartHour // e.g. 23 (11 PM)
        val end = settings.quietHoursEndHour     // e.g. 7 (7 AM)

        return if (start > end) {
            nowHour >= start || nowHour < end
        } else {
            nowHour in start until end
        }
    }

    fun sendDailyCheckIn(context: Context, settings: AppSettings) {
        if (isQuietHours(settings)) return
        sendNightly9PmCheckIn(context)
    }

    /** Nightly Check-in fired precisely at 9:00 PM. */
    fun sendNightly9PmCheckIn(context: Context) {
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
            .setContentTitle("Evening Money Check-in (9:00 PM) 🌙")
            .setContentText("Log today's cash, UPI & card expenses before you unwind.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Keep your financial clarity intact. Take 30 seconds to record what you spent today or review unlogged days."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(reviewPendingIntent)
            .addAction(android.R.drawable.ic_input_add, "Add Expense", addPendingIntent)
            .addAction(android.R.drawable.ic_menu_agenda, "Review Ledger", reviewPendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Catch Up", catchUpPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(2090, notification)
    }

    /** Nightly Financial Wrap-up fired precisely at 10:00 PM. */
    fun sendNightly10PmWrapUp(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val addIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_ACTION, ACTION_ADD_EXPENSE)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val addPendingIntent = PendingIntent.getActivity(
            context, 105, addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val reviewIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_ACTION, ACTION_REVIEW_DAY)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val reviewPendingIntent = PendingIntent.getActivity(
            context, 106, reviewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_CHECKIN)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Nightly Wealth Wrap-up (10:00 PM) 🔒")
            .setContentText("Did you record all spends today? Lock in your daily peace of mind.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Review your daily pacing and ensure all cash & bank accounts are balanced before heading to sleep."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(reviewPendingIntent)
            .addAction(android.R.drawable.ic_input_add, "Add Expense", addPendingIntent)
            .addAction(android.R.drawable.ic_menu_agenda, "Review Day", reviewPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(2100, notification)
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

        val publicNotification = NotificationCompat.Builder(context, CHANNEL_BACKUP)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("WealthOS Backup Notice")
            .setContentText("Your local encrypted vault needs attention.")
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val notification = NotificationCompat.Builder(context, CHANNEL_BACKUP)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Encrypted backup needs attention")
            .setContentText(errorMsg)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_rotate, "Open Settings", pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(2002, notification)
    }

    /**
     * Privacy-safe reminder for scheduled bills and recurring transactions.
     * When hideSensitive is enabled, monetary values and accounts are withheld on lock screens.
     */
    fun sendBillDueReminder(
        context: Context,
        title: String,
        description: String,
        amountMinor: Long,
        currencyCode: String,
        hideSensitive: Boolean,
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_ACTION, ACTION_VIEW_PLANS)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 107, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val publicNotification = NotificationCompat.Builder(context, CHANNEL_BILLS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Scheduled Bill Due")
            .setContentText("A recurring bill is due today.")
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val currency = app.privatemoney.domain.model.Currency.fromCode(currencyCode) ?: app.privatemoney.domain.model.Currency.INR
        val moneyStr = app.privatemoney.ui.MoneyFormat.format(amountMinor, currency)
        val privateText = if (hideSensitive) {
            "$description is due today. Tap to review."
        } else {
            "$description ($moneyStr) is due today. Tap to review."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_BILLS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(privateText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(description.hashCode(), notification)
    }
}
