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

/**
 * Pizzero — estira la masa al tamaño perfecto.
 *
 * Mecánica:
 *  - 5 pizzas. Por cada una, una barra avanza 0..1.0 en ~1.6s.
 *  - Zona perfecta: 60..78 (+3), aceptable: 45..60 / 78..88 (+1), fuera: 0.
 *  - Tap "🍕 SACAR" decide. 30s, score teórico ~15.
 */
@Composable
fun PizzeriaJobScreen(state: GameState, onFinish: (Double) -> Unit, onCancel: () -> Unit) {
    val job = JobId.PIZZAIOLO
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val total = 5
    var idx by remember { mutableStateOf(0) }
    var progress by remember { mutableStateOf(0f) }
    var score by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<String?>(null) }
    var hold by remember { mutableStateOf(false) }

    LaunchedEffect(idx, done) {
        if (done) return@LaunchedEffect
        progress = 0f; lastResult = null; hold = false
        while (!done && progress < 1f) { delay(16L); progress += 0.013f }
        if (!done && progress >= 1f) {
            hold = true; lastResult = "red"; delay(500L); idx += 1; if (idx >= total) done = true
        }
    }
    LaunchedEffect(hold, lastResult, done) {
        if (hold && !done && lastResult != null && progress < 1f) {
            delay(450L); idx += 1; if (idx >= total) done = true
        }
    }
    fun tap() {
        if (hold || done) return
        val pct = (progress * 100).toInt()
        val (g, c) = when {
            pct in 60..78 -> 3 to "green"; pct in 45..59 -> 1 to "yellow"; pct in 79..88 -> 1 to "yellow"; else -> 0 to "red"
        }
        score += g; lastResult = c; hold = true
    }

    val maxScore = 15
    val scoreMul = if (done) (0.5 + (score.toFloat() / maxScore).coerceIn(0f, 1f)).toDouble() else 0.5

    Column(Modifier.fillMaxSize().background(Ink).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(job.emoji, fontSize = 26.sp); Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Pizzero: estira la masa al punto", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap SACAR cuando esté en zona verde.", color = Dim, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Pizza", color = Dim, fontSize = 10.sp)
                Text("${(idx + 1).coerceAtMost(total)}/$total", color = Sapphire, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }
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
        Spacer(Modifier.height(16.dp))
        Text("🍕", fontSize = 64.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF132030))) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height; val padX = 8f; val barW = w - padX * 2
                fun zone(from: Float, to: Float, color: Color) {
                    drawRect(color, Offset(padX + barW * from, h * 0.30f), Size(barW * (to - from), h * 0.40f))
                }
                drawRect(Color(0xFF263444), Offset(padX, h * 0.30f), Size(barW, h * 0.40f))
                zone(0f, 0.45f, Color(0x66FF5252)); zone(0.45f, 0.60f, Color(0x66FFB74D))
                zone(0.60f, 0.78f, Color(0x6666BB6A)); zone(0.78f, 0.88f, Color(0x66FFB74D))
                zone(0.88f, 1.00f, Color(0x66FF5252))
                val indX = padX + barW * progress.coerceIn(0f, 1f)
                drawRect(Color.White, Offset(indX - 2f, h * 0.20f), Size(4f, h * 0.60f))
            }
        }
        Text("Estirado: ${(progress * 100).toInt()}%", color = Paper, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        lastResult?.let {
            val (lbl, c) = when (it) { "green" -> "🌟 Pizza perfecta (+3)" to Emerald; "yellow" -> "👍 Aceptable (+1)" to Color(0xFFFFB74D); else -> "💀 Quemada/Cruda" to Color(0xFFFF7A7A) }
            Text(lbl, color = c, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        }
        Spacer(Modifier.weight(1f))
        if (done) Button({ onFinish(scoreMul) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
            Text("Cobrar y volver", color = Color.Black, fontWeight = FontWeight.Bold)
        } else Row(Modifier.fillMaxWidth()) {
            TextButton({ onCancel() }, Modifier.weight(0.4f)) { Text("Cancelar", color = Dim, fontSize = 12.sp) }
            Button({ tap() }, enabled = !hold, modifier = Modifier.weight(0.6f),
                colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
                Text("🍕 SACAR DEL HORNO", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
