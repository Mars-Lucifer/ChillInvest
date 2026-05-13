package components.page

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricPrompt
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.chillinvest.R
import com.example.chillinvest.ui.theme.AppBackground
import com.example.chillinvest.ui.theme.AppAccentOrange
import com.example.chillinvest.ui.theme.AppAccentOrangeSoft
import com.example.chillinvest.ui.theme.AppAccentYellow
import com.example.chillinvest.ui.theme.AppBorder
import com.example.chillinvest.ui.theme.AppDarkAccent
import com.example.chillinvest.ui.theme.AppMutedText
import com.example.chillinvest.ui.theme.AppOverlay
import com.example.chillinvest.ui.theme.AppPositive
import com.example.chillinvest.ui.theme.AppPrimary
import com.example.chillinvest.ui.theme.AppPrimaryText
import com.example.chillinvest.ui.theme.AppSurface
import com.example.chillinvest.ui.theme.ChillInvestTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

private const val SecurePrefsName = "chill_invest_secure"
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
private val DeadlineDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT)

private enum class HelloScreen {
    Welcome,
    CreatePin,
    ConfirmPin,
    FinalSetup,
    Unlock,
    Home,
    Analytics
}

private enum class StrategyMode(val title: String) {
    Fixed("Фикс"),
    Adaptive("Адаптив")
}

private data class StoredHelloState(
    val server: String = "",
    val login: String = "",
    val password: String = "",
    val localPin: String = "",
    val biometryEnabled: Boolean = false,
    val deadlineDate: String = "",
    val deadlineInfinite: Boolean = true,
    val goalAmount: String = "",
    val goalSyncEnabled: Boolean = false,
    val strategyMode: StrategyMode = StrategyMode.Adaptive,
    val profitPercent: String = "",
    val onboardingCompleted: Boolean = false
)

