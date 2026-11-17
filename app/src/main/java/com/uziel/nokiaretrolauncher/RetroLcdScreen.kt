package com.uziel.nokiaretrolauncher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed as listItemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RetroLcdScreen(
    mode: ScreenMode,
    theme: LcdTheme,
    wallpaperMode: WallpaperMode,
    customWallpaper: ImageBitmap?,
    status: DeviceStatus,
    dialBuffer: String,
    t9Filter: String,
    searchQuery: String,
    searchActive: Boolean,
    filteredApps: List<AppEntry>,
    shortcutApps: List<AppEntry>,
    shortcutPackages: Set<String>,
    selectedAppIndex: Int,
    gridState: LazyGridState,
    goToItems: List<Pair<String, () -> Unit>>,
    selectedGoToIndex: Int,
    settingItems: List<Triple<String, String, () -> Unit>>,
    selectedSettingIndex: Int,
    shortcutPickerItems: List<Pair<String, String>>,
    selectedShortcutPickerIndex: Int,
    snakeController: SnakeController,
    torchOn: Boolean,
    onOpenGoTo: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearchActive: (Boolean) -> Unit,
    onClearSearch: () -> Unit,
    onLaunchApp: (AppEntry) -> Unit,
    onToggleShortcut: (AppEntry) -> Unit,
    onToggleShortcutByIndex: (Int) -> Unit,
    onSnakeEat: () -> Unit,
    onSnakeDie: () -> Unit,
    onExitSubScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
    ) {
        when (mode) {
            ScreenMode.HOME -> {
                HomeLcdContent(
                    theme = theme,
                    wallpaperMode = wallpaperMode,
                    customWallpaper = customWallpaper,
                    status = status,
                    dialBuffer = dialBuffer,
                    shortcutApps = shortcutApps,
                    torchOn = torchOn,
                    onOpenGoTo = onOpenGoTo,
                    onOpenMenu = onOpenMenu,
                    onOpenSearch = onOpenSearch,
                    onOpenSettings = onOpenSettings,
                    onLaunchApp = onLaunchApp
                )
            }
            ScreenMode.MENU -> {
                AppDrawerContent(
                    theme = theme,
                    wallpaperMode = wallpaperMode,
                    customWallpaper = customWallpaper,
                    apps = filteredApps,
                    shortcutPackages = shortcutPackages,
                    selectedIndex = selectedAppIndex,
                    t9Filter = t9Filter,
                    searchQuery = searchQuery,
                    searchActive = searchActive,
                    gridState = gridState,
                    onSearchQueryChange = onSearchQueryChange,
                    onToggleSearchActive = onToggleSearchActive,
                    onClearSearch = onClearSearch,
                    onLaunchApp = onLaunchApp,
                    onToggleShortcut = onToggleShortcut
                )
            }
            ScreenMode.GO_TO -> {
                RetroListMenuContent(
                    title = stringResource(R.string.menu_title_goto),
                    theme = theme,
                    items = goToItems.map { it.first to "" },
                    selectedIndex = selectedGoToIndex,
                    leftLabel = stringResource(R.string.softkey_select),
                    rightLabel = stringResource(R.string.softkey_back),
                    onItemClick = { idx -> goToItems.getOrNull(idx)?.second?.invoke() },
                    onLeftSoftkey = { goToItems.getOrNull(selectedGoToIndex)?.second?.invoke() },
                    onRightSoftkey = onExitSubScreen
                )
            }
            ScreenMode.SETTINGS -> {
                RetroListMenuContent(
                    title = stringResource(R.string.menu_title_setting),
                    theme = theme,
                    items = settingItems.map { it.first to it.second },
                    selectedIndex = selectedSettingIndex,
                    leftLabel = stringResource(R.string.softkey_select),
                    rightLabel = stringResource(R.string.softkey_back),
                    onItemClick = { idx -> settingItems.getOrNull(idx)?.third?.invoke() },
                    onLeftSoftkey = { settingItems.getOrNull(selectedSettingIndex)?.third?.invoke() },
                    onRightSoftkey = onExitSubScreen
                )
            }
            ScreenMode.PICK_SHORTCUT -> {
                RetroListMenuContent(
                    title = stringResource(R.string.menu_title_shortcuts),
                    subtitle = stringResource(R.string.shortcut_hint_speed),
                    theme = theme,
                    items = shortcutPickerItems,
                    selectedIndex = selectedShortcutPickerIndex,
                    leftLabel = stringResource(R.string.softkey_select),
                    rightLabel = stringResource(R.string.softkey_back),
                    onItemClick = onToggleShortcutByIndex,
                    onLeftSoftkey = { onToggleShortcutByIndex(selectedShortcutPickerIndex) },
                    onRightSoftkey = onExitSubScreen
                )
            }
            ScreenMode.SNAKE -> {
                SnakeLcdScreen(
                    controller = snakeController,
                    theme = theme,
                    onEat = onSnakeEat,
                    onDie = onSnakeDie,
                    onExit = onExitSubScreen
                )
            }
        }
    }
}

