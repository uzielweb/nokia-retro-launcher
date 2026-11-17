package com.uziel.nokiaretrolauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import java.util.Locale

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
    val baseContext = LocalContext.current
    val baseConfig = LocalConfiguration.current
    val prefs = remember { baseContext.getSharedPreferences("nokia_launcher_prefs", Context.MODE_PRIVATE) }

    var langIndex by remember { mutableIntStateOf(prefs.getInt("app_lang", 0)) }
    val currentLanguage = AppLanguage.entries[langIndex.coerceIn(0, AppLanguage.entries.lastIndex)]
    val targetLocale = remember(currentLanguage) {
        if (currentLanguage == AppLanguage.AUTO) Locale.getDefault()
        else Locale.forLanguageTag(currentLanguage.code)
    }

    val localizedConfig = remember(baseConfig, targetLocale) {
        Configuration(baseConfig).apply { setLocale(targetLocale) }
    }
    val localizedResources = remember(baseContext, localizedConfig) {
        baseContext.createConfigurationContext(localizedConfig).resources
    }

    CompositionLocalProvider(LocalAppResources provides localizedResources) {
        NokiaLauncherContent(
            baseContext = baseContext,
            prefs = prefs,
            currentLanguage = currentLanguage,
            targetLocale = targetLocale,
            onCycleLanguage = {
                langIndex = (langIndex + 1) % AppLanguage.entries.size
                prefs.edit().putInt("app_lang", langIndex).apply()
            },
            registerBackHandler = registerBackHandler
        )
    }
}

