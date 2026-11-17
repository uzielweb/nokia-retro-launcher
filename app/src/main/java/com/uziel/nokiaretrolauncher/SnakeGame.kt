package com.uziel.nokiaretrolauncher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

class SnakeController(
    initialHighScore: Int = 0,
    private val onHighScoreChanged: (Int) -> Unit = {}
) {
    companion object {
        const val COLS = 20
        const val ROWS = 12
    }

    var snake by mutableStateOf(initialSnake())
        private set
    var food by mutableStateOf(spawnFood(initialSnake()))
        private set
    var direction by mutableStateOf(DpadDirection.RIGHT)
        private set
    private var nextDirection: DpadDirection = DpadDirection.RIGHT

    var score by mutableIntStateOf(0)
        private set
    var highScore by mutableIntStateOf(initialHighScore)
        private set
    var isPaused by mutableStateOf(false)
        private set
    var isGameOver by mutableStateOf(false)
        private set

    fun reset() {
        val start = initialSnake()
        snake = start
        direction = DpadDirection.RIGHT
        nextDirection = DpadDirection.RIGHT
        food = spawnFood(start)
        score = 0
        isPaused = false
        isGameOver = false
    }

    fun steer(newDir: DpadDirection) {
        if (newDir == DpadDirection.CENTER) {
            togglePauseOrRestart()
            return
        }
        val opposite = when (direction) {
            DpadDirection.UP -> DpadDirection.DOWN
            DpadDirection.DOWN -> DpadDirection.UP
            DpadDirection.LEFT -> DpadDirection.RIGHT
            DpadDirection.RIGHT -> DpadDirection.LEFT
            DpadDirection.CENTER -> DpadDirection.CENTER
        }
        if (newDir != opposite) {
            nextDirection = newDir
            if (isPaused && !isGameOver) {
                isPaused = false
            }
        }
    }

    fun togglePauseOrRestart() {
        if (isGameOver) {
            reset()
        } else {
            isPaused = !isPaused
        }
    }

    fun step(onEat: () -> Unit, onDie: () -> Unit) {
        if (isPaused || isGameOver) return

        direction = nextDirection
        val head = snake.first()
        val next = when (direction) {
            DpadDirection.UP -> head.first to (head.second - 1)
            DpadDirection.DOWN -> head.first to (head.second + 1)
            DpadDirection.LEFT -> (head.first - 1) to head.second
            DpadDirection.RIGHT -> (head.first + 1) to head.second
            DpadDirection.CENTER -> head
        }

        val hitWall = next.first < 0 || next.first >= COLS || next.second < 0 || next.second >= ROWS
        val ate = next == food
        val bodyToCheck = if (ate) snake else snake.dropLast(1)
        val hitSelf = bodyToCheck.contains(next)

        if (hitWall || hitSelf) {
            isGameOver = true
            onDie()
            return
        }

        val updated = if (ate) {
            listOf(next) + snake
        } else {
            listOf(next) + snake.dropLast(1)
        }
        snake = updated

        if (ate) {
            score += 10
            if (score > highScore) {
                highScore = score
                onHighScoreChanged(highScore)
            }
            food = spawnFood(updated)
            onEat()
        }
    }

    private fun initialSnake(): List<Pair<Int, Int>> = listOf(
        10 to 6,
        9 to 6,
        8 to 6,
        7 to 6
    )

    private fun spawnFood(occupied: List<Pair<Int, Int>>): Pair<Int, Int> {
        val free = mutableListOf<Pair<Int, Int>>()
        for (x in 0 until COLS) {
            for (y in 0 until ROWS) {
                val cell = x to y
                if (!occupied.contains(cell)) free.add(cell)
            }
        }
        return if (free.isEmpty()) 0 to 0 else free[Random.nextInt(free.size)]
    }
}

