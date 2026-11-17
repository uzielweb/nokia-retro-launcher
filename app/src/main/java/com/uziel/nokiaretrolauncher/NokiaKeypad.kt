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
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
                    onClick = onLeftSoftkey
                ) {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White)
                    )
                }

                ControlButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    onClick = onCallPress
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Call",
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Center: Large square rounded D-Pad with white border
            Box(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                DpadControl(
                    onDpadPress = onDpadPress,
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
                    onClick = onRightSoftkey
                ) {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White)
                    )
                }

                ControlButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    onClick = onEndPress
                ) {
                    Icon(
                        imageVector = Icons.Filled.CallEnd,
                        contentDescription = "End",
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
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1A20), Color(0xFF101014))
                )
            )
            .border(1.dp, Color(0xFF282830), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun DpadControl(
    onDpadPress: (DpadDirection) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF18181D), Color(0xFF0E0E12))
                )
            )
            .border(1.8.dp, Color(0xFFEBEBEB), shape)
    ) {
        // 3x3 invisible hit grid for UP, DOWN, LEFT, RIGHT, CENTER
        Column(modifier = Modifier.fillMaxSize()) {
            // Top row -> UP
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { onDpadPress(DpadDirection.UP) }
            )
            // Middle row -> LEFT | CENTER | RIGHT
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onDpadPress(DpadDirection.LEFT) }
                )
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .clickable { onDpadPress(DpadDirection.CENTER) }
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onDpadPress(DpadDirection.RIGHT) }
                )
            }
            // Bottom row -> DOWN
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { onDpadPress(DpadDirection.DOWN) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun T9KeyButton(
    spec: T9KeySpec,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1B1B21), Color(0xFF111115))
                )
            )
            .border(1.dp, Color(0xFF282830), shape)
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
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(end = 3.dp, top = 4.dp)
                )
            }

            Text(
                text = spec.digit,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )

            if (!spec.subOnLeft && spec.sub.isNotEmpty()) {
                Spacer(modifier = Modifier.width(3.dp))
                when (spec.sub) {
                    "lock" -> Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Lock",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(13.dp)
                            .padding(top = 2.dp)
                    )
                    "torch" -> Icon(
                        imageVector = Icons.Filled.FlashlightOn,
                        contentDescription = "Torch",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(14.dp)
                            .padding(top = 2.dp)
                    )
                    else -> Text(
                        text = spec.sub,
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
