package components.page

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.chillinvest.ui.theme.AppMutedText
import com.example.chillinvest.ui.theme.AppPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun WelcomeScreen(
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
