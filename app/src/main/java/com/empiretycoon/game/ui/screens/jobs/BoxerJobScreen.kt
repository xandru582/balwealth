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
 * Mini-juego del Boxeador — esquiva direccionales en rhythm.
 *
 * Mecánica:
 *  - Cada 900ms aparece un puñetazo entrante desde una dirección
 *    (UP / DOWN / LEFT / RIGHT). Tienes 1.4s para esquivar pulsando
 *    la dirección OPUESTA (un puñetazo desde la izquierda → tap RIGHT).
 *  - Tap correcto = +1 score, esquivado.
 *  - Tap incorrecto o timeout = -1 score, encajado.
 *  - Duración 30s. Score teórico ≈ 18 → scoreMul 0.5..1.5.
 */
private enum class PunchDir(val emoji: String, val opposite: PunchDir? = null) {
    UP("⬆️"), DOWN("⬇️"), LEFT("⬅️"), RIGHT("➡️");

    fun opposite(): PunchDir = when (this) {
        UP -> DOWN; DOWN -> UP; LEFT -> RIGHT; RIGHT -> LEFT
    }
}

private data class PunchRound(
    val from: PunchDir,
    val expectedDodge: PunchDir,
    val startedAtMs: Long
)

@Composable
fun BoxerJobScreen(
    state: GameState,
    onFinish: (Double) -> Unit,
    onCancel: () -> Unit
) {
    val job = JobId.BOXER
    val totalSeconds = 30
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }
    val roundDurMs = 1400L

    fun newRound(): PunchRound {
        val from = PunchDir.values().random(rng)
        return PunchRound(from, from.opposite(), System.currentTimeMillis())
    }

    var round by remember { mutableStateOf(newRound()) }
    var secondsLeft by remember { mutableStateOf(totalSeconds) }
    var score by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var feedback by remember { mutableStateOf<String?>(null) }  // "ok"/"bad"/"timeout"

    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && !done) {
            delay(1000L)
            secondsLeft -= 1
        }
        done = true
    }
    LaunchedEffect(Unit) {
        while (!done) {
            delay(60L)
            nowMs = System.currentTimeMillis()
            if (nowMs - round.startedAtMs >= roundDurMs && feedback == null) {
                feedback = "timeout"
                score -= 1
            }
        }
    }
    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(280L)
            round = newRound()
            feedback = null
        }
    }

    fun tap(dir: PunchDir) {
        if (done || feedback != null) return
        if (dir == round.expectedDodge) {
            score += 1
            feedback = "ok"
        } else {
            score -= 1
            feedback = "bad"
        }
    }

    val maxScore = 18
    val scoreMul = if (done) {
        (0.5 + (score.coerceAtLeast(0).toFloat() / maxScore).coerceIn(0f, 1f)).toDouble()
    } else 0.5

    val pctTime = ((roundDurMs - (nowMs - round.startedAtMs)).coerceAtLeast(0L) / roundDurMs.toFloat()).coerceIn(0f, 1f)

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
                Text("Boxeador: esquiva los directos", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = Gold)
                Text("Pulsa la dirección OPUESTA al puñetazo entrante",
                    color = Dim, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Tiempo total", color = Dim, fontSize = 10.sp)
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
                    Text("Score", color = Dim, fontSize = 10.sp)
                    Text("$score / $maxScore",
                        color = if (score >= maxScore / 2) Emerald else Paper,
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
        Spacer(Modifier.height(8.dp))

        // Round timer
        LinearProgressIndicator(
            progress = { pctTime },
            color = when {
                pctTime > 0.5f -> Emerald
                pctTime > 0.25f -> Color(0xFFFFB74D)
                else -> Color(0xFFFF7A7A)
            },
            modifier = Modifier.fillMaxWidth().height(6.dp)
        )
        Spacer(Modifier.height(16.dp))

        // Indicador del puñetazo entrante
        EmpireCard(borderColor = when (feedback) {
            "ok" -> Emerald
            "bad", "timeout" -> Color(0xFFFF7A7A)
            else -> Gold
        }) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("PUÑETAZO ENTRANTE DESDE",
                    color = Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(round.from.emoji, fontSize = 56.sp)
                Spacer(Modifier.height(2.dp))
                Text("Esquiva pulsando ${round.expectedDodge.emoji}",
                    color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                feedback?.let { fb ->
                    val (label, c) = when (fb) {
                        "ok" -> "🌟 ¡Esquiva limpia!" to Emerald
                        "bad" -> "❌ Te lo comiste" to Color(0xFFFF7A7A)
                        else -> "⏰ Tarde..." to Color(0xFFFF7A7A)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(label, color = c, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Cruz direccional
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DirBtn(PunchDir.UP) { tap(PunchDir.UP) }
            Row {
                DirBtn(PunchDir.LEFT) { tap(PunchDir.LEFT) }
                Spacer(Modifier.size(72.dp))
                DirBtn(PunchDir.RIGHT) { tap(PunchDir.RIGHT) }
            }
            DirBtn(PunchDir.DOWN) { tap(PunchDir.DOWN) }
        }
        Spacer(Modifier.height(10.dp))

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

@Composable
private fun DirBtn(dir: PunchDir, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(4.dp)
            .size(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(InkBorder)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(dir.emoji, fontSize = 30.sp)
    }
}
