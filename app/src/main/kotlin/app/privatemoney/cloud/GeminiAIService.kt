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
     * Supports Indian number formatting (lakhs/crores), international formats,
     * debit/credit keywords (DR/CR, salary, refund, deposit), reference numbers,
     * and flags ambiguous direction rows for manual review rather than guessing.
     */
    suspend fun extractTransactions(statementText: String): String = withContext(Dispatchers.IO) {
        val lines = statementText.lines()
        val array = JSONArray()

        // Supports YYYY-MM-DD, DD/MM/YYYY, DD-MM-YYYY, DD.MM.YYYY
        val dateRegex = Regex("""\b(\d{4}[-/.]\d{2}[-/.]\d{2}|\d{2}[-/.]\d{2}[-/.]\d{4}|\d{1,2}[-/.]\d{1,2}[-/.]\d{2,4})\b""")

        // Supports Indian numbering (e.g. 1,50,000.00), Western (e.g. 150,000.00), and unformatted
        val amountRegex = Regex(
            """(?i)(?:rs\.?|inr|\$|€|£|₹)?\s*([+-]?\(?\s*(?:\d{1,2}(?:,\d{2})+,\d{3}|\d{1,3}(?:,\d{3})+|\d+)(?:\.\d{1,2})?\s*\)?)"""
        )

        val creditKeywords = Regex("""(?i)\b(cr\.?|credit|credited|salary|refund|cashback|interest|deposit|reversal|received)\b""")
        val debitKeywords = Regex("""(?i)\b(dr\.?|debit|debited|withdrawal|withdrawn|paid|sent|purchase|pos|atm)\b""")
        val refRegex = Regex("""(?i)\b(?:upi[/:]?|neft[/:]?|imps[/:]?|utr[/:]?|ref(?:erence)?[/:]?)\s*([a-z0-9]{6,25})\b""")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            val dateMatch = dateRegex.find(trimmed) ?: continue

            // Strip the date first so the year (e.g. 2026) is never misidentified as an amount
            var nonDateText = trimmed.removeRange(dateMatch.range)

            // Extract reference first if present so long UPI/NEFT/IMPS/UTR numbers are not misidentified as amounts
            val refMatch = refRegex.find(nonDateText)
            val referenceNumber = refMatch?.groupValues?.getOrNull(1)
            if (refMatch != null) {
                nonDateText = nonDateText.removeRange(refMatch.range)
            }

            // Find candidate amounts in non-date, non-reference text
            val allAmountMatches = amountRegex.findAll(nonDateText).toList()
            if (allAmountMatches.isEmpty()) continue

            // Prioritize matches with currency symbols or decimals/commas
            val amountMatch = allAmountMatches.firstOrNull { m ->
                val v = m.value.lowercase()
                v.contains("rs") || v.contains("inr") || v.contains("$") || v.contains("€") || v.contains("£") || v.contains("₹")
            } ?: allAmountMatches.firstOrNull { m ->
                m.groupValues[1].contains(".") || m.groupValues[1].contains(",")
            } ?: allAmountMatches.last()

            val rawAmtStr = amountMatch.groupValues[1].trim()
            val isParenthesized = rawAmtStr.startsWith("(") && rawAmtStr.endsWith(")")
            val hasMinus = rawAmtStr.startsWith("-") || isParenthesized
            val hasPlus = rawAmtStr.startsWith("+")

            val cleanDigits = rawAmtStr.replace(Regex("""[^0-9.]"""), "")
            val numericVal = cleanDigits.toDoubleOrNull() ?: 0.0
            if (numericVal <= 0.0) continue

            val parts = cleanDigits.split(".")
            val integerPart = parts[0].toLongOrNull() ?: 0L
            val decimalPart = if (parts.size > 1) parts[1].padEnd(2, '0').take(2).toIntOrNull() ?: 0 else 0
            val absMinor = integerPart * 100 + decimalPart

                // Check direction context in text outside matched amount
                val contextText = trimmed.replace(amountMatch.value, " ")
                val hasCreditKeyword = creditKeywords.containsMatchIn(contextText) || hasPlus
                val hasDebitKeyword = debitKeywords.containsMatchIn(contextText) || hasMinus

                val isCredit: Boolean
                val isDebit: Boolean
                val isAmbiguous: Boolean
                val finalType: String
                val signedMinor: Long
                val signedMajor: Double

                if (hasCreditKeyword && !hasDebitKeyword) {
                    isCredit = true
                    isDebit = false
                    isAmbiguous = false
                    finalType = "INCOME"
                    signedMinor = absMinor
                    signedMajor = numericVal
                } else if (hasDebitKeyword && !hasCreditKeyword) {
                    isCredit = false
                    isDebit = true
                    isAmbiguous = false
                    finalType = "EXPENSE"
                    signedMinor = -absMinor
                    signedMajor = -numericVal
                } else {
                    // Ambiguous direction: both or neither indicator matched.
                    // Flag for manual review rather than silently guessing negative.
                    isCredit = false
                    isDebit = false
                    isAmbiguous = true
                    finalType = "AMBIGUOUS"
                    signedMinor = -absMinor
                    signedMajor = -numericVal
                }

                // Clean description
                var desc = trimmed
                    .replace(dateMatch.value, "")
                    .replace(amountMatch.value, "")
                if (refMatch != null) {
                    desc = desc.replace(refMatch.value, "")
                }
                desc = desc.replace(Regex("""\s+"""), " ").trim()

                val merchantGuess = desc
                    .replace(Regex("""(?i)\b(cr\.?|dr\.?|upi|neft|imps|pos|inr|rs\.?)\b"""), "")
                    .replace(Regex("""[^a-zA-Z0-9\s&]"""), "")
                    .trim()
                    .take(40)
                    .ifBlank { "Statement Item" }

                array.put(JSONObject().apply {
                    put("date", dateMatch.value.replace('/', '-').replace('.', '-'))
                    put("amount", signedMajor)
                    put("amountMinor", signedMinor)
                    put("type", finalType)
                    put("isCredit", isCredit)
                    put("isDebit", isDebit)
                    put("isAmbiguous", isAmbiguous)
                    put("needsReview", isAmbiguous)
                    put("merchant", merchantGuess)
                    put("reference", referenceNumber ?: JSONObject.NULL)
                    put("description", desc.ifBlank { "Statement Item" })
                })
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

