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
}
