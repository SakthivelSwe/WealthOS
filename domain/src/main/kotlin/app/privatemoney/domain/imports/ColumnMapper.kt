package app.privatemoney.domain.imports

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ColumnMapping(
    val headerRowIndex: Int,
    val dateCol: Int,
    val descriptionCol: Int,
    val debitCol: Int,
    val creditCol: Int,
    val amountCol: Int,
    val indicatorCol: Int, // Dr/Cr or Type column
    val balanceCol: Int,
    val referenceCol: Int,
    val confidence: MappingConfidence,
)

enum class MappingConfidence {
    HIGH,
    MEDIUM,
    LOW
}

object ColumnMapper {

    private val DATE_HEADERS = listOf(
        "tran date", "transaction date", "value date", "txn date", "posting date", "date", "txn_date", "trans date"
    )

    private val NARRATION_HEADERS = listOf(
        "particulars", "narration", "description", "remarks", "transaction remarks", "details", "desc", "memo"
    )

    private val DEBIT_HEADERS = listOf(
        "withdrawal amt.", "withdrawal amt", "withdrawal", "debit amt.", "debit amt", "debit amount", "debit", "dr amt", "dr"
    )

    private val CREDIT_HEADERS = listOf(
        "deposit amt.", "deposit amt", "deposit", "credit amt.", "credit amt", "credit amount", "credit", "cr amt", "cr"
    )

    private val AMOUNT_HEADERS = listOf(
        "amount", "net amount", "txn amount", "transaction amount", "amt"
    )

    private val INDICATOR_HEADERS = listOf(
        "type", "cr/dr", "dr/cr", "cr_dr", "dr_cr", "indicator"
    )

    private val BALANCE_HEADERS = listOf(
        "balance", "closing balance", "running balance", "closing bal", "bal"
    )

    private val REFERENCE_HEADERS = listOf(
        "chq no", "cheque no", "ref no", "reference no", "utr", "chq / ref no", "ref/chq no", "reference", "rrn"
    )

    private val DATE_FORMATTERS = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("d-M-yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
    )

    /**
     * Exact whole-word matching to avoid false positives (e.g. "Address" or "Sundry Dr").
     */
    private fun matchesHeader(colHeader: String, dictionary: List<String>): Boolean {
        val h = colHeader.lowercase().trim()
        return dictionary.any { target ->
            h == target ||
            Regex("\\b${Regex.escape(target)}\\b").containsMatchIn(h)
        }
    }

    /**
     * Finds headers and maps column indices across raw statement rows.
     */
    fun discoverMapping(rows: List<List<String>>): ColumnMapping? {
        val maxScan = minOf(40, rows.size)

        for (i in 0 until maxScan) {
            val row = rows[i]
            if (row.size < 2) continue

            val dCol = row.indexOfFirst { matchesHeader(it, DATE_HEADERS) }
            val nCol = row.indexOfFirst { matchesHeader(it, NARRATION_HEADERS) }

            if (dCol >= 0 && nCol >= 0 && dCol != nCol) {
                val debitCol = row.indexOfFirst { matchesHeader(it, DEBIT_HEADERS) }
                val creditCol = row.indexOfFirst { matchesHeader(it, CREDIT_HEADERS) }
                val amountCol = row.indexOfFirst { matchesHeader(it, AMOUNT_HEADERS) }
                val indicatorCol = row.indexOfFirst { matchesHeader(it, INDICATOR_HEADERS) }
                val balanceCol = row.indexOfFirst { matchesHeader(it, BALANCE_HEADERS) }
                val refCol = row.indexOfFirst { matchesHeader(it, REFERENCE_HEADERS) }

                val hasAmount = (debitCol >= 0 && creditCol >= 0) || amountCol >= 0
                val confidence = if (hasAmount) MappingConfidence.HIGH else MappingConfidence.MEDIUM

                return ColumnMapping(
                    headerRowIndex = i,
                    dateCol = dCol,
                    descriptionCol = nCol,
                    debitCol = debitCol,
                    creditCol = creditCol,
                    amountCol = amountCol,
                    indicatorCol = indicatorCol,
                    balanceCol = balanceCol,
                    referenceCol = refCol,
                    confidence = confidence
                )
            }
        }

        // Low confidence fallback if first row has date-like data
        return null
    }

    /**
     * Parses calendar dates without UTC conversion.
     */
    fun parseDate(raw: String): LocalDate? {
        val s = raw.trim().replace('.', '-').replace('/', '-')
        for (f in DATE_FORMATTERS) {
            try {
                return LocalDate.parse(raw.trim(), f)
            } catch (_: Exception) {}
            try {
                return LocalDate.parse(s, f)
            } catch (_: Exception) {}
        }
        return null
    }
}
