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
 * Albañil — coloca ladrillos en una pared vacía rellenando el hueco más bajo.
 *
 * Mecánica:
 *  - Pared 5 columnas × 6 filas. Cada columna tiene su propia altura
 *    de ladrillos colocados (empieza en 0).
 *  - Cada 1.0s aparece un "ladrillo entrante" en color (la columna
 *    correcta a colocar). Tienes 1.5s para tap el botón de columna
 *    correcto.
 *  - Tap correcto = +1 score, ladrillo añadido. Tap mal = -1.
 *  - 30s, score teórico ~22.
 */
@Composable
fun BricklayerJobScreen(state: GameState, onFinish: (Double) -> Unit, onCancel: () -> Unit) {
    val job = JobId.BRICKLAYER
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }
    val cols = 5; val rows = 6
    val roundDurMs = 1500L
    var heights by remember { mutableStateOf(List(cols) { 0 }) }
    var targetCol by remember { mutableStateOf(rng.nextInt(cols)) }
    var roundStart by remember { mutableStateOf(System.currentTimeMillis()) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var secondsLeft by remember { mutableStateOf(30) }
    var score by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && !done) { delay(1000L); secondsLeft -= 1 }
        done = true
    }
    LaunchedEffect(Unit) {
        while (!done) {
            delay(60L); nowMs = System.currentTimeMillis()
            if (nowMs - roundStart >= roundDurMs && feedback == null) { feedback = "timeout"; score -= 1 }
        }
    }
    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(220L)
            // Si pared llena en una columna, evita repetirla.
            val available = (0 until cols).filter { heights[it] < rows }
            targetCol = if (available.isEmpty()) rng.nextInt(cols) else available.random(rng)
            roundStart = System.currentTimeMillis(); feedback = null
        }
    }
    fun tap(c: Int) {
        if (done || feedback != null) return
        if (c == targetCol) {
            score += 1
            heights = heights.toMutableList().also { if (it[c] < rows) it[c] = it[c] + 1 }
            feedback = "ok"
        } else { score -= 1; feedback = "bad" }
    }

    val maxScore = 22
    val scoreMul = if (done) (0.5 + (score.coerceAtLeast(0).toFloat() / maxScore).coerceIn(0f, 1f)).toDouble() else 0.5
    val pctTime = ((roundDurMs - (nowMs - roundStart)).coerceAtLeast(0L) / roundDurMs.toFloat()).coerceIn(0f, 1f)

    Column(Modifier.fillMaxSize().background(Ink).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(job.emoji, fontSize = 26.sp); Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Albañil: levanta la pared", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap la columna del ladrillo entrante.", color = Dim, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Tiempo", color = Dim, fontSize = 10.sp)
                Text("${secondsLeft}s", color = Sapphire, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator({ secondsLeft.toFloat() / 30f }, Modifier.fillMaxWidth().height(4.dp))
        Spacer(Modifier.height(8.dp))
        EmpireCard(borderColor = Sapphire) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text("Ladrillos", color = Dim, fontSize = 10.sp)
                    Text("$score / $maxScore", color = if (score >= maxScore / 2) Emerald else Paper, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Wage", color = Dim, fontSize = 10.sp)
                    Text((previewBaseWage * scoreMul).fmtMoney(), color = if (scoreMul >= 1.10) Emerald else Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("×${"%.2f".format(scoreMul)}", color = Dim, fontSize = 10.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator({ pctTime }, color = if (pctTime > 0.5f) Emerald else if (pctTime > 0.25f) Color(0xFFFFB74D) else Color(0xFFFF7A7A), modifier = Modifier.fillMaxWidth().height(6.dp))
        Spacer(Modifier.height(12.dp))

        // Indicador de columna
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            for (c in 0 until cols) {
                Text(if (c == targetCol && feedback == null) "⬇️" else "  ", fontSize = 22.sp,
                    color = if (c == targetCol) Gold else Color.Transparent)
            }
        }

        // Pared
        Box(Modifier.fillMaxWidth().aspectRatio(cols.toFloat() / rows.toFloat()).clip(RoundedCornerShape(10.dp)).background(Color(0xFF132030)).padding(4.dp)) {
            Column(Modifier.fillMaxSize()) {
                for (row in (rows - 1) downTo 0) {
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        for (col in 0 until cols) {
                            val isFilled = heights[col] > row
                            Box(Modifier.weight(1f).fillMaxHeight().padding(1.dp).clip(RoundedCornerShape(4.dp))
                                .background(if (isFilled) Color(0xFF8D6E63) else Color(0xFF1B2738)))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            for (c in 0 until cols) {
                Box(Modifier.weight(1f).padding(2.dp).height(54.dp).clip(RoundedCornerShape(8.dp)).background(InkBorder).clickable { tap(c) }, contentAlignment = Alignment.Center) {
                    Text("${c + 1}", color = Paper, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.weight(1f))
        if (done) Button({ onFinish(scoreMul) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
            Text("Cobrar y volver", color = Color.Black, fontWeight = FontWeight.Bold)
        } else Row(Modifier.fillMaxWidth()) {
            TextButton({ onCancel() }, Modifier.weight(1f)) { Text("Cancelar", color = Dim) }
            Spacer(Modifier.width(8.dp))
            Button({ done = true }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB85C5C))) { Text("Plantarse", color = Color.White) }
        }
    }
}
