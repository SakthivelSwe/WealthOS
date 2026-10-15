package app.privatemoney.data

import app.privatemoney.data.local.AppDatabase
import app.privatemoney.data.local.ChitFundEntity
import app.privatemoney.data.local.DebtEntity
import app.privatemoney.data.local.GoalEntity
import app.privatemoney.data.local.RecurringTxnEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

import app.privatemoney.domain.planning.RecurrenceEngine
import app.privatemoney.domain.planning.RecurrenceFrequency
import java.time.LocalDate

class PlanningRepository(private val db: AppDatabase, private val clock: () -> Long = System::currentTimeMillis) {

    // --- Goals ---
    fun observeGoals(): Flow<List<GoalEntity>> = db.goalDao().observeActive()

    suspend fun createGoal(
        name: String,
        targetAmountMinor: Long,
        currentAmountMinor: Long,
        plannedMonthlyContributionMinor: Long,
        currency: String,
        targetDate: String
    ) {
        val now = clock()
        val entity = GoalEntity(
            id = "goal_" + UUID.randomUUID().toString().replace("-", ""),
            name = name,
            targetAmountMinor = targetAmountMinor,
            currentAmountMinor = currentAmountMinor,
            plannedMonthlyContributionMinor = plannedMonthlyContributionMinor,
            currency = currency,
            targetDate = targetDate,
            archived = false,
            createdAt = now,
            updatedAt = now,
        )
        db.goalDao().insert(entity)
    }

    suspend fun contributeToGoal(id: String, amountMinor: Long) {
        db.goalDao().addContribution(id, amountMinor, clock())
    }

    suspend fun updateChitFundStatus(id: String, active: Boolean) {
        db.chitFundDao().updateStatus(id, active, clock())
    }
    fun observeDebts(): Flow<List<DebtEntity>> = db.debtDao().observeActive()

    suspend fun createDebt(
        name: String,
        principalMinor: Long,
        currentBalanceMinor: Long,
        annualPercent: String,
        expectedMonthlyPaymentMinor: Long,
        currency: String
    ) {
        val now = clock()
        val entity = DebtEntity(
            id = "debt_" + UUID.randomUUID().toString().replace("-", ""),
            name = name,
            principalMinor = principalMinor,
            currentBalanceMinor = currentBalanceMinor,
            annualPercent = annualPercent,
            expectedMonthlyPaymentMinor = expectedMonthlyPaymentMinor,
            currency = currency,
            archived = false,
            createdAt = now,
            updatedAt = now,
        )
        db.debtDao().insert(entity)
    }

    // --- Chit Funds ---
    fun observeChitFunds(): Flow<List<ChitFundEntity>> = db.chitFundDao().observeActive()

    suspend fun createChitFund(
        name: String,
        totalMonths: Int,
        startMonth: String,
        monthlyContributionMinor: Long,
        currency: String
    ) {
        val now = clock()
        val entity = ChitFundEntity(
            id = "chit_" + UUID.randomUUID().toString().replace("-", ""),
            name = name,
            totalMonths = totalMonths,
            startMonth = startMonth,
            monthlyContributionMinor = monthlyContributionMinor,
            currency = currency,
            active = true,
            createdAt = now,
            updatedAt = now,
        )
        db.chitFundDao().insert(entity)
    }

    // --- Recurring Transactions ---
    fun observeRecurringTxns(): Flow<List<RecurringTxnEntity>> = db.recurringTxnDao().observeActive()

    suspend fun createRecurringTxn(
        type: String,
        amountMinor: Long,
        currency: String,
        accountId: String,
        categoryId: String?,
        description: String?,
        frequency: String,
        nextRunDate: String,
        autoPost: Boolean = false,
    ) {
        val now = clock()
        val entity = RecurringTxnEntity(
            id = "recur_" + UUID.randomUUID().toString().replace("-", ""),
            type = type,
            amountMinor = amountMinor,
            currency = currency,
            accountId = accountId,
            categoryId = categoryId,
            description = description,
            frequency = frequency,
            nextRunDate = nextRunDate,
            active = true,
            createdAt = now,
            updatedAt = now,
            autoPost = autoPost,
        )
        db.recurringTxnDao().insert(entity)
    }

    suspend fun setRecurringTxnActive(id: String, active: Boolean) {
        db.recurringTxnDao().setActive(id, active, clock())
    }

    suspend fun skipRecurringTxnOnce(id: String) {
        val txn = db.recurringTxnDao().get(id) ?: return
        val currentRunDate = try {
            LocalDate.parse(txn.nextRunDate)
        } catch (_: Exception) {
            LocalDate.now()
        }
        val freq = RecurrenceFrequency.fromString(txn.frequency)
        val nextDate = RecurrenceEngine.nextOccurrenceDate(currentRunDate, freq, currentRunDate.dayOfMonth)
        db.recurringTxnDao().updateNextRunDate(id, nextDate.toString(), clock())
    }

    suspend fun deleteRecurringTxn(id: String) {
        db.recurringTxnDao().delete(id)
    }
}
