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
    SETTINGS,
    SNAKE,
    PICK_SHORTCUT
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
    ),
    BLUE_8210(
        titleRes = R.string.theme_blue_8210,
        background = Color(0xFF6898C8),
        ink = Color(0xFF0B1828),
        tabBackground = Color(0xFF11243B),
        tabText = Color(0xFFB4D6F8)
    ),
    CYAN_8310(
        titleRes = R.string.theme_cyan_8310,
        background = Color(0xFF58B8B0),
        ink = Color(0xFF08201E),
        tabBackground = Color(0xFF0F2E2B),
        tabText = Color(0xFFA6ECE6)
    ),
    WHITE_PAPER(
        titleRes = R.string.theme_white_paper,
        background = Color(0xFFD8DDD6),
        ink = Color(0xFF141814),
        tabBackground = Color(0xFF222822),
        tabText = Color(0xFFE8EDE6)
    ),
    RED_6610(
        titleRes = R.string.theme_red_6610,
        background = Color(0xFFB85252),
        ink = Color(0xFF1E0808),
        tabBackground = Color(0xFF2E1010),
        tabText = Color(0xFFF4B8B8)
    ),
    MATRIX_NIGHT(
        titleRes = R.string.theme_matrix_night,
        background = Color(0xFF101A12),
        ink = Color(0xFF5CFF78),
        tabBackground = Color(0xFF1D3322),
        tabText = Color(0xFF88FF9E)
    )
}

enum class KeypadBacklight(
    val titleRes: Int,
    val ledColor: Color?
) {
    SYNC_LCD(R.string.backlight_sync, null),
    GREEN_LED(R.string.backlight_green, Color(0xFF76FF03)),
    BLUE_LED(R.string.backlight_blue, Color(0xFF40C4FF)),
    AMBER_LED(R.string.backlight_amber, Color(0xFFFFAB00)),
    WHITE_LED(R.string.backlight_white, Color(0xFFF5F5F5)),
    OFF(R.string.backlight_off, Color.Transparent);

    fun resolveColor(theme: LcdTheme): Color? = when (this) {
        OFF -> null
        SYNC_LCD -> theme.tabText
        else -> ledColor
    }
}

enum class AppLanguage(
    val code: String,
    val titleRes: Int
) {
    AUTO("", R.string.lang_auto),
    PT("pt-BR", R.string.lang_pt),
    EN("en", R.string.lang_en),
    ES("es", R.string.lang_es)
}

enum class WallpaperMode(val titleRes: Int) {
    NONE(R.string.wallpaper_none),
    PIXEL_GRID(R.string.wallpaper_grid),
    RETRO_HORIZON(R.string.wallpaper_horizon),
    CUSTOM_IMAGE(R.string.wallpaper_custom)
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
