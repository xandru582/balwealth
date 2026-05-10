package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.engine.SeasonsEngine
import com.empiretycoon.game.model.*
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney

/**
 * Pantalla de Event Seasons. Muestra la temporada actual + countdown,
 * el calendario completo con días, y las recompensas obtenidas.
 */
@Composable
fun SeasonsScreen(state: GameState, vm: GameViewModel) {
    val ss = state.seasons
    val active = ss.activeSeason
    val mods = ss.activeModifiers
    val daysLeft = SeasonsEngine.daysLeftInActiveSeason(state)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SectionTitle(
            "🎭 Temporadas",
            subtitle = "Ciclo de 30 días: 4 temporadas + temporada baja. Modifican el juego."
        )
        Spacer(Modifier.height(12.dp))

        // ----- Temporada activa -----
        EmpireCard(borderColor = Gold) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(InkBorder),
                    contentAlignment = Alignment.Center
                ) {
                    Text(active.emoji, fontSize = 36.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Activa ahora", color = Dim, fontSize = 10.sp)
                    Text(
                        active.displayName,
                        fontWeight = FontWeight.Black, color = Gold, fontSize = 22.sp
                    )
                    Text(active.tagline, color = Paper, fontSize = 12.sp)
                    Text(
                        "Día del ciclo: ${ss.cycleDay}/${SeasonsCatalog.CYCLE_DAYS} · " +
                            "termina en ${daysLeft} días",
                        color = Sapphire, fontSize = 11.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // ----- Modifiers activos -----
        SectionTitle("⚙️ Modificadores activos")
        Spacer(Modifier.height(6.dp))
        EmpireCard {
            ModifierRow("Recompensa eventos al pisar tile", mods.worldEventRewardMul)
            ModifierRow("Multiplicador venta mercado", mods.marketSellMul)
            ModifierRow("XP del jugador", mods.playerXpMul)
            ModifierRow("Renta de inmuebles", mods.realEstateRentMul)
            if (mods.snowfall) Text("❄️  Nevada visual activa", color = Sapphire, fontSize = 12.sp)
            if (mods.fireworks) Text("🎆  Fuegos artificiales nocturnos", color = Gold, fontSize = 12.sp)
            if (mods == SeasonModifiers.NEUTRAL) {
                Text("Sin efectos activos esta temporada.", color = Dim, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))

        // ----- Calendario completo -----
        SectionTitle("📅 Calendario del ciclo")
        Spacer(Modifier.height(6.dp))
        for (s in SeasonId.values()) {
            CalendarRow(
                season = s,
                isActive = s == active,
                cycleDay = ss.cycleDay,
                daysUntil = SeasonsEngine.daysUntil(state, s),
                completed = ss.completedCount[s.name] ?: 0
            )
            Spacer(Modifier.height(6.dp))
        }

        // ----- Recompensas -----
        Spacer(Modifier.height(12.dp))
        SectionTitle(
            "🎁 Recompensas obtenidas",
            subtitle = "Una vez por temporada lifetime."
        )
        Spacer(Modifier.height(6.dp))
        if (ss.claimedRewards.isEmpty()) {
            EmpireCard {
                Text("Aún sin recompensas. Vive una temporada completa para reclamar la primera.",
                    color = Dim, fontSize = 12.sp)
            }
        } else {
            for (r in ss.claimedRewards) {
                RewardRow(r)
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun ModifierRow(label: String, value: Double) {
    val pct = ((value - 1.0) * 100).toInt()
    val color = when {
        value > 1.0 -> Emerald
        value < 1.0 -> Color(0xFFFF7A7A)
        else -> Dim
    }
    val display = when {
        value == 1.0 -> "—"
        pct > 0 -> "+$pct%"
        else -> "$pct%"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Paper, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(display, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun CalendarRow(
    season: SeasonId,
    isActive: Boolean,
    cycleDay: Int,
    daysUntil: Int,
    completed: Int
) {
    EmpireCard(borderColor = if (isActive) Gold else InkBorder) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(InkBorder),
                contentAlignment = Alignment.Center
            ) {
                Text(season.emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    season.displayName,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isActive) Gold else Paper,
                    fontSize = 14.sp
                )
                Text(
                    "Días ${season.startDay}–${season.startDay + season.durationDays - 1} · ${season.durationDays}d total",
                    color = Dim, fontSize = 11.sp
                )
                Text(season.tagline, color = Sapphire, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (isActive) {
                    Text("AHORA", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("en $daysUntil d", color = Dim, fontSize = 11.sp)
                }
                Text("×$completed", color = Emerald, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun RewardRow(r: SeasonReward) {
    EmpireCard(borderColor = Emerald) {
        Text(r.title, fontWeight = FontWeight.Bold, color = Gold, fontSize = 14.sp)
        Text(r.description, color = Paper, fontSize = 12.sp)
        Text(
            "Bonus: ${r.cashBonus.fmtMoney()} · ${r.xpBonus} XP",
            color = Emerald, fontSize = 11.sp
        )
    }
}
