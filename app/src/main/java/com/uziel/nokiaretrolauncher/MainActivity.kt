package com.uziel.nokiaretrolauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var backAction: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backAction?.invoke()
            }
        })

        setContent {
            NokiaLauncherRoot(
                registerBackHandler = { handler -> backAction = handler }
            )
        }
    }
}

@Composable
private fun NokiaLauncherRoot(
    registerBackHandler: ((() -> Unit)) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("nokia_launcher_prefs", Context.MODE_PRIVATE) }

    var themeIndex by remember { mutableIntStateOf(prefs.getInt("lcd_theme", 0)) }
    var hapticsEnabled by remember { mutableStateOf(prefs.getBoolean("haptics", true)) }
    var soundEnabled by remember { mutableStateOf(prefs.getBoolean("sound", false)) }

    val currentTheme = LcdTheme.entries[themeIndex.coerceIn(0, LcdTheme.entries.lastIndex)]

    var screenMode by remember { mutableStateOf(ScreenMode.HOME) }
    var dialBuffer by remember { mutableStateOf("") }
    var t9Filter by remember { mutableStateOf("") }
    var torchOn by remember { mutableStateOf(false) }

    var allApps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var deviceStatus by remember { mutableStateOf(SystemBridge.readDeviceStatus(context)) }

    var selectedAppIndex by remember { mutableIntStateOf(0) }
    var selectedGoToIndex by remember { mutableIntStateOf(0) }
    var selectedSettingIndex by remember { mutableIntStateOf(0) }

    val gridState = rememberLazyGridState()

    val filteredApps = remember(allApps, t9Filter) {
        if (t9Filter.isEmpty()) allApps
        else allApps.filter { SystemBridge.matchesT9(it.label, t9Filter) }
    }

    LaunchedEffect(filteredApps.size) {
        if (selectedAppIndex >= filteredApps.size) {
            selectedAppIndex = (filteredApps.size - 1).coerceAtLeast(0)
        }
    }

    LaunchedEffect(selectedAppIndex, screenMode) {
        if (screenMode == ScreenMode.MENU && filteredApps.isNotEmpty()) {
            gridState.animateScrollToItem(selectedAppIndex.coerceIn(0, filteredApps.lastIndex))
        }
    }

    // Load apps initially and refresh on resume
    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) { SystemBridge.loadInstalledApps(context) }
        allApps = loaded
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                deviceStatus = SystemBridge.readDeviceStatus(context)
                scope.launch {
                    val loaded = withContext(Dispatchers.IO) { SystemBridge.loadInstalledApps(context) }
                    allApps = loaded
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Listen to time & battery changes
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                deviceStatus = SystemBridge.readDeviceStatus(context)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    // Register back button behavior (never exits launcher from Home)
    LaunchedEffect(screenMode, dialBuffer, t9Filter) {
        registerBackHandler {
            when (screenMode) {
                ScreenMode.MENU -> {
                    if (t9Filter.isNotEmpty()) {
                        t9Filter = ""
                    } else {
                        screenMode = ScreenMode.HOME
                    }
                }
                ScreenMode.GO_TO, ScreenMode.SETTINGS -> {
                    screenMode = ScreenMode.HOME
                }
                ScreenMode.HOME -> {
                    if (dialBuffer.isNotEmpty()) {
                        dialBuffer = ""
                    }
                }
            }
        }
    }

    fun feedback() {
        SystemBridge.playKeyFeedback(context, hapticsEnabled, soundEnabled)
    }

    val goToItems = listOf(
        stringResource(R.string.shortcut_dialer) to {
            SystemBridge.openDialer(context, dialBuffer)
        },
        stringResource(R.string.shortcut_messages) to {
            SystemBridge.openMessages(context)
        },
        stringResource(R.string.shortcut_camera) to {
            SystemBridge.openCamera(context)
        },
        stringResource(R.string.shortcut_torch) to {
            torchOn = SystemBridge.toggleTorch(context, !torchOn)
        },
        stringResource(R.string.shortcut_settings) to {
            SystemBridge.openAndroidSettings(context)
        }
    )

    val onLabel = stringResource(R.string.on_label)
    val offLabel = stringResource(R.string.off_label)
    val settingItems = listOf(
        Triple(
            stringResource(R.string.setting_lcd_theme),
            stringResource(currentTheme.titleRes)
        ) {
            themeIndex = (themeIndex + 1) % LcdTheme.entries.size
            prefs.edit().putInt("lcd_theme", themeIndex).apply()
        },
        Triple(
            stringResource(R.string.setting_haptics),
            if (hapticsEnabled) onLabel else offLabel
        ) {
            hapticsEnabled = !hapticsEnabled
            prefs.edit().putBoolean("haptics", hapticsEnabled).apply()
        },
        Triple(
            stringResource(R.string.setting_sound),
            if (soundEnabled) onLabel else offLabel
        ) {
            soundEnabled = !soundEnabled
            prefs.edit().putBoolean("sound", soundEnabled).apply()
        },
        Triple(
            stringResource(R.string.setting_default_launcher),
            ""
        ) {
            SystemBridge.openDefaultLauncherSettings(context)
        },
        Triple(
            stringResource(R.string.setting_android_settings),
            ""
        ) {
            SystemBridge.openAndroidSettings(context)
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0D))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top chassis bezel with centered NOKIA brand mark
        Text(
            text = stringResource(R.string.brand_header),
            color = Color(0xFFF2F2F2),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 6.dp)
                .align(Alignment.CenterHorizontally)
        )

        // Upper half: Retro LCD / App Drawer
        RetroLcdScreen(
            mode = screenMode,
            theme = currentTheme,
            status = deviceStatus,
            dialBuffer = dialBuffer,
            t9Filter = t9Filter,
            filteredApps = filteredApps,
            selectedAppIndex = selectedAppIndex,
            gridState = gridState,
            goToItems = goToItems,
            selectedGoToIndex = selectedGoToIndex,
            settingItems = settingItems,
            selectedSettingIndex = selectedSettingIndex,
            torchOn = torchOn,
            onOpenGoTo = {
                feedback()
                selectedGoToIndex = 0
                screenMode = ScreenMode.GO_TO
            },
            onOpenMenu = {
                feedback()
                t9Filter = ""
                selectedAppIndex = 0
                screenMode = ScreenMode.MENU
            },
            onOpenSettings = {
                feedback()
                selectedSettingIndex = 0
                screenMode = ScreenMode.SETTINGS
            },
            onLaunchApp = { app ->
                feedback()
                SystemBridge.launchApp(context, app)
            },
            modifier = Modifier
                .weight(0.47f)
                .padding(horizontal = 10.dp)
        )

        // Lower half: Physical-style Virtual T9 Keypad
        NokiaKeypad(
            onLeftSoftkey = {
                feedback()
                when (screenMode) {
                    ScreenMode.HOME -> {
                        selectedGoToIndex = 0
                        screenMode = ScreenMode.GO_TO
                    }
                    ScreenMode.MENU -> {
                        filteredApps.getOrNull(selectedAppIndex)?.let {
                            SystemBridge.launchApp(context, it)
                        }
                    }
                    ScreenMode.GO_TO -> {
                        goToItems.getOrNull(selectedGoToIndex)?.second?.invoke()
                    }
                    ScreenMode.SETTINGS -> {
                        settingItems.getOrNull(selectedSettingIndex)?.third?.invoke()
                    }
                }
            },
            onRightSoftkey = {
                feedback()
                when (screenMode) {
                    ScreenMode.HOME -> {
                        if (dialBuffer.isNotEmpty()) {
                            dialBuffer = dialBuffer.dropLast(1)
                        } else {
                            selectedSettingIndex = 0
                            screenMode = ScreenMode.SETTINGS
                        }
                    }
                    ScreenMode.MENU -> {
                        if (t9Filter.isNotEmpty()) {
                            t9Filter = t9Filter.dropLast(1)
                        } else {
                            screenMode = ScreenMode.HOME
                        }
                    }
                    ScreenMode.GO_TO, ScreenMode.SETTINGS -> {
                        screenMode = ScreenMode.HOME
                    }
                }
            },
            onCallPress = {
                feedback()
                SystemBridge.openDialer(context, dialBuffer)
            },
            onEndPress = {
                feedback()
                dialBuffer = ""
                t9Filter = ""
                screenMode = ScreenMode.HOME
            },
            onDpadPress = { dir ->
                feedback()
                when (screenMode) {
                    ScreenMode.HOME -> {
                        when (dir) {
                            DpadDirection.CENTER -> {
                                t9Filter = ""
                                selectedAppIndex = 0
                                screenMode = ScreenMode.MENU
                            }
                            DpadDirection.UP -> SystemBridge.openCamera(context)
                            DpadDirection.DOWN -> SystemBridge.openMessages(context)
                            DpadDirection.LEFT -> {
                                selectedGoToIndex = 0
                                screenMode = ScreenMode.GO_TO
                            }
                            DpadDirection.RIGHT -> {
                                selectedSettingIndex = 0
                                screenMode = ScreenMode.SETTINGS
                            }
                        }
                    }
                    ScreenMode.MENU -> {
                        val count = filteredApps.size
                        if (count > 0) {
                            when (dir) {
                                DpadDirection.LEFT -> {
                                    selectedAppIndex = (selectedAppIndex - 1).coerceAtLeast(0)
                                }
                                DpadDirection.RIGHT -> {
                                    selectedAppIndex = (selectedAppIndex + 1).coerceAtMost(count - 1)
                                }
                                DpadDirection.UP -> {
                                    selectedAppIndex = (selectedAppIndex - 4).coerceAtLeast(0)
                                }
                                DpadDirection.DOWN -> {
                                    selectedAppIndex = (selectedAppIndex + 4).coerceAtMost(count - 1)
                                }
                                DpadDirection.CENTER -> {
                                    filteredApps.getOrNull(selectedAppIndex)?.let {
                                        SystemBridge.launchApp(context, it)
                                    }
                                }
                            }
                        }
                    }
                    ScreenMode.GO_TO -> {
                        when (dir) {
                            DpadDirection.UP -> {
                                selectedGoToIndex = (selectedGoToIndex - 1).coerceAtLeast(0)
                            }
                            DpadDirection.DOWN -> {
                                selectedGoToIndex = (selectedGoToIndex + 1).coerceAtMost(goToItems.lastIndex)
                            }
                            DpadDirection.CENTER -> {
                                goToItems.getOrNull(selectedGoToIndex)?.second?.invoke()
                            }
                            else -> Unit
                        }
                    }
                    ScreenMode.SETTINGS -> {
                        when (dir) {
                            DpadDirection.UP -> {
                                selectedSettingIndex = (selectedSettingIndex - 1).coerceAtLeast(0)
                            }
                            DpadDirection.DOWN -> {
                                selectedSettingIndex = (selectedSettingIndex + 1).coerceAtMost(settingItems.lastIndex)
                            }
                            DpadDirection.CENTER, DpadDirection.LEFT, DpadDirection.RIGHT -> {
                                settingItems.getOrNull(selectedSettingIndex)?.third?.invoke()
                            }
                        }
                    }
                }
            },
            onDigitPress = { digit ->
                feedback()
                when (screenMode) {
                    ScreenMode.HOME -> {
                        if (dialBuffer.length < 18) {
                            dialBuffer += digit
                        }
                    }
                    ScreenMode.MENU -> {
                        if (digit in "0".."9") {
                            t9Filter += digit
                            selectedAppIndex = 0
                        }
                    }
                    else -> Unit
                }
            },
            onDigitLongPress = { digit ->
                feedback()
                if (digit == "0") {
                    torchOn = SystemBridge.toggleTorch(context, !torchOn)
                }
            },
            modifier = Modifier.weight(0.53f)
        )
    }
}
