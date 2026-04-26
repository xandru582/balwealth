package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.model.*
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.ProgressBarWithLabel
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney
import com.empiretycoon.game.util.fmtTimeSeconds

@Composable
fun EmpireScreen(state: GameState, vm: GameViewModel) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = tab,
            containerColor = InkSoft,
            contentColor = Gold
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 },
                text = { Text("Edificios") })
            Tab(selected = tab == 1, onClick = { tab = 1 },
                text = { Text("Construir") })
            Tab(selected = tab == 2, onClick = { tab = 2 },
                text = { Text("Empleados") })
        }
        when (tab) {
            0 -> BuildingsTab(state, vm)
            1 -> ConstructTab(state, vm)
            2 -> EmployeesTab(state, vm)
        }
    }
}

// ========== Buildings ==========

@Composable
private fun BuildingsTab(state: GameState, vm: GameViewModel) {
    val buildings = state.company.buildings
    if (buildings.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Aún no tienes instalaciones", color = Dim)
            Spacer(Modifier.height(4.dp))
            Text("Cambia a 'Construir' para empezar.", color = Dim, fontSize = 12.sp)
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(buildings, key = { it.id }) { b -> BuildingCard(b, state, vm) }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun BuildingCard(b: Building, state: GameState, vm: GameViewModel) {
    var pickRecipe by remember(b.id) { mutableStateOf(false) }
    val recipe = b.currentRecipeId?.let { AdvancedRecipeCatalog.byId(it) }
    val frac = if (recipe == null) 0f
        else (b.progressSeconds / recipe.seconds.toDouble()).toFloat()

    EmpireCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(b.type.emoji, fontSize = 28.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(b.name, fontWeight = FontWeight.Bold)
                Text("Trabajadores ${b.assignedWorkers}/${b.workerCapacity} · Prod x${"%.2f".format(b.productivity)}",
                    color = Dim, fontSize = 12.sp)
            }
            TextButton(onClick = { vm.upgrade(b.id) }) {
                Text("Mejorar", color = Gold)
            }
        }
        Spacer(Modifier.height(6.dp))
        if (recipe == null) {
            Button(
                onClick = { pickRecipe = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Sapphire, contentColor = Paper)
            ) { Text("Asignar receta") }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(recipe.name, fontWeight = FontWeight.SemiBold)
                    Text("Ciclo ${recipe.seconds}s · auto ${if (b.autoRestart) "ON" else "OFF"}",
                        color = Dim, fontSize = 11.sp)
                }
                TextButton(onClick = { pickRecipe = true }) {
                    Text("Cambiar", color = Sapphire)
                }
            }
            Spacer(Modifier.height(6.dp))
            ProgressBarWithLabel(
                frac,
                label = "Progreso ${b.progressSeconds.toInt()} / ${recipe.seconds}s",
                color = Emerald
            )
            Spacer(Modifier.height(8.dp))
            Row {
                recipe.inputs.forEach { (id, q) ->
                    val res = ResourceCatalog.byId(id)
                    Chip(label = "${res.emoji} -$q ${res.name}", Ruby)
                    Spacer(Modifier.width(4.dp))
                }
                recipe.outputs.forEach { (id, q) ->
                    val res = ResourceCatalog.byId(id)
                    Chip(label = "${res.emoji} +$q ${res.name}", Emerald)
                    Spacer(Modifier.width(4.dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row {
            OutlinedButton(onClick = { vm.removeWorker(b.id) }) { Text("-1 👤") }
            Spacer(Modifier.width(4.dp))
            OutlinedButton(onClick = { vm.addWorker(b.id) }) { Text("+1 👤") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { vm.toggleAutoRestart(b.id) }) {
                Text(if (b.autoRestart) "Auto ON" else "Auto OFF", color = Dim)
            }
            TextButton(onClick = { vm.demolish(b.id) }) {
                Text("Vender", color = Ruby)
            }
        }
        if (recipe != null) {
            Text("Nivel siguiente coste: ${b.type.costAtLevel(b.level + 1).fmtMoney()}",
                color = Dim, fontSize = 11.sp)
        }
    }

    if (pickRecipe) {
        RecipePicker(
            buildingType = b.type,
            research = state.research,
            onPick = {
                vm.setRecipe(b.id, it)
                pickRecipe = false
            },
            onClose = { pickRecipe = false }
        )
    }
}

@Composable
private fun Chip(label: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) { Text(label, fontSize = 11.sp, color = color) }
}

@Composable
private fun RecipePicker(
    buildingType: BuildingType,
    research: ResearchState,
    onPick: (String?) -> Unit,
    onClose: () -> Unit
) {
    val recipes = AdvancedRecipeCatalog.forBuilding(buildingType)
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text("Cerrar", color = Gold) } },
        dismissButton = { TextButton(onClick = { onPick(null) }) { Text("Parar", color = Ruby) } },
        title = { Text("Asignar receta") },
        text = {
            LazyColumn {
                items(recipes, key = { it.id }) { r ->
                    val locked = r.requiredResearch != null &&
                        !research.isCompleted(r.requiredResearch)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (locked) InkBorder.copy(alpha = 0.3f) else InkBorder.copy(alpha = 0.5f))
                            .clickable(enabled = !locked) { onPick(r.id) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(r.name, fontWeight = FontWeight.SemiBold,
                                color = if (locked) Dim else Paper)
                            Text("Ciclo ${r.seconds}s", color = Dim, fontSize = 11.sp)
                            val inStr = r.inputs.entries.joinToString(", ") {
                                "${ResourceCatalog.byId(it.key).name} x${it.value}"
                            }.ifBlank { "sin insumos" }
                            val outStr = r.outputs.entries.joinToString(", ") {
                                "${ResourceCatalog.byId(it.key).name} x${it.value}"
                            }
                            Text("⟵ $inStr", color = Dim, fontSize = 11.sp)
                            Text("⟶ $outStr", color = Emerald, fontSize = 11.sp)
                        }
                        if (locked) Icon(Icons.Filled.Lock, null, tint = Ruby)
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        },
        containerColor = InkSoft
    )
}

