package app.privatemoney.domain.imports

import app.privatemoney.domain.ledger.TransactionType
import java.time.LocalDate

data class ParsedStatementRow(
    val id: String,
    val date: LocalDate,
    val description: String,
    val merchant: String?,
    val amountMinor: Long,
    val type: TransactionType,
    val reference: String?,
    val rawText: String,
    val balanceMinor: Long? = null,
    val paymentRail: PaymentRail = PaymentRail.NON_UPI,
    val jarBucket: String = "NEEDS",
    val emotionTag: SpendEmotionTag = SpendEmotionTag.NEED,
    val isRecurringMandate: Boolean = false,
    val isSelfTransfer: Boolean = false,
    val formatName: String = "GENERIC",
)

data class StatementParseResult(
    val rows: List<ParsedStatementRow>,
    val totalFound: Int,
    val invalidRows: Int,
    val detectedDelimiter: Char,
    val headers: List<String>,
    val formatName: String = "GENERIC",
    val totalDebitsMinor: Long = 0L,
    val totalCreditsMinor: Long = 0L,
    val smallScansTotalMinor: Long = 0L,
    val smallScansCount: Int = 0,
    val detectedMandatesCount: Int = 0,
    val openingBalanceMinor: Long? = null,
    val closingBalanceMinor: Long? = null,
    val isReconciled: Boolean = false,
)

object StatementParser {

    private const val DEFAULT_SMALL_SCAN_THRESHOLD_PAISE = 20000L // ₹200

    fun parseCsv(
        text: String,
        limits: ImportLimits = ImportLimits(),
        smallScanThresholdPaise: Long = DEFAULT_SMALL_SCAN_THRESHOLD_PAISE
    ): StatementParseResult {
        val detectedDelim = CsvReader.detectDelimiter(text)
        val rawRows = CsvReader.read(text, detectedDelim, limits)
        if (rawRows.isEmpty()) {
            return StatementParseResult(emptyList(), 0, 0, detectedDelim, emptyList())
        }

        val mapping = ColumnMapper.discoverMapping(rawRows)
        val headerIndex = mapping?.headerRowIndex ?: 0
        val headers = rawRows.getOrNull(headerIndex) ?: emptyList()

        val dateCol = mapping?.dateCol ?: 0
        val descCol = mapping?.descriptionCol ?: 1
        val debitCol = mapping?.debitCol ?: -1
        val creditCol = mapping?.creditCol ?: -1
        val amountCol = mapping?.amountCol ?: -1
        val indicatorCol = mapping?.indicatorCol ?: -1
        val balanceCol = mapping?.balanceCol ?: -1
        val refCol = mapping?.referenceCol ?: -1

        val parsed = mutableListOf<ParsedStatementRow>()
        var invalid = 0
        val runningBalances = mutableListOf<Long>()

        for (i in (headerIndex + 1) until rawRows.size) {
            val row = rawRows[i]
            if (row.all { it.isBlank() }) continue

            try {
                val dateStr = row.getOrNull(dateCol)?.trim().orEmpty()
                val desc = row.getOrNull(descCol)?.trim().orEmpty()
                val date = ColumnMapper.parseDate(dateStr)
                if (date == null || desc.isBlank()) {
                    invalid++
                    continue
                }

                var amountMinor = 0L
                var type = TransactionType.EXPENSE

                // 1. Split debit/credit columns
                if (debitCol >= 0 && creditCol >= 0) {
                    val debitStr = row.getOrNull(debitCol)?.trim().orEmpty()
                    val creditStr = row.getOrNull(creditCol)?.trim().orEmpty()
                    val debitVal = parseAmountMinor(debitStr)
                    val creditVal = parseAmountMinor(creditStr)

                    if (debitVal > 0L) {
                        amountMinor = debitVal
                        type = TransactionType.EXPENSE
                    } else if (creditVal > 0L) {
                        amountMinor = creditVal
                        type = TransactionType.INCOME
                    }
                }

                // 2. Single amount column
                if (amountMinor == 0L && amountCol >= 0) {
                    val amtStr = row.getOrNull(amountCol)?.trim().orEmpty()
                    val indicatorStr = if (indicatorCol >= 0) row.getOrNull(indicatorCol)?.trim().orEmpty().uppercase() else ""
                    val isNegativeByFormat = amtStr.startsWith("-") || (amtStr.startsWith("(") && amtStr.endsWith(")")) || amtStr.endsWith("Dr", ignoreCase = true)
                    val isDebitIndicator = indicatorStr == "DR" || indicatorStr == "D" || indicatorStr == "DEBIT" || indicatorStr == "W"
                    val isCreditIndicator = indicatorStr == "CR" || indicatorStr == "C" || indicatorStr == "CREDIT" || indicatorStr == "DEP"

                    val parsedAmt = parseAmountMinor(amtStr)
                    amountMinor = parsedAmt

                    type = when {
                        isCreditIndicator -> TransactionType.INCOME
                        isDebitIndicator -> TransactionType.EXPENSE
                        isNegativeByFormat -> TransactionType.EXPENSE
                        else -> TransactionType.INCOME // default positive credit unless indicator says debit
                    }
                }

                if (amountMinor <= 0L) {
                    invalid++
                    continue
                }

                val balanceMinor = if (balanceCol >= 0) {
                    val bStr = row.getOrNull(balanceCol)?.trim().orEmpty()
                    if (bStr.isNotBlank() && bStr != "-" && bStr != "—") {
                        val bVal = parseAmountMinor(bStr)
                        runningBalances.add(bVal)
                        bVal
                    } else null
                } else null

                // Decode narration
                val decoded = NarrationDecoder.decode(desc)
                val explicitRef = if (refCol >= 0) row.getOrNull(refCol)?.trim()?.takeIf { it.isNotBlank() } else null
                val finalReference = explicitRef ?: decoded.referenceOrRrn

                val finalType = if (decoded.isSelfTransfer) {
                    TransactionType.TRANSFER
                } else {
                    type
                }

                // Classify
                val isCredit = finalType == TransactionType.INCOME
                val classification = Classifier.classify(
                    merchantOrNarration = decoded.cleanMerchantOrPayee,
                    amountMinor = amountMinor,
                    rail = decoded.paymentRail,
                    isCredit = isCredit
                )

                parsed.add(
                    ParsedStatementRow(
                        id = "import_row_${i}_${System.nanoTime() % 1000000}",
                        date = date,
                        description = desc,
                        merchant = decoded.cleanMerchantOrPayee,
                        amountMinor = amountMinor,
                        type = finalType,
                        reference = finalReference,
                        rawText = row.joinToString(","),
                        balanceMinor = balanceMinor,
                        paymentRail = decoded.paymentRail,
                        jarBucket = classification.jarBucket,
                        emotionTag = classification.emotionTag,
                        isRecurringMandate = decoded.isRecurringMandate || classification.isRecurringMandate,
                        isSelfTransfer = decoded.isSelfTransfer,
                        formatName = decoded.formatName
                    )
                )
            } catch (_: Exception) {
                invalid++
            }
        }

        val totalDebits = parsed.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.TRANSFER }.sumOf { it.amountMinor }
        val totalCredits = parsed.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
        val smallScans = parsed.filter { it.type == TransactionType.EXPENSE && it.amountMinor <= smallScanThresholdPaise }
        val smallScansTotal = smallScans.sumOf { it.amountMinor }
        val mandateCount = parsed.count { it.isRecurringMandate }

