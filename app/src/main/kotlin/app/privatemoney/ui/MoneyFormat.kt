package app.privatemoney.ui

import app.privatemoney.domain.model.Currency
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/** Display formatting only. Never used for calculations. */
object MoneyFormat {
    private val indianLocale: Locale = Locale.Builder().setLanguage("en").setRegion("IN").build()

    fun symbol(currency: Currency): String = when (currency.code) {
        "INR" -> "\u20B9"
        "USD" -> "$"
        "EUR" -> "\u20AC"
        "GBP" -> "\u00A3"
        else -> currency.code + " "
    }

    fun format(minor: Long, currency: Currency, showPlus: Boolean = false): String {
        val abs = BigDecimal.valueOf(minor, currency.minorUnit).abs()
        val nf = NumberFormat.getNumberInstance(if (currency.code == "INR") indianLocale else Locale.US).apply {
            minimumFractionDigits = currency.minorUnit
            maximumFractionDigits = currency.minorUnit
        }
        val sign = when {
            minor < 0 -> "-"
            showPlus && minor > 0 -> "+"
            else -> ""
        }
        return sign + symbol(currency) + nf.format(abs)
    }
}
