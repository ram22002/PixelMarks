package com.pixelmarks.core.security.biometric

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.pixelmarks.core.security.model.BiometricAuthCapability

/**
 * Production-ready helper for AndroidX BiometricPrompt and system credential authentication.
 */
class BiometricAuthManager(
    private val context: Context
) {

    private val allowedAuthenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    /**
     * Evaluates current hardware and enrollment readiness on this device.
     */
    fun checkCapability(): BiometricAuthCapability {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(allowedAuthenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAuthCapability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAuthCapability.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAuthCapability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAuthCapability.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAuthCapability.SECURITY_UPDATE_REQUIRED
            else -> BiometricAuthCapability.UNAVAILABLE
        }
    }

    /**
     * Displays system BiometricPrompt with biometric & device credential (PIN/pattern/password) fallback.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String? = null,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed()
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setAllowedAuthenticators(allowedAuthenticators)

        if (!subtitle.isNullOrBlank()) {
            promptInfoBuilder.setSubtitle(subtitle)
        }

        try {
            prompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            // Fallback in case device credentials are not supported with current builder
            onError(-1, e.message ?: "Authentication failed to start")
        }
    }

    /**
     * Constructs intent to direct the user to device biometric/security settings.
     */
    @Suppress("DEPRECATION")
    fun createEnrollIntent(): Intent {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                    putExtra(
                        Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                        allowedAuthenticators
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Intent(Settings.ACTION_FINGERPRINT_ENROLL)
            } else {
                Intent(Settings.ACTION_SECURITY_SETTINGS)
            }
        } catch (_: Exception) {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }
}
