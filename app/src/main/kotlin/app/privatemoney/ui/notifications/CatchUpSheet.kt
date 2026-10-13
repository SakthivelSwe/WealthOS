package app.privatemoney.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.privatemoney.App
import app.privatemoney.ui.components.GlassCard
import app.privatemoney.ui.theme.LocalFinanceColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class MissedDateStatus(
    val date: LocalDate,
    val dateString: String,
    val displayLabel: String,
    val hasTxn: Boolean,
    val isConfirmedNoSpending: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatchUpSheet(
    app: App,
    onDismiss: () -> Unit,
    onAddExpenseForDate: (String) -> Unit,
) {
    val finance = LocalFinanceColors.current
    val missedDates = remember { mutableStateListOf<MissedDateStatus>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val today = LocalDate.now()
            val list = mutableListOf<MissedDateStatus>()
            val formatter = DateTimeFormatter.ofPattern("EEEE, MMM dd")

            // Inspect the last 7 days
            for (i in 1..6) {
                val pastDate = today.minusDays(i.toLong())
                val pastDateStr = pastDate.toString()
                val hasTxn = app.database.ledgerDao().hasTransactionOnDate(pastDateStr) != null
                val label = when (i) {
                    1 -> "Yesterday (${pastDate.format(DateTimeFormatter.ofPattern("MMM dd"))})"
                    else -> pastDate.format(formatter)
                }
                list.add(MissedDateStatus(pastDate, pastDateStr, label, hasTxn))
            }
            missedDates.clear()
            missedDates.addAll(list)
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(finance.primaryGlow),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.EventNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text("Catch Up on Missed Days", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Review the past week to keep your budget clear and accurate", style = MaterialTheme.typography.bodySmall, color = finance.muted)
                }
            }

            val unreviewed = missedDates.filter { !it.hasTxn && !it.isConfirmedNoSpending }

            if (!isLoading && unreviewed.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = finance.positive) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = finance.positive, modifier = Modifier.size(32.dp))
                        Text("You're Completely Up to Date!", fontWeight = FontWeight.Bold, color = finance.positive)
                        Text("Every day in the past week has been reviewed or recorded.", style = MaterialTheme.typography.bodySmall, color = finance.muted)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(missedDates, key = { it.dateString }) { item ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = if (item.hasTxn || item.isConfirmedNoSpending) finance.positive.copy(alpha = 0.5f) else finance.glassBorder,
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.displayLabel, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        when {
                                            item.hasTxn -> "✓ Recorded in ledger"
                                            item.isConfirmedNoSpending -> "✓ Confirmed: No spending"
                                            else -> "• No expenses logged"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (item.hasTxn || item.isConfirmedNoSpending) finance.positive else finance.muted,
                                    )
                                }

                                if (!item.hasTxn && !item.isConfirmedNoSpending) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                val idx = missedDates.indexOfFirst { it.dateString == item.dateString }
                                                if (idx >= 0) missedDates[idx] = item.copy(isConfirmedNoSpending = true)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                        ) {
                                            Text("No Spending", fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = {
                                                onDismiss()
                                                onAddExpenseForDate(item.dateString)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Text("Log", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
