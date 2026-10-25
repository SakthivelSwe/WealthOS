package app.privatemoney.domain.planning

import app.privatemoney.domain.model.Money

data class ForensicsReport(
    val openingBalancePaise: Long,
    val totalCreditsPaise: Long,
    val totalDebitsPaise: Long,
    val expectedClosingPaise: Long,
    val actualClosingPaise: Long,
    val discrepancyPaise: Long,
    val isReconciled: Boolean,
    val daysElapsed: Int,
    val averageBurnPerDayPaise: Long, // floor(debits / daysElapsed)
    val first7DaysSpendPaise: Long,
    val sevenDayVelocityPct: Int,
    val isVelocityAlertTriggered: Boolean, // alert above 50%
)

enum class CrisisBucket(val label: String) {
    FIXED_BILLS_RENT_EMI("Fixed Bills, Rent & EMI"),
    FOOD_TEA_SNACKS("Food, Tea & Snacks"),
    SHOPPING_SUBSCRIPTIONS("Shopping & Subscriptions"),
    TRANSFERS_FRIENDS_FAMILY("Transfers to Friends & Family")
}

data class CrisisBucketSummary(
    val bucket: CrisisBucket,
    val label: String,
    val totalPaise: Long,
    val percentage: Int,
    val count: Int
)

data class MerchantSpendSummary(
    val merchant: String,
    val totalPaise: Long,
    val count: Int
)

data class MandateSummary(
    val name: String,
    val amountPaise: Long,
    val count: Int
)

data class TxnForensicItem(
    val id: String,
    val amountPaise: Long,
    val type: String, // "EXPENSE", "INCOME", "TRANSFER"
    val localDate: String, // "YYYY-MM-DD"
    val merchant: String? = null,
    val description: String? = null,
    val categoryName: String? = null,
    val paymentRail: String? = null,
    val spendEmotionTag: String? = null,
    val jarBucket: String? = null,
    val isRecurringMandate: Boolean = false
)

enum class ReconciliationDiagnosis {
    BALANCED,
    MISSING_ROWS,
    DUPLICATES_LIKELY,
    UNKNOWN_DISCREPANCY
}

data class CycleForensicsResult(
    val openingBalancePaise: Long,
    val actualClosingPaise: Long,
    val expectedClosingPaise: Long,
    val discrepancyPaise: Long,
    val isReconciled: Boolean,
    val reconciliationDiagnosis: ReconciliationDiagnosis,
    val totalCreditsPaise: Long,
    val totalDebitsPaise: Long,
    val daysElapsed: Int,
    val averageBurnPerDayPaise: Long,
    val first7DaysSpendPaise: Long,
    val sevenDaySalarySpendPct: Int,
    val sevenDayDiscretionaryBurnPct: Int,
    val isVelocityAlertTriggered: Boolean,
    val smallScanCount: Int,
    val smallScanTotalPaise: Long,
    val topMerchants: List<MerchantSpendSummary>,
    val mandates: List<MandateSummary>,
    val crisisBuckets: Map<CrisisBucket, CrisisBucketSummary>
)

object ForensicsEngine {

    /**
     * Answers "where did my money go?" and verifies ledger reconciliation conforming to Spec §7 and §A.6.
     * E.g. ₹31,400 over 17 days -> 184,705 paise per day (₹1,847.05).
     */
    fun analyze(
        openingBalancePaise: Long,
        totalCreditsPaise: Long,
        totalDebitsPaise: Long,
        actualClosingPaise: Long,
        daysElapsed: Int,
        discretionaryBudgetPaise: Long = 0L,
        first7DaysSpendPaise: Long = 0L
    ): ForensicsReport {
        require(daysElapsed >= 1) { "Days elapsed must be at least 1" }

        val expectedClosing = openingBalancePaise + totalCreditsPaise - totalDebitsPaise
        val discrepancy = expectedClosing - actualClosingPaise
        val isReconciled = discrepancy == 0L

        val avgBurn = totalDebitsPaise / daysElapsed.toLong()

        val velocityPct = if (discretionaryBudgetPaise > 0L) {
            Money.calculatePercentage(first7DaysSpendPaise, discretionaryBudgetPaise)
        } else {
            0
        }

        val velocityAlert = velocityPct > 50

        return ForensicsReport(
            openingBalancePaise = openingBalancePaise,
            totalCreditsPaise = totalCreditsPaise,
            totalDebitsPaise = totalDebitsPaise,
            expectedClosingPaise = expectedClosing,
            actualClosingPaise = actualClosingPaise,
            discrepancyPaise = discrepancy,
            isReconciled = isReconciled,
            daysElapsed = daysElapsed,
            averageBurnPerDayPaise = avgBurn,
            first7DaysSpendPaise = first7DaysSpendPaise,
            sevenDayVelocityPct = velocityPct,
            isVelocityAlertTriggered = velocityAlert
        )
    }

