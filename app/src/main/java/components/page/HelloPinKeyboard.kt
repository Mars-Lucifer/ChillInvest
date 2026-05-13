package components.page

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chillinvest.R
import com.example.chillinvest.ui.theme.AppPrimary
import com.example.chillinvest.ui.theme.AppSurface

@Composable
internal fun PinIndicators(activeCount: Int) {
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
internal fun PinKeyboard(
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
