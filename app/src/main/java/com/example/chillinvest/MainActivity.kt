package com.example.chillinvest

import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import components.page.HelloFlow
import com.example.chillinvest.ui.theme.ChillInvestTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = TRANSPARENT
        window.navigationBarColor = TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent {
            ChillInvestTheme {
                HelloFlow()
            }
        }
    }
}
