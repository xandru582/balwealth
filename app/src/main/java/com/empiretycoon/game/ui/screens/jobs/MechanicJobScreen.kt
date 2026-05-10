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
 * Mini-juego del Mecánico — identifica la pieza averiada.
 *
 * Mecánica:
 *  - 4 piezas mostradas en grid 2×2 (motor, batería, rueda, escape, faro,
 *    sistema de frenos…). Una al azar tiene un símbolo de avería ⚠️.
 *  - Tap la pieza correcta → +1 score, nueva ronda inmediata.
 *  - Tap pieza incorrecta → -1 score, nueva ronda.
 *  - Cada ronda dura máximo 3.5s. Si expira sin tap → -1 score, nueva
 *    ronda.
 *  - Duración 30s. Score teórico máximo ~10 → scoreMul 0.5..1.5.
 */
private val MECH_PARTS = listOf(
    "🔧" to "Motor",
    "🔋" to "Batería",
    "🛞" to "Rueda",
    "💡" to "Faro",
    "🌀" to "Turbo",
    "💨" to "Escape",
    "❄️" to "Aire",
    "🛢️" to "Aceite"
)

private data class MechRound(
    val parts: List<Pair<String, String>>,   // 4 piezas
    val brokenIdx: Int,
    val startedAtMs: Long
)

@Composable
fun MechanicJobScreen(
    state: GameState,
    onFinish: (Double) -> Unit,
    onCancel: () -> Unit
) {
    val job = JobId.CAR_MECHANIC
    val totalSeconds = 30
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }
    val roundDurMs = 3500L

    fun newRound(): MechRound {
        val pool = MECH_PARTS.shuffled(rng).take(4)
        val brokenIdx = rng.nextInt(4)
        return MechRound(pool, brokenIdx, System.currentTimeMillis())
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
            delay(80L)
            nowMs = System.currentTimeMillis()
            // Detect timeout
            if (nowMs - round.startedAtMs >= roundDurMs && feedback == null) {
                feedback = "timeout"
                score -= 1
            }
        }
    }
    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(450L)
            round = newRound()
            feedback = null
        }
    }

    fun tapPart(idx: Int) {
        if (done || feedback != null) return
        if (idx == round.brokenIdx) {
            score += 1
            feedback = "ok"
        } else {
            score -= 1
            feedback = "bad"
        }
    }

    val maxScore = 10
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
                Text("Mecánico: ¿qué se ha roto?", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap la pieza con el ⚠️ antes de que expire la ronda",
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
        Spacer(Modifier.height(10.dp))

        // 4 piezas en grid 2×2
        Column(modifier = Modifier.fillMaxWidth()) {
            for (row in 0 until 2) {
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0 until 2) {
                        val idx = row * 2 + col
                        val (emoji, name) = round.parts[idx]
                        val isBroken = idx == round.brokenIdx
                        val highlightOk = feedback == "ok" && isBroken
                        val highlightBad = feedback == "bad" && idx == round.brokenIdx
                        val isWrongTap = feedback == "bad" && idx != round.brokenIdx && false  // no destacamos el wrong tap
                        Box(
                            Modifier
                                .weight(1f)
                                .padding(4.dp)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(when {
                                    highlightOk -> Color(0xFF66BB6A)
                                    highlightBad -> Color(0xFFFFB74D)
                                    else -> Color(0xFF1E3245)
                                })
                                .clickable { tapPart(idx) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(emoji, fontSize = 42.sp)
                                Text(name, color = Paper, fontSize = 11.sp)
                                // Mostrar ⚠️ solo si feedback != null (ya se decidió la ronda).
                                if (feedback != null && isBroken) {
                                    Text("⚠️ AVERÍA", color = Color(0xFFFF7A7A),
                                        fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            when (feedback) {
                "ok" -> "🌟 ¡Bien diagnosticado!"
                "bad" -> "❌ No, no era esa..."
                "timeout" -> "⏰ Demasiado lento."
                else -> "Tap la pieza que crees averiada."
            },
            color = when (feedback) {
                "ok" -> Emerald
                "bad", "timeout" -> Color(0xFFFF7A7A)
                else -> Dim
            },
            fontSize = 13.sp, fontWeight = FontWeight.Bold,
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
