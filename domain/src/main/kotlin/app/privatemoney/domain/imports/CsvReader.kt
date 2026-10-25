package app.privatemoney.domain.imports

object CsvReader {

    /**
     * Detects delimiter from candidate characters (comma, tab, semicolon).
     */
    fun detectDelimiter(text: String): Char {
        val sample = text.lineSequence().take(20).joinToString("\n")
        var commaCount = 0
        var tabCount = 0
        var semicolonCount = 0
        var inQuotes = false

        for (i in sample.indices) {
            val c = sample[i]
            if (c == '"') {
                if (i + 1 < sample.length && sample[i + 1] == '"') {
                    // escaped quote
                } else {
                    inQuotes = !inQuotes
                }
            } else if (!inQuotes) {
                when (c) {
                    ',' -> commaCount++
                    '\t' -> tabCount++
                    ';' -> semicolonCount++
                }
            }
        }

        return when {
            tabCount > commaCount && tabCount > semicolonCount -> '\t'
            semicolonCount > commaCount && semicolonCount > tabCount -> ';'
            else -> ','
        }
    }

    /**
     * Full RFC 4180 parser supporting dynamic delimiter, escaped quotes, BOM stripping,
     * CRLF / LF line endings, and multi-line quoted fields.
     */
    fun read(
        input: String,
        delimiter: Char? = null,
        limits: ImportLimits = ImportLimits()
    ): List<List<String>> {
        val text = input.removePrefix("\uFEFF")
        if (text.isBlank()) throw ImportRejectedException(RejectReason.EMPTY)

        val delim = delimiter ?: detectDelimiter(text)
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0

        fun endCell() {
            row.add(cell.toString().trim())
            cell.setLength(0)
            if (row.size > limits.maxColumns) throw ImportRejectedException(RejectReason.TOO_MANY_COLUMNS)
        }

        fun endRow() {
            endCell()
            // Ignore completely empty rows
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
                        i++ // skip second quote
                    } else {
                        inQuotes = false
                    }
                } else {
                    cell.append(c)
                }
            } else {
                when (c) {
                    '"' -> {
                        if (cell.isEmpty()) {
                            inQuotes = true
                        } else {
                            cell.append(c)
                        }
                    }
                    delim -> endCell()
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
}
