package app.privatemoney.domain.imports

import app.privatemoney.domain.ledger.TransactionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipInputStream

data class ParsedStatementRow(
    val id: String,
    val date: LocalDate,
    val description: String,
    val merchant: String?,
    val amountMinor: Long,
    val type: TransactionType,
    val reference: String?,
    val rawText: String,
)

data class StatementParseResult(
    val rows: List<ParsedStatementRow>,
    val totalFound: Int,
    val invalidRows: Int,
    val detectedDelimiter: Char,
    val headers: List<String>,
)

object StatementParser {

    private val DATE_FORMATTERS = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("d-M-yyyy"),
        DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
    )

    fun parseCsv(text: String, limits: ImportLimits = ImportLimits()): StatementParseResult {
        val rawRows = CsvFormat.parse(text, limits)
        if (rawRows.isEmpty()) {
            return StatementParseResult(emptyList(), 0, 0, ',', emptyList())
        }

        // Find header row (first row with recognisable column names)
        var headerIndex = -1
        var dateCol = -1
        var descCol = -1
        var debitCol = -1
        var creditCol = -1
        var amountCol = -1

        for (i in 0 until minOf(15, rawRows.size)) {
            val candidate = rawRows[i].map { it.lowercase().trim() }
            val dIdx = candidate.indexOfFirst { it.contains("date") || it == "dt" }
            val descIdx = candidate.indexOfFirst {
                it.contains("description") || it.contains("narration") || it.contains("particular") ||
                    it.contains("detail") || it.contains("remarks") || it.contains("payee") || it == "memo"
            }
            if (dIdx >= 0 && descIdx >= 0) {
                headerIndex = i
                dateCol = dIdx
                descCol = descIdx
                debitCol = candidate.indexOfFirst { it.contains("debit") || it.contains("withdrawal") || it == "dr" }
                creditCol = candidate.indexOfFirst { it.contains("credit") || it.contains("deposit") || it == "cr" }
                amountCol = candidate.indexOfFirst { it == "amount" || it.contains("net amount") || it.contains("txn amount") }
                break
            }
        }

        if (headerIndex == -1) {
            // Fallback: assume first row is headers if >= 3 columns
            headerIndex = 0
            val candidate = rawRows[0].map { it.lowercase().trim() }
            dateCol = candidate.indexOfFirst { it.contains("date") }.coerceAtLeast(0)
            descCol = candidate.indexOfFirst { it.contains("desc") || it.contains("detail") || it.contains("narration") }.coerceAtLeast(1)
            amountCol = candidate.indexOfFirst { it.contains("amount") }.coerceAtLeast(2)
        }

        val headers = rawRows[headerIndex]
        val parsed = mutableListOf<ParsedStatementRow>()
        var invalid = 0

        for (i in (headerIndex + 1) until rawRows.size) {
            val row = rawRows[i]
            if (row.all { it.isBlank() }) continue

            try {
                val dateStr = row.getOrNull(dateCol)?.trim().orEmpty()
                val desc = row.getOrNull(descCol)?.trim().orEmpty()
                val date = parseDate(dateStr)
                if (date == null) {
                    invalid++
                    continue
                }

                var amountMinor = 0L
                var type = TransactionType.EXPENSE

                if (debitCol >= 0 && creditCol >= 0) {
                    val debitStr = row.getOrNull(debitCol)?.trim().orEmpty()
                    val creditStr = row.getOrNull(creditCol)?.trim().orEmpty()
                    val debitVal = parseAmountMinor(debitStr)
                    val creditVal = parseAmountMinor(creditStr)

                    if (debitVal > 0) {
                        amountMinor = debitVal
                        type = TransactionType.EXPENSE
                    } else if (creditVal > 0) {
                        amountMinor = creditVal
                        type = TransactionType.INCOME
                    }
                }

                if (amountMinor == 0L && amountCol >= 0) {
                    val amtStr = row.getOrNull(amountCol)?.trim().orEmpty()
                    val isNegative = amtStr.startsWith("-") || (amtStr.startsWith("(") && amtStr.endsWith(")"))
                    val parsedAmt = parseAmountMinor(amtStr)
                    amountMinor = parsedAmt
                    type = if (isNegative) TransactionType.EXPENSE else TransactionType.INCOME
                }

                if (amountMinor > 0 && desc.isNotBlank()) {
                    parsed.add(
                        ParsedStatementRow(
                            id = "import_row_$i",
                            date = date,
                            description = desc,
                            merchant = extractMerchant(desc),
                            amountMinor = amountMinor,
                            type = type,
                            reference = null,
                            rawText = row.joinToString(","),
                        )
                    )
                } else {
                    invalid++
                }
            } catch (e: Exception) {
                invalid++
            }
        }

        return StatementParseResult(
            rows = parsed,
            totalFound = rawRows.size - headerIndex - 1,
            invalidRows = invalid,
            detectedDelimiter = ',',
            headers = headers,
        )
    }

    fun parseAmountMinor(text: String): Long {
        val clean = text.replace(Regex("[^0-9.]"), "")
        if (clean.isBlank()) return 0L
        val parts = clean.split(".")
        val integerPart = parts[0].toLongOrNull() ?: 0L
        val decimalPart = if (parts.size > 1) parts[1].padEnd(2, '0').take(2).toIntOrNull() ?: 0 else 0
        return integerPart * 100 + decimalPart
    }

    private fun parseDate(text: String): LocalDate? {
        val clean = text.trim().replace('.', '-').replace('/', '-')
        for (f in DATE_FORMATTERS) {
            try {
                return LocalDate.parse(text, f)
            } catch (e: Exception) {}
            try {
                return LocalDate.parse(clean, f)
            } catch (e: Exception) {}
        }
        return null
    }

    private fun extractMerchant(description: String): String {
        val cleaned = description
            .replace(Regex("(?i)upi[-/:]"), " ")
            .replace(Regex("(?i)pos[-/:]"), " ")
            .replace(Regex("(?i)card[-/:]"), " ")
            .replace(Regex("(?i)atm[-/:]"), " ")
            .replace(Regex("(?i)ach[-/:]"), " ")
            .replace(Regex("[0-9]{6,}"), " ") // Strip long reference numbers
            .trim()
        val tokens = cleaned.split(Regex("[^a-zA-Z0-9&]+")).filter { it.length > 1 }
        return tokens.take(3).joinToString(" ").ifBlank { description.take(25) }
    }
}
