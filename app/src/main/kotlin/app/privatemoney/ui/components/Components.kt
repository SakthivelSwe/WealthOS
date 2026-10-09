package app.privatemoney.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.privatemoney.R
import app.privatemoney.domain.ledger.TransactionType
import app.privatemoney.domain.model.Currency
import app.privatemoney.ui.MoneyFormat
import app.privatemoney.ui.ledger.TransactionRowUi
import app.privatemoney.ui.theme.LocalFinanceColors
import app.privatemoney.ui.theme.MoneyStyles
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

enum class MoneyTint { NEUTRAL, POSITIVE, NEGATIVE }

@Composable
fun MoneyText(
    minor: Long,
    currency: Currency,
    modifier: Modifier = Modifier,
    style: TextStyle = MoneyStyles.row,
    tint: MoneyTint = MoneyTint.NEUTRAL,
    showPlus: Boolean = false,
) {
    val finance = LocalFinanceColors.current
    val color = when (tint) {
        MoneyTint.NEUTRAL -> MaterialTheme.colorScheme.onBackground
        MoneyTint.POSITIVE -> finance.positive
        MoneyTint.NEGATIVE -> finance.negative
    }
    Text(MoneyFormat.format(minor, currency, showPlus), modifier = modifier, color = color, style = style, maxLines = 1)
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = LocalFinanceColors.current.muted,
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = LocalFinanceColors.current.hairline)
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = LocalFinanceColors.current.muted)
    }
}

@Composable
fun dateLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> stringResource(R.string.today)
        today.minusDays(1) -> stringResource(R.string.yesterday)
        else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
}

@Composable
private fun CategoryGlyph(label: String) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(label.take(1).uppercase(), style = MaterialTheme.typography.titleSmall, color = LocalFinanceColors.current.muted)
    }
}

/** A single ledger line: glyph, title, metadata, signed amount aligned right. */
@Composable
fun TransactionRow(row: TransactionRowUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val typeWord = stringResource(
        when (row.type) {
            TransactionType.EXPENSE -> R.string.type_expense
            TransactionType.INCOME -> R.string.type_income
            TransactionType.TRANSFER -> R.string.type_transfer
        },
    )
    val title = row.merchant ?: row.description ?: row.categoryName ?: typeWord
    val meta = listOfNotNull(row.categoryName ?: typeWord, dateLabel(row.date)).joinToString(" \u00B7 ")
    val signed = when (row.type) {
        TransactionType.EXPENSE -> -row.amountMinor
        else -> row.amountMinor
    }
    val tint = when (row.type) {
        TransactionType.EXPENSE -> MoneyTint.NEGATIVE
        TransactionType.INCOME -> MoneyTint.POSITIVE
        TransactionType.TRANSFER -> MoneyTint.NEUTRAL
    }
    val spoken = "$typeWord, $title, ${MoneyFormat.format(row.amountMinor, row.currency)}, $meta"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
            .semantics(mergeDescendants = true) { contentDescription = spoken },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryGlyph(row.categoryName ?: typeWord)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(meta, style = MaterialTheme.typography.bodySmall, color = LocalFinanceColors.current.muted, maxLines = 1)
        }
        Spacer(Modifier.width(12.dp))
        MoneyText(signed, row.currency, tint = tint, showPlus = row.type == TransactionType.INCOME)
    }
}
