package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.empiretycoon.game.model.*
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*

/**
 * Pantalla "Líneas de producción". Permite gestionar líneas existentes
 * y crear nuevas a partir de presets del catálogo.
 */
@Composable
fun LinesScreen(state: GameState, vm: GameViewModel) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab, containerColor = InkSoft, contentColor = Gold) {
            Tab(selected = tab == 0, onClick = { tab = 0 },
                text = { Text("Mis líneas") })
            Tab(selected = tab == 1, onClick = { tab = 1 },
                text = { Text("Crear línea") })
        }
        when (tab) {
            0 -> MyLinesTab(state, vm)
            1 -> CreateLineTab(state, vm, onCreated = { tab = 0 })
        }
    }
}

// ---------- Mis líneas ----------

@Composable
private fun MyLinesTab(state: GameState, vm: GameViewModel) {
    val lines = state.productionLines.lines
    if (lines.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Aún no tienes líneas de producción", color = Dim)
            Spacer(Modifier.height(4.dp))
            Text("Cambia a 'Crear línea' para empezar.", color = Dim, fontSize = 12.sp)
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(lines, key = { it.id }) { line -> LineCard(line, state, vm) }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun LineCard(line: ProductionLine, state: GameState, vm: GameViewModel) {
    val accent = if (line.enabled) Emerald else Dim
    EmpireCard(borderColor = accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(line.name, fontWeight = FontWeight.Bold)
                Text(
                    "${line.balancingMode.emoji} ${line.balancingMode.displayName} · " +
                            "${line.buildingIds.size} etapas",
                    color = Dim, fontSize = 11.sp
                )
            }
            Switch(
                checked = line.enabled,
                onCheckedChange = { vm.toggleLine(line.id) }
            )
        }
        Spacer(Modifier.height(8.dp))

        // Visual de la cadena: emoji edificio -> emoji edificio
        LineChainVisual(line, state)

        Spacer(Modifier.height(8.dp))

        // Throughput aproximado: receta más lenta determina cadencia
        val slowest = line.buildingIds.mapNotNull { bid ->
            line.recipeIdsPerBuilding[bid]?.let { rid ->
                AdvancedRecipeCatalog.byId(rid)?.seconds
            }
        }.maxOrNull() ?: 0
        Text(
            "Cadencia (cuello de botella): ${slowest}s",
            color = Sapphire, fontSize = 11.sp
        )

        // Estado: ¿alguno parado?
        val statuses = line.buildingIds.map { bid ->
            val b = state.company.buildings.firstOrNull { it.id == bid }
            when {
                b == null -> "❌ falta"
                b.assignedWorkers == 0 -> "⏸ sin operarios"
                b.currentRecipeId == null -> "⏹ sin receta"
                else -> "▶ produciendo"
            }
        }
        Text(
            statuses.joinToString(" · "),
            color = Dim, fontSize = 11.sp
        )

        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedButton(onClick = { vm.toggleLine(line.id) }) {
                Text(if (line.enabled) "Pausar" else "Activar")
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { vm.deleteLine(line.id) }) {
                Text("Eliminar", color = Ruby)
            }
        }
    }
}

@Composable
private fun LineChainVisual(line: ProductionLine, state: GameState) {
    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(line.buildingIds.size) { idx ->
            val bid = line.buildingIds[idx]
            val b = state.company.buildings.firstOrNull { it.id == bid }
            val recipeId = line.recipeIdsPerBuilding[bid]
            val recipe = recipeId?.let { AdvancedRecipeCatalog.byId(it) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(InkBorder.copy(alpha = 0.6f))
                        .border(1.dp, Sapphire.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            b?.type?.emoji ?: "❓",
                            fontSize = 22.sp
                        )
                        Text(
                            recipe?.name ?: "—",
                            color = Paper, fontSize = 9.sp
                        )
                    }
                }
                if (idx < line.buildingIds.lastIndex) {
                    Spacer(Modifier.width(2.dp))
                    Text("→", color = Gold, fontSize = 18.sp,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(2.dp))
                }
            }
        }
    }
}

// ---------- Crear línea ----------

