package components.page

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity

internal fun showBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Chill.Invest")
        .setSubtitle("Подтвердите подключение биометрии")
        .setNegativeButtonText("Отмена")
        .build()

    val biometricPrompt = BiometricPrompt(
        activity,
        activity.mainExecutor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onDismiss()
            }

            override fun onAuthenticationFailed() {
                onDismiss()
            }
        }
    )

    biometricPrompt.authenticate(promptInfo)
}
