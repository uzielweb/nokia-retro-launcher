package com.uziel.nokiaretrolauncher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NokiaKeypad(
    onLeftSoftkey: () -> Unit,
    onRightSoftkey: () -> Unit,
    onCallPress: () -> Unit,
    onEndPress: () -> Unit,
    onDpadPress: (DpadDirection) -> Unit,
    onDigitPress: (String) -> Unit,
    onDigitLongPress: (String) -> Unit,
    backlightColor: Color? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0B0B0D))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top control cluster: Left Softkey/Call | Square D-Pad | Right Softkey/End
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.35f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left column: Softkey dash + Green Call
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ControlButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    backlightColor = backlightColor,
                    onClick = onLeftSoftkey
                ) {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(backlightColor ?: Color.White)
                    )
                }

                ControlButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    backlightColor = backlightColor,
                    onClick = onCallPress
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = tr(R.string.cd_call),
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Center: Large square rounded D-Pad with 4 directional shortcut icons + OK center
            Box(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                DpadControl(
                    onDpadPress = onDpadPress,
                    backlightColor = backlightColor,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f, matchHeightConstraintsFirst = true)
                )
            }

            // Right column: Softkey dash + Red End Call
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ControlButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    backlightColor = backlightColor,
                    onClick = onRightSoftkey
                ) {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(backlightColor ?: Color.White)
                    )
                }

                ControlButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    backlightColor = backlightColor,
                    onClick = onEndPress
                ) {
                    Icon(
                        imageVector = Icons.Filled.CallEnd,
                        contentDescription = tr(R.string.cd_end),
                        tint = Color(0xFFD93025),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Numeric T9 Grid (4 rows x 3 columns)
        val rows = listOf(
            listOf(
                T9KeySpec("1", "", false),
                T9KeySpec("2", "abc", false),
                T9KeySpec("3", "def", false)
            ),
            listOf(
                T9KeySpec("4", "ghi", false),
                T9KeySpec("5", "jkl", false),
                T9KeySpec("6", "mno", true)
            ),
            listOf(
                T9KeySpec("7", "pqrs", false),
                T9KeySpec("8", "tuv", false),
                T9KeySpec("9", "wxyz", true)
            ),
            listOf(
                T9KeySpec("*", "lock", false),
                T9KeySpec("0", "torch", false),
                T9KeySpec("#", "", false)
            )
        )

        rows.forEach { rowKeys ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowKeys.forEach { spec ->
                    T9KeyButton(
                        spec = spec,
                        backlightColor = backlightColor,
                        onClick = { onDigitPress(spec.digit) },
                        onLongClick = { onDigitLongPress(spec.digit) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

private data class T9KeySpec(
    val digit: String,
    val sub: String,
    val subOnLeft: Boolean
)

@Composable
private fun ControlButton(
    modifier: Modifier = Modifier,
    backlightColor: Color? = null,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val bgTop = backlightColor?.copy(alpha = 0.16f) ?: Color(0xFF1A1A20)
    val bgBottom = Color(0xFF101014)
    val borderColor = backlightColor?.copy(alpha = 0.65f) ?: Color(0xFF282830)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(bgTop, bgBottom)
                )
            )
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun DpadControl(
    onDpadPress: (DpadDirection) -> Unit,
    backlightColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val outerBorder = backlightColor?.copy(alpha = 0.88f) ?: Color(0xFFEBEBEB)
    val outerTop = backlightColor?.copy(alpha = 0.14f) ?: Color(0xFF18181D)
    val outerBottom = Color(0xFF0E0E12)
    val iconTint = backlightColor?.copy(alpha = 0.92f) ?: Color.White.copy(alpha = 0.84f)
    val okBg = backlightColor?.copy(alpha = 0.18f) ?: Color(0xFF1B1B22)
    val okBorder = backlightColor?.copy(alpha = 0.82f) ?: Color(0xFF42424C)
    val okTextColor = backlightColor ?: Color.White
    val okShadow = backlightColor?.let { Shadow(color = it, blurRadius = 10f) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(outerTop, outerBottom)
                )
            )
            .border(1.8.dp, outerBorder, shape)
    ) {
        // 3x3 directional ring with 4 shortcut icons + demarcated OK center
        Column(modifier = Modifier.fillMaxSize()) {
            // Top row -> UP (Camera)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { onDpadPress(DpadDirection.UP) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = tr(R.string.cd_dpad_up),
                    tint = iconTint,
                    modifier = Modifier.size(13.dp)
                )
            }
            // Middle row -> LEFT (Go to / Shortcuts) | CENTER (OK) | RIGHT (Settings)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.3f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onDpadPress(DpadDirection.LEFT) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = tr(R.string.cd_dpad_left),
                        tint = iconTint,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .padding(2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(okBg)
                        .border(1.2.dp, okBorder, RoundedCornerShape(10.dp))
                        .clickable { onDpadPress(DpadDirection.CENTER) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tr(R.string.dpad_ok_label),
                        color = okTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(shadow = okShadow)
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onDpadPress(DpadDirection.RIGHT) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = tr(R.string.cd_dpad_right),
                        tint = iconTint,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            // Bottom row -> DOWN (Messages)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { onDpadPress(DpadDirection.DOWN) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Email,
                    contentDescription = tr(R.string.cd_dpad_down),
                    tint = iconTint,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun T9KeyButton(
    spec: T9KeySpec,
    backlightColor: Color? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    val bgTop = backlightColor?.copy(alpha = 0.16f) ?: Color(0xFF1B1B21)
    val bgBottom = Color(0xFF111115)
    val borderColor = backlightColor?.copy(alpha = 0.65f) ?: Color(0xFF282830)
    val mainColor = backlightColor ?: Color.White
    val subColor = backlightColor?.copy(alpha = 0.82f) ?: Color.White.copy(alpha = 0.72f)
    val glowShadow = backlightColor?.let { Shadow(color = it, blurRadius = 10f) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(bgTop, bgBottom)
                )
            )
            .border(1.dp, borderColor, shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (spec.subOnLeft && spec.sub.isNotEmpty()) {
                Text(
                    text = spec.sub,
                    color = subColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    style = TextStyle(shadow = glowShadow),
                    modifier = Modifier.padding(end = 3.dp, top = 4.dp)
                )
            }

            Text(
                text = spec.digit,
                color = mainColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                style = TextStyle(shadow = glowShadow)
            )

            if (!spec.subOnLeft && spec.sub.isNotEmpty()) {
                Spacer(modifier = Modifier.width(3.dp))
                when (spec.sub) {
                    "lock" -> Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = tr(R.string.cd_lock),
                        tint = subColor,
                        modifier = Modifier
                            .size(13.dp)
                            .padding(top = 2.dp)
                    )
                    "torch" -> Icon(
                        imageVector = Icons.Filled.FlashlightOn,
                        contentDescription = tr(R.string.cd_torch),
                        tint = subColor,
                        modifier = Modifier
                            .size(14.dp)
                            .padding(top = 2.dp)
                    )
                    else -> Text(
                        text = spec.sub,
                        color = subColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        style = TextStyle(shadow = glowShadow),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
