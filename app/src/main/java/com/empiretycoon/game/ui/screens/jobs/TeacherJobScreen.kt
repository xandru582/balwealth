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
 * Mini-juego del Profesor — trivia rápida.
 *
 * Mecánica:
 *  - Pool de 15 preguntas con 3 opciones cada una.
 *  - Cada ronda muestra 1 pregunta. 5s para responder. Tap correcta = +1,
 *    tap mal = -1, timeout = -1.
 *  - 30s. Score teórico ≈ 6 → scoreMul.
 */
private data class TriviaQ(
    val question: String,
    val options: List<String>,
    val correctIdx: Int
)

private val TRIVIA_POOL = listOf(
    TriviaQ("¿Capital de Francia?", listOf("Madrid", "París", "Roma"), 1),
    TriviaQ("¿Cuánto es 2 + 2 × 3?", listOf("8", "12", "10"), 0),
    TriviaQ("¿Cuántos planetas hay en el sistema solar?", listOf("8", "9", "7"), 0),
    TriviaQ("¿Año del primer hombre en la Luna?", listOf("1972", "1969", "1965"), 1),
    TriviaQ("¿Fórmula química del agua?", listOf("CO2", "H2O", "O3"), 1),
    TriviaQ("¿Quién pintó La Gioconda?", listOf("Picasso", "Van Gogh", "Leonardo"), 2),
    TriviaQ("¿En qué continente está Egipto?", listOf("Asia", "África", "Europa"), 1),
    TriviaQ("¿Qué animal pone el huevo más grande?", listOf("Avestruz", "Pingüino", "Águila"), 0),
    TriviaQ("¿Cuántos lados tiene un hexágono?", listOf("5", "6", "7"), 1),
    TriviaQ("¿En qué año cayó el Muro de Berlín?", listOf("1985", "1989", "1991"), 1),
    TriviaQ("¿Quién escribió 'Don Quijote'?", listOf("Lope de Vega", "Cervantes", "Quevedo"), 1),
    TriviaQ("¿Mayor océano del mundo?", listOf("Atlántico", "Pacífico", "Índico"), 1),
    TriviaQ("¿Qué planeta es el más cercano al Sol?", listOf("Venus", "Mercurio", "Marte"), 1),
    TriviaQ("¿Cuál es el río más largo de Europa?", listOf("Danubio", "Rin", "Volga"), 2),
    TriviaQ("¿Velocidad de la luz aproximada?", listOf("300 mil km/s", "30 mil km/s", "3 mil km/s"), 0)
)

private data class TeacherRound(val q: TriviaQ, val startedAtMs: Long)

@Composable
fun TeacherJobScreen(
    state: GameState,
    onFinish: (Double) -> Unit,
    onCancel: () -> Unit
) {
    val job = JobId.TEACHER
    val totalSeconds = 30
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }
    val roundDurMs = 5000L

    fun newRound(): TeacherRound = TeacherRound(
        q = TRIVIA_POOL[rng.nextInt(TRIVIA_POOL.size)],
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
            delay(80L)
            nowMs = System.currentTimeMillis()
            if (nowMs - round.startedAtMs >= roundDurMs && feedback == null) {
                feedback = "timeout"; score -= 1
            }
        }
    }
    LaunchedEffect(feedback) {
        if (feedback != null) { delay(700L); round = newRound(); feedback = null }
    }

    fun tapOption(idx: Int) {
        if (done || feedback != null) return
        if (idx == round.q.correctIdx) { score += 1; feedback = "ok" }
        else { score -= 1; feedback = "bad" }
    }

    val maxScore = 6
    val scoreMul = if (done) (0.5 + (score.coerceAtLeast(0).toFloat() / maxScore).coerceIn(0f, 1f)).toDouble() else 0.5
    val pctTime = ((roundDurMs - (nowMs - round.startedAtMs)).coerceAtLeast(0L) / roundDurMs.toFloat()).coerceIn(0f, 1f)

    Column(Modifier.fillMaxSize().background(Ink).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(job.emoji, fontSize = 26.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Profesor: trivia rápida", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gold)
                Text("5s por pregunta. Tap correcta = +1, error/timeout = -1.", color = Dim, fontSize = 11.sp)
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
        Spacer(Modifier.height(16.dp))

        EmpireCard(borderColor = when (feedback) {
            "ok" -> Emerald; "bad", "timeout" -> Color(0xFFFF7A7A); else -> Gold
        }) {
            Text(round.q.question, color = Paper, fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            feedback?.let { fb ->
                val (label, c) = when (fb) {
                    "ok" -> "🌟 ¡Correcto!" to Emerald
                    "bad" -> "❌ Era: ${round.q.options[round.q.correctIdx]}" to Color(0xFFFF7A7A)
                    else -> "⏰ Timeout: ${round.q.options[round.q.correctIdx]}" to Color(0xFFFF7A7A)
                }
                Text(label, color = c, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.weight(1f))

        for ((idx, opt) in round.q.options.withIndex()) {
            Box(
                Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (feedback != null && idx == round.q.correctIdx) Emerald else InkBorder)
                    .clickable { tapOption(idx) }
                    .padding(14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text("${('A' + idx)})  $opt", color = Paper, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold)
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
