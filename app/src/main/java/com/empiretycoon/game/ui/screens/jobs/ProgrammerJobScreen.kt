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
import androidx.compose.ui.text.font.FontFamily
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
 * Mini-juego del Programador — encuentra el bug.
 *
 * Mecánica:
 *  - Cada ronda muestra 4 líneas de "código". 1 contiene un bug obvio
 *    (typo, operador mal, signo de puntuación raro). Las otras 3 son
 *    correctas.
 *  - Tap la línea con el bug → +1 score, nueva ronda inmediata.
 *  - Tap línea correcta → -1 score, nueva ronda.
 *  - Si la ronda dura más de 5s sin tap → -1 score, nueva ronda.
 *  - Duración 30s. Score máximo ≈ 6 → scoreMul 0.5..1.5.
 */
private data class CodeSnippet(
    val ok: String,
    val buggy: String,
    val bugHint: String
)

private val SNIPPETS = listOf(
    CodeSnippet(
        ok = "val name = \"Iris\"",
        buggy = "val name = \"Iris",
        bugHint = "Comilla sin cerrar"
    ),
    CodeSnippet(
        ok = "if (x == 0) return null",
        buggy = "if (x = 0) return null",
        bugHint = "Asignación dentro de condición"
    ),
    CodeSnippet(
        ok = "for (i in 0..10) print(i)",
        buggy = "for (i in 0...10) print(i)",
        bugHint = "Tres puntos no es operador rango"
    ),
    CodeSnippet(
        ok = "list.filter { it > 0 }",
        buggy = "list.fitler { it > 0 }",
        bugHint = "Typo: fitler -> filter"
    ),
    CodeSnippet(
        ok = "fun area(r: Int) = 3.14 * r * r",
        buggy = "fun area(r: Int) = 3.14 * r r",
        bugHint = "Falta operador entre r y r"
    ),
    CodeSnippet(
        ok = "val total = items.sum()",
        buggy = "val total = items.sum",
        bugHint = "Falta paréntesis ()"
    ),
    CodeSnippet(
        ok = "println(\"Hola, mundo\")",
        buggy = "prinln(\"Hola, mundo\")",
        bugHint = "Typo: prinln"
    ),
    CodeSnippet(
        ok = "while (running) tick()",
        buggy = "whlie (running) tick()",
        bugHint = "Typo: whlie"
    )
)

private data class CodeRound(
    val lines: List<String>,
    val correctIndex: Int,
    val hint: String,
    val startedAtMs: Long
)

@Composable
fun ProgrammerJobScreen(
    state: GameState,
    onFinish: (Double) -> Unit,
    onCancel: () -> Unit
) {
    val job = JobId.PROGRAMMER
    val totalSeconds = 30
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }
    val roundDurMs = 5000L

    fun newRound(): CodeRound {
        // 1 buggy snippet + 3 OK distintos
        val pool = SNIPPETS.shuffled(rng)
        val buggy = pool.first()
        val others = pool.drop(1).take(3)
        val all = mutableListOf<Pair<String, Boolean>>()
        all.add(buggy.buggy to true)
        for (s in others) all.add(s.ok to false)
        all.shuffle(rng)
        val correctIdx = all.indexOfFirst { it.second }
        return CodeRound(
            lines = all.map { it.first },
            correctIndex = correctIdx,
            hint = buggy.bugHint,
            startedAtMs = System.currentTimeMillis()
        )
    }

    var round by remember { mutableStateOf(newRound()) }
    var secondsLeft by remember { mutableStateOf(totalSeconds) }
    var score by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var feedback by remember { mutableStateOf<String?>(null) }  // "ok" / "bad" / "timeout"

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
            round = newRound()
            feedback = null
        }
    }

    fun tapLine(idx: Int) {
        if (done || feedback != null) return
        if (idx == round.correctIndex) {
            score += 1
            feedback = "ok"
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
                Text("Programador: encuentra el bug", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = Gold)
                Text("3 líneas correctas, 1 con error obvio. Tap la mala.",
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
        Spacer(Modifier.height(12.dp))

        // 4 líneas de código
        Column(modifier = Modifier.fillMaxWidth()) {
            for ((idx, line) in round.lines.withIndex()) {
                val isCorrect = idx == round.correctIndex
                val highlightOk = feedback == "ok" && isCorrect
                val highlightBad = feedback == "bad" && isCorrect
                val bg = when {
                    highlightOk -> Color(0xFF66BB6A)
                    highlightBad -> Color(0xFFFFB74D)
                    else -> Color(0xFF1B2738)
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg)
                        .clickable { tapLine(idx) }
                        .padding(horizontal = 10.dp, vertical = 12.dp)
                ) {
                    Text(
                        "${idx + 1}.  $line",
                        color = Paper,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            when (feedback) {
                "ok" -> "🌟 ¡Bien visto! (${round.hint})"
                "bad" -> "❌ La pista era: ${round.hint}"
                "timeout" -> "⏰ Demasiado lento. (${round.hint})"
                else -> "Tap la línea que crees con bug."
            },
            color = when (feedback) {
                "ok" -> Emerald
                "bad", "timeout" -> Color(0xFFFF7A7A)
                else -> Dim
            },
            fontSize = 12.sp, fontWeight = FontWeight.Bold,
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
