package app.privatemoney.ui.auth

import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.privatemoney.App
import app.privatemoney.ui.components.GlassCard
import app.privatemoney.ui.components.Hairline
import app.privatemoney.ui.theme.LocalFinanceColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupabaseLoginSheet(
    app: App,
    onDismiss: () -> Unit,
    onLoginSuccess: (email: String) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val finance = LocalFinanceColors.current

    val isLoggedIn = app.authService.isLoggedIn()
    var email by remember { mutableStateOf(app.authService.currentUserEmail() ?: "") }
    var otpCode by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(if (isLoggedIn) "LOGGED_IN" else "EMAIL") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showConfig by remember { mutableStateOf(false) }

    var customUrl by remember { mutableStateOf(app.settings.supabaseUrl) }
    var customAnonKey by remember { mutableStateOf(app.settings.supabaseAnonKey) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(finance.primaryGlow),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Column {
                        Text(
                            text = if (isLoggedIn) "Account Synced" else "Sync with Web Account",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Supabase real-time cloud synchronizer",
                            style = MaterialTheme.typography.bodySmall,
                            color = finance.muted,
                        )
                    }
                }

                IconButton(onClick = { showConfig = !showConfig }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Supabase settings",
                        tint = if (showConfig) MaterialTheme.colorScheme.primary else finance.muted,
                    )
                }
            }

            // Supabase Config Accordion
            if (showConfig) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Cloud Project Settings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = customUrl,
                            onValueChange = { customUrl = it },
                            label = { Text("Supabase URL") },
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
                                Toast.makeText(context, "Cloud settings saved", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Save Cloud Settings")
                        }
                    }
                }
            }

            // Error & Success Banners
            errorMessage?.let { err ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = MaterialTheme.colorScheme.error,
                ) {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            successMessage?.let { msg ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = finance.positive,
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = finance.positive, modifier = Modifier.size(18.dp))
                        Text(text = msg, color = finance.positive, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Body depending on step
            when (step) {
                "LOGGED_IN" -> {
                    // Account Info Card
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CloudDone, contentDescription = null, tint = finance.positive, modifier = Modifier.size(20.dp))
                                Text(
                                    text = "Connected Account",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = finance.muted,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Text(
                                text = app.authService.currentUserEmail() ?: "Unknown User",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )

                            Hairline()

                            val lastSync = app.settings.lastSyncTimestamp
                            val lastSyncFormatted = if (lastSync > 0) {
                                Instant.ofEpochMilli(lastSync).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM dd, yyyy · hh:mm a"))
                            } else "Never"

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Last Synced with Cloud", style = MaterialTheme.typography.bodySmall, color = finance.muted)
                                Text(lastSyncFormatted, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Sync & Sign Out Buttons
                    if (isLoading) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Synchronizing data with Supabase...", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                successMessage = null
                                scope.launch(Dispatchers.IO) {
                                    val res = app.syncEngine.syncAll()
                                    withContext(Dispatchers.Main) {
                                        isLoading = false
                                        if (res.isSuccess) {
                                            successMessage = "Sync complete! Pulled ${res.accountsPulled} accounts, pushed ${res.accountsPushed} accounts."
                                        } else {
                                            errorMessage = res.errorMessage
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("⚡ Sync Now with Web", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                isLoading = true
                                scope.launch(Dispatchers.IO) {
                                    app.authService.signOut()
                                    withContext(Dispatchers.Main) {
                                        isLoading = false
                                        step = "EMAIL"
                                        successMessage = null
                                        errorMessage = null
                                        Toast.makeText(context, "Signed out of cloud account", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Sign Out of Account", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                "EMAIL" -> {
                    Text(
                        text = "Enter the email ID you used on the WealthOS Web application. We will send you a 6-digit verification code to instantly connect and sync your ledger.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = finance.muted,
                        lineHeight = 22.sp,
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim() },
                        label = { Text("Your Email Address") },
                        placeholder = { Text("e.g. name@example.com") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = finance.muted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                    )

                    if (app.settings.supabaseAnonKey.isBlank()) {
                        Text(
                            text = "⚠️ Supabase Publishable Key not configured. Click the gear icon above to paste your key.",
                            color = finance.warning,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    if (isLoading) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Sending verification email...", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Button(
                            onClick = {
                                val cleanEmail = email.trim()
                                if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                                    errorMessage = "Please enter a valid email address."
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                scope.launch(Dispatchers.IO) {
                                    val result = app.authService.sendEmailOtp(cleanEmail)
                                    withContext(Dispatchers.Main) {
                                        isLoading = false
                                        result.fold(
                                            onSuccess = {
                                                step = "OTP"
                                                successMessage = "Code sent to $cleanEmail. Please check your inbox or spam folder."
                                            },
                                            onFailure = { err ->
                                                errorMessage = err.message ?: "Failed to send verification code"
                                            },
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = email.isNotBlank(),
                        ) {
                            Text("Send Verification Code", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }

                "OTP" -> {
                    Text(
                        text = "Enter the 6-digit verification code sent to $email:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = finance.muted,
                    )

                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { otpCode = it.filter { ch -> ch.isDigit() }.take(6) },
                        label = { Text("6-Digit Code") },
                        placeholder = { Text("123456") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = finance.muted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                    )

                    if (isLoading) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Verifying code & syncing data...", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (otpCode.length < 6) {
                                    errorMessage = "Please enter all 6 digits."
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                scope.launch(Dispatchers.IO) {
                                    val verifyResult = app.authService.verifyEmailOtp(email, otpCode)
                                    if (verifyResult.isFailure) {
                                        withContext(Dispatchers.Main) {
                                            isLoading = false
                                            errorMessage = verifyResult.exceptionOrNull()?.message ?: "Invalid code"
                                        }
                                        return@launch
                                    }

                                    // Instantly synchronize with cloud
                                    val syncResult = app.syncEngine.syncAll()
                                    withContext(Dispatchers.Main) {
                                        isLoading = false
                                        step = "LOGGED_IN"
                                        successMessage = "Successfully connected as $email! Pulled ${syncResult.accountsPulled} accounts from Web."
                                        onLoginSuccess(email)
                                        Toast.makeText(context, "Account verified & synchronized!", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = otpCode.length >= 6,
                        ) {
                            Text("Verify Code & Sync Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        TextButton(
                            onClick = {
                                step = "EMAIL"
                                otpCode = ""
                                errorMessage = null
                                successMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Use a different email address")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
