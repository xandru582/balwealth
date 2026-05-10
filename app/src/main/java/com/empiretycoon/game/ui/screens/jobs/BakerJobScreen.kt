package com.empiretycoon.game.ui.screens.jobs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
 * Mini-juego del Panadero: timing del horno.
 *
 * Mecánica:
 *  - 5 panes consecutivos.
 *  - Por cada pan, una barra de "tueste" llena de 0% a 100% en ~2.0–2.4s
 *    (velocidad ligeramente aleatoria por pan para no ser memorizable).
 *  - Tu objetivo es tocar "Sacar" cuando el indicador esté en la zona
 *    verde (65–85% del tueste). Fuera de eso, se cuenta como peor:
 *      green   65..85  → +3 (perfecto)
 *      yellow  50..65 / 85..92  → +1 (aceptable)
 *      red     <50 / >92         → 0  (crudo o quemado)
 *  - Si el indicador llega al 100% sin tap, se marca como rojo.
 *  - Score máximo 15. scoreMul = 0.5 + (score/15)·1.0 → 0.5..1.5.
 */
@Composable
fun BakerJobScreen(
    state: GameState,
    onFinish: (Double) -> Unit,
    onCancel: () -> Unit
) {
    val job = JobId.BAKER
    val totalLoaves = 5
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }

    var currentLoaf by remember { mutableStateOf(0) }
    var bakeProgress by remember { mutableStateOf(0f) }    // 0..1
    var loafSpeed by remember { mutableStateOf(0.012f) }   // increment por tick (16ms)
    var score by remember { mutableStateOf(0) }
    var lastResult by remember { mutableStateOf<String?>(null) }  // "green"/"yellow"/"red"
    var done by remember { mutableStateOf(false) }
    var holdInput by remember { mutableStateOf(false) }    // bloquea tap mientras avanza nuevo pan

    // Loop del horno: avanza el % cada 16ms.
    LaunchedEffect(currentLoaf, done) {
        if (done) return@LaunchedEffect
        // Inicializa el siguiente pan
        bakeProgress = 0f
        // Velocidad random ligeramente diferente cada pan (entre 0.010 y 0.015 por tick).
        loafSpeed = 0.010f + rng.nextFloat() * 0.005f
        lastResult = null
        holdInput = false
        while (!done && currentLoaf < totalLoaves && bakeProgress < 1f) {
            delay(16L)
            bakeProgress += loafSpeed
        }
        // Si llegó a 100% sin tap, registramos rojo.
        if (!done && currentLoaf < totalLoaves && bakeProgress >= 1f) {
            holdInput = true
            lastResult = "red"
            delay(700L)
            currentLoaf += 1
            if (currentLoaf >= totalLoaves) done = true
        }
    }

    val scoreMul = if (done) {
        (0.5 + (score.toFloat() / 15f).coerceIn(0f, 1f)).toDouble()
    } else 0.5

    fun handleTap() {
        if (holdInput || done) return
        val pct = (bakeProgress * 100).toInt().coerceIn(0, 100)
        val (gain, color) = when {
            pct in 65..85 -> 3 to "green"
            pct in 50..64 -> 1 to "yellow"
            pct in 86..92 -> 1 to "yellow"
            else -> 0 to "red"
        }
        score += gain
        lastResult = color
        holdInput = true
        // Avanza al siguiente pan tras 500ms.
    }

    // Después de tap (holdInput=true), avanza al siguiente pan.
    LaunchedEffect(holdInput, lastResult, done) {
        if (holdInput && !done && lastResult != null && bakeProgress < 1f) {
            // Tap manual antes de fin natural — esperamos un momento y avanzamos.
            delay(500L)
            currentLoaf += 1
            if (currentLoaf >= totalLoaves) done = true
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(job.emoji, fontSize = 26.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Panadero: hornea 5 panes", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = Gold)
                Text("Pulsa SACAR cuando el tueste esté en zona verde",
                    color = Dim, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Pan", color = Dim, fontSize = 10.sp)
                Text("${(currentLoaf + 1).coerceAtMost(totalLoaves)} / $totalLoaves",
                    color = Sapphire, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(8.dp))

        // Score + wage preview
        EmpireCard(borderColor = Sapphire) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Score", color = Dim, fontSize = 10.sp)
                    Text("$score / 15",
                        color = if (score >= 8) Emerald else Paper,
                        fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                val previewWin = previewBaseWage * scoreMul
                Column(horizontalAlignment = Alignment.End) {
                    Text("Wage previsto", color = Dim, fontSize = 10.sp)
                    Text(previewWin.fmtMoney(),
                        color = if (scoreMul >= 1.10) Emerald else Gold,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("×${"%.2f".format(scoreMul)}", color = Dim, fontSize = 10.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Barra de tueste
        Box(
            Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF132030))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val padX = 8f
                // Banda de fondo gris
                drawRect(
                    color = Color(0xFF263444),
                    topLeft = Offset(padX, h * 0.30f),
                    size = Size(w - padX * 2, h * 0.40f)
                )
                // Zonas: red 0-50, yellow 50-65, green 65-85, yellow 85-92, red 92-100
                val barW = w - padX * 2
                fun drawZone(from: Float, to: Float, color: Color) {
                    drawRect(
                        color = color,
                        topLeft = Offset(padX + barW * from, h * 0.30f),
                        size = Size(barW * (to - from), h * 0.40f)
                    )
                }
                drawZone(0f, 0.50f, Color(0x66FF5252))
                drawZone(0.50f, 0.65f, Color(0x66FFB74D))
                drawZone(0.65f, 0.85f, Color(0x6666BB6A))
                drawZone(0.85f, 0.92f, Color(0x66FFB74D))
                drawZone(0.92f, 1.00f, Color(0x66FF5252))

                // Indicador (línea blanca)
                val indX = padX + barW * bakeProgress.coerceIn(0f, 1f)
                drawRect(
                    color = Color.White,
                    topLeft = Offset(indX - 2f, h * 0.20f),
                    size = Size(4f, h * 0.60f)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Tueste: ${(bakeProgress * 100).toInt()}%",
            color = Paper, fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        lastResult?.let { res ->
            val (label, color) = when (res) {
                "green" -> "🌟 Perfecto (+3)" to Emerald
                "yellow" -> "👍 Aceptable (+1)" to Color(0xFFFFB74D)
                else -> "💀 Quemado/Crudo (0)" to Color(0xFFFF7A7A)
            }
            Text(label, color = color, fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        }
        Spacer(Modifier.weight(1f))

        if (done) {
            Button(
                onClick = { onFinish(scoreMul) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Gold)
            ) {
                Text("Cobrar y volver al hub",
                    color = Color.Black, fontWeight = FontWeight.Bold)
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(0.4f)
                ) {
                    Text("Cancelar", color = Dim, fontSize = 12.sp)
                }
                Button(
                    onClick = { handleTap() },
                    enabled = !holdInput,
                    modifier = Modifier.weight(0.6f),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) {
                    Text("🥖 SACAR DEL HORNO",
                        color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(
            "🟢 65–85% perfecto · 🟠 50–65 / 85–92 ok · 🔴 fuera = 0",
            color = Dim, fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        )
    }
}
