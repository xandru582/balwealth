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
 * Mini-juego del Farmacéutico — combina viales (sin orden).
 *
 * Mecánica:
 *  - Pool de 6 viales con emoji distinto.
 *  - Cada receta pide 3 viales concretos (sin importar el orden).
 *  - Player tap los viales. Si está en la receta y no marcado aún →
 *    se marca con ✓ (verde). Si NO está en la receta o ya estaba
 *    marcado → -1 score y reset de los marcados.
 *  - Cuando los 3 viales correctos están marcados → +1 score, nueva
 *    receta.
 *  - 30s. Score teórico ~8 → scoreMul.
 */
private val POTIONS = listOf(
    "🧪" to "Verde",
    "🧫" to "Cultivo",
    "💉" to "Suero",
    "🩸" to "Plasma",
    "💧" to "Acuoso",
    "🧬" to "Genético"
)

@Composable
fun PharmacistJobScreen(
    state: GameState,
    onFinish: (Double) -> Unit,
    onCancel: () -> Unit
) {
    val job = JobId.PHARMACIST
    val totalSeconds = 30
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }

    fun newRecipe(): Set<Int> {
        // 3 viales únicos del pool de 6
        val s = mutableSetOf<Int>()
        while (s.size < 3) s.add(rng.nextInt(POTIONS.size))
        return s
    }

    var recipe by remember { mutableStateOf(newRecipe()) }
    var marked by remember { mutableStateOf(setOf<Int>()) }
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

    fun tapVial(idx: Int) {
        if (done || feedback != null) return
        if (idx in recipe && idx !in marked) {
            val newMarked = marked + idx
            if (newMarked == recipe) {
                score += 1
                feedback = "ok"
            } else {
                marked = newMarked
            }
        } else {
            score -= 1
            feedback = "bad"
        }
    }

    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(500L)
            recipe = newRecipe()
            marked = emptySet()
            feedback = null
        }
    }

    val maxScore = 8
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
                Text("Farmacéutico: prepara la receta", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap los 3 viales correctos. El orden no importa.",
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
                    Text("Recetas", color = Dim, fontSize = 10.sp)
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
        Spacer(Modifier.height(12.dp))

        // Receta visible
        EmpireCard(borderColor = when (feedback) {
            "ok" -> Emerald
            "bad" -> Color(0xFFFF7A7A)
            else -> Gold
        }) {
            Column(Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Receta:", color = Dim, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (idx in recipe.sorted()) {
                        val (emoji, name) = POTIONS[idx]
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(emoji, fontSize = 30.sp)
                            Text(name, color = Paper, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Marcados: ${marked.size}/3", color = Sapphire, fontSize = 11.sp)
                feedback?.let { fb ->
                    Spacer(Modifier.height(4.dp))
                    val (label, c) = when (fb) {
                        "ok" -> "✅ Receta correcta" to Emerald
                        else -> "❌ Vial incorrecto" to Color(0xFFFF7A7A)
                    }
                    Text(label, color = c, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.weight(1f))

        // Pool de viales: 6 botones en grid 3x2
        Text("Tap los viales para mezclar:", color = Dim, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        for (row in 0 until 2) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 3) {
                    val idx = row * 3 + col
                    val (emoji, name) = POTIONS[idx]
                    val isMarked = idx in marked
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .height(80.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isMarked) Color(0xFF66BB6A) else InkBorder)
                            .clickable { tapVial(idx) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(emoji, fontSize = 26.sp)
                            Text(name,
                                color = if (isMarked) Color.Black else Paper,
                                fontSize = 10.sp)
                            if (isMarked) Text("✓",
                                color = Color.White, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }
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
