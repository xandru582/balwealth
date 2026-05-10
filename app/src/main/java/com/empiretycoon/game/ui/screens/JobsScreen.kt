package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.engine.JobsEngine
import com.empiretycoon.game.model.*
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.screens.jobs.ActorJobScreen
import com.empiretycoon.game.ui.screens.jobs.BakerJobScreen
import com.empiretycoon.game.ui.screens.jobs.BaristaJobScreen
import com.empiretycoon.game.ui.screens.jobs.BoxerJobScreen
import com.empiretycoon.game.ui.screens.jobs.ChefJobScreen
import com.empiretycoon.game.ui.screens.jobs.DetectiveJobScreen
import com.empiretycoon.game.ui.screens.jobs.FirefighterJobScreen
import com.empiretycoon.game.ui.screens.jobs.FishermanJobScreen
import com.empiretycoon.game.ui.screens.jobs.FootballJobScreen
import com.empiretycoon.game.ui.screens.jobs.DentistJobScreen
import com.empiretycoon.game.ui.screens.jobs.FarmerJobScreen
import com.empiretycoon.game.ui.screens.jobs.GarbageJobScreen
import com.empiretycoon.game.ui.screens.jobs.IceCreamJobScreen
import com.empiretycoon.game.ui.screens.jobs.K9JobScreen
import com.empiretycoon.game.ui.screens.jobs.LibrarianJobScreen
import com.empiretycoon.game.ui.screens.jobs.MechanicJobScreen
import com.empiretycoon.game.ui.screens.jobs.PainterJobScreen
import com.empiretycoon.game.ui.screens.jobs.ParamedicJobScreen
import com.empiretycoon.game.ui.screens.jobs.PharmacistJobScreen
import com.empiretycoon.game.ui.screens.jobs.PizzeriaJobScreen
import com.empiretycoon.game.ui.screens.jobs.PoliceJobScreen
import com.empiretycoon.game.ui.screens.jobs.PostmanJobScreen
import com.empiretycoon.game.ui.screens.jobs.ProgrammerJobScreen
import com.empiretycoon.game.ui.screens.jobs.StreamerJobScreen
import com.empiretycoon.game.ui.screens.jobs.TaxiJobScreen
import com.empiretycoon.game.ui.screens.jobs.TeacherJobScreen
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney

/**
 * Pantalla de Oficios — hub con 40 trabajos agrupados en 11 categorías.
 *
 * Estados:
 *  - Si !state.jobs.accepted → pantalla de bienvenida.
 *  - Si accepted → header de stats + cards agrupadas por categoría con
 *    botón "Trabajar 1h" en cada oficio desbloqueado.
 *
 * Las categorías son colapsables: se abren con click. Por defecto solo
 * "Servicios de emergencia" abierta para no ahogar al jugador.
 */
@Composable
fun JobsScreen(state: GameState, vm: GameViewModel) {
    val js = state.jobs

    // Estado local: si != null, mostramos el mini-juego del oficio. Cuando
    // el composable termina (onFinish / onCancel), volvemos al hub.
    var activeMiniJob by remember { mutableStateOf<JobId?>(null) }

    activeMiniJob?.let { job ->
        val onFinish: (Double) -> Unit = { scoreMul ->
            vm.jobsWorkShiftWithScore(job, scoreMul)
            activeMiniJob = null
        }
        val onCancel: () -> Unit = { activeMiniJob = null }
        when (job) {
            JobId.POLICE_OFFICER -> PoliceJobScreen(state, onFinish, onCancel)
            JobId.FIREFIGHTER -> FirefighterJobScreen(state, onFinish, onCancel)
            JobId.BAKER -> BakerJobScreen(state, onFinish, onCancel)
            JobId.CHEF -> ChefJobScreen(state, onFinish, onCancel)
            JobId.TAXI_DRIVER -> TaxiJobScreen(state, onFinish, onCancel)
            JobId.CAR_MECHANIC -> MechanicJobScreen(state, onFinish, onCancel)
            JobId.PROGRAMMER -> ProgrammerJobScreen(state, onFinish, onCancel)
            JobId.DETECTIVE -> DetectiveJobScreen(state, onFinish, onCancel)
            JobId.BOXER -> BoxerJobScreen(state, onFinish, onCancel)
            JobId.FISHERMAN -> FishermanJobScreen(state, onFinish, onCancel)
            JobId.FOOTBALL_PLAYER -> FootballJobScreen(state, onFinish, onCancel)
            JobId.STREAMER -> StreamerJobScreen(state, onFinish, onCancel)
            JobId.PAINTER -> PainterJobScreen(state, onFinish, onCancel)
            JobId.PHARMACIST -> PharmacistJobScreen(state, onFinish, onCancel)
            JobId.TEACHER -> TeacherJobScreen(state, onFinish, onCancel)
            JobId.FARMER -> FarmerJobScreen(state, onFinish, onCancel)
            JobId.LIBRARIAN -> LibrarianJobScreen(state, onFinish, onCancel)
            JobId.DENTIST -> DentistJobScreen(state, onFinish, onCancel)
            JobId.POSTMAN -> PostmanJobScreen(state, onFinish, onCancel)
            JobId.ICE_CREAM_SELLER -> IceCreamJobScreen(state, onFinish, onCancel)
            JobId.PARAMEDIC -> ParamedicJobScreen(state, onFinish, onCancel)
            JobId.K9_OFFICER -> K9JobScreen(state, onFinish, onCancel)
            JobId.ACTOR -> ActorJobScreen(state, onFinish, onCancel)
            JobId.PIZZAIOLO -> PizzeriaJobScreen(state, onFinish, onCancel)
            JobId.BARISTA -> BaristaJobScreen(state, onFinish, onCancel)
            JobId.GARBAGE_COLLECTOR -> GarbageJobScreen(state, onFinish, onCancel)
            else -> { activeMiniJob = null }  // safety: no debería ocurrir
        }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (!js.accepted) {
            JobsWelcome(state, vm)
        } else {
            JobsHubView(state, vm) { job ->
                if (job.miniGameImplemented) activeMiniJob = job
                else vm.jobsWorkShift(job)
            }
        }
    }
}

