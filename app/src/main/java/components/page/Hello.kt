package components.page

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricPrompt
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.chillinvest.R
import com.example.chillinvest.ui.theme.AppBackground
import com.example.chillinvest.ui.theme.AppBorder
import com.example.chillinvest.ui.theme.AppDarkAccent
import com.example.chillinvest.ui.theme.AppMutedText
import com.example.chillinvest.ui.theme.AppOverlay
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
    Done
}

private enum class StrategyMode(val title: String) {
    Fixed("Фикс"),
    Adaptive("Адаптив")
}

private data class StoredHelloState(
    val server: String = "",
    val login: String = "",
    val password: String = "",
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
            if (storedState.onboardingCompleted) {
                HelloScreen.Done
            } else {
                HelloScreen.Welcome
            }
        )
    }
    var server by rememberSaveable { mutableStateOf(storedState.server) }
    var login by rememberSaveable { mutableStateOf(storedState.login) }
    var password by rememberSaveable { mutableStateOf(storedState.password) }
    var pin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    var deadlineDate by rememberSaveable { mutableStateOf(storedState.deadlineDate) }
    var deadlineInfinite by rememberSaveable { mutableStateOf(storedState.deadlineInfinite) }
    var goalAmount by rememberSaveable { mutableStateOf(storedState.goalAmount) }
    var goalSyncEnabled by rememberSaveable { mutableStateOf(storedState.goalSyncEnabled) }
    var strategyMode by rememberSaveable { mutableStateOf(storedState.strategyMode) }
    var profitPercent by rememberSaveable { mutableStateOf(storedState.profitPercent) }
    var showBiometryPopup by rememberSaveable { mutableStateOf(false) }
    var isBiometryLoading by rememberSaveable { mutableStateOf(false) }

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
                    onDigitClick = {
                        if (pin.length < 4) {
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
                    }
                )

                HelloScreen.ConfirmPin -> PinScreen(
                    title = "Введите еще раз",
                    pin = confirmPin,
                    onDigitClick = {
                        if (confirmPin.length < 4) {
                            confirmPin += it
                            if (confirmPin.length == 4) {
                                if (confirmPin == pin) {
                                    saveLocalPin(context, pin)
                                    showBiometryPopup = true
                                } else {
                                    confirmPin = ""
                                }
                            }
                        }
                    },
                    onBackspace = {
                        if (confirmPin.isNotEmpty()) {
                            confirmPin = confirmPin.dropLast(1)
                        }
                    }
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
                        screen = HelloScreen.Done
                    }
                )

                HelloScreen.Done -> DoneScreen()
            }
        }

        BiometryPopup(
            visible = showBiometryPopup,
            loading = isBiometryLoading,
            onEnable = {
                val activity = context as? FragmentActivity ?: return@BiometryPopup
                if (isBiometryLoading) return@BiometryPopup

                isBiometryLoading = true
                showBiometricPrompt(
                    activity = activity,
                    onSuccess = {
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
                saveBiometryEnabled(context, false)
                showBiometryPopup = false
                screen = HelloScreen.FinalSetup
            }
        )
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
    pin: String,
    onDigitClick: (Int) -> Unit,
    onBackspace: () -> Unit
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
            AppLogo()
            Spacer(modifier = Modifier.height(66.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = AppPrimary
            )
            Spacer(modifier = Modifier.height(if (title == "Введите еще раз") 153.dp else 107.dp))
            PinIndicators(activeCount = pin.length)
        }

        PinKeyboard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            onDigitClick = onDigitClick,
            onBackspace = onBackspace
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
    onDigitClick: (Int) -> Unit,
    onBackspace: () -> Unit
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
                enabled = false,
                onClick = {}
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
    visible: Boolean,
    loading: Boolean,
    onEnable: () -> Unit,
    onSkip: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)) + slideInVertically(
            animationSpec = tween(320),
            initialOffsetY = { it }
        ),
        exit = fadeOut(animationSpec = tween(180)) + slideOutVertically(
            animationSpec = tween(260),
            targetOffsetY = { it }
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppOverlay)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {}
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(AppBackground)
                    .border(
                        width = 1.dp,
                        color = AppPrimary.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
                    .padding(horizontal = 32.dp, vertical = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 80.dp, height = 4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(AppPrimary)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Image(
                    painter = painterResource(R.drawable.biometry),
                    contentDescription = "Биометрия",
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Подключить биометрию?",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 44.sp, lineHeight = 52.sp),
                    color = AppPrimary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HelloButton(
                        text = "Подключить",
                        enabled = true,
                        loading = loading,
                        onClick = onEnable
                    )
                    HelloButton(
                        text = "Не подключать",
                        enabled = !loading,
                        loading = false,
                        onClick = onSkip,
                        dark = true
                    )
                }
            }
        }
    }
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
