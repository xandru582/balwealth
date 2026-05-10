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
 * Mini-juego del Policía: tap-reaction sobre sospechosos.
 *
 * Mecánica:
 *  - Duración 30 segundos in-game (real ms).
 *  - Cada ~700 ms aparece un objetivo en posición aleatoria del área.
 *    65% son sospechosos (rojos), 35% civiles (verdes).
 *  - Sospechoso vivo 1.6s. Tap = +1 score.
 *  - Civil vivo 1.6s. Tap por error = -2 score.
 *  - Score final 0..30 → scoreMul = 0.5 + (score/20).coerceAtMost(1.0).
 *
 * Cuando termina, llama onFinish(scoreMul). Cancelable con onCancel
 * (vuelve al hub sin descontar energía ni cobrar).
 */
private data class PoliceTarget(
    val id: Long,
    val xPct: Float,           // 0..1 — relativo al área
    val yPct: Float,           // 0..1
    val isSuspect: Boolean,
    val expiresAtMs: Long
)

@Composable
fun PoliceJobScreen(
    state: GameState,
    onFinish: (Double) -> Unit,
    onCancel: () -> Unit
) {
    val job = JobId.POLICE_OFFICER
    val totalSeconds = 30
    val previewBaseWage = JobsEngine.previewWage(state, job)

    var secondsLeft by remember { mutableStateOf(totalSeconds) }
    var score by remember { mutableStateOf(0) }
    var targets by remember { mutableStateOf<List<PoliceTarget>>(emptyList()) }
    var done by remember { mutableStateOf(false) }
    val rng = remember { Random(System.currentTimeMillis()) }

    // Timer countdown
    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && !done) {
            delay(1000L)
            secondsLeft -= 1
        }
        done = true
    }

    // Spawner
    LaunchedEffect(Unit) {
        while (!done) {
            delay(700L)
            val now = System.currentTimeMillis()
            val isSuspect = rng.nextDouble() < 0.65
            val nt = PoliceTarget(
                id = now,
                xPct = 0.05f + rng.nextFloat() * 0.85f,
                yPct = 0.10f + rng.nextFloat() * 0.80f,
                isSuspect = isSuspect,
                expiresAtMs = now + 1600
            )
            targets = (targets + nt).filter { it.expiresAtMs > now }
        }
    }

    // Cleanup
    LaunchedEffect(Unit) {
        while (!done) {
            delay(120L)
            val now = System.currentTimeMillis()
            targets = targets.filter { it.expiresAtMs > now }
        }
    }

    val maxScore = 20
    val scoreMul = if (done) {
        (0.5 + (score.toFloat() / maxScore).coerceIn(0f, 1f)).toDouble()
    } else 0.5

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .padding(12.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(job.emoji, fontSize = 26.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Policía: patrulla", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gold)
                Text("Tap sospechosos rojos · evita civiles verdes",
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

        // Score + wage preview
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

        // Game area — Box con targets posicionados
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF132030))
        ) {
            // Renderizamos solo los targets activos
            val now = System.currentTimeMillis()
            for (t in targets) {
                if (t.expiresAtMs <= now) continue
                val color = if (t.isSuspect) Color(0xFFFF5252) else Color(0xFF66BB6A)
                val emoji = if (t.isSuspect) "🚨" else "👤"
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(0.dp)
                ) {
                    // Posición absoluta proporcional al tamaño del Box.
                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                x = (t.xPct * 0.92f * 200).dp,  // approx — relative space
                                y = (t.yPct * 0.92f * 320).dp
                            )
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(color.copy(alpha = 0.85f))
                            .clickable {
                                if (done) return@clickable
                                if (t.isSuspect) score += 1 else score -= 2
                                targets = targets.filterNot { it.id == t.id }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 22.sp)
                    }
                }
            }

            if (done) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xCC000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🚓 Patrulla terminada",
                            color = Color.White, fontSize = 22.sp,
                            fontWeight = FontWeight.Black)
                        Text("Score: $score / $maxScore",
                            color = Color.White, fontSize = 14.sp)
                        Text("Wage: ${(previewBaseWage * scoreMul).fmtMoney()}",
                            color = Gold, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Acciones
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
            "Tip: cada sospechoso atrapado vale +1, cada civil tap por error vale -2.",
            color = Dim, fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }
}
