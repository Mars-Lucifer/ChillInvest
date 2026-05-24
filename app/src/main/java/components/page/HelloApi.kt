package components.page

import android.util.Base64
import com.example.chillinvest.ui.theme.AppAccentOrange
import com.example.chillinvest.ui.theme.AppAccentYellow
import com.example.chillinvest.ui.theme.AppPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

internal data class ServerConnectionConfig(
    val server: String,
    val login: String,
    val password: String,
)

internal class HelloApiException(
    val statusCode: Int?,
    message: String,
) : Exception(message)

private const val EmergencyStopMessage = "Action blocked by emergency stop"
private const val EmergencyStopMessageAlt = "System is stopped due to emergency"

internal suspend fun testServerConnection(config: ServerConnectionConfig): Boolean {
    return try {
        requestJson(config = config, path = "/test_action", method = "GET")
        false
    } catch (error: HelloApiException) {
        if (error.statusCode == 503 && isEmergencyStopError(error.message)) {
            true
        } else {
            throw error
        }
    }
}

internal suspend fun startStrategy(
    config: ServerConnectionConfig,
    deadlineDate: String,
    deadlineInfinite: Boolean,
    strategyMode: StrategyMode,
    profitPercent: String,
) {
    val normalizedProfitPercent = profitPercent.replace(',', '.').toDoubleOrNull()
        ?: throw HelloApiException(statusCode = null, message = "Введите корректный процент прибыли")

    val body = mutableMapOf<String, Any>(
        "infinite_run" to deadlineInfinite,
        "mode" to strategyMode.toApiMode(),
        "profit_reserve_percent" to normalizedProfitPercent,
    )

    if (deadlineInfinite) {
        body["target_date"] = JSONObject.NULL
    } else {
        body["target_date"] = parseDeadlineDate(deadlineDate)?.toString()
            ?: throw HelloApiException(statusCode = null, message = "Введите корректную дату цели")
    }

    requestJson(
        config = config,
        path = "/start",
        method = "POST",
        body = JSONObject(body).toString(),
    )
}

