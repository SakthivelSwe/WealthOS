package app.privatemoney.ui.cloud

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import app.privatemoney.App
import app.privatemoney.cloud.CloudSyncService
import app.privatemoney.cloud.RestorePreview
import app.privatemoney.ui.components.GlassCard
import app.privatemoney.ui.components.Hairline
import app.privatemoney.ui.components.SectionLabel
import app.privatemoney.ui.theme.LocalFinanceColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudBackupSheet(
    app: App,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val finance = LocalFinanceColors.current
    val cloudService = remember { CloudSyncService(context, app.database, app.settings) }

    var userPin by remember { mutableStateOf("0000") }
    var isOperating by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var showConfig by remember { mutableStateOf(false) }

    var customUrl by remember { mutableStateOf(app.settings.supabaseUrl) }
    var customAnonKey by remember { mutableStateOf(app.settings.supabaseAnonKey) }

    var restorePreview by remember { mutableStateOf<RestorePreview?>(null) }
    var pendingRestoreBytes by remember { mutableStateOf<ByteArray?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            isOperating = true
            statusMessage = "Decrypting and inspecting backup envelope..."
            scope.launch(Dispatchers.IO) {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalArgumentException("Could not read backup file.")
                    val preview = cloudService.previewBackup(bytes, userPin)
                    pendingRestoreBytes = bytes
                    restorePreview = preview
                    isOperating = false
                    isError = false
                    statusMessage = null
                } catch (e: Exception) {
                    isOperating = false
                    isError = true
                    statusMessage = "Restore preview failed: ${e.message}"
                }
            }
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
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(finance.primaryGlow),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text("Cloud Backup & Restore", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("End-to-end encrypted off-device vault backup", style = MaterialTheme.typography.bodySmall, color = finance.muted)
                    }
                }

                IconButton(onClick = { showConfig = !showConfig }) {
                    Icon(Icons.Default.Settings, contentDescription = "Supabase Settings", tint = if (showConfig) MaterialTheme.colorScheme.primary else finance.muted)
                }
            }

            // Supabase Config Accordion
            if (showConfig) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Supabase Cloud Credentials", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = customUrl,
                            onValueChange = { customUrl = it },
                            label = { Text("Supabase Project URL") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = customAnonKey,
                            onValueChange = { customAnonKey = it },
                            label = { Text("Publishable Anon Key") },
                            placeholder = { Text("eyJh...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = {
                                app.settings.supabaseUrl = customUrl
                                app.settings.supabaseAnonKey = customAnonKey
                                showConfig = false
                                Toast.makeText(context, "Supabase credentials updated!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Save Credentials")
                        }
                    }
                }
            }

            // Backup Health Dashboard Card
            val lastTime = app.settings.lastBackupTimestamp
            val lastStatus = app.settings.lastBackupStatus
            val lastError = app.settings.lastBackupError
            val lastTimeFormatted = if (lastTime > 0) {
                Instant.ofEpochMilli(lastTime).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM dd, yyyy · hh:mm a"))
            } else "Never"

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (lastStatus == "SUCCESS") finance.positive.copy(alpha = 0.5f) else finance.glassBorder,
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("BACKUP HEALTH DASHBOARD", style = MaterialTheme.typography.labelSmall, color = finance.muted, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (lastStatus == "SUCCESS") Color(0x3334D399) else if (lastStatus == "FAILED") Color(0x33F87171) else Color(0x22888888))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                if (lastStatus == "SUCCESS") "UP TO DATE" else if (lastStatus == "FAILED") "ATTENTION NEEDED" else "NO BACKUP",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (lastStatus == "SUCCESS") finance.positive else if (lastStatus == "FAILED") MaterialTheme.colorScheme.error else finance.muted,
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Last Successful Backup", style = MaterialTheme.typography.bodySmall, color = finance.muted)
                        Text(lastTimeFormatted, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }

                    if (lastError != null) {
                        Text("Notice: $lastError", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Restore Confirmation Modal
            restorePreview?.let { preview ->
                GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = MaterialTheme.colorScheme.primary) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Restore Preview & Confirmation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("• Accounts to merge: ${preview.accountsToRestore}", style = MaterialTheme.typography.bodySmall)
                        Text("• Transactions to restore: ${preview.txnsToRestore}", style = MaterialTheme.typography.bodySmall, color = finance.positive, fontWeight = FontWeight.Bold)
                        if (preview.existingTxnsToSkip > 0) {
                            Text("• Existing duplicates to skip: ${preview.existingTxnsToSkip}", style = MaterialTheme.typography.bodySmall, color = finance.muted)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { restorePreview = null; pendingRestoreBytes = null },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    val bytes = pendingRestoreBytes ?: return@Button
                                    isOperating = true
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            cloudService.executeRestore(bytes, userPin)
                                            withContext(Dispatchers.Main) {
                                                isOperating = false
                                                restorePreview = null
                                                pendingRestoreBytes = null
                                                Toast.makeText(context, "Successfully restored backup without duplicates!", Toast.LENGTH_LONG).show()
                                                onDismiss()
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                isOperating = false
                                                isError = true
                                                statusMessage = "Restore failed: ${e.message}"
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Apply Restore")
                            }
                        }
                    }
                }
                return@Column
            }

            // Vault PIN Input
            OutlinedTextField(
                value = userPin,
                onValueChange = { userPin = it },
                label = { Text("Vault Encryption Passphrase / PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            // Primary Actions
            if (isOperating) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(statusMessage ?: "Processing...", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            isOperating = true
                            statusMessage = "Creating encrypted backup and syncing..."
                            scope.launch(Dispatchers.IO) {
                                try {
                                    cloudService.backupToCloud(userPin)
                                    withContext(Dispatchers.Main) {
                                        isOperating = false
                                        isError = false
                                        statusMessage = "Encrypted cloud backup completed successfully!"
                                        Toast.makeText(context, "Backup synced successfully to Supabase!", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isOperating = false
                                        isError = true
                                        statusMessage = e.message
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cloud Backup", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch("*/*")
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Restore", fontWeight = FontWeight.Bold)
                    }
                }

                // Export to file
                OutlinedButton(
                    onClick = {
                        isOperating = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                val envelope = cloudService.createBackupEnvelope(userPin)
                                val file = File(context.cacheDir, "wealthos_backup_${System.currentTimeMillis()}.wosenc")
                                file.writeBytes(envelope)
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/octet-stream"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                withContext(Dispatchers.Main) {
                                    isOperating = false
                                    context.startActivity(Intent.createChooser(shareIntent, "Save or Export WealthOS Encrypted Backup"))
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isOperating = false
                                    isError = true
                                    statusMessage = "Export failed: ${e.message}"
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export Encrypted Backup File (.wosenc)")
                }
            }

            statusMessage?.let { msg ->
                Text(
                    msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) MaterialTheme.colorScheme.error else finance.positive,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
