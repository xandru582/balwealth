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
 * Fontanero — repara fugas tappeando con la llave la fuga correcta.
 *
 * Mecánica:
 *  - Pantalla con 4 tubos verticales. En cada ronda, 1 random tiene
 *    una fuga 💧 visible.
 *  - Hay 3 herramientas para reparar (LLAVE / SOLDADURA / CINTA), cada
 *    una tiene un emoji y se especializa en un tipo de fuga (pequeña /
 *    media / grande, distinguible por color).
 *  - Tap herramienta correcta = +1, mal = -1.
 *  - 30s, 1.6s/ronda, score teórico ~16.
 */
@Composable
fun PlumberJobScreen(state: GameState, onFinish: (Double) -> Unit, onCancel: () -> Unit) {
    val job = JobId.PLUMBER
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }
    val tools = listOf("🔧" to "LLAVE", "🔥" to "SOLDADURA", "🩹" to "CINTA")
    val severities = listOf(Color(0xFF66BB6A) to "small", Color(0xFFFFB74D) to "medium", Color(0xFFFF5252) to "big")
    val roundDurMs = 1600L
    var leakPipe by remember { mutableStateOf(rng.nextInt(4)) }
    var severity by remember { mutableStateOf(rng.nextInt(severities.size)) }
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
            delay(280L); leakPipe = rng.nextInt(4); severity = rng.nextInt(severities.size)
            roundStart = System.currentTimeMillis(); feedback = null
        }
    }
    fun tapTool(idx: Int) {
        if (done || feedback != null) return
        if (idx == severity) { score += 1; feedback = "ok" } else { score -= 1; feedback = "bad" }
    }

    val maxScore = 16
    val scoreMul = if (done) (0.5 + (score.coerceAtLeast(0).toFloat() / maxScore).coerceIn(0f, 1f)).toDouble() else 0.5
    val pctTime = ((roundDurMs - (nowMs - roundStart)).coerceAtLeast(0L) / roundDurMs.toFloat()).coerceIn(0f, 1f)
    val (sevColor, sevName) = severities[severity]
    val expectedTool = tools[severity]

    Column(Modifier.fillMaxSize().background(Ink).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(job.emoji, fontSize = 26.sp); Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Fontanero: usa la herramienta correcta", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Gold)
                Text("🟢 → 🔧 · 🟠 → 🔥 · 🔴 → 🩹", color = Dim, fontSize = 11.sp)
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
                    Text("Reparaciones", color = Dim, fontSize = 10.sp)
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

        // Tubos con fuga
        Row(Modifier.fillMaxWidth().height(180.dp)) {
            for (i in 0 until 4) {
                Box(Modifier.weight(1f).fillMaxHeight().padding(4.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF455A64)),
                    contentAlignment = Alignment.Center) {
                    if (i == leakPipe) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(sevColor.copy(alpha = 0.3f))
                                .padding(4.dp).clip(RoundedCornerShape(20.dp)).background(sevColor),
                                contentAlignment = Alignment.Center) {
                                Text("💧", fontSize = 22.sp)
                            }
                            Text("Fuga $sevName", color = sevColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        feedback?.let {
            val (lbl, c) = when (it) { "ok" -> "🌟 ¡Reparado! (+1)" to Emerald; "bad" -> "❌ Herramienta equivocada (-1) — era ${expectedTool.first}" to Color(0xFFFF7A7A); else -> "⏰ Lento (-1)" to Color(0xFFFF7A7A) }
            Spacer(Modifier.height(4.dp)); Text(lbl, color = c, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(1f))

        Row(Modifier.fillMaxWidth()) {
            for ((idx, t) in tools.withIndex()) {
                val (emoji, label) = t
                Box(Modifier.weight(1f).padding(3.dp).height(70.dp).clip(RoundedCornerShape(10.dp)).background(InkBorder).clickable { tapTool(idx) }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(emoji, fontSize = 28.sp); Text(label, color = Paper, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
