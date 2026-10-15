package app.privatemoney.data

import app.privatemoney.data.local.AppDatabase
import app.privatemoney.domain.ledger.IntegrityCheckReport
import app.privatemoney.domain.ledger.JournalEntry
import app.privatemoney.domain.ledger.LedgerIntegrityValidator
import app.privatemoney.domain.ledger.TransactionType
import app.privatemoney.domain.ledger.ValidationAccount
import app.privatemoney.domain.ledger.ValidationTxn
import app.privatemoney.domain.model.Currency

/**
 * Service to execute a full structural and balance integrity audit on the Room database.
 */
class DatabaseIntegrityValidator(private val db: AppDatabase) {

    suspend fun validateDatabase(): IntegrityCheckReport {
        val rawAccounts = db.accountDao().getAllForBackup()
        val rawCategories = db.categoryDao().getAllForBackup()
        val rawTxns = db.ledgerDao().getAllForBackup()
        val rawEntries = db.ledgerDao().getAllEntriesForBackup()
        val rawRecurring = db.recurringTxnDao().getAllForBackup()

        val validCategoryIds = rawCategories.map { it.id }.toSet()

        val validationAccounts = rawAccounts.map { acct ->
            ValidationAccount(
                id = acct.id,
                name = acct.name,
                currency = Currency(acct.currency, 2),
                openingBalanceMinor = acct.openingBalanceMinor,
                recordedBalanceMinor = null, // Let validator derive and compare if needed
            )
        }

        val validationTxns = rawTxns.map { txn ->
            ValidationTxn(
                id = txn.id,
                type = runCatching { TransactionType.valueOf(txn.type) }.getOrDefault(TransactionType.EXPENSE),
                amountMinor = txn.amountMinor,
                currency = Currency(txn.currency, 2),
                accountId = txn.accountId,
                toAccountId = txn.toAccountId,
                categoryId = txn.categoryId,
                isDeleted = txn.deletedAt != null,
            )
        }

        val domainJournalEntries = rawEntries.map { entry ->
            JournalEntry(
                txnId = entry.txnId,
                accountId = entry.accountId,
                categoryId = entry.categoryId,
                amountMinor = entry.amountMinor,
            )
        }

        val recurringKeys = rawRecurring.map { "txn_recur_${it.id}_${it.nextRunDate.replace("-", "")}" }

        return LedgerIntegrityValidator.validate(
            transactions = validationTxns,
            journalEntries = domainJournalEntries,
            accounts = validationAccounts,
            validCategoryIds = validCategoryIds,
            recurringOccurrenceKeys = recurringKeys,
        )
    }
}
