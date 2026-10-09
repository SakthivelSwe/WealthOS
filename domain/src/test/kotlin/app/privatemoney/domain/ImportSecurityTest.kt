package app.privatemoney.domain

import app.privatemoney.domain.imports.CsvFormat
import app.privatemoney.domain.imports.DetectedFormat
import app.privatemoney.domain.imports.DuplicateCandidate
import app.privatemoney.domain.imports.DuplicateDetector
import app.privatemoney.domain.imports.FileSniffer
import app.privatemoney.domain.imports.ImportLimits
import app.privatemoney.domain.imports.ImportRejectedException
import app.privatemoney.domain.imports.Likelihood
import app.privatemoney.domain.imports.RejectReason
import app.privatemoney.domain.imports.ZipGuard
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class ImportSecurityTest {

    private fun reason(block: () -> Unit): RejectReason =
        assertFailsWith<ImportRejectedException> { block() }.reason

    @Test
    fun exportEscapesFormulaTriggers() {
        for (v in listOf("=SUM(A1)", "+1", "-1", "@cmd", "\tx", "\rx")) {
            assertEquals('\'', CsvFormat.escapeText(v).trimStart('"')[0], "value: $v")
        }
        assertEquals("plain", CsvFormat.escapeText("plain"))
    }

    @Test
    fun exportQuotesSpecialCharacters() {
        assertEquals("\"a,b\"", CsvFormat.escapeText("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", CsvFormat.escapeText("say \"hi\""))
    }

    @Test
    fun parsesQuotedFieldsAndNewlines() {
        val rows = CsvFormat.parse("date,desc\r\n2026-01-02,\"Coffee, large\"\n2026-01-03,\"multi\nline\"\n")
        assertEquals(3, rows.size)
        assertEquals("Coffee, large", rows[1][1])
        assertEquals("multi\nline", rows[2][1])
    }

    @Test
    fun formulasStayInertText() {
        val rows = CsvFormat.parse("a\n=HYPERLINK(\"http://evil\")\n")
        assertEquals("=HYPERLINK(\"http://evil\")", rows[1][0])
    }

    @Test
    fun stripsBom() {
        assertEquals("a", CsvFormat.parse("\uFEFFa,b\n1,2")[0][0])
    }

    @Test
    fun enforcesLimits() {
        val limits = ImportLimits(maxRows = 3, maxColumns = 2, maxCellChars = 5)
        assertEquals(RejectReason.TOO_MANY_ROWS, reason { CsvFormat.parse("a\nb\nc\nd\n", limits) })
        assertEquals(RejectReason.TOO_MANY_COLUMNS, reason { CsvFormat.parse("a,b,c\n", limits) })
        assertEquals(RejectReason.CELL_TOO_LONG, reason { CsvFormat.parse("abcdefgh\n", limits) })
        assertEquals(RejectReason.MALFORMED, reason { CsvFormat.parse("\"unterminated") })
        assertEquals(RejectReason.EMPTY, reason { CsvFormat.parse("  \n", limits) })
    }

    @Test
    fun sizeLimit() {
        assertEquals(RejectReason.TOO_LARGE, reason { CsvFormat.requireSize(11L * 1024 * 1024) })
        assertEquals(RejectReason.EMPTY, reason { CsvFormat.requireSize(0) })
    }

    @Test
    fun sniffsRealFormatsFromBytes() {
        assertEquals(DetectedFormat.PDF, FileSniffer.sniff("%PDF-1.7 rest".toByteArray()))
        assertEquals(DetectedFormat.ZIP_XLSX, FileSniffer.sniff(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0)))
        assertEquals(DetectedFormat.TEXT, FileSniffer.sniff("date,amount\n1,2".toByteArray()))
        assertEquals(DetectedFormat.UNKNOWN, FileSniffer.sniff(byteArrayOf(1, 2, 0, 3)))
    }

    @Test
    fun textSampleCutMidCharacterIsStillText() {
        val full = "caf\u00E9".toByteArray(Charsets.UTF_8)
        assertEquals(DetectedFormat.TEXT, FileSniffer.sniff(full.copyOf(full.size - 1)))
    }

    @Test
    fun rejectsMimeSpoofing() {
        // An executable-looking blob renamed to .csv, and a PDF renamed to .xlsx
        assertEquals(RejectReason.FORMAT_MISMATCH, reason { FileSniffer.requireConsistent("csv", DetectedFormat.UNKNOWN) })
        assertEquals(RejectReason.FORMAT_MISMATCH, reason { FileSniffer.requireConsistent("xlsx", DetectedFormat.PDF) })
        FileSniffer.requireConsistent("xlsx", DetectedFormat.ZIP_XLSX)
    }

    @Test
    fun zipGuardRejectsBombsAndTraversal() {
        val limits = ImportLimits()
        assertEquals(RejectReason.ZIP_BOMB, reason { ZipGuard.checkEntry("xl/sheet1.xml", 10, 10_000, limits) })
        assertEquals(RejectReason.UNSAFE_PATH, reason { ZipGuard.checkEntry("../../evil", 10, 10, limits) })
        assertEquals(RejectReason.UNSAFE_PATH, reason { ZipGuard.checkEntry("/abs/path", 10, 10, limits) })
        assertEquals(RejectReason.ZIP_BOMB, reason { ZipGuard.checkTotals(5, limits.maxZipUncompressedBytes + 1, limits) })
        assertEquals(RejectReason.ZIP_BOMB, reason { ZipGuard.checkTotals(limits.maxZipEntries + 1, 1, limits) })
        ZipGuard.checkEntry("xl/sheet1.xml", 100, 5_000, limits)
    }
}

