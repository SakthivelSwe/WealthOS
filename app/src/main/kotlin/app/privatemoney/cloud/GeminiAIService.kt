package app.privatemoney.cloud

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiAIService {

    // API key is injected at build time via BuildConfig.GEMINI_API_KEY
    // Set GEMINI_API_KEY in local.properties (not committed) or as a CI/CD secret.
    private val apiKey: String = try {
        Class.forName("app.privatemoney.BuildConfig")
            .getField("GEMINI_" + "API_KEY").get(null) as? String ?: ""
    } catch (_: Exception) { "" }

    private val extractionModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
        }
    )

    private val categorizationModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey
    )

    /**
     * Extracts a list of transactions from raw bank statement text.
     */
    suspend fun extractTransactions(statementText: String): String = withContext(Dispatchers.IO) {
        val prompt = """
            You are a precise financial data extraction assistant.
            Extract all valid transactions from the following bank statement text.
            Return ONLY a JSON array of objects, where each object has:
            - date: "YYYY-MM-DD"
            - amount: double (positive for income, negative for expense)
            - merchant: string
            - reference: string (or null)
            - description: string
            
            Text:
            $statementText
        """.trimIndent()
        
        val response = extractionModel.generateContent(prompt)
        response.text ?: "[]"
    }

    /**
     * Guesses the category ID based on the transaction description and merchant.
     */
    suspend fun categorizeTransaction(merchant: String?, description: String?, defaultCategories: List<Pair<String, String>>): String? = withContext(Dispatchers.IO) {
        val categoriesList = defaultCategories.joinToString("\n") { "${it.first}: ${it.second}" }
        val prompt = """
            You are a transaction categorizer.
            Transaction Merchant: $merchant
            Transaction Description: $description
            
            Available Categories:
            $categoriesList
            
            Return ONLY the exact Category ID (the first part before the colon) that best fits this transaction.
            If unsure, return "unknown".
        """.trimIndent()
        
        val response = categorizationModel.generateContent(prompt)
        val result = response.text?.trim() ?: "unknown"
        if (result == "unknown") null else result
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
        if (apiKey.isBlank()) {
            return@withContext getFallbackAdvice(currencySymbol, weeklySpend, topCategory)
        }

        try {
            val prompt = """
                You are an elite, compassionate personal finance advisor grounded in the wisdom of timeless financial books:
                - "The Psychology of Money" (Morgan Housel)
                - "The Richest Man in Babylon" (George S. Clason)
                - "I Will Teach You to Be Rich" (Ramit Sethi)
                - "All Your Worth" (Elizabeth Warren)

                Analyze the user's spending data:
                - Spent this week: $currencySymbol${weeklySpend / 100.0}
                - Spent last week: $currencySymbol${lastWeekSpend / 100.0}
                - Top Category: $topCategory ($currencySymbol${topCategorySpend / 100.0})
                - Total Monthly Spend: $currencySymbol${totalMonthlyExpense / 100.0}
                - Monthly Income Basis: $currencySymbol${basisIncome / 100.0}

                Output exactly 2 concise, highly encouraging and practical bullet points:
                1. Spending Pace & Over-Expense Assessment (calculate if pacing is too fast and recommend daily discretionary ceiling).
                2. One timeless quote and rule from one of the books directly tailored to their numbers.
                Do not include generic fluff. Format in markdown bullet points.
            """.trimIndent()

            val response = categorizationModel.generateContent(prompt)
            response.text?.trim() ?: getFallbackAdvice(currencySymbol, weeklySpend, topCategory)
        } catch (_: Exception) {
            getFallbackAdvice(currencySymbol, weeklySpend, topCategory)
        }
    }

    fun getFallbackAdvice(currencySymbol: String, weeklySpend: Long, topCategory: String): String {
        return "• **Pacing Diagnostic**: Your primary outflow this cycle is **$topCategory**. Keep daily discretionary spending controlled to preserve monthly peace of mind.\n• **The Psychology of Money**: \"Spending money to show people how much money you have is the fastest way to have less money.\" Pay yourself first before lifestyle inflation."
    }
}
