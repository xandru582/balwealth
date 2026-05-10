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
 * Mini-juego del Pintor — traza el dibujo en orden.
 *
 * Mecánica:
 *  - 6 puntos numerados (1..6) en posiciones aleatorias del lienzo.
 *  - El jugador debe tap los puntos en orden ascendente (1, 2, 3, ...).
 *  - Tap correcto = punto se vuelve verde, avanza el contador.
 *  - Tap mal (cualquier otro punto, incluso uno ya pintado) = reset
 *    de toda la composición y -1 score.
 *  - Cuando todos los 6 están pintados → +1 score, nueva composición.
 *  - 30s. Score teórico ~6 → scoreMul.
 */
private data class PaintDot(val id: Int, val xPct: Float, val yPct: Float)

@Composable
fun PainterJobScreen(
    state: GameState,
    onFinish: (Double) -> Unit,
    onCancel: () -> Unit
) {
    val job = JobId.PAINTER
    val totalSeconds = 30
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }

    fun newComposition(): List<PaintDot> {
        return (1..6).map { id ->
            PaintDot(
                id = id,
                xPct = 0.06f + rng.nextFloat() * 0.85f,
                yPct = 0.06f + rng.nextFloat() * 0.85f
            )
        }
    }

    var dots by remember { mutableStateOf(newComposition()) }
    var nextId by remember { mutableStateOf(1) }
    var secondsLeft by remember { mutableStateOf(totalSeconds) }
    var score by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && !done) {
            delay(1000L)
            secondsLeft -= 1
        }
        done = true
    }

    fun tapDot(id: Int) {
        if (done || feedback != null) return
        if (id == nextId) {
            val newNext = nextId + 1
            if (newNext > 6) {
                score += 1
                feedback = "ok"
            } else {
                nextId = newNext
            }
        } else {
            score -= 1
            feedback = "bad"
        }
    }

    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(550L)
            dots = newComposition()
            nextId = 1
            feedback = null
        }
    }

    val maxScore = 6
    val scoreMul = if (done) {
        (0.5 + (score.coerceAtLeast(0).toFloat() / maxScore).coerceIn(0f, 1f)).toDouble()
    } else 0.5

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
                Text("Pintor: traza en orden", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap los puntos del 1 al 6 sin equivocarte",
                    color = Dim, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Tiempo", color = Dim, fontSize = 10.sp)
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
                    Text("Cuadros pintados", color = Dim, fontSize = 10.sp)
                    Text("$score / $maxScore",
                        color = if (score >= maxScore / 2) Emerald else Paper,
                        fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Próximo punto: $nextId", color = Gold, fontSize = 11.sp)
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
        Spacer(Modifier.height(10.dp))

        // Lienzo
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF132030))
        ) {
            for (dot in dots) {
                val isDone = dot.id < nextId
                val isNext = dot.id == nextId
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(0.dp)
                ) {
                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                x = (dot.xPct * 240).dp,
                                y = (dot.yPct * 320).dp
                            )
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(when {
                                isDone -> Color(0xFF66BB6A)
                                isNext -> Gold
                                else -> Color(0xFF1B2738)
                            })
                            .clickable { tapDot(dot.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${dot.id}",
                            color = if (isDone || isNext) Color.Black else Paper,
                            fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            feedback?.let { fb ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0x88000000)),
                    contentAlignment = Alignment.Center
                ) {
                    val (label, c) = when (fb) {
                        "ok" -> "🎨 ¡Cuadro completo!" to Emerald
                        else -> "❌ Trazo incorrecto" to Color(0xFFFF7A7A)
                    }
                    Text(label, color = c, fontSize = 22.sp,
                        fontWeight = FontWeight.Black)
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
        Text(
            "Cualquier tap fuera de orden resetea el cuadro entero.",
            color = Dim, fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }
}
