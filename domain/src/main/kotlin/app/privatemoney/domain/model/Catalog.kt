package app.privatemoney.domain.model

enum class AccountType(val isLiquid: Boolean, val isLiability: Boolean = false) {
    BANK(isLiquid = true),
    CASH(isLiquid = true),
    UPI_WALLET(isLiquid = true),
    SAVINGS(isLiquid = true),
    CREDIT_CARD(isLiquid = false, isLiability = true),
    INVESTMENT(isLiquid = false),
    LOAN(isLiquid = false, isLiability = true),
    OTHER_ASSET(isLiquid = false),
    OTHER_LIABILITY(isLiquid = false, isLiability = true),
}

enum class CategoryKind { EXPENSE, INCOME }

enum class NeedWant { NEED, WANT, SAVING }

/** Seed data only. Users can rename, recolour, archive and add categories. */
data class DefaultCategory(
    val id: String,
    val parentId: String?,
    val name: String,
    val kind: CategoryKind,
    val needWant: NeedWant?,
    val color: Long,
)

object DefaultCategories {
    val all: List<DefaultCategory> = buildList {
        fun group(slug: String, name: String, nw: NeedWant?, color: Long, subs: List<String> = emptyList()) {
            add(DefaultCategory("cat_$slug", null, name, CategoryKind.EXPENSE, nw, color))
            subs.forEach { sub ->
                add(DefaultCategory("cat_${slug}_${sub.lowercase().replace(' ', '_')}", "cat_$slug", sub, CategoryKind.EXPENSE, nw, color))
            }
        }
        group("food", "🍔 Food", NeedWant.NEED, 0xFFC9A66B, listOf("Groceries", "Restaurant", "Snacks", "Coffee", "Delivery"))
        group("transport", "🚗 Transport", NeedWant.NEED, 0xFF7FA6D9, listOf("Fuel", "Metro", "Bus", "Taxi", "Maintenance"))
        group("shopping", "🛍️ Shopping", NeedWant.WANT, 0xFFB58BC9, listOf("Clothing", "Electronics", "Personal care", "Household"))
        group("bills", "🧾 Bills", NeedWant.NEED, 0xFFD1A85A, listOf("Rent", "Electricity", "Internet", "Mobile", "Subscriptions"))
        group("health", "❤️ Health", NeedWant.NEED, 0xFFE0736B, listOf("Medicine", "Doctor", "Fitness"))
        group("education", "📚 Education", NeedWant.NEED, 0xFF6FB7C4, listOf("Courses", "Books", "Certifications"))
        group("entertainment", "🍿 Entertainment", NeedWant.WANT, 0xFFC98B9E)
        group("travel", "✈️ Travel", NeedWant.WANT, 0xFF8BC9A6)
        group("family", "👨‍👩‍👧 Family", NeedWant.NEED, 0xFFC9B58B)
        group("investment", "📈 Investment", NeedWant.SAVING, 0xFF7FD1AE)
        group("loan_emi", "🏦 Loan / EMI", NeedWant.NEED, 0xFFA3A3AD)
        group("misc", "📦 Misc", NeedWant.WANT, 0xFF8E8E96)
        listOf("💰 Salary", "💼 Business", "📈 Interest", "🎁 Other income").forEachIndexed { i, name ->
            add(DefaultCategory("cat_income_$i", null, name, CategoryKind.INCOME, null, 0xFF7FD1AE))
        }
    }
}

/** Keypad-driven amount entry. Pure so the rules are unit-testable. */
object AmountInput {
    const val MAX_INTEGER_DIGITS = 12

    fun append(current: String, key: Char, maxDecimals: Int): String {
        if (key == '.') {
            if (maxDecimals == 0 || current.contains('.')) return current
            return if (current.isEmpty()) "0." else "$current."
        }
        if (key !in '0'..'9') return current
        val dot = current.indexOf('.')
        if (dot >= 0) {
            return if (current.length - dot - 1 >= maxDecimals) current else current + key
        }
        if (current == "0") return key.toString()
        if (current.length >= MAX_INTEGER_DIGITS) return current
        return current + key
    }

    fun backspace(current: String): String = current.dropLast(1)
}
