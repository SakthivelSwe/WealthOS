package app.privatemoney.ui.auth

import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    var verificationInput by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordMode by remember { mutableStateOf(true) }
    var isSignUp by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
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
                    // Auth Mode Selector Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Button(
                            onClick = { isPasswordMode = true; errorMessage = null },
                            modifier = Modifier.weight(1f).height(38.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPasswordMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (isPasswordMode) Color.White else finance.muted,
                            ),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Password Login", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { isPasswordMode = false; errorMessage = null },
                            modifier = Modifier.weight(1f).height(38.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isPasswordMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (!isPasswordMode) Color.White else finance.muted,
                            ),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Magic Link / OTP", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Text(
                        text = if (isPasswordMode) {
                            if (isSignUp) "Create your WealthOS Cloud account with email and password." else "Enter your email and password to connect and sync your ledger instantly."
                        } else {
                            "Enter your email to receive a secure sign-in link or 6-digit verification code."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = finance.muted,
                        lineHeight = 20.sp,
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

                    if (isPasswordMode) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(if (isSignUp) "Choose Password (min 6 chars)" else "Your Password") },
                            placeholder = { Text("••••••••") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = finance.muted) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility",
                                        tint = finance.muted,
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                        )

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = finance.positive, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Use this exact same email & password on WealthOS Web to sync balances seamlessly!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = finance.muted,
                                    lineHeight = 16.sp,
                                )
                            }
                        }
                    }

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
                            Text(if (isPasswordMode) "Authenticating & synchronizing..." else "Sending sign-in email...", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Button(
                            onClick = {
                                val cleanEmail = email.trim()
                                if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                                    errorMessage = "Please enter a valid email address."
                                    return@Button
                                }

                                if (isPasswordMode) {
                                    if (password.isBlank()) {
                                        errorMessage = "Please enter your password."
                                        return@Button
                                    }
                                    if (isSignUp && password.length < 6) {
                                        errorMessage = "Password must be at least 6 characters."
                                        return@Button
                                    }
                                    isLoading = true
                                    errorMessage = null
                                    scope.launch(Dispatchers.IO) {
                                        val res = if (isSignUp) {
                                            app.authService.signUpWithPassword(cleanEmail, password)
                                        } else {
                                            app.authService.signInWithPassword(cleanEmail, password)
                                        }
                                        if (res.isFailure) {
                                            withContext(Dispatchers.Main) {
                                                isLoading = false
                                                errorMessage = res.exceptionOrNull()?.message ?: "Authentication failed"
                                            }
                                            return@launch
                                        }
                                        val syncRes = app.syncEngine.syncAll()
                                        withContext(Dispatchers.Main) {
                                            isLoading = false
                                            step = "LOGGED_IN"
                                            successMessage = "Successfully connected as $cleanEmail! Pulled ${syncRes.accountsPulled} accounts from Web."
                                            onLoginSuccess(cleanEmail)
                                            Toast.makeText(context, "Account verified & synchronized!", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    isLoading = true
                                    errorMessage = null
                                    scope.launch(Dispatchers.IO) {
                                        val result = app.authService.sendEmailOtp(cleanEmail)
                                        withContext(Dispatchers.Main) {
                                            isLoading = false
                                            result.fold(
                                                onSuccess = {
                                                    step = "OTP"
                                                    successMessage = "Sign-in link / code sent to $cleanEmail. Check your inbox!"
                                                },
                                                onFailure = { err ->
                                                    errorMessage = err.message ?: "Failed to send sign-in link"
                                                },
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = email.isNotBlank(),
                        ) {
                            Text(
                                text = if (isPasswordMode) {
                                    if (isSignUp) "Create Account & Sync" else "Sign In with Password & Sync"
                                } else {
                                    "Send Sign-in Link / Code"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            )
                        }

                        if (isPasswordMode) {
                            TextButton(
                                onClick = {
                                    isSignUp = !isSignUp
                                    errorMessage = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (isSignUp) "Already have an account? Sign in here" else "New user? Create a cloud account")
                            }
                        }
                    }
                }

                "OTP" -> {
                    Text(
                        text = "Enter the 6-digit code OR paste the sign-in link sent to $email:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = finance.muted,
                        lineHeight = 20.sp,
                    )

                    OutlinedTextField(
                        value = verificationInput,
                        onValueChange = { verificationInput = it },
                        label = { Text("Code or Sign-in Link") },
                        placeholder = { Text("123456 or https://...") },
                        singleLine = false,
                        maxLines = 3,
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = finance.muted) },
                        trailingIcon = {
                            if (verificationInput.isNotBlank()) {
                                IconButton(onClick = { verificationInput = "" }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Clear input", tint = finance.muted, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                    )

                    // Quick Paste from Clipboard Button
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                            if (!clipText.isNullOrBlank()) {
                                verificationInput = clipText
                                Toast.makeText(context, "Pasted link from clipboard", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("📋 Paste Link from Clipboard", fontSize = 13.sp)
                    }

                    // Helper Instructions Card
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text("How to verify with your email:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                            Text(
                                text = "• If your email has a 6-digit code: Type the digits above.\n• If your email has a 'Sign in' button: Long-press the 'Sign in' button in Gmail, tap 'Copy link address', then tap 'Paste Link from Clipboard' above!",
                                style = MaterialTheme.typography.bodySmall,
                                color = finance.muted,
                                lineHeight = 18.sp,
                            )
                        }
                    }

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
                                val input = verificationInput.trim()
                                if (input.isBlank()) {
                                    errorMessage = "Please enter the 6-digit code or paste the sign-in link."
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                scope.launch(Dispatchers.IO) {
                                    val verifyResult = app.authService.verifyTokenOrLink(email, input)
                                    if (verifyResult.isFailure) {
                                        withContext(Dispatchers.Main) {
                                            isLoading = false
                                            errorMessage = verifyResult.exceptionOrNull()?.message ?: "Invalid verification code or link"
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
                            enabled = verificationInput.isNotBlank(),
                        ) {
                            Text("Verify & Sync Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        TextButton(
                            onClick = {
                                step = "EMAIL"
                                verificationInput = ""
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
