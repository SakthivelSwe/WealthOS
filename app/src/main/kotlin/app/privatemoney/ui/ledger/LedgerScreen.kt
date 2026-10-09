package app.privatemoney.ui.ledger

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import app.privatemoney.R
import app.privatemoney.ui.components.EmptyState
import app.privatemoney.ui.components.Hairline
import app.privatemoney.ui.components.SectionLabel
import app.privatemoney.ui.components.TransactionRow
import app.privatemoney.ui.components.dateLabel
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerRoute(viewModel: LedgerViewModel) {
    val pagingItems = viewModel.pagingData.collectAsLazyPagingItems()
    var pendingActionTxn by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val context = LocalContext.current

    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::updateSearchQuery,
            label = { Text("Search transactions...") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
        )

        if (pagingItems.itemCount == 0 && pagingItems.loadState.refresh is LoadState.NotLoading) {
            EmptyState(
                stringResource(R.string.no_transactions_title),
                stringResource(R.string.no_transactions_body),
                Modifier.padding(20.dp),
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp)) {
                items(
                    count = pagingItems.itemCount,
                    key = pagingItems.itemKey { item ->
                        when (item) {
                            is LedgerItem.Header -> "h_${item.date}"
                            is LedgerItem.Transaction -> "t_${item.row.id}"
                        }
                    }
                ) { index ->
                    val item = pagingItems[index]
                    if (item != null) {
                        when (item) {
                            is LedgerItem.Header -> {
                                SectionLabel(dateLabel(item.date), Modifier.padding(top = 16.dp, bottom = 4.dp))
                            }
                            is LedgerItem.Transaction -> {
                                TransactionRow(item.row, onClick = { pendingActionTxn = item.row.id })
                                Hairline()
                            }
                        }
                    }
                }
            }
        }
    }

    pendingActionTxn?.let { id ->
        ModalBottomSheet(onDismissRequest = { pendingActionTxn = null }) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Transaction Actions", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.padding(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Button(onClick = { 
                        Toast.makeText(context, "Edit form coming soon", Toast.LENGTH_SHORT).show()
                        pendingActionTxn = null
                    }) {
                        Text("Edit")
                    }
                    Button(onClick = { 
                        pendingDelete = id
                        pendingActionTxn = null
                    }, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.error)) {
                        Text("Delete")
                    }
                }
                Spacer(modifier = Modifier.padding(16.dp))
            }
        }
    }

    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(id); pendingDelete = null }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