@Composable
fun HelloFlow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val storedState = remember(context) { loadStoredHelloState(context) }
    var screen by rememberSaveable {
        mutableStateOf(
            when {
                !storedState.onboardingCompleted -> HelloScreen.Welcome
                storedState.localPin.isNotBlank() -> HelloScreen.Unlock
                else -> HelloScreen.Home
            }
        )
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

@Composable
private fun WelcomeScreen(
    server: String,
    login: String,
    password: String,
    onServerChange: (String) -> Unit,
    onLoginChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onContinue: (String, String, String) -> Unit
) {
    var isLoading by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val canContinue = server.isNotBlank() && login.isNotBlank() && password.isNotBlank() && !isLoading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        AppLogo()
        Spacer(modifier = Modifier.height(66.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Добро пожаловать!",
                style = MaterialTheme.typography.headlineLarge,
                color = AppPrimary
            )
            Text(
                text = "Введите данные подключения для того чтобы продолжить",
                style = MaterialTheme.typography.bodyLarge,
                color = AppMutedText
            )
        }
        Spacer(modifier = Modifier.height(70.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HelloInput(
                value = server,
                placeholder = "Адрес сервера",
                onValueChange = onServerChange,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next
            )
            HelloInput(
                value = login,
                placeholder = "Логин",
                onValueChange = onLoginChange,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
            HelloInput(
                value = password,
                placeholder = "Пароль",
                onValueChange = onPasswordChange,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                isPassword = true
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        HelloButton(
            text = "Продолжить",
            enabled = canContinue,
            loading = isLoading,
            onClick = {
                if (!canContinue) return@HelloButton
                scope.launch {
                    isLoading = true
                    delay(250)
                    onContinue(server.trim(), login.trim(), password)
                }
            }
        )
    }
}

@Composable
private fun PinScreen(
    title: String,
    subtitle: String? = null,
    pin: String,
    errorMessage: String? = null,
    biometryEnabled: Boolean,
    onDigitClick: (Int) -> Unit,
    onBackspace: () -> Unit,
    onBiometryClick: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = 32.dp, bottom = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            AppCompactLogo()
            Spacer(modifier = Modifier.height(72.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = AppPrimary
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppMutedText
                )
            }
            Spacer(modifier = Modifier.height(if (subtitle != null) 145.dp else 122.dp))
            PinIndicators(activeCount = pin.length)
            AnimatedVisibility(visible = errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppAccentOrange,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

        PinKeyboard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            biometryEnabled = biometryEnabled,
            onDigitClick = onDigitClick,
            onBackspace = onBackspace,
            onBiometryClick = onBiometryClick
        )
    }
}

@Composable
private fun FinalSetupScreen(
    deadlineDate: String,
    deadlineInfinite: Boolean,
    goalAmount: String,
    goalSyncEnabled: Boolean,
    strategyMode: StrategyMode,
    profitPercent: String,
    onDeadlineDateChange: (String) -> Unit,
    onDeadlineInfiniteChange: (Boolean) -> Unit,
    onGoalAmountChange: (String) -> Unit,
    onGoalSyncEnabledChange: (Boolean) -> Unit,
    onStrategyModeChange: (StrategyMode) -> Unit,
    onProfitPercentChange: (String) -> Unit,
    onConnect: () -> Unit
) {
    val scrollState = rememberScrollState()
    val minimumDeadlineDate = remember { LocalDate.now().plusMonths(3) }
    val minimumDeadlineText = remember(minimumDeadlineDate) {
        minimumDeadlineDate.format(DeadlineDateFormatter)
    }
    val deadlineIsValid = deadlineInfinite || isDeadlineDateValid(
        value = deadlineDate,
        minimumDate = minimumDeadlineDate
    )
    val deadlineHint = when {
        deadlineInfinite -> "Минимум: $minimumDeadlineText"
        deadlineDate.isBlank() -> "Минимум: $minimumDeadlineText"
        deadlineIsValid -> "Дата подходит"
        else -> "Дата должна быть не раньше $minimumDeadlineText"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        AppLogo()
        Spacer(modifier = Modifier.height(54.dp))
        Text(
            text = "Последняя настройка",
            style = MaterialTheme.typography.headlineLarge,
            color = AppPrimary
        )
        Spacer(modifier = Modifier.height(40.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            HelloSettingsSection(
                title = "Срок",
                description = "К этой дате стратегия будет завершена"
            ) {
                HelloDateInput(
                    value = deadlineDate,
                    placeholder = "Дата",
                    onValueChange = onDeadlineDateChange,
                    enabled = !deadlineInfinite,
                    containerAlpha = if (deadlineInfinite) 0.4f else 1f
                )
                Text(
                    text = deadlineHint,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppMutedText
                )
                HelloSwitchRow(
                    text = "Бесконечно",
                    checked = deadlineInfinite,
                    onCheckedChange = onDeadlineInfiniteChange
                )
            }

            HelloDivider()

            HelloSettingsSection(title = "Цель") {
                HelloInput(
                    value = goalAmount,
                    placeholder = "Сумма ₽",
                    onValueChange = onGoalAmountChange,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )
                HelloSwitchRow(
                    text = "Синхронизировать со сроком",
                    checked = goalSyncEnabled,
                    onCheckedChange = onGoalSyncEnabledChange
                )
                Text(
                    text = "Если активно, система рассчитает необходимую доходность для достижения цели к дате",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppMutedText
                )
            }

            HelloDivider()

            HelloSettingsSection(
                title = "Режим стратегии",
                description = "Система будет автоматически заменять бумаги при росте ключевой ставки"
            ) {
                HelloSegmentedControl(
                    selectedMode = strategyMode,
                    onModeChange = onStrategyModeChange
                )
            }

            HelloDivider()

            HelloSettingsSection(
                title = "Вывод прибыли",
                description = "Система будет запасать X процентов от дохода в месяц для вашего вывода"
            ) {
                HelloInput(
                    value = profitPercent,
                    placeholder = "Проценты (0-100%)",
                    onValueChange = onProfitPercentChange,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
        HelloButton(
            text = "Подключить",
            enabled = deadlineIsValid,
            loading = false,
            onClick = onConnect
        )
    }
}

@Composable
private fun DoneScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppLogo()
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Настройка завершена",
            style = MaterialTheme.typography.headlineLarge,
            color = AppPrimary
        )
        Text(
            text = "Все параметры сохранены на устройстве и готовы к следующему экрану.",
            style = MaterialTheme.typography.bodyLarge,
            color = AppMutedText,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun AppLogo() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Логотип Chill Invest",
            modifier = Modifier.size(44.dp)
        )
        Text(
            text = "Chill.Invest",
            color = AppPrimary,
            fontSize = 34.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 46.sp
        )
    }
}

private data class HomeState(
    val totalAmount: String,
    val monthlyChange: String,
    val monthlyLabel: String,
    val goal: HomeGoalState,
    val portfolio: List<PortfolioSliceState>
)

private data class HomeGoalState(
    val targetAmount: String,
    val progress: Float,
    val progressLabel: String,
    val periodLabel: String
)

private data class PortfolioSliceState(
    val label: String,
    val fraction: Float,
    val color: Color
)

private data class AnalyticsState(
    val totalIncome: String,
    val paidOutLabel: String,
    val payouts: List<AnalyticsPayoutState>,
    val availableToWithdraw: String,
    val allTimeYield: String,
    val monthYield: String,
    val operations: List<AnalyticsOperationState>
)

private data class AnalyticsPayoutState(
    val day: String,
    val month: String,
    val amount: String,
    /** День выплаты уже наступил и выплачен — карточка с пониженной непрозрачностью. */
    val isPaidOut: Boolean = false
)

private data class AnalyticsOperationState(
    val title: String,
    val date: String,
    val amount: String,
    val positive: Boolean,
    val badge: String
)

private enum class PopupButtonStyle {
    Primary,
    Secondary,
    Accent
}

private data class PopupButtonConfig(
    val text: String,
    val style: PopupButtonStyle,
    val onClick: () -> Unit = {},
    val enabled: Boolean = true,
    val loading: Boolean = false,
    val dismissOnClick: Boolean = true
)

private enum class HomePopup {
    Goal,
    Portfolio,
    StopStrategy
}

@Composable
private fun AppCompactLogo() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Логотип Chill Invest",
            modifier = Modifier.size(30.dp)
        )
        Text(
            text = "Chill.Invest",
            style = MaterialTheme.typography.titleLarge,
            color = AppPrimary
        )
    }
}

@Composable
private fun AnalyticsScreen(
    state: AnalyticsState,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val payoutsScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 32.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(64.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            AnalyticsBackButton(
                modifier = Modifier.align(Alignment.CenterStart),
                onClick = onBack
            )
            Text(
                text = "Аналитика",
                style = MaterialTheme.typography.titleLarge,
                color = AppPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "₽",
                        color = AppPrimary,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 54.sp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = state.totalIncome,
                            color = AppPrimary,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 54.sp
                        )
                        Text(
                            text = state.paidOutLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppMutedText
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(payoutsScrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.payouts.forEach { payout ->
                        AnalyticsPayoutItem(payout = payout)
                    }
                }
            }

            HomeCard {
                Text(
                    text = "Доходность",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(AppSurface)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "₽",
                            color = AppPositive,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 46.sp
                        )
                        Text(
                            text = state.availableToWithdraw,
                            color = AppPositive,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 46.sp
                        )
                    }
                    Text(
                        text = "Доступно к выводу",
                        style = MaterialTheme.typography.labelLarge,
                        color = AppPrimary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                AnalyticsYieldRow(
                    value = state.allTimeYield,
                    label = "Все время"
                )
                Spacer(modifier = Modifier.height(12.dp))
                AnalyticsYieldRow(
                    value = state.monthYield,
                    label = "Этот месяц"
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "История операций",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppPrimary
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.operations.forEach { operation ->
                        AnalyticsOperationRow(operation = operation)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsBackButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = 800f),
        label = "analytics_back_button_scale"
    )

    Box(
        modifier = modifier
            .size(40.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val stroke = 2.dp.toPx()
            drawLine(
                color = AppPrimary,
                start = Offset(size.width * 0.68f, size.height * 0.2f),
                end = Offset(size.width * 0.34f, size.height * 0.5f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = AppPrimary,
                start = Offset(size.width * 0.34f, size.height * 0.5f),
                end = Offset(size.width * 0.68f, size.height * 0.8f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun analyticsPayoutAmountSegments(amount: String): List<Pair<Boolean, String>> {
    val cleaned = amount.filter { it != ' ' && it != '\u00A0' }
    if (cleaned.isEmpty()) return emptyList()
    fun isMonoRegion(c: Char) = c.isDigit()
    val segments = mutableListOf<Pair<Boolean, String>>()
    var i = 0
    while (i < cleaned.length) {
        val mono = isMonoRegion(cleaned[i])
        val start = i
        i++
        while (i < cleaned.length && isMonoRegion(cleaned[i]) == mono) i++
        segments.add(mono to cleaned.substring(start, i))
    }
    return segments
}

@Composable
private fun AnalyticsPayoutAmountText(amount: String, modifier: Modifier = Modifier) {
    val segments = remember(amount) { analyticsPayoutAmountSegments(amount) }
    val baseStyle = TextStyle(
        color = AppPositive,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontFamily = FontFamily.SansSerif
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        segments.forEach { (useMono, text) ->
            Text(
                text = text,
                style = if (useMono) {
                    baseStyle.copy(fontFamily = FontFamily.Monospace)
                } else {
                    baseStyle
                }
            )
        }
    }
}

@Composable
private fun AnalyticsPayoutItem(
    modifier: Modifier = Modifier,
    payout: AnalyticsPayoutState
) {
    val paidOutAlpha = if (payout.isPaidOut) 0.4f else 1f
    Column(
        modifier = modifier
            .alpha(paidOutAlpha)
            .width(IntrinsicSize.Max),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(AppSurface)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = payout.day,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = AppPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 46.sp
            )
            Text(
                text = payout.month,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = AppPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 24.sp
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(AppPositive.copy(alpha = 0.2f))
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnalyticsPayoutAmountText(
                amount = payout.amount,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AnalyticsYieldRow(
    value: String,
    label: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = value,
            color = AppPrimary,
            fontSize = 34.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 46.sp
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = AppMutedText
        )
    }
}

@Composable
private fun AnalyticsOperationRow(operation: AnalyticsOperationState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AppSurface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = operation.badge,
                style = MaterialTheme.typography.labelLarge,
                color = AppPrimary
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = operation.title,
                style = MaterialTheme.typography.labelLarge,
                color = AppPrimary
            )
            Text(
                text = operation.date,
                style = MaterialTheme.typography.bodyLarge,
                color = AppMutedText
            )
        }
        Text(
            text = operation.amount,
            style = MaterialTheme.typography.labelLarge,
            color = if (operation.positive) AppPositive else AppMutedText
        )
    }
}

@Composable
private fun HomeScreen(
    state: HomeState,
    onStopStrategyConfirm: () -> Unit,
    onAnalyticsClick: () -> Unit
) {
    var activePopup by rememberSaveable { mutableStateOf<HomePopup?>(null) }
    val blurRadius by animateDpAsState(
        targetValue = if (activePopup != null) 18.dp else 0.dp,
        animationSpec = tween(300),
        label = "home_popup_blur"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = 24.dp, top = 32.dp, end = 24.dp, bottom = 70.dp),
            verticalArrangement = Arrangement.spacedBy(64.dp)
        ) {
            AppCompactLogo()

            Column(verticalArrangement = Arrangement.spacedBy(40.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "₽",
                                color = AppPrimary,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 54.sp
                            )
                            BalanceAmountText(amount = state.totalAmount)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = state.monthlyChange,
                                style = MaterialTheme.typography.bodyLarge,
                                color = AppPositive
                            )
                            Text(
                                text = state.monthlyLabel,
                                style = MaterialTheme.typography.bodyLarge,
                                color = AppPrimary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        HomeActionButton(
                            modifier = Modifier.weight(1f),
                            text = "Завершить",
                            iconRes = R.drawable.close,
                            containerColor = AppAccentOrangeSoft,
                            contentColor = AppAccentOrange,
                            onClick = { activePopup = HomePopup.StopStrategy }
                        )
                        HomeActionButton(
                            modifier = Modifier.weight(1f),
                            text = "Аналитика",
                            iconRes = R.drawable.anal,
                            containerColor = AppSurface,
                            contentColor = AppPrimary,
                            onClick = onAnalyticsClick
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HomeCard(onClick = { activePopup = HomePopup.Goal }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "Цель",
                                style = MaterialTheme.typography.titleLarge,
                                color = AppPrimary
                            )
                            Text(
                                text = state.goal.targetAmount,
                                style = MaterialTheme.typography.bodyLarge,
                                color = AppMutedText
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            GoalProgressBar(
                                modifier = Modifier.weight(1f),
                                progress = state.goal.progress
                            )
                            Text(
                                text = state.goal.progressLabel,
                                style = MaterialTheme.typography.headlineLarge,
                                color = AppPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        GoalPeriodButton(
                            text = state.goal.periodLabel,
                            onClick = { activePopup = HomePopup.Goal }
                        )
                    }

                    HomeCard(onClick = { activePopup = HomePopup.Portfolio }) {
                        Text(
                            text = "Портфель",
                            style = MaterialTheme.typography.titleLarge,
                            color = AppPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            state.portfolio.forEach { slice ->
                                PortfolioSliceBar(
                                    modifier = Modifier.weight(1f),
                                    slice = slice
                                )
                            }
                        }
                    }
                }
            }
        }

        when (activePopup) {
            HomePopup.Goal -> HomeInfoPopup(
                title = "Цель",
                description = "Это ваша цель которую вы установили при настройке приложения. Виджет показывает сколько процентов накоплено, а снизу отображается расчет сколько понадобится времени для достижения цели",
                onDismiss = { activePopup = null }
            )

            HomePopup.Portfolio -> HomeInfoPopup(
                title = "Портфель",
                description = "Отображает уровень диверсификации портфеля в диаграммах, показывая сколько процентов от портфеля вложено в те или иные активы",
                onDismiss = { activePopup = null }
            )

            HomePopup.StopStrategy -> StopStrategyPopup(
                onDismiss = { activePopup = null },
                onConfirm = {
                    onStopStrategyConfirm()
                }
            )

            null -> Unit
        }
    }
}

@Composable
private fun BalanceAmountText(amount: String) {
    val delimiterIndex = amount.indexOf(',')
    val primaryPart = if (delimiterIndex >= 0) amount.substring(0, delimiterIndex) else amount
    val decimalPart = if (delimiterIndex >= 0) amount.substring(delimiterIndex) else ""

    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = primaryPart,
            color = AppPrimary,
            fontSize = 44.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 54.sp
        )
        if (decimalPart.isNotEmpty()) {
            Text(
                text = decimalPart,
                color = AppMutedText,
                fontSize = 44.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 54.sp
            )
        }
    }
}

@Composable
private fun HomeActionButton(
    modifier: Modifier = Modifier,
    text: String,
    iconRes: Int,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = 700f),
        label = "home_action_button_scale"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(28.dp))
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor
        )
    }
}

