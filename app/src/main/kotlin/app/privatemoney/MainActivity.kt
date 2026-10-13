package app.privatemoney

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import app.privatemoney.notification.NotificationHub
import app.privatemoney.security.BiometricAuth
import app.privatemoney.ui.PmosRoot

class MainActivity : FragmentActivity() {
    private val app get() = application as App
    private var prompting = false
    private var pendingAction by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        updateSecureFlag()
        handleNotificationAction(intent)
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
