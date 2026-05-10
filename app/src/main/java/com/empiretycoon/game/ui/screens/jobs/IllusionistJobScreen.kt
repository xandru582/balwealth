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
 * Ilusionista — secuencia de cartas memorizada.
 *
 * 4 palos de cartas: ♠️ ♥️ ♦️ ♣️
 * Cada truco muestra una secuencia de 5 cartas brevemente y debes
 * reproducirla. Si todo correcto = +1, mal = -1. 6s/truco, 30s, score ~5.
 */
private enum class CardSuit(val emoji: String) { SPADE("♠️"), HEART("♥️"), DIAMOND("♦️"), CLUB("♣️") }

@Composable
fun IllusionistJobScreen(state: GameState, onFinish: (Double) -> Unit, onCancel: () -> Unit) {
    val job = JobId.ILLUSIONIST
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }
    val roundDurMs = 6000L
    var sequence by remember { mutableStateOf<List<CardSuit>>(emptyList()) }
    var progress by remember { mutableStateOf(0) }
    var roundStart by remember { mutableStateOf(0L) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var secondsLeft by remember { mutableStateOf(30) }
    var score by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    fun newSeq(): List<CardSuit> = (0 until 5).map { CardSuit.values().random(rng) }
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
        if (feedback != null) { delay(500L); sequence = newSeq(); progress = 0; roundStart = System.currentTimeMillis(); feedback = null }
    }
    fun tap(s: CardSuit) {
        if (done || feedback != null) return
        val expected = sequence.getOrNull(progress) ?: return
        if (s == expected) {
            val np = progress + 1
            if (np >= sequence.size) { score += 1; feedback = "ok" } else progress = np
        } else { score -= 1; feedback = "bad" }
    }

    val maxScore = 5
    val scoreMul = if (done) (0.5 + (score.coerceAtLeast(0).toFloat() / maxScore).coerceIn(0f, 1f)).toDouble() else 0.5
    val pctTime = ((roundDurMs - (nowMs - roundStart)).coerceAtLeast(0L) / roundDurMs.toFloat()).coerceIn(0f, 1f)

    Column(Modifier.fillMaxSize().background(Ink).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(job.emoji, fontSize = 26.sp); Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Ilusionista: secuencia de cartas", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Gold)
                Text("Reproduce la secuencia mostrada, en orden.", color = Dim, fontSize = 11.sp)
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
                    Text("Trucos perfectos", color = Dim, fontSize = 10.sp)
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
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎩", fontSize = 36.sp)
                Spacer(Modifier.height(4.dp))
                Text("Truco:", color = Dim, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((i, c) in sequence.withIndex()) {
                        val isDone = i < progress
                        Text(if (isDone) "✓" else c.emoji, fontSize = 32.sp, color = if (isDone) Emerald else Color.Unspecified)
                    }
                }
                feedback?.let {
                    val (lbl, c) = when (it) { "ok" -> "🌟 ¡Magia!" to Emerald; "bad" -> "❌ Truco fallido" to Color(0xFFFF7A7A); else -> "⏰ Tarde" to Color(0xFFFF7A7A) }
                    Spacer(Modifier.height(4.dp)); Text(lbl, color = c, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth()) {
            for (s in CardSuit.values()) {
                Box(Modifier.weight(1f).padding(3.dp).height(80.dp).clip(RoundedCornerShape(10.dp)).background(InkBorder).clickable { tap(s) }, contentAlignment = Alignment.Center) {
                    Text(s.emoji, fontSize = 36.sp)
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
