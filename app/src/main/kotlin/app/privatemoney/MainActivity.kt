package app.privatemoney

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import app.privatemoney.security.BiometricAuth
import app.privatemoney.ui.PmosRoot

class MainActivity : FragmentActivity() {
    private val app get() = application as App
    private var prompting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        updateSecureFlag()
        setContent {
            val locked by app.lock.locked.collectAsState()
            PmosRoot(
                app = app,
                locked = locked,
                onUnlock = ::promptUnlock,
                onSecureFlagChange = ::updateSecureFlag,
            )
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
