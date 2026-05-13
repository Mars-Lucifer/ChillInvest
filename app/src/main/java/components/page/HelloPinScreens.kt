package components.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chillinvest.ui.theme.AppAccentOrange
import com.example.chillinvest.ui.theme.AppMutedText
import com.example.chillinvest.ui.theme.AppPrimary

@Composable
internal fun PinScreen(
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
