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
 * Electricista — fusibles en cortocircuito.
 *
 * Mecánica:
 *  - Panel con 6 fusibles. Cada ronda uno parpadea (cortocircuito).
 *    Tap el correcto en <1.5s. Correcto = +1, mal o timeout = -1.
 *  - 30s, score teórico ~18.
 */
@Composable
fun ElectricianJobScreen(state: GameState, onFinish: (Double) -> Unit, onCancel: () -> Unit) {
    val job = JobId.ELECTRICIAN
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }
    val n = 6
    val roundDurMs = 1500L
    var brokenIdx by remember { mutableStateOf(rng.nextInt(n)) }
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
        if (feedback != null) { delay(280L); brokenIdx = rng.nextInt(n); roundStart = System.currentTimeMillis(); feedback = null }
    }
    fun tap(i: Int) {
        if (done || feedback != null) return
        if (i == brokenIdx) { score += 1; feedback = "ok" } else { score -= 1; feedback = "bad" }
    }

    val maxScore = 18
    val scoreMul = if (done) (0.5 + (score.coerceAtLeast(0).toFloat() / maxScore).coerceIn(0f, 1f)).toDouble() else 0.5
    val pctTime = ((roundDurMs - (nowMs - roundStart)).coerceAtLeast(0L) / roundDurMs.toFloat()).coerceIn(0f, 1f)

    Column(Modifier.fillMaxSize().background(Ink).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(job.emoji, fontSize = 26.sp); Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Electricista: panel de fusibles", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap el fusible que parpadea ⚠️.", color = Dim, fontSize = 11.sp)
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
                    Text("Reparados", color = Dim, fontSize = 10.sp)
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

        // Grid 3x2 de fusibles
        for (row in 0 until 2) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 3) {
                    val i = row * 3 + col
                    val isBroken = i == brokenIdx
                    val highlightOk = feedback == "ok" && isBroken
                    val highlightBad = feedback == "bad" && isBroken
                    Box(Modifier.weight(1f).padding(6.dp).aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                        .background(when { highlightOk -> Color(0xFF66BB6A); highlightBad -> Color(0xFFFFB74D); isBroken && feedback == null -> Color(0xFFFFB74D); else -> Color(0xFF1E3245) })
                        .clickable { tap(i) }, contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (isBroken && feedback == null) "⚠️" else "🔌", fontSize = 36.sp)
                            Text("Fusible ${i + 1}", color = Paper, fontSize = 11.sp)
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
