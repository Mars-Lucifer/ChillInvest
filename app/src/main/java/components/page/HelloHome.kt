package components.page

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chillinvest.R
import com.example.chillinvest.ui.theme.AppAccentOrange
import com.example.chillinvest.ui.theme.AppAccentOrangeSoft
import com.example.chillinvest.ui.theme.AppAccentYellow
import com.example.chillinvest.ui.theme.AppBorder
import com.example.chillinvest.ui.theme.AppMutedText
import com.example.chillinvest.ui.theme.AppPositive
import com.example.chillinvest.ui.theme.AppPrimary
import com.example.chillinvest.ui.theme.AppPrimaryText
import com.example.chillinvest.ui.theme.AppSurface
import java.time.LocalDate

internal fun buildHomePlaceholderState(
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
internal fun HomeScreen(
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
internal fun HomeCard(
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
                .clip(RoundedCornerShape(12.dp))
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
