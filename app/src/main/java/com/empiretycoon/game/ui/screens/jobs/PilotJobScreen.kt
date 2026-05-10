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
 * Piloto de avión — ajusta el ángulo de aterrizaje al objetivo.
 *
 * Mecánica:
 *  - Cada ronda muestra ángulo actual (-30..+30°) y ángulo objetivo.
 *  - Botones -5° / +5° para ajustar.
 *  - Tap ATERRIZAR cuando estés ±2° = +1, ±5° = +1, fuera = 0.
 *  - 6s por ronda, 30s, score teórico ~12.
 */
@Composable
fun PilotJobScreen(state: GameState, onFinish: (Double) -> Unit, onCancel: () -> Unit) {
    val job = JobId.AIRLINE_PILOT
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }
    val roundDurMs = 6000L
    var angle by remember { mutableStateOf(0) }
    var target by remember { mutableStateOf(((rng.nextInt(11) - 5) * 5)) }
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
            delay(450L); angle = 0; target = ((rng.nextInt(11) - 5) * 5); roundStart = System.currentTimeMillis(); feedback = null
        }
    }
    fun adjust(delta: Int) {
        if (done || feedback != null) return
        angle = (angle + delta).coerceIn(-30, 30)
    }
    fun land() {
        if (done || feedback != null) return
        val diff = kotlin.math.abs(angle - target)
        if (diff == 0) { score += 2; feedback = "perfect" }
        else if (diff <= 5) { score += 1; feedback = "ok" }
        else { score -= 1; feedback = "bad" }
    }

    val maxScore = 12
    val scoreMul = if (done) (0.5 + (score.coerceAtLeast(0).toFloat() / maxScore).coerceIn(0f, 1f)).toDouble() else 0.5
    val pctTime = ((roundDurMs - (nowMs - roundStart)).coerceAtLeast(0L) / roundDurMs.toFloat()).coerceIn(0f, 1f)

    Column(Modifier.fillMaxSize().background(Ink).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(job.emoji, fontSize = 26.sp); Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Piloto: ajusta el ángulo y aterriza", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Gold)
                Text("± botones · ATERRIZAR cuando ángulo = objetivo (±2° perfecto)", color = Dim, fontSize = 11.sp)
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
                    Text("Aterrizajes", color = Dim, fontSize = 10.sp)
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

        EmpireCard(borderColor = when (feedback) { "perfect", "ok" -> Emerald; "bad", "timeout" -> Color(0xFFFF7A7A); else -> Gold }) {
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✈️", fontSize = 56.sp)
                Spacer(Modifier.height(4.dp))
                Text("Ángulo actual: ${angle}°", color = Paper, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Objetivo: ${target}°", color = Gold, fontSize = 14.sp)
                Text("Diferencia: ${kotlin.math.abs(angle - target)}°", color = Dim, fontSize = 12.sp)
                feedback?.let {
                    val (lbl, c) = when (it) { "perfect" -> "🌟 ¡PERFECTO! +2" to Emerald; "ok" -> "✅ +1" to Emerald; "bad" -> "💥 -1" to Color(0xFFFF7A7A); else -> "⏰ -1" to Color(0xFFFF7A7A) }
                    Spacer(Modifier.height(4.dp)); Text(lbl, color = c, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(10.dp)).background(InkBorder).clickable { adjust(-5) }, contentAlignment = Alignment.Center) {
                Text("◀ −5°", color = Paper, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.weight(1.4f).height(60.dp).clip(RoundedCornerShape(10.dp)).background(Gold).clickable { land() }, contentAlignment = Alignment.Center) {
                Text("🛬 ATERRIZAR", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
            Box(Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(10.dp)).background(InkBorder).clickable { adjust(5) }, contentAlignment = Alignment.Center) {
                Text("+5° ▶", color = Paper, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
