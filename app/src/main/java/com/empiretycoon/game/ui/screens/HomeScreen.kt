package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.model.*
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.ProgressBarWithLabel
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.components.StatPill
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney

@Composable
fun HomeScreen(state: GameState, vm: GameViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item { Greeting(state) }
        item { Summary(state) }
        item { PendingQuests(state, vm) }
        item { RecentActivity(state) }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun Greeting(state: GameState) {
    val hour = state.hourOfDay
    val salute = when (hour) {
        in 5..11 -> "Buenos días"
        in 12..19 -> "Buenas tardes"
        else -> "Buenas noches"
    }
    EmpireCard {
        Text("$salute, ${state.player.name}", fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text("Día ${state.day} · Hora ${"%02d".format(hour)}:00", color = Dim)
        Spacer(Modifier.height(10.dp))
        val xpFrac = if (state.company.xpForNextLevel() == 0L) 0f
            else state.company.xp.toFloat() / state.company.xpForNextLevel()
        ProgressBarWithLabel(xpFrac,
            label = "XP empresa · Nivel ${state.company.level}",
            color = Emerald)
        Spacer(Modifier.height(8.dp))
        val playerFrac = if (state.player.xpForNextLevel() == 0L) 0f
            else state.player.xp.toFloat() / state.player.xpForNextLevel()
        ProgressBarWithLabel(playerFrac,
            label = "XP personaje · Nivel ${state.player.level}",
            color = Gold)
    }
}

@Composable
private fun Summary(state: GameState) {
    EmpireCard {
        SectionTitle("Resumen")
        Spacer(Modifier.height(8.dp))
        SummaryPills(state)
    }
}

@Composable
private fun SummaryPills(state: GameState) {
    val c = state.company
    val activeBuildings = c.buildings.count { it.currentRecipeId != null && it.assignedWorkers > 0 }
    val inv = c.inventoryCount()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPill("Edificios", "${c.buildings.size}", "\uD83C\uDFED", Modifier.weight(1f))
            StatPill("Activos", "$activeBuildings", "\u25B6\uFE0F", Modifier.weight(1f))
            StatPill("Empleados", "${c.employees.size}", "\uD83D\uDC65", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPill("Almacén", "$inv/${c.effectiveCapacity()}", "\uD83D\uDCE6", Modifier.weight(1f))
            StatPill("Reputación", "${c.reputation}/100", "\uD83C\uDFC6", Modifier.weight(1f))
            StatPill("Rentas/día", state.realEstate.dailyNet.fmtMoney(), "\uD83C\uDFE0", Modifier.weight(1f))
        }
    }
}

@Composable
private fun PendingQuests(state: GameState, vm: GameViewModel) {
    val pending = state.quests.filter { it.completed && !it.claimed }
    val open = state.quests.filter { !it.completed }.take(3)

    EmpireCard {
        SectionTitle("Misiones",
            subtitle = "${state.quests.count { it.claimed }}/${state.quests.size} completadas")

        if (pending.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Listas para cobrar", color = Emerald, fontWeight = FontWeight.Bold)
            pending.forEach { q ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(q.title, fontWeight = FontWeight.Bold)
                        Text("+${q.rewardXp} XP · ${q.rewardCash.fmtMoney()} · +${q.rewardReputation} rep",
                            style = MaterialTheme.typography.labelSmall, color = Dim)
                    }
                    Button(
                        onClick = { vm.claim(q.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Ink)
                    ) { Text("Cobrar") }
                }
            }
        }

        if (open.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("En curso", color = Dim, fontWeight = FontWeight.Bold)
            open.forEach { q ->
                Column(Modifier.padding(vertical = 6.dp)) {
                    Text(q.title, fontWeight = FontWeight.SemiBold)
                    Text(q.goalDescription,
                        style = MaterialTheme.typography.bodyMedium, color = Dim)
                }
            }
        }
    }
}

@Composable
private fun RecentActivity(state: GameState) {
    EmpireCard {
        SectionTitle("Actividad reciente")
        if (state.notifications.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Todo en calma por aquí. Construye, produce, vende.", color = Dim)
        } else {
            state.notifications.reversed().take(6).forEach { n ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (n.kind) {
                                    NotificationKind.SUCCESS -> Emerald
                                    NotificationKind.WARNING -> Gold
                                    NotificationKind.ERROR -> Ruby
                                    NotificationKind.EVENT -> Sapphire
                                    NotificationKind.ECONOMY -> Gold
                                    NotificationKind.INFO -> Dim
                                }
                            )
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(n.title, fontWeight = FontWeight.SemiBold)
                        Text(n.message, color = Dim,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