        // Reconciliation
        var openBal: Long? = null
        var closeBal: Long? = null
        var isReconciled = false

        if (runningBalances.size >= 2 && parsed.isNotEmpty()) {
            val first = parsed.first()
            val last = parsed.last()
            if (first.balanceMinor != null && last.balanceMinor != null) {
                if (!first.date.isAfter(last.date)) {
                    // Chronological ascending
                    val firstEffect = if (first.type == TransactionType.INCOME) first.amountMinor else -first.amountMinor
                    openBal = first.balanceMinor - firstEffect
                    closeBal = last.balanceMinor
                } else {
                    // Chronological descending
                    val lastEffect = if (last.type == TransactionType.INCOME) last.amountMinor else -last.amountMinor
                    openBal = last.balanceMinor - lastEffect
                    closeBal = first.balanceMinor
                }
                val expectedClosing = openBal + totalCredits - totalDebits
                isReconciled = (expectedClosing == closeBal)
            }
        }

        return StatementParseResult(
            rows = parsed,
            totalFound = rawRows.size - headerIndex - 1,
            invalidRows = invalid,
            detectedDelimiter = detectedDelim,
            headers = headers,
            formatName = parsed.firstOrNull()?.formatName ?: "UNKNOWN",
            totalDebitsMinor = totalDebits,
            totalCreditsMinor = totalCredits,
            smallScansTotalMinor = smallScansTotal,
            smallScansCount = smallScans.size,
            detectedMandatesCount = mandateCount,
            openingBalanceMinor = openBal,
            closingBalanceMinor = closeBal,
            isReconciled = isReconciled
        )
    }

    /**
     * Pure integer parsing of minor units from amount strings.
     */
    fun parseAmountMinor(text: String): Long {
        val clean = text.replace(Regex("[^0-9.]"), "")
        if (clean.isBlank()) return 0L
        val parts = clean.split(".")
        val integerPart = parts[0].toLongOrNull() ?: 0L
        val decimalPart = if (parts.size > 1) {
            parts[1].take(2).padEnd(2, '0').toLongOrNull() ?: 0L
        } else 0L
        return integerPart * 100L + decimalPart
    }
}
