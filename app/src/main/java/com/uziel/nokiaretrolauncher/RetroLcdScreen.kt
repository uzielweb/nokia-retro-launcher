package com.uziel.nokiaretrolauncher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
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
    status: DeviceStatus,
    dialBuffer: String,
    t9Filter: String,
    filteredApps: List<AppEntry>,
    selectedAppIndex: Int,
    gridState: LazyGridState,
    goToItems: List<Pair<String, () -> Unit>>,
    selectedGoToIndex: Int,
    settingItems: List<Triple<String, String, () -> Unit>>,
    selectedSettingIndex: Int,
    torchOn: Boolean,
    onOpenGoTo: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenSettings: () -> Unit,
    onLaunchApp: (AppEntry) -> Unit,
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
                    status = status,
                    dialBuffer = dialBuffer,
                    torchOn = torchOn,
                    onOpenGoTo = onOpenGoTo,
                    onOpenMenu = onOpenMenu,
                    onOpenSettings = onOpenSettings
                )
            }
            ScreenMode.MENU -> {
                AppDrawerContent(
                    apps = filteredApps,
                    selectedIndex = selectedAppIndex,
                    t9Filter = t9Filter,
                    gridState = gridState,
                    onLaunchApp = onLaunchApp
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
                    onRightSoftkey = onOpenMenu
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
                    onRightSoftkey = onOpenMenu
                )
            }
        }
    }
}

@Composable
private fun HomeLcdContent(
    theme: LcdTheme,
    status: DeviceStatus,
    dialBuffer: String,
    torchOn: Boolean,
    onOpenGoTo: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top status bar: antenna + signal on left, wifi + arrows + battery on right
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
                if (torchOn) {
                    Text(
                        text = "🔦",
                        fontSize = 11.sp,
                        color = theme.ink
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

        // Center area: Date or Dial Buffer + Divider with angular clock tab
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
                    modifier = Modifier.padding(bottom = 14.dp)
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
                    modifier = Modifier.padding(bottom = 18.dp)
                )
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

@Composable
private fun AppDrawerContent(
    apps: List<AppEntry>,
    selectedIndex: Int,
    t9Filter: String,
    gridState: LazyGridState,
    onLaunchApp: (AppEntry) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF17181E),
                        Color(0xFF101116)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (t9Filter.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF262830))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "T9: $t9Filter",
                        color = Color(0xFFD6D27A),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
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
                                .clickable { onLaunchApp(app) }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
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
                    }
                }
            }
        }
    }
}

@Composable
private fun RetroListMenuContent(
    title: String,
    theme: LcdTheme,
    items: List<Pair<String, String>>,
    selectedIndex: Int,
    leftLabel: String,
    rightLabel: String,
    onItemClick: (Int) -> Unit,
    onLeftSoftkey: () -> Unit,
    onRightSoftkey: () -> Unit
) {
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
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(theme.ink)
            )
            Spacer(modifier = Modifier.height(6.dp))

            items.forEachIndexed { idx, (label, value) ->
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
