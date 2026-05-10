package com.empiretycoon.game.ui.screens.jobs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
 * Carpintero — cortes de tablones con sierra rotativa.
 *
 * Mecánica:
 *  - Una sierra cruza un tablón de izquierda a derecha en ~1.4s.
 *  - Línea de corte marcada en posición aleatoria (40..80%).
 *  - Tap "CORTAR" cuando la sierra esté sobre la línea. Tolerancia ±5%
 *    = +3, ±10% = +1, fuera = 0.
 *  - 8 tablones, 30s, score teórico ~24.
 */
@Composable
fun CarpenterJobScreen(state: GameState, onFinish: (Double) -> Unit, onCancel: () -> Unit) {
    val job = JobId.CARPENTER
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }
    val total = 8
    var idx by remember { mutableStateOf(0) }
    var sawX by remember { mutableStateOf(0f) }
    var targetX by remember { mutableStateOf(0.4f + rng.nextFloat() * 0.4f) }
    var score by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<String?>(null) }
    var hold by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableStateOf(30) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && !done) { delay(1000L); secondsLeft -= 1 }
        done = true
    }
    LaunchedEffect(idx, done) {
        if (done || idx >= total) { if (idx >= total) done = true; return@LaunchedEffect }
        sawX = 0f; targetX = 0.4f + rng.nextFloat() * 0.4f
        lastResult = null; hold = false
        while (!done && sawX < 1f) { delay(16L); sawX += 0.012f }
        if (!done && sawX >= 1f) { hold = true; lastResult = "red"; delay(450L); idx += 1 }
    }
    LaunchedEffect(hold, lastResult, done) {
        if (hold && !done && lastResult != null && sawX < 1f) { delay(400L); idx += 1 }
    }
    fun tap() {
        if (hold || done) return
        val diff = kotlin.math.abs(sawX - targetX)
        val (g, c) = when { diff <= 0.05f -> 3 to "green"; diff <= 0.10f -> 1 to "yellow"; else -> 0 to "red" }
        score += g; lastResult = c; hold = true
    }

    val maxScore = 24
    val scoreMul = if (done) (0.5 + (score.toFloat() / maxScore).coerceIn(0f, 1f)).toDouble() else 0.5

    Column(Modifier.fillMaxSize().background(Ink).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(job.emoji, fontSize = 26.sp); Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Carpintero: corta donde marca la línea", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap CORTAR cuando la sierra esté sobre la línea roja.", color = Dim, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Tablón", color = Dim, fontSize = 10.sp)
                Text("${(idx + 1).coerceAtMost(total)}/$total", color = Sapphire, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator({ secondsLeft.toFloat() / 30f }, Modifier.fillMaxWidth().height(4.dp))
        Spacer(Modifier.height(8.dp))
        EmpireCard(borderColor = Sapphire) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text("Score", color = Dim, fontSize = 10.sp)
                    Text("$score / $maxScore", color = if (score >= maxScore / 2) Emerald else Paper, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Wage", color = Dim, fontSize = 10.sp)
                    Text((previewBaseWage * scoreMul).fmtMoney(), color = if (scoreMul >= 1.10) Emerald else Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("×${"%.2f".format(scoreMul)}", color = Dim, fontSize = 10.sp)
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        // Tablón con sierra
        Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF8D6E63))) {
            Canvas(Modifier.fillMaxSize()) {
                // línea objetivo (rojo)
                val w = size.width; val h = size.height
                drawRect(Color(0xFFE53935), Offset(w * targetX - 3f, 0f), Size(6f, h))
                // sierra
                drawRect(Color(0xFFFFFFFF), Offset(w * sawX - 8f, h * 0.10f), Size(16f, h * 0.80f))
            }
        }
        Text("Posición sierra: ${(sawX * 100).toInt()}% · objetivo ${(targetX * 100).toInt()}%",
            color = Paper, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        lastResult?.let {
            val (lbl, c) = when (it) { "green" -> "🌟 Corte preciso (+3)" to Emerald; "yellow" -> "👍 Casi (+1)" to Color(0xFFFFB74D); else -> "💀 Tablón roto" to Color(0xFFFF7A7A) }
            Text(lbl, color = c, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        }
        Spacer(Modifier.weight(1f))
        if (done) Button({ onFinish(scoreMul) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
            Text("Cobrar y volver", color = Color.Black, fontWeight = FontWeight.Bold)
        } else Row(Modifier.fillMaxWidth()) {
            TextButton({ onCancel() }, Modifier.weight(0.4f)) { Text("Cancelar", color = Dim, fontSize = 12.sp) }
            Button({ tap() }, enabled = !hold, modifier = Modifier.weight(0.6f),
                colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
                Text("🪚 CORTAR", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
