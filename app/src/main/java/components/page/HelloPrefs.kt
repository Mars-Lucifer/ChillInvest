package components.page

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val SecurePrefsName = "chill_invest_secure"
/** Used when [EncryptedSharedPreferences] / Keystore fails (see KeyMint VERIFICATION_FAILED in logcat). */
private const val PlainPrefsName = "chill_invest_app_prefs"
private const val KeyServer = "server"
private const val KeyLogin = "login"
private const val KeyPassword = "password"
private const val KeyLocalPin = "local_pin"
private const val KeyBiometryEnabled = "biometry_enabled"
private const val KeyDeadlineDate = "deadline_date"
private const val KeyDeadlineInfinite = "deadline_infinite"
private const val KeyGoalAmount = "goal_amount"
private const val KeyGoalSyncEnabled = "goal_sync_enabled"
private const val KeyStrategyMode = "strategy_mode"
private const val KeyProfitPercent = "profit_percent"
private const val KeyOnboardingCompleted = "onboarding_completed"

private fun buildMasterKey(context: Context, requestStrongBox: Boolean): MasterKey {
    val builder = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        builder.setRequestStrongBoxBacked(requestStrongBox)
    }
    return builder.build()
}

private fun tryCreateEncryptedPreferences(context: Context, requestStrongBox: Boolean): SharedPreferences? {
    return runCatching {
        val masterKey = buildMasterKey(context, requestStrongBox)
        EncryptedSharedPreferences.create(
            context,
            SecurePrefsName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrNull()
}

/**
 * Prefers [EncryptedSharedPreferences]; if Keystore / KeyMint fails (common on some Android 16 builds
 * or after backup/restore mismatches), falls back to app-private non-encrypted prefs so the app
 * still persists settings.
 */
internal fun securePreferences(context: Context): SharedPreferences {
    val appContext = context.applicationContext
    tryCreateEncryptedPreferences(appContext, requestStrongBox = false)?.let { return it }
    tryCreateEncryptedPreferences(appContext, requestStrongBox = true)?.let { return it }
    return appContext.getSharedPreferences(PlainPrefsName, Context.MODE_PRIVATE)
}

internal fun loadStoredHelloState(context: Context): StoredHelloState {
    val preferences = securePreferences(context)
    val storedMode = preferences.getString(KeyStrategyMode, StrategyMode.Adaptive.name)
    val strategyMode = StrategyMode.values().firstOrNull { it.name == storedMode } ?: StrategyMode.Adaptive

    return StoredHelloState(
        server = preferences.getString(KeyServer, "") ?: "",
        login = preferences.getString(KeyLogin, "") ?: "",
        password = preferences.getString(KeyPassword, "") ?: "",
        localPin = preferences.getString(KeyLocalPin, "") ?: "",
        biometryEnabled = preferences.getBoolean(KeyBiometryEnabled, false),
        deadlineDate = preferences.getString(KeyDeadlineDate, "") ?: "",
        deadlineInfinite = preferences.getBoolean(KeyDeadlineInfinite, true),
        goalAmount = preferences.getString(KeyGoalAmount, "") ?: "",
        goalSyncEnabled = preferences.getBoolean(KeyGoalSyncEnabled, false),
        strategyMode = strategyMode,
        profitPercent = preferences.getString(KeyProfitPercent, "") ?: "",
        onboardingCompleted = preferences.getBoolean(KeyOnboardingCompleted, false)
    )
}

internal fun saveWelcomeSettings(
    context: Context,
    server: String,
    login: String,
    password: String
) {
    securePreferences(context).edit()
        .putString(KeyServer, server)
        .putString(KeyLogin, login)
        .putString(KeyPassword, password)
        .apply()
}

internal fun saveLocalPin(context: Context, pin: String) {
    securePreferences(context).edit()
        .putString(KeyLocalPin, pin)
        .commit()
}

internal fun saveBiometryEnabled(context: Context, enabled: Boolean) {
    securePreferences(context).edit()
        .putBoolean(KeyBiometryEnabled, enabled)
        .apply()
}

internal fun saveFinalSetupSettings(
    context: Context,
    deadlineDate: String,
    deadlineInfinite: Boolean,
    goalAmount: String,
    goalSyncEnabled: Boolean,
    strategyMode: StrategyMode,
    profitPercent: String
) {
    securePreferences(context).edit()
        .putString(KeyDeadlineDate, deadlineDate)
        .putBoolean(KeyDeadlineInfinite, deadlineInfinite)
        .putString(KeyGoalAmount, goalAmount)
        .putBoolean(KeyGoalSyncEnabled, goalSyncEnabled)
        .putString(KeyStrategyMode, strategyMode.name)
        .putString(KeyProfitPercent, profitPercent)
        .apply()
}

internal fun saveOnboardingCompleted(context: Context, completed: Boolean) {
    securePreferences(context).edit()
        .putBoolean(KeyOnboardingCompleted, completed)
        .commit()
}

internal fun clearStoredHelloState(context: Context) {
    securePreferences(context).edit()
        .clear()
        .commit()
}
