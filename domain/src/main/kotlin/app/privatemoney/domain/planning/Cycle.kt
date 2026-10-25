package app.privatemoney.domain.planning

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class IncomeScheduleType {
    VARIABLE_WINDOW,
    FIXED_DATE,
    IRREGULAR
}

data class CycleDefinition(
    val scheduleType: IncomeScheduleType,
    val paydayWindowStartDay: Int = 2,
    val paydayWindowEndDay: Int = 5,
    val fixedPaydayOfMonth: Int = 1,
    val irregularHorizonDays: Int = 30,
) {
    init {
        require(paydayWindowStartDay in 1..31) { "Window start day must be in 1..31" }
        require(paydayWindowEndDay in paydayWindowStartDay..31) { "Window end day must be >= start day" }
        require(fixedPaydayOfMonth in 1..31) { "Fixed payday must be in 1..31" }
        require(irregularHorizonDays >= 1) { "Irregular horizon must be at least 1 day" }
    }
}

object CycleEngine {

    /**
     * Checks if a given date falls within the payday salary credit window.
     */
    fun isWithinPaydayWindow(date: LocalDate, definition: CycleDefinition): Boolean {
        return when (definition.scheduleType) {
            IncomeScheduleType.VARIABLE_WINDOW -> {
                date.dayOfMonth in definition.paydayWindowStartDay..definition.paydayWindowEndDay
            }
            IncomeScheduleType.FIXED_DATE -> {
                date.dayOfMonth == definition.fixedPaydayOfMonth
            }
            IncomeScheduleType.IRREGULAR -> false
        }
    }

    /**
     * Computes the number of days in the cycle from cycle start to next expected payday.
     * Conforms to §2.3 and §5.4. Falls back to 30 if unknown.
     */
    fun calculateDaysInCycle(
        cycleStartDate: LocalDate,
        definition: CycleDefinition,
        explicitNextPayday: LocalDate? = null
    ): Int {
        if (explicitNextPayday != null) {
            val days = ChronoUnit.DAYS.between(cycleStartDate, explicitNextPayday).toInt()
            return if (days >= 1) days else 30
        }

        return when (definition.scheduleType) {
            IncomeScheduleType.VARIABLE_WINDOW -> {
                // Next month's window start day
                val nextMonth = cycleStartDate.plusMonths(1)
                val day = minOf(definition.paydayWindowStartDay, nextMonth.lengthOfMonth())
                val nextPayday = nextMonth.withDayOfMonth(day)
                val days = ChronoUnit.DAYS.between(cycleStartDate, nextPayday).toInt()
                if (days >= 1) days else 30
            }
            IncomeScheduleType.FIXED_DATE -> {
                val nextMonth = cycleStartDate.plusMonths(1)
                val day = minOf(definition.fixedPaydayOfMonth, nextMonth.lengthOfMonth())
                val nextPayday = nextMonth.withDayOfMonth(day)
                val days = ChronoUnit.DAYS.between(cycleStartDate, nextPayday).toInt()
                if (days >= 1) days else 30
            }
            IncomeScheduleType.IRREGULAR -> {
                definition.irregularHorizonDays
            }
        }
    }
}
