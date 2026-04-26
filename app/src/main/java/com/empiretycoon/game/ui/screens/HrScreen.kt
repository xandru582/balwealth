package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.model.*
import com.empiretycoon.game.ui.components.BadgePill
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.EmployeeCard
import com.empiretycoon.game.ui.components.ProgressBarWithLabel
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney

/**
 * Pantalla de RRHH con cuatro pestañas:
 *  - Plantilla     -> empleados activos con acciones (asignar, ascender, despedir)
 *  - Ejecutivos    -> 5 slots: CEO + CFO/COO/CTO/CMO
 *  - Formación     -> programas activos y catálogo
 *  - Solicitantes  -> portal de candidatos diario
 */
@Composable
fun HrScreen(state: GameState, vm: GameViewModel) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = tab,
            containerColor = InkSoft,
            contentColor = Gold
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 },
                text = { Text("Plantilla") })
            Tab(selected = tab == 1, onClick = { tab = 1 },
                text = { Text("Ejecutivos") })
            Tab(selected = tab == 2, onClick = { tab = 2 },
                text = { Text("Formación") })
            Tab(selected = tab == 3, onClick = { tab = 3 },
                text = { Text("Solicitantes") })
        }
        when (tab) {
            0 -> RosterTab(state, vm)
            1 -> ExecutivesTab(state, vm)
            2 -> TrainingTab(state, vm)
            3 -> ApplicantsTab(state, vm)
        }
    }
}

// ============================== PLANTILLA ==============================

