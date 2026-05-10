package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.model.*
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Hub del Arcade. Muestra 5 mini-juegos: SNAKE jugable, los otros stub
 * "próximamente". Permite ajustar la apuesta y ver estadísticas.
 *
 * Cuando el jugador empieza una partida, switcheamos a la vista del juego
 * (estado local del Composable, no en el GameState). Al terminar, llamamos
 * a vm.arcadeFinishPlay() para que el motor liquide la partida.
 */
@Composable
fun ArcadeScreen(state: GameState, vm: GameViewModel) {
    val ar = state.arcade

    // Estado local: si null, mostramos el hub. Si != null, mostramos el juego.
    var activeGame by remember { mutableStateOf<ArcadeGameId?>(null) }
    var activeBet by remember { mutableStateOf(0.0) }

    if (activeGame == ArcadeGameId.SNAKE) {
        SnakePlayScreen(
            bet = activeBet,
            onFinish = { score, winnings ->
                vm.arcadeFinishPlay(ArcadeGameId.SNAKE, activeBet, score, winnings)
                activeGame = null
                activeBet = 0.0
            }
        )
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (!ar.unlocked) {
            ArcadeLockedView(state, vm)
        } else {
            ArcadeHubView(
                state = state,
                vm = vm,
                onStartGame = { game, bet ->
                    activeGame = game
                    activeBet = bet
                }
            )
        }
    }
}

// ===================== LOCKED =====================

