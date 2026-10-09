package app.privatemoney.ui.ledger

import androidx.compose.runtime.Immutable
import app.privatemoney.data.local.TxnEntity
import app.privatemoney.domain.ledger.TransactionType
import app.privatemoney.domain.model.Currency
import java.time.LocalDate

@Immutable
data class TransactionRowUi(
    val id: String,
    val type: TransactionType,
    val merchant: String?,
    val description: String?,
    val categoryName: String?,
    val date: LocalDate,
    val amountMinor: Long,
    val currency: Currency,
)

internal fun TxnEntity.toRowUi(categoryNames: Map<String, String>): TransactionRowUi = TransactionRowUi(
    id = id,
    type = TransactionType.valueOf(type),
    merchant = merchant,
    description = description,
    categoryName = categoryId?.let(categoryNames::get),
    date = LocalDate.parse(localDate),
    amountMinor = amountMinor,
    currency = Currency.fromCode(currency) ?: Currency.INR,
)
