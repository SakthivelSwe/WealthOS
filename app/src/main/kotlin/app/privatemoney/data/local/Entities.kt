package app.privatemoney.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "account")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val institution: String?,
    val lastFour: String?,
    val openingBalanceMinor: Long,
    val currency: String,
    val includeInNetWorth: Boolean,
    val includeInStats: Boolean,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    @androidx.room.ColumnInfo(defaultValue = "NULL") val accountRole: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "NULL") val bankName: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "0") val interestRatePercentBps: Int = 0,
    @androidx.room.ColumnInfo(defaultValue = "0") val monthlyEmiPaise: Long = 0L,
    @androidx.room.ColumnInfo(defaultValue = "0") val outstandingPaise: Long = 0L,
    @androidx.room.ColumnInfo(defaultValue = "0") val isBnpl: Boolean = false,
)

@Entity(tableName = "category", indices = [Index("parentId")])
data class CategoryEntity(
    @PrimaryKey val id: String,
    val parentId: String?,
    val name: String,
    val kind: String,
    val needWant: String?,
    val icon: String,
    val color: Long,
    val archived: Boolean,
    val sortOrder: Int,
)

@Entity(
    tableName = "txn",
    indices = [
        Index("localDate"),
        Index("accountId", "localDate"),
        Index("categoryId"),
        Index("type"),
        Index("merchant"),
        Index("createdAt"),
        Index("updatedAt"),
        Index("rrn"),
    ],
)
data class TxnEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amountMinor: Long,
    val currency: String,
    val localDate: String,
    val localTime: String,
    val accountId: String,
    val toAccountId: String?,
    val categoryId: String?,
    val merchant: String?,
    val description: String?,
    val notes: String?,
    val paymentMethod: String?,
    val needWant: String?,
    val imported: Boolean,
    val source: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    @androidx.room.ColumnInfo(defaultValue = "NULL") val jarBucket: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "NULL") val spendEmotionTag: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "NULL") val paymentRail: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "NULL") val rrn: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "NULL") val counterpartyName: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "NULL") val vpa: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "NULL") val importBatchId: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "0") val userOverridden: Boolean = false,
    @androidx.room.ColumnInfo(defaultValue = "0") val isRecurringMandate: Boolean = false,
)

@Entity(
    tableName = "salary_cycles",
    indices = [
        Index("cycleStartDate"),
        Index("userId", "cycleStartDate", unique = true),
    ],
)
data class SalaryCycleEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val cycleStartDate: String,
    val salaryPaise: Long,
    val baselinePaise: Long,
    val emiPaise: Long = 0L,
    val framework: String,
    val jarAmountsJson: String,
    val daysInCycle: Int,
    val dailySafeUpiPaise: Long = 0L,
    val netWorthSnapshotPaise: Long? = null,
    val checklistStateJson: String = "{}",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "journal_entry",
    foreignKeys = [ForeignKey(TxnEntity::class, ["id"], ["txnId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("txnId"), Index("accountId"), Index("categoryId")],
)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val txnId: String,
    val accountId: String?,
    val categoryId: String?,
    val amountMinor: Long,
)

@Entity(tableName = "goal")
data class GoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetAmountMinor: Long,
    val currentAmountMinor: Long,
    val plannedMonthlyContributionMinor: Long,
    val currency: String,
    val targetDate: String,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "debt")
data class DebtEntity(
    @PrimaryKey val id: String,
    val name: String,
    val principalMinor: Long,
    val currentBalanceMinor: Long,
    val annualPercent: String,
    val expectedMonthlyPaymentMinor: Long,
    val currency: String,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "chit_fund")
data class ChitFundEntity(
    @PrimaryKey val id: String,
    val name: String,
    val totalMonths: Int,
    val startMonth: String,
    val monthlyContributionMinor: Long,
    val currency: String,
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "recurring_txn")
data class RecurringTxnEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amountMinor: Long,
    val currency: String,
    val accountId: String,
    val categoryId: String?,
    val description: String?,
    val frequency: String, // e.g., "MONTHLY", "WEEKLY"
    val nextRunDate: String,
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    @androidx.room.ColumnInfo(defaultValue = "0") val autoPost: Boolean = false,
)

@Entity(
    tableName = "app_notification",
    indices = [
        Index("createdAt"),
        Index("category"),
        Index("read"),
    ],
)
data class AppNotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val category: String, // "CHECK_IN", "BILL", "BUDGET", "GOAL", "BACKUP", "SECURITY"
    val actionType: String? = null, // "ADD_EXPENSE", "REVIEW_DAY", "VIEW_BILL", "VIEW_BUDGET", "RETRY_BACKUP", etc.
    val payload: String? = null,
    val read: Boolean = false,
    val createdAt: Long,
)

