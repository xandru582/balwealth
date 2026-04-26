package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
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

@Composable
fun MoreScreen(state: GameState, vm: GameViewModel) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab, containerColor = InkSoft, contentColor = Gold) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Misiones") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Inventario") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Ajustes") })
        }
        when (tab) {
            0 -> QuestsTab(state, vm)
            1 -> InventoryTab(state)
            2 -> SettingsTab(state, vm)
        }
    }
}

@Composable
private fun QuestsTab(state: GameState, vm: GameViewModel) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        val ordered = state.quests.sortedBy {
            if (it.claimed) 3
            else if (it.completed) 0
            else 1
        }
        items(ordered, key = { it.id }) { q ->
            val accent = when {
                q.claimed -> Dim
                q.completed -> Emerald
                else -> Gold
            }
            EmpireCard(borderColor = accent) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when {
                            q.claimed -> "✅"
                            q.completed -> "🎯"
                            else -> "📋"
                        }, fontSize = 24.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(q.title, fontWeight = FontWeight.Bold)
                        Text(q.description, color = Dim, fontSize = 12.sp)
                        Text("Objetivo: ${q.goalDescription}", color = Dim, fontSize = 11.sp)
                        val rew = buildList {
                            if (q.rewardCash > 0) add(q.rewardCash.fmtMoney())
                            if (q.rewardXp > 0) add("${q.rewardXp} XP")
                            if (q.rewardReputation > 0) add("+${q.rewardReputation} rep")
                        }.joinToString(" · ")
                        if (rew.isNotEmpty())
                            Text("Recompensa: $rew", color = Gold, fontSize = 11.sp)
                    }
                    if (q.completed && !q.claimed) {
                        Button(
                            onClick = { vm.claim(q.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Emerald, contentColor = Ink)
                        ) { Text("Cobrar") }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun InventoryTab(state: GameState) {
    val nonZero = state.company.inventory.filter { it.value > 0 }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                SectionTitle("Almacén",
                    subtitle = "${state.company.inventoryCount()}/${state.company.effectiveCapacity()} uds.")
            }
        }
        if (nonZero.isEmpty()) {
            item { Text("Almacén vacío.", color = Dim, modifier = Modifier.padding(16.dp)) }
        } else {
            items(nonZero.entries.toList(), key = { it.key }) { (id, qty) ->
                val res = ResourceCatalog.tryById(id) ?: return@items
                EmpireCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(res.emoji, fontSize = 24.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(res.name, fontWeight = FontWeight.SemiBold)
                            Text(res.category.name, color = Dim, fontSize = 10.sp)
                        }
                        Text("x$qty", fontWeight = FontWeight.Bold, color = Gold,
                            fontSize = 18.sp)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun SettingsTab(state: GameState, vm: GameViewModel) {
    var pName by remember { mutableStateOf(state.player.name) }
    var cName by remember { mutableStateOf(state.company.name) }
    var showReset by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                SectionTitle("Identidad")
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = pName, onValueChange = { pName = it },
                    label = { Text("Tu nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = cName, onValueChange = { cName = it },
                    label = { Text("Nombre empresa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { vm.rename(pName, cName) },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)
                ) { Text("Guardar") }
            }
        }
        item {
            EmpireCard {
                SectionTitle("Velocidad de juego",
                    subtitle = "Actual: ${state.speedMultiplier}x")
                Spacer(Modifier.height(6.dp))
                Row {
                    listOf(1, 2, 4, 8).forEach { s ->
                        Button(
                            onClick = { vm.setSpeed(s) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (s == state.speedMultiplier) Gold else InkBorder,
                                contentColor = if (s == state.speedMultiplier) Ink else Paper
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text("${s}x") }
                        Spacer(Modifier.width(4.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { vm.togglePause() },
                    colors = ButtonDefaults.buttonColors(containerColor = Sapphire, contentColor = Paper),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (state.paused) "Reanudar" else "Pausar") }
            }
        }
        item {
            EmpireCard {
                SectionTitle("Datos y ayuda")
                Spacer(Modifier.height(6.dp))
                Text("Empire Tycoon v1.0.0 — juego de simulación de negocios.",
                    color = Dim, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text("El juego se guarda automáticamente cada 10s y al cerrar.",
                    color = Dim, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { showReset = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Ruby, contentColor = Paper)
                ) { Text("Borrar partida") }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.resetAll()
                    showReset = false
                }) { Text("Borrar", color = Ruby) }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text("Cancelar") }
            },
            title = { Text("¿Borrar partida?") },
            text = { Text("Se perderá todo el progreso. Esta acción no se puede deshacer.",
                color = Dim) },
            containerColor = InkSoft
        )
    }
}
