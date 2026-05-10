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
 * Piloto de carreras — drift en cada curva. Cada curva indica dirección
 * (IZQ/DCHA) + tipo de turno (CERRADA/MEDIA/AMPLIA).
 *
 * Mecánica:
 *  - Cada 1.4s aparece una curva con dirección + tipo. Tap el botón
 *    correspondiente (6 botones: IZQ-CERRADA, IZQ-MEDIA, IZQ-AMPLIA,
 *    DCHA-CERRADA, DCHA-MEDIA, DCHA-AMPLIA).
 *  - Correcto = +1, mal o timeout = -1. 30s, score teórico ~16.
 */
private enum class Curve(val emoji: String, val label: String) {
    L_TIGHT("⬅️🔻", "IZQ-CERRADA"), L_MID("⬅️▽", "IZQ-MEDIA"), L_WIDE("⬅️▷", "IZQ-AMPLIA"),
    R_TIGHT("➡️🔻", "DCHA-CERRADA"), R_MID("➡️▽", "DCHA-MEDIA"), R_WIDE("➡️▷", "DCHA-AMPLIA")
}

@Composable
fun RacingJobScreen(state: GameState, onFinish: (Double) -> Unit, onCancel: () -> Unit) {
    val job = JobId.RACING_DRIVER
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }
    val roundDurMs = 1400L
    var expected by remember { mutableStateOf(Curve.values().random(rng)) }
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
        if (feedback != null) { delay(200L); expected = Curve.values().random(rng); roundStart = System.currentTimeMillis(); feedback = null }
    }
    fun tap(c: Curve) {
        if (done || feedback != null) return
        if (c == expected) { score += 1; feedback = "ok" } else { score -= 1; feedback = "bad" }
    }

    val maxScore = 16
    val scoreMul = if (done) (0.5 + (score.coerceAtLeast(0).toFloat() / maxScore).coerceIn(0f, 1f)).toDouble() else 0.5
    val pctTime = ((roundDurMs - (nowMs - roundStart)).coerceAtLeast(0L) / roundDurMs.toFloat()).coerceIn(0f, 1f)

    Column(Modifier.fillMaxSize().background(Ink).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(job.emoji, fontSize = 26.sp); Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Piloto de carreras: drift!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap el tipo de curva correcta antes de 1.4s.", color = Dim, fontSize = 11.sp)
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
                    Text("Curvas perfectas", color = Dim, fontSize = 10.sp)
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
        Spacer(Modifier.height(20.dp))

        EmpireCard(borderColor = when (feedback) { "ok" -> Emerald; "bad", "timeout" -> Color(0xFFFF7A7A); else -> Gold }) {
            Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Curva entrante:", color = Dim, fontSize = 11.sp)
                Text("🏎️", fontSize = 56.sp)
                Text(expected.label, color = Gold, fontSize = 18.sp, fontWeight = FontWeight.Black)
                feedback?.let {
                    val (lbl, c) = when (it) { "ok" -> "🌟 Drift perfecto +1" to Emerald; "bad" -> "💥 Te has salido -1" to Color(0xFFFF7A7A); else -> "⏰ Te lo comió la curva -1" to Color(0xFFFF7A7A) }
                    Text(lbl, color = c, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.weight(1f))

        // 6 botones en grid 2x3
        for (row in 0 until 2) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 3) {
                    val c = Curve.values()[row * 3 + col]
                    Box(Modifier.weight(1f).padding(3.dp).height(60.dp).clip(RoundedCornerShape(8.dp)).background(InkBorder).clickable { tap(c) }, contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(c.emoji, fontSize = 18.sp); Text(c.label, color = Paper, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (done) Button({ onFinish(scoreMul) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
            Text("Cobrar y volver", color = Color.Black, fontWeight = FontWeight.Bold)
        } else Row(Modifier.fillMaxWidth()) {
            TextButton({ onCancel() }, Modifier.weight(1f)) { Text("Cancelar", color = Dim) }
            Spacer(Modifier.width(8.dp))
            Button({ done = true }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB85C5C))) { Text("Plantarse", color = Color.White) }
        }
    }
}
