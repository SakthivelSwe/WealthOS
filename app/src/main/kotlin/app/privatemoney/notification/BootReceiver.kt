package app.privatemoney.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Ensures nightly reminders remain scheduled across phone reboots,
 * app updates, and system time changes.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.i("WealthOS", "BootReceiver re-arming scheduled reminders for action: $action")
        NightlyReminderScheduler.scheduleNightlyAlarms(context)
    }
}
