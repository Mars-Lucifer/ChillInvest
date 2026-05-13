package components.page

import androidx.compose.ui.graphics.Color

internal enum class HelloScreen {
    Welcome,
    CreatePin,
    ConfirmPin,
    FinalSetup,
    Unlock,
    Home,
    Analytics
}

internal enum class StrategyMode(val title: String) {
    Fixed("Фикс"),
    Adaptive("Адаптив")
}

internal data class StoredHelloState(
    val server: String = "",
    val login: String = "",
    val password: String = "",
    val localPin: String = "",
    val biometryEnabled: Boolean = false,
    val deadlineDate: String = "",
    val deadlineInfinite: Boolean = true,
    val goalAmount: String = "",
    val goalSyncEnabled: Boolean = false,
    val strategyMode: StrategyMode = StrategyMode.Adaptive,
    val profitPercent: String = "",
    val onboardingCompleted: Boolean = false
)

internal data class HomeState(
    val totalAmount: String,
    val monthlyChange: String,
    val monthlyLabel: String,
    val goal: HomeGoalState,
    val portfolio: List<PortfolioSliceState>
)

internal data class HomeGoalState(
    val targetAmount: String,
    val progress: Float,
    val progressLabel: String,
    val periodLabel: String
)

internal data class PortfolioSliceState(
    val label: String,
    val fraction: Float,
    val color: Color
)

internal data class AnalyticsState(
    val totalIncome: String,
    val paidOutLabel: String,
    val payouts: List<AnalyticsPayoutState>,
    val availableToWithdraw: String,
    val allTimeYield: String,
    val monthYield: String,
    val operations: List<AnalyticsOperationState>
)

internal data class AnalyticsPayoutState(
    val day: String,
    val month: String,
    val amount: String,
    /** День выплаты уже наступил и выплачен — карточка с пониженной непрозрачностью. */
    val isPaidOut: Boolean = false
)

internal data class AnalyticsOperationState(
    val title: String,
    val date: String,
    val amount: String,
    val positive: Boolean,
    val badge: String
)

internal enum class PopupButtonStyle {
    Primary,
    Secondary,
    Accent
}

internal data class PopupButtonConfig(
    val text: String,
    val style: PopupButtonStyle,
    val onClick: () -> Unit = {},
    val enabled: Boolean = true,
    val loading: Boolean = false,
    val dismissOnClick: Boolean = true
)

internal enum class HomePopup {
    Goal,
    Portfolio,
    StopStrategy
}
