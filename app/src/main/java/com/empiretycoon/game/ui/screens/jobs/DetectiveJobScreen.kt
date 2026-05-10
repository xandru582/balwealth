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
 * Mini-juego del Detective — find-the-clue.
 *
 * Mecánica:
 *  - Escena representada como grid 5×5 de "puntos de interés" indistintos
 *    a la vista. 3 de las 25 celdas tienen una pista escondida.
 *  - Tap en pista → revela 🔍 y +2 score.
 *  - Tap en no-pista → revela ❌ visiblemente y -1 score.
 *  - Cuando se encuentran las 3 pistas → +3 bonus, escena completada,
 *    nueva escena con 3 pistas distintas.
 *  - Duración 30s. Score teórico máximo ≈ 12 → scoreMul 0.5..1.5.
 */
private data class Scene(
    val cluePositions: Set<Int>,    // índices 0..24
    val revealed: Set<Int>,
    val foundClues: Int
) {
    val complete: Boolean get() = foundClues >= cluePositions.size
}

@Composable
fun DetectiveJobScreen(
    state: GameState,
    onFinish: (Double) -> Unit,
    onCancel: () -> Unit
) {
    val job = JobId.DETECTIVE
    val totalSeconds = 30
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }

    fun newScene(): Scene {
        val clues = mutableSetOf<Int>()
        while (clues.size < 3) clues.add(rng.nextInt(25))
        return Scene(cluePositions = clues, revealed = emptySet(), foundClues = 0)
    }

    var scene by remember { mutableStateOf(newScene()) }
    var secondsLeft by remember { mutableStateOf(totalSeconds) }
    var score by remember { mutableStateOf(0) }
    var scenesCompleted by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && !done) {
            delay(1000L)
            secondsLeft -= 1
        }
        done = true
    }

    fun tapCell(idx: Int) {
        if (done || scene.complete) return
        if (idx in scene.revealed) return
        val newRevealed = scene.revealed + idx
        if (idx in scene.cluePositions) {
            score += 2
            val newFound = scene.foundClues + 1
            val newScene = scene.copy(revealed = newRevealed, foundClues = newFound)
            if (newScene.complete) {
                score += 3   // bonus por escena completa
                scenesCompleted += 1
            }
            scene = newScene
        } else {
            score -= 1
            scene = scene.copy(revealed = newRevealed)
        }
    }

    // Cuando una escena se completa, después de un instante damos paso a otra.
    LaunchedEffect(scene.complete) {
        if (scene.complete && !done) {
            delay(900L)
            scene = newScene()
        }
    }

    val maxScore = 12
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
                Text("Detective: encuentra las 3 pistas", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap celdas. 🔍 = pista (+2). ❌ = error (-1).",
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
                Column(Modifier.weight(1f)) {
                    Text("Escena", color = Dim, fontSize = 10.sp)
                    Text("${scenesCompleted + 1}",
                        color = Gold,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Pistas: ${scene.foundClues}/3", color = Dim, fontSize = 10.sp)
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

        // Grid 5x5
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF132030))
                .padding(4.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                for (row in 0 until 5) {
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        for (col in 0 until 5) {
                            val idx = row * 5 + col
                            val isRevealed = idx in scene.revealed
                            val isClue = idx in scene.cluePositions
                            val (bg, content) = when {
                                isRevealed && isClue -> Color(0xFF66BB6A) to "🔍"
                                isRevealed && !isClue -> Color(0xFF552B2B) to "❌"
                                else -> Color(0xFF1E3245) to "?"
                            }
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(bg)
                                    .clickable { tapCell(idx) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(content, color = Paper, fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Cada escena resuelta = +3 bonus.",
            color = Dim, fontSize = 11.sp,
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
