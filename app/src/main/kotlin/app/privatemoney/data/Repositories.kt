package app.privatemoney.data

import androidx.room.withTransaction
import app.privatemoney.data.local.AccountEntity
import app.privatemoney.data.local.AppDatabase
import app.privatemoney.data.local.CategoryEntity
import app.privatemoney.domain.model.AccountType
import app.privatemoney.domain.model.Currency
import app.privatemoney.domain.model.DefaultCategories
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AccountRepository(private val db: AppDatabase, private val clock: () -> Long = System::currentTimeMillis) {

    fun observeActive(): Flow<List<AccountEntity>> = db.accountDao().observeActive()

    /** Creates an account. The opening balance is stored as-is; later changes flow only through the journal. */
    suspend fun create(name: String, type: AccountType, openingBalanceMinor: Long, currency: Currency): String {
        require(name.isNotBlank() && name.length <= MAX_NAME) { "Invalid name" }
        val now = clock()
        val id = UUID.randomUUID().toString()
        db.accountDao().insert(
            AccountEntity(
                id = id,
                name = name.trim(),
                type = type.name,
                institution = null,
                lastFour = null,
                openingBalanceMinor = openingBalanceMinor,
                currency = currency.code,
                includeInNetWorth = true,
                includeInStats = true,
                archived = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return id
    }

    private companion object {
        const val MAX_NAME = 60
    }
}

class CategoryRepository(private val db: AppDatabase) {

    fun observeActive(): Flow<List<CategoryEntity>> = db.categoryDao().observeActive()

    suspend fun seedDefaultsIfEmpty() {
        db.withTransaction {
            val dao = db.categoryDao()
            if (dao.count() > 0) return@withTransaction
            dao.insertAll(
                DefaultCategories.all.mapIndexed { index, c ->
                    CategoryEntity(
                        id = c.id,
                        parentId = c.parentId,
                        name = c.name,
                        kind = c.kind.name,
                        needWant = c.needWant?.name,
                        icon = "default",
                        color = c.color,
                        archived = false,
                        sortOrder = index,
                    )
                },
            )
        }
    }
}
