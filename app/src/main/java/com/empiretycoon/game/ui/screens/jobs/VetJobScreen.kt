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
 * Veterinario — diagnostica el síntoma del animal.
 *
 * Cada paciente (animal random) presenta UN síntoma textual
 * (fiebre/dolor/tos/falta de apetito). Tap el diagnóstico correcto.
 * 4 botones (resfriado/fractura/parásitos/dieta). Cada tipo de síntoma
 * mapea con un diagnóstico único. 30s, score teórico ~14.
 */
private val SYMPTOM_TO_DIAG = listOf(
    Triple("🌡️", "Fiebre alta", 0),
    Triple("🦴", "Cojera evidente", 1),
    Triple("🤧", "Tos persistente", 2),
    Triple("🥣", "Sin apetito", 3)
)
private val DIAGNOSES = listOf("🤒" to "RESFRIADO", "🦴" to "FRACTURA", "🪱" to "PARÁSITOS", "🥗" to "DIETA")
private val ANIMALS = listOf("🐶", "🐱", "🐰", "🐦", "🐢")

@Composable
fun VetJobScreen(state: GameState, onFinish: (Double) -> Unit, onCancel: () -> Unit) {
    val job = JobId.VET
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }
    val roundDurMs = 3500L
    var symptom by remember { mutableStateOf(SYMPTOM_TO_DIAG.random(rng)) }
    var animal by remember { mutableStateOf(ANIMALS.random(rng)) }
    var roundStart by remember { mutableStateOf(System.currentTimeMillis()) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var secondsLeft by remember { mutableStateOf(30) }
    var score by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && !done) { delay(1000L); secondsLeft -= 1 }
        done = true
    }
    LaunchedEffect(Unit) {
        while (!done) {
            delay(80L); nowMs = System.currentTimeMillis()
            if (nowMs - roundStart >= roundDurMs && feedback == null) { feedback = "timeout"; score -= 1 }
        }
    }
    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(400L); symptom = SYMPTOM_TO_DIAG.random(rng); animal = ANIMALS.random(rng)
            roundStart = System.currentTimeMillis(); feedback = null
        }
    }
    fun tap(d: Int) {
        if (done || feedback != null) return
        if (d == symptom.third) { score += 1; feedback = "ok" } else { score -= 1; feedback = "bad" }
    }

    val maxScore = 14
    val scoreMul = if (done) (0.5 + (score.coerceAtLeast(0).toFloat() / maxScore).coerceIn(0f, 1f)).toDouble() else 0.5
    val pctTime = ((roundDurMs - (nowMs - roundStart)).coerceAtLeast(0L) / roundDurMs.toFloat()).coerceIn(0f, 1f)

    Column(Modifier.fillMaxSize().background(Ink).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(job.emoji, fontSize = 26.sp); Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Veterinario: diagnostica", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap el diagnóstico que coincide con el síntoma.", color = Dim, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Tiempo", color = Dim, fontSize = 10.sp)
                Text("${secondsLeft}s", color = Sapphire, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator({ secondsLeft.toFloat() / 30f }, Modifier.fillMaxWidth().height(4.dp))
        Spacer(Modifier.height(8.dp))
        EmpireCard(borderColor = Sapphire) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text("Diagnósticos OK", color = Dim, fontSize = 10.sp)
                    Text("$score / $maxScore", color = if (score >= maxScore / 2) Emerald else Paper, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Wage", color = Dim, fontSize = 10.sp)
                    Text((previewBaseWage * scoreMul).fmtMoney(), color = if (scoreMul >= 1.10) Emerald else Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("×${"%.2f".format(scoreMul)}", color = Dim, fontSize = 10.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator({ pctTime }, color = if (pctTime > 0.5f) Emerald else if (pctTime > 0.25f) Color(0xFFFFB74D) else Color(0xFFFF7A7A), modifier = Modifier.fillMaxWidth().height(6.dp))
        Spacer(Modifier.height(20.dp))

        EmpireCard(borderColor = when (feedback) { "ok" -> Emerald; "bad", "timeout" -> Color(0xFFFF7A7A); else -> Gold }) {
            Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(animal, fontSize = 64.sp)
                Spacer(Modifier.height(4.dp))
                Text("Síntoma:", color = Dim, fontSize = 11.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(symptom.first, fontSize = 30.sp); Spacer(Modifier.width(6.dp))
                    Text(symptom.second, color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
                feedback?.let {
                    val (lbl, c) = when (it) { "ok" -> "🌟 ¡Diagnóstico correcto! +1" to Emerald; "bad" -> "❌ Era ${DIAGNOSES[symptom.third].second}" to Color(0xFFFF7A7A); else -> "⏰ Lento -1" to Color(0xFFFF7A7A) }
                    Text(lbl, color = c, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.weight(1f))

        for (row in 0 until 2) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 2) {
                    val i = row * 2 + col
                    val (emoji, label) = DIAGNOSES[i]
                    Box(Modifier.weight(1f).padding(4.dp).height(70.dp).clip(RoundedCornerShape(10.dp)).background(InkBorder).clickable { tap(i) }, contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(emoji, fontSize = 28.sp); Text(label, color = Paper, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (done) Button({ onFinish(scoreMul) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
            Text("Cobrar y volver", color = Color.Black, fontWeight = FontWeight.Bold)
        } else Row(Modifier.fillMaxWidth()) {
            TextButton({ onCancel() }, Modifier.weight(1f)) { Text("Cancelar", color = Dim) }
            Spacer(Modifier.width(8.dp))
            Button({ done = true }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB85C5C))) { Text("Plantarse", color = Color.White) }
        }
    }
}
