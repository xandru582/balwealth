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
 * Mini-juego del Chef: gestiona la cola de tickets en cocina.
 *
 * Mecánica:
 *  - 4 slots de ticket. Cada ticket tiene una lista ordenada de 2–3
 *    ingredientes (de un pool de 6).
 *  - Cada ticket vive 8s. Si su timer expira, sale -1 score.
 *  - Tap los ingredientes en el orden correcto → completas (+1 score),
 *    el slot se libera y un nuevo ticket llega a los pocos ms.
 *  - Tap mal → solo reseteamos el progreso del ticket actual (perderás
 *    tiempo, no score). Sin penalización inmediata.
 *  - Duración 30s. Score = tickets completados (–perdidos).
 *  - Score teórico máximo ≈ 12. scoreMul = 0.5 + (score/12)·1.0.
 */
private val INGREDIENTS = listOf(
    Ing("🍅", "Tomate"),
    Ing("🧀", "Queso"),
    Ing("🍞", "Pan"),
    Ing("🥬", "Lechuga"),
    Ing("🥩", "Carne"),
    Ing("🍳", "Huevo")
)

private data class Ing(val emoji: String, val name: String)

private data class Ticket(
    val id: Long,
    val recipe: List<Ing>,
    /** Cuántos ingredientes ya tappeados correctamente. */
    val progress: Int,
    val createdAtMs: Long
)

@Composable
fun ChefJobScreen(
    state: GameState,
    onFinish: (Double) -> Unit,
    onCancel: () -> Unit
) {
    val job = JobId.CHEF
    val totalSeconds = 30
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }

    var secondsLeft by remember { mutableStateOf(totalSeconds) }
    var score by remember { mutableStateOf(0) }
    var tickets by remember { mutableStateOf<List<Ticket?>>(List(4) { null }) }
    var done by remember { mutableStateOf(false) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }

    fun newTicket(): Ticket {
        val recipeLen = 2 + rng.nextInt(2)  // 2 o 3 ingredientes
        val recipe = (0 until recipeLen).map { INGREDIENTS[rng.nextInt(INGREDIENTS.size)] }
        return Ticket(
            id = System.nanoTime(),
            recipe = recipe,
            progress = 0,
            createdAtMs = System.currentTimeMillis()
        )
    }

    // Llenado inicial de slots
    LaunchedEffect(Unit) {
        // Llena slots con un staggered spawn de 400ms
        val initial = MutableList<Ticket?>(4) { null }
        for (i in 0..3) {
            delay(300L * i)
            if (done) break
            initial[i] = newTicket()
            tickets = initial.toList()
        }
    }

    // Countdown + tick global cada 100ms
    LaunchedEffect(Unit) {
        while (!done) {
            delay(100L)
            nowMs = System.currentTimeMillis()
        }
    }
    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && !done) {
            delay(1000L)
            secondsLeft -= 1
        }
        done = true
    }

    // Expiración de tickets — cada 200ms revisamos si alguno sobrepasa 8s.
    LaunchedEffect(Unit) {
        while (!done) {
            delay(200L)
            val now = System.currentTimeMillis()
            val newList = tickets.toMutableList()
            var expired = 0
            for (i in newList.indices) {
                val t = newList[i] ?: continue
                if (now - t.createdAtMs >= 8000L) {
                    expired += 1
                    newList[i] = newTicket()
                }
            }
            if (expired > 0) {
                score -= expired
                tickets = newList.toList()
            }
        }
    }

    val maxScore = 12
    val scoreMul = if (done) {
        (0.5 + (score.coerceAtLeast(0).toFloat() / maxScore).coerceIn(0f, 1f)).toDouble()
    } else 0.5

    fun tapIngredient(ing: Ing) {
        if (done) return
        // Buscamos el primer ticket cuyo siguiente esperado sea este ing.
        val idx = tickets.indexOfFirst {
            it != null && it.progress < it.recipe.size && it.recipe[it.progress] == ing
        }
        if (idx >= 0) {
            val newList = tickets.toMutableList()
            val cur = newList[idx]!!
            val newProgress = cur.progress + 1
            if (newProgress >= cur.recipe.size) {
                // Completado → +1 score, nuevo ticket
                score += 1
                newList[idx] = newTicket()
            } else {
                newList[idx] = cur.copy(progress = newProgress)
            }
            tickets = newList.toList()
        } else {
            // Ningún ticket espera este ingrediente como siguiente — reset
            // del primer ticket con algún progreso (frustrante pero sin
            // penalty inmediato).
            val resetIdx = tickets.indexOfFirst { it != null && it.progress > 0 }
            if (resetIdx >= 0) {
                val newList = tickets.toMutableList()
                newList[resetIdx] = newList[resetIdx]!!.copy(progress = 0)
                tickets = newList.toList()
            }
        }
    }

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
                Text("Chef: gestión de cocina", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap los ingredientes en el orden de cada ticket",
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
        Spacer(Modifier.height(10.dp))

        // Tickets en columnas
        Row(modifier = Modifier.fillMaxWidth()) {
            for (i in 0 until 4) {
                val t = tickets.getOrNull(i)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF132030))
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (t != null) {
                        // Timer del ticket: 8s desde createdAtMs
                        val elapsed = (nowMs - t.createdAtMs).coerceAtLeast(0L)
                        val pct = (1f - elapsed / 8000f).coerceIn(0f, 1f)
                        val tColor = when {
                            pct > 0.6f -> Emerald
                            pct > 0.3f -> Color(0xFFFFB74D)
                            else -> Color(0xFFFF7A7A)
                        }
                        LinearProgressIndicator(
                            progress = { pct },
                            color = tColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("Ticket #${i + 1}", color = Dim, fontSize = 9.sp)
                        Spacer(Modifier.height(2.dp))
                        // Receta — cada paso: emoji + checked si progress > i
                        for ((stepIdx, ing) in t.recipe.withIndex()) {
                            val isDone = stepIdx < t.progress
                            val isNext = stepIdx == t.progress
                            val stepColor = when {
                                isDone -> Emerald
                                isNext -> Gold
                                else -> Dim
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(ing.emoji, fontSize = 16.sp)
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    "${if (isDone) "✓" else if (isNext) "▶" else "·"}",
                                    color = stepColor, fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Text("—", color = Dim, fontSize = 18.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("Ingredientes (tap para añadir):", color = Dim, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        // Botones de ingredientes en grid 3×2
        for (row in 0 until 2) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 3) {
                    val i = row * 3 + col
                    val ing = INGREDIENTS[i]
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(InkBorder)
                            .clickable { tapIngredient(ing) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(ing.emoji, fontSize = 28.sp)
                            Text(ing.name, color = Dim, fontSize = 9.sp)
                        }
                    }
                }
            }
        }

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
        Text(
            "Tap MAL = no penaliza, solo resetea el ticket actual.",
            color = Dim, fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }
}
