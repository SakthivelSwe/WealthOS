package app.privatemoney.domain

import app.privatemoney.domain.model.Currency
import app.privatemoney.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyTest {

    @Test
    fun testParseIndianStrings() {
        assertEquals(12397000L, Money.parseIndian("₹ 1,23,970").amountMinor)
        assertEquals(12397050L, Money.parseIndian("₹ 1,23,970.50").amountMinor)
        assertEquals(12397050L, Money.parseIndian("INR 1,23,970.50").amountMinor)
        assertEquals(1250000L, Money.parseIndian("12,500").amountMinor)
        assertEquals(-1250000L, Money.parseIndian("12,500 Dr").amountMinor)
        assertEquals(1250000L, Money.parseIndian("12,500 Cr").amountMinor)
        assertEquals(-50000L, Money.parseIndian("(500.00)").amountMinor)
        assertEquals(-50000L, Money.parseIndian("-500.00").amountMinor)
        assertEquals(0L, Money.parseIndian("-").amountMinor)
        assertEquals(0L, Money.parseIndian("").amountMinor)
    }

    @Test
    fun testFormatIndianStrings() {
        // ₹1,23,970 formats as ₹1,23,970
        assertEquals("₹1,23,970", Money(12397000L, Currency.INR).formatIndian())
        assertEquals("₹1,23,970.50", Money(12397050L, Currency.INR).formatIndian(showFraction = true))
        assertEquals("₹500", Money(50000L, Currency.INR).formatIndian())
        assertEquals("₹1,000", Money(100000L, Currency.INR).formatIndian())
        assertEquals("₹1,23,45,678", Money(1234567800L, Currency.INR).formatIndian())
        assertEquals("-₹500", Money(-50000L, Currency.INR).formatIndian())
    }

    @Test
    fun testRoundHalfUp() {
        assertEquals(3L, Money.roundHalfUp(5L, 2L)) // 5/2 = 2.5 -> 3
        assertEquals(2L, Money.roundHalfUp(4L, 2L)) // 4/2 = 2.0 -> 2
        assertEquals(1L, Money.roundHalfUp(1L, 2L)) // 1/2 = 0.5 -> 1
        assertEquals(0L, Money.roundHalfUp(0L, 2L)) // 0/2 = 0
    }
}
