package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.model.*
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney

/** Pantalla de retos diarios y semanales. */
@Composable
fun DailyChallengesScreen(state: GameState, vm: GameViewModel) {
    val dc = state.dailyChallenges

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SectionTitle(
            "🎯 Retos diarios",
            subtitle = "3 retos cada 24h. Completa todos para subir la racha."
        )
        Spacer(Modifier.height(8.dp))

        // Streak banner
        EmpireCard(borderColor = Gold) {
            Text("🔥 Racha: ${dc.streakDays} días", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gold)
            val mul = streakMultiplier(dc.streakDays)
            Text(
                "Multiplicador actual: ×${"%.1f".format(mul)} · mejor racha: ${dc.bestStreak} días",
                color = Dim, fontSize = 12.sp
            )
            Text(
                "Lifetime: ${dc.totalCompleted} retos · ${dc.totalEarnedCash.fmtMoney()} ganados",
                color = Dim, fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(12.dp))

        // Daily challenges
        if (dc.daily.isEmpty()) {
            EmpireCard {
                Text("Esperando al amanecer in-game…", color = Dim, fontSize = 12.sp)
            }
        } else {
            for (c in dc.daily) {
                ChallengeCard(c, vm)
                Spacer(Modifier.height(8.dp))
            }
        }

        // Weekly challenge
        Spacer(Modifier.height(12.dp))
        SectionTitle("👑 Reto semanal")
        Spacer(Modifier.height(6.dp))
        val weekly = dc.weekly
        if (weekly == null) {
            EmpireCard {
                Text("Aún sin reto semanal asignado.", color = Dim, fontSize = 12.sp)
            }
        } else {
            ChallengeCard(weekly, vm, accent = Gold)
        }
    }
}

@Composable
private fun ChallengeCard(c: Challenge, vm: GameViewModel, accent: androidx.compose.ui.graphics.Color = Sapphire) {
    EmpireCard(borderColor = if (c.completed) Emerald else accent) {
        Text(c.title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(c.description, color = Dim, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { c.percent },
            modifier = Modifier.fillMaxWidth().height(6.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text("${c.progress} / ${c.target}", color = Dim, fontSize = 11.sp)
        Text(
            "Recompensa: ${c.rewardCash.fmtMoney()} · ${c.rewardXp} XP" +
                (if (c.rewardReputation > 0) " · +${c.rewardReputation} rep" else "") +
                (if (c.rewardKarma != 0) " · ${if (c.rewardKarma > 0) "+" else ""}${c.rewardKarma} karma" else ""),
            color = Gold, fontSize = 11.sp
        )
        if (c.completed && !c.claimed) {
            TextButton(onClick = { vm.claimChallenge(c.id) }) {
                Text("Reclamar recompensa", color = Emerald, fontWeight = FontWeight.Bold)
            }
        } else if (c.claimed) {
            Text("✅ Reclamado", color = Emerald, fontSize = 12.sp)
        }
    }
}

private fun streakMultiplier(streak: Int): Double = when {
    streak <= 0 -> 1.0
    streak <= 2 -> 1.2
    streak <= 5 -> 1.6
    streak <= 10 -> 2.4
    streak <= 20 -> 3.6
    else -> 5.0
}
