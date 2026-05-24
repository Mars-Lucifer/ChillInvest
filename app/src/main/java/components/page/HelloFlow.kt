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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.chillinvest.ui.theme.AppBackground
import com.example.chillinvest.ui.theme.ChillInvestTheme
import kotlinx.coroutines.launch

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
    var isWelcomeLoading by rememberSaveable { mutableStateOf(false) }
    var welcomeErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isFinalSetupLoading by rememberSaveable { mutableStateOf(false) }
    var finalSetupErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var stopActionLoading by rememberSaveable { mutableStateOf(false) }
    var avariaStopEnabled by rememberSaveable { mutableStateOf(false) }
    var homeReloadKey by rememberSaveable { mutableIntStateOf(0) }
    var analyticsReloadKey by rememberSaveable { mutableIntStateOf(0) }
    var homeState by remember {
        mutableStateOf(
            buildHomeFallbackState(
                goalAmount = storedState.goalAmount,
                deadlineDate = storedState.deadlineDate,
                deadlineInfinite = storedState.deadlineInfinite
            )
        )
    }
    var analyticsState by remember { mutableStateOf(buildAnalyticsFallbackState()) }

    val scope = rememberCoroutineScope()
    var pendingLockOnForeground by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun currentConfig(): ServerConnectionConfig {
        return ServerConnectionConfig(
            server = server,
            login = login,
            password = password
        )
    }

    suspend fun refreshHomeState() {
        if (server.isBlank() || login.isBlank() || password.isBlank() || avariaStopEnabled) return
        runCatching {
            loadHomeStateFromServer(
                config = currentConfig(),
                goalAmount = goalAmount.ifBlank { storedState.goalAmount },
                deadlineDate = deadlineDate.ifBlank { storedState.deadlineDate },
                deadlineInfinite = deadlineInfinite
            )
        }.onSuccess {
            homeState = it
        }.onFailure {
            if (isEmergencyStopError(buildUserMessage(it))) {
                avariaStopEnabled = true
                screen = HelloScreen.Stopped
            }
        }
    }

    suspend fun refreshAnalyticsState() {
        if (server.isBlank() || login.isBlank() || password.isBlank() || avariaStopEnabled) return
        runCatching {
            loadAnalyticsStateFromServer(currentConfig())
        }.onSuccess {
            analyticsState = it
        }.onFailure {
            if (isEmergencyStopError(buildUserMessage(it))) {
                avariaStopEnabled = true
                screen = HelloScreen.Stopped
            }
        }
    }

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

    LaunchedEffect(screen, homeReloadKey, deadlineDate, deadlineInfinite, goalAmount, avariaStopEnabled) {
        if (screen == HelloScreen.Home) {
            refreshHomeState()
        }
    }

    LaunchedEffect(screen, analyticsReloadKey, avariaStopEnabled) {
        if (screen == HelloScreen.Analytics) {
            refreshAnalyticsState()
        }
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
                    isLoading = isWelcomeLoading,
                    errorMessage = welcomeErrorMessage,
                    onServerChange = { server = it },
                    onLoginChange = { login = it },
                    onPasswordChange = { password = it },
                    onContinue = { nextServer, nextLogin, nextPassword ->
                        if (isWelcomeLoading) return@WelcomeScreen
                        scope.launch {
                            isWelcomeLoading = true
                            welcomeErrorMessage = null
                            runCatching {
                                testServerConnection(
                                    ServerConnectionConfig(
                                        server = nextServer,
                                        login = nextLogin,
                                        password = nextPassword
                                    )
                                )
                            }.onSuccess { avariaEnabled ->
                                server = nextServer
                                login = nextLogin
                                password = nextPassword
                                avariaStopEnabled = avariaEnabled
                                pinErrorMessage = null
                                saveWelcomeSettings(
                                    context = context,
                                    server = nextServer,
                                    login = nextLogin,
                                    password = nextPassword
                                )
                                screen = HelloScreen.CreatePin
                            }.onFailure {
                                welcomeErrorMessage = buildUserMessage(it)
                            }
                            isWelcomeLoading = false
                        }
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
                    isLoading = isFinalSetupLoading,
                    errorMessage = finalSetupErrorMessage,
                    onDeadlineDateChange = { deadlineDate = it },
                    onDeadlineInfiniteChange = { deadlineInfinite = it },
                    onGoalAmountChange = { goalAmount = it },
                    onGoalSyncEnabledChange = { goalSyncEnabled = it },
                    onStrategyModeChange = { strategyMode = it },
                    onProfitPercentChange = { profitPercent = it },
                    onConnect = {
                        if (isFinalSetupLoading) return@FinalSetupScreen
                        scope.launch {
                            isFinalSetupLoading = true
                            finalSetupErrorMessage = null
                            runCatching {
                                startStrategy(
                                    config = currentConfig(),
                                    deadlineDate = deadlineDate,
                                    deadlineInfinite = deadlineInfinite,
                                    strategyMode = strategyMode,
                                    profitPercent = profitPercent
                                )
                            }.onSuccess {
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
                                homeReloadKey += 1
                                analyticsReloadKey += 1
                                screen = HelloScreen.Home
                            }.onFailure {
                                finalSetupErrorMessage = buildUserMessage(it)
                            }
                            isFinalSetupLoading = false
                        }
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
                    state = homeState,
                    onStopStrategyConfirm = {
                        if (stopActionLoading) return@HomeScreen
                        scope.launch {
                            stopActionLoading = true
                            runCatching {
                                toggleAvariaStop(currentConfig())
                            }.onSuccess { newState ->
                                avariaStopEnabled = newState
                                if (newState) {
                                    screen = HelloScreen.Stopped
                                } else {
                                    homeReloadKey += 1
                                    analyticsReloadKey += 1
                                    screen = HelloScreen.Home
                                }
                            }
                            stopActionLoading = false
                        }
                    },
                    onAnalyticsClick = { screen = HelloScreen.Analytics },
                    onLogoutConfirm = {
                        clearStoredHelloState(context)
                        server = ""
                        login = ""
                        password = ""
                        localPin = ""
                        biometryEnabled = false
                        pin = ""
                        confirmPin = ""
                        pinErrorMessage = null
                        unlockPin = ""
                        unlockErrorMessage = null
                        deadlineDate = ""
                        deadlineInfinite = true
                        goalAmount = ""
                        goalSyncEnabled = false
                        strategyMode = StrategyMode.Adaptive
                        profitPercent = ""
                        showBiometryPopup = false
                        isBiometryLoading = false
                        isUnlockBiometryLoading = false
                        isWelcomeLoading = false
                        welcomeErrorMessage = null
                        isFinalSetupLoading = false
                        finalSetupErrorMessage = null
                        stopActionLoading = false
                        avariaStopEnabled = false
                        homeReloadKey += 1
                        analyticsReloadKey += 1
                        homeState = buildHomeFallbackState(
                            goalAmount = "",
                            deadlineDate = "",
                            deadlineInfinite = true
                        )
                        analyticsState = buildAnalyticsFallbackState()
                        screen = HelloScreen.Welcome
                    }
                )

                HelloScreen.Analytics -> AnalyticsScreen(
                    state = analyticsState,
                    onBack = { screen = HelloScreen.Home }
                )

                HelloScreen.Stopped -> StoppedScreen(
                    balanceAmount = homeState.totalAmount,
                    isLoading = stopActionLoading,
                    onResume = {
                        if (stopActionLoading) return@StoppedScreen
                        scope.launch {
                            stopActionLoading = true
                            runCatching {
                                toggleAvariaStop(currentConfig())
                            }.onSuccess { newState ->
                                avariaStopEnabled = newState
                                if (!newState) {
                                    homeReloadKey += 1
                                    analyticsReloadKey += 1
                                    screen = HelloScreen.Home
                                } else {
                                    screen = HelloScreen.Stopped
                                }
                            }
                            stopActionLoading = false
                        }
                    }
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
