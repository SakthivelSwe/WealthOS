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

    companion object {
        fun zero(currency: Currency) = Money(0L, currency)

        /** Parses a plain decimal string such as "100.50". Rejects excess precision. */
        fun parse(text: String, currency: Currency): Money {
            val decimal = BigDecimal(text.trim())
            require(decimal.scale() <= currency.minorUnit || decimal.stripTrailingZeros().scale() <= currency.minorUnit) {
                "Too many decimal places"
            }
            val minor = decimal.movePointRight(currency.minorUnit).longValueExact()
            return Money(minor, currency)
        }

        fun sum(values: Iterable<Money>, currency: Currency): Money =
            values.fold(zero(currency)) { acc, m -> acc + m }
    }
}