internal suspend fun loadHomeStateFromServer(
    config: ServerConnectionConfig,
    goalAmount: String,
    deadlineDate: String,
    deadlineInfinite: Boolean,
): HomeState {
    val response = requestJson(config = config, path = "/data", method = "GET")
    val diversification = response.optJSONArray("diversification")
    val portfolioBalance = response.optDouble("portfolio_balance")
    val monthlyGrowth = response.optDouble("monthly_total_growth")
    val normalizedGoal = parseCurrencyValue(goalAmount)
    val progress = if (normalizedGoal > 0) {
        (portfolioBalance / normalizedGoal).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val periodLabel = response.optString("strategy_duration")
        .takeIf { it.isNotBlank() && it != "null" }
        ?: resolveGoalPeriodLabel(deadlineDate = deadlineDate, deadlineInfinite = deadlineInfinite)

    return HomeState(
        totalAmount = formatMoney(portfolioBalance),
        monthlyChange = formatSignedMoney(monthlyGrowth),
        monthlyLabel = "за месяц",
        goal = HomeGoalState(
            targetAmount = if (normalizedGoal > 0) formatCurrencyAmount(normalizedGoal) else "0₽",
            progress = progress,
            progressLabel = "${(progress * 100).toInt()}%",
            periodLabel = periodLabel,
        ),
        portfolio = listOf(
            PortfolioSliceState(
                label = "ОФЗ",
                fraction = ((diversification?.optDouble(0) ?: 0.0) / 100.0).toFloat(),
                color = AppPrimary,
            ),
            PortfolioSliceState(
                label = "Корп.",
                fraction = ((diversification?.optDouble(1) ?: 0.0) / 100.0).toFloat(),
                color = AppAccentOrange,
            ),
            PortfolioSliceState(
                label = "Золото",
                fraction = ((diversification?.optDouble(2) ?: 0.0) / 100.0).toFloat(),
                color = AppAccentYellow,
            ),
        ),
    )
}

internal suspend fun loadAnalyticsStateFromServer(config: ServerConnectionConfig): AnalyticsState {
    val response = requestJson(config = config, path = "/analyze", method = "GET")
    val monthlyCashIncome = response.optDouble("monthly_cash_income")
    val estimatedMonthlyIncome = response.optDouble("estimated_monthly_income")
    val availableToWithdraw = response.optDouble("available_to_withdraw")
    val totalLifetimeIncome = response.optDouble("total_lifetime_income")
    val monthlyGrowth = response.optDouble("monthly_total_growth_duplicate")

    return AnalyticsState(
        totalIncome = formatMoneyCompact(monthlyCashIncome),
        paidOutLabel = "Прогноз в месяц ${formatMoneyCompact(estimatedMonthlyIncome)} ₽",
        payouts = response.optJSONObject("monthly_payouts_history")
            ?.let(::mapPayouts)
            .orEmpty(),
        availableToWithdraw = formatMoneyCompact(availableToWithdraw),
        allTimeYield = formatMoneyCompact(totalLifetimeIncome) + " ₽",
        monthYield = formatMoneyCompact(monthlyGrowth) + " ₽",
        operations = response.optJSONArray("operations_history")
            ?.let(::mapOperations)
            .orEmpty(),
    )
}

internal suspend fun toggleAvariaStop(config: ServerConnectionConfig): Boolean {
    val response = requestJson(config = config, path = "/avaria_stop", method = "DELETE")
    return response.optBoolean("avaria_stop")
}

internal fun buildHomeFallbackState(
    goalAmount: String,
    deadlineDate: String,
    deadlineInfinite: Boolean,
): HomeState {
    val normalizedGoal = parseCurrencyValue(goalAmount)
    return HomeState(
        totalAmount = "0,00",
        monthlyChange = "0,00 ₽",
        monthlyLabel = "за месяц",
        goal = HomeGoalState(
            targetAmount = if (normalizedGoal > 0) formatCurrencyAmount(normalizedGoal) else "0₽",
            progress = 0f,
            progressLabel = "0%",
            periodLabel = resolveGoalPeriodLabel(deadlineDate = deadlineDate, deadlineInfinite = deadlineInfinite),
        ),
        portfolio = listOf(
            PortfolioSliceState(label = "ОФЗ", fraction = 0f, color = AppPrimary),
            PortfolioSliceState(label = "Корп.", fraction = 0f, color = AppAccentOrange),
            PortfolioSliceState(label = "Золото", fraction = 0f, color = AppAccentYellow),
        ),
    )
}

internal fun buildAnalyticsFallbackState(): AnalyticsState {
    return AnalyticsState(
        totalIncome = "0",
        paidOutLabel = "Нет данных",
        payouts = emptyList(),
        availableToWithdraw = "0",
        allTimeYield = "0 ₽",
        monthYield = "0 ₽",
        operations = emptyList(),
    )
}

internal fun isEmergencyStopError(message: String?): Boolean {
    return message?.contains(EmergencyStopMessage, ignoreCase = true) == true ||
        message?.contains(EmergencyStopMessageAlt, ignoreCase = true) == true ||
        message?.contains("stopped due to emergency", ignoreCase = true) == true ||
        message?.contains("аварийный стоп активен", ignoreCase = true) == true
}

internal fun buildUserMessage(error: Throwable): String {
    return when (error) {
        is HelloApiException -> when (error.statusCode) {
            401 -> "Проверьте логин и пароль сервера"
            422 -> error.message ?: "Проверьте введённые параметры"
            502 -> "Сервер не смог получить данные портфеля"
            503 -> if (isEmergencyStopError(error.message)) {
                "Аварийный стоп активен"
            } else {
                error.message ?: "Сервер временно недоступен"
            }
            else -> error.message ?: "Не удалось выполнить запрос к серверу"
        }

        else -> "Не удалось подключиться к серверу"
    }
}

private suspend fun requestJson(
    config: ServerConnectionConfig,
    path: String,
    method: String,
    body: String? = null,
): JSONObject = withContext(Dispatchers.IO) {
    val connection = (URL("${normalizeServerUrl(config.server)}$path").openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = 10_000
        readTimeout = 15_000
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Authorization", basicAuthHeader(config.login, config.password))
        if (body != null) {
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            outputStream.use { stream ->
                stream.write(body.toByteArray(Charsets.UTF_8))
            }
        }
    }

    try {
        val statusCode = connection.responseCode
        val responseText = readResponseText(
            if (statusCode in 200..299) connection.inputStream else connection.errorStream
        )
        if (statusCode !in 200..299) {
            throw HelloApiException(statusCode = statusCode, message = parseErrorMessage(responseText))
        }
        if (responseText.isBlank()) JSONObject() else JSONObject(responseText)
    } catch (error: HelloApiException) {
        throw error
    } catch (error: Exception) {
        throw HelloApiException(statusCode = null, message = error.message ?: "Network error")
    } finally {
        connection.disconnect()
    }
}

private fun readResponseText(inputStream: InputStream?): String {
    if (inputStream == null) return ""
    return BufferedReader(InputStreamReader(inputStream)).use { reader ->
        buildString {
            var line = reader.readLine()
            while (line != null) {
                append(line)
                line = reader.readLine()
            }
        }
    }
}

private fun parseErrorMessage(responseText: String): String {
    if (responseText.isBlank()) return "Сервер вернул ошибку"
    return runCatching {
        val json = JSONObject(responseText)
        when (val detail = json.opt("detail")) {
            is JSONArray -> detail.optJSONObject(0)?.optString("msg")
                ?: detail.optString(0)
            is String -> detail
            else -> json.optString("message")
                .ifBlank { json.optString("error") }
                .ifBlank { responseText }
        }
    }.getOrElse { responseText }
}

private fun normalizeServerUrl(server: String): String {
    val trimmed = server.trim().removeSuffix("/")
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "http://$trimmed"
    }
}