@Composable
private fun HomeCard(
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .border(1.dp, AppBorder, RoundedCornerShape(32.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = onClick != null,
                onClick = { onClick?.invoke() }
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        content = content
    )
}

@Composable
private fun GoalProgressBar(
    modifier: Modifier = Modifier,
    progress: Float
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppSurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(AppPrimary)
        )
    }
}

@Composable
private fun GoalPeriodButton(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.calend),
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = AppPrimary
        )
    }
}

@Composable
private fun PortfolioSliceBar(
    modifier: Modifier = Modifier,
    slice: PortfolioSliceState
) {
    Box(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppSurface)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .fillMaxHeight(slice.fraction.coerceIn(0.18f, 1f))
                .clip(RoundedCornerShape(12.dp))
                .background(slice.color)
                .padding(8.dp)
        ) {
            Text(
                text = slice.label,
                style = MaterialTheme.typography.bodyLarge,
                color = AppPrimaryText
            )
        }
    }
}

@Composable
private fun HomeInfoPopup(
    title: String,
    description: String,
    onDismiss: () -> Unit
) {
    BottomPopupSheet(
        onDismissRequest = onDismiss,
        title = title,
        body = description,
        buttons = listOf(
            PopupButtonConfig(
                text = "Понятно!",
                style = PopupButtonStyle.Primary
            )
        )
    )
}

