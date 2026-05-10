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
 * Mini-juego del Heladero — sirve bolas con timing perfecto.
 *
 * Mecánica:
 *  - Cada bola tiene una "barra de servida" 0..100% que avanza en 1.5s.
 *  - Zona perfecta: 70..85%. Aceptable: 50..70 / 85..92. Mal: <50 ó >92.
 *  - Tap "SERVIR" decide. perfecto = +3, aceptable = +1, mal = 0.
 *  - 30s. Score teórico ~30 → scoreMul.
 */
@Composable
fun IceCreamJobScreen(
    state: GameState,
    onFinish: (Double) -> Unit,
    onCancel: () -> Unit
) {
    val job = JobId.ICE_CREAM_SELLER
    val totalSeconds = 30
    val previewBaseWage = JobsEngine.previewWage(state, job)

    var secondsLeft by remember { mutableStateOf(totalSeconds) }
    var progress by remember { mutableStateOf(0f) }
    var score by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var holdInput by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<String?>(null) }
    var ballsServed by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && !done) { delay(1000L); secondsLeft -= 1 }
        done = true
    }
    // Bola que avanza
    LaunchedEffect(ballsServed, done) {
        if (done) return@LaunchedEffect
        progress = 0f
        lastResult = null
        holdInput = false
        while (!done && progress < 1f) {
            delay(16L)
            progress += 0.011f  // ~1.5s para llegar a 1.0
        }
        if (!done && progress >= 1f) {
            holdInput = true
            lastResult = "red"
            delay(400L)
            ballsServed += 1
        }
    }
    LaunchedEffect(holdInput, lastResult, done) {
        if (holdInput && !done && lastResult != null && progress < 1f) {
            delay(350L)
            ballsServed += 1
        }
    }

    fun handleTap() {
        if (holdInput || done) return
        val pct = (progress * 100).toInt().coerceIn(0, 100)
        val (gain, color) = when {
            pct in 70..85 -> 3 to "green"
            pct in 50..69 -> 1 to "yellow"
            pct in 86..92 -> 1 to "yellow"
            else -> 0 to "red"
        }
        score += gain
        lastResult = color
        holdInput = true
    }

    val maxScore = 30
    val scoreMul = if (done) (0.5 + (score.coerceAtLeast(0).toFloat() / maxScore).coerceIn(0f, 1f)).toDouble() else 0.5

    Column(Modifier.fillMaxSize().background(Ink).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(job.emoji, fontSize = 26.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Heladero: sirve con timing", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap SERVIR cuando la barra esté en zona verde",
                    color = Dim, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Tiempo", color = Dim, fontSize = 10.sp)
                Text("${secondsLeft}s", color = Sapphire, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(progress = { secondsLeft.toFloat() / totalSeconds.toFloat() },
            modifier = Modifier.fillMaxWidth().height(4.dp))
        Spacer(Modifier.height(8.dp))

        EmpireCard(borderColor = Sapphire) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Score", color = Dim, fontSize = 10.sp)
                    Text("$score / $maxScore",
                        color = if (score >= maxScore / 2) Emerald else Paper,
                        fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Bolas: $ballsServed", color = Dim, fontSize = 10.sp)
                }
                val previewWin = previewBaseWage * scoreMul
                Column(horizontalAlignment = Alignment.End) {
                    Text("Wage previsto", color = Dim, fontSize = 10.sp)
                    Text(previewWin.fmtMoney(), color = if (scoreMul >= 1.10) Emerald else Gold,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("×${"%.2f".format(scoreMul)}", color = Dim, fontSize = 10.sp)
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        // Cucurucho visual
        Text("🍦", fontSize = 64.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))

        // Barra de servida
        Box(
            Modifier.fillMaxWidth().height(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF132030))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val padX = 8f
                val barW = w - padX * 2
                fun zone(from: Float, to: Float, color: Color) {
                    drawRect(color = color,
                        topLeft = Offset(padX + barW * from, h * 0.30f),
                        size = Size(barW * (to - from), h * 0.40f))
                }
                drawRect(color = Color(0xFF263444),
                    topLeft = Offset(padX, h * 0.30f),
                    size = Size(barW, h * 0.40f))
                zone(0f, 0.50f, Color(0x66FF5252))
                zone(0.50f, 0.70f, Color(0x66FFB74D))
                zone(0.70f, 0.85f, Color(0x6666BB6A))
                zone(0.85f, 0.92f, Color(0x66FFB74D))
                zone(0.92f, 1.00f, Color(0x66FF5252))
                val indX = padX + barW * progress.coerceIn(0f, 1f)
                drawRect(color = Color.White,
                    topLeft = Offset(indX - 2f, h * 0.20f),
                    size = Size(4f, h * 0.60f))
            }
        }
        Text("Servida: ${(progress * 100).toInt()}%",
            color = Paper, fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        lastResult?.let { res ->
            val (label, c) = when (res) {
                "green" -> "🌟 ¡Cono perfecto! (+3)" to Emerald
                "yellow" -> "👍 Bien (+1)" to Color(0xFFFFB74D)
                else -> "💧 Derretido (0)" to Color(0xFFFF7A7A)
            }
            Text(label, color = c, fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }
        Spacer(Modifier.weight(1f))

        if (done) {
            Button(onClick = { onFinish(scoreMul) }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
                Text("Cobrar y volver al hub", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(0.4f)) {
                    Text("Cancelar", color = Dim, fontSize = 12.sp)
                }
                Button(onClick = { handleTap() }, enabled = !holdInput,
                    modifier = Modifier.weight(0.6f),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
                    Text("🍨 SERVIR", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text("🟢 70–85% perfecto · 🟠 50–70 / 85–92 ok · 🔴 fuera = 0",
            color = Dim, fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
    }
}