// ========== Construct ==========

@Composable
private fun ConstructTab(state: GameState, vm: GameViewModel) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(BuildingType.values()) { type ->
            val existing = state.company.buildings.count { it.type == type }
            val cost = type.costAtLevel(existing + 1)
            EmpireCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(type.emoji, fontSize = 30.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(type.displayName, fontWeight = FontWeight.Bold)
                        Text(type.description, color = Dim, fontSize = 12.sp)
                        Text("Capacidad trabajadores: ${type.workerCapacityBase}+",
                            color = Dim, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(cost.fmtMoney(), fontWeight = FontWeight.Bold,
                            color = if (state.company.cash >= cost) Gold else Ruby)
                        Text("#${existing + 1}", color = Dim, fontSize = 10.sp)
                        Spacer(Modifier.height(4.dp))
                        Button(
                            enabled = state.company.cash >= cost,
                            onClick = { vm.build(type) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Gold, contentColor = Ink,
                                disabledContainerColor = InkBorder,
                                disabledContentColor = Dim
                            )
                        ) { Text("Construir") }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

// ========== Employees ==========

@Composable
private fun EmployeesTab(state: GameState, vm: GameViewModel) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Plantilla", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge)
                        Text("${state.company.employees.size} empleados · gasto mensual ${state.company.totalSalaries.fmtMoney()}",
                            color = Dim, fontSize = 12.sp)
                    }
                    TextButton(onClick = { vm.refreshCandidates() }) {
                        Text("Nuevos candidatos", color = Sapphire)
                    }
                }
            }
        }

        item { SectionTitle("Tu equipo") }
        if (state.company.employees.isEmpty()) {
            item { Text("Nadie contratado aún.", color = Dim, modifier = Modifier.padding(16.dp)) }
        } else {
            items(state.company.employees, key = { it.id }) { e ->
                EmpireCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🧑‍💼", fontSize = 22.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.name, fontWeight = FontWeight.SemiBold)
                            Text("Skill ${"%.2f".format(e.skill)} · Lealtad ${(e.loyalty*100).toInt()}%",
                                color = Dim, fontSize = 11.sp)
                            Text("Sueldo ${e.monthlySalary.fmtMoney()}/mes",
                                color = Dim, fontSize = 11.sp)
                            if (e.assignedBuildingId != null) {
                                val b = state.company.buildings.find { it.id == e.assignedBuildingId }
                                Text("Asignado a: ${b?.name ?: "—"}", color = Emerald, fontSize = 11.sp)
                            } else {
                                Text("Libre", color = Dim, fontSize = 11.sp)
                            }
                        }
                        TextButton(onClick = { vm.fire(e.id) }) {
                            Text("Despedir", color = Ruby)
                        }
                    }
                }
            }
        }

        item { SectionTitle("Candidatos") }
        items(state.candidates, key = { it.id }) { c ->
            EmpireCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🧑", fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, fontWeight = FontWeight.SemiBold)
                        Text("Skill ${"%.2f".format(c.skill)} · ${c.monthlySalary.fmtMoney()}/mes",
                            color = Dim, fontSize = 11.sp)
                        Text("Prima de fichaje: ${(c.monthlySalary * 0.5).fmtMoney()}",
                            color = Dim, fontSize = 11.sp)
                    }
                    Button(
                        onClick = { vm.hire(c.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Ink)
                    ) { Text("Fichar") }
                }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}
