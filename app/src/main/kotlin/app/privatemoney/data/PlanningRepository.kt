package app.privatemoney.data

import app.privatemoney.data.local.AppDatabase
import app.privatemoney.data.local.ChitFundEntity
import app.privatemoney.data.local.DebtEntity
import app.privatemoney.data.local.GoalEntity
import app.privatemoney.data.local.RecurringTxnEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

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

    // --- Debts ---
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
        nextRunDate: String
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
        )
        db.recurringTxnDao().insert(entity)
    }
}