@Composable
private fun StopStrategyPopup(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    BottomPopupSheet(
        onDismissRequest = onDismiss,
        iconRes = R.drawable.close,
        title = "Вы уверены?",
        body = "Отключение стратегии отключит все связи приложения с вашим брокерским счетом. Для возобновления потребуется заново пройти настройку",
        buttons = listOf(
            PopupButtonConfig(
                text = "Остановить",
                style = PopupButtonStyle.Accent,
                onClick = onConfirm
            ),
            PopupButtonConfig(
                text = "Отмена",
                style = PopupButtonStyle.Secondary
            )
        )
    )
}

@Composable
private fun BottomPopupSheet(
    onDismissRequest: () -> Unit,
    iconRes: Int? = null,
    title: String,
    body: String,
    buttons: List<PopupButtonConfig>
) {
    val scope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }
    var isClosing by remember { mutableStateOf(false) }
    val overlayAlpha by animateFloatAsState(
        targetValue = if (isVisible) 0.4f else 0f,
        animationSpec = tween(durationMillis = if (isVisible) 220 else 180),
        label = "popup_overlay_alpha"
    )

    LaunchedEffect(Unit) {
        isVisible = true
    }

    fun requestClose() {
        if (isClosing) return
        scope.launch {
            isClosing = true
            isVisible = false
            delay(260)
            onDismissRequest()
        }
    }

    BackHandler(onBack = ::requestClose)

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppOverlay.copy(alpha = overlayAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !isClosing,
                    onClick = ::requestClose
                )
        )

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(220)) + slideInVertically(
                animationSpec = tween(320),
                initialOffsetY = { it }
            ),
            exit = fadeOut(animationSpec = tween(180)) + slideOutVertically(
                animationSpec = tween(260),
                targetOffsetY = { it }
            ),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .border(
                        width = 1.dp,
                        color = AppPrimary.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
                    .background(AppBackground)
                    .padding(horizontal = 32.dp, vertical = 32.dp)
                    .height(500.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 80.dp, height = 4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(AppPrimary)
                )
                Spacer(modifier = Modifier.height(28.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (iconRes != null) {
                        Image(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Text(
                        text = title,
                        color = AppPrimary,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 54.sp
                    )
                    if (body.isNotBlank()) {
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    buttons.take(2).forEach { button ->
                        PopupActionButton(
                            button = button,
                            onDismiss = ::requestClose
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PopupActionButton(
    button: PopupButtonConfig,
    onDismiss: () -> Unit
) {
    val handleClick = {
        button.onClick()
        if (button.dismissOnClick) {
            onDismiss()
        }
    }

    when (button.style) {
        PopupButtonStyle.Primary -> HelloButton(
            text = button.text,
            enabled = button.enabled,
            loading = button.loading,
            onClick = handleClick
        )

        PopupButtonStyle.Secondary -> HelloButton(
            text = button.text,
            enabled = button.enabled,
            loading = button.loading,
            onClick = handleClick,
            dark = true
        )

        PopupButtonStyle.Accent -> PopupAccentButton(
            text = button.text,
            enabled = button.enabled,
            onClick = handleClick
        )
    }
}

@Composable
private fun PopupAccentButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        animationSpec = spring(stiffness = 700f),
        label = "popup_action_button_scale"
    )
    val containerColor by animateColorAsState(
        targetValue = if (enabled) AppAccentOrangeSoft else AppAccentOrangeSoft.copy(alpha = 0.45f),
        label = "popup_action_button_background"
    )
    val contentColor by animateColorAsState(
        targetValue = if (enabled) AppAccentOrange else AppAccentOrange.copy(alpha = 0.55f),
        label = "popup_action_button_content"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(28.dp))
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor
        )
    }
}

private fun buildHomePlaceholderState(
    goalAmount: String,
    deadlineDate: String,
    deadlineInfinite: Boolean,
    profitPercent: String
): HomeState {
    // TODO: replace this stub with portfolio summary and allocation GET requests.
    val totalAmountValue = 100_000
    val normalizedGoal = parseCurrencyValue(goalAmount)
    val progress = if (normalizedGoal > 0) {
        (totalAmountValue.toFloat() / normalizedGoal.toFloat()).coerceIn(0f, 1f)
    } else {
        0.2f
    }
    val periodLabel = resolveGoalPeriodLabel(
        deadlineDate = deadlineDate,
        deadlineInfinite = deadlineInfinite
    )
    val profitLabel = profitPercent.filter(Char::isDigit).takeIf { it.isNotBlank() }
        ?.let { "+12 400 ($it%)" }
        ?: "+12 400 (16%)"

    return HomeState(
        totalAmount = "100 000,00",
        monthlyChange = profitLabel,
        monthlyLabel = "за месяц",
        goal = HomeGoalState(
            targetAmount = if (normalizedGoal > 0) formatCurrencyAmount(normalizedGoal) else "500 000₽",
            progress = progress,
            progressLabel = "${(progress * 100).toInt()}%",
            periodLabel = periodLabel
        ),
        portfolio = listOf(
            PortfolioSliceState(label = "ОФЗ", fraction = 0.64f, color = AppPrimary),
            PortfolioSliceState(label = "Корп.", fraction = 0.5f, color = AppAccentOrange),
            PortfolioSliceState(label = "Золото", fraction = 0.76f, color = AppAccentYellow)
        )
    )
}

private fun buildAnalyticsPlaceholderState(): AnalyticsState {
    return AnalyticsState(
        totalIncome = "5 000",
        paidOutLabel = "1 500 выплачено",
        payouts = listOf(
            AnalyticsPayoutState(day = "5", month = "май", amount = "500₽", isPaidOut = true),
            AnalyticsPayoutState(day = "10", month = "май", amount = "1500₽", isPaidOut = true),
            AnalyticsPayoutState(day = "15", month = "май", amount = "25000₽", isPaidOut = true),
            AnalyticsPayoutState(day = "20", month = "май", amount = "200₽", isPaidOut = false),
            AnalyticsPayoutState(day = "24", month = "май", amount = "1500₽", isPaidOut = false),
            AnalyticsPayoutState(day = "31", month = "май", amount = "9999₽", isPaidOut = false)
        ),
        availableToWithdraw = "7 000",
        allTimeYield = "16%",
        monthYield = "12%",
        operations = listOf(
            AnalyticsOperationState(
                title = "Покупка Яндекс",
                date = "22.04.2026",
                amount = "-20 000₽",
                positive = false,
                badge = "YA"
            ),
            AnalyticsOperationState(
                title = "Продажа Газпром",
                date = "23.04.2026",
                amount = "+15 500₽",
                positive = true,
                badge = "GA"
            ),
            AnalyticsOperationState(
                title = "Покупка Сбербанк",
                date = "24.04.2026",
                amount = "-10 000₽",
                positive = false,
                badge = "SB"
            ),
            AnalyticsOperationState(
                title = "Дивиденды Лукойл",
                date = "25.04.2026",
                amount = "+2 400₽",
                positive = true,
                badge = "LK"
            ),
            AnalyticsOperationState(
                title = "Продажа МТС",
                date = "26.04.2026",
                amount = "+8 750₽",
                positive = true,
                badge = "MT"
            )
        )
    )
}

private fun parseCurrencyValue(value: String): Int {
    return value.filter(Char::isDigit).toIntOrNull() ?: 0
}

private fun formatCurrencyAmount(value: Int): String {
    return value.toString()
        .reversed()
        .chunked(3)
        .joinToString(" ")
        .reversed() + "₽"
}

private fun resolveGoalPeriodLabel(
    deadlineDate: String,
    deadlineInfinite: Boolean
): String {
    if (deadlineInfinite) return "Без срока"

    val parsedDate = parseDeadlineDate(deadlineDate) ?: return "6 месяцев"
    val monthsLeft = ((parsedDate.year - LocalDate.now().year) * 12) +
        (parsedDate.monthValue - LocalDate.now().monthValue)

    return when {
        monthsLeft <= 0 -> deadlineDate
        monthsLeft == 1 -> "1 месяц"
        monthsLeft in 2..4 -> "$monthsLeft месяца"
        else -> "$monthsLeft месяцев"
    }
}

@Composable
private fun HelloSettingsSection(
    title: String,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = AppPrimary
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = AppMutedText
            )
        }
        content()
    }
}

@Composable
private fun HelloDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AppBorder)
    )
}

