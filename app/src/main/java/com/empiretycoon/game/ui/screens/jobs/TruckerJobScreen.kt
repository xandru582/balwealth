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
 * Camionero — parking inverso. El camión retrocede; tap "FRENAR" cuando
 * esté alineado con el slot pintado.
 *
 * 5 entregas en 30s. Score teórico ~15.
 */
@Composable
fun TruckerJobScreen(state: GameState, onFinish: (Double) -> Unit, onCancel: () -> Unit) {
    val job = JobId.TRUCKER
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }
    val total = 5
    var idx by remember { mutableStateOf(0) }
    var truckX by remember { mutableStateOf(0f) }
    var slotX by remember { mutableStateOf(0.5f) }
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
        truckX = 0f; slotX = 0.3f + rng.nextFloat() * 0.5f; lastResult = null; hold = false
        while (!done && truckX < 1f) { delay(16L); truckX += 0.011f }
        if (!done && truckX >= 1f) { hold = true; lastResult = "red"; delay(450L); idx += 1 }
    }
    LaunchedEffect(hold, lastResult, done) {
        if (hold && !done && lastResult != null && truckX < 1f) { delay(400L); idx += 1 }
    }
    fun tap() {
        if (hold || done) return
        val diff = kotlin.math.abs(truckX - slotX)
        val (g, c) = when { diff <= 0.04f -> 3 to "green"; diff <= 0.10f -> 1 to "yellow"; else -> 0 to "red" }
        score += g; lastResult = c; hold = true
    }

    val maxScore = 15
    val scoreMul = if (done) (0.5 + (score.toFloat() / maxScore).coerceIn(0f, 1f)).toDouble() else 0.5

    Column(Modifier.fillMaxSize().background(Ink).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(job.emoji, fontSize = 26.sp); Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Camionero: parking inverso", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap FRENAR cuando el camión esté sobre el slot.", color = Dim, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Entrega", color = Dim, fontSize = 10.sp)
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

        Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF263238))) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                drawRect(Color(0xFF66BB6A), Offset(w * slotX - w * 0.04f, h * 0.20f), Size(w * 0.08f, h * 0.60f))
                drawRect(Color(0xFFFFB74D), Offset(w * truckX - w * 0.06f, h * 0.30f), Size(w * 0.12f, h * 0.40f))
            }
            Text("🚚", fontSize = 32.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = (truckX * 280).dp))
        }
        lastResult?.let {
            val (lbl, c) = when (it) { "green" -> "🌟 Aparcado perfecto (+3)" to Emerald; "yellow" -> "👍 Aceptable (+1)" to Color(0xFFFFB74D); else -> "💥 Te pasaste (0)" to Color(0xFFFF7A7A) }
            Text(lbl, color = c, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        }
        Spacer(Modifier.weight(1f))
        if (done) Button({ onFinish(scoreMul) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
            Text("Cobrar y volver", color = Color.Black, fontWeight = FontWeight.Bold)
        } else Row(Modifier.fillMaxWidth()) {
            TextButton({ onCancel() }, Modifier.weight(0.4f)) { Text("Cancelar", color = Dim, fontSize = 12.sp) }
            Button({ tap() }, enabled = !hold, modifier = Modifier.weight(0.6f),
                colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
                Text("🛑 FRENAR", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