@Composable
fun SnakeLcdScreen(
    controller: SnakeController,
    theme: LcdTheme,
    onEat: () -> Unit,
    onDie: () -> Unit,
    onExit: () -> Unit
) {
    LaunchedEffect(controller.isPaused, controller.isGameOver, controller.score) {
        while (!controller.isPaused && !controller.isGameOver) {
            val tickMs = (150L - (controller.score / 20) * 8L).coerceAtLeast(70L)
            delay(tickMs)
            controller.step(onEat = onEat, onDie = onDie)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header: Title | Score | High Score
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr(R.string.snake_title),
                color = theme.ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = tr(R.string.snake_score, controller.score),
                color = theme.ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = tr(R.string.snake_high_score, controller.highScore),
                color = theme.ink.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Playable Nokia Matrix Arena
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .border(2.dp, theme.ink, RoundedCornerShape(2.dp))
                .padding(3.dp)
                .border(1.dp, theme.ink.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellW = size.width / SnakeController.COLS
                val cellH = size.height / SnakeController.ROWS

                // Faint retro pixel grid
                for (x in 0 until SnakeController.COLS) {
                    for (y in 0 until SnakeController.ROWS) {
                        drawRect(
                            color = theme.ink.copy(alpha = 0.07f),
                            topLeft = Offset(x * cellW + 1f, y * cellH + 1f),
                            size = Size((cellW - 2f).coerceAtLeast(1f), (cellH - 2f).coerceAtLeast(1f))
                        )
                    }
                }

                // Food in classic Nokia 3310 4-pixel cross style
                val fx = controller.food.first * cellW
                val fy = controller.food.second * cellH
                val padX = cellW * 0.16f
                val padY = cellH * 0.16f
                drawRect(
                    color = theme.ink,
                    topLeft = Offset(fx + cellW * 0.34f, fy + padY),
                    size = Size(cellW * 0.32f, cellH - padY * 2f)
                )
                drawRect(
                    color = theme.ink,
                    topLeft = Offset(fx + padX, fy + cellH * 0.34f),
                    size = Size(cellW - padX * 2f, cellH * 0.32f)
                )

                // Snake body & head
                controller.snake.forEachIndexed { idx, (sx, sy) ->
                    val left = sx * cellW + 1.5f
                    val top = sy * cellH + 1.5f
                    val w = (cellW - 3f).coerceAtLeast(2f)
                    val h = (cellH - 3f).coerceAtLeast(2f)
                    if (idx == 0) {
                        drawRect(
                            color = theme.ink,
                            topLeft = Offset(sx * cellW + 0.5f, sy * cellH + 0.5f),
                            size = Size((cellW - 1f).coerceAtLeast(3f), (cellH - 1f).coerceAtLeast(3f))
                        )
                        drawRect(
                            color = theme.background,
                            topLeft = Offset(sx * cellW + cellW * 0.35f, sy * cellH + cellH * 0.35f),
                            size = Size((cellW * 0.28f).coerceAtLeast(2f), (cellH * 0.28f).coerceAtLeast(2f))
                        )
                    } else {
                        drawRect(
                            color = theme.ink,
                            topLeft = Offset(left, top),
                            size = Size(w, h)
                        )
                        drawRect(
                            color = theme.background.copy(alpha = 0.2f),
                            topLeft = Offset(left, top),
                            size = Size(w, h),
                            style = Stroke(width = 1f)
                        )
                    }
                }
            }

            if (controller.isPaused || controller.isGameOver) {
                Column(
                    modifier = Modifier
                        .background(theme.tabBackground, RoundedCornerShape(4.dp))
                        .border(1.5.dp, theme.tabText, RoundedCornerShape(4.dp))
                        .clickable { controller.togglePauseOrRestart() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tr(
                            if (controller.isGameOver) R.string.snake_game_over else R.string.snake_paused
                        ),
                        color = theme.tabText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = tr(R.string.snake_press_start),
                        color = theme.tabText.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Bottom Softkeys
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val leftText = tr(
                if (controller.isPaused || controller.isGameOver) R.string.softkey_start
                else R.string.softkey_pause
            )
            Text(
                text = leftText,
                color = theme.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { controller.togglePauseOrRestart() }
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Text(
                text = tr(R.string.softkey_back),
                color = theme.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onExit
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}
