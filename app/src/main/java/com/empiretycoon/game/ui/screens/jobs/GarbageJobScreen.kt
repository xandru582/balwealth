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
 * Recolector de basura — vacía contenedores antes de que se desborden.
 *
 * Mecánica:
 *  - 6 contenedores en una fila. Cada uno empieza vacío (0%).
 *  - Cada 0.7s un contenedor random aumenta +20% (ruido suave).
 *  - Tap contenedor = vaciar (vuelve a 0%) y +1 score.
 *  - Si pasa 100% sin vaciar = se desborda, -1 score (vuelve a 50%).
 *  - 30s, score teórico ~25.
 */
@Composable
fun GarbageJobScreen(state: GameState, onFinish: (Double) -> Unit, onCancel: () -> Unit) {
    val job = JobId.GARBAGE_COLLECTOR
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val n = 6
    val rng = remember { Random(System.currentTimeMillis()) }
    var bins by remember { mutableStateOf(List(n) { 0f }) }
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
            val pick = rng.nextInt(n)
            bins = bins.toMutableList().also { it[pick] = (it[pick] + 0.20f).coerceAtMost(1.0f) }
        }
    }
    LaunchedEffect(Unit) {
        while (!done) {
            delay(200L)
            val newBins = bins.toMutableList()
            var overflowed = 0
            for (i in newBins.indices) {
                if (newBins[i] >= 1.0f) { newBins[i] = 0.5f; overflowed += 1 }
            }
            if (overflowed > 0) { bins = newBins; score -= overflowed }
        }
    }
    fun tap(i: Int) {
        if (done) return
        if (bins[i] > 0.05f) {
            score += 1
            bins = bins.toMutableList().also { it[i] = 0f }
        }
    }

    val maxScore = 25
    val scoreMul = if (done) (0.5 + (score.coerceAtLeast(0).toFloat() / maxScore).coerceIn(0f, 1f)).toDouble() else 0.5

    Column(Modifier.fillMaxSize().background(Ink).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(job.emoji, fontSize = 26.sp); Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Basurero: vacía contenedores", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap antes de que lleguen al 100%.", color = Dim, fontSize = 11.sp)
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
                    Text("Vaciados", color = Dim, fontSize = 10.sp)
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

        Row(Modifier.fillMaxWidth()) {
            for (i in 0 until n) {
                val fill = bins[i]
                val color = when {
                    fill >= 0.85f -> Color(0xFFFF5252); fill >= 0.6f -> Color(0xFFFFB74D); fill >= 0.3f -> Color(0xFFFFEB3B); else -> Color(0xFF66BB6A)
                }
                Column(modifier = Modifier.weight(1f).padding(4.dp).clickable { tap(i) },
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🗑️", fontSize = 32.sp)
                    Box(Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(6.dp)).background(InkBorder), contentAlignment = Alignment.BottomCenter) {
                        Box(Modifier.fillMaxWidth().fillMaxHeight(fill).background(color))
                    }
                    Text("${(fill * 100).toInt()}%", color = Paper, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
