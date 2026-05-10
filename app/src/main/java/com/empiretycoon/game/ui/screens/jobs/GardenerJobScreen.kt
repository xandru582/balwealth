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
 * Jardinero — atiende plantas según necesidad: 🌱 SEMBRAR / 💧 REGAR / ✂️ PODAR.
 *
 * Cada planta del grid 3x3 tiene un estado random. Tap herramienta + planta
 * en orden correcto. 30s, score teórico ~25.
 */
private enum class GardenAction(val emoji: String, val label: String) {
    SEED("🌱", "SEMBRAR"), WATER("💧", "REGAR"), PRUNE("✂️", "PODAR")
}
private enum class PlantState(val emoji: String) { EMPTY("🟫"), DRY("🥀"), OVERGROWN("🌿") }

@Composable
fun GardenerJobScreen(state: GameState, onFinish: (Double) -> Unit, onCancel: () -> Unit) {
    val job = JobId.GARDENER
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }
    val n = 9
    var plants by remember { mutableStateOf(List(n) { PlantState.values().random(rng) }) }
    var secondsLeft by remember { mutableStateOf(30) }
    var score by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var selectedTool by remember { mutableStateOf(GardenAction.SEED) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && !done) { delay(1000L); secondsLeft -= 1 }
        done = true
    }
    LaunchedEffect(Unit) {
        while (!done) {
            delay(900L)
            // De vez en cuando una planta cambia a un nuevo estado random
            val pick = rng.nextInt(n)
            plants = plants.toMutableList().also { it[pick] = PlantState.values().random(rng) }
        }
    }

    fun matches(state: PlantState, tool: GardenAction): Boolean = when (state) {
        PlantState.EMPTY -> tool == GardenAction.SEED
        PlantState.DRY -> tool == GardenAction.WATER
        PlantState.OVERGROWN -> tool == GardenAction.PRUNE
    }

    fun tapPlant(i: Int) {
        if (done) return
        if (matches(plants[i], selectedTool)) {
            score += 1
            // Tras la acción, pasa a estado feliz (random distinto al actual).
            val newStates = PlantState.values().filter { it != plants[i] }
            plants = plants.toMutableList().also { it[i] = newStates.random(rng) }
        } else {
            score -= 1
        }
    }

    val maxScore = 25
    val scoreMul = if (done) (0.5 + (score.coerceAtLeast(0).toFloat() / maxScore).coerceIn(0f, 1f)).toDouble() else 0.5

    Column(Modifier.fillMaxSize().background(Ink).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(job.emoji, fontSize = 26.sp); Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Jardinero: atiende cada planta", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Gold)
                Text("🟫 → SEMBRAR · 🥀 → REGAR · 🌿 → PODAR", color = Dim, fontSize = 11.sp)
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
                    Text("Plantas", color = Dim, fontSize = 10.sp)
                    Text("$score / $maxScore", color = if (score >= maxScore / 2) Emerald else Paper, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Wage", color = Dim, fontSize = 10.sp)
                    Text((previewBaseWage * scoreMul).fmtMoney(), color = if (scoreMul >= 1.10) Emerald else Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("×${"%.2f".format(scoreMul)}", color = Dim, fontSize = 10.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        Text("Herramienta seleccionada:", color = Dim, fontSize = 11.sp)
        Row(Modifier.fillMaxWidth()) {
            for (a in GardenAction.values()) {
                Box(Modifier.weight(1f).padding(3.dp).height(54.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (selectedTool == a) Gold else InkBorder).clickable { selectedTool = a }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(a.emoji, fontSize = 22.sp); Text(a.label, color = if (selectedTool == a) Color.Black else Paper, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Grid 3x3 de plantas
        for (row in 0 until 3) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 3) {
                    val i = row * 3 + col
                    Box(Modifier.weight(1f).padding(4.dp).aspectRatio(1f).clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1B2738)).clickable { tapPlant(i) }, contentAlignment = Alignment.Center) {
                        Text(plants[i].emoji, fontSize = 36.sp)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        if (done) Button({ onFinish(scoreMul) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
            Text("Cobrar y volver", color = Color.Black, fontWeight = FontWeight.Bold)
        } else Row(Modifier.fillMaxWidth()) {
            TextButton({ onCancel() }, Modifier.weight(1f)) { Text("Cancelar", color = Dim) }
            Spacer(Modifier.width(8.dp))
            Button({ done = true }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB85C5C))) { Text("Plantarse", color = Color.White) }
        }
    }
}
