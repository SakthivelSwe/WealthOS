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

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: AccountEntity)

    @Query("SELECT * FROM account WHERE archived = 0 ORDER BY name")
    fun observeActive(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM account WHERE id = :id")
    suspend fun get(id: String): AccountEntity?
}

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("SELECT COUNT(*) FROM category")
    suspend fun count(): Int

    @Query("SELECT * FROM category WHERE archived = 0 ORDER BY sortOrder")
    fun observeActive(): Flow<List<CategoryEntity>>
}

@Dao
interface LedgerDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTxn(txn: TxnEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntries(entries: List<JournalEntryEntity>)

    @Query("UPDATE txn SET deletedAt = :at, updatedAt = :at WHERE id = :id AND deletedAt IS NULL")
    suspend fun softDelete(id: String, at: Long): Int

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
     * (BANK, CASH, UPI_WALLET, SAVINGS).
     */
    @Query(
        """
        SELECT
          (SELECT COALESCE(SUM(openingBalanceMinor), 0) FROM account
             WHERE archived = 0 AND type IN ('BANK','CASH','UPI_WALLET','SAVINGS'))
          +
          (SELECT COALESCE(SUM(j.amountMinor), 0) FROM journal_entry j
             JOIN txn t ON t.id = j.txnId
             JOIN account a ON a.id = j.accountId
             WHERE t.deletedAt IS NULL AND a.archived = 0
               AND a.type IN ('BANK','CASH','UPI_WALLET','SAVINGS'))
        """,
    )
    fun observeLiquidBalance(): Flow<Long>

    @Query(
        """
        SELECT COALESCE(SUM(amountMinor), 0) FROM txn
        WHERE type = :type AND deletedAt IS NULL AND localDate >= :fromDate AND localDate < :toDate
        """,
    )
    fun observeTotal(type: String, fromDate: String, toDate: String): Flow<Long>

    @Query(
        """
        SELECT needWant AS needWant, COALESCE(SUM(amountMinor), 0) AS total FROM txn
        WHERE type = 'EXPENSE' AND deletedAt IS NULL AND localDate >= :fromDate AND localDate < :toDate
        GROUP BY needWant
        """,
    )
    fun observeLaneTotals(fromDate: String, toDate: String): Flow<List<LaneTotalRow>>

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
          AND type IN ('INCOME','EXPENSE')
          AND localDate >= :fromDate
        GROUP BY month, type
        ORDER BY month ASC
        """,
    )
    fun observeMonthlyTotals(fromDate: String): Flow<List<MonthlyTotalRow>>

    /** Top expense categories for a date range. */
    @Query(
        """
        SELECT categoryId, COALESCE(SUM(amountMinor), 0) AS total
        FROM txn
        WHERE type = 'EXPENSE' AND deletedAt IS NULL
          AND localDate >= :fromDate AND localDate < :toDate
        GROUP BY categoryId
        ORDER BY total DESC
        LIMIT :limit
        """,
    )
    fun observeTopCategories(fromDate: String, toDate: String, limit: Int): Flow<List<CategorySpendRow>>
}

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity)

    @Query("SELECT * FROM goal WHERE archived = 0 ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<GoalEntity>>
}

@Dao
interface DebtDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(debt: DebtEntity)

    @Query("SELECT * FROM debt WHERE archived = 0 ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<DebtEntity>>
}

@Dao
interface ChitFundDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chitFund: ChitFundEntity)

    @Query("SELECT * FROM chit_fund WHERE active = 1 ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<ChitFundEntity>>
}

@Dao
interface RecurringTxnDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurringTxn: RecurringTxnEntity)

    @Query("SELECT * FROM recurring_txn WHERE active = 1 ORDER BY nextRunDate ASC")
    fun observeActive(): Flow<List<RecurringTxnEntity>>
}

@Database(
    entities = [
        AccountEntity::class, CategoryEntity::class, TxnEntity::class, JournalEntryEntity::class,
        GoalEntity::class, DebtEntity::class, ChitFundEntity::class, RecurringTxnEntity::class
    ],
    version = 2,
    autoMigrations = [
        androidx.room.AutoMigration(from = 1, to = 2)
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
}