@Composable
private fun CreateLineTab(
    state: GameState,
    vm: GameViewModel,
    onCreated: () -> Unit
) {
    var selectedPresetId by rememberSaveable { mutableStateOf<String?>(null) }
    var assignments by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var balancingName by rememberSaveable { mutableStateOf(BalancingMode.JUST_IN_TIME.name) }
    var customName by rememberSaveable { mutableStateOf("") }

    val preset = selectedPresetId?.let { LinePresetCatalog.byId(it) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item { SectionTitle("1. Elige un preset") }
        items(LinePresetCatalog.all, key = { it.id }) { p ->
            val selected = p.id == selectedPresetId
            EmpireCard(
                borderColor = if (selected) Gold else InkBorder
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedPresetId = p.id
                            assignments = List(p.requiredBuildingTypes.size) { "" }
                            balancingName = p.recommendedBalancing.name
                            customName = p.name
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(p.emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name, fontWeight = FontWeight.Bold,
                            color = if (selected) Gold else Paper)
                        Text(p.description, color = Dim, fontSize = 12.sp)
                        Text("Necesita: ${p.requiredBuildingTypes.joinToString(", ") { it.displayName }}",
                            color = Dim, fontSize = 10.sp)
                    }
                }
            }
        }
        if (preset != null) {
            item { SectionTitle("2. Asigna tus edificios") }
            items(preset.requiredBuildingTypes.size) { idx ->
                val needed = preset.requiredBuildingTypes[idx]
                val candidates = state.company.buildings.filter { it.type == needed }
                EmpireCard {
                    Text("Etapa ${idx + 1}: ${needed.emoji} ${needed.displayName}",
                        fontWeight = FontWeight.SemiBold)
                    Text("Receta: ${preset.recipeChain[idx]}", color = Dim, fontSize = 10.sp)
                    Spacer(Modifier.height(6.dp))
                    if (candidates.isEmpty()) {
                        Text("⚠ No tienes edificios de este tipo", color = Ruby, fontSize = 12.sp)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(candidates, key = { it.id }) { b ->
                                val sel = assignments.getOrNull(idx) == b.id
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (sel) Gold.copy(alpha = 0.25f) else InkBorder)
                                        .border(
                                            1.dp,
                                            if (sel) Gold else InkBorder.copy(alpha = 0.7f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            assignments = assignments.toMutableList().also {
                                                while (it.size <= idx) it.add("")
                                                it[idx] = b.id
                                            }
                                        }
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(b.name, fontWeight = FontWeight.SemiBold,
                                            color = if (sel) Gold else Paper, fontSize = 12.sp)
                                        Text("${b.assignedWorkers}/${b.workerCapacity} 👤",
                                            color = Dim, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                EmpireCard {
                    SectionTitle("3. Modo de balanceo")
                    Spacer(Modifier.height(6.dp))
                    BalancingMode.values().forEach { mode ->
                        val sel = mode.name == balancingName
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) Gold.copy(alpha = 0.18f) else Color.Transparent)
                                .clickable { balancingName = mode.name }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(mode.emoji, fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(mode.displayName, fontWeight = FontWeight.SemiBold,
                                    color = if (sel) Gold else Paper)
                                Text(mode.description, color = Dim, fontSize = 11.sp)
                            }
                            if (sel) Text("✓", color = Gold, fontSize = 18.sp)
                        }
                    }
                }
            }
            item {
                EmpireCard {
                    SectionTitle("4. Nombre y confirmar")
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Nombre de la línea") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    val complete = assignments.size == preset.requiredBuildingTypes.size &&
                        assignments.all { it.isNotBlank() }
                    Button(
                        onClick = {
                            vm.createLine(
                                presetId = preset.id,
                                buildingIds = assignments,
                                name = customName.ifBlank { preset.name },
                                balancingMode = BalancingMode.values()
                                    .first { it.name == balancingName }
                            )
                            // reset
                            selectedPresetId = null
                            assignments = emptyList()
                            customName = ""
                            onCreated()
                        },
                        enabled = complete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold, contentColor = Ink,
                            disabledContainerColor = InkBorder,
                            disabledContentColor = Dim
                        )
                    ) {
                        Text(if (complete) "Crear línea" else "Asigna todos los edificios")
                    }
                }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}
