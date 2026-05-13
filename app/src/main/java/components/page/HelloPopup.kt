package components.page

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chillinvest.R
import com.example.chillinvest.ui.theme.AppAccentOrange
import com.example.chillinvest.ui.theme.AppAccentOrangeSoft
import com.example.chillinvest.ui.theme.AppBackground
import com.example.chillinvest.ui.theme.AppOverlay
import com.example.chillinvest.ui.theme.AppPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun BottomPopupSheet(
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

@Composable
internal fun BiometryPopup(
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
