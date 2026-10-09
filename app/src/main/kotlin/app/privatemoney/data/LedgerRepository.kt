package app.privatemoney.data

import androidx.paging.PagingSource
import androidx.room.withTransaction
import app.privatemoney.data.local.AppDatabase
import app.privatemoney.data.local.JournalEntryEntity
import app.privatemoney.data.local.LaneTotalRow
import app.privatemoney.data.local.TxnEntity
import app.privatemoney.domain.ledger.JournalPosting
import app.privatemoney.domain.ledger.TransactionDraft
import app.privatemoney.domain.ledger.TransactionType
import kotlinx.coroutines.flow.Flow

/** Extra descriptive fields stored alongside the draft. None of these affect balances. */
data class TransactionDetails(
    val localDate: String,
    val localTime: String,
    val merchant: String? = null,
    val description: String? = null,
    val notes: String? = null,
    val paymentMethod: String? = null,
    val needWant: String? = null,
    val imported: Boolean = false,
    val source: String = "MANUAL",
)

class LedgerRepository(private val db: AppDatabase, private val clock: () -> Long = System::currentTimeMillis) {

    /**
     * Posts a transaction atomically: the domain engine builds balanced journal legs, then the
     * transaction row and every leg are written in one database transaction. Any failure rolls back.
     */
    suspend fun record(draft: TransactionDraft, details: TransactionDetails) {
        val legs = JournalPosting.post(draft)
        check(JournalPosting.isBalanced(legs)) { "Unbalanced journal" }
        val now = clock()
        db.withTransaction {
            val dao = db.ledgerDao()
            dao.insertTxn(
                TxnEntity(
                    id = draft.id,
                    type = draft.type.name,
                    amountMinor = draft.amountMinor,
                    currency = draft.currency.code,
                    localDate = details.localDate,
                    localTime = details.localTime,
                    accountId = draft.accountId,
                    toAccountId = draft.toAccountId,
                    categoryId = draft.categoryId,
                    merchant = details.merchant,
                    description = details.description,
                    notes = details.notes,
                    paymentMethod = details.paymentMethod,
                    needWant = details.needWant,
                    imported = details.imported,
                    source = details.source,
                    createdAt = now,
                    updatedAt = now,
                    deletedAt = null,
                ),
            )
            dao.insertEntries(legs.map { JournalEntryEntity(txnId = it.txnId, accountId = it.accountId, categoryId = it.categoryId, amountMinor = it.amountMinor) })
        }
    }

    /** Soft delete keeps the record for audit; balances ignore deleted transactions. */
    suspend fun delete(id: String): Boolean = db.ledgerDao().softDelete(id, clock()) > 0

    fun observeRecent(limit: Int): Flow<List<TxnEntity>> = db.ledgerDao().observeRecent(limit)

    fun pagingSource(query: String = ""): PagingSource<Int, TxnEntity> = 
        if (query.isBlank()) db.ledgerDao().pagingSource() else db.ledgerDao().pagingSourceFiltered(query)

    fun observeLiquidBalance(): Flow<Long> = db.ledgerDao().observeLiquidBalance()

    /** Total of [type] transactions with localDate in [fromDate, toDate) (ISO yyyy-MM-dd strings). */
    fun observeTotal(type: TransactionType, fromDate: String, toDate: String): Flow<Long> =
        db.ledgerDao().observeTotal(type.name, fromDate, toDate)

    fun observeLaneTotals(fromDate: String, toDate: String): Flow<List<LaneTotalRow>> =
        db.ledgerDao().observeLaneTotals(fromDate, toDate)

    fun observeMonthlyTotals(fromDate: String): Flow<List<app.privatemoney.data.local.MonthlyTotalRow>> =
        db.ledgerDao().observeMonthlyTotals(fromDate)

    fun observeTopCategories(fromDate: String, toDate: String, limit: Int): Flow<List<app.privatemoney.data.local.CategorySpendRow>> =
        db.ledgerDao().observeTopCategories(fromDate, toDate, limit)
}
