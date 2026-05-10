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
 * Actor — secuencia de gestos / emociones en orden.
 *
 * Mecánica:
 *  - 4 emociones: SONREÍR 😊 / LLORAR 😢 / GRITAR 😱 / REÍR 😆
 *  - Cada ronda muestra una secuencia de 3 emociones aleatorias.
 *  - Tap los botones en el orden mostrado. Combo entero = +1.
 *  - Tap mal o timeout 5s = -1, nueva secuencia.
 *  - 30s, score teórico ~6 → scoreMul.
 */
private enum class ActMood(val emoji: String, val label: String) {
    SMILE("😊", "SONREÍR"), CRY("😢", "LLORAR"), SHOUT("😱", "GRITAR"), LAUGH("😆", "REÍR")
}

@Composable
fun ActorJobScreen(state: GameState, onFinish: (Double) -> Unit, onCancel: () -> Unit) {
    val job = JobId.ACTOR
    val totalSeconds = 30
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }
    val roundDurMs = 5000L
    var sequence by remember { mutableStateOf<List<ActMood>>(emptyList()) }
    var progress by remember { mutableStateOf(0) }
    var roundStart by remember { mutableStateOf(0L) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var secondsLeft by remember { mutableStateOf(totalSeconds) }
    var score by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    fun newSeq(): List<ActMood> = (0 until 3).map { ActMood.values().random(rng) }
    LaunchedEffect(Unit) { sequence = newSeq(); roundStart = System.currentTimeMillis() }
    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && !done) { delay(1000L); secondsLeft -= 1 }
        done = true
    }
    LaunchedEffect(Unit) {
        while (!done) {
            delay(80L); nowMs = System.currentTimeMillis()
            if (nowMs - roundStart >= roundDurMs && feedback == null && sequence.isNotEmpty()) { feedback = "timeout"; score -= 1 }
        }
    }
    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(450L); sequence = newSeq(); progress = 0; roundStart = System.currentTimeMillis(); feedback = null
        }
    }
    fun tap(m: ActMood) {
        if (done || feedback != null) return
        val expected = sequence.getOrNull(progress) ?: return
        if (m == expected) {
            val np = progress + 1
            if (np >= sequence.size) { score += 1; feedback = "ok" } else progress = np
        } else { score -= 1; feedback = "bad" }
    }

    val maxScore = 6
    val scoreMul = if (done) (0.5 + (score.coerceAtLeast(0).toFloat() / maxScore).coerceIn(0f, 1f)).toDouble() else 0.5
    val pctTime = ((roundDurMs - (nowMs - roundStart)).coerceAtLeast(0L) / roundDurMs.toFloat()).coerceIn(0f, 1f)

    Column(Modifier.fillMaxSize().background(Ink).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(job.emoji, fontSize = 26.sp); Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Actor: secuencia de gestos", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap las emociones en orden mostrado.", color = Dim, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Tiempo", color = Dim, fontSize = 10.sp)
                Text("${secondsLeft}s", color = Sapphire, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator({ secondsLeft.toFloat() / totalSeconds.toFloat() }, Modifier.fillMaxWidth().height(4.dp))
        Spacer(Modifier.height(8.dp))
        EmpireCard(borderColor = Sapphire) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text("Funciones", color = Dim, fontSize = 10.sp)
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
        Spacer(Modifier.height(16.dp))

        EmpireCard(borderColor = when (feedback) { "ok" -> Emerald; "bad", "timeout" -> Color(0xFFFF7A7A); else -> Gold }) {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Guion:", color = Dim, fontSize = 11.sp); Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((i, m) in sequence.withIndex()) {
                        val isDone = i < progress
                        Text(if (isDone) "✓" else m.emoji, fontSize = 36.sp, color = if (isDone) Emerald else Color.Unspecified)
                    }
                }
                feedback?.let {
                    val (lbl, c) = when (it) { "ok" -> "🌟 ¡Función perfecta! +1" to Emerald; "bad" -> "❌ Mal gesto -1" to Color(0xFFFF7A7A); else -> "⏰ Lento -1" to Color(0xFFFF7A7A) }
                    Spacer(Modifier.height(4.dp)); Text(lbl, color = c, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth()) {
            for (m in ActMood.values()) {
                Box(Modifier.weight(1f).padding(3.dp).height(70.dp).clip(RoundedCornerShape(10.dp)).background(InkBorder).clickable { tap(m) }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(m.emoji, fontSize = 26.sp); Text(m.label, color = Paper, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        if (done) Button({ onFinish(scoreMul) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Gold)) { Text("Cobrar y volver", color = Color.Black, fontWeight = FontWeight.Bold) }
        else Row(Modifier.fillMaxWidth()) {
            TextButton({ onCancel() }, Modifier.weight(1f)) { Text("Cancelar", color = Dim) }
            Spacer(Modifier.width(8.dp))
            Button({ done = true }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB85C5C))) { Text("Plantarse", color = Color.White) }
        }
    }
}
