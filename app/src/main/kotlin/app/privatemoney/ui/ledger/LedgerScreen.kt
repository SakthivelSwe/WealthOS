package app.privatemoney.ui.ledger

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import app.privatemoney.R
import app.privatemoney.domain.ledger.TransactionType
import app.privatemoney.ui.components.CategoryGlyph
import app.privatemoney.ui.components.EmptyState
import app.privatemoney.ui.components.GlassCard
import app.privatemoney.ui.components.Hairline
import app.privatemoney.ui.components.MoneyText
import app.privatemoney.ui.components.MoneyTint
import app.privatemoney.ui.components.SectionLabel
import app.privatemoney.ui.components.TransactionRow
import app.privatemoney.ui.components.dateLabel
import app.privatemoney.ui.theme.LocalFinanceColors
import app.privatemoney.ui.theme.MoneyStyles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerRoute(viewModel: LedgerViewModel) {
    val pagingItems = viewModel.pagingData.collectAsLazyPagingItems()
    var selectedTxn by remember { mutableStateOf<TransactionRowUi?>(null) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    var isEditing by remember { mutableStateOf(false) }

    val searchQuery by viewModel.searchQuery.collectAsState()
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) }
    val context = LocalContext.current
    val finance = LocalFinanceColors.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Search Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Transaction Ledger",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            // Modern Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                placeholder = { Text("Search by merchant, note, or tag...", color = finance.muted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = finance.muted,
                        modifier = Modifier.size(20.dp),
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = finance.muted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = finance.glassBorder,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Quick Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedTypeFilter == null,
                    onClick = { selectedTypeFilter = null },
                    label = { Text("All") },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                )
                FilterChip(
                    selected = selectedTypeFilter == TransactionType.EXPENSE,
                    onClick = { selectedTypeFilter = if (selectedTypeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE },
                    label = { Text("Expenses") },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = finance.negative.copy(alpha = 0.2f),
                        selectedLabelColor = finance.negative,
                    ),
                )
                FilterChip(
                    selected = selectedTypeFilter == TransactionType.INCOME,
                    onClick = { selectedTypeFilter = if (selectedTypeFilter == TransactionType.INCOME) null else TransactionType.INCOME },
                    label = { Text("Income") },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = finance.positive.copy(alpha = 0.2f),
                        selectedLabelColor = finance.positive,
                    ),
                )
                FilterChip(
                    selected = selectedTypeFilter == TransactionType.TRANSFER,
                    onClick = { selectedTypeFilter = if (selectedTypeFilter == TransactionType.TRANSFER) null else TransactionType.TRANSFER },
                    label = { Text("Transfers") },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = finance.info.copy(alpha = 0.2f),
                        selectedLabelColor = finance.info,
                    ),
                )
            }
        }

        if (pagingItems.itemCount == 0 && pagingItems.loadState.refresh is LoadState.NotLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = stringResource(R.string.no_transactions_title),
                        body = if (searchQuery.isNotEmpty()) "No transactions found matching \"$searchQuery\"." else stringResource(R.string.no_transactions_body),
                        icon = Icons.Default.ReceiptLong,
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    count = pagingItems.itemCount,
                    key = pagingItems.itemKey { item ->
                        when (item) {
                            is LedgerItem.Header -> "h_${item.date}"
                            is LedgerItem.Transaction -> "t_${item.row.id}"
                        }
                    },
                ) { index ->
                    val item = pagingItems[index]
                    if (item != null) {
                        when (item) {
                            is LedgerItem.Header -> {
                                SectionLabel(
                                    text = dateLabel(item.date),
                                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                                )
                            }
                            is LedgerItem.Transaction -> {
                                val matchType = selectedTypeFilter == null || item.row.type == selectedTypeFilter
                                if (matchType) {
                                    GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { selectedTxn = item.row },
                                    ) {
                                        TransactionRow(
                                            row = item.row,
                                            onClick = { selectedTxn = item.row },
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail & Edit BottomSheet
    selectedTxn?.let { txn ->
        ModalBottomSheet(
            onDismissRequest = {
                selectedTxn = null
                isEditing = false
            },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            TransactionDetailSheet(
                txn = txn,
                isEditing = isEditing,
                onStartEdit = { isEditing = true },
                onCancelEdit = { isEditing = false },
                onSaveEdit = { newMerchant, newDescription, newNotes ->
                    viewModel.update(
                        id = txn.id,
                        merchant = newMerchant.trim().ifEmpty { null },
                        description = newDescription.trim().ifEmpty { null },
                        notes = newNotes.trim().ifEmpty { null },
                    )
                    Toast.makeText(context, "Transaction updated!", Toast.LENGTH_SHORT).show()
                    isEditing = false
                    selectedTxn = null
                    pagingItems.refresh()
                },
                onDelete = {
                    pendingDelete = txn.id
                    selectedTxn = null
                    isEditing = false
                },
            )
        }
    }

    // Delete Confirmation Dialog
    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.delete(id)
                        pendingDelete = null
                        pagingItems.refresh()
                        Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

/** Detail & Edit BottomSheet Content */
@Composable
private fun TransactionDetailSheet(
    txn: TransactionRowUi,
    isEditing: Boolean,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: (String, String, String) -> Unit,
    onDelete: () -> Unit,
) {
    val finance = LocalFinanceColors.current
    var editMerchant by remember(txn) { mutableStateOf(txn.merchant ?: "") }
    var editDescription by remember(txn) { mutableStateOf(txn.description ?: "") }
    var editNotes by remember(txn) { mutableStateOf(txn.notes ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Top Icon & Amount
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CategoryGlyph(
                label = txn.categoryName ?: txn.type.name,
                type = txn.type,
                modifier = Modifier.size(54.dp),
            )

            Column(horizontalAlignment = Alignment.End) {
                val signed = when (txn.type) {
                    TransactionType.EXPENSE -> -txn.amountMinor
                    else -> txn.amountMinor
                }
                MoneyText(
                    minor = signed,
                    currency = txn.currency,
                    style = MoneyStyles.hero,
                    tint = when (txn.type) {
                        TransactionType.EXPENSE -> MoneyTint.NEUTRAL
                        TransactionType.INCOME -> MoneyTint.POSITIVE
                        TransactionType.TRANSFER -> MoneyTint.MUTED
                    },
                    showPlus = txn.type == TransactionType.INCOME,
                )
                Text(
                    text = txn.type.name,
                    style = TextStyle(fontSize = 12.sp, color = finance.muted, fontWeight = FontWeight.Bold),
                )
            }
        }

        Hairline()

        if (!isEditing) {
            // View Mode
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (!txn.merchant.isNullOrBlank()) {
                        DetailLine("Merchant", txn.merchant)
                    }
                    if (!txn.description.isNullOrBlank()) {
                        DetailLine("Description", txn.description)
                    } else if (txn.merchant.isNullOrBlank()) {
                        DetailLine("Description", "None")
                    }
                    DetailLine("Category", txn.categoryName ?: "Uncategorized")
                    DetailLine("Date", dateLabel(txn.date))
                    if (!txn.notes.isNullOrBlank()) {
                        DetailLine("Note", txn.notes)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onStartEdit,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit")
                }

                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete")
                }
            }
        } else {
            // Inline Edit Mode
            Text("Edit Transaction Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = editMerchant,
                onValueChange = { editMerchant = it },
                label = { Text("Merchant / Payee") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = editDescription,
                onValueChange = { editDescription = it },
                label = { Text("Description / Purpose") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = editNotes,
                onValueChange = { editNotes = it },
                label = { Text("Notes (optional)") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onCancelEdit,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = { onSaveEdit(editMerchant, editDescription, editNotes) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Save Changes")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    val finance = LocalFinanceColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = finance.muted)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
