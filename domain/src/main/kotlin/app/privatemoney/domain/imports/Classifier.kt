package app.privatemoney.domain.imports

enum class SpendEmotionTag {
    NEED,
    PLANNED_WANT,
    REGRET_LEAK,
    UNPLANNED_TRANSFER,
    INCOME,
    UNCATEGORISED
}

data class ClassificationResult(
    val categoryId: String,
    val jarBucket: String,
    val emotionTag: SpendEmotionTag,
    val isRecurringMandate: Boolean,
    val suggestedNote: String? = null,
)

object Classifier {

    private fun wordMatch(text: String, keywords: List<String>): Boolean {
        val lower = text.lowercase()
        return keywords.any { kw ->
            Regex("\\b${Regex.escape(kw.lowercase())}\\b").containsMatchIn(lower)
        }
    }

    // 1. Investments -> FREEDOM_SIP
    private val INVESTMENT_KEYWORDS = listOf(
        "zerodha", "groww", "kuvera", "camsonline", "kfintech", "mfu", "mutual fund",
        "uti mf", "sbi mf", "hdfc mf", "icici pru", "nippon", "mirae", "ppfas",
        "parag parikh", "coin", "etmoney", "indmoney", "smallcase", "sip"
    )

    // 2. Education -> EDUCATION
    private val EDUCATION_KEYWORDS = listOf(
        "udemy", "coursera", "edx", "scaler", "upgrad", "simplilearn",
        "book", "kindle", "school", "college", "tuition", "exam fee", "unacademy"
    )

    // 3. Loans & EMI -> NEEDS
    private val LOAN_KEYWORDS = listOf(
        "emi", "loan", "hdfc loan", "sbi loan", "bajaj finance", "home credit",
        "cred", "slice", "uni card", "stashfin", "kreditbee", "earlysalary", "zestmoney"
    )

    // 4. Subscriptions / Mandates -> PLAY
    private val SUBSCRIPTION_KEYWORDS = listOf(
        "netflix", "spotify", "youtube premium", "prime video", "hotstar", "apple.com/bill",
        "google play", "disney", "chatgpt", "openai", "gym", "cult.fit"
    )

    // 5. Essentials (Utilities, Groceries, Transport, Health) -> NEEDS
    private val UTILITY_KEYWORDS = listOf(
        "bescom", "tneb", "tangedco", "mseb", "uppcl", "electricity", "water board",
        "airtel", "jio", "vi", "act fibernet", "broadband", "cylinder", "indane", "hp gas", "bharat gas"
    )
    private val GROCERY_KEYWORDS = listOf(
        "dmart", "reliance fresh", "bigbasket", "bb daily", "blinkit", "zepto", "instamart",
        "grofers", "supermarket", "provision", "vegetables", "milk", "dairy"
    )
    private val TRANSPORT_KEYWORDS = listOf(
        "uber", "ola", "rapido", "namma yatri", "metro", "irctc", "petrol", "fuel", "hpcl", "bpcl", "iocl"
    )
    private val HEALTH_KEYWORDS = listOf(
        "apollo", "medplus", "1mg", "pharmeasy", "netmeds", "hospital", "clinic", "pharmacy", "doctor", "lab"
    )
    private val HOUSING_KEYWORDS = listOf(
        "rent", "maintenance", "house", "pg", "society"
    )

    // 6. Food Delivery / Dining -> PLAY (PLANNED_WANT)
    private val DINING_KEYWORDS = listOf(
        "swiggy", "zomato", "restaurant", "starbucks", "mcdonalds", "kfc", "dominos", "pizza", "burger", "cafe"
    )

    // 7. Shopping -> PLAY (PLANNED_WANT)
    private val SHOPPING_KEYWORDS = listOf(
        "amazon", "flipkart", "myntra", "meesho", "ajio", "nykaa", "tata cliq", "zara", "h&m"
    )

    // Income keywords
    private val SALARY_KEYWORDS = listOf(
        "salary", "payroll", "cms", "wages", "stipend"
    )
    private val REFUND_KEYWORDS = listOf(
        "refund", "cashback", "reversal"
    )
    private val INTEREST_KEYWORDS = listOf(
        "interest", "int.pd", "dividend"
    )

