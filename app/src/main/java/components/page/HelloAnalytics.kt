package components.page

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chillinvest.ui.theme.AppMutedText
import com.example.chillinvest.ui.theme.AppPositive
import com.example.chillinvest.ui.theme.AppPrimary
import com.example.chillinvest.ui.theme.AppSurface

internal fun buildAnalyticsPlaceholderState(): AnalyticsState {
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

@Composable
internal fun AnalyticsScreen(
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
