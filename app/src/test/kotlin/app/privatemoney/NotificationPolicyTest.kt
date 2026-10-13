package app.privatemoney

import app.privatemoney.notification.NotificationHub
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPolicyTest {

    private fun isQuietHour(currentHour: Int, quietStartHour: Int = 22, quietEndHour: Int = 7): Boolean {
        return if (quietStartHour > quietEndHour) {
            currentHour >= quietStartHour || currentHour < quietEndHour
        } else {
            currentHour in quietStartHour until quietEndHour
        }
    }

    @Test
    fun quietHoursSpanningMidnightBlocksLateNightNotifications() {
        // Quiet from 22:00 (10 PM) to 07:00 (7 AM)
        assertTrue(isQuietHour(22, 22, 7))
        assertTrue(isQuietHour(23, 22, 7))
        assertTrue(isQuietHour(0, 22, 7))
        assertTrue(isQuietHour(3, 22, 7))
        assertTrue(isQuietHour(6, 22, 7))

        // Active hours
        assertFalse(isQuietHour(7, 22, 7))
        assertFalse(isQuietHour(12, 22, 7))
        assertFalse(isQuietHour(18, 22, 7))
        assertFalse(isQuietHour(21, 22, 7))
    }

    @Test
    fun daytimeQuietHoursHandledCorrectly() {
        // Quiet during focus hours: 13:00 to 17:00
        assertTrue(isQuietHour(13, 13, 17))
        assertTrue(isQuietHour(15, 13, 17))
        assertFalse(isQuietHour(12, 13, 17))
        assertFalse(isQuietHour(17, 13, 17))
    }

    @Test
    fun notificationChannelsAreDefinedWithUniqueKeys() {
        val channels = listOf(
            NotificationHub.CHANNEL_DAILY_CHECKIN,
            NotificationHub.CHANNEL_BILLS,
            NotificationHub.CHANNEL_BUDGET,
            NotificationHub.CHANNEL_SAVINGS,
            NotificationHub.CHANNEL_BACKUP,
            NotificationHub.CHANNEL_SECURITY,
        )
        // All channels must be distinct
        assertEquals(channels.size, channels.toSet().size)
    }

    @Test
    fun actionKeysAreDefinedDistinctly() {
        val actions = listOf(
            NotificationHub.ACTION_ADD_EXPENSE,
            NotificationHub.ACTION_REVIEW_DAY,
            NotificationHub.ACTION_CATCH_UP,
            NotificationHub.ACTION_VIEW_PLANS,
            NotificationHub.ACTION_VIEW_BACKUP,
        )
        assertEquals(actions.size, actions.toSet().size)
    }

    @Test
    fun nightly9PmAnd10PmAreNotBlockedByDefaultQuietHours() {
        // Default quiet hours: 23:00 (11 PM) to 07:00 (7 AM)
        val defaultQuietStart = 23
        val defaultQuietEnd = 7

        assertFalse("9:00 PM (21:00) must not be in quiet hours", isQuietHour(21, defaultQuietStart, defaultQuietEnd))
        assertFalse("10:00 PM (22:00) must not be in quiet hours", isQuietHour(22, defaultQuietStart, defaultQuietEnd))
        assertTrue("11:00 PM (23:00) should be in quiet hours", isQuietHour(23, defaultQuietStart, defaultQuietEnd))
        assertTrue("Midnight (00:00) should be in quiet hours", isQuietHour(0, defaultQuietStart, defaultQuietEnd))
    }

    @Test
    fun nightlyActionConstantsAreDistinct() {
        val actions = listOf(
            app.privatemoney.notification.NightlyReminderScheduler.ACTION_NIGHTLY_9PM,
            app.privatemoney.notification.NightlyReminderScheduler.ACTION_NIGHTLY_10PM,
        )
        assertEquals(actions.size, actions.toSet().size)
    }
}
