package app.privatemoney.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.privatemoney.data.NotificationRepository
import app.privatemoney.data.local.AppNotificationEntity
import app.privatemoney.ui.components.EmptyState
import app.privatemoney.ui.components.GlassCard
import app.privatemoney.ui.components.Hairline
import app.privatemoney.ui.theme.LocalFinanceColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterSheet(
    repository: NotificationRepository,
    onDismiss: () -> Unit,
    onActionClick: (String, String?) -> Unit,
) {
    val notifications by repository.observeAll().collectAsStateWithLifecycle(emptyList())
    val unreadCount by repository.observeUnreadCount().collectAsStateWithLifecycle(0)
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val finance = LocalFinanceColors.current

    val filteredNotifications = remember(notifications, selectedCategory) {
        if (selectedCategory == null) notifications else notifications.filter { it.category == selectedCategory }
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Notifications", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                "$unreadCount unread",
                                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary),
                            )
                        }
                    }
                }

                Row {
                    if (unreadCount > 0) {
                        IconButton(onClick = { scope.launch { repository.markAllAsRead() } }) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Mark all read", tint = finance.muted)
                        }
                    }
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = { scope.launch { repository.clearAll() } }) {
                            Icon(Icons.Default.ClearAll, contentDescription = "Clear all", tint = finance.muted)
                        }
                    }
                }
            }

            // Category Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All (${notifications.size})") },
                    shape = RoundedCornerShape(12.dp),
                )
                FilterChip(
                    selected = selectedCategory == "CHECK_IN",
                    onClick = { selectedCategory = if (selectedCategory == "CHECK_IN") null else "CHECK_IN" },
                    label = { Text("Reminders") },
                    shape = RoundedCornerShape(12.dp),
                )
                FilterChip(
                    selected = selectedCategory == "BILL",
                    onClick = { selectedCategory = if (selectedCategory == "BILL") null else "BILL" },
                    label = { Text("Bills") },
                    shape = RoundedCornerShape(12.dp),
                )
                FilterChip(
                    selected = selectedCategory == "BACKUP",
                    onClick = { selectedCategory = if (selectedCategory == "BACKUP") null else "BACKUP" },
                    label = { Text("Backup") },
                    shape = RoundedCornerShape(12.dp),
                )
            }

            if (filteredNotifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        title = "No Notifications",
                        body = "You're all caught up! Important reminders and financial alerts will appear here.",
                        icon = Icons.Default.Notifications,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filteredNotifications, key = { it.id }) { item ->
                        NotificationCard(
                            item = item,
                            onRead = { scope.launch { repository.markAsRead(item.id) } },
                            onAction = { action, payload ->
                                scope.launch { repository.markAsRead(item.id) }
                                onActionClick(action, payload)
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NotificationCard(
    item: AppNotificationEntity,
    onRead: () -> Unit,
    onAction: (String, String?) -> Unit,
) {
    val finance = LocalFinanceColors.current
    val (icon, tint) = when (item.category) {
        "CHECK_IN" -> Icons.Default.Notifications to MaterialTheme.colorScheme.primary
        "BILL" -> Icons.Default.Receipt to Color(0xFFFBBF24)
        "BUDGET" -> Icons.Default.TrendingUp to Color(0xFFF87171)
        "BACKUP" -> Icons.Default.CloudSync to Color(0xFF60A5FA)
        "SECURITY" -> Icons.Default.Security to Color(0xFFA78BFA)
        else -> Icons.Default.Notifications to finance.muted
    }

    val timeStr = remember(item.createdAt) {
        val dt = Instant.ofEpochMilli(item.createdAt).atZone(ZoneId.systemDefault())
        dt.format(DateTimeFormatter.ofPattern("MMM dd, hh:mm a"))
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (!item.read) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else finance.glassBorder,
        onClick = onRead,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(tint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                    }
                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(timeStr, style = TextStyle(fontSize = 10.sp, color = finance.muted))
                    if (!item.read) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }

            Text(item.message, style = MaterialTheme.typography.bodySmall, color = if (item.read) finance.muted else MaterialTheme.colorScheme.onSurface)

            item.actionType?.let { action ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = { onAction(action, item.payload) },
                    ) {
                        Text(
                            when (action) {
                                "ADD_EXPENSE" -> "Add Expense"
                                "REVIEW_DAY" -> "Review Day"
                                "VIEW_PLANS" -> "View Plans"
                                "VIEW_BACKUP" -> "Backup Settings"
                                else -> "Open"
                            },
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
