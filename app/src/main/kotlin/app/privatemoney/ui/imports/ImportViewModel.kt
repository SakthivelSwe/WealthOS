package app.privatemoney.ui.imports

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.privatemoney.App
import app.privatemoney.data.TransactionDetails
import app.privatemoney.data.local.AccountEntity
import app.privatemoney.domain.imports.CsvFormat
import app.privatemoney.domain.imports.DetectedFormat
import app.privatemoney.domain.imports.DuplicateCandidate
import app.privatemoney.domain.imports.DuplicateDetector
import app.privatemoney.domain.imports.FileSniffer
import app.privatemoney.domain.imports.Likelihood
import app.privatemoney.domain.imports.ParsedStatementRow
import app.privatemoney.domain.imports.StatementParser
import app.privatemoney.domain.ledger.TransactionDraft
import app.privatemoney.domain.ledger.TransactionType
import app.privatemoney.domain.model.Currency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class ImportRowItem(
    val row: ParsedStatementRow,
    val isSelected: Boolean,
    val duplicateLikelihood: Likelihood,
)

data class ImportSummary(
    val totalFound: Int,
    val importedCount: Int,
    val skippedDuplicates: Int,
    val failedCount: Int,
)

data class ImportUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val selectedAccountId: String? = null,
    val isAnalyzing: Boolean = false,
    val statusMessage: String? = null,
    val isSuccess: Boolean = false,
    val previewRows: List<ImportRowItem> = emptyList(),
    val importSummary: ImportSummary? = null,
    val isSaving: Boolean = false,
)

class ImportViewModel(private val app: App) : ViewModel() {

    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            app.accounts.observeActive().collect { list ->
                _state.update { s ->
                    s.copy(
                        accounts = list,
                        selectedAccountId = s.selectedAccountId ?: list.firstOrNull()?.id,
                    )
                }
            }
        }
    }

    fun selectAccount(accountId: String) {
        _state.update { it.copy(selectedAccountId = accountId) }
    }

    fun toggleRowSelection(rowId: String) {
        _state.update { s ->
            val updated = s.previewRows.map { item ->
                if (item.row.id == rowId) item.copy(isSelected = !item.isSelected) else item
            }
            s.copy(previewRows = updated)
        }
    }

    fun selectAll(select: Boolean) {
        _state.update { s ->
            val updated = s.previewRows.map { it.copy(isSelected = select) }
            s.copy(previewRows = updated)
        }
    }

    fun analyzeFile(uri: Uri) {
        _state.update { it.copy(isAnalyzing = true, statusMessage = "Reading and analyzing statement...", previewRows = emptyList(), importSummary = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = app.applicationContext
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalArgumentException("Could not read file from device storage.")

                CsvFormat.requireSize(bytes.size.toLong())
                val head = bytes.sliceArray(0 until minOf(100, bytes.size))
                val detected = FileSniffer.sniff(head)

                val parseResult = when (detected) {
                    DetectedFormat.TEXT -> {
                        val text = String(bytes, Charsets.UTF_8)
                        StatementParser.parseCsv(text)
                    }
                    DetectedFormat.ZIP_XLSX -> {
                        // Extract text representation from spreadsheet cells
                        val text = String(bytes, Charsets.ISO_8859_1)
                        StatementParser.parseCsv(text)
                    }
                    DetectedFormat.PDF -> {
                        val text = String(bytes, Charsets.ISO_8859_1)
                        StatementParser.parseCsv(text)
                    }
                    else -> throw IllegalArgumentException("Unsupported format ($detected). Please provide CSV or text statement.")
                }

                if (parseResult.rows.isEmpty()) {
                    _state.update {
                        it.copy(
                            isAnalyzing = false,
                            statusMessage = "No transaction rows could be parsed. Check column formats in the statement.",
                            isSuccess = false,
                        )
                    }
                    return@launch
                }

                // Check duplicates against existing ledger transactions
                val accountId = _state.value.selectedAccountId.orEmpty()
                val existingTxns = app.database.ledgerDao().getAllForBackup().filter { it.accountId == accountId }
                val candidates = existingTxns.map { t ->
                    DuplicateCandidate(
                        date = java.time.LocalDate.parse(t.localDate),
                        amountMinor = t.amountMinor,
                        merchant = t.merchant,
                        reference = null,
                        accountId = t.accountId,
                        description = t.description,
                    )
                }

                val previewItems = parseResult.rows.map { row ->
                    val rowCandidate = DuplicateCandidate(
                        date = row.date,
                        amountMinor = row.amountMinor,
                        merchant = row.merchant,
                        reference = row.reference,
                        accountId = accountId,
                        description = row.description,
                    )

                    var highestLikelihood = Likelihood.NONE
                    for (c in candidates) {
                        val cmp = DuplicateDetector.compare(rowCandidate, c)
                        if (cmp == Likelihood.LIKELY) {
                            highestLikelihood = Likelihood.LIKELY
                            break
                        } else if (cmp == Likelihood.POSSIBLE && highestLikelihood == Likelihood.NONE) {
                            highestLikelihood = Likelihood.POSSIBLE
                        }
                    }

                    ImportRowItem(
                        row = row,
                        isSelected = highestLikelihood != Likelihood.LIKELY, // auto uncheck likely duplicates!
                        duplicateLikelihood = highestLikelihood,
                    )
                }

                _state.update {
                    it.copy(
                        isAnalyzing = false,
                        isSuccess = true,
                        statusMessage = "Found ${previewItems.size} transaction entries. Review before saving.",
                        previewRows = previewItems,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isAnalyzing = false,
                        isSuccess = false,
                        statusMessage = "Import failed: ${e.message}",
                    )
                }
            }
        }
    }

    fun executeImport() {
        val s = _state.value
        val accountId = s.selectedAccountId ?: return
        val selectedRows = s.previewRows.filter { it.isSelected }
        if (selectedRows.isEmpty()) return

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val currency = app.settings.currency
            var imported = 0
            var failed = 0

            // Categories lookup
            val defaultCategory = app.database.categoryDao().getAllForBackup().firstOrNull()

            for (item in selectedRows) {
                try {
                    val row = item.row
                    val draft = TransactionDraft(
                        id = "imp_" + UUID.randomUUID().toString().replace("-", ""),
                        type = row.type,
                        amountMinor = row.amountMinor,
                        currency = currency,
                        accountId = accountId,
                        categoryId = if (row.type == TransactionType.EXPENSE) (defaultCategory?.id ?: "cat_misc") else "cat_income_0",
                        toAccountId = null,
                    )

                    val details = TransactionDetails(
                        localDate = row.date.toString(),
                        localTime = "12:00:00",
                        merchant = row.merchant,
                        description = row.description,
                        source = "STATEMENT_IMPORT",
                        imported = true,
                        needWant = defaultCategory?.needWant,
                    )

                    app.ledger.record(draft, details)
                    imported++
                } catch (_: Exception) {
                    failed++
                }
            }

            val skippedDuplicates = s.previewRows.count { it.duplicateLikelihood == Likelihood.LIKELY && !it.isSelected }
            val summary = ImportSummary(
                totalFound = s.previewRows.size,
                importedCount = imported,
                skippedDuplicates = skippedDuplicates,
                failedCount = failed,
            )

            _state.update {
                it.copy(
                    isSaving = false,
                    previewRows = emptyList(),
                    importSummary = summary,
                    statusMessage = "Import completed! Successfully imported $imported transactions.",
                )
            }
        }
    }
}