@Composable
private fun RosterTab(state: GameState, vm: GameViewModel) {
    val employees = state.company.employees
    val profiles = state.hrState.profiles
    val executiveIds = state.hrState.executives.assignedIds()
    if (employees.isEmpty()) {
        EmptyState(
            title = "Aún no tienes plantilla",
            subtitle = "Ficha tu primer empleado en la pestaña 'Solicitantes'."
        )
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                Text("Resumen plantilla", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${employees.size} empleados · " +
                        "${state.company.totalSalaries.fmtMoney()}/mes en salarios",
                    color = Dim, fontSize = 12.sp
                )
                val avgSat = profiles.values.map { it.satisfactionScore }
                    .ifEmpty { listOf(70) }.average().toInt()
                val avgBurn = profiles.values.map { it.burnoutRisk }
                    .ifEmpty { listOf(10) }.average().toInt()
                Spacer(Modifier.height(6.dp))
                Text("Satisfacción media $avgSat%", color = Dim, fontSize = 11.sp)
                ProgressBarWithLabel(
                    progress = avgSat / 100f,
                    color = if (avgSat >= 60) Emerald else if (avgSat >= 30) Gold else Ruby
                )
                Spacer(Modifier.height(4.dp))
                Text("Burnout medio $avgBurn%", color = Dim, fontSize = 11.sp)
                ProgressBarWithLabel(
                    progress = avgBurn / 100f,
                    color = if (avgBurn < 40) Sapphire else if (avgBurn < 75) Gold else Ruby
                )
            }
        }
        items(employees, key = { it.id }) { emp ->
            val profile = profiles[emp.id]
            val isExec = emp.id in executiveIds
            val execSlot = when (emp.id) {
                state.hrState.executives.cfo -> ExecSlot.CFO
                state.hrState.executives.coo -> ExecSlot.COO
                state.hrState.executives.cto -> ExecSlot.CTO
                state.hrState.executives.cmo -> ExecSlot.CMO
                else -> null
            }
            EmployeeCard(
                employee = emp,
                profile = profile,
                isExecutive = isExec,
                execSlot = execSlot,
                actions = {
                    val canPromote = profile != null &&
                        RoleCatalog.promotionPath(profile.role).isNotEmpty()
                    if (canPromote) {
                        TextButton(onClick = { vm.promoteEmployee(emp.id) }) {
                            Text("Ascender", color = Gold)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { vm.fire(emp.id) }) {
                        Text("Despedir", color = Ruby)
                    }
                }
            )
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

// ============================== EJECUTIVOS ==============================

@Composable
private fun ExecutivesTab(state: GameState, vm: GameViewModel) {
    val team = state.hrState.executives
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item { SectionTitle("Cúpula directiva", "5 slots: CEO + 4 CXO.") }

        // CEO (jugador)
        item {
            EmpireCard(borderColor = Gold) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(CeoSlot.emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("CEO", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            BadgePill("Jugador", emoji = "🕹️", tint = Gold)
                        }
                        Text(state.player.name, color = Dim, fontSize = 12.sp)
                        Text(CeoSlot.description, color = Dim, fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            EmpireCard {
                Text("Bonos activos", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                ExecSlot.all.forEach { slot ->
                    val occupant = when (slot) {
                        ExecSlot.CFO -> team.cfo
                        ExecSlot.COO -> team.coo
                        ExecSlot.CTO -> team.cto
                        ExecSlot.CMO -> team.cmo
                    }
                    val active = occupant != null
                    Text(
                        "${slot.emoji} ${slot.displayName}: ${slot.description} " +
                            if (active) "✅" else "❌",
                        color = if (active) Emerald else Dim,
                        fontSize = 12.sp
                    )
                }
            }
        }

        items(ExecSlot.all) { slot ->
            ExecSlotCard(slot, state, vm)
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun ExecSlotCard(slot: ExecSlot, state: GameState, vm: GameViewModel) {
    val team = state.hrState.executives
    val occupantId = when (slot) {
        ExecSlot.CFO -> team.cfo
        ExecSlot.COO -> team.coo
        ExecSlot.CTO -> team.cto
        ExecSlot.CMO -> team.cmo
    }
    val occupant = occupantId?.let { id -> state.company.employees.find { it.id == id } }
    val occupantProfile = occupantId?.let { state.hrState.profiles[it] }
    var picking by remember(slot) { mutableStateOf(false) }

    EmpireCard(borderColor = if (occupantId != null) Gold else InkBorder) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(slot.emoji, fontSize = 28.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(slot.displayName, fontWeight = FontWeight.Bold)
                Text(slot.description, color = Dim, fontSize = 12.sp)
            }
            if (occupantId != null) {
                TextButton(onClick = { vm.assignToExec(slot.key, null) }) {
                    Text("Liberar", color = Ruby)
                }
            } else {
                TextButton(onClick = { picking = true }) {
                    Text("Asignar", color = Gold)
                }
            }
        }
        if (occupant != null && occupantProfile != null) {
            Spacer(Modifier.height(6.dp))
            EmployeeCard(
                employee = occupant,
                profile = occupantProfile,
                isExecutive = true,
                execSlot = slot,
                actions = null
            )
        }
    }

    if (picking) {
        ExecPicker(
            slot = slot,
            state = state,
            onPick = { id ->
                vm.assignToExec(slot.key, id)
                picking = false
            },
            onDismiss = { picking = false }
        )
    }
}

@Composable
private fun ExecPicker(
    slot: ExecSlot,
    state: GameState,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val candidates = state.company.employees.filter { emp ->
        val p = state.hrState.profiles[emp.id] ?: return@filter false
        (p.role == EmployeeRole.DIRECTOR || p.role == EmployeeRole.EXECUTIVE_ASSISTANT) &&
            !state.hrState.executives.isOccupied(emp.id)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar", color = Gold) } },
        title = { Text("Asignar ${slot.displayName}") },
        text = {
            if (candidates.isEmpty()) {
                Text(
                    "No hay candidatos. Asciende empleados a Director " +
                        "o Asistente Ejecutivo primero.",
                    color = Dim
                )
            } else {
                Column {
                    candidates.forEach { emp ->
                        val prof = state.hrState.profiles[emp.id]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Ink)
                                .border(1.dp, InkBorder, RoundedCornerShape(10.dp))
                                .clickable { onPick(emp.id) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                RoleCatalog.get(prof?.role ?: EmployeeRole.LABORER).emoji,
                                fontSize = 22.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(emp.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${RoleCatalog.get(prof?.role ?: EmployeeRole.LABORER).displayName} · " +
                                        "Nv ${prof?.level ?: 1}",
                                    color = Dim, fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = InkSoft
    )
}

// ============================== FORMACIÓN ==============================

@Composable
private fun TrainingTab(state: GameState, vm: GameViewModel) {
    val active = state.hrState.training.active
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                Text("Programas activos: ${active.size}", fontWeight = FontWeight.Bold)
                Text("Histórico completados: ${state.hrState.training.history}",
                    color = Dim, fontSize = 12.sp)
            }
        }
        if (active.isNotEmpty()) {
            item { SectionTitle("En curso") }
            items(active, key = { it.programId + it.startedAtTick }) { at ->
                ActiveTrainingCard(at, state)
            }
        }
        item { SectionTitle("Catálogo de programas") }
        items(TrainingCatalog.all, key = { it.id }) { p ->
            TrainingProgramCard(p, state, vm)
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun ActiveTrainingCard(at: ActiveTraining, state: GameState) {
    val program = TrainingCatalog.byId(at.programId) ?: return
    val progress = at.progress(state.tick)
    val daysLeft = ((at.endsAtTick - state.tick) / 1_440L).coerceAtLeast(0L)
    EmpireCard(borderColor = Sapphire) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(program.emoji, fontSize = 24.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(program.name, fontWeight = FontWeight.Bold)
                Text("${at.employeeIds.size} alumnos · quedan ${daysLeft}d",
                    color = Dim, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        ProgressBarWithLabel(progress = progress, color = Sapphire)
    }
}

@Composable
private fun TrainingProgramCard(
    p: TrainingProgram,
    state: GameState,
    vm: GameViewModel
) {
    var open by remember(p.id) { mutableStateOf(false) }
    var selected by remember(p.id) { mutableStateOf(setOf<String>()) }

    EmpireCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(p.emoji, fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, fontWeight = FontWeight.Bold)
                Text(
                    "${p.durationDays}d · ${p.costPerEmployee.fmtMoney()}/persona",
                    color = Dim, fontSize = 11.sp
                )
            }
            BadgePill(
                text = p.role?.let { RoleCatalog.get(it).displayName } ?: "Cualquier rol",
                emoji = p.role?.let { RoleCatalog.get(it).emoji } ?: "🌐",
                tint = Sapphire
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(p.descripcion, color = Dim, fontSize = 12.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            "+${"%.2f".format(p.statBoost.skillDelta)} skill · +${p.statBoost.xpDelta} XP",
            color = Gold, fontSize = 11.sp
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { open = !open }) {
                Text(if (open) "Cancelar" else "Inscribir", color = Gold)
            }
        }
        if (open) {
            val eligible = state.company.employees.filter { emp ->
                val pr = state.hrState.profiles[emp.id] ?: return@filter false
                (p.role == null || p.role == pr.role) &&
                    pr.education.rank >= p.minimumEducation.rank &&
                    pr.level >= p.minimumLevel
            }
            if (eligible.isEmpty()) {
                Text("Sin candidatos elegibles.", color = Dim, fontSize = 12.sp)
            } else {
                Column {
                    eligible.forEach { emp ->
                        val checked = emp.id in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable {
                                    selected = if (checked) selected - emp.id
                                        else selected + emp.id
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checked, onCheckedChange = {
                                selected = if (it) selected + emp.id else selected - emp.id
                            })
                            Text(emp.name, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = {
                            vm.startTraining(p.id, selected.toList())
                            selected = emptySet()
                            open = false
                        },
                        enabled = selected.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Sapphire, contentColor = Paper
                        )
                    ) {
                        Text("Iniciar (${selected.size})")
                    }
                }
            }
        }
    }
}

// ============================== SOLICITANTES ==============================

@Composable
private fun ApplicantsTab(state: GameState, vm: GameViewModel) {
    val applicants = state.hrState.applicants
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Portal de empleo", fontWeight = FontWeight.Bold)
                        Text(
                            "Pool diario filtrado por reputación y nivel.",
                            color = Dim, fontSize = 12.sp
                        )
                    }
                    TextButton(onClick = { vm.refreshApplicants() }) {
                        Text("Refrescar", color = Gold)
                    }
                }
            }
        }
        if (applicants.isEmpty()) {
            item {
                EmptyState(
                    title = "Sin candidatos",
                    subtitle = "Pulsa 'Refrescar' para llamar a más postulantes."
                )
            }
        } else {
            items(applicants, key = { it.id }) { app -> ApplicantCard(app, vm) }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun ApplicantCard(app: JobApplicant, vm: GameViewModel) {
    val role = RoleCatalog.get(app.role)
    EmpireCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(app.portrait, fontSize = 30.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(app.name, fontWeight = FontWeight.Bold)
                Text(
                    "${app.age} años · ${app.prevExperienceYears}a exp.",
                    color = Dim, fontSize = 11.sp
                )
            }
            BadgePill(text = role.displayName, emoji = role.emoji, tint = Sapphire)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BadgePill(text = app.education.displayName, emoji = app.education.emoji, tint = Paper)
        }
        if (app.traits.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                app.traits.joinToString(" · ") { "${it.emoji} ${it.displayName}" },
                color = Dim, fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Salario: ${app.expectedSalary.fmtMoney()}/mes · " +
                "Prima fichaje: ${app.askingBonus.fmtMoney()}",
            color = Gold, fontSize = 12.sp
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { vm.hireApplicant(app.id) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Emerald, contentColor = Midnight
                )
            ) {
                Text("Fichar")
            }
        }
    }
}

// ============================== UTIL ==============================

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = Dim)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = Dim, fontSize = 12.sp)
    }
}