@Composable
private fun ArcadeLockedView(state: GameState, vm: GameViewModel) {
    SectionTitle(
        "🎮 Arcade",
        subtitle = "Mini-juegos arcade jugables con apuestas en cash de empresa."
    )
    Spacer(Modifier.height(12.dp))

    val canUnlock = state.player.level >= 2
    EmpireCard(borderColor = if (canUnlock) Sapphire else InkBorder) {
        Text("Requisitos", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Paper)
        Text(
            "Nivel del jugador: ≥ 2 (tu nivel: ${state.player.level})",
            color = if (canUnlock) Emerald else Color(0xFFFF7A7A),
            fontSize = 12.sp
        )
    }
    Spacer(Modifier.height(12.dp))

    Button(
        onClick = { vm.arcadeUnlock() },
        enabled = canUnlock,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Gold)
    ) {
        Text("🎮 Abrir el Arcade",
            fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

// ===================== HUB =====================

@Composable
private fun ArcadeHubView(
    state: GameState,
    vm: GameViewModel,
    onStartGame: (ArcadeGameId, Double) -> Unit
) {
    val ar = state.arcade

    // ----- Header lifetime -----
    EmpireCard(borderColor = Gold) {
        Text("🎰 Tu historial", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Gold)
        Spacer(Modifier.height(4.dp))
        Row {
            StatCol("Partidas", "${ar.totalLifetimePlays}")
            Spacer(Modifier.width(16.dp))
            StatCol(
                "Neto lifetime",
                ar.totalLifetimeNet.fmtMoney(),
                color = if (ar.totalLifetimeNet >= 0) Emerald else Color(0xFFFF7A7A)
            )
        }
    }
    Spacer(Modifier.height(12.dp))

    // ----- Selector de apuesta -----
    BetSelector(state, vm)
    Spacer(Modifier.height(16.dp))

    // ----- Cards de juegos -----
    SectionTitle("Juegos disponibles")
    Spacer(Modifier.height(8.dp))
    for (game in ArcadeGameId.values()) {
        GameCard(
            game = game,
            stats = ar.statsFor(game),
            currentCash = state.company.cash,
            currentBet = ar.selectedBet,
            onPlay = {
                if (!game.available) {
                    // Stub: dispara notificación "próximamente" y no arranca.
                    vm.arcadePlaceBet(game, ar.selectedBet)
                    return@GameCard
                }
                // Pre-validación: si no hay cash, dispara la notif y no arranca.
                if (state.company.cash < ar.selectedBet) {
                    vm.arcadePlaceBet(game, ar.selectedBet)
                    return@GameCard
                }
                vm.arcadePlaceBet(game, ar.selectedBet)
                onStartGame(game, ar.selectedBet)
            }
        )
        Spacer(Modifier.height(8.dp))
    }

    // ----- Recientes -----
    if (ar.recentPlays.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        SectionTitle("📜 Últimas partidas")
        Spacer(Modifier.height(6.dp))
        for (play in ar.recentPlays.takeLast(8).reversed()) {
            RecentPlayRow(play)
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun StatCol(label: String, value: String, color: Color = Paper) {
    Column {
        Text(label, color = Dim, fontSize = 10.sp)
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BetSelector(state: GameState, vm: GameViewModel) {
    val bet = state.arcade.selectedBet
    EmpireCard(borderColor = Sapphire) {
        Text("Apuesta seleccionada", color = Dim, fontSize = 11.sp)
        Text(bet.fmtMoney(), color = Gold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row {
            QuickBetChip("50", 50.0, vm)
            QuickBetChip("100", 100.0, vm)
            QuickBetChip("250", 250.0, vm)
            QuickBetChip("500", 500.0, vm)
            QuickBetChip("1k", 1_000.0, vm)
            QuickBetChip("5k", 5_000.0, vm)
        }
        Text(
            "Rango ${ArcadeCatalog.MIN_BET.toInt()}–${ArcadeCatalog.MAX_BET.toInt()} €",
            color = Dim, fontSize = 10.sp
        )
    }
}

@Composable
private fun QuickBetChip(label: String, value: Double, vm: GameViewModel) {
    Box(
        Modifier
            .padding(end = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(InkBorder)
            .clickable { vm.arcadeSelectBet(value) }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, color = Paper, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GameCard(
    game: ArcadeGameId,
    stats: ArcadeGameStats,
    currentCash: Double,
    currentBet: Double,
    onPlay: () -> Unit
) {
    val canPlay = game.available && currentCash >= currentBet
    EmpireCard(borderColor = if (game.available) Gold else InkBorder) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(InkBorder),
                contentAlignment = Alignment.Center
            ) {
                Text(game.emoji, fontSize = 28.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(game.displayName, fontWeight = FontWeight.Bold, color = Paper, fontSize = 15.sp)
                Text(game.description, color = Dim, fontSize = 11.sp)
                if (stats.gamesPlayed > 0) {
                    Text(
                        "Mejor: ${stats.highScore} · Partidas: ${stats.gamesPlayed} · Mayor premio: ${stats.biggestWin.fmtMoney()}",
                        color = Sapphire, fontSize = 10.sp
                    )
                }
            }
            if (game.available) {
                Button(
                    onClick = onPlay,
                    enabled = canPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) {
                    Text("Jugar", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                Text("Próx.", color = Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RecentPlayRow(play: ArcadePlayResult) {
    val color = if (play.won) Emerald
        else if (play.winnings > 0) Color(0xFFFFB74D)
        else Color(0xFFFF7A7A)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(InkBorder)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(play.game.emoji, fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            "Día ${play.day}: score ${play.score} · apuesta ${play.bet.fmtMoney()}",
            color = Paper, fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            "${if (play.won) "+" else ""}${(play.winnings - play.bet).fmtMoney()}",
            color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold
        )
    }
}

// ===================== SNAKE GAME =====================

private const val SNAKE_COLS = 12
private const val SNAKE_ROWS = 16
private const val SNAKE_TICK_MS = 220L

private enum class SnakeDir { UP, DOWN, LEFT, RIGHT }

@Composable
private fun SnakePlayScreen(
    bet: Double,
    onFinish: (score: Int, winnings: Double) -> Unit
) {
    var snake by remember {
        mutableStateOf(listOf(
            SNAKE_COLS / 2 to SNAKE_ROWS / 2,
            SNAKE_COLS / 2 - 1 to SNAKE_ROWS / 2,
            SNAKE_COLS / 2 - 2 to SNAKE_ROWS / 2
        ))
    }
    var dir by remember { mutableStateOf(SnakeDir.RIGHT) }
    var pendingDir by remember { mutableStateOf<SnakeDir?>(null) }
    var food by remember { mutableStateOf(spawnFood(snake)) }
    var score by remember { mutableStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var ticks by remember { mutableStateOf(0L) }

    // Game loop
    LaunchedEffect(gameOver, paused) {
        while (!gameOver && !paused) {
            delay(SNAKE_TICK_MS)
            ticks += 1
            // Aplicar dirección pendiente si no es opuesta
            pendingDir?.let { p ->
                if (!isOpposite(dir, p)) dir = p
                pendingDir = null
            }
            val (hx, hy) = snake.first()
            val (nx, ny) = when (dir) {
                SnakeDir.UP -> hx to (hy - 1)
                SnakeDir.DOWN -> hx to (hy + 1)
                SnakeDir.LEFT -> (hx - 1) to hy
                SnakeDir.RIGHT -> (hx + 1) to hy
            }
            // Colisión paredes
            if (nx < 0 || nx >= SNAKE_COLS || ny < 0 || ny >= SNAKE_ROWS) {
                gameOver = true
                continue
            }
            // Colisión cuerpo
            if (snake.contains(nx to ny)) {
                gameOver = true
                continue
            }
            val ate = (nx to ny) == food
            val newSnake = if (ate) {
                listOf(nx to ny) + snake
            } else {
                listOf(nx to ny) + snake.dropLast(1)
            }
            snake = newSnake
            if (ate) {
                score += 1
                food = spawnFood(newSnake)
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .padding(12.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🐍", fontSize = 26.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Serpiente", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gold)
                Text("Apuesta: ${bet.fmtMoney()}", color = Dim, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Score", color = Dim, fontSize = 10.sp)
                Text("$score", color = Emerald, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(8.dp))

        // Predicted winnings
        val previewWin = ArcadeCatalog.snakeWinnings(bet, score)
        Text(
            "Previsión: ${if (previewWin > 0) "+" else ""}${(previewWin - bet).fmtMoney()} (gana ${previewWin.fmtMoney()})",
            color = if (previewWin > bet) Emerald else if (previewWin > 0) Color(0xFFFFB74D) else Dim,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))

        // Tablero
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(SNAKE_COLS.toFloat() / SNAKE_ROWS.toFloat())
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0E1B24))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellW = size.width / SNAKE_COLS
                val cellH = size.height / SNAKE_ROWS
                // Grid muy sutil
                for (i in 1 until SNAKE_COLS) {
                    drawRect(
                        color = Color(0x10FFFFFF),
                        topLeft = Offset(i * cellW, 0f),
                        size = Size(0.5f, size.height)
                    )
                }
                for (i in 1 until SNAKE_ROWS) {
                    drawRect(
                        color = Color(0x10FFFFFF),
                        topLeft = Offset(0f, i * cellH),
                        size = Size(size.width, 0.5f)
                    )
                }
                // Comida (rojo brillante con glow)
                val (fx, fy) = food
                drawRect(
                    color = Color(0xFFFF5252),
                    topLeft = Offset(fx * cellW + cellW * 0.15f, fy * cellH + cellH * 0.15f),
                    size = Size(cellW * 0.7f, cellH * 0.7f)
                )
                // Serpiente
                snake.forEachIndexed { idx, (sx, sy) ->
                    val isHead = idx == 0
                    val color = if (isHead) Color(0xFF66E58A) else Color(0xFF2EB85C)
                    drawRect(
                        color = color,
                        topLeft = Offset(sx * cellW + cellW * 0.05f, sy * cellH + cellH * 0.05f),
                        size = Size(cellW * 0.9f, cellH * 0.9f)
                    )
                }
                // Game over overlay
                if (gameOver) {
                    drawRect(
                        color = Color(0xCC000000),
                        topLeft = Offset.Zero,
                        size = size
                    )
                }
            }
            if (gameOver) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("💀 Game Over", fontSize = 22.sp, color = Color.White,
                        fontWeight = FontWeight.Black)
                    Text("Score final: $score", color = Color.White, fontSize = 14.sp)
                    Text("Recompensa: ${ArcadeCatalog.snakeWinnings(bet, score).fmtMoney()}",
                        color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Controles direccionales
        if (!gameOver) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                DirButton("▲", Modifier.size(56.dp)) { pendingDir = SnakeDir.UP }
                Row {
                    DirButton("◀", Modifier.size(56.dp)) { pendingDir = SnakeDir.LEFT }
                    Spacer(Modifier.width(56.dp))
                    DirButton("▶", Modifier.size(56.dp)) { pendingDir = SnakeDir.RIGHT }
                }
                DirButton("▼", Modifier.size(56.dp)) { pendingDir = SnakeDir.DOWN }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Acciones
        Row(modifier = Modifier.fillMaxWidth()) {
            if (!gameOver) {
                Button(
                    onClick = { paused = !paused },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Sapphire)
                ) {
                    Text(if (paused) "Reanudar" else "Pausa")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        // Cobrar como si game over en este momento.
                        val win = ArcadeCatalog.snakeWinnings(bet, score)
                        onFinish(score, win)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB85C5C))
                ) {
                    Text("Plantarse", color = Color.White)
                }
            } else {
                Button(
                    onClick = {
                        val win = ArcadeCatalog.snakeWinnings(bet, score)
                        onFinish(score, win)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) {
                    Text("Cobrar y volver al Arcade",
                        fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Recompensas: 4-9 piezas = recuperas proporcional · 10 = ×1.5 · 15 = ×2.5 · 25 = ×5 · 40+ = ×10",
            color = Dim, fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DirButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(InkBorder)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Gold, fontSize = 26.sp, fontWeight = FontWeight.Black)
    }
}

private fun isOpposite(a: SnakeDir, b: SnakeDir): Boolean = when (a) {
    SnakeDir.UP -> b == SnakeDir.DOWN
    SnakeDir.DOWN -> b == SnakeDir.UP
    SnakeDir.LEFT -> b == SnakeDir.RIGHT
    SnakeDir.RIGHT -> b == SnakeDir.LEFT
}

private fun spawnFood(snake: List<Pair<Int, Int>>): Pair<Int, Int> {
    val rng = Random.Default
    var pos: Pair<Int, Int>
    var safety = 0
    do {
        pos = rng.nextInt(SNAKE_COLS) to rng.nextInt(SNAKE_ROWS)
        safety++
        if (safety > 200) break  // tablero casi lleno; aceptamos lo que venga
    } while (snake.contains(pos))
    return pos
}
