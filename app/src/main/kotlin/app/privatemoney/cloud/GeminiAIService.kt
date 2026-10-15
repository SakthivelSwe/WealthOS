package app.privatemoney.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * AI Service for Money Lens insights, categorized spending, and bank statement extraction.
 *
 * In accordance with docs/AI.md:
 * - Client-side GEMINI_API_KEY embedding is strictly prohibited.
 * - Server-side Gemini processing operates via authenticated Supabase Edge Functions.
 * - When offline or unauthenticated, deterministic financial rule engines provide advice.
 */
class GeminiAIService {

    /**
     * Extracts a list of transactions from raw bank statement text.
     */
    suspend fun extractTransactions(statementText: String): String = withContext(Dispatchers.IO) {
        val lines = statementText.lines()
        val array = JSONArray()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue
            val dateRegex = Regex("""\b(\d{4}-\d{2}-\d{2}|\d{2}/\d{2}/\d{4})\b""")
            val amountRegex = Regex("""(?i)(?:rs\.?|inr|\$|€)?\s*([+-]?\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)""")
            val dateMatch = dateRegex.find(trimmed)
            val amountMatch = amountRegex.find(trimmed)
            if (dateMatch != null && amountMatch != null) {
                val amtStr = amountMatch.groupValues[1].replace(",", "")
                val amt = amtStr.toDoubleOrNull() ?: 0.0
                val desc = trimmed.replace(dateMatch.value, "").replace(amountMatch.value, "").trim()
                array.put(JSONObject().apply {
                    put("date", dateMatch.value)
                    put("amount", -kotlin.math.abs(amt))
                    put("merchant", desc.take(40).ifBlank { "Statement Item" })
                    put("reference", JSONObject.NULL)
                    put("description", desc)
                })
            }
        }
        array.toString()
    }

    /**
     * Guesses the category ID based on the transaction description and merchant.
     */
    suspend fun categorizeTransaction(
        merchant: String?,
        description: String?,
        defaultCategories: List<Pair<String, String>>,
    ): String? = withContext(Dispatchers.IO) {
        val query = "${merchant.orEmpty()} ${description.orEmpty()}".lowercase()
        if (query.isBlank()) return@withContext null

        val keywordCategoryMap = mapOf(
            "grocery" to listOf("supermarket", "mart", "grocery", "blinkit", "zepto", "instamart", "bigbasket"),
            "dining" to listOf("swiggy", "zomato", "restaurant", "cafe", "coffee", "starbucks", "mcdonalds", "pizza"),
            "utilities" to listOf("electric", "water", "gas", "broadband", "wifi", "bill", "recharge", "airtel", "jio"),
            "transport" to listOf("uber", "ola", "metro", "fuel", "petrol", "diesel", "parking", "toll"),
            "shopping" to listOf("amazon", "flipkart", "myntra", "zara", "clothing", "retail"),
            "investment" to listOf("zerodha", "groww", "sip", "mutual fund", "etf", "sebi", "demat"),
        )

        for ((catKeyword, terms) in keywordCategoryMap) {
            if (terms.any { query.contains(it) }) {
                val match = defaultCategories.find { it.second.lowercase().contains(catKeyword) }
                if (match != null) return@withContext match.first
            }
        }
        null
    }

    /**
     * Synthesizes actionable financial advice and over-expense diagnostics
     * grounded in timeless principles from global financial literature.
     */
    suspend fun generateFinancialAdvice(
        currencySymbol: String,
        weeklySpend: Long,
        lastWeekSpend: Long,
        topCategory: String,
        topCategorySpend: Long,
        totalMonthlyExpense: Long,
        basisIncome: Long,
    ): String = withContext(Dispatchers.IO) {
        val weeklySpendMajor = weeklySpend / 100.0
        val lastWeekSpendMajor = lastWeekSpend / 100.0
        val topCategorySpendMajor = topCategorySpend / 100.0

        val paceDiagnosis = when {
            lastWeekSpend > 0 && weeklySpend > lastWeekSpend -> {
                val percentIncrease = (((weeklySpend - lastWeekSpend).toDouble() / lastWeekSpend) * 100).toInt()
                val dailyCeiling = maxOf(50.0, weeklySpendMajor / 7 * 0.85)
                "• **Pacing Alert**: Outflows increased by **$percentIncrease%** over last week, driven primarily by **$topCategory** ($currencySymbol${String.format(java.util.Locale.US, "%.2f", topCategorySpendMajor)}). Recommended discretionary ceiling: **$currencySymbol${String.format(java.util.Locale.US, "%.2f", dailyCeiling)}/day**."
            }
            lastWeekSpend > 0 && weeklySpend < lastWeekSpend -> {
                val percentDecrease = (((lastWeekSpend - weeklySpend).toDouble() / lastWeekSpend) * 100).toInt()
                "• **Pacing Health**: Excellent discipline—weekly spending dropped **$percentDecrease%** versus last week. Your primary category was **$topCategory** ($currencySymbol${String.format(java.util.Locale.US, "%.2f", topCategorySpendMajor)})."
            }
            else -> {
                "• **Pacing Diagnostic**: Weekly outflow is currently **$currencySymbol${String.format(java.util.Locale.US, "%.2f", weeklySpendMajor)}**, led by **$topCategory**. Keep discretionary purchases bounded to preserve peaceful cash reserves."
            }
        }

        val wisdomRule = when {
            basisIncome > 0 && totalMonthlyExpense > (basisIncome * 0.70) -> {
                "• **The Richest Man in Babylon**: *\"A part of all you earn is yours to keep.\"* Your monthly outflow is pacing high against income basis. Ensure at least 10% is prioritized to savings before other obligations."
            }
            topCategory.contains("Dining", ignoreCase = true) || topCategory.contains("Shopping", ignoreCase = true) -> {
                "• **The Psychology of Money**: *\"Spending money to show people how much money you have is the fastest way to have less money.\"* Pause 24 hours on discretionary wants to protect compounding freedom."
            }
            else -> {
                "• **I Will Teach You to Be Rich**: *\"Spend extravagantly on the things you love, and cut costs mercilessly on the things you don't.\"* Direct conscious spending where it brings lasting value."
            }
        }

        "$paceDiagnosis\n$wisdomRule"
    }

    fun getFallbackAdvice(currencySymbol: String, weeklySpend: Long, topCategory: String): String {
        return "• **Pacing Diagnostic**: Your primary outflow this cycle is **$topCategory**. Keep daily discretionary spending controlled to preserve monthly peace of mind.\n• **The Psychology of Money**: \"Spending money to show people how much money you have is the fastest way to have less money.\" Pay yourself first before lifestyle inflation."
    }
}

