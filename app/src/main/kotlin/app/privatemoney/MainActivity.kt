package app.privatemoney

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import app.privatemoney.notification.NightlyReminderScheduler
import app.privatemoney.notification.NotificationHub
import app.privatemoney.security.BiometricAuth
import app.privatemoney.ui.PmosRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast

class MainActivity : FragmentActivity() {
    private val app get() = application as App
    private var prompting = false
    private var pendingAction by mutableStateOf<String?>(null)

    companion object {
        private const val REQUEST_CODE_NOTIFICATIONS = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        updateSecureFlag()
        handleNotificationAction(intent)
        handleDeepLinkAuth(intent)

        // Request POST_NOTIFICATIONS runtime permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATIONS,
                )
            }
        }

        // Arm exact 9:00 PM and 10:00 PM nightly reminders
        NightlyReminderScheduler.scheduleNightlyAlarms(this)

        setContent {
            val locked by app.lock.locked.collectAsState()
            PmosRoot(
                app = app,
                locked = locked,
                onUnlock = ::promptUnlock,
                onSecureFlagChange = ::updateSecureFlag,
                initialAction = pendingAction,
                onActionConsumed = { pendingAction = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationAction(intent)
        handleDeepLinkAuth(intent)
    }

    private fun handleDeepLinkAuth(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "wealthos" || uri.host?.contains("supabase") == true || uri.host?.contains("pages.dev") == true) {
            val fullUrl = uri.toString()
            lifecycleScope.launch(Dispatchers.IO) {
                val email = app.authService.currentUserEmail() ?: app.settings.supabaseUserEmail ?: ""
                val res = app.authService.verifyTokenOrLink(email, fullUrl)
                if (res.isSuccess) {
                    val syncRes = app.syncEngine.syncAll()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "Cloud account verified! Pulled ${syncRes.accountsPulled} accounts.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATIONS && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            NightlyReminderScheduler.scheduleNightlyAlarms(this)
        }
    }

    private fun handleNotificationAction(intent: Intent?) {
        val action = intent?.getStringExtra(NotificationHub.EXTRA_ACTION)
        if (!action.isNullOrBlank()) {
            pendingAction = action
        }
    }

    fun updateSecureFlag() {
        if (app.settings.blockScreenshots) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onResume() {
        super.onResume()
        updateSecureFlag()
    }

    override fun onStart() {
        super.onStart()
        app.lock.onForeground()
        if (app.lock.locked.value) promptUnlock()
    }

    override fun onStop() {
        super.onStop()
        app.lock.onBackground()
    }

    private fun promptUnlock() {
        if (prompting) return
        prompting = true
        BiometricAuth.prompt(this, getString(R.string.unlock_prompt)) { success ->
            prompting = false
            if (success) app.lock.unlocked()
        }
    }
}
