package components.page

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.chillinvest.ui.theme.AppMutedText
import com.example.chillinvest.ui.theme.AppPrimary
import java.time.LocalDate

@Composable
internal fun FinalSetupScreen(
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
internal fun DoneScreen() {
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
