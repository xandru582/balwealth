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
 * Mini-juego del Futbolista — penaltis.
 *
 * Mecánica:
 *  - 3 zonas del arco: IZQ, CENTRO, DCHA.
 *  - Cada ronda el portero salta a una zona random (visualmente).
 *  - El jugador tap la zona donde chutar. Si difiere de la del
 *    portero → ¡gol! +1 score. Si coinciden → atajada, -1 score.
 *  - 30s. Score teórico ~12 → scoreMul 0.5..1.5.
 */
private enum class GoalZone(val emoji: String, val displayName: String) {
    LEFT("⬅️", "Izquierda"),
    CENTER("⬆️", "Centro"),
    RIGHT("➡️", "Derecha")
}

@Composable
fun FootballJobScreen(
    state: GameState,
    onFinish: (Double) -> Unit,
    onCancel: () -> Unit
) {
    val job = JobId.FOOTBALL_PLAYER
    val totalSeconds = 30
    val previewBaseWage = JobsEngine.previewWage(state, job)
    val rng = remember { Random(System.currentTimeMillis()) }

    var keeperZone by remember { mutableStateOf(GoalZone.values().random(rng)) }
    var secondsLeft by remember { mutableStateOf(totalSeconds) }
    var score by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<String?>(null) }   // "goal"/"saved"
    var roundCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && !done) {
            delay(1000L)
            secondsLeft -= 1
        }
        done = true
    }

    fun tapZone(z: GoalZone) {
        if (done || lastResult != null) return
        if (z != keeperZone) {
            score += 1
            lastResult = "goal"
        } else {
            score -= 1
            lastResult = "saved"
        }
    }

    LaunchedEffect(lastResult) {
        if (lastResult != null) {
            delay(900L)
            keeperZone = GoalZone.values().random(rng)
            roundCount += 1
            lastResult = null
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
                Text("Futbolista: chuta penaltis", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap la zona OPUESTA al portero",
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
                    Text("Goles", color = Dim, fontSize = 10.sp)
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
        Spacer(Modifier.height(20.dp))

        // Arco con portero
        EmpireCard(borderColor = Color(0xFF2C3E50)) {
            Column(Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🥅 Arco", color = Dim, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (zone in GoalZone.values()) {
                        Box(
                            Modifier
                                .padding(4.dp)
                                .size(width = 80.dp, height = 100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (zone == keeperZone) Color(0xFFFFCDD2)
                                    else Color(0xFF1B2738)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (zone == keeperZone) {
                                Text("🧤", fontSize = 36.sp)
                            } else {
                                Text("　", fontSize = 36.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                lastResult?.let { res ->
                    val (label, color) = when (res) {
                        "goal" -> "🎉 ¡GOL!" to Emerald
                        else -> "🧤 Atajada..." to Color(0xFFFF7A7A)
                    }
                    Text(label, color = color, fontSize = 18.sp,
                        fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(Modifier.weight(1f))

        // Botones de zona
        Text("Tap zona donde chutar:", color = Dim, fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            for (zone in GoalZone.values()) {
                Box(
                    Modifier
                        .weight(1f)
                        .padding(4.dp)
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(InkBorder)
                        .clickable { tapZone(zone) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(zone.emoji, fontSize = 30.sp)
                        Text(zone.displayName, color = Paper, fontSize = 11.sp)
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
