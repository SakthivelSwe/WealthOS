package app.privatemoney.ui.add

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.privatemoney.R
import app.privatemoney.domain.ledger.TransactionType
import app.privatemoney.domain.model.Currency
import app.privatemoney.ui.MoneyFormat
import app.privatemoney.ui.components.GlassCard
import app.privatemoney.ui.components.SectionLabel
import app.privatemoney.ui.theme.LocalFinanceColors
import app.privatemoney.ui.theme.MoneyStyles

@Composable
fun AddTransactionScreen(
    viewModel: AddViewModel,
    currency: Currency,
    initialDate: String? = null,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val finance = LocalFinanceColors.current

    LaunchedEffect(initialDate) {
        viewModel.reset()
        if (initialDate != null) {
            viewModel.setCustomDate(initialDate)
        }
    }
    LaunchedEffect(state.saved) {
        if (state.saved) {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            onClose()
        }
    }

    val typeColor by animateColorAsState(
        targetValue = when (state.type) {
            TransactionType.EXPENSE -> finance.negative
            TransactionType.INCOME -> finance.positive
            TransactionType.TRANSFER -> finance.info
        },
        animationSpec = tween(300),
        label = "typeColor",
    )

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Top Bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
            }

            TypeSwitcherPill(
                selected = state.type,
                onSelect = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.setType(it)
                },
            )

            Spacer(Modifier.size(48.dp)) // balance spacing
        }

        // Hero Amount Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = MoneyFormat.symbol(currency),
                    style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = typeColor.copy(alpha = 0.8f)),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = state.amountText.ifEmpty { "0" },
                    style = MoneyStyles.input,
                    color = if (state.amountText.isEmpty()) finance.muted else MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
            }
        }

        // Quick Date Selector (Today, Yesterday, Specific Date)
        DateSelectorRow(
            selectedDateStr = state.customDate,
            accentColor = typeColor,
            onSelectDate = { viewModel.setCustomDate(it) },
        )

        // Scrollable Options Form
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state.type != TransactionType.TRANSFER) {
                SectionLabel(stringResource(R.string.category))
                CategoryChipsGrid(
                    options = state.categories,
                    selectedId = state.categoryId,
                    accentColor = typeColor,
                    onSelect = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.setCategory(it)
                    },
                )
            }

            SectionLabel(stringResource(if (state.type == TransactionType.TRANSFER) R.string.from_account else R.string.account))
            AccountChipsGrid(
                options = state.accounts,
                selectedId = state.accountId,
                onSelect = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.setAccount(it)
                },
            )

            if (state.type == TransactionType.TRANSFER) {
                SectionLabel(stringResource(R.string.to_account))
                AccountChipsGrid(
                    options = state.accounts,
                    selectedId = state.toAccountId,
                    onSelect = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.setToAccount(it)
                    },
                )
            }

            OutlinedTextField(
                value = state.merchant,
                onValueChange = viewModel::setMerchant,
                label = { Text(stringResource(R.string.merchant_optional)) },
                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = finance.muted, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = typeColor,
                    unfocusedBorderColor = finance.glassBorder,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text(stringResource(R.string.note_optional)) },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = finance.muted, modifier = Modifier.size(18.dp)) },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = typeColor,
                    unfocusedBorderColor = finance.glassBorder,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Error message if any
        state.error?.let {
            Text(
                text = stringResource(errorText(it)),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
            )
        }

        // Financial Keypad
        Keypad(
            decimalEnabled = currency.minorUnit > 0,
            onKey = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.onKey(it)
            },
            onBackspace = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.onBackspace()
            },
        )

        // Submit Button
        Button(
            onClick = viewModel::save,
            enabled = !state.saving,
            colors = ButtonDefaults.buttonColors(containerColor = typeColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .height(54.dp),
        ) {
            Text(
                text = stringResource(
                    when (state.type) {
                        TransactionType.EXPENSE -> R.string.save_expense
                        TransactionType.INCOME -> R.string.save_income
                        TransactionType.TRANSFER -> R.string.save_transfer
                    },
                ),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (state.type == TransactionType.EXPENSE) Color.White else Color.Black,
            )
        }
    }
}

private fun errorText(error: AddError): Int = when (error) {
    AddError.AMOUNT_REQUIRED -> R.string.err_amount_required
    AddError.AMOUNT_INVALID -> R.string.err_amount_invalid
    AddError.CATEGORY_REQUIRED -> R.string.err_category_required
    AddError.ACCOUNT_REQUIRED -> R.string.err_account_required
    AddError.DESTINATION_REQUIRED -> R.string.err_destination_required
    AddError.SAME_ACCOUNT -> R.string.err_same_account
    AddError.SAVE_FAILED -> R.string.err_save_failed
}