private fun basicAuthHeader(login: String, password: String): String {
    val token = Base64.encodeToString(
        "$login:$password".toByteArray(Charsets.UTF_8),
        Base64.NO_WRAP,
    )
    return "Basic $token"
}

private fun StrategyMode.toApiMode(): String {
    return when (this) {
        StrategyMode.Fixed -> "static"
        StrategyMode.Adaptive -> "adaptive"
    }
}

private fun mapPayouts(payload: JSONObject): List<AnalyticsPayoutState> {
    val today = LocalDate.now()
    return payload.keys().asSequence()
        .map { key -> key to payload.optDouble(key) }
        .sortedBy { (key, _) -> runCatching { LocalDate.parse(key) }.getOrNull() ?: LocalDate.MAX }
        .map { (key, amount) ->
            val date = runCatching { LocalDate.parse(key) }.getOrNull()
            AnalyticsPayoutState(
                day = date?.dayOfMonth?.toString() ?: "-",
                month = date?.month?.getDisplayName(TextStyle.SHORT, Locale("ru"))
                    ?.lowercase()
                    ?.removeSuffix(".")
                    ?: "дата",
                amount = formatMoneyCompact(amount) + "₽",
                isPaidOut = date?.let { !it.isAfter(today) } ?: false,
            )
        }
        .toList()
}

private fun mapOperations(payload: JSONArray): List<AnalyticsOperationState> {
    return buildList {
        for (index in 0 until payload.length()) {
            val item = payload.optJSONObject(index) ?: continue
            val amount = item.optDouble("amount")
            val title = item.optString("operation_name").ifBlank { "Операция" }
            add(
                AnalyticsOperationState(
                    title = title,
                    date = formatOperationDate(item.optString("date")),
                    amount = formatSignedMoney(amount),
                    positive = amount >= 0,
                    badge = buildOperationBadge(title),
                )
            )
        }
    }
}

private fun formatOperationDate(value: String): String {
    return runCatching {
        OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }.getOrDefault(value.ifBlank { "--.--.----" })
}

private fun buildOperationBadge(title: String): String {
    val letters = title.split(' ', '-', '_')
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")
    return letters.ifBlank { "OP" }
}

private fun formatMoney(value: Double): String {
    val formatted = String.format(Locale.US, "%,.2f", kotlin.math.abs(value))
        .replace(",", " ")
        .replace(".", ",")
    return if (value < 0) "-$formatted" else formatted
}

private fun formatMoneyCompact(value: Double): String {
    return formatMoney(value).removeSuffix(",00")
}

private fun formatSignedMoney(value: Double): String {
    val prefix = when {
        value > 0 -> "+"
        value < 0 -> "-"
        else -> ""
    }
    return prefix + formatMoney(kotlin.math.abs(value)) + " ₽"
}