    fun classify(
        merchantOrNarration: String,
        amountMinor: Long,
        rail: PaymentRail,
        isCredit: Boolean,
        userOverrideRule: ClassificationResult? = null
    ): ClassificationResult {
        // Priority 1: User override
        if (userOverrideRule != null) return userOverrideRule

        val text = merchantOrNarration.trim()

        // Handle Income/Credits
        if (isCredit) {
            return when {
                wordMatch(text, SALARY_KEYWORDS) -> ClassificationResult(
                    categoryId = "cat_income_0",
                    jarBucket = "INCOME",
                    emotionTag = SpendEmotionTag.INCOME,
                    isRecurringMandate = true
                )
                wordMatch(text, REFUND_KEYWORDS) -> ClassificationResult(
                    categoryId = "cat_income_2",
                    jarBucket = "INCOME",
                    emotionTag = SpendEmotionTag.INCOME,
                    isRecurringMandate = false
                )
                wordMatch(text, INTEREST_KEYWORDS) -> ClassificationResult(
                    categoryId = "cat_income_1",
                    jarBucket = "INCOME",
                    emotionTag = SpendEmotionTag.INCOME,
                    isRecurringMandate = false
                )
                else -> ClassificationResult(
                    categoryId = "cat_income_0",
                    jarBucket = "INCOME",
                    emotionTag = SpendEmotionTag.INCOME,
                    isRecurringMandate = false
                )
            }
        }

        // Priority 2: Investments (FREEDOM_SIP)
        if (wordMatch(text, INVESTMENT_KEYWORDS)) {
            return ClassificationResult(
                categoryId = "cat_savings",
                jarBucket = "FREEDOM_SIP",
                emotionTag = SpendEmotionTag.NEED,
                isRecurringMandate = rail == PaymentRail.UPI_AUTOPAY || rail == PaymentRail.ACH_NACH
            )
        }

        // Priority 3: Education
        if (wordMatch(text, EDUCATION_KEYWORDS)) {
            return ClassificationResult(
                categoryId = "cat_education",
                jarBucket = "EDUCATION",
                emotionTag = SpendEmotionTag.NEED,
                isRecurringMandate = false
            )
        }

        // Priority 4: Loan / EMI
        if (wordMatch(text, LOAN_KEYWORDS)) {
            return ClassificationResult(
                categoryId = "cat_debt",
                jarBucket = "NEEDS",
                emotionTag = SpendEmotionTag.NEED,
                isRecurringMandate = true
            )
        }

        // Priority 5: Subscriptions & Autopay
        if (rail == PaymentRail.UPI_AUTOPAY || wordMatch(text, SUBSCRIPTION_KEYWORDS)) {
            return ClassificationResult(
                categoryId = "cat_entertainment",
                jarBucket = "PLAY",
                emotionTag = SpendEmotionTag.PLANNED_WANT,
                isRecurringMandate = true
            )
        }

        // Priority 6: Fixed & Basic Needs (Utilities, Groceries, Transport, Health, Housing)
        if (wordMatch(text, HOUSING_KEYWORDS)) {
            return ClassificationResult(
                categoryId = "cat_housing",
                jarBucket = "NEEDS",
                emotionTag = SpendEmotionTag.NEED,
                isRecurringMandate = false
            )
        }
        if (wordMatch(text, GROCERY_KEYWORDS)) {
            return ClassificationResult(
                categoryId = "cat_groceries",
                jarBucket = "NEEDS",
                emotionTag = SpendEmotionTag.NEED,
                isRecurringMandate = false
            )
        }
        if (wordMatch(text, UTILITY_KEYWORDS)) {
            return ClassificationResult(
                categoryId = "cat_utilities",
                jarBucket = "NEEDS",
                emotionTag = SpendEmotionTag.NEED,
                isRecurringMandate = false
            )
        }
        if (wordMatch(text, TRANSPORT_KEYWORDS)) {
            return ClassificationResult(
                categoryId = "cat_transport",
                jarBucket = "NEEDS",
                emotionTag = SpendEmotionTag.NEED,
                isRecurringMandate = false
            )
        }
        if (wordMatch(text, HEALTH_KEYWORDS)) {
            return ClassificationResult(
                categoryId = "cat_health",
                jarBucket = "NEEDS",
                emotionTag = SpendEmotionTag.NEED,
                isRecurringMandate = false
            )
        }

        // Priority 7: Food Delivery & Dining
        if (wordMatch(text, DINING_KEYWORDS)) {
            return ClassificationResult(
                categoryId = "cat_dining",
                jarBucket = "PLAY",
                emotionTag = SpendEmotionTag.PLANNED_WANT,
                isRecurringMandate = false
            )
        }

        // Priority 8: Shopping
        if (wordMatch(text, SHOPPING_KEYWORDS)) {
            return ClassificationResult(
                categoryId = "cat_shopping",
                jarBucket = "PLAY",
                emotionTag = SpendEmotionTag.PLANNED_WANT,
                isRecurringMandate = false
            )
        }

        // Priority 9: P2A / Person-to-Person heuristics
        if (rail == PaymentRail.UPI_P2A || rail == PaymentRail.UPI_P2P) {
            return if (amountMinor >= 500000L) { // >= ₹5,000
                ClassificationResult(
                    categoryId = "cat_general",
                    jarBucket = "NEEDS",
                    emotionTag = SpendEmotionTag.NEED,
                    isRecurringMandate = false,
                    suggestedNote = "Possible rent or family support"
                )
            } else {
                ClassificationResult(
                    categoryId = "cat_general",
                    jarBucket = "PLAY",
                    emotionTag = SpendEmotionTag.UNCATEGORISED,
                    isRecurringMandate = false,
                    suggestedNote = "P2A transfer for review"
                )
            }
        }

        // Default: PLAY with PLANNED_WANT (never auto-tag REGRET_LEAK)
        return ClassificationResult(
            categoryId = "cat_general",
            jarBucket = "PLAY",
            emotionTag = SpendEmotionTag.PLANNED_WANT,
            isRecurringMandate = false
        )
    }
}