@Composable
private fun TypeSwitcherPill(selected: TransactionType, onSelect: (TransactionType) -> Unit) {
    val finance = LocalFinanceColors.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, finance.glassBorder, RoundedCornerShape(20.dp))
            .padding(3.dp),
    ) {
        TransactionType.entries.forEach { type ->
            val active = type == selected
            val activeBg = when (type) {
                TransactionType.EXPENSE -> finance.negative
                TransactionType.INCOME -> finance.positive
                TransactionType.TRANSFER -> finance.info
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (active) activeBg else Color.Transparent)
                    .clickable { onSelect(type) }
                .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = when (type) {
                        TransactionType.EXPENSE -> "Expense"
                        TransactionType.INCOME -> "Income"
                        TransactionType.TRANSFER -> "Transfer"
                    },
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) (if (type == TransactionType.EXPENSE) Color.White else Color.Black) else finance.muted,
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChipsGrid(
    options: List<Option>,
    selectedId: String?,
    accentColor: Color,
    onSelect: (String) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val selected = option.id == selectedId
            FilterChip(
                selected = selected,
                onClick = { onSelect(option.id) },
                label = { Text(option.name, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor.copy(alpha = 0.2f),
                    selectedLabelColor = accentColor,
                ),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountChipsGrid(
    options: List<Option>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val selected = option.id == selectedId
            FilterChip(
                selected = selected,
                onClick = { onSelect(option.id) },
                label = { Text(option.name) },
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

@Composable
private fun Keypad(decimalEnabled: Boolean, onKey: (Char) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf("123", "456", "789", ".0<")
    Column(
        Modifier
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
    ) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { ch ->
                    when {
                        ch == '<' -> KeyCell(
                            label = "⌫",
                            description = stringResource(R.string.backspace),
                            isIcon = true,
                            onClick = onBackspace,
                        )
                        ch == '.' && !decimalEnabled -> Box(Modifier.weight(1f).height(50.dp))
                        else -> KeyCell(
                            label = ch.toString(),
                            description = ch.toString(),
                            isIcon = false,
                            onClick = { onKey(ch) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.KeyCell(
    label: String,
    description: String,
    isIcon: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .weight(1f)
            .height(50.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (isIcon) {
            Icon(Icons.Default.Backspace, contentDescription = description, modifier = Modifier.size(20.dp))
        } else {
            Text(
                text = label,
                style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
            )
        }
    }
}

@Composable
private fun DateSelectorRow(
    selectedDateStr: String?,
    accentColor: Color,
    onSelectDate: (String?) -> Unit,
) {
    val finance = LocalFinanceColors.current
    val today = remember { LocalDate.now() }
    val yesterday = remember { today.minusDays(1) }
    var showDatePicker by remember { mutableStateOf(false) }

    val activeDate = remember(selectedDateStr) {
        if (selectedDateStr != null) {
            runCatching { LocalDate.parse(selectedDateStr) }.getOrDefault(today)
        } else today
    }

    val isToday = selectedDateStr == null || activeDate == today
    val isYesterday = selectedDateStr != null && activeDate == yesterday
    val isCustom = selectedDateStr != null && !isToday && !isYesterday

    val customLabel = remember(activeDate, isCustom) {
        if (isCustom) {
            activeDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        } else {
            "Specific Date 📅"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DateChip(
            label = "Today",
            selected = isToday,
            accentColor = accentColor,
            onClick = { onSelectDate(null) },
        )

        DateChip(
            label = "Yesterday",
            selected = isYesterday,
            accentColor = accentColor,
            onClick = { onSelectDate(yesterday.toString()) },
        )

        DateChip(
            label = if (isCustom) "📅 $customLabel" else "Specific Date 📅",
            selected = isCustom,
            accentColor = accentColor,
            onClick = { showDatePicker = true },
        )
    }

    if (showDatePicker) {
        TransactionDatePickerDialog(
            initialDate = activeDate,
            accentColor = accentColor,
            onDateSelected = { picked ->
                if (picked == today) {
                    onSelectDate(null)
                } else {
                    onSelectDate(picked.toString())
                }
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
private fun DateChip(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val finance = LocalFinanceColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) accentColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(
                width = 1.dp,
                color = if (selected) accentColor else finance.glassBorder,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accentColor else finance.muted,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDatePickerDialog(
    initialDate: LocalDate,
    accentColor: Color,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialMillis = remember(initialDate) {
        initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onDateSelected(picked)
                    }
                    onDismiss()
                },
            ) {
                Text("Select", fontWeight = FontWeight.Bold, color = accentColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}
