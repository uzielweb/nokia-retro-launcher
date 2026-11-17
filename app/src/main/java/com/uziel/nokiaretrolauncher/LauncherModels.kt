package com.uziel.nokiaretrolauncher

import android.content.ComponentName
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

data class AppEntry(
    val label: String,
    val packageName: String,
    val componentName: ComponentName,
    val icon: ImageBitmap?
)

enum class ScreenMode {
    HOME,
    MENU,
    GO_TO,
    SETTINGS
}

enum class LcdTheme(
    val titleRes: Int,
    val background: Color,
    val ink: Color,
    val tabBackground: Color,
    val tabText: Color
) {
    MUSTARD(
        titleRes = R.string.theme_mustard,
        background = Color(0xFFA49F24),
        ink = Color(0xFF141408),
        tabBackground = Color(0xFF22241C),
        tabText = Color(0xFFD6D27A)
    ),
    GREEN_3310(
        titleRes = R.string.theme_green,
        background = Color(0xFF8AA872),
        ink = Color(0xFF121A0E),
        tabBackground = Color(0xFF1C2617),
        tabText = Color(0xFFBBD8A3)
    ),
    AMBER(
        titleRes = R.string.theme_amber,
        background = Color(0xFFC88B22),
        ink = Color(0xFF1A1105),
        tabBackground = Color(0xFF281D0D),
        tabText = Color(0xFFF0BE66)
    )
}

data class DeviceStatus(
    val batteryPercent: Int = 37,
    val isWifiConnected: Boolean = true,
    val hasMobileData: Boolean = true,
    val signalBars: Int = 4,
    val dateText: String = "Thu, 15 Oct",
    val timeText: String = "04:53"
)

enum class DpadDirection {
    UP, DOWN, LEFT, RIGHT, CENTER
}