class DuplicateDetectorTest {
    private val day = LocalDate.of(2026, 3, 10)
    private fun c(
        merchant: String? = "Swiggy", amount: Long = 42_000, date: LocalDate = day,
        ref: String? = null, account: String? = "a",
    ) = DuplicateCandidate(date, amount, merchant, ref, account)

    @Test
    fun identicalRowsAreLikely() {
        assertEquals(Likelihood.LIKELY, DuplicateDetector.compare(c(), c()))
    }

    @Test
    fun differentAmountIsNeverDuplicate() {
        assertEquals(Likelihood.NONE, DuplicateDetector.compare(c(), c(amount = 42_001)))
    }

    @Test
    fun differentAccountIsNeverDuplicate() {
        assertEquals(Likelihood.NONE, DuplicateDetector.compare(c(), c(account = "b")))
    }

    @Test
    fun matchingReferenceIsLikelyEvenWithDifferentMerchantText() {
        assertEquals(Likelihood.LIKELY, DuplicateDetector.compare(c(ref = "UTR123"), c(merchant = "SWIGGY BLR", ref = "utr123")))
    }

    @Test
    fun nextDaySimilarMerchantIsPossible() {
        assertEquals(Likelihood.POSSIBLE, DuplicateDetector.compare(c(), c(date = day.plusDays(1))))
    }

    @Test
    fun farApartDatesAreNotDuplicates() {
        assertEquals(Likelihood.NONE, DuplicateDetector.compare(c(), c(date = day.plusDays(5))))
    }

    @Test
    fun unrelatedMerchantSameDayIsNotLikely() {
        assertEquals(Likelihood.NONE, DuplicateDetector.compare(c(), c(merchant = "Electricity Board")))
    }

    @Test
    fun fingerprintIsStableAndSensitive() {
        val a = DuplicateDetector.rowFingerprint("acc", c())
        assertEquals(a, DuplicateDetector.rowFingerprint("acc", c(merchant = "  swiggy ")))
        assertNotEquals(a, DuplicateDetector.rowFingerprint("acc", c(amount = 1)))
        assertNotEquals(a, DuplicateDetector.rowFingerprint("other", c()))
    }
}
