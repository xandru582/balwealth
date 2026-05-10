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
import kotlin.random.Random

/**
 * Mini-juego del Cartero — encuentra los buzones correctos.
 *
 * Mecánica:
 *  - Grid 4×3 = 12 casas. 3 al azar tienen buzón con correo, las otras
 *    9 no.
 *  - Tap casa = toca puerta. Si tiene buzón → 📬 verde, +1 score. Si no
 *    → ❌ amarillo, -1 score.
 *  - Cuando se entregan los 3 → +2 bonus, nueva calle (ronda).
 *  - 30s. Score teórico ~10 → scoreMul.
 */
private data class PostScene(
    val targets: Set<Int>,
    val visited: Set<Int>,
    val deliveredCount: Int
) {
    val complete: Boolean get() = deliveredCount >= targets.size
}

@Composable
fun PostmanJobScreen(
    state: GameState,
    onFinish: (Double) -> Unit,
    onCancel: () -> Unit
) {
    val job = JobId.POSTMAN
    val totalSeconds = 30
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }
    val gridCols = 4
    val gridRows = 3

    fun newScene(): PostScene {
        val targets = mutableSetOf<Int>()
        while (targets.size < 3) targets.add(rng.nextInt(gridCols * gridRows))
        return PostScene(targets, emptySet(), 0)
    }

    var scene by remember { mutableStateOf(newScene()) }
    var secondsLeft by remember { mutableStateOf(totalSeconds) }
    var score by remember { mutableStateOf(0) }
    var streetsCompleted by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && !done) { delay(1000L); secondsLeft -= 1 }
        done = true
    }
    LaunchedEffect(scene.complete) {
        if (scene.complete && !done) {
            score += 2
            streetsCompleted += 1
            delay(700L)
            scene = newScene()
        }
    }

    fun tap(idx: Int) {
        if (done || scene.complete) return
        if (idx in scene.visited) return
        val newVisited = scene.visited + idx
        if (idx in scene.targets) {
            score += 1
            scene = scene.copy(visited = newVisited, deliveredCount = scene.deliveredCount + 1)
        } else {
            score -= 1
            scene = scene.copy(visited = newVisited)
        }
    }

    val maxScore = 10
    val scoreMul = if (done) (0.5 + (score.coerceAtLeast(0).toFloat() / maxScore).coerceIn(0f, 1f)).toDouble() else 0.5

    Column(Modifier.fillMaxSize().background(Ink).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(job.emoji, fontSize = 26.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Cartero: encuentra los 3 buzones", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap casa para tocar puerta. 📬 = correcto, ❌ = puerta cerrada.",
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
                    Text("Calles: $streetsCompleted · Buzones: ${scene.deliveredCount}/3",
                        color = Dim, fontSize = 10.sp)
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
        Spacer(Modifier.height(10.dp))

        // Calle (grid 4x3)
        Box(
            Modifier.fillMaxWidth().aspectRatio(gridCols.toFloat() / gridRows.toFloat())
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF132030)).padding(4.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                for (row in 0 until gridRows) {
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        for (col in 0 until gridCols) {
                            val idx = row * gridCols + col
                            val visited = idx in scene.visited
                            val isMail = idx in scene.targets
                            val (bg, content) = when {
                                visited && isMail -> Color(0xFF66BB6A) to "📬"
                                visited && !isMail -> Color(0xFF552B2B) to "❌"
                                else -> Color(0xFF1E3245) to "🏠"
                            }
                            Box(
                                Modifier.weight(1f).fillMaxHeight().padding(2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(bg)
                                    .clickable { tap(idx) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(content, fontSize = 26.sp)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Calle entera entregada = +2 bonus.", color = Dim, fontSize = 11.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.weight(1f))
        if (done) {
            Button(onClick = { onFinish(scoreMul) }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
                Text("Cobrar y volver al hub", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancelar (sin energy ni cash)", color = Dim)
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { done = true }, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB85C5C))) {
                    Text("Plantarse", color = Color.White)
                }
            }
        }
    }
}
