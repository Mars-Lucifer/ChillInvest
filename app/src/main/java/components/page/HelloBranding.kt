package components.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chillinvest.R
import com.example.chillinvest.ui.theme.AppPrimary

@Composable
internal fun AppLogo() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Логотип Chill Invest",
            modifier = Modifier.size(44.dp)
        )
        Text(
            text = "Chill.Invest",
            color = AppPrimary,
            fontSize = 34.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 46.sp
        )
    }
}

@Composable
internal fun AppCompactLogo() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Логотип Chill Invest",
            modifier = Modifier.size(30.dp)
        )
        Text(
            text = "Chill.Invest",
            style = MaterialTheme.typography.titleLarge,
            color = AppPrimary
        )
    }
}
