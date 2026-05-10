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
 * Médico (cirujano) — precision-tap sobre puntos del paciente.
 *
 * Mecánica:
 *  - Aparece un punto rojo que parpadea en posición random sobre el
 *    paciente (📐 silueta).
 *  - El punto se mueve cada 700ms a otra posición.
 *  - Tap el punto rojo cuando está visible = +1 score.
 *  - Tap fuera del punto (en silueta vacía) = -1.
 *  - 30s, score teórico ~30.
 */
@Composable
fun DoctorJobScreen(state: GameState, onFinish: (Double) -> Unit, onCancel: () -> Unit) {
    val job = JobId.DOCTOR
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }
    var pointPos by remember { mutableStateOf(rng.nextInt(9)) }
    var visible by remember { mutableStateOf(true) }
    var secondsLeft by remember { mutableStateOf(30) }
    var score by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && !done) { delay(1000L); secondsLeft -= 1 }
        done = true
    }
    LaunchedEffect(Unit) {
        while (!done) {
            delay(700L)
            visible = !visible
            if (visible) pointPos = rng.nextInt(9)
        }
    }
    fun tap(idx: Int) {
        if (done) return
        if (visible && idx == pointPos) {
            score += 1; visible = false; pointPos = rng.nextInt(9)
        } else {
            score -= 1
        }
    }

    val maxScore = 30
    val scoreMul = if (done) (0.5 + (score.coerceAtLeast(0).toFloat() / maxScore).coerceIn(0f, 1f)).toDouble() else 0.5

    Column(Modifier.fillMaxSize().background(Ink).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(job.emoji, fontSize = 26.sp); Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Médico: cirugía precisa", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap los puntos rojos. NO toques cuando no haya nada.", color = Dim, fontSize = 11.sp)
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
                    Text("Cirugías", color = Dim, fontSize = 10.sp)
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

        Text("👤 Paciente", color = Dim, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
        // Silueta del paciente (3x3 grid de zonas anatómicas)
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)).background(Color(0xFFFFCCBC))) {
            Column(Modifier.fillMaxSize()) {
                for (row in 0 until 3) {
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        for (col in 0 until 3) {
                            val i = row * 3 + col
                            val isPoint = visible && i == pointPos
                            Box(Modifier.weight(1f).fillMaxHeight().padding(8.dp).clip(RoundedCornerShape(50)).background(if (isPoint) Color(0xFFE53935) else Color.Transparent).clickable { tap(i) },
                                contentAlignment = Alignment.Center) {
                                if (isPoint) Text("⛔", fontSize = 24.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        if (done) Button({ onFinish(scoreMul) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
            Text("Cobrar y volver", color = Color.Black, fontWeight = FontWeight.Bold)
        } else Row(Modifier.fillMaxWidth()) {
            TextButton({ onCancel() }, Modifier.weight(1f)) { Text("Cancelar", color = Dim) }
            Spacer(Modifier.width(8.dp))
            Button({ done = true }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB85C5C))) { Text("Plantarse", color = Color.White) }
        }
    }
}
