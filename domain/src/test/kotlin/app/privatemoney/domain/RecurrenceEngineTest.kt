package app.privatemoney.domain

import app.privatemoney.domain.planning.RecurrenceEngine
import app.privatemoney.domain.planning.RecurrenceFrequency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.time.LocalDate

class RecurrenceEngineTest {

    @Test
    fun occurrenceKeyIsStableAndDeterministic() {
        val ruleId = "bill_rent_101"
        val date = LocalDate.of(2026, 10, 15)
        val key1 = RecurrenceEngine.occurrenceKey(ruleId, date)
        val key2 = RecurrenceEngine.occurrenceKey(ruleId, date)

        assertEquals("txn_recur_bill_rent_101_20261015", key1)
        assertEquals(key1, key2)
    }

    @Test
    fun monthEndPreservesAnchorDayAcrossVaryingMonthLengths() {
        // Start on Jan 31, 2026 (non-leap year)
        val jan31 = LocalDate.of(2026, 1, 31)
        val anchor = 31

        // Feb 2026 has 28 days -> clamped to Feb 28
        val feb = RecurrenceEngine.nextOccurrenceDate(jan31, RecurrenceFrequency.MONTHLY, anchor)
        assertEquals(LocalDate.of(2026, 2, 28), feb)

        // Mar 2026 has 31 days -> restored to Mar 31!
        val mar = RecurrenceEngine.nextOccurrenceDate(feb, RecurrenceFrequency.MONTHLY, anchor)
        assertEquals(LocalDate.of(2026, 3, 31), mar)

        // Apr 2026 has 30 days -> clamped to Apr 30
        val apr = RecurrenceEngine.nextOccurrenceDate(mar, RecurrenceFrequency.MONTHLY, anchor)
        assertEquals(LocalDate.of(2026, 4, 30), apr)

        // May 2026 has 31 days -> restored to May 31!
        val may = RecurrenceEngine.nextOccurrenceDate(apr, RecurrenceFrequency.MONTHLY, anchor)
        assertEquals(LocalDate.of(2026, 5, 31), may)
    }

    @Test
    fun leapYearMonthEndHandlesFebruary29th() {
        // Start on Jan 31, 2028 (leap year)
        val jan31 = LocalDate.of(2028, 1, 31)
        val anchor = 31

        val feb29 = RecurrenceEngine.nextOccurrenceDate(jan31, RecurrenceFrequency.MONTHLY, anchor)
        assertEquals(LocalDate.of(2028, 2, 29), feb29)

        val mar31 = RecurrenceEngine.nextOccurrenceDate(feb29, RecurrenceFrequency.MONTHLY, anchor)
        assertEquals(LocalDate.of(2028, 3, 31), mar31)
    }

    @Test
    fun leapDayYearlyRecurrencePreservesFebruary29Anchor() {
        val leapDay2024 = LocalDate.of(2024, 2, 29)
        val anchor = 29

        val y2025 = RecurrenceEngine.nextOccurrenceDate(leapDay2024, RecurrenceFrequency.YEARLY, anchor)
        assertEquals(LocalDate.of(2025, 2, 28), y2025)

        val y2026 = RecurrenceEngine.nextOccurrenceDate(y2025, RecurrenceFrequency.YEARLY, anchor)
        assertEquals(LocalDate.of(2026, 2, 28), y2026)

        val y2027 = RecurrenceEngine.nextOccurrenceDate(y2026, RecurrenceFrequency.YEARLY, anchor)
        assertEquals(LocalDate.of(2027, 2, 28), y2027)

        val y2028 = RecurrenceEngine.nextOccurrenceDate(y2027, RecurrenceFrequency.YEARLY, anchor)
        assertEquals(LocalDate.of(2028, 2, 29), y2028)
    }

    @Test
    fun dailyWeeklyBiweeklyQuarterlyCalculations() {
        val start = LocalDate.of(2026, 5, 1)

        val daily = RecurrenceEngine.nextOccurrenceDate(start, RecurrenceFrequency.DAILY)
        assertEquals(LocalDate.of(2026, 5, 2), daily)

        val weekly = RecurrenceEngine.nextOccurrenceDate(start, RecurrenceFrequency.WEEKLY)
        assertEquals(LocalDate.of(2026, 5, 8), weekly)

        val biweekly = RecurrenceEngine.nextOccurrenceDate(start, RecurrenceFrequency.BIWEEKLY)
        assertEquals(LocalDate.of(2026, 5, 15), biweekly)

        val quarterly = RecurrenceEngine.nextOccurrenceDate(start, RecurrenceFrequency.QUARTERLY)
        assertEquals(LocalDate.of(2026, 8, 1), quarterly)
    }

    @Test
    fun isDueChecksBoundaryDatesCorrectly() {
        val today = LocalDate.of(2026, 10, 15)

        assertTrue(RecurrenceEngine.isDue(LocalDate.of(2026, 10, 14), today))
        assertTrue(RecurrenceEngine.isDue(LocalDate.of(2026, 10, 15), today))
        assertFalse(RecurrenceEngine.isDue(LocalDate.of(2026, 10, 16), today))
    }

    @Test
    fun getDueOccurrencesCatchesUpSafelyAndBoundsCount() {
        val initialRun = LocalDate.of(2026, 1, 15)
        val asOf = LocalDate.of(2026, 4, 15)

        val (dueList, nextDate) = RecurrenceEngine.getDueOccurrences(
            initialNextRunDate = initialRun,
            frequency = RecurrenceFrequency.MONTHLY,
            asOfDate = asOf,
            anchorDayOfMonth = 15,
        )

        // Expected due: Jan 15, Feb 15, Mar 15, Apr 15 (4 occurrences)
        assertEquals(4, dueList.size)
        assertEquals(LocalDate.of(2026, 1, 15), dueList[0])
        assertEquals(LocalDate.of(2026, 2, 15), dueList[1])
        assertEquals(LocalDate.of(2026, 3, 15), dueList[2])
        assertEquals(LocalDate.of(2026, 4, 15), dueList[3])
        assertEquals(LocalDate.of(2026, 5, 15), nextDate)
    }

    @Test
    fun autoPostDisclosureTextExplicitlyCommunicatesLocalBookkeeping() {
        assertTrue(RecurrenceEngine.AUTO_POST_DISCLOSURE.contains("local bookkeeping"))
        assertTrue(RecurrenceEngine.AUTO_POST_DISCLOSURE.contains("does not initiate bank transfers"))
    }
}