@Composable
private fun HelloInput(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    isPassword: Boolean = false,
    enabled: Boolean = true,
    containerAlpha: Float = 1f
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (enabled && isFocused) AppPrimary else AppBorder,
        label = "hello_input_border"
    )
    val textColor = if (enabled) AppPrimary else AppMutedText

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(containerAlpha)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        if (enabled) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = imeAction
                ),
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = AppMutedText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 24.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
        } else {
            Text(
                text = value.ifEmpty { placeholder },
                color = AppMutedText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
private fun HelloDateInput(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    containerAlpha: Float = 1f
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (enabled && isFocused) AppPrimary else AppBorder,
        label = "hello_date_input_border"
    )
    val textColor = if (enabled) AppPrimary else AppMutedText
    val textFieldValue = remember(value) {
        TextFieldValue(
            text = value,
            selection = TextRange(value.length)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(containerAlpha)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        if (enabled) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { updatedValue ->
                    onValueChange(formatDeadlineDateInput(updatedValue.text))
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = AppMutedText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 24.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
        } else {
            Text(
                text = value.ifEmpty { placeholder },
                color = AppMutedText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
private fun HelloSwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HelloSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = AppMutedText
        )
    }
}

@Composable
private fun HelloSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val backgroundColor by animateColorAsState(
        targetValue = if (checked) AppPrimary else AppSurface,
        label = "hello_switch_background"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) AppDarkAccent else AppPrimary,
        label = "hello_switch_thumb"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 18.dp else 0.dp,
        animationSpec = tween(180),
        label = "hello_switch_offset"
    )

    Box(
        modifier = Modifier
            .width(52.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onCheckedChange(!checked) }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = thumbOffset)
                .size(22.dp)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

@Composable
private fun HelloSegmentedControl(
    selectedMode: StrategyMode,
    onModeChange: (StrategyMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppSurface)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        StrategyMode.values().forEach { mode ->
            val selected = mode == selectedMode
            val backgroundColor by animateColorAsState(
                targetValue = if (selected) AppPrimary else Color.Transparent,
                label = "segmented_background_${mode.name}"
            )
            val textColor by animateColorAsState(
                targetValue = if (selected) AppPrimaryText else AppMutedText,
                label = "segmented_text_${mode.name}"
            )
            val interactionSource = remember { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onModeChange(mode) }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun HelloButton(
    text: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    dark: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = 700f),
        label = "hello_button_scale"
    )
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled && !dark -> AppPrimary.copy(alpha = 0.25f)
            !enabled && dark -> Color(0xFF202329)
            dark -> AppSurface
            else -> AppPrimary
        },
        label = "hello_button_background"
    )
    val contentColor by animateColorAsState(
        targetValue = if (dark) AppPrimary else if (enabled) AppPrimaryText else Color(0xFF8C9099),
        label = "hello_button_content"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(28.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !loading,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = if (dark) AppPrimary else AppPrimaryText,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
}

@Composable
private fun PinIndicators(activeCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        repeat(4) { index ->
            val isActive = index < activeCount
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (isActive) AppPrimary else AppSurface)
            )
        }
    }
}

