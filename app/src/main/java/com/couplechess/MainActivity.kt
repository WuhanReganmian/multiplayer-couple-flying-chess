package com.couplechess

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw edge-to-edge (status bar transparent, content fills screen)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            CoupleChessApp(context = applicationContext)
        }
    }
}
