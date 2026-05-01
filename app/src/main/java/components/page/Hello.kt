package components.page

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import com.example.chillinvest.ui.theme.AppMutedText
import com.example.chillinvest.ui.theme.AppOverlay
import com.example.chillinvest.ui.theme.AppPrimary
import com.example.chillinvest.ui.theme.AppPrimaryText
import com.example.chillinvest.ui.theme.AppSurface
import com.example.chillinvest.ui.theme.ChillInvestTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class HelloScreen {
    Welcome,
    CreatePin,
    ConfirmPin,
    Done
}

@Composable
fun HelloFlow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var screen by rememberSaveable { mutableStateOf(HelloScreen.Welcome) }
    var server by rememberSaveable { mutableStateOf("") }
    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
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
                        screen = HelloScreen.Done
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
                screen = HelloScreen.Done
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
            text = "Стартовый сценарий готов к следующему экрану.",
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
private fun HelloInput(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    isPassword: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) AppPrimary else AppBorder,
        label = "hello_input_border"
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(
            color = AppPrimary,
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
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp)
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

private fun saveLocalPin(context: Context, pin: String) {
    runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val preferences = EncryptedSharedPreferences.create(
            context,
            "chill_invest_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        preferences.edit().putString("local_pin", pin).apply()
    }
}

private fun saveBiometryEnabled(context: Context, enabled: Boolean) {
    runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val preferences = EncryptedSharedPreferences.create(
            context,
            "chill_invest_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        preferences.edit().putBoolean("biometry_enabled", enabled).apply()
    }
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

@Preview(showBackground = true, backgroundColor = 0xFF15181F)
@Composable
private fun HelloFlowPreview() {
    ChillInvestTheme {
        HelloFlow()
    }
}
