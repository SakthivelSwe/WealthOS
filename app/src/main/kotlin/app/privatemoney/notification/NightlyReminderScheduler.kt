package app.privatemoney.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import app.privatemoney.MainActivity
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Exact, reliable nightly reminder scheduler using Android AlarmManager.
 * Bypasses OS Doze Mode and aggressive vendor battery managers (MIUI/HyperOS, Samsung, OnePlus)
 * by utilizing AlarmManager.setAlarmClock() for guaranteed 9:00 PM and 10:00 PM wakeups.
 */
object NightlyReminderScheduler {

    const val ACTION_NIGHTLY_9PM = "app.privatemoney.action.NIGHTLY_9PM"
    const val ACTION_NIGHTLY_10PM = "app.privatemoney.action.NIGHTLY_10PM"

    private const val REQUEST_CODE_9PM = 9001
    private const val REQUEST_CODE_10PM = 9002

    fun scheduleNightlyAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Exact 9:00 PM (21:00) Evening Check-in
        scheduleAlarmAt(
            context = context,
            alarmManager = alarmManager,
            targetHour = 21,
            targetMinute = 0,
            action = ACTION_NIGHTLY_9PM,
            requestCode = REQUEST_CODE_9PM,
        )

        // 2. Exact 10:00 PM (22:00) Nightly Wrap-up
        scheduleAlarmAt(
            context = context,
            alarmManager = alarmManager,
            targetHour = 22,
            targetMinute = 0,
            action = ACTION_NIGHTLY_10PM,
            requestCode = REQUEST_CODE_10PM,
        )

        Log.i("WealthOS", "Nightly alarms scheduled for 9:00 PM and 10:00 PM")
    }

    fun calculateTriggerMillis(targetHour: Int, targetMinute: Int, fromTime: LocalDateTime = LocalDateTime.now()): Long {
        var targetTime = fromTime.with(LocalTime.of(targetHour, targetMinute, 0, 0))
        if (!targetTime.isAfter(fromTime)) {
            targetTime = targetTime.plusDays(1)
        }
        return targetTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun scheduleAlarmAt(
        context: Context,
        alarmManager: AlarmManager,
        targetHour: Int,
        targetMinute: Int,
        action: String,
        requestCode: Int,
    ) {
        val triggerAtMillis = calculateTriggerMillis(targetHour, targetMinute)

        val intent = Intent(context, NightlyReminderReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode + 100,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true

        if (canExact) {
            try {
                // AlarmClockInfo guarantees high-priority wakeup in Doze Mode
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent),
                    pendingIntent,
                )
                return
            } catch (_: SecurityException) {
                // Exact alarm permission was revoked between check and call
            }
        }

        // Inexact / battery-optimized fallback when exact alarm permission is unavailable
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            }
        } catch (e: Exception) {
            Log.w("WealthOS", "Failed to schedule nightly alarm fallback", e)
        }
    }
}
