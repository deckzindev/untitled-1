package com.example.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

private const val TAG = "SecurityAuthHelper"

object SecurityAuthHelper {

    fun isDeviceSecure(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            keyguardManager.isDeviceSecure
        } else {
            @Suppress("DEPRECATION")
            keyguardManager.isKeyguardSecure
        }
    }

    fun openSecuritySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun authenticate(
        activity: Activity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: ((String) -> Unit)?,
        onFallbackToKeyguard: (Intent) -> Unit
    ) {
        if (!isDeviceSecure(activity)) {
            onError?.invoke("Dispositivo não protegido por biometria ou senha")
            return
        }

        // On API 29+ (Android 10+), BiometricPrompt supports device credential integration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val cancellationSignal = CancellationSignal()
                val executor = ContextCompat.getMainExecutor(activity)
                val builder = BiometricPrompt.Builder(activity)
                    .setTitle(title)
                    .setSubtitle(subtitle)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    builder.setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                } else {
                    @Suppress("DEPRECATION")
                    builder.setDeviceCredentialAllowed(true)
                }

                val prompt = builder.build()
                prompt.authenticate(cancellationSignal, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        // If user canceled explicitly, report error and do not fall back
                        if (errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED) {
                            onError?.invoke(errString?.toString() ?: "Autenticação cancelada")
                            return
                        }
                        // For hardware errors or enrollment gaps, fallback to KeyguardManager
                        fallbackToKeyguard(activity, title, subtitle, onError, onFallbackToKeyguard)
                    }
                })
                return
            } catch (e: Throwable) {
                Log.w(TAG, "BiometricPrompt setup failed, falling back to KeyguardManager", e)
            }
        }

        // Fallback for API < 29 or if BiometricPrompt setup encountered an exception
        fallbackToKeyguard(activity, title, subtitle, onError, onFallbackToKeyguard)
    }

    private fun fallbackToKeyguard(
        activity: Activity,
        title: String,
        subtitle: String,
        onError: ((String) -> Unit)?,
        onFallbackToKeyguard: (Intent) -> Unit
    ) {
        val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val intent = keyguardManager?.createConfirmDeviceCredentialIntent(title, subtitle)
        if (intent != null) {
            onFallbackToKeyguard(intent)
        } else {
            onError?.invoke("Não foi possível iniciar o serviço de autenticação do sistema")
        }
    }
}
