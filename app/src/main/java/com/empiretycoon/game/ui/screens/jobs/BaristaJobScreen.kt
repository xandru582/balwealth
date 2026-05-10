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
 * Barista — patrones de latte art tap-secuencia.
 *
 * Mecánica:
 *  - 6 puntos numerados (1..6) en una "taza" virtual con disposiciones
 *    distintas según el patrón (corazón / hoja / roseta / cisne).
 *  - Tap los puntos en orden. Combo entero = +1, mal = -1.
 *  - 6s por patrón. 30s, score teórico ~5.
 */
private data class LattePoint(val id: Int, val xPct: Float, val yPct: Float)
private val LATTE_PATTERNS = listOf(
    listOf(LattePoint(1, 0.50f, 0.20f), LattePoint(2, 0.30f, 0.40f), LattePoint(3, 0.70f, 0.40f), LattePoint(4, 0.20f, 0.65f), LattePoint(5, 0.80f, 0.65f), LattePoint(6, 0.50f, 0.85f)) to "❤️ Corazón",
    listOf(LattePoint(1, 0.30f, 0.20f), LattePoint(2, 0.50f, 0.30f), LattePoint(3, 0.70f, 0.20f), LattePoint(4, 0.50f, 0.50f), LattePoint(5, 0.40f, 0.75f), LattePoint(6, 0.60f, 0.75f)) to "🍃 Hoja",
    listOf(LattePoint(1, 0.20f, 0.40f), LattePoint(2, 0.40f, 0.30f), LattePoint(3, 0.60f, 0.30f), LattePoint(4, 0.80f, 0.40f), LattePoint(5, 0.50f, 0.60f), LattePoint(6, 0.50f, 0.85f)) to "🌹 Roseta",
    listOf(LattePoint(1, 0.50f, 0.20f), LattePoint(2, 0.70f, 0.40f), LattePoint(3, 0.30f, 0.50f), LattePoint(4, 0.50f, 0.65f), LattePoint(5, 0.40f, 0.85f), LattePoint(6, 0.60f, 0.85f)) to "🦢 Cisne"
)

@Composable
fun BaristaJobScreen(state: GameState, onFinish: (Double) -> Unit, onCancel: () -> Unit) {
    val job = JobId.BARISTA
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }
    val roundDurMs = 6000L
    var idx by remember { mutableStateOf(rng.nextInt(LATTE_PATTERNS.size)) }
    var nextStep by remember { mutableStateOf(1) }
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
            delay(80L); nowMs = System.currentTimeMillis()
            if (nowMs - roundStart >= roundDurMs && feedback == null) { feedback = "timeout"; score -= 1 }
        }
    }
    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(550L); idx = rng.nextInt(LATTE_PATTERNS.size); nextStep = 1
            roundStart = System.currentTimeMillis(); feedback = null
        }
    }
    val (pattern, patternName) = LATTE_PATTERNS[idx]
    fun tap(id: Int) {
        if (done || feedback != null) return
        if (id == nextStep) {
            val ns = nextStep + 1
            if (ns > pattern.size) { score += 1; feedback = "ok" } else nextStep = ns
        } else { score -= 1; feedback = "bad" }
    }

    val maxScore = 5
    val scoreMul = if (done) (0.5 + (score.coerceAtLeast(0).toFloat() / maxScore).coerceIn(0f, 1f)).toDouble() else 0.5
    val pctTime = ((roundDurMs - (nowMs - roundStart)).coerceAtLeast(0L) / roundDurMs.toFloat()).coerceIn(0f, 1f)

    Column(Modifier.fillMaxSize().background(Ink).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(job.emoji, fontSize = 26.sp); Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Barista: $patternName", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap puntos en orden 1→6. Próximo: $nextStep", color = Dim, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Tiempo", color = Dim, fontSize = 10.sp)
                Text("${secondsLeft}s", color = Sapphire, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator({ secondsLeft.toFloat() / 30f }, Modifier.fillMaxWidth().height(4.dp))
        Spacer(Modifier.height(6.dp))
        EmpireCard(borderColor = Sapphire) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text("Cafés", color = Dim, fontSize = 10.sp)
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

        // Taza con puntos
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp)).background(Color(0xFF8D6E63))) {
            Box(Modifier.fillMaxSize().padding(12.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFD7CCC8))) {
                for (p in pattern) {
                    val isDone = p.id < nextStep; val isNext = p.id == nextStep && feedback == null
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.align(Alignment.TopStart).offset(x = (p.xPct * 280).dp, y = (p.yPct * 280).dp)
                            .size(44.dp).clip(RoundedCornerShape(22.dp))
                            .background(when {
                                isDone -> Color(0xFF66BB6A); isNext -> Gold; else -> Color(0xFF6D4C41)
                            }).clickable { tap(p.id) }, contentAlignment = Alignment.Center) {
                            Text("${p.id}", color = if (isDone || isNext) Color.Black else Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                feedback?.let {
                    Box(Modifier.fillMaxSize().background(Color(0x88000000)), contentAlignment = Alignment.Center) {
                        val (lbl, c) = when (it) { "ok" -> "☕ ¡Latte perfecto!" to Emerald; "bad" -> "❌ Trazo erróneo" to Color(0xFFFF7A7A); else -> "⏰ Lento" to Color(0xFFFF7A7A) }
                        Text(lbl, color = c, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        if (done) Button({ onFinish(scoreMul) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
            Text("Cobrar y volver", color = Color.Black, fontWeight = FontWeight.Bold)
        } else Row(Modifier.fillMaxWidth()) {
            TextButton({ onCancel() }, Modifier.weight(1f)) { Text("Cancelar", color = Dim) }
            Spacer(Modifier.width(8.dp))
            Button({ done = true }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB85C5C))) { Text("Plantarse", color = Color.White) }
        }
    }
}
