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
import kotlin.math.abs
import kotlin.random.Random

/**
 * Mini-juego del Taxista — pickup + dropoff en grid 6×4.
 *
 * Mecánica:
 *  - Tu taxi 🚕 empieza en (0,0).
 *  - Hay un solo "cliente activo" a la vez, con un tile de pickup y otro
 *    de dropoff (siempre distintos al taxi).
 *  - Tap en celda ADYACENTE = mueves el taxi una celda (4 direcciones).
 *  - Al pisar pickup, cliente se sube (visualmente el tile cambia).
 *  - Al pisar dropoff, +1 score y nuevo cliente generado.
 *  - 30s totales. Score = trayectos completados. Score teórico ~12 →
 *    scoreMul 0.5..1.5.
 */
private data class TaxiState(
    val taxiX: Int,
    val taxiY: Int,
    val pickup: Pair<Int, Int>,
    val dropoff: Pair<Int, Int>,
    val passengerOnboard: Boolean
)

@Composable
fun TaxiJobScreen(
    state: GameState,
    onFinish: (Double) -> Unit,
    onCancel: () -> Unit
) {
    val job = JobId.TAXI_DRIVER
    val totalSeconds = 30
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }
    val gridW = 6
    val gridH = 4

    fun newClient(taxiPos: Pair<Int, Int>): TaxiState {
        var p: Pair<Int, Int>
        do { p = rng.nextInt(gridW) to rng.nextInt(gridH) } while (p == taxiPos)
        var d: Pair<Int, Int>
        do { d = rng.nextInt(gridW) to rng.nextInt(gridH) } while (d == p || d == taxiPos)
        return TaxiState(taxiPos.first, taxiPos.second, p, d, false)
    }

    var taxiSt by remember { mutableStateOf(newClient(0 to 0)) }
    var secondsLeft by remember { mutableStateOf(totalSeconds) }
    var score by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && !done) {
            delay(1000L)
            secondsLeft -= 1
        }
        done = true
    }

    fun moveTaxi(dx: Int, dy: Int) {
        if (done) return
        val nx = (taxiSt.taxiX + dx).coerceIn(0, gridW - 1)
        val ny = (taxiSt.taxiY + dy).coerceIn(0, gridH - 1)
        if (nx == taxiSt.taxiX && ny == taxiSt.taxiY) return
        var s = taxiSt.copy(taxiX = nx, taxiY = ny)
        // Pickup
        if (!s.passengerOnboard && (nx to ny) == s.pickup) {
            s = s.copy(passengerOnboard = true)
        }
        // Dropoff
        if (s.passengerOnboard && (nx to ny) == s.dropoff) {
            score += 1
            s = newClient(nx to ny)
        }
        taxiSt = s
    }

    val maxScore = 12
    val scoreMul = if (done) {
        (0.5 + (score.toFloat() / maxScore).coerceIn(0f, 1f)).toDouble()
    } else 0.5

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
                Text("Taxista: pickup + dropoff", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = Gold)
                Text("Recoge en 🟦 y deja en 🟧",
                    color = Dim, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Tiempo", color = Dim, fontSize = 10.sp)
                Text("${secondsLeft}s", color = Sapphire, fontSize = 22.sp,
                    fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { secondsLeft.toFloat() / totalSeconds.toFloat() },
            modifier = Modifier.fillMaxWidth().height(4.dp)
        )
        Spacer(Modifier.height(8.dp))

        EmpireCard(borderColor = Sapphire) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Trayectos", color = Dim, fontSize = 10.sp)
                    Text("$score / $maxScore",
                        color = if (score >= maxScore / 2) Emerald else Paper,
                        fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text("Pasajero", color = Dim, fontSize = 10.sp)
                    Text(if (taxiSt.passengerOnboard) "🧑 a bordo" else "—",
                        color = if (taxiSt.passengerOnboard) Emerald else Dim,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
        Spacer(Modifier.height(12.dp))

        // Grid
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(gridW.toFloat() / gridH.toFloat())
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF132030))
                .padding(4.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                for (row in 0 until gridH) {
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        for (col in 0 until gridW) {
                            val isTaxi = col == taxiSt.taxiX && row == taxiSt.taxiY
                            val isPickup = !taxiSt.passengerOnboard && (col to row) == taxiSt.pickup
                            val isDropoff = taxiSt.passengerOnboard && (col to row) == taxiSt.dropoff
                            // Distancia al taxi para detectar adyacencia
                            val dist = abs(col - taxiSt.taxiX) + abs(row - taxiSt.taxiY)
                            val adjacent = dist == 1
                            val (bg, content) = when {
                                isTaxi -> Color(0xFFFFD166) to "🚕"
                                isPickup -> Color(0xFF118AB2) to "🧑"
                                isDropoff -> Color(0xFFFF8A65) to "🏠"
                                adjacent -> Color(0xFF1E3245) to ""
                                else -> Color(0xFF152030) to ""
                            }
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(bg)
                                    .clickable(enabled = adjacent) {
                                        moveTaxi(col - taxiSt.taxiX, row - taxiSt.taxiY)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (content.isNotEmpty()) {
                                    Text(content, fontSize = 22.sp)
                                }
                            }
                        }
                    }
                }
            }
            if (done) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xCC000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🚕 Turno terminado",
                            color = Color.White, fontSize = 22.sp,
                            fontWeight = FontWeight.Black)
                        Text("Trayectos: $score / $maxScore",
                            color = Color.White, fontSize = 14.sp)
                        Text("Wage: ${(previewBaseWage * scoreMul).fmtMoney()}",
                            color = Gold, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Toca celdas adyacentes para mover el taxi.",
            color = Dim, fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
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
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar (sin energy ni cash)", color = Dim)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { done = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB85C5C))
                ) {
                    Text("Plantarse", color = Color.White)
                }
            }
        }
    }
}
