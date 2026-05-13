package components.page

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.chillinvest.ui.theme.AppBackground
import com.example.chillinvest.ui.theme.ChillInvestTheme

internal fun initialHelloScreen(context: Context): HelloScreen {
    val stored = loadStoredHelloState(context)
    return when {
        !stored.onboardingCompleted -> HelloScreen.Welcome
        stored.localPin.isNotBlank() -> HelloScreen.Unlock
        else -> HelloScreen.Home
    }
}

@Composable
fun HelloFlow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val storedState = remember(appContext) { loadStoredHelloState(appContext) }
    var screen by rememberSaveable {
        mutableStateOf(initialHelloScreen(appContext))
    }
    var server by rememberSaveable { mutableStateOf(storedState.server) }
    var login by rememberSaveable { mutableStateOf(storedState.login) }
    var password by rememberSaveable { mutableStateOf(storedState.password) }
    var localPin by rememberSaveable { mutableStateOf(storedState.localPin) }
    var biometryEnabled by rememberSaveable { mutableStateOf(storedState.biometryEnabled) }
    var pin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    var pinErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var unlockPin by rememberSaveable { mutableStateOf("") }
    var unlockErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var deadlineDate by rememberSaveable { mutableStateOf(storedState.deadlineDate) }
    var deadlineInfinite by rememberSaveable { mutableStateOf(storedState.deadlineInfinite) }
    var goalAmount by rememberSaveable { mutableStateOf(storedState.goalAmount) }
    var goalSyncEnabled by rememberSaveable { mutableStateOf(storedState.goalSyncEnabled) }
    var strategyMode by rememberSaveable { mutableStateOf(storedState.strategyMode) }
    var profitPercent by rememberSaveable { mutableStateOf(storedState.profitPercent) }
    var showBiometryPopup by rememberSaveable { mutableStateOf(false) }
    var isBiometryLoading by rememberSaveable { mutableStateOf(false) }
    var isUnlockBiometryLoading by rememberSaveable { mutableStateOf(false) }

    var pendingLockOnForeground by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, appContext) {
        val activity = context as? FragmentActivity
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (activity?.isChangingConfigurations == true) return@LifecycleEventObserver
                val prefs = loadStoredHelloState(appContext)
                if (prefs.onboardingCompleted && prefs.localPin.isNotBlank()) {
                    pendingLockOnForeground = true
                }
            }
            if (event == Lifecycle.Event.ON_START && pendingLockOnForeground) {
                pendingLockOnForeground = false
                val prefs = loadStoredHelloState(appContext)
                if (prefs.onboardingCompleted && prefs.localPin.isNotBlank()) {
                    localPin = prefs.localPin
                    biometryEnabled = prefs.biometryEnabled
                    unlockPin = ""
                    unlockErrorMessage = null
                    screen = HelloScreen.Unlock
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                if (targetState.ordinal >= initialState.ordinal) {
                    slideInHorizontally(
                        animationSpec = tween(320),
                        initialOffsetX = { it / 4 }
                    ) + fadeIn(animationSpec = tween(320)) togetherWith slideOutHorizontally(
                        animationSpec = tween(320),
                        targetOffsetX = { -it / 5 }
                    ) + fadeOut(animationSpec = tween(220))
                } else {
                    slideInHorizontally(
                        animationSpec = tween(320),
                        initialOffsetX = { -it / 4 }
                    ) + fadeIn(animationSpec = tween(320)) togetherWith slideOutHorizontally(
                        animationSpec = tween(320),
                        targetOffsetX = { it / 5 }
                    ) + fadeOut(animationSpec = tween(220))
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .then(if (showBiometryPopup) Modifier.blur(18.dp) else Modifier),
            label = "hello_flow"
        ) { currentScreen ->
            when (currentScreen) {
                HelloScreen.Welcome -> WelcomeScreen(
                    server = server,
                    login = login,
                    password = password,
                    onServerChange = { server = it },
                    onLoginChange = { login = it },
                    onPasswordChange = { password = it },
                    onContinue = { nextServer, nextLogin, nextPassword ->
                        server = nextServer
                        login = nextLogin
                        password = nextPassword
                        pinErrorMessage = null
                        saveWelcomeSettings(
                            context = context,
                            server = nextServer,
                            login = nextLogin,
                            password = nextPassword
                        )
                        screen = HelloScreen.CreatePin
                    }
                )

                HelloScreen.CreatePin -> PinScreen(
                    title = "Давайте придумаем пароль",
                    pin = pin,
                    errorMessage = pinErrorMessage,
                    biometryEnabled = false,
                    onDigitClick = {
                        if (pin.length < 4) {
                            pinErrorMessage = null
                            pin += it
                            if (pin.length == 4) {
                                screen = HelloScreen.ConfirmPin
                            }
                        }
                    },
                    onBackspace = {
                        if (pin.isNotEmpty()) {
                            pin = pin.dropLast(1)
                        }
                    },
                    onBiometryClick = null
                )

                HelloScreen.ConfirmPin -> PinScreen(
                    title = "Введите еще раз",
                    pin = confirmPin,
                    errorMessage = pinErrorMessage,
                    biometryEnabled = false,
                    onDigitClick = {
                        if (confirmPin.length < 4) {
                            pinErrorMessage = null
                            confirmPin += it
                            if (confirmPin.length == 4) {
                                if (confirmPin == pin) {
                                    saveLocalPin(context, pin)
                                    localPin = pin
                                    showBiometryPopup = true
                                } else {
                                    confirmPin = ""
                                    pinErrorMessage = "PIN-коды не совпадают"
                                }
                            }
                        }
                    },
                    onBackspace = {
                        if (confirmPin.isNotEmpty()) {
                            confirmPin = confirmPin.dropLast(1)
                        }
                    },
                    onBiometryClick = null
                )

                HelloScreen.FinalSetup -> FinalSetupScreen(
                    deadlineDate = deadlineDate,
                    deadlineInfinite = deadlineInfinite,
                    goalAmount = goalAmount,
                    goalSyncEnabled = goalSyncEnabled,
                    strategyMode = strategyMode,
                    profitPercent = profitPercent,
                    onDeadlineDateChange = { deadlineDate = it },
                    onDeadlineInfiniteChange = { deadlineInfinite = it },
                    onGoalAmountChange = { goalAmount = it },
                    onGoalSyncEnabledChange = { goalSyncEnabled = it },
                    onStrategyModeChange = { strategyMode = it },
                    onProfitPercentChange = { profitPercent = it },
                    onConnect = {
                        saveFinalSetupSettings(
                            context = context,
                            deadlineDate = deadlineDate,
                            deadlineInfinite = deadlineInfinite,
                            goalAmount = goalAmount,
                            goalSyncEnabled = goalSyncEnabled,
                            strategyMode = strategyMode,
                            profitPercent = profitPercent
                        )
                        saveOnboardingCompleted(context, true)
                        screen = HelloScreen.Home
                    }
                )

                HelloScreen.Unlock -> PinScreen(
                    title = "Здравствуйте",
                    subtitle = "Введите PIN-код, чтобы открыть приложение",
                    pin = unlockPin,
                    errorMessage = unlockErrorMessage,
                    biometryEnabled = biometryEnabled,
                    onDigitClick = { digit ->
                        if (unlockPin.length < 4) {
                            unlockErrorMessage = null
                            val nextPin = unlockPin + digit
                            unlockPin = nextPin
                            if (nextPin.length == 4) {
                                if (nextPin == localPin) {
                                    unlockPin = ""
                                    screen = HelloScreen.Home
                                } else {
                                    unlockPin = ""
                                    unlockErrorMessage = "Неверный PIN-код"
                                }
                            }
                        }
                    },
                    onBackspace = {
                        if (unlockPin.isNotEmpty()) {
                            unlockPin = unlockPin.dropLast(1)
                        }
                    },
                    onBiometryClick = {
                        val activity = context as? FragmentActivity ?: return@PinScreen
                        if (isUnlockBiometryLoading) return@PinScreen

                        isUnlockBiometryLoading = true
                        showBiometricPrompt(
                            activity = activity,
                            onSuccess = {
                                isUnlockBiometryLoading = false
                                unlockPin = ""
                                unlockErrorMessage = null
                                screen = HelloScreen.Home
                            },
                            onDismiss = {
                                isUnlockBiometryLoading = false
                            }
                        )
                    }
                )

                HelloScreen.Home -> HomeScreen(
                    state = buildHomePlaceholderState(
                        goalAmount = goalAmount.ifBlank { storedState.goalAmount },
                        deadlineDate = deadlineDate.ifBlank { storedState.deadlineDate },
                        deadlineInfinite = deadlineInfinite,
                        profitPercent = profitPercent.ifBlank { storedState.profitPercent }
                    ),
                    onStopStrategyConfirm = {
                        // TODO: replace with DELETE strategy request.
                    },
                    onAnalyticsClick = { screen = HelloScreen.Analytics }
                )

                HelloScreen.Analytics -> AnalyticsScreen(
                    state = buildAnalyticsPlaceholderState(),
                    onBack = { screen = HelloScreen.Home }
                )
            }
        }

        if (showBiometryPopup) {
            BiometryPopup(
                loading = isBiometryLoading,
                onEnable = {
                    val activity = context as? FragmentActivity ?: return@BiometryPopup
                    if (isBiometryLoading) return@BiometryPopup

                    isBiometryLoading = true
                    showBiometricPrompt(
                        activity = activity,
                        onSuccess = {
                            biometryEnabled = true
                            saveBiometryEnabled(context, true)
                            isBiometryLoading = false
                            showBiometryPopup = false
                            screen = HelloScreen.FinalSetup
                        },
                        onDismiss = {
                            isBiometryLoading = false
                        }
                    )
                },
                onSkip = {
                    if (isBiometryLoading) return@BiometryPopup
                    biometryEnabled = false
                    saveBiometryEnabled(context, false)
                    showBiometryPopup = false
                    screen = HelloScreen.FinalSetup
                }
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF15181F)
@Composable
private fun HelloFlowPreview() {
    ChillInvestTheme {
        HelloFlow()
    }
}