    /**
     * Classifies a transaction into one of the 4 Crisis Buckets (§7).
     */
    fun classifyCrisisBucket(item: TxnForensicItem): CrisisBucket {
        val text = "${item.merchant.orEmpty()} ${item.description.orEmpty()} ${item.categoryName.orEmpty()}".lowercase()

        // 1. Fixed bills, rent, EMI
        if (Regex("\\b(rent|maintenance|pg|flat|deposit|emi|loan|bajaj|hdfc loan|sbi loan|cred|slice|electricity|bescom|tneb|gas|cylinder|wifi|broadband|airtel|jio|vi|act fibernet|insurance|lic)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) ||
            (item.jarBucket.equals("NEEDS", ignoreCase = true) && item.spendEmotionTag.equals("NEED", ignoreCase = true))
        ) {
            return CrisisBucket.FIXED_BILLS_RENT_EMI
        }

        // 2. Food, tea, snacks
        if (Regex("\\b(swiggy|zomato|starbucks|tea|chai|coffee|snacks|bakery|cafe|restaurant|burger|pizza|mcdonald|kfc|blinkit|zepto|instamart|dmart|bigbasket|supermarket|groceries|vegetables|milk|dairy)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            return CrisisBucket.FOOD_TEA_SNACKS
        }

        // 4. Transfers to friends and family
        if (item.type.equals("TRANSFER", ignoreCase = true) ||
            item.paymentRail.equals("UPI_P2A", ignoreCase = true) ||
            item.spendEmotionTag.equals("UNPLANNED_TRANSFER", ignoreCase = true) ||
            Regex("\\b(transfer|p2a|sent to|friend|family|mom|dad|brother|sister|wife|husband|self transfer)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
        ) {
            return CrisisBucket.TRANSFERS_FRIENDS_FAMILY
        }

        // 3. Shopping & subscriptions
        if (Regex("\\b(amazon|flipkart|myntra|meesho|ajio|nykaa|zara|h&m|netflix|spotify|youtube|prime|hotstar|apple|google play|gym|cult\\.fit|shopping)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) ||
            item.jarBucket.equals("PLAY", ignoreCase = true) ||
            item.spendEmotionTag.equals("PLANNED_WANT", ignoreCase = true) ||
            item.spendEmotionTag.equals("REGRET_LEAK", ignoreCase = true)
        ) {
            return CrisisBucket.SHOPPING_SUBSCRIPTIONS
        }

        if (item.spendEmotionTag.equals("NEED", ignoreCase = true)) {
            return CrisisBucket.FIXED_BILLS_RENT_EMI
        }
        return CrisisBucket.SHOPPING_SUBSCRIPTIONS
    }

    /**
     * Full cycle forensics analysis conforming to Spec §7.
     */
    fun analyzeCycle(
        transactions: List<TxnForensicItem>,
        cycleStartDate: String,
        salaryPaise: Long,
        openingBalancePaise: Long,
        actualClosingPaise: Long? = null,
        daysElapsed: Int = 1,
        discretionaryBudgetPaise: Long = 0L,
        smallScanThresholdPaise: Long = 20_000L
    ): CycleForensicsResult {
        val cycleTxns = transactions.filter { it.localDate >= cycleStartDate }

        var totalCredits = 0L
        var totalDebits = 0L
        var first7DaysSpend = 0L
        var smallScanCount = 0
        var smallScanTotal = 0L

        val merchantMap = mutableMapOf<String, Pair<Long, Int>>() // merchant -> (total, count)
        val mandateMap = mutableMapOf<String, Pair<Long, Int>>()
        val bucketMap = CrisisBucket.values().associateWith { Pair(0L, 0) }.toMutableMap()

        // 7 days window
        val cycleStartEpochDay = try {
            java.time.LocalDate.parse(cycleStartDate).toEpochDay()
        } catch (_: Exception) { 0L }

        for (t in cycleTxns) {
            if (t.type.equals("INCOME", ignoreCase = true)) {
                totalCredits += t.amountPaise
                continue
            }

            totalDebits += t.amountPaise

            val txnEpochDay = try {
                java.time.LocalDate.parse(t.localDate).toEpochDay()
            } catch (_: Exception) { cycleStartEpochDay }

            if (txnEpochDay - cycleStartEpochDay in 0..6) {
                first7DaysSpend += t.amountPaise
            }

            if (t.type.equals("EXPENSE", ignoreCase = true) && t.amountPaise <= smallScanThresholdPaise) {
                smallScanCount++
                smallScanTotal += t.amountPaise
            }

            val merchantName = (t.merchant?.ifBlank { null } ?: t.description?.ifBlank { null } ?: "Other Spend").trim()
            val currMerch = merchantMap.getOrDefault(merchantName, Pair(0L, 0))
            merchantMap[merchantName] = Pair(currMerch.first + t.amountPaise, currMerch.second + 1)

            if (t.isRecurringMandate) {
                val currMandate = mandateMap.getOrDefault(merchantName, Pair(0L, 0))
                mandateMap[merchantName] = Pair(currMandate.first + t.amountPaise, currMandate.second + 1)
            }

            val bucket = classifyCrisisBucket(t)
            val currB = bucketMap.getValue(bucket)
            bucketMap[bucket] = Pair(currB.first + t.amountPaise, currB.second + 1)
        }

        val expectedClosing = openingBalancePaise + totalCredits - totalDebits
        val closing = actualClosingPaise ?: expectedClosing
        val discrepancy = expectedClosing - closing
        val isReconciled = discrepancy == 0L

        val diagnosis = when {
            isReconciled -> ReconciliationDiagnosis.BALANCED
            discrepancy > 0 -> ReconciliationDiagnosis.MISSING_ROWS
            else -> ReconciliationDiagnosis.DUPLICATES_LIKELY
        }

        val burnPerDay = if (daysElapsed >= 1) totalDebits / daysElapsed.toLong() else totalDebits

        val salarySpendPct = if (salaryPaise > 0L) Money.calculatePercentage(first7DaysSpend, salaryPaise) else 0
        val discretionaryBurnPct = if (discretionaryBudgetPaise > 0L) Money.calculatePercentage(first7DaysSpend, discretionaryBudgetPaise) else 0
        val velocityAlert = discretionaryBurnPct > 50 || salarySpendPct > 50

        val topMerchants = merchantMap.entries
            .map { MerchantSpendSummary(it.key, it.value.first, it.value.second) }
            .sortedByDescending { it.totalPaise }
            .take(10)

        val mandates = mandateMap.entries
            .map { MandateSummary(it.key, it.value.first, it.value.second) }
            .sortedByDescending { it.amountPaise }

        val bucketSummaries = bucketMap.mapValues { (bucket, pair) ->
            CrisisBucketSummary(
                bucket = bucket,
                label = bucket.label,
                totalPaise = pair.first,
                percentage = if (totalDebits > 0L) Money.calculatePercentage(pair.first, totalDebits) else 0,
                count = pair.second
            )
        }

        return CycleForensicsResult(
            openingBalancePaise = openingBalancePaise,
            actualClosingPaise = closing,
            expectedClosingPaise = expectedClosing,
            discrepancyPaise = discrepancy,
            isReconciled = isReconciled,
            reconciliationDiagnosis = diagnosis,
            totalCreditsPaise = totalCredits,
            totalDebitsPaise = totalDebits,
            daysElapsed = daysElapsed,
            averageBurnPerDayPaise = burnPerDay,
            first7DaysSpendPaise = first7DaysSpend,
            sevenDaySalarySpendPct = salarySpendPct,
            sevenDayDiscretionaryBurnPct = discretionaryBurnPct,
            isVelocityAlertTriggered = velocityAlert,
            smallScanCount = smallScanCount,
            smallScanTotalPaise = smallScanTotal,
            topMerchants = topMerchants,
            mandates = mandates,
            crisisBuckets = bucketSummaries
        )
    }
}
