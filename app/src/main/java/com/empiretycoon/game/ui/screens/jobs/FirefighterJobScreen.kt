package com.empiretycoon.game.ui.screens.jobs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.engine.JobsEngine
import com.empiretycoon.game.model.GameState
import com.empiretycoon.game.model.JobId
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Mini-juego del Bombero: extinguir fuegos en un grid 4×4 antes de que se
 * descontrolen.
 *
 * Reglas:
 *  - Grid 4×4 (16 celdas). Estado por celda: NORMAL / FIRE / BURNT.
 *  - Cada ~900ms aparece un fuego en una celda NORMAL aleatoria.
 *  - Una celda FIRE se vuelve BURNT si pasa 4500ms sin apagar.
 *  - Tap celda FIRE → se apaga (vuelve a NORMAL) y +1 score.
 *  - Tap celda BURNT → no hace nada.
 *  - Tap celda NORMAL → no hace nada.
 *  - 30 segundos totales. Si todas las celdas están BURNT, game over
 *    inmediato.
 *  - Score final 0..N → scoreMul 0.5..1.5 según ratio score/maxScore.
 */
private enum class CellState { NORMAL, FIRE, BURNT }

private data class FireCell(
    val state: CellState = CellState.NORMAL,
    /** Tick de spawn del fuego — para detectar timeout de 4.5s. */
    val firedAtMs: Long = 0L
)

@Composable
fun FirefighterJobScreen(
    state: GameState,
    onFinish: (Double) -> Unit,
    onCancel: () -> Unit
) {
    val job = JobId.FIREFIGHTER
    val totalSeconds = 30
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val gridCols = 4
    val gridRows = 4
    val cellCount = gridCols * gridRows

    var secondsLeft by remember { mutableStateOf(totalSeconds) }
    var score by remember { mutableStateOf(0) }
    var grid by remember { mutableStateOf(List(cellCount) { FireCell() }) }
    var done by remember { mutableStateOf(false) }
    val rng = remember { Random(System.currentTimeMillis()) }

    // Countdown
    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && !done) {
            delay(1000L)
            secondsLeft -= 1
        }
        done = true
    }

    // Spawner — un fuego nuevo cada 900ms
    LaunchedEffect(Unit) {
        while (!done) {
            delay(900L)
            val now = System.currentTimeMillis()
            val empties = grid.mapIndexedNotNull { idx, c ->
                if (c.state == CellState.NORMAL) idx else null
            }
            if (empties.isNotEmpty()) {
                val pick = empties[rng.nextInt(empties.size)]
                grid = grid.toMutableList().also {
                    it[pick] = FireCell(state = CellState.FIRE, firedAtMs = now)
                }
            }
        }
    }

    // Burn-out check — fuegos que llevan >4500ms se queman
    LaunchedEffect(Unit) {
        while (!done) {
            delay(200L)
            val now = System.currentTimeMillis()
            var changed = false
            val newGrid = grid.toMutableList()
            for (i in newGrid.indices) {
                val c = newGrid[i]
                if (c.state == CellState.FIRE && now - c.firedAtMs >= 4500L) {
                    newGrid[i] = FireCell(state = CellState.BURNT)
                    changed = true
                }
            }
            if (changed) grid = newGrid
            // Si todas BURNT, game over
            if (newGrid.all { it.state == CellState.BURNT }) {
                done = true
            }
        }
    }

    val maxScore = 30
    val scoreMul = if (done) {
        (0.5 + (score.toFloat() / maxScore).coerceIn(0f, 1f)).toDouble()
    } else 0.5

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(job.emoji, fontSize = 26.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Bombero: contén el incendio", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap el fuego antes de que se descontrole",
                    color = Dim, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Tiempo", color = Dim, fontSize = 10.sp)
                Text("${secondsLeft}s", color = Sapphire, fontSize = 22.sp,
                    fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { secondsLeft.toFloat() / totalSeconds.toFloat() },
            modifier = Modifier.fillMaxWidth().height(4.dp)
        )
        Spacer(Modifier.height(8.dp))

        // Score + wage preview
        EmpireCard(borderColor = Sapphire) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Apagados", color = Dim, fontSize = 10.sp)
                    Text("$score / $maxScore",
                        color = if (score >= maxScore / 2) Emerald else Paper,
                        fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                val burntCount = grid.count { it.state == CellState.BURNT }
                Column(Modifier.weight(1f)) {
                    Text("Quemado", color = Dim, fontSize = 10.sp)
                    Text("$burntCount / $cellCount",
                        color = if (burntCount > 4) Color(0xFFFF7A7A) else Paper,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                val previewWin = previewBaseWage * scoreMul
                Column(horizontalAlignment = Alignment.End) {
                    Text("Wage previsto", color = Dim, fontSize = 10.sp)
                    Text(previewWin.fmtMoney(),
                        color = if (scoreMul >= 1.10) Emerald else Gold,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("×${"%.2f".format(scoreMul)}", color = Dim, fontSize = 10.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        // Grid — column de filas, fila de columnas
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF132030))
                .padding(6.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                for (row in 0 until gridRows) {
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        for (col in 0 until gridCols) {
                            val idx = row * gridCols + col
                            val cell = grid[idx]
                            val (bg, emoji) = when (cell.state) {
                                CellState.NORMAL -> Color(0xFF1B2738) to "  "
                                CellState.FIRE -> Color(0xFFE65100) to "🔥"
                                CellState.BURNT -> Color(0xFF3A1F1A) to "💀"
                            }
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .clickable {
                                        if (done) return@clickable
                                        if (cell.state != CellState.FIRE) return@clickable
                                        grid = grid.toMutableList().also {
                                            it[idx] = FireCell(state = CellState.NORMAL)
                                        }
                                        score += 1
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }
            if (done) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xCC000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🚒 Misión terminada",
                            color = Color.White, fontSize = 22.sp,
                            fontWeight = FontWeight.Black)
                        Text("Apagados: $score",
                            color = Color.White, fontSize = 14.sp)
                        Text("Wage: ${(previewBaseWage * scoreMul).fmtMoney()}",
                            color = Gold, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (done) {
            Button(
                onClick = { onFinish(scoreMul) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Gold)
            ) {
                Text("Cobrar y volver al hub",
                    color = Color.Black, fontWeight = FontWeight.Bold)
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar (sin energy ni cash)", color = Dim)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { done = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB85C5C))
                ) {
                    Text("Plantarse", color = Color.White)
                }
            }
        }
        Text(
            "Tip: cada celda FIRE no apagada en 4.5s queda BURNT (no apagable).",
            color = Dim, fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }
}
