package components.page

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chillinvest.ui.theme.AppAccentOrange
import com.example.chillinvest.ui.theme.AppMutedText

@Composable
internal fun StoppedScreen(
    balanceAmount: String,
    isLoading: Boolean,
    onResume: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        AppCompactLogo()

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "₽ $balanceAmount",
                color = AppMutedText,
                fontSize = 44.sp,
                lineHeight = 54.sp,
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Временно остановлено",
                style = MaterialTheme.typography.titleLarge,
                color = AppAccentOrange
            )
        }

        HelloButton(
            text = "Включить",
            enabled = !isLoading,
            loading = isLoading,
            onClick = onResume
        )
    }
}