// ===================== WELCOME =====================

@Composable
private fun JobsWelcome(state: GameState, vm: GameViewModel) {
    SectionTitle(
        "💼 Bolsa de empleo",
        subtitle = "40 oficios jugables. Trabajas tú directamente, no la empresa."
    )
    Spacer(Modifier.height(12.dp))

    EmpireCard(borderColor = Sapphire) {
        Text("¿Qué es esto?", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Paper)
        Text(
            "Tu personaje puede tener empleo aparte de la empresa. Cada turno consume " +
                "energía y te paga a tu cartera personal.\n\n" +
                "Subes nivel del oficio con cada turno. Más nivel = más wage. " +
                "Si tu stat preferido (INT/STR/CHA/LUC/DEX) es alto, ganas aún más.",
            color = Dim, fontSize = 12.sp
        )
    }
    Spacer(Modifier.height(8.dp))

    EmpireCard {
        Text("Cómo funciona", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Paper)
        Text(
            "1. Aceptas la bolsa de empleo (gratis, irreversible).\n" +
                "2. Los oficios se desbloquean automáticamente al subir tu nivel del personaje.\n" +
                "3. Cada turno = 1 hora in-game = -X⚡ energía + cash + XP del oficio.\n" +
                "4. Cada nivel del oficio = +5% wage permanente.\n" +
                "5. Mini-juegos jugables se irán añadiendo en próximas tandas (uno por oficio).",
            color = Dim, fontSize = 12.sp
        )
    }
    Spacer(Modifier.height(16.dp))

    Button(
        onClick = { vm.jobsAccept(); vm.jobsCheckUnlocks() },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Gold)
    ) {
        Text("💼 Abrir la bolsa de empleo",
            fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

// ===================== HUB =====================

@Composable
private fun JobsHubView(
    state: GameState,
    vm: GameViewModel,
    onWorkRequested: (JobId) -> Unit
) {
    val js = state.jobs
    val player = state.player

    // ----- Header lifetime -----
    EmpireCard(borderColor = Gold) {
        Text("📊 Resumen", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Gold)
        Spacer(Modifier.height(4.dp))
        Row {
            HeaderCol("Cartera personal", player.cash.fmtMoney(), color = Emerald)
            Spacer(Modifier.width(12.dp))
            HeaderCol("Energía", "${player.energy}/${player.maxEnergy} ⚡")
        }
        Spacer(Modifier.height(6.dp))
        Row {
            HeaderCol("Turnos lifetime", "${js.totalShifts}")
            Spacer(Modifier.width(12.dp))
            HeaderCol("Total ganado", js.totalEarned.fmtMoney(), color = Sapphire)
        }
        val unlockedCount = js.progress.values.count { it.unlocked }
        Spacer(Modifier.height(6.dp))
        Text(
            "Oficios desbloqueados: $unlockedCount / ${JobId.values().size} · Tu nivel: ${player.level}",
            color = Dim, fontSize = 11.sp
        )
    }
    Spacer(Modifier.height(16.dp))

    // ----- Categorías -----
    SectionTitle("🗂️ Oficios por categoría", subtitle = "Toca un oficio desbloqueado para trabajar 1h.")
    Spacer(Modifier.height(8.dp))

    // Estado expandible por categoría — por defecto la primera abierta.
    val expanded = remember {
        mutableStateMapOf<JobCategory, Boolean>().apply {
            JobCategory.values().forEachIndexed { i, c -> this[c] = i == 0 }
        }
    }

    for (cat in JobCategory.values()) {
        val jobsInCat = JobId.values().filter { it.category == cat }
        val unlockedInCat = jobsInCat.count { js.progressOf(it).unlocked }
        CategoryHeader(
            category = cat,
            count = jobsInCat.size,
            unlocked = unlockedInCat,
            isExpanded = expanded[cat] == true,
            onToggle = { expanded[cat] = !(expanded[cat] ?: false) }
        )
        if (expanded[cat] == true) {
            for (job in jobsInCat) {
                JobCard(
                    job = job,
                    progress = js.progressOf(job),
                    waitingPlayerLevel = state.player.level < job.requiredPlayerLevel,
                    requiredLevel = job.requiredPlayerLevel,
                    wagePreview = JobsEngine.previewWage(state, job),
                    canWork = state.player.energy >= job.energyCost,
                    onWork = { onWorkRequested(job) }
                )
                Spacer(Modifier.height(6.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    // ----- Recientes -----
    if (js.recentShifts.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        SectionTitle("📜 Últimos turnos")
        Spacer(Modifier.height(6.dp))
        for (sh in js.recentShifts.takeLast(8).reversed()) {
            ShiftRow(sh)
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun HeaderCol(label: String, value: String, color: Color = Paper) {
    Column {
        Text(label, color = Dim, fontSize = 10.sp)
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CategoryHeader(
    category: JobCategory,
    count: Int,
    unlocked: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(InkSoft)
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(category.emoji, fontSize = 22.sp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(category.displayName, fontWeight = FontWeight.Bold, color = Paper, fontSize = 14.sp)
            Text("$unlocked / $count desbloqueados", color = Dim, fontSize = 11.sp)
        }
        Text(if (isExpanded) "▾" else "▸", color = Gold, fontSize = 18.sp)
    }
}

@Composable
private fun JobCard(
    job: JobId,
    progress: JobProgress,
    waitingPlayerLevel: Boolean,
    requiredLevel: Int,
    wagePreview: Double,
    canWork: Boolean,
    onWork: () -> Unit
) {
    val border = when {
        !progress.unlocked -> InkBorder
        progress.level >= 25 -> Gold
        progress.level >= 10 -> Emerald
        else -> Sapphire
    }
    EmpireCard(borderColor = border) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(InkBorder),
                contentAlignment = Alignment.Center
            ) {
                Text(job.emoji, fontSize = 26.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    job.displayName,
                    fontWeight = FontWeight.Bold,
                    color = if (progress.unlocked) Paper else Dim,
                    fontSize = 14.sp
                )
                Text(job.description, color = Dim, fontSize = 11.sp)
                if (progress.unlocked) {
                    Text(
                        "Nivel ${progress.level} · XP ${progress.xpInLevel}/100 · Stat: ${job.preferredStat.name} · ⚡-${job.energyCost}",
                        color = Sapphire, fontSize = 10.sp
                    )
                    if (!job.miniGameImplemented) {
                        Text(
                            "🚧 Mini-juego pendiente: ${job.miniGameDescription}",
                            color = Color(0xFFFFB74D), fontSize = 10.sp
                        )
                    }
                } else {
                    Text(
                        "🔒 Requiere nivel $requiredLevel del jugador",
                        color = Color(0xFFFF7A7A), fontSize = 10.sp
                    )
                }
            }
            if (progress.unlocked) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(wagePreview.fmtMoney(), color = Emerald, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold)
                    Text("/ turno", color = Dim, fontSize = 9.sp)
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onWork,
                        enabled = canWork,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold)
                    ) {
                        Text("Trabajar 1h", fontSize = 11.sp,
                            color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (progress.unlocked && progress.level < 50) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress.xpInLevel / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )
        }
    }
}

@Composable
private fun ShiftRow(sh: JobShiftResult) {
    val job = runCatching { JobId.valueOf(sh.jobName) }.getOrNull()
    val emoji = job?.emoji ?: "💼"
    val name = job?.displayName ?: sh.jobName
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(InkBorder)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            "Día ${sh.day}: $name (nv ${sh.level})",
            color = Paper, fontSize = 11.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )
        Text(sh.cashEarned.fmtMoney(), color = Emerald, fontSize = 12.sp,
            fontWeight = FontWeight.Bold)
    }
}