@Composable
private fun PinKeyboard(
    modifier: Modifier = Modifier,
    biometryEnabled: Boolean,
    onDigitClick: (Int) -> Unit,
    onBackspace: () -> Unit,
    onBiometryClick: (() -> Unit)?
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PinRow(listOf(1, 2, 3), onDigitClick)
        PinRow(listOf(4, 5, 6), onDigitClick)
        PinRow(listOf(7, 8, 9), onDigitClick)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconKey(
                modifier = Modifier.weight(1f),
                painterRes = R.drawable.biometry,
                enabled = biometryEnabled,
                onClick = { onBiometryClick?.invoke() }
            )
            DigitKey(
                modifier = Modifier.weight(1f),
                digit = 0,
                onClick = { onDigitClick(0) }
            )
            IconKey(
                modifier = Modifier.weight(1f),
                painterRes = R.drawable.backspace,
                enabled = true,
                onClick = onBackspace
            )
        }
    }
}

@Composable
private fun PinRow(
    digits: List<Int>,
    onDigitClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        digits.forEach { digit ->
            DigitKey(
                modifier = Modifier.weight(1f),
                digit = digit,
                onClick = { onDigitClick(digit) }
            )
        }
    }
}

@Composable
private fun DigitKey(
    modifier: Modifier = Modifier,
    digit: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = 800f),
        label = "digit_key_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit.toString(),
            color = AppPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 38.sp
        )
    }
}

