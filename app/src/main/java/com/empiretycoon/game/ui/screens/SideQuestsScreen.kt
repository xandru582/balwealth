package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.engine.SideQuestEngine
import com.empiretycoon.game.model.*
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.ProgressBarWithLabel
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney

@Composable
fun SideQuestsScreen(state: GameState, vm: GameViewModel) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab, containerColor = InkSoft, contentColor = Gold) {
            Tab(selected = tab == 0, onClick = { tab = 0 },
                text = { Text("Activas (${state.sideQuests.active.size})") })
            Tab(selected = tab == 1, onClick = { tab = 1 },
                text = { Text("Disponibles (${state.sideQuests.available.size})") })
            Tab(selected = tab == 2, onClick = { tab = 2 },
                text = { Text("Hechas (${state.sideQuests.completed.size})") })
        }
        when (tab) {
            0 -> ActiveTab(state, vm)
            1 -> AvailableTab(state, vm)
            2 -> CompletedTab(state)
        }
    }
}

@Composable
private fun ActiveTab(state: GameState, vm: GameViewModel) {
    val s = state.sideQuests
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        if (s.active.isEmpty()) {
            item {
                EmpireCard {
                    Text("No tienes misiones activas. Pásate por la pestaña de Disponibles.",
                        color = Dim)
                }
            }
        }
        items(s.active, key = { it.id }) { q ->
            ActiveQuestCard(q, state, vm)
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun ActiveQuestCard(q: SideQuest, state: GameState, vm: GameViewModel) {
    val giver = NPCCatalog.byId(q.giverNpcId)
    val (cur, target) = SideQuestEngine.progressOf(q, state)
    val pct = if (target == 0L) 0f else (cur.toFloat() / target).coerceIn(0f, 1f)
    val isComplete = state.sideQuests.completed.contains(q.id)
    val daysLeft = (q.deadlineDay - state.day).coerceAtLeast(0)
    val accent = when {
        isComplete -> Emerald
        daysLeft <= 1 -> Ruby
        else -> Gold
    }
    EmpireCard(borderColor = accent) {
        Row(verticalAlignment = Alignment.Top) {
            Text(giver?.portrait ?: "📋", fontSize = 28.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    Text(q.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${q.difficulty.emoji} ${q.difficulty.displayName}",
                        color = Dim, fontSize = 11.sp)
                }
                if (giver != null) {
                    Text("Encargo de ${giver.name}", color = Dim, fontSize = 11.sp)
                }
                Spacer(Modifier.height(6.dp))
                Text(q.description, color = Paper, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text(objectiveLabel(q.objective), color = Sapphire, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                ProgressBarWithLabel(
                    pct,
                    label = "Progreso $cur / $target  ·  $daysLeft días restantes",
                    color = accent
                )
                Spacer(Modifier.height(8.dp))
                Text("Recompensa: ${rewardLabel(q.reward)}", color = Gold, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Row {
                    if (isComplete) {
                        Button(
                            onClick = { vm.claimSideQuestReward(q.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Emerald, contentColor = Ink),
                            modifier = Modifier.weight(1f)
                        ) { Text("Cobrar") }
                    } else {
                        OutlinedButton(
                            onClick = { vm.abandonSideQuest(q.id) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Abandonar", color = Ruby) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AvailableTab(state: GameState, vm: GameViewModel) {
    val s = state.sideQuests
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                SectionTitle("Encargos disponibles",
                    subtitle = "Se renuevan cada día.")
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { vm.refreshSideQuests() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Sapphire, contentColor = Paper),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Refrescar listado") }
            }
        }
        if (s.available.isEmpty()) {
            item {
                EmpireCard {
                    Text("No hay encargos por ahora. Vuelve mañana.", color = Dim)
                }
            }
        }
        items(s.available, key = { it.id }) { q ->
            AvailableQuestCard(q, vm)
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun AvailableQuestCard(q: SideQuest, vm: GameViewModel) {
    val giver = NPCCatalog.byId(q.giverNpcId)
    val accent = difficultyColor(q.difficulty)
    EmpireCard(borderColor = accent) {
        Row(verticalAlignment = Alignment.Top) {
            Text(giver?.portrait ?: "📋", fontSize = 28.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    Text(q.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${q.difficulty.emoji} ${q.difficulty.displayName}",
                        color = Dim, fontSize = 11.sp)
                }
                if (giver != null) {
                    Text("De ${giver.name}", color = Dim, fontSize = 11.sp)
                }
                Spacer(Modifier.height(6.dp))
                Text(q.description, color = Paper, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text("Objetivo: ${objectiveLabel(q.objective)}",
                    color = Sapphire, fontSize = 12.sp)
                Text("Recompensa: ${rewardLabel(q.reward)}",
                    color = Gold, fontSize = 11.sp)
                Text("Plazo: ${q.expirationDays} días",
                    color = Dim, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { vm.acceptSideQuest(q.id) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent, contentColor = Ink),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Aceptar") }
            }
        }
    }
}

@Composable
private fun CompletedTab(state: GameState) {
    val s = state.sideQuests
    val all = SideQuestCatalog.all
    val done = all.filter { s.completed.contains(it.id) }
    val failed = all.filter { s.failed.contains(it.id) }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        if (done.isEmpty() && failed.isEmpty()) {
            item { EmpireCard { Text("Aún no has terminado ninguna misión.", color = Dim) } }
        }
        if (done.isNotEmpty()) {
            item { SectionTitle("Completadas") }
            items(done, key = { "d_${it.id}" }) { q ->
                EmpireCard(borderColor = Emerald) {
                    Row {
                        Text("✅", fontSize = 20.sp)
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(q.title, fontWeight = FontWeight.SemiBold)
                            Text("Categoría: ${q.category.name}", color = Dim, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        if (failed.isNotEmpty()) {
            item { SectionTitle("Fracasadas") }
            items(failed, key = { "f_${it.id}" }) { q ->
                EmpireCard(borderColor = Ruby) {
                    Row {
                        Text("❌", fontSize = 20.sp)
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(q.title, fontWeight = FontWeight.SemiBold, color = Dim)
                            Text("Caducó", color = Ruby, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

private fun objectiveLabel(o: QuestObjective): String = when (o) {
    is QuestObjective.AccumulateCash -> "Acumular ${o.amount.fmtMoney()}"
    is QuestObjective.ProduceX -> {
        val res = ResourceCatalog.tryById(o.resourceId)?.name ?: o.resourceId
        "Producir ${o.qty} de $res"
    }
    is QuestObjective.SellAtPriceAbove -> {
        val res = ResourceCatalog.tryById(o.resourceId)?.name ?: o.resourceId
        "Vender ${o.qty} de $res a más de ${"%,.0f".format(o.price)} €"
    }
    is QuestObjective.HireRole -> if (o.role == "any_25") "Tener 25 empleados"
        else "Contratar empleado"
    is QuestObjective.ReachLevel -> "Alcanzar nivel ${o.lvl}"
    is QuestObjective.CompleteContracts -> "Completar ${o.n} contratos"
    is QuestObjective.ResearchTech -> "Investigar tecnología ${o.id}"
    is QuestObjective.DonateToCharity -> "Donar ${o.amount.fmtMoney()}"
    is QuestObjective.DefeatRival -> "Vencer al rival"
    is QuestObjective.VisitLocation -> "Visitar ${o.locId}"
    is QuestObjective.PassDays -> "Esperar ${o.n} días"
}

private fun rewardLabel(r: QuestReward): String {
    val parts = mutableListOf<String>()
    if (r.cash != 0.0) parts += r.cash.fmtMoney()
    if (r.xp > 0) parts += "${r.xp} XP"
    if (r.reputation != 0) {
        parts += if (r.reputation > 0) "+${r.reputation} rep" else "${r.reputation} rep"
    }
    if (r.karmaDelta != 0) {
        parts += if (r.karmaDelta > 0) "+${r.karmaDelta} karma" else "${r.karmaDelta} karma"
    }
    if (r.unlockedNpcId != null) parts += "contacto"
    if (r.items.isNotEmpty()) parts += "+items"
    return parts.joinToString(" · ").ifEmpty { "—" }
}

private fun difficultyColor(d: QuestDifficulty): Color = when (d) {
    QuestDifficulty.EASY -> Emerald
    QuestDifficulty.MEDIUM -> Gold
    QuestDifficulty.HARD -> Color(0xFFFF8800)
    QuestDifficulty.LEGENDARY -> Ruby
}
