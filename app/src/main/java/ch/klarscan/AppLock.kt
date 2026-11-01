package ch.klarscan

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.view.WindowManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * App-Sperre per Fingerabdruck. Klappt der Fingerabdruck nicht, geht die PIN/das Muster des Handys.
 * Die App sperrt sich, sobald sie in den Hintergrund geht – ausser beim Scannen, Teilen und Speichern,
 * die kurz eine andere App-Ansicht öffnen.
 */
class AppLock(private val activity: FragmentActivity) {

    private val prefs = activity.getSharedPreferences("klarscan", Context.MODE_PRIVATE)

    var enabled by mutableStateOf(prefs.getBoolean(KEY, false))
        private set
    var locked by mutableStateOf(enabled)
        private set

    /** Vor dem Öffnen von Scanner, Teilen-Dialog oder Speicherort setzen. */
    var expectExternal = false

    private var leftAt = 0L
    private var authInProgress = false
    private var lastAuthAt = 0L
    private var pending: ((Boolean, String?) -> Unit)? = null

    // Muss in onCreate gebaut werden, darum hier im Konstruktor
    private val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                authInProgress = false
                lastAuthAt = SystemClock.elapsedRealtime()
                pending?.invoke(true, null)
                pending = null
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                authInProgress = false
                val cancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                pending?.invoke(false, if (cancelled) null else errString.toString())
                pending = null
            }
            // onAuthenticationFailed: falscher Finger, der Dialog bleibt offen und zählt selbst mit
        },
    )

    init {
        disableIfNoScreenLock()
        applySecureFlag()
    }

    private val authenticators: Int
        get() = if (Build.VERSION.SDK_INT >= 30) BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        else BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    private fun status() = BiometricManager.from(activity).canAuthenticate(authenticators)

    private fun authenticate(title: String, subtitle: String, done: (Boolean, String?) -> Unit) {
        if (authInProgress) return
        authInProgress = true
        pending = done
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
    }

    fun unlock(onError: (String) -> Unit) {
        if (!locked) return
        authenticate("Klarscan entsperren", "Finger auflegen oder Handy-PIN eingeben") { ok, err ->
            if (ok) locked = false else err?.let(onError)
        }
    }

    fun setEnabled(on: Boolean, onError: (String) -> Unit) {
        if (on == enabled) return
        if (on && status() != BiometricManager.BIOMETRIC_SUCCESS) {
            onError("Richte zuerst in den Android-Einstellungen einen Fingerabdruck oder eine Bildschirmsperre ein.")
            return
        }
        val title = if (on) "App-Sperre einschalten" else "App-Sperre ausschalten"
        authenticate(title, "Zur Bestätigung Finger auflegen") { ok, err ->
            if (ok) {
                enabled = on
                prefs.edit().putBoolean(KEY, on).apply()
                applySecureFlag()
            } else err?.let(onError)
        }
    }

    fun onStop() {
        leftAt = SystemClock.elapsedRealtime()
    }

    fun onStart() {
        disableIfNoScreenLock()
        val justAuthenticated = SystemClock.elapsedRealtime() - lastAuthAt < 2_000
        if (enabled && leftAt > 0 && !expectExternal && !authInProgress && !justAuthenticated) {
            locked = true
        }
        expectExternal = false
    }

    /**
     * Wurde die Bildschirmsperre des Handys entfernt, gibt es nichts mehr, womit man entsperren könnte.
     * Dann schützt die Sperre auch nichts mehr – sie wird ausgeschaltet, statt dich auszusperren.
     */
    private fun disableIfNoScreenLock() {
        if (enabled && status() == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            enabled = false
            locked = false
            prefs.edit().putBoolean(KEY, false).apply()
            applySecureFlag()
        }
    }

    /** Solange die Sperre an ist: keine Vorschau in der App-Übersicht, keine Bildschirmfotos. */
    private fun applySecureFlag() {
        if (enabled) activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private companion object {
        const val KEY = "app_lock"
    }
}