@Composable
private fun LcdWallpaperLayer(
    theme: LcdTheme,
    wallpaperMode: WallpaperMode,
    customWallpaper: ImageBitmap?
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.background)
        )
        when (wallpaperMode) {
            WallpaperMode.NONE -> Unit
            WallpaperMode.PIXEL_GRID -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 10.dp.toPx()
                    var x = step
                    while (x < size.width) {
                        drawLine(
                            color = theme.ink.copy(alpha = 0.08f),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
                        x += step
                    }
                    var y = step
                    while (y < size.height) {
                        drawLine(
                            color = theme.ink.copy(alpha = 0.08f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += step
                    }
                    val mountain = Path().apply {
                        moveTo(0f, size.height * 0.85f)
                        lineTo(size.width * 0.24f, size.height * 0.54f)
                        lineTo(size.width * 0.48f, size.height * 0.72f)
                        lineTo(size.width * 0.72f, size.height * 0.45f)
                        lineTo(size.width, size.height * 0.78f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(mountain, color = theme.ink.copy(alpha = 0.09f))
                }
            }
            WallpaperMode.RETRO_HORIZON -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width * 0.5f
                    val horizonY = size.height * 0.58f
                    val sunR = size.minDimension * 0.24f
                    drawCircle(
                        color = theme.ink.copy(alpha = 0.14f),
                        radius = sunR,
                        center = Offset(cx, horizonY - sunR * 0.2f)
                    )
                    drawLine(
                        color = theme.ink.copy(alpha = 0.22f),
                        start = Offset(0f, horizonY),
                        end = Offset(size.width, horizonY),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    for (i in -4..4) {
                        drawLine(
                            color = theme.ink.copy(alpha = 0.12f),
                            start = Offset(cx + i * 14.dp.toPx(), horizonY),
                            end = Offset(cx + i * 48.dp.toPx(), size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    for (j in 1..4) {
                        val frac = (j * j) / 16f
                        val py = horizonY + (size.height - horizonY) * frac
                        drawLine(
                            color = theme.ink.copy(alpha = 0.11f),
                            start = Offset(0f, py),
                            end = Offset(size.width, py),
                            strokeWidth = 1f
                        )
                    }
                }
            }
            WallpaperMode.CUSTOM_IMAGE -> {
                if (customWallpaper != null) {
                    Image(
                        bitmap = customWallpaper,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(theme.background.copy(alpha = 0.56f))
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeLcdContent(
    theme: LcdTheme,
    wallpaperMode: WallpaperMode,
    customWallpaper: ImageBitmap?,
    status: DeviceStatus,
    dialBuffer: String,
    shortcutApps: List<AppEntry>,
    torchOn: Boolean,
    onOpenGoTo: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onLaunchApp: (AppEntry) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LcdWallpaperLayer(
            theme = theme,
            wallpaperMode = wallpaperMode,
            customWallpaper = customWallpaper
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top status bar: antenna + signal on left, search + torch + wifi + arrows + battery on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SignalAntennaIcon(
                    bars = status.signalBars,
                    tint = theme.ink
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(theme.ink.copy(alpha = 0.12f))
                            .clickable(onClick = onOpenSearch)
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.cd_search),
                            tint = theme.ink,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (torchOn) {
                        Icon(
                            imageVector = Icons.Filled.FlashlightOn,
                            contentDescription = stringResource(R.string.cd_torch),
                            tint = theme.ink,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    if (status.isWifiConnected) {
                        WifiRetroIcon(tint = theme.ink)
                    }
                    if (status.hasMobileData) {
                        DataArrowsIcon(tint = theme.ink)
                    }
                    BatteryRetroBadge(
                        percent = status.batteryPercent,
                        ink = theme.ink,
                        bg = theme.background
                    )
                }
            }

            // Center area: Date or Dial Buffer + Quick Shortcuts Strip + Divider with angular clock tab
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (dialBuffer.isNotEmpty()) {
                    Text(
                        text = dialBuffer,
                        color = theme.ink,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                } else {
                    Text(
                        text = status.dateText,
                        color = theme.ink,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = if (shortcutApps.isNotEmpty()) 8.dp else 16.dp)
                    )

                    if (shortcutApps.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            items(shortcutApps, key = { it.componentName.flattenToString() }) { app ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(theme.ink.copy(alpha = 0.14f))
                                        .border(1.dp, theme.ink.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                        .clickable { onLaunchApp(app) }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    if (app.icon != null) {
                                        Image(
                                            bitmap = app.icon,
                                            contentDescription = app.label,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = app.label,
                                        color = theme.ink,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Divider line + angular dark tab with time on the right
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val tabWidth = 96.dp.toPx()
                        val tabHeight = 26.dp.toPx()
                        val slant = 20.dp.toPx()

                        // Horizontal line across entire screen
                        drawLine(
                            color = theme.ink,
                            start = Offset(0f, 1.dp.toPx()),
                            end = Offset(w, 1.dp.toPx()),
                            strokeWidth = 1.6.dp.toPx()
                        )

                        // Angular tab on right side
                        val path = Path().apply {
                            moveTo(w - tabWidth - slant, 1.dp.toPx())
                            lineTo(w, 1.dp.toPx())
                            lineTo(w, tabHeight)
                            lineTo(w - tabWidth, tabHeight)
                            close()
                        }
                        drawPath(path = path, color = theme.tabBackground)
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(96.dp)
                            .height(26.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = status.timeText,
                            color = theme.tabText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Bottom softkeys row: Go to | • Menu | Setting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                SoftkeyText(
                    label = stringResource(R.string.softkey_goto),
                    color = theme.ink,
                    onClick = onOpenGoTo
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenMenu
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f))
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = stringResource(R.string.softkey_menu),
                        color = theme.ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }

                SoftkeyText(
                    label = stringResource(R.string.softkey_setting),
                    color = theme.ink,
                    onClick = onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun SoftkeyText(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Text(
        text = label,
        color = color,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppDrawerContent(
    theme: LcdTheme,
    wallpaperMode: WallpaperMode,
    customWallpaper: ImageBitmap?,
    apps: List<AppEntry>,
    shortcutPackages: Set<String>,
    selectedIndex: Int,
    t9Filter: String,
    searchQuery: String,
    searchActive: Boolean,
    gridState: LazyGridState,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearchActive: (Boolean) -> Unit,
    onClearSearch: () -> Unit,
    onLaunchApp: (AppEntry) -> Unit,
    onToggleShortcut: (AppEntry) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(searchActive) {
        if (searchActive) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (wallpaperMode != WallpaperMode.NONE) {
            LcdWallpaperLayer(
                theme = theme,
                wallpaperMode = wallpaperMode,
                customWallpaper = customWallpaper
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF17181E).copy(alpha = if (wallpaperMode == WallpaperMode.NONE) 1f else 0.78f),
                            Color(0xFF101116).copy(alpha = if (wallpaperMode == WallpaperMode.NONE) 1f else 0.86f)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Interactive Search & T9 Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF22242D).copy(alpha = 0.92f))
                        .clickable { onToggleSearchActive(true) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.cd_search),
                        tint = Color(0xFFD6D27A),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchActive) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_placeholder),
                                    color = Color.White.copy(alpha = 0.45f),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                singleLine = true,
                                cursorBrush = SolidColor(Color(0xFFD6D27A)),
                                textStyle = TextStyle(
                                    color = Color(0xFFD6D27A),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                        } else if (t9Filter.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.t9_filter_prefix, t9Filter),
                                color = Color(0xFFD6D27A),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.search_placeholder),
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    if (searchActive || searchQuery.isNotEmpty() || t9Filter.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_clear_search),
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable(onClick = onClearSearch)
                        )
                    }
                }

                if (apps.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_apps_found),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        state = gridState,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = apps,
                            key = { _, app -> app.componentName.flattenToString() }
                        ) { index, app ->
                            val isSelected = index == selectedIndex
                            val isPinned = shortcutPackages.contains(app.packageName)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .then(
                                        if (isSelected) {
                                            Modifier
                                                .background(Color.White.copy(alpha = 0.14f))
                                                .border(
                                                    1.dp,
                                                    Color(0xFFD6D27A).copy(alpha = 0.8f),
                                                    RoundedCornerShape(10.dp)
                                                )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .combinedClickable(
                                        onClick = { onLaunchApp(app) },
                                        onLongClick = { onToggleShortcut(app) }
                                    )
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (app.icon != null) {
                                        Image(
                                            bitmap = app.icon,
                                            contentDescription = app.label,
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF2D2F3A)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = app.label.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = app.label,
                                        color = Color.White.copy(alpha = 0.92f),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                if (isPinned) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(end = 4.dp)
                                            .size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RetroListMenuContent(
    title: String,
    subtitle: String = "",
    theme: LcdTheme,
    items: List<Pair<String, String>>,
    selectedIndex: Int,
    leftLabel: String,
    rightLabel: String,
    onItemClick: (Int) -> Unit,
    onLeftSoftkey: () -> Unit,
    onRightSoftkey: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex, items.size) {
        if (items.isNotEmpty() && selectedIndex in items.indices) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = theme.ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    color = theme.ink.copy(alpha = 0.78f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(theme.ink)
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listItemsIndexed(items) { idx, (label, value) ->
                    val selected = idx == selectedIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (selected) theme.ink else Color.Transparent)
                            .clickable { onItemClick(idx) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            color = if (selected) theme.background else theme.ink,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (value.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = value,
                                color = if (selected) theme.tabText else theme.ink.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SoftkeyText(label = leftLabel, color = theme.ink, onClick = onLeftSoftkey)
            SoftkeyText(label = rightLabel, color = theme.ink, onClick = onRightSoftkey)
        }
    }
}

@Composable
private fun SignalAntennaIcon(bars: Int, tint: Color) {
    Canvas(modifier = Modifier.size(width = 28.dp, height = 16.dp)) {
        val stroke = 1.6.dp.toPx()
        // Antenna Y shape
        val ax = 4.dp.toPx()
        drawLine(tint, Offset(ax, 2.dp.toPx()), Offset(ax, size.height - 1.dp.toPx()), stroke)
        drawLine(tint, Offset(1.dp.toPx(), 2.dp.toPx()), Offset(ax, 6.dp.toPx()), stroke)
        drawLine(tint, Offset(7.dp.toPx(), 2.dp.toPx()), Offset(ax, 6.dp.toPx()), stroke)

        // 4 rising bars
        for (i in 0 until 4) {
            val bx = 11.dp.toPx() + i * 4.dp.toPx()
            val bh = (4 + i * 3).dp.toPx()
            val active = i < bars
            drawRect(
                color = if (active) tint else tint.copy(alpha = 0.25f),
                topLeft = Offset(bx, size.height - bh - 1.dp.toPx()),
                size = Size(2.5.dp.toPx(), bh)
            )
        }
    }
}

@Composable
private fun WifiRetroIcon(tint: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        val cx = size.width / 2f
        val cy = size.height - 2.dp.toPx()
        val stroke = 1.5.dp.toPx()
        drawCircle(tint, radius = 1.4.dp.toPx(), center = Offset(cx, cy))
        for (r in listOf(4.5.dp.toPx(), 8.dp.toPx(), 11.5.dp.toPx())) {
            drawArc(
                color = tint,
                startAngle = 225f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun DataArrowsIcon(tint: Color) {
    Canvas(modifier = Modifier.size(width = 12.dp, height = 14.dp)) {
        val stroke = 1.4.dp.toPx()
        // Up arrow
        val ux = 3.5.dp.toPx()
        drawLine(tint, Offset(ux, 2.dp.toPx()), Offset(ux, size.height - 2.dp.toPx()), stroke)
        drawLine(tint, Offset(ux, 2.dp.toPx()), Offset(1.dp.toPx(), 5.dp.toPx()), stroke)
        drawLine(tint, Offset(ux, 2.dp.toPx()), Offset(6.dp.toPx(), 5.dp.toPx()), stroke)

        // Down arrow
        val dx = 9.dp.toPx()
        drawLine(tint, Offset(dx, 2.dp.toPx()), Offset(dx, size.height - 2.dp.toPx()), stroke)
        drawLine(tint, Offset(dx, size.height - 2.dp.toPx()), Offset(6.5.dp.toPx(), size.height - 5.dp.toPx()), stroke)
        drawLine(tint, Offset(dx, size.height - 2.dp.toPx()), Offset(11.5.dp.toPx(), size.height - 5.dp.toPx()), stroke)
    }
}

@Composable
private fun BatteryRetroBadge(percent: Int, ink: Color, bg: Color) {
    Box(
        modifier = Modifier
            .width(28.dp)
            .height(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 1.4.dp.toPx()
            // Battery tip on left (Nokia style) or right
            drawRect(
                color = ink,
                topLeft = Offset(0f, 4.dp.toPx()),
                size = Size(2.5.dp.toPx(), size.height - 8.dp.toPx())
            )
            // Outer frame
            drawRoundRect(
                color = ink,
                topLeft = Offset(2.5.dp.toPx(), 1.dp.toPx()),
                size = Size(size.width - 3.5.dp.toPx(), size.height - 2.dp.toPx()),
                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
                style = Stroke(width = stroke)
            )
        }
        Text(
            text = percent.toString(),
            color = ink,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}
