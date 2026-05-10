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
 * Mini-juego del Streamer — speed-tap secuencia.
 *
 * Mecánica:
 *  - 5 botones de colores (rojo/azul/verde/amarillo/morado).
 *  - Cada ronda muestra una secuencia de 4 colores en pantalla.
 *  - El jugador tap los botones en el orden mostrado.
 *  - Si todos correctos = +1 score, secuencia nueva más larga
 *    (max 6 colores).
 *  - Tap mal = secuencia reset, -1 score.
 *  - Timer 5s por secuencia. Si expira = -1 score, nueva secuencia.
 *  - 30s. Score teórico ~6 → scoreMul 0.5..1.5.
 */
private val COLOR_BTNS = listOf(
    Color(0xFFE53935) to "🔴",
    Color(0xFF1E88E5) to "🔵",
    Color(0xFF43A047) to "🟢",
    Color(0xFFFFB300) to "🟡",
    Color(0xFF8E24AA) to "🟣"
)

private data class SeqRound(
    val sequence: List<Int>,         // índices 0..4
    val progress: Int,
    val startedAtMs: Long
)

@Composable
fun StreamerJobScreen(
    state: GameState,
    onFinish: (Double) -> Unit,
    onCancel: () -> Unit
) {
    val job = JobId.STREAMER
    val totalSeconds = 30
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }
    val roundDurMs = 5000L

    fun newRound(len: Int): SeqRound {
        val seq = (0 until len).map { rng.nextInt(COLOR_BTNS.size) }
        return SeqRound(seq, 0, System.currentTimeMillis())
    }

    var seqLen by remember { mutableStateOf(4) }
    var round by remember { mutableStateOf(newRound(seqLen)) }
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
    LaunchedEffect(Unit) {
        while (!done) {
            delay(80L)
            nowMs = System.currentTimeMillis()
            if (nowMs - round.startedAtMs >= roundDurMs && feedback == null) {
                feedback = "timeout"
                score -= 1
            }
        }
    }
    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(500L)
            // Aumentar dificultad si fue correcto
            if (feedback == "ok" && seqLen < 6) seqLen += 1
            if (feedback == "bad" && seqLen > 4) seqLen -= 1
            round = newRound(seqLen)
            feedback = null
        }
    }

    fun tapColor(idx: Int) {
        if (done || feedback != null) return
        val expected = round.sequence.getOrNull(round.progress) ?: return
        if (idx == expected) {
            val newProgress = round.progress + 1
            if (newProgress >= round.sequence.size) {
                score += 1
                feedback = "ok"
            } else {
                round = round.copy(progress = newProgress)
            }
        } else {
            score -= 1
            feedback = "bad"
        }
    }

    val maxScore = 6
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
                Text("Streamer: speed-react", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap los colores en el orden mostrado",
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
                    Text("Secuencia: $seqLen colores", color = Dim, fontSize = 10.sp)
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

        // Mostrar la secuencia
        EmpireCard(borderColor = when (feedback) {
            "ok" -> Emerald
            "bad", "timeout" -> Color(0xFFFF7A7A)
            else -> Gold
        }) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Secuencia a tap:", color = Dim, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for ((idx, colIdx) in round.sequence.withIndex()) {
                        val (c, emoji) = COLOR_BTNS[colIdx]
                        val isDone = idx < round.progress
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDone) c.copy(alpha = 0.4f) else c),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isDone) "✓" else emoji, fontSize = 18.sp,
                                color = if (isDone) Color.White else Color.Transparent)
                        }
                    }
                }
                feedback?.let { fb ->
                    Spacer(Modifier.height(6.dp))
                    val (label, color) = when (fb) {
                        "ok" -> "🌟 ¡Combo!" to Emerald
                        "bad" -> "❌ Error" to Color(0xFFFF7A7A)
                        else -> "⏰ Timeout" to Color(0xFFFF7A7A)
                    }
                    Text(label, color = color, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.weight(1f))

        // Botones colorados
        Text("Tu turno:", color = Dim, fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            for ((idx, pair) in COLOR_BTNS.withIndex()) {
                val (c, emoji) = pair
                Box(
                    Modifier
                        .weight(1f)
                        .padding(4.dp)
                        .height(70.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(c)
                        .clickable { tapColor(idx) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 32.sp)
                }
            }
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
