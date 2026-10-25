package app.privatemoney.domain.model

import java.math.BigDecimal

/** ISO 4217 currency with its number of minor-unit digits (INR=2, JPY=0). */
data class Currency(val code: String, val minorUnit: Int) {
    init {
        require(code.length == 3 && code.all { it in 'A'..'Z' }) { "Invalid currency code" }
        require(minorUnit in 0..4) { "Invalid minor unit" }
    }

    companion object {
        val INR = Currency("INR", 2)
        val USD = Currency("USD", 2)
        val EUR = Currency("EUR", 2)
        val GBP = Currency("GBP", 2)
        val JPY = Currency("JPY", 0)

        val SUPPORTED: List<Currency> = listOf(INR, USD, EUR, GBP)

        fun fromCode(code: String?): Currency? = (SUPPORTED + JPY).firstOrNull { it.code == code }
    }
}

/** Exact money value in integer minor units. Never uses Float/Double. Overflow throws. */
data class Money(val amountMinor: Long, val currency: Currency) : Comparable<Money> {

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(Math.addExact(amountMinor, other.amountMinor), currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(Math.subtractExact(amountMinor, other.amountMinor), currency)
    }

    operator fun unaryMinus(): Money = Money(Math.negateExact(amountMinor), currency)

    val isZero: Boolean get() = amountMinor == 0L
    val isNegative: Boolean get() = amountMinor < 0L
    val isPositive: Boolean get() = amountMinor > 0L

    fun coerceAtLeastZero(): Money = if (amountMinor < 0) zero(currency) else this

    /** Exact decimal representation for display only. */
    fun toDecimal(): BigDecimal = BigDecimal.valueOf(amountMinor, currency.minorUnit)

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return amountMinor.compareTo(other.amountMinor)
    }

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) { "Currency mismatch" }
    }

    /**
     * Formats this money using Indian digit grouping (e.g. ₹1,23,970).
     */
    fun formatIndian(showFraction: Boolean = false): String {
        return formatIndian(amountMinor, currency, showFraction)
    }

    companion object {
        fun zero(currency: Currency) = Money(0L, currency)

        /**
         * Non-negative integer HALF_UP rounding: (2 * num + den) / (2 * den).
         * Used across all allocation, split, percentage, SIP, debt and EMI calculations.
         */
        fun roundHalfUp(num: Long, den: Long): Long {
            require(num >= 0L) { "Numerator must be non-negative: $num" }
            require(den > 0L) { "Denominator must be positive: $den" }
            return (2L * num + den) / (2L * den)
        }

        /** Parses a plain decimal string such as "100.50". Rejects excess precision. */
        fun parse(text: String, currency: Currency): Money {
            val decimal = BigDecimal(text.trim())
            require(decimal.scale() <= currency.minorUnit || decimal.stripTrailingZeros().scale() <= currency.minorUnit) {
                "Too many decimal places"
            }
            val minor = decimal.movePointRight(currency.minorUnit).longValueExact()
            return Money(minor, currency)
        }

        /**
         * Parses an Indian monetary string (e.g. "₹ 1,23,970.50", "12,500 Dr", "(500.00)")
         * into exact integer minor units without floating-point conversion.
         */
        fun parseIndian(text: String, currency: Currency = Currency.INR): Money {
            var s = text.trim()
            if (s.isBlank() || s == "-" || s == "—") return zero(currency)

            val isNegative = (s.startsWith("(") && s.endsWith(")")) ||
                s.endsWith("Dr", ignoreCase = true) ||
                s.startsWith("-")

            val clean = s.replace(Regex("[^0-9.]"), "")
            if (clean.isBlank()) return zero(currency)

            val parts = clean.split(".")
            val intPart = parts[0].toLongOrNull() ?: 0L
            val fracPart = if (parts.size > 1 && currency.minorUnit > 0) {
                val fracRaw = parts[1].take(currency.minorUnit).padEnd(currency.minorUnit, '0')
                fracRaw.toLongOrNull() ?: 0L
            } else {
                0L
            }

            var multiplier = 1L
            for (i in 0 until currency.minorUnit) multiplier *= 10L
            val totalMinor = Math.addExact(Math.multiplyExact(intPart, multiplier), fracPart)
            return Money(if (isNegative) Math.negateExact(totalMinor) else totalMinor, currency)
        }

        /**
         * Formats minor units into Indian digit grouped format: ₹1,23,970 (or ₹1,23,970.50).
         */
        fun formatIndian(amountMinor: Long, currency: Currency = Currency.INR, showFraction: Boolean = false): String {
            val isNeg = amountMinor < 0
            val absMinor = if (isNeg) -amountMinor else amountMinor
            var divisor = 1L
            for (i in 0 until currency.minorUnit) divisor *= 10L

            val whole = if (currency.minorUnit > 0) absMinor / divisor else absMinor
            val fraction = if (currency.minorUnit > 0) absMinor % divisor else 0L

            val wholeStr = whole.toString()
            val grouped = if (wholeStr.length <= 3) {
                wholeStr
            } else {
                val last3 = wholeStr.takeLast(3)
                val remaining = wholeStr.dropLast(3)
                val chunks = mutableListOf<String>()
                var r = remaining
                while (r.length > 2) {
                    chunks.add(0, r.takeLast(2))
                    r = r.dropLast(2)
                }
                if (r.isNotEmpty()) chunks.add(0, r)
                chunks.joinToString(",") + "," + last3
            }

            val symbol = if (currency == Currency.INR) "₹" else "${currency.code} "
            val fracStr = if (showFraction && currency.minorUnit > 0) {
                "." + fraction.toString().padStart(currency.minorUnit, '0')
            } else {
                ""
            }
            return "${if (isNeg) "-" else ""}$symbol$grouped$fracStr"
        }

        fun sum(values: Iterable<Money>, currency: Currency): Money =
            values.fold(zero(currency)) { acc, m -> acc + m }

        /**
         * Calculates integer percentage using explicit rounding (HALF_UP).
         * Safely handles zero/negative denominator, no income/spend, and negative balances.
         */
        fun calculatePercentage(numeratorMinor: Long, denominatorMinor: Long): Int {
            if (denominatorMinor <= 0L || numeratorMinor <= 0L) return 0
            return roundHalfUp(numeratorMinor * 100L, denominatorMinor).toInt()
        }
    }
}
