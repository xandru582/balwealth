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
 * Mini-juego del Pescador — agotando peces.
 *
 * Mecánica simplificada:
 *  - Cada ronda hay un pez con `fishStamina` 100.
 *  - Cada 600ms baseline el pez recupera +5 stamina.
 *  - Tap "TIRAR" → -10 stamina inmediato.
 *  - Cuando stamina llega a 0 → +1 score, nuevo pez.
 *  - Cada ronda dura máximo 10s. Si timer expira sin haberlo agotado,
 *    el pez escapa: -1 score, nuevo pez.
 *  - Duración 30s totales. Score teórico ≈ 5 → scoreMul 0.5..1.5.
 */
private data class FishRound(
    val maxStamina: Int = 100,
    val stamina: Int = 100,
    val startedAtMs: Long
)

@Composable
fun FishermanJobScreen(
    state: GameState,
    onFinish: (Double) -> Unit,
    onCancel: () -> Unit
) {
    val job = JobId.FISHERMAN
    val totalSeconds = 30
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }
    val roundDurMs = 10_000L

    fun newRound(): FishRound = FishRound(startedAtMs = System.currentTimeMillis())

    var round by remember { mutableStateOf(newRound()) }
    var secondsLeft by remember { mutableStateOf(totalSeconds) }
    var score by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var feedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && !done) {
            delay(1000L)
            secondsLeft -= 1
        }
        done = true
    }
    // Recovery del pez baseline
    LaunchedEffect(round.startedAtMs) {
        while (!done) {
            delay(600L)
            if (feedback != null) continue
            round = round.copy(
                stamina = (round.stamina + 5).coerceAtMost(round.maxStamina)
            )
        }
    }
    // Now ms tracker
    LaunchedEffect(Unit) {
        while (!done) {
            delay(80L)
            nowMs = System.currentTimeMillis()
            if (nowMs - round.startedAtMs >= roundDurMs && feedback == null) {
                feedback = "escape"
                score -= 1
            }
        }
    }
    // Caught check
    LaunchedEffect(round.stamina) {
        if (round.stamina <= 0 && feedback == null) {
            feedback = "caught"
            score += 1
        }
    }
    // After feedback, new round
    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(700L)
            round = newRound()
            feedback = null
        }
    }

    fun pull() {
        if (done || feedback != null) return
        round = round.copy(
            stamina = (round.stamina - 10).coerceAtLeast(0)
        )
    }

    val maxScore = 5
    val scoreMul = if (done) {
        (0.5 + (score.coerceAtLeast(0).toFloat() / maxScore).coerceIn(0f, 1f)).toDouble()
    } else 0.5

    val pctTime = ((roundDurMs - (nowMs - round.startedAtMs)).coerceAtLeast(0L) / roundDurMs.toFloat()).coerceIn(0f, 1f)
    val pctStamina = (round.stamina.toFloat() / round.maxStamina).coerceIn(0f, 1f)

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
                Text("Pescador: agota al pez", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap TIRAR para reducir stamina del pez. Si llega a 0 = capturado.",
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
                    Text("Peces capturados", color = Dim, fontSize = 10.sp)
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
        Spacer(Modifier.height(16.dp))

        // Visual del pez con stamina
        EmpireCard(borderColor = when (feedback) {
            "caught" -> Emerald
            "escape" -> Color(0xFFFF7A7A)
            else -> Gold
        }) {
            Column(Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(when (feedback) {
                    "caught" -> "🐟"
                    "escape" -> "💨"
                    else -> "🐠"
                }, fontSize = 64.sp)
                Spacer(Modifier.height(8.dp))
                Text("Stamina del pez: ${round.stamina}/${round.maxStamina}",
                    color = Paper, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(
                    progress = { pctStamina },
                    color = when {
                        pctStamina > 0.5f -> Color(0xFFFF7A7A)
                        pctStamina > 0.2f -> Color(0xFFFFB74D)
                        else -> Emerald
                    },
                    modifier = Modifier.fillMaxWidth().height(8.dp).padding(top = 4.dp)
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { pctTime },
                    color = Sapphire,
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                )
                Text("Tiempo de la lucha: ${(pctTime * 10).toInt()}s",
                    color = Dim, fontSize = 10.sp)
                feedback?.let { fb ->
                    val (label, c) = when (fb) {
                        "caught" -> "🎉 ¡Capturado! +1" to Emerald
                        else -> "💨 Se ha escapado. -1" to Color(0xFFFF7A7A)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(label, color = c, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.weight(1f))

        // Botón TIRAR
        if (!done) {
            Button(
                onClick = { pull() },
                enabled = feedback == null,
                modifier = Modifier.fillMaxWidth().height(70.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold)
            ) {
                Text("🎣 TIRAR (-10 stamina)",
                    color = Color.Black, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar", color = Dim, fontSize = 12.sp)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = { done = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Plantarse", color = Color(0xFFFF7A7A), fontSize = 12.sp)
                }
            }
        } else {
            Button(
                onClick = { onFinish(scoreMul) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Gold)
            ) {
                Text("Cobrar y volver al hub",
                    color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            "El pez recupera +5 stamina cada 600ms. Tira fuerte y constante.",
            color = Dim, fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }
}
