package app.privatemoney.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.privatemoney.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered at exactly 9:00 PM and 10:00 PM every night.
 * Fires high-importance heads-up notifications and records inbox entries.
 * Automatically re-arms alarms for the following day.
 */
class NightlyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.i("WealthOS", "NightlyReminderReceiver triggered: $action")

        val app = context.applicationContext as? App

        when (action) {
            NightlyReminderScheduler.ACTION_NIGHTLY_9PM -> {
                NotificationHub.sendNightly9PmCheckIn(context)

                app?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            it.notifications.postNotification(
                                title = "Evening Money Check-in (9:00 PM)",
                                message = "Take a minute to log your dinner, transport, or UPI payments today. Keep your ledger clear and accurate!",
                                category = "CHECK_IN",
                                actionType = NotificationHub.ACTION_ADD_EXPENSE,
                            )
                        } catch (_: Exception) {}
                    }
                }
            }
            NightlyReminderScheduler.ACTION_NIGHTLY_10PM -> {
                NotificationHub.sendNightly10PmWrapUp(context)

                app?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            it.notifications.postNotification(
                                title = "Nightly Wealth Wrap-up (10:00 PM)",
                                message = "Review your daily pacing and make sure today's transactions are balanced before sleep.",
                                category = "CHECK_IN",
                                actionType = NotificationHub.ACTION_REVIEW_DAY,
                            )
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        // Re-arm alarms for tomorrow
        NightlyReminderScheduler.scheduleNightlyAlarms(context)
    }
}
