package app.privatemoney.domain.imports

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/** Limits applied to every imported document. Files are untrusted input. */
data class ImportLimits(
    val maxBytes: Long = 10L * 1024 * 1024,
    val maxRows: Int = 100_000,
    val maxColumns: Int = 64,
    val maxCellChars: Int = 2_000,
    val maxZipUncompressedBytes: Long = 100L * 1024 * 1024,
    val maxZipRatio: Int = 100,
    val maxZipEntries: Int = 2_000,
)

enum class RejectReason {
    TOO_LARGE, EMPTY, TOO_MANY_ROWS, TOO_MANY_COLUMNS, CELL_TOO_LONG, MALFORMED, FORMAT_MISMATCH,
    UNSUPPORTED_FORMAT, ZIP_BOMB, UNSAFE_PATH,
}

class ImportRejectedException(val reason: RejectReason) : Exception(reason.name)

enum class DetectedFormat { PDF, ZIP_XLSX, OLE_XLS, PNG, JPEG, TEXT, UNKNOWN }

object FileSniffer {
    private val PDF = "%PDF-".toByteArray(Charsets.US_ASCII)
    private val ZIP = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
    private val OLE = byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte())
    private val PNG = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val JPEG = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

    /** Decides the real format from content, never from the file name or declared MIME type. */
    fun sniff(head: ByteArray): DetectedFormat = when {
        head.startsWith(PDF) -> DetectedFormat.PDF
        head.startsWith(ZIP) -> DetectedFormat.ZIP_XLSX
        head.startsWith(OLE) -> DetectedFormat.OLE_XLS
        head.startsWith(PNG) -> DetectedFormat.PNG
        head.startsWith(JPEG) -> DetectedFormat.JPEG
        looksLikeText(head) -> DetectedFormat.TEXT
        else -> DetectedFormat.UNKNOWN
    }

    /** Rejects when the declared extension disagrees with the sniffed content (MIME spoofing). */
    fun requireConsistent(extension: String, detected: DetectedFormat) {
        val ok = when (extension.lowercase().removePrefix(".")) {
            "csv", "txt" -> detected == DetectedFormat.TEXT
            "xlsx" -> detected == DetectedFormat.ZIP_XLSX
            "xls" -> detected == DetectedFormat.OLE_XLS
            "pdf" -> detected == DetectedFormat.PDF
            "png" -> detected == DetectedFormat.PNG
            "jpg", "jpeg" -> detected == DetectedFormat.JPEG
            else -> false
        }
        if (!ok) throw ImportRejectedException(RejectReason.FORMAT_MISMATCH)
    }

    private fun looksLikeText(head: ByteArray): Boolean {
        if (head.isEmpty() || head.any { it == 0.toByte() }) return false
        // A truncated multi-byte sequence at the end of the sample must not cause a false reject.
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        for (cut in 0..3) {
            if (head.size - cut <= 0) break
            try {
                decoder.reset()
                decoder.decode(ByteBuffer.wrap(head, 0, head.size - cut))
                return true
            } catch (e: CharacterCodingException) {
                continue
            }
        }
        return false
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
}

/** Guards against zip / decompression bombs and path traversal when reading XLSX containers. */
object ZipGuard {
    fun checkEntry(name: String, compressedSize: Long, uncompressedSize: Long, limits: ImportLimits) {
        if (name.startsWith("/") || name.startsWith("\\") || name.contains("..") || name.contains(':')) {
            throw ImportRejectedException(RejectReason.UNSAFE_PATH)
        }
        if (uncompressedSize < 0 || compressedSize < 0) throw ImportRejectedException(RejectReason.MALFORMED)
        if (compressedSize > 0 && uncompressedSize / compressedSize > limits.maxZipRatio) {
            throw ImportRejectedException(RejectReason.ZIP_BOMB)
        }
    }

    fun checkTotals(entryCount: Int, totalUncompressed: Long, limits: ImportLimits) {
        if (entryCount > limits.maxZipEntries || totalUncompressed > limits.maxZipUncompressedBytes) {
            throw ImportRejectedException(RejectReason.ZIP_BOMB)
        }
    }
}

object CsvFormat {
    private val FORMULA_TRIGGERS = charArrayOf('=', '+', '-', '@', '\t', '\r')

    /**
     * Escapes a TEXT value for CSV export: neutralises spreadsheet formula injection by prefixing a
     * single quote, then applies RFC 4180 quoting. Do NOT use for numeric columns.
     */
    fun escapeText(value: String): String {
        val safe = if (value.isNotEmpty() && value[0] in FORMULA_TRIGGERS) "'$value" else value
        return quote(safe)
    }

    /** Quotes a value that is already known to be safe (for example a formatted number). */
    fun quote(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

    /**
     * Parses CSV text into rows of plain strings. Cells are never interpreted, so formulas stay inert.
     * Enforces row, column and cell limits while scanning.
     */
    fun parse(input: String, limits: ImportLimits = ImportLimits()): List<List<String>> {
        val text = input.removePrefix("\uFEFF")
        if (text.isBlank()) throw ImportRejectedException(RejectReason.EMPTY)
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0

        fun endCell() {
            row.add(cell.toString())
            cell.setLength(0)
            if (row.size > limits.maxColumns) throw ImportRejectedException(RejectReason.TOO_MANY_COLUMNS)
        }

        fun endRow() {
            endCell()
            if (!(row.size == 1 && row[0].isEmpty())) {
                rows.add(row)
                if (rows.size > limits.maxRows) throw ImportRejectedException(RejectReason.TOO_MANY_ROWS)
            }
            row = ArrayList()
        }

        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        cell.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    cell.append(c)
                }
            } else {
                when (c) {
                    '"' -> if (cell.isEmpty()) inQuotes = true else cell.append(c)
                    ',' -> endCell()
                    '\r' -> {
                        if (i + 1 < text.length && text[i + 1] == '\n') i++
                        endRow()
                    }
                    '\n' -> endRow()
                    else -> cell.append(c)
                }
            }
            if (cell.length > limits.maxCellChars) throw ImportRejectedException(RejectReason.CELL_TOO_LONG)
            i++
        }
        if (inQuotes) throw ImportRejectedException(RejectReason.MALFORMED)
        if (cell.isNotEmpty() || row.isNotEmpty()) endRow()
        if (rows.isEmpty()) throw ImportRejectedException(RejectReason.EMPTY)
        return rows
    }

    fun requireSize(bytes: Long, limits: ImportLimits = ImportLimits()) {
        if (bytes <= 0) throw ImportRejectedException(RejectReason.EMPTY)
        if (bytes > limits.maxBytes) throw ImportRejectedException(RejectReason.TOO_LARGE)
    }
}
