package app.privatemoney.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow

/** Sum of expenses per need/want/saving classification. needWant is null for unclassified expenses. */
data class LaneTotalRow(val needWant: String?, val total: Long)

/** Monthly totals used for the trend chart (income/expense by calendar month). */
data class MonthlyTotalRow(val month: String, val type: String, val total: Long)

/** Top spending categories. */
data class CategorySpendRow(val categoryId: String?, val total: Long)

data class AccountWithBalance(
    @androidx.room.Embedded val account: AccountEntity,
    val currentBalanceMinor: Long,
)

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: AccountEntity)

    @Query("SELECT * FROM account WHERE archived = 0 ORDER BY name")
    fun observeActive(): Flow<List<AccountEntity>>

    @Query(
        """
        SELECT a.*, (a.openingBalanceMinor + COALESCE(
            (SELECT SUM(j.amountMinor) FROM journal_entry j
             JOIN txn t ON t.id = j.txnId
             WHERE j.accountId = a.id AND t.deletedAt IS NULL), 0)) AS currentBalanceMinor
        FROM account a WHERE a.archived = 0 ORDER BY a.name
        """
    )
    fun observeActiveWithBalances(): Flow<List<AccountWithBalance>>

    @Query("SELECT * FROM account WHERE id = :id")
    suspend fun get(id: String): AccountEntity?

    @Query("UPDATE account SET name = :name, type = :type, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateDetails(id: String, name: String, type: String, updatedAt: Long): Int

    @Query("UPDATE account SET openingBalanceMinor = :openingMinor, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateOpeningBalance(id: String, openingMinor: Long, updatedAt: Long): Int

    @Query("UPDATE account SET archived = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archive(id: String, updatedAt: Long): Int

    @Query("SELECT * FROM account")
    suspend fun getAllForBackup(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun restoreAccounts(accounts: List<AccountEntity>): List<Long>
}



@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("SELECT COUNT(*) FROM category")
    suspend fun count(): Int

    @Query("SELECT * FROM category WHERE archived = 0 ORDER BY sortOrder")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category")
    suspend fun getAllForBackup(): List<CategoryEntity>
}


@Dao
interface LedgerDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTxn(txn: TxnEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntries(entries: List<JournalEntryEntity>)

    @Query("UPDATE txn SET deletedAt = :at, updatedAt = :at WHERE id = :id AND deletedAt IS NULL")
    suspend fun softDelete(id: String, at: Long): Int

    @Query("UPDATE txn SET merchant = :merchant, description = :description, notes = :notes, updatedAt = :updatedAt WHERE id = :id AND deletedAt IS NULL")
    suspend fun updateDetails(id: String, merchant: String?, description: String?, notes: String?, updatedAt: Long): Int

    @Query("SELECT * FROM txn WHERE id = :id AND deletedAt IS NULL")
    suspend fun getTxn(id: String): TxnEntity?

    @Query("SELECT * FROM journal_entry WHERE txnId = :txnId")
    suspend fun getEntriesForTxn(txnId: String): List<JournalEntryEntity>

    /** Derived balance: opening balance plus the sum of live journal legs. */
    @Query(
        """
        SELECT a.openingBalanceMinor + COALESCE(
            (SELECT SUM(j.amountMinor) FROM journal_entry j
             JOIN txn t ON t.id = j.txnId
             WHERE j.accountId = a.id AND t.deletedAt IS NULL), 0)
        FROM account a WHERE a.id = :accountId
        """,
    )
    fun observeBalance(accountId: String): Flow<Long?>

    /**
     * Sum of balances of active liquid accounts. The type list must match AccountType.isLiquid
     * (BANK, CASH, UPI_WALLET, SAVINGS). Optionally filtered by currency.
     */
    @Query(
        """
        SELECT
          (SELECT COALESCE(SUM(openingBalanceMinor), 0) FROM account
             WHERE archived = 0 AND (:currency IS NULL OR currency = :currency) AND type IN ('BANK','CASH','UPI_WALLET','SAVINGS'))
          +
          (SELECT COALESCE(SUM(j.amountMinor), 0) FROM journal_entry j
             JOIN txn t ON t.id = j.txnId
             JOIN account a ON a.id = j.accountId
             WHERE t.deletedAt IS NULL AND a.archived = 0 AND (:currency IS NULL OR a.currency = :currency)
               AND a.type IN ('BANK','CASH','UPI_WALLET','SAVINGS'))
        """,
    )
    fun observeLiquidBalance(currency: String? = null): Flow<Long>

    @Query(
        """
        SELECT COALESCE(SUM(amountMinor), 0) FROM txn
        WHERE type = :type AND (:currency IS NULL OR currency = :currency) AND deletedAt IS NULL AND localDate >= :fromDate AND localDate < :toDate
        """,
    )
    fun observeTotal(type: String, fromDate: String, toDate: String, currency: String? = null): Flow<Long>

    @Query(
        """
        SELECT needWant AS needWant, COALESCE(SUM(amountMinor), 0) AS total FROM txn
        WHERE type = 'EXPENSE' AND (:currency IS NULL OR currency = :currency) AND deletedAt IS NULL AND localDate >= :fromDate AND localDate < :toDate
        GROUP BY needWant
        """,
    )
    fun observeLaneTotals(fromDate: String, toDate: String, currency: String? = null): Flow<List<LaneTotalRow>>

    @Query("SELECT * FROM txn WHERE deletedAt IS NULL ORDER BY localDate DESC, createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun page(limit: Int, offset: Int): List<TxnEntity>

    @Query("SELECT * FROM txn WHERE deletedAt IS NULL ORDER BY localDate DESC, createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TxnEntity>>

    @Query("SELECT * FROM txn WHERE deletedAt IS NULL ORDER BY localDate DESC, createdAt DESC")
    fun pagingSource(): PagingSource<Int, TxnEntity>

    @Query("""
        SELECT * FROM txn 
        WHERE deletedAt IS NULL 
        AND (:query = '' OR description LIKE '%' || :query || '%' OR merchant LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%')
        ORDER BY localDate DESC, createdAt DESC
    """)
    fun pagingSourceFiltered(query: String): PagingSource<Int, TxnEntity>

    /** Monthly income & expense totals for the last [months] months, for the trend chart. */
    @Query(
        """
        SELECT substr(localDate, 1, 7) AS month, type, COALESCE(SUM(amountMinor), 0) AS total
        FROM txn
        WHERE deletedAt IS NULL
          AND (:currency IS NULL OR currency = :currency)
          AND type IN ('INCOME','EXPENSE')
          AND localDate >= :fromDate
        GROUP BY month, type
        ORDER BY month ASC
        """,
    )
    fun observeMonthlyTotals(fromDate: String, currency: String? = null): Flow<List<MonthlyTotalRow>>

    /** Top expense categories for a date range. */
    @Query(
        """
        SELECT categoryId, COALESCE(SUM(amountMinor), 0) AS total
        FROM txn
        WHERE type = 'EXPENSE' AND (:currency IS NULL OR currency = :currency) AND deletedAt IS NULL
          AND localDate >= :fromDate AND localDate < :toDate
        GROUP BY categoryId
        ORDER BY total DESC
        LIMIT :limit
        """,
    )
    fun observeTopCategories(fromDate: String, toDate: String, limit: Int, currency: String? = null): Flow<List<CategorySpendRow>>

    @Query(
        """
        UPDATE txn 
        SET needWant = (SELECT needWant FROM category WHERE category.id = txn.categoryId)
        WHERE needWant IS NULL AND categoryId IS NOT NULL
        """
    )
    suspend fun backfillNeedWant(): Int

    @Query("SELECT * FROM txn WHERE deletedAt IS NULL AND localDate = :date LIMIT 1")
    suspend fun hasTransactionOnDate(date: String): TxnEntity?

    @Query("SELECT DISTINCT localDate FROM txn WHERE deletedAt IS NULL AND localDate >= :fromDate ORDER BY localDate ASC")
    suspend fun getDatesWithTransactions(fromDate: String): List<String>

    @Query("SELECT * FROM txn WHERE deletedAt IS NULL")
    suspend fun getAllForBackup(): List<TxnEntity>

    @Query("SELECT j.* FROM journal_entry j JOIN txn t ON t.id = j.txnId WHERE t.deletedAt IS NULL")
    suspend fun getAllEntriesForBackup(): List<JournalEntryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun restoreTxns(txns: List<TxnEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun restoreEntries(entries: List<JournalEntryEntity>): List<Long>
}

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity)

    @Query("SELECT * FROM goal WHERE archived = 0 ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goal WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GoalEntity?

    @Query("UPDATE goal SET currentAmountMinor = currentAmountMinor + :amountMinor, updatedAt = :updatedAt WHERE id = :id")
    suspend fun addContribution(id: String, amountMinor: Long, updatedAt: Long)

    @Query("SELECT * FROM goal")
    suspend fun getAllForBackup(): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun restoreGoals(goals: List<GoalEntity>): List<Long>
}

@Dao
interface DebtDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(debt: DebtEntity)

    @Query("SELECT * FROM debt WHERE archived = 0 ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debt")
    suspend fun getAllForBackup(): List<DebtEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun restoreDebts(debts: List<DebtEntity>): List<Long>
}

@Dao
interface ChitFundDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chitFund: ChitFundEntity)

    @Query("SELECT * FROM chit_fund WHERE active = 1 ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<ChitFundEntity>>

    @Query("SELECT * FROM chit_fund WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ChitFundEntity?

    @Query("UPDATE chit_fund SET active = :active, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, active: Boolean, updatedAt: Long)

    @Query("SELECT * FROM chit_fund")
    suspend fun getAllForBackup(): List<ChitFundEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun restoreChitFunds(chitFunds: List<ChitFundEntity>): List<Long>
}

@Dao
interface RecurringTxnDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurringTxn: RecurringTxnEntity)

    @Query("SELECT * FROM recurring_txn WHERE active = 1 ORDER BY nextRunDate ASC")
    fun observeActive(): Flow<List<RecurringTxnEntity>>

    @Query("SELECT * FROM recurring_txn")
    suspend fun getAllForBackup(): List<RecurringTxnEntity>

    @Query("SELECT * FROM recurring_txn WHERE id = :id")
    suspend fun get(id: String): RecurringTxnEntity?

    @Query("UPDATE recurring_txn SET active = :active, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setActive(id: String, active: Boolean, updatedAt: Long): Int

    @Query("UPDATE recurring_txn SET nextRunDate = :nextRunDate, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNextRunDate(id: String, nextRunDate: String, updatedAt: Long): Int

    @Query("DELETE FROM recurring_txn WHERE id = :id")
    suspend fun delete(id: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun restoreRecurringTxns(txns: List<RecurringTxnEntity>): List<Long>
}


@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: AppNotificationEntity)

    @Query("SELECT * FROM app_notification ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AppNotificationEntity>>

    @Query("SELECT * FROM app_notification WHERE read = 0 ORDER BY createdAt DESC")
    fun observeUnread(): Flow<List<AppNotificationEntity>>

    @Query("SELECT * FROM app_notification WHERE category = :category ORDER BY createdAt DESC")
    fun observeByCategory(category: String): Flow<List<AppNotificationEntity>>

    @Query("SELECT COUNT(*) FROM app_notification WHERE read = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("UPDATE app_notification SET read = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE app_notification SET read = 1 WHERE read = 0")
    suspend fun markAllAsRead()

    @Query("DELETE FROM app_notification WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM app_notification")
    suspend fun clearAll()
}

@Dao
interface SalaryCycleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cycle: SalaryCycleEntity)

    @Query("SELECT * FROM salary_cycles ORDER BY cycleStartDate DESC")
    fun observeAll(): Flow<List<SalaryCycleEntity>>

    @Query("SELECT * FROM salary_cycles ORDER BY cycleStartDate DESC LIMIT 1")
    suspend fun getLatest(): SalaryCycleEntity?

    @Query("SELECT * FROM salary_cycles WHERE cycleStartDate <= :date ORDER BY cycleStartDate DESC LIMIT 1")
    suspend fun getForDate(date: String): SalaryCycleEntity?

    @Query("SELECT * FROM salary_cycles WHERE id = :id")
    suspend fun getById(id: String): SalaryCycleEntity?

    @Query("DELETE FROM salary_cycles WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(
    entities = [
        AccountEntity::class, CategoryEntity::class, TxnEntity::class, JournalEntryEntity::class,
        GoalEntity::class, DebtEntity::class, ChitFundEntity::class, RecurringTxnEntity::class,
        AppNotificationEntity::class, SalaryCycleEntity::class,
    ],
    version = 5,
    autoMigrations = [
        androidx.room.AutoMigration(from = 1, to = 2),
        androidx.room.AutoMigration(from = 2, to = 3),
        androidx.room.AutoMigration(from = 3, to = 4),
        androidx.room.AutoMigration(from = 4, to = 5),
    ],
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun goalDao(): GoalDao
    abstract fun debtDao(): DebtDao
    abstract fun chitFundDao(): ChitFundDao
    abstract fun recurringTxnDao(): RecurringTxnDao
    abstract fun notificationDao(): NotificationDao
    abstract fun salaryCycleDao(): SalaryCycleDao
}