@Composable
private fun NokiaLauncherContent(
    baseContext: Context,
    prefs: android.content.SharedPreferences,
    currentLanguage: AppLanguage,
    targetLocale: Locale,
    onCycleLanguage: () -> Unit,
    registerBackHandler: ((() -> Unit)) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val currentLocaleState by rememberUpdatedState(targetLocale)

    var themeIndex by remember { mutableIntStateOf(prefs.getInt("lcd_theme", 0)) }
    var backlightIndex by remember { mutableIntStateOf(prefs.getInt("keypad_backlight", 0)) }
    var wallpaperIndex by remember { mutableIntStateOf(prefs.getInt("wallpaper_mode", 0)) }
    var snakeSpeedIndex by remember { mutableIntStateOf(prefs.getInt("snake_speed", SnakeSpeed.SLOW.ordinal)) }
    var hapticsEnabled by remember { mutableStateOf(prefs.getBoolean("haptics", true)) }
    var soundEnabled by remember { mutableStateOf(prefs.getBoolean("retro_sound", true)) }

    val currentTheme = LcdTheme.entries[themeIndex.coerceIn(0, LcdTheme.entries.lastIndex)]
    val currentBacklight = KeypadBacklight.entries[backlightIndex.coerceIn(0, KeypadBacklight.entries.lastIndex)]
    val currentWallpaperMode = WallpaperMode.entries[wallpaperIndex.coerceIn(0, WallpaperMode.entries.lastIndex)]
    val currentSnakeSpeed = SnakeSpeed.entries[snakeSpeedIndex.coerceIn(0, SnakeSpeed.entries.lastIndex)]
    val backlightColor = currentBacklight.resolveColor(currentTheme)

    var customWallpaper by remember { mutableStateOf<ImageBitmap?>(null) }
    var availableUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var shortcutPackageList by remember { mutableStateOf(SystemBridge.loadShortcutPackages(prefs)) }
    val shortcutPackagesSet = remember(shortcutPackageList) { shortcutPackageList.toSet() }

    val snakeController = remember {
        SnakeController(initialHighScore = prefs.getInt("snake_high_score", 0)) { newHi ->
            prefs.edit().putInt("snake_high_score", newHi).apply()
        }
    }

    var screenMode by remember { mutableStateOf(ScreenMode.HOME) }
    var dialBuffer by remember { mutableStateOf("") }
    var t9Filter by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var torchOn by remember { mutableStateOf(false) }

    var allApps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var deviceStatus by remember { mutableStateOf(SystemBridge.readDeviceStatus(baseContext, targetLocale)) }

    var selectedAppIndex by remember { mutableIntStateOf(0) }
    var selectedGoToIndex by remember { mutableIntStateOf(0) }
    var selectedSettingIndex by remember { mutableIntStateOf(0) }
    var selectedShortcutPickerIndex by remember { mutableIntStateOf(0) }

    val gridState = rememberLazyGridState()

    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val loaded = withContext(Dispatchers.IO) {
                    SystemBridge.saveCustomWallpaper(baseContext, uri)
                }
                if (loaded != null) {
                    customWallpaper = loaded
                    wallpaperIndex = WallpaperMode.CUSTOM_IMAGE.ordinal
                    prefs.edit().putInt("wallpaper_mode", wallpaperIndex).apply()
                }
            }
        }
    }

    val filteredApps = remember(allApps, t9Filter, searchQuery) {
        if (t9Filter.isEmpty() && searchQuery.isBlank()) allApps
        else allApps.filter { SystemBridge.matchesAppQuery(it.label, t9Filter, searchQuery) }
    }

    val shortcutApps = remember(allApps, shortcutPackageList) {
        val byPkg = allApps.associateBy { it.packageName }
        shortcutPackageList.mapNotNull { byPkg[it] }
    }

    LaunchedEffect(targetLocale) {
        deviceStatus = SystemBridge.readDeviceStatus(baseContext, targetLocale)
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

    // Initialize Retro Sound Engine + load apps and custom wallpaper
    LaunchedEffect(Unit) {
        RetroSoundEngine.init(baseContext)
        if (soundEnabled) {
            RetroSoundEngine.play(baseContext, RetroSound.STARTUP)
        }
        val loadedWp = withContext(Dispatchers.IO) { SystemBridge.loadCustomWallpaper(baseContext) }
        customWallpaper = loadedWp
        val loaded = withContext(Dispatchers.IO) { SystemBridge.loadInstalledApps(baseContext) }
        allApps = loaded
        availableUpdate = withContext(Dispatchers.IO) { SystemBridge.checkForUpdate() }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                deviceStatus = SystemBridge.readDeviceStatus(baseContext, currentLocaleState)
                scope.launch {
                    val loaded = withContext(Dispatchers.IO) { SystemBridge.loadInstalledApps(baseContext) }
                    allApps = loaded
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Listen to time & battery changes
    DisposableEffect(baseContext) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                deviceStatus = SystemBridge.readDeviceStatus(baseContext, currentLocaleState)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        runCatching { baseContext.registerReceiver(receiver, filter) }
        onDispose { runCatching { baseContext.unregisterReceiver(receiver) } }
    }

    // Register back button behavior (never exits launcher from Home)
    LaunchedEffect(screenMode, dialBuffer, t9Filter, searchQuery, searchActive) {
        registerBackHandler {
            when (screenMode) {
                ScreenMode.MENU -> {
                    if (searchActive || searchQuery.isNotEmpty() || t9Filter.isNotEmpty()) {
                        searchActive = false
                        searchQuery = ""
                        t9Filter = ""
                        selectedAppIndex = 0
                    } else {
                        screenMode = ScreenMode.HOME
                    }
                }
                ScreenMode.GO_TO, ScreenMode.SETTINGS, ScreenMode.SNAKE, ScreenMode.PICK_SHORTCUT -> {
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

    fun feedback(sound: RetroSound = RetroSound.SELECT) {
        SystemBridge.playKeyFeedback(baseContext, hapticsEnabled, soundEnabled, sound)
    }

    fun toggleShortcut(app: AppEntry) {
        shortcutPackageList = SystemBridge.toggleShortcutPackage(prefs, app.packageName)
        feedback(RetroSound.SELECT)
    }

    fun cycleSnakeSpeed() {
        snakeSpeedIndex = (snakeSpeedIndex + 1) % SnakeSpeed.entries.size
        prefs.edit().putInt("snake_speed", snakeSpeedIndex).apply()
    }

    val goToItems: List<Pair<String, () -> Unit>> = buildList {
        shortcutApps.forEach { app ->
            add("★ ${app.label}" to {
                feedback(RetroSound.SELECT)
                SystemBridge.launchApp(baseContext, app)
            })
        }
        add(tr(R.string.shortcut_snake) to {
            feedback(RetroSound.SELECT)
            snakeController.reset()
            screenMode = ScreenMode.SNAKE
        })
        add(tr(R.string.shortcut_manage_apps) to {
            feedback(RetroSound.SELECT)
            selectedShortcutPickerIndex = 0
            screenMode = ScreenMode.PICK_SHORTCUT
        })
        add(tr(R.string.shortcut_dialer) to {
            SystemBridge.openDialer(baseContext, dialBuffer)
        })
        add(tr(R.string.shortcut_messages) to {
            SystemBridge.openMessages(baseContext)
        })
        add(tr(R.string.shortcut_camera) to {
            SystemBridge.openCamera(baseContext)
        })
        add(tr(R.string.shortcut_torch) to {
            feedback(RetroSound.TORCH)
            torchOn = SystemBridge.toggleTorch(baseContext, !torchOn)
        })
        add(tr(R.string.shortcut_settings) to {
            SystemBridge.openAndroidSettings(baseContext)
        })
    }

    val onLabel = tr(R.string.on_label)
    val offLabel = tr(R.string.off_label)
    val settingItems: List<Triple<String, String, () -> Unit>> = listOf(
        Triple(
            tr(R.string.setting_lcd_theme),
            tr(currentTheme.titleRes)
        ) {
            themeIndex = (themeIndex + 1) % LcdTheme.entries.size
            prefs.edit().putInt("lcd_theme", themeIndex).apply()
        },
        Triple(
            tr(R.string.setting_keypad_backlight),
            tr(currentBacklight.titleRes)
        ) {
            backlightIndex = (backlightIndex + 1) % KeypadBacklight.entries.size
            prefs.edit().putInt("keypad_backlight", backlightIndex).apply()
        },
        Triple(
            tr(R.string.setting_wallpaper),
            tr(currentWallpaperMode.titleRes)
        ) {
            val nextIdx = (wallpaperIndex + 1) % WallpaperMode.entries.size
            val nextMode = WallpaperMode.entries[nextIdx]
            if (nextMode == WallpaperMode.CUSTOM_IMAGE) {
                if (customWallpaper != null) {
                    wallpaperIndex = nextIdx
                    prefs.edit().putInt("wallpaper_mode", wallpaperIndex).apply()
                }
                runCatching { wallpaperPickerLauncher.launch("image/*") }
            } else {
                wallpaperIndex = nextIdx
                prefs.edit().putInt("wallpaper_mode", wallpaperIndex).apply()
            }
            Unit
        },
        Triple(
            tr(R.string.setting_snake_speed),
            tr(currentSnakeSpeed.titleRes)
        ) {
            cycleSnakeSpeed()
        },
        Triple(
            tr(R.string.setting_language),
            tr(currentLanguage.titleRes)
        ) {
            onCycleLanguage()
        },
        Triple(
            tr(R.string.setting_haptics),
            if (hapticsEnabled) onLabel else offLabel
        ) {
            hapticsEnabled = !hapticsEnabled
            prefs.edit().putBoolean("haptics", hapticsEnabled).apply()
        },
        Triple(
            tr(R.string.setting_sound),
            if (soundEnabled) onLabel else offLabel
        ) {
            soundEnabled = !soundEnabled
            prefs.edit().putBoolean("retro_sound", soundEnabled).apply()
            if (soundEnabled) {
                RetroSoundEngine.play(baseContext, RetroSound.STARTUP)
            }
        },
        Triple(
            tr(R.string.setting_nokia_tune),
            tr(R.string.nokia_tune_play_badge)
        ) {
            RetroSoundEngine.play(baseContext, RetroSound.NOKIA_TUNE)
        },
        Triple(
            tr(R.string.menu_title_shortcuts),
            shortcutApps.size.toString()
        ) {
            selectedShortcutPickerIndex = 0
            screenMode = ScreenMode.PICK_SHORTCUT
        },
        Triple(
            tr(R.string.setting_check_update),
            when {
                checkingUpdate -> tr(R.string.update_checking)
                availableUpdate != null -> tr(R.string.update_available_badge, availableUpdate!!.version)
                else -> tr(R.string.update_up_to_date, SystemBridge.CURRENT_VERSION_NAME)
            }
        ) {
            val update = availableUpdate
            if (update != null) {
                SystemBridge.openUrl(baseContext, update.apkUrl)
            } else if (!checkingUpdate) {
                checkingUpdate = true
                scope.launch {
                    availableUpdate = withContext(Dispatchers.IO) { SystemBridge.checkForUpdate() }
                    checkingUpdate = false
                }
            }
        },
        Triple(
            tr(R.string.setting_default_launcher),
            ""
        ) {
            SystemBridge.openDefaultLauncherSettings(baseContext)
        },
        Triple(
            tr(R.string.setting_android_settings),
            ""
        ) {
            SystemBridge.openAndroidSettings(baseContext)
        }
    )

    val shortcutPickerItems = remember(allApps, shortcutPackagesSet) {
        allApps.map { app ->
            app.label to if (shortcutPackagesSet.contains(app.packageName)) "★" else ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0D))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top chassis bezel with centered NOKIA brand mark
        Text(
            text = tr(R.string.brand_header),
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

        // Upper half: Retro LCD / App Drawer / Snake
        RetroLcdScreen(
            mode = screenMode,
            theme = currentTheme,
            wallpaperMode = currentWallpaperMode,
            customWallpaper = customWallpaper,
            status = deviceStatus,
            dialBuffer = dialBuffer,
            t9Filter = t9Filter,
            searchQuery = searchQuery,
            searchActive = searchActive,
            filteredApps = filteredApps,
            shortcutApps = shortcutApps,
            shortcutPackages = shortcutPackagesSet,
            selectedAppIndex = selectedAppIndex,
            gridState = gridState,
            goToItems = goToItems,
            selectedGoToIndex = selectedGoToIndex,
            settingItems = settingItems,
            selectedSettingIndex = selectedSettingIndex,
            shortcutPickerItems = shortcutPickerItems,
            selectedShortcutPickerIndex = selectedShortcutPickerIndex,
            snakeController = snakeController,
            snakeSpeed = currentSnakeSpeed,
            availableUpdate = availableUpdate,
            torchOn = torchOn,
            onOpenGoTo = {
                feedback(RetroSound.SELECT)
                selectedGoToIndex = 0
                screenMode = ScreenMode.GO_TO
            },
            onOpenMenu = {
                feedback(RetroSound.SELECT)
                t9Filter = ""
                searchQuery = ""
                searchActive = false
                selectedAppIndex = 0
                screenMode = ScreenMode.MENU
            },
            onOpenSearch = {
                feedback(RetroSound.SELECT)
                t9Filter = ""
                searchQuery = ""
                searchActive = true
                selectedAppIndex = 0
                screenMode = ScreenMode.MENU
            },
            onOpenSettings = {
                feedback(RetroSound.SELECT)
                selectedSettingIndex = 0
                screenMode = ScreenMode.SETTINGS
            },
            onSearchQueryChange = { q ->
                searchQuery = q
                selectedAppIndex = 0
            },
            onToggleSearchActive = { active ->
                searchActive = active
            },
            onClearSearch = {
                feedback(RetroSound.BACK)
                searchQuery = ""
                t9Filter = ""
                searchActive = false
                selectedAppIndex = 0
            },
            onLaunchApp = { app ->
                feedback(RetroSound.SELECT)
                SystemBridge.launchApp(baseContext, app)
            },
            onToggleShortcut = { app ->
                toggleShortcut(app)
            },
            onToggleShortcutByIndex = { idx ->
                allApps.getOrNull(idx)?.let { toggleShortcut(it) }
            },
            onCycleSnakeSpeed = {
                feedback(RetroSound.SELECT)
                cycleSnakeSpeed()
            },
            onSnakeEat = {
                if (soundEnabled) RetroSoundEngine.play(baseContext, RetroSound.SNAKE_EAT)
            },
            onSnakeDie = {
                if (soundEnabled) RetroSoundEngine.play(baseContext, RetroSound.SNAKE_DIE)
            },
            onOpenUpdate = {
                availableUpdate?.let {
                    feedback(RetroSound.SELECT)
                    SystemBridge.openUrl(baseContext, it.apkUrl)
                }
            },
            onExitSubScreen = {
                feedback(RetroSound.BACK)
                screenMode = ScreenMode.HOME
            },
            modifier = Modifier
                .weight(0.47f)
                .padding(horizontal = 10.dp)
        )

        // Lower half: Physical-style Retro-Illuminated T9 Keypad
        NokiaKeypad(
            backlightColor = backlightColor,
            onLeftSoftkey = {
                feedback(RetroSound.SELECT)
                when (screenMode) {
                    ScreenMode.HOME -> {
                        selectedGoToIndex = 0
                        screenMode = ScreenMode.GO_TO
                    }
                    ScreenMode.MENU -> {
                        filteredApps.getOrNull(selectedAppIndex)?.let {
                            SystemBridge.launchApp(baseContext, it)
                        }
                    }
                    ScreenMode.GO_TO -> {
                        goToItems.getOrNull(selectedGoToIndex)?.second?.invoke()
                    }
                    ScreenMode.SETTINGS -> {
                        settingItems.getOrNull(selectedSettingIndex)?.third?.invoke()
                    }
                    ScreenMode.PICK_SHORTCUT -> {
                        allApps.getOrNull(selectedShortcutPickerIndex)?.let { toggleShortcut(it) }
                    }
                    ScreenMode.SNAKE -> {
                        snakeController.togglePauseOrRestart()
                    }
                }
            },
            onRightSoftkey = {
                feedback(RetroSound.BACK)
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
                        if (searchQuery.isNotEmpty() || t9Filter.isNotEmpty() || searchActive) {
                            searchQuery = ""
                            t9Filter = ""
                            searchActive = false
                            selectedAppIndex = 0
                        } else {
                            screenMode = ScreenMode.HOME
                        }
                    }
                    ScreenMode.GO_TO, ScreenMode.SETTINGS, ScreenMode.PICK_SHORTCUT, ScreenMode.SNAKE -> {
                        screenMode = ScreenMode.HOME
                    }
                }
            },
            onCallPress = {
                feedback(RetroSound.CALL)
                SystemBridge.openDialer(baseContext, dialBuffer)
            },
            onEndPress = {
                feedback(RetroSound.END)
                dialBuffer = ""
                t9Filter = ""
                searchQuery = ""
                searchActive = false
                screenMode = ScreenMode.HOME
            },
            onDpadPress = { dir ->
                val snd = if (dir == DpadDirection.CENTER) RetroSound.SELECT else RetroSound.NAV
                feedback(snd)
                when (screenMode) {
                    ScreenMode.HOME -> {
                        when (dir) {
                            DpadDirection.CENTER -> {
                                t9Filter = ""
                                searchQuery = ""
                                searchActive = false
                                selectedAppIndex = 0
                                screenMode = ScreenMode.MENU
                            }
                            DpadDirection.UP -> SystemBridge.openCamera(baseContext)
                            DpadDirection.DOWN -> SystemBridge.openMessages(baseContext)
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
                                        SystemBridge.launchApp(baseContext, it)
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
                    ScreenMode.PICK_SHORTCUT -> {
                        val count = allApps.size
                        if (count > 0) {
                            when (dir) {
                                DpadDirection.UP -> {
                                    selectedShortcutPickerIndex = (selectedShortcutPickerIndex - 1).coerceAtLeast(0)
                                }
                                DpadDirection.DOWN -> {
                                    selectedShortcutPickerIndex = (selectedShortcutPickerIndex + 1).coerceAtMost(count - 1)
                                }
                                DpadDirection.CENTER, DpadDirection.LEFT, DpadDirection.RIGHT -> {
                                    allApps.getOrNull(selectedShortcutPickerIndex)?.let { toggleShortcut(it) }
                                }
                            }
                        }
                    }
                    ScreenMode.SNAKE -> {
                        snakeController.steer(dir)
                    }
                }
            },
            onDigitPress = { digit ->
                feedback(RetroSound.forDigit(digit))
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
                        } else if (digit == "#") {
                            filteredApps.getOrNull(selectedAppIndex)?.let { toggleShortcut(it) }
                        }
                    }
                    ScreenMode.SNAKE -> {
                        when (digit) {
                            "2" -> snakeController.steer(DpadDirection.UP)
                            "8" -> snakeController.steer(DpadDirection.DOWN)
                            "4" -> snakeController.steer(DpadDirection.LEFT)
                            "6" -> snakeController.steer(DpadDirection.RIGHT)
                            "5" -> snakeController.togglePauseOrRestart()
                            "*", "#" -> cycleSnakeSpeed()
                        }
                    }
                    else -> Unit
                }
            },
            onDigitLongPress = { digit ->
                if (digit == "0") {
                    feedback(RetroSound.TORCH)
                    torchOn = SystemBridge.toggleTorch(baseContext, !torchOn)
                } else if (screenMode == ScreenMode.HOME && digit in "1".."9") {
                    val idx = digit.toInt() - 1
                    val shortcut = shortcutApps.getOrNull(idx)
                    if (shortcut != null) {
                        feedback(RetroSound.SELECT)
                        SystemBridge.launchApp(baseContext, shortcut)
                    } else {
                        feedback(RetroSound.forDigit(digit))
                    }
                } else {
                    feedback(RetroSound.forDigit(digit))
                }
            },
            modifier = Modifier.weight(0.53f)
        )
    }
}
