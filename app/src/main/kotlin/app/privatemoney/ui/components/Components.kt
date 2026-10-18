package app.privatemoney.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.privatemoney.R
import app.privatemoney.domain.ledger.TransactionType
import app.privatemoney.domain.model.Currency
import app.privatemoney.ui.MoneyFormat
import app.privatemoney.ui.ledger.TransactionRowUi
import app.privatemoney.ui.theme.LocalFinanceColors
import app.privatemoney.ui.theme.LocalPrivacyMask
import app.privatemoney.ui.theme.MoneyStyles
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

enum class MoneyTint { NEUTRAL, POSITIVE, NEGATIVE, PRIMARY, MUTED }

@Composable
fun MoneyText(
    minor: Long,
    currency: Currency,
    modifier: Modifier = Modifier,
    style: TextStyle = MoneyStyles.row,
    tint: MoneyTint = MoneyTint.NEUTRAL,
    showPlus: Boolean = false,
    forceUnmasked: Boolean = false,
) {
    val finance = LocalFinanceColors.current
    val isMasked = LocalPrivacyMask.current && !forceUnmasked

    val color = when (tint) {
        MoneyTint.NEUTRAL -> MaterialTheme.colorScheme.onBackground
        MoneyTint.POSITIVE -> finance.positive
        MoneyTint.NEGATIVE -> finance.negative
        MoneyTint.PRIMARY -> MaterialTheme.colorScheme.primary
        MoneyTint.MUTED -> finance.muted
    }

    val display = if (isMasked) {
        "${MoneyFormat.symbol(currency)} • • • •"
    } else {
        MoneyFormat.format(minor, currency, showPlus)
    }

    Text(
        text = display,
        modifier = modifier,
        color = color,
        style = style,
        maxLines = 1,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = LocalFinanceColors.current.muted,
        letterSpacing = 1.3.sp,
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = LocalFinanceColors.current.hairline,
    )
}

/** Glassmorphic card surface with glowing subtle border and curved corners. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    borderColor: Color = LocalFinanceColors.current.glassBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val cardModifier = modifier
        .clip(shape)
        .background(backgroundColor)
        .border(1.dp, borderColor, shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Box(modifier = cardModifier) {
        content()
    }
}

/** Sleek modern progress bar with animated fill and gradient styling. */
@Composable
fun LinearProgressModern(
    progress: Float,
    modifier: Modifier = Modifier,
    brush: Brush = Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
    trackColor: Color = LocalFinanceColors.current.hairline,
    height: Dp = 8.dp,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "progressAnimation",
    )
    val shape = RoundedCornerShape(height / 2)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(height)
                .clip(shape)
                .background(brush),
        )
    }
}

/** Modern category glyph with vibrant gradient squircle container. */
@Composable
fun CategoryGlyph(
    label: String,
    modifier: Modifier = Modifier,
    type: TransactionType = TransactionType.EXPENSE,
    customColor: Color? = null,
) {
    val finance = LocalFinanceColors.current
    val baseColor = customColor ?: when (type) {
        TransactionType.INCOME -> finance.positive
        TransactionType.EXPENSE -> Color(0xFF818CF8)
        TransactionType.TRANSFER -> finance.info
    }

    val glyph = if (label.contains(" ")) {
        label.split(" ").first()
    } else if (label.length >= 2 && label.first().isSurrogate()) {
        label.take(2)
    } else {
        label.take(1).uppercase()
    }

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(baseColor.copy(alpha = 0.16f))
            .border(1.dp, baseColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            fontSize = if (glyph.length <= 2 && !glyph.all { it.isLetter() }) 19.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (glyph.all { it.isLetter() }) baseColor else Color.Unspecified,
        )
    }
}

/** Modern transaction item row with category glyph, merchant title, and signed amount badge. */
@Composable
fun TransactionRow(
    row: TransactionRowUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeWord = stringResource(
        when (row.type) {
            TransactionType.EXPENSE -> R.string.type_expense
            TransactionType.INCOME -> R.string.type_income
            TransactionType.TRANSFER -> R.string.type_transfer
        },
    )
    val title = row.merchant?.ifBlank { null } ?: row.description?.ifBlank { null } ?: row.categoryName ?: typeWord
    val meta = listOfNotNull(row.categoryName ?: typeWord, dateLabel(row.date)).joinToString(" • ")
    val signed = when (row.type) {
        TransactionType.EXPENSE -> -row.amountMinor
        else -> row.amountMinor
    }
    val tint = when (row.type) {
        TransactionType.EXPENSE -> MoneyTint.NEUTRAL
        TransactionType.INCOME -> MoneyTint.POSITIVE
        TransactionType.TRANSFER -> MoneyTint.MUTED
    }
    val spoken = "$typeWord, $title, ${MoneyFormat.format(row.amountMinor, row.currency)}, $meta"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .semantics(mergeDescendants = true) { contentDescription = spoken },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryGlyph(label = row.categoryName ?: typeWord, type = row.type)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = LocalFinanceColors.current.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            MoneyText(
                minor = signed,
                currency = row.currency,
                style = MoneyStyles.row,
                tint = tint,
                showPlus = row.type == TransactionType.INCOME,
            )
            if (row.type == TransactionType.TRANSFER) {
                Text(
                    text = "Transfer",
                    style = TextStyle(fontSize = 11.sp, color = LocalFinanceColors.current.muted),
                )
            }
        }
    }
}

/** Modern empty state with glowing pill icon container, title, and description. */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = LocalFinanceColors.current.muted,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalFinanceColors.current.muted,
            lineHeight = 20.sp,
        )
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