@Composable
private fun IconKey(
    modifier: Modifier = Modifier,
    painterRes: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(stiffness = 800f),
        label = "icon_key_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(painterRes),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            alpha = if (enabled) 1f else 0.85f
        )
    }
}

@Composable
private fun BiometryPopup(
    loading: Boolean,
    onEnable: () -> Unit,
    onSkip: () -> Unit
) {
    BottomPopupSheet(
        onDismissRequest = onSkip,
        iconRes = R.drawable.biometry,
        title = "Подключить биометрию?",
        body = "",
        buttons = listOf(
            PopupButtonConfig(
                text = "Подключить",
                style = PopupButtonStyle.Primary,
                onClick = onEnable,
                loading = loading,
                dismissOnClick = false
            ),
            PopupButtonConfig(
                text = "Не подключать",
                style = PopupButtonStyle.Secondary,
                onClick = onSkip,
                enabled = !loading
            )
        )
    )
}

private fun securePreferences(context: Context): SharedPreferences? {
    return runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            SecurePrefsName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrNull()
}

private fun loadStoredHelloState(context: Context): StoredHelloState {
    val preferences = securePreferences(context) ?: return StoredHelloState()
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

private fun saveWelcomeSettings(
    context: Context,
    server: String,
    login: String,
    password: String
) {
    securePreferences(context)?.edit()
        ?.putString(KeyServer, server)
        ?.putString(KeyLogin, login)
        ?.putString(KeyPassword, password)
        ?.apply()
}

private fun saveLocalPin(context: Context, pin: String) {
    securePreferences(context)?.edit()
        ?.putString(KeyLocalPin, pin)
        ?.apply()
}

private fun saveBiometryEnabled(context: Context, enabled: Boolean) {
    securePreferences(context)?.edit()
        ?.putBoolean(KeyBiometryEnabled, enabled)
        ?.apply()
}

private fun saveFinalSetupSettings(
    context: Context,
    deadlineDate: String,
    deadlineInfinite: Boolean,
    goalAmount: String,
    goalSyncEnabled: Boolean,
    strategyMode: StrategyMode,
    profitPercent: String
) {
    securePreferences(context)?.edit()
        ?.putString(KeyDeadlineDate, deadlineDate)
        ?.putBoolean(KeyDeadlineInfinite, deadlineInfinite)
        ?.putString(KeyGoalAmount, goalAmount)
        ?.putBoolean(KeyGoalSyncEnabled, goalSyncEnabled)
        ?.putString(KeyStrategyMode, strategyMode.name)
        ?.putString(KeyProfitPercent, profitPercent)
        ?.apply()
}

private fun saveOnboardingCompleted(context: Context, completed: Boolean) {
    securePreferences(context)?.edit()
        ?.putBoolean(KeyOnboardingCompleted, completed)
        ?.apply()
}

private fun showBiometricPrompt(
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

private fun formatDeadlineDateInput(value: String): String {
    val digits = value.filter(Char::isDigit).take(8)
    val builder = StringBuilder()

    digits.forEachIndexed { index, char ->
        if (index == 2 || index == 4) {
            builder.append('.')
        }
        builder.append(char)
    }

    return builder.toString()
}

private fun isDeadlineDateValid(
    value: String,
    minimumDate: LocalDate = LocalDate.now().plusMonths(3)
): Boolean {
    val parsedDate = parseDeadlineDate(value) ?: return false
    return !parsedDate.isBefore(minimumDate)
}

private fun parseDeadlineDate(value: String): LocalDate? {
    if (value.length != 10) return null
    return runCatching {
        LocalDate.parse(value, DeadlineDateFormatter)
    }.getOrNull()
}

@Preview(showBackground = true, backgroundColor = 0xFF15181F)
@Composable
private fun HelloFlowPreview() {
    ChillInvestTheme {
        HelloFlow()
    }
}
