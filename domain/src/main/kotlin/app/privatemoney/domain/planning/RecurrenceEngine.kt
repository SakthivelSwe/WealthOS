package app.privatemoney.domain.planning

import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY;

    companion object {
        fun fromString(value: String): RecurrenceFrequency {
            return try {
                valueOf(value.trim().uppercase())
            } catch (_: Exception) {
                MONTHLY
            }
        }
    }
}

/**
 * Deterministic schedule and occurrence engine for recurring transactions and bills.
 * Guarantees idempotency, stable occurrence keys, and month-end/leap-year calendar preservation.
 */
object RecurrenceEngine {

    const val AUTO_POST_DISCLOSURE =
        "Auto-post is local bookkeeping only. WealthOS does not initiate bank transfers or credit card charges."

    /**
     * Produces a unique, deterministic occurrence ID for an instance of a recurring rule.
     * Guaranteed stable across worker retries, app restarts, and database backups.
     */
    fun occurrenceKey(ruleId: String, occurrenceDate: LocalDate): String {
        val dateCompact = occurrenceDate.format(DateTimeFormatter.BASIC_ISO_DATE)
        return "txn_recur_${ruleId}_$dateCompact"
    }

    /**
     * Computes the next scheduled occurrence date while preserving anchor day-of-month intent.
     * For example, with an anchor day of 31:
     * Jan 31 -> Feb 28 (or 29) -> Mar 31 -> Apr 30 -> May 31.
     *
     * @param currentDate The current scheduled occurrence date.
     * @param frequency The interval frequency (DAILY, WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, YEARLY).
     * @param anchorDayOfMonth The original intended day of month (e.g., 31 for end-of-month bills).
     */
    fun nextOccurrenceDate(
        currentDate: LocalDate,
        frequency: RecurrenceFrequency,
        anchorDayOfMonth: Int = currentDate.dayOfMonth
    ): LocalDate {
        val safeAnchor = if (anchorDayOfMonth in 1..31) anchorDayOfMonth else currentDate.dayOfMonth
        return when (frequency) {
            RecurrenceFrequency.DAILY -> currentDate.plusDays(1)
            RecurrenceFrequency.WEEKLY -> currentDate.plusWeeks(1)
            RecurrenceFrequency.BIWEEKLY -> currentDate.plusWeeks(2)
            RecurrenceFrequency.MONTHLY -> {
                val targetMonth = currentDate.plusMonths(1)
                val targetDay = minOf(safeAnchor, targetMonth.lengthOfMonth())
                targetMonth.withDayOfMonth(targetDay)
            }
            RecurrenceFrequency.QUARTERLY -> {
                val targetQuarter = currentDate.plusMonths(3)
                val targetDay = minOf(safeAnchor, targetQuarter.lengthOfMonth())
                targetQuarter.withDayOfMonth(targetDay)
            }
            RecurrenceFrequency.YEARLY -> {
                val targetYear = currentDate.plusYears(1)
                val targetDay = minOf(safeAnchor, targetYear.lengthOfMonth())
                targetYear.withDayOfMonth(targetDay)
            }
        }
    }

    /**
     * Returns true if the scheduled date is due on or before [asOfDate].
     */
    fun isDue(scheduledDate: LocalDate, asOfDate: LocalDate): Boolean =
        !scheduledDate.isAfter(asOfDate)

    /**
     * Calculates all due occurrences up to [asOfDate] bounded by [maxCatchUp] instances
     * to avoid runaways if a rule has an ancient start date.
     * Returns a pair of: list of due occurrence dates, and the advanced next run date.
     */
    fun getDueOccurrences(
        initialNextRunDate: LocalDate,
        frequency: RecurrenceFrequency,
        asOfDate: LocalDate,
        anchorDayOfMonth: Int = initialNextRunDate.dayOfMonth,
        maxCatchUp: Int = 24
    ): Pair<List<LocalDate>, LocalDate> {
        val occurrences = mutableListOf<LocalDate>()
        var cursor = initialNextRunDate
        var count = 0

        while (isDue(cursor, asOfDate) && count < maxCatchUp) {
            occurrences.add(cursor)
            cursor = nextOccurrenceDate(cursor, frequency, anchorDayOfMonth)
            count++
        }

        return occurrences to cursor
    }
}
