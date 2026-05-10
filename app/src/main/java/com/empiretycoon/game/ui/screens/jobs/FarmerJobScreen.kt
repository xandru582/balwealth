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
 * Mini-juego del Granjero — rhythm de cosecha.
 *
 * Mecánica:
 *  - Cada ronda aparece una indicación: SEMBRAR 🌱 / REGAR 💧 / COSECHAR 🌾.
 *  - El jugador tap el botón correspondiente antes de 1.2s.
 *  - Correcto = +1 score. Error o timeout = -1.
 *  - 30s. Score teórico ~18 → scoreMul.
 */
private enum class FarmAction(val emoji: String, val label: String) {
    SEED("🌱", "SEMBRAR"),
    WATER("💧", "REGAR"),
    HARVEST("🌾", "COSECHAR")
}

private data class FarmRound(val expected: FarmAction, val startedAtMs: Long)

@Composable
fun FarmerJobScreen(
    state: GameState,
    onFinish: (Double) -> Unit,
    onCancel: () -> Unit
) {
    val job = JobId.FARMER
    val totalSeconds = 30
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }
    val roundDurMs = 1200L

    fun newRound(): FarmRound = FarmRound(
        expected = FarmAction.values().random(rng),
        startedAtMs = System.currentTimeMillis()
    )

    var round by remember { mutableStateOf(newRound()) }
    var secondsLeft by remember { mutableStateOf(totalSeconds) }
    var score by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var feedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && !done) { delay(1000L); secondsLeft -= 1 }
        done = true
    }
    LaunchedEffect(Unit) {
        while (!done) {
            delay(60L)
            nowMs = System.currentTimeMillis()
            if (nowMs - round.startedAtMs >= roundDurMs && feedback == null) {
                feedback = "timeout"; score -= 1
            }
        }
    }
    LaunchedEffect(feedback) {
        if (feedback != null) { delay(220L); round = newRound(); feedback = null }
    }

    fun tap(action: FarmAction) {
        if (done || feedback != null) return
        if (action == round.expected) { score += 1; feedback = "ok" }
        else { score -= 1; feedback = "bad" }
    }

    val maxScore = 18
    val scoreMul = if (done) (0.5 + (score.coerceAtLeast(0).toFloat() / maxScore).coerceIn(0f, 1f)).toDouble() else 0.5
    val pctTime = ((roundDurMs - (nowMs - round.startedAtMs)).coerceAtLeast(0L) / roundDurMs.toFloat()).coerceIn(0f, 1f)

    Column(Modifier.fillMaxSize().background(Ink).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(job.emoji, fontSize = 26.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Granjero: rhythm cosecha", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap el botón que coincida con la acción pedida.", color = Dim, fontSize = 11.sp)
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
        Spacer(Modifier.height(8.dp))

        LinearProgressIndicator(progress = { pctTime },
            color = if (pctTime > 0.5f) Emerald else if (pctTime > 0.25f) Color(0xFFFFB74D) else Color(0xFFFF7A7A),
            modifier = Modifier.fillMaxWidth().height(6.dp))
        Spacer(Modifier.height(20.dp))

        EmpireCard(borderColor = when (feedback) {
            "ok" -> Emerald; "bad", "timeout" -> Color(0xFFFF7A7A); else -> Gold
        }) {
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Acción ahora:", color = Dim, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text(round.expected.emoji, fontSize = 64.sp)
                Text(round.expected.label, color = Gold, fontSize = 18.sp, fontWeight = FontWeight.Black)
                feedback?.let { fb ->
                    val (label, c) = when (fb) {
                        "ok" -> "✅" to Emerald
                        "bad" -> "❌" to Color(0xFFFF7A7A)
                        else -> "⏰" to Color(0xFFFF7A7A)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(label, color = c, fontSize = 22.sp)
                }
            }
        }
        Spacer(Modifier.weight(1f))

        // Botones de las 3 acciones
        Row(modifier = Modifier.fillMaxWidth()) {
            for (action in FarmAction.values()) {
                Box(
                    Modifier.weight(1f).padding(4.dp).height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(InkBorder)
                        .clickable { tap(action) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(action.emoji, fontSize = 32.sp)
                        Text(action.label, color = Paper, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))

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
