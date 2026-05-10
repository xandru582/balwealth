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
 * Mini-juego del Dentista — limpia muelas con caries.
 *
 * Mecánica:
 *  - Fila de 8 muelas. Estado por muela: SANA / CARIES / NEGRA.
 *  - Cada ~700ms aparece una caries en una muela SANA random.
 *  - Una muela CARIES sin limpiar 2.5s se vuelve NEGRA (perdida, -1 score).
 *  - Tap muela CARIES → limpia (vuelve a SANA), +1 score.
 *  - Tap muela SANA → -1 score (perdiste tiempo).
 *  - Tap muela NEGRA → no hace nada.
 *  - 30s. Score teórico ~22 → scoreMul.
 */
private enum class ToothState { SANA, CARIES, NEGRA }
private data class Tooth(val state: ToothState, val cariesAtMs: Long = 0L)

@Composable
fun DentistJobScreen(
    state: GameState,
    onFinish: (Double) -> Unit,
    onCancel: () -> Unit
) {
    val job = JobId.DENTIST
    val totalSeconds = 30
    val toothCount = 8
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }

    var teeth by remember { mutableStateOf(List(toothCount) { Tooth(ToothState.SANA) }) }
    var secondsLeft by remember { mutableStateOf(totalSeconds) }
    var score by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && !done) { delay(1000L); secondsLeft -= 1 }
        done = true
    }
    // Spawner — caries cada 700ms
    LaunchedEffect(Unit) {
        while (!done) {
            delay(700L)
            val sanas = teeth.mapIndexedNotNull { i, t -> if (t.state == ToothState.SANA) i else null }
            if (sanas.isNotEmpty()) {
                val pick = sanas[rng.nextInt(sanas.size)]
                teeth = teeth.toMutableList().also {
                    it[pick] = Tooth(ToothState.CARIES, System.currentTimeMillis())
                }
            }
        }
    }
    // Burn-out — caries → negra tras 2.5s
    LaunchedEffect(Unit) {
        while (!done) {
            delay(150L)
            val now = System.currentTimeMillis()
            val newList = teeth.toMutableList()
            var burned = 0
            for (i in newList.indices) {
                val t = newList[i]
                if (t.state == ToothState.CARIES && now - t.cariesAtMs >= 2500L) {
                    newList[i] = Tooth(ToothState.NEGRA)
                    burned += 1
                }
            }
            if (burned > 0) {
                teeth = newList
                score -= burned
            }
        }
    }

    fun tap(idx: Int) {
        if (done) return
        val t = teeth[idx]
        when (t.state) {
            ToothState.CARIES -> {
                teeth = teeth.toMutableList().also { it[idx] = Tooth(ToothState.SANA) }
                score += 1
            }
            ToothState.SANA -> { score -= 1 }
            ToothState.NEGRA -> { /* nada */ }
        }
    }

    val maxScore = 22
    val scoreMul = if (done) (0.5 + (score.coerceAtLeast(0).toFloat() / maxScore).coerceIn(0f, 1f)).toDouble() else 0.5

    Column(Modifier.fillMaxSize().background(Ink).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(job.emoji, fontSize = 26.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Dentista: limpia caries antes de que se pudran", fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap muela amarilla = limpiar. Tap muela sana = -1.", color = Dim, fontSize = 11.sp)
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
                    Text("Limpiezas", color = Dim, fontSize = 10.sp)
                    Text("$score / $maxScore",
                        color = if (score >= maxScore / 2) Emerald else Paper,
                        fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                val negras = teeth.count { it.state == ToothState.NEGRA }
                Column(Modifier.weight(1f)) {
                    Text("Perdidas", color = Dim, fontSize = 10.sp)
                    Text("$negras / $toothCount",
                        color = if (negras > 2) Color(0xFFFF7A7A) else Paper,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
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

        // Boca: 8 muelas en una fila
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFFFCDD2)).padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (i in 0 until toothCount) {
                    val t = teeth[i]
                    val (bg, emoji) = when (t.state) {
                        ToothState.SANA -> Color(0xFFFFFFFF) to "🦷"
                        ToothState.CARIES -> Color(0xFFFFD54F) to "🦷"
                        ToothState.NEGRA -> Color(0xFF424242) to "💀"
                    }
                    Box(
                        Modifier.weight(1f).padding(2.dp).aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .clickable { tap(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 26.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "🦷 Sana · 🦷 con fondo amarillo = caries (tap rápido) · 💀 perdida.",
            color = Dim, fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

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
