package app.privatemoney.domain

import app.privatemoney.domain.model.AmountInput
import app.privatemoney.domain.model.DefaultCategories
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogTest {
    @Test
    fun keypadBuildsAmounts() {
        var t = ""
        for (c in "12.50") t = AmountInput.append(t, c, 2)
        assertEquals("12.50", t)
    }

    @Test
    fun keypadLimitsDecimalsAndSingleDot() {
        var t = ""
        for (c in "1.234.5") t = AmountInput.append(t, c, 2)
        assertEquals("1.23", t)
    }

    @Test
    fun leadingDotBecomesZeroDot() {
        assertEquals("0.", AmountInput.append("", '.', 2))
    }

    @Test
    fun leadingZerosCollapse() {
        assertEquals("5", AmountInput.append("0", '5', 2))
        assertEquals("0", AmountInput.append("0", '0', 2))
    }

    @Test
    fun zeroDecimalCurrencyRejectsDot() {
        assertEquals("5", AmountInput.append("5", '.', 0))
    }

    @Test
    fun integerDigitsAreCapped() {
        var t = ""
        repeat(20) { t = AmountInput.append(t, '9', 2) }
        assertEquals(AmountInput.MAX_INTEGER_DIGITS, t.length)
    }

    @Test
    fun backspaceOnEmptyIsSafe() {
        assertEquals("", AmountInput.backspace(""))
    }

    @Test
    fun seededCategoriesHaveUniqueIdsAndValidParents() {
        val all = DefaultCategories.all
        assertEquals(all.size, all.map { it.id }.toSet().size)
        val ids = all.map { it.id }.toSet()
        assertTrue(all.all { it.parentId == null || it.parentId in ids })
    }
}
