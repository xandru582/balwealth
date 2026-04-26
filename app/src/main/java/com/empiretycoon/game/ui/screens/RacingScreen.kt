package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.engine.RacingEngine
import com.empiretycoon.game.model.*
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*

private enum class RacingTab(val label: String, val emoji: String) {
    Team("Equipo", "🏎️"),
    Calendar("Calendario", "📅"),
    Standings("Clasificación", "🏆"),
    Market("Pilotos", "🪪"),
    Sponsors("Patrocinadores", "💰"),
    Staff("Personal", "👨‍🔧"),
    Records("Records", "📊"),
    HallOfFame("Hall of Fame", "🏛️")
}

@Composable
fun RacingScreen(state: GameState, vm: GameViewModel) {
    // Bootstrap on first entry
    LaunchedEffect(Unit) {
        if (state.racing.teams.isEmpty()) vm.racingInit()
    }

    var tab by rememberSaveableTab()

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🏁", fontSize = 28.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Formula Manager", color = Gold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                val ownedT = state.racing.ownedTeam()
                val sub = if (ownedT == null) "Compra un equipo en el mercado para empezar"
                          else "${ownedT.flag} ${ownedT.name} · Temp. ${state.racing.currentSeason}"
                Text(sub, color = Dim, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))

        // Tabs scrollables (8 tabs)
        ScrollableTabRow(
            selectedTabIndex = tab.ordinal,
            containerColor = InkSoft,
            contentColor = Gold,
            edgePadding = 0.dp
        ) {
            for (t in RacingTab.values()) {
                Tab(
                    selected = tab == t,
                    onClick = { tab = t },
                    text = {
                        Text("${t.emoji} ${t.label}",
                            fontSize = 11.sp,
                            color = if (tab == t) Gold else Dim,
                            fontWeight = if (tab == t) FontWeight.Bold else FontWeight.Normal)
                    }
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        when (tab) {
            RacingTab.Team -> TeamTab(state, vm)
            RacingTab.Calendar -> CalendarTab(state)
            RacingTab.Standings -> StandingsTab(state)
            RacingTab.Market -> MarketTab(state, vm)
            RacingTab.Sponsors -> SponsorsTab(state, vm)
            RacingTab.Staff -> StaffTab(state, vm)
            RacingTab.Records -> RecordsTab(state)
            RacingTab.HallOfFame -> HallOfFameTab(state)
        }
    }
}

@Composable
private fun rememberSaveableTab(): MutableState<RacingTab> =
    remember { mutableStateOf(RacingTab.Team) }

// =====================================================================
//                              MI EQUIPO
// =====================================================================

@Composable
private fun TeamTab(state: GameState, vm: GameViewModel) {
    val team = state.racing.ownedTeam()
    if (team == null) {
        EmptyTeamPrompt()
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Identidad del equipo
        EmpireCard(borderColor = Color(team.primaryColor.toInt())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Stripe de color
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                    .background(Brush.verticalGradient(
                        listOf(Color(team.primaryColor.toInt()), Color(team.secondaryColor.toInt()))
                    )))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("${team.flag} ${team.name}",
                        color = Paper, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Fundado ${team.founded} · ${team.country}",
                        color = Dim, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${team.seasonPoints} pts", color = Gold, fontWeight = FontWeight.Bold)
                    Text("${team.championshipsWon}× 🏆", color = Dim, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Stats del coche + upgrades
        EmpireCard {
            SectionTitle("Coche", "Mejora cada componente para subir rendimiento")
            Spacer(Modifier.height(8.dp))
            CarStatRow("⚙️ Motor",      team.car.engine,      RacingEngine.CarPart.ENGINE,      vm, state)
            CarStatRow("✈️ Aerodinámica", team.car.aero,      RacingEngine.CarPart.AERO,        vm, state)
            CarStatRow("🛡️ Fiabilidad", team.car.reliability, RacingEngine.CarPart.RELIABILITY, vm, state)
            CarStatRow("🛞 Neumáticos", team.car.tyres,        RacingEngine.CarPart.TYRES,       vm, state)
            Spacer(Modifier.height(6.dp))
            Text("Overall: ${team.car.overall()}/100",
                color = Emerald, fontWeight = FontWeight.Bold)
            Text("Total invertido: ${"%,.0f".format(team.car.totalUpgradeSpend)} €",
                color = Dim, fontSize = 11.sp)
        }

        Spacer(Modifier.height(8.dp))

        // Pilotos
        EmpireCard {
            SectionTitle("Pilotos", "Cada uno tiene salario diario")
            Spacer(Modifier.height(8.dp))
            DriverSlot(state, vm, slot = 1, driverId = team.driver1Id)
            Spacer(Modifier.height(8.dp))
            DriverSlot(state, vm, slot = 2, driverId = team.driver2Id)
        }

        Spacer(Modifier.height(8.dp))

        // Finanzas
        EmpireCard {
            SectionTitle("Finanzas")
            Spacer(Modifier.height(6.dp))
            FinancialRow("💰 Sponsor diario", "+${"%,.0f".format(team.sponsorIncomePerDay)} €", Emerald)
            FinancialRow("👨‍🔧 Coste diario", "-${"%,.0f".format(team.totalDailyCost())} €", Ruby)
            val net = team.sponsorIncomePerDay - team.totalDailyCost()
            FinancialRow("📊 Neto / día",
                "${if (net >= 0) "+" else ""}${"%,.0f".format(net)} €",
                if (net >= 0) Emerald else Ruby)
            FinancialRow("🏛️ Budget acumulado", "${"%,.0f".format(team.budget)} €", Gold)
            FinancialRow("⭐ Brand value", "${team.brandValue}/100", Sapphire)
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { vm.racingSellTeam() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Ruby.copy(alpha = 0.8f), contentColor = Paper)
        ) {
            Text("Vender equipo (-20%)", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun EmptyTeamPrompt() {
    EmpireCard {
        Text("Todavía no tienes equipo", color = Gold,
            fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(6.dp))
        Text("Ve a la pestaña Mercado para comprar tu primer equipo. " +
            "Hay opciones desde 9.5M€ (Phoenix Garage) hasta 320M€ (Apex Racing).",
            color = Paper, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        Text("Como dueño, ganarás dinero con patrocinadores y premios de carrera, pero también pagarás salarios y mantenimiento. La temporada tiene 16 carreras.",
            color = Dim, fontSize = 12.sp)
    }
}

@Composable
private fun CarStatRow(
    label: String, level: Int,
    part: RacingEngine.CarPart, vm: GameViewModel, state: GameState
) {
    val cost = part.baseCost * (1.0 + level * 0.05)
    val canAfford = state.company.cash >= cost
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Paper, modifier = Modifier.weight(1f))
        // Barra de nivel
        Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(InkBorder)) {
            Box(
                Modifier.fillMaxHeight()
                    .fillMaxWidth(level / 100f)
                    .background(Brush.horizontalGradient(listOf(Sapphire, Emerald, Gold)))
            )
        }
        Spacer(Modifier.width(6.dp))
        Text("$level", color = Gold, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
        Spacer(Modifier.width(6.dp))
        TextButton(
            onClick = { vm.racingUpgrade(part) },
            enabled = canAfford && level < 99,
            colors = ButtonDefaults.textButtonColors(contentColor = if (canAfford) Emerald else Dim)
        ) {
            Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
    Text("Coste mejora: ${"%,.0f".format(cost)} €",
        color = Dim, fontSize = 10.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
}

@Composable
private fun DriverSlot(state: GameState, vm: GameViewModel, slot: Int, driverId: String?) {
    val driver = driverId?.let { id -> state.racing.drivers.find { it.id == id } ?: DriverPool.byId(id) }
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Ink)
            .border(1.dp, InkBorder, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (driver == null) {
            Text("Sin piloto en slot $slot",
                color = Dim, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("Ve a Pilotos para fichar", color = Sapphire, fontSize = 11.sp)
        } else {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(driver.flag, fontSize = 22.sp)
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(driver.name, color = Paper, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${driver.age} años · ${driver.nationality}",
                            color = Dim, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                // Stats principales
                Row {
                    StatChipMini("SK", driver.skill, Sapphire)
                    StatChipMini("CO", driver.consistency, Emerald)
                    StatChipMini("AG", driver.aggression, Ruby)
                    StatChipMini("MO", driver.morale, Gold)
                }
                Spacer(Modifier.height(3.dp))
                // Specialties
                Row {
                    StatChipMini("☔", driver.rainSkill, Sapphire)
                    StatChipMini("🏙", driver.streetSkill, Emerald)
                    StatChipMini("⚡", driver.highSpeedSkill, Ruby)
                    StatChipMini("⏱", driver.qualifyingPace, Gold)
                }
                Spacer(Modifier.height(4.dp))
                Text("Salario: ${"%,.0f".format(driver.salaryPerDay)} €/día",
                    color = Gold, fontSize = 11.sp)
                Text("Temp: ${driver.seasonPoints}pts · ${driver.championships}× 👑 · " +
                    "${driver.careerWins}🥇 ${driver.careerPodiums}🏆 ${driver.careerPoles}P",
                    color = Dim, fontSize = 10.sp)
                if (driver.currentWinStreak >= 2) {
                    Text("🔥 Win streak: ${driver.currentWinStreak}",
                        color = Ruby, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = { vm.racingFireDriver(slot) }) {
                Text("Despedir", color = Ruby, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun StatChipMini(label: String, value: Int, color: Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text("$label $value", fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.width(3.dp))
}

@Composable
private fun FinancialRow(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, color = Paper, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// =====================================================================
//                              CALENDARIO
// =====================================================================

@Composable
private fun CalendarTab(state: GameState) {
    val r = state.racing
    if (r.calendar.isEmpty()) {
        EmpireCard { Text("Calendario aún no generado", color = Dim) }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            EmpireCard(borderColor = Gold) {
                Text("Temporada ${r.currentSeason}",
                    color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${r.racesThisSeason}/${RacingConstants.RACES_PER_SEASON} carreras corridas",
                    color = Dim, fontSize = 12.sp)
                val next = r.nextRace()
                if (next != null) {
                    val c = CircuitCatalog.byId(next.circuitId)
                    if (c != null) {
                        Spacer(Modifier.height(6.dp))
                        Text("Próxima: ${c.flag} ${c.name}",
                            color = Emerald, fontWeight = FontWeight.SemiBold)
                        Text("Día ${next.raceDay} (faltan ${(next.raceDay - state.day).coerceAtLeast(0)} días)",
                            color = Dim, fontSize = 11.sp)
                    }
                }
            }
        }
        items(r.calendar, key = { it.raceIndex }) { race ->
            val c = CircuitCatalog.byId(race.circuitId) ?: return@items
            val pastRace = race.raceIndex < r.nextRaceIndex
            val isNext = race.raceIndex == r.nextRaceIndex
            Spacer(Modifier.height(6.dp))
            EmpireCard(borderColor = when {
                pastRace -> InkBorder
                isNext -> Emerald
                else -> InkBorder
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.flag, fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("R${race.raceIndex + 1}: ${c.name}",
                            color = if (pastRace) Dim else Paper,
                            fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${c.country} · ${c.lengthKm} km · ${c.laps} vueltas · Dif. ${c.difficulty}/10",
                            color = Dim, fontSize = 10.sp)
                        Text("Premio 1°: ${"%,.0f".format(c.basePrize)} €",
                            color = Gold, fontSize = 10.sp)
                    }
                    if (pastRace) {
                        val res = r.resultsHistory.find { it.raceIndex == race.raceIndex }
                        val winnerId = res?.finishOrder?.firstOrNull()?.first
                        val winnerName = winnerId?.let { DriverPool.byId(it)?.name?.split(" ")?.last() }
                        Text("✅ $winnerName", color = Emerald, fontSize = 11.sp)
                    } else if (isNext) {
                        Text("PRÓXIMA", color = Emerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Día ${race.raceDay}", color = Dim, fontSize = 11.sp)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

// =====================================================================
//                              CLASIFICACIÓN
// =====================================================================

@Composable
private fun StandingsTab(state: GameState) {
    val r = state.racing
    LazyColumn(Modifier.fillMaxSize()) {
        item { SectionTitle("Mundial de Pilotos", "Top 10 por puntos esta temporada") }
        items(r.driverStandings(10).withIndex().toList()) { (idx, d) ->
            Spacer(Modifier.height(4.dp))
            val team = r.teams.find { t -> t.driver1Id == d.id || t.driver2Id == d.id }
            EmpireCard(borderColor = if (team?.id == r.ownedTeamId) Gold else InkBorder) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${idx + 1}.", color = Gold, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp))
                    Text(d.flag, fontSize = 18.sp)
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.name, color = Paper, fontWeight = FontWeight.SemiBold)
                        Text(team?.name ?: "Sin equipo", color = Dim, fontSize = 11.sp)
                    }
                    Text("${d.seasonPoints} pts", color = Gold, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Spacer(Modifier.height(14.dp))
            SectionTitle("Mundial de Constructores", "Top 10 por puntos esta temporada")
        }
        items(r.constructorStandings(10).withIndex().toList()) { (idx, t) ->
            Spacer(Modifier.height(4.dp))
            EmpireCard(borderColor = if (t.id == r.ownedTeamId) Gold else InkBorder) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${idx + 1}.", color = Gold, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp))
                    Box(Modifier.size(20.dp).clip(RoundedCornerShape(4.dp))
                        .background(Color(t.primaryColor.toInt())))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${t.flag} ${t.name}", color = Paper, fontWeight = FontWeight.SemiBold)
                        Text("Coche ${t.car.overall()}/100", color = Dim, fontSize = 11.sp)
                    }
                    Text("${t.seasonPoints} pts", color = Gold, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

// =====================================================================
//                              MERCADO
// =====================================================================

@Composable
private fun MarketTab(state: GameState, vm: GameViewModel) {
    val r = state.racing
    val owned = r.ownedTeam()
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            if (owned == null) {
                EmpireCard(borderColor = Emerald) {
                    Text("💼 Compra tu primer equipo",
                        color = Emerald, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Cash actual: ${"%,.0f".format(state.company.cash)} €",
                        color = Paper, fontSize = 12.sp)
                }
            } else {
                EmpireCard(borderColor = Gold) {
                    Text("Mercado de pilotos libres",
                        color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Pulsa para fichar en slot 1 ó 2 (prima = 30 días de salario)",
                        color = Dim, fontSize = 11.sp)
                }
            }
        }

        // Equipos a la venta
        if (owned == null) {
            item {
                Spacer(Modifier.height(8.dp))
                SectionTitle("Equipos a la venta", "Ordenados por precio")
            }
            items(r.teams.sortedBy { it.price }, key = { it.id }) { team ->
                Spacer(Modifier.height(6.dp))
                EmpireCard(borderColor = Color(team.primaryColor.toInt()).copy(alpha = 0.6f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(6.dp))
                            .background(Color(team.primaryColor.toInt())))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${team.flag} ${team.name}",
                                color = Paper, fontWeight = FontWeight.Bold)
                            Text("${team.country} · Fundado ${team.founded}",
                                color = Dim, fontSize = 10.sp)
                            Text("Coche ${team.car.overall()}/100 · Brand ${team.brandValue} · Sponsor ${"%,.0f".format(team.sponsorIncomePerDay)}€/día",
                                color = Sapphire, fontSize = 10.sp)
                            Text("${team.championshipsWon}× campeón",
                                color = Gold, fontSize = 10.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${"%,.0f".format(team.price / 1_000_000.0)}M €",
                                color = Gold, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = { vm.racingBuyTeam(team.id) },
                                enabled = state.company.cash >= team.price,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Emerald, contentColor = Ink,
                                    disabledContainerColor = Dim
                                )
                            ) { Text("COMPRAR", fontSize = 11.sp) }
                        }
                    }
                }
            }
        } else {
            // Mercado de pilotos libres
            val freeDrivers = DriverPool.all.filter { d ->
                r.teams.none { it.driver1Id == d.id || it.driver2Id == d.id }
            }
            items(freeDrivers, key = { it.id }) { d ->
                Spacer(Modifier.height(6.dp))
                EmpireCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(d.flag, fontSize = 22.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(d.name, color = Paper, fontWeight = FontWeight.Bold)
                            Text("${d.age} años · ${d.nationality}", color = Dim, fontSize = 11.sp)
                            Row {
                                Text("Skill ${d.skill}", color = Sapphire, fontSize = 10.sp)
                                Spacer(Modifier.width(6.dp))
                                Text("Cons ${d.consistency}", color = Emerald, fontSize = 10.sp)
                                Spacer(Modifier.width(6.dp))
                                Text("Agg ${d.aggression}", color = Ruby, fontSize = 10.sp)
                            }
                            Text("${"%,.0f".format(d.salaryPerDay)} €/día · Prima ${"%,.0f".format(d.salaryPerDay * 30)} €",
                                color = Gold, fontSize = 11.sp)
                        }
                        Column {
                            TextButton(onClick = { vm.racingSignDriver(d.id, 1) },
                                enabled = state.company.cash >= d.salaryPerDay * 30) {
                                Text("Slot 1", color = Emerald, fontSize = 11.sp)
                            }
                            TextButton(onClick = { vm.racingSignDriver(d.id, 2) },
                                enabled = state.company.cash >= d.salaryPerDay * 30) {
                                Text("Slot 2", color = Emerald, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

// =====================================================================
//                          PATROCINADORES
// =====================================================================

@Composable
private fun SponsorsTab(state: GameState, vm: GameViewModel) {
    val r = state.racing
    val team = r.ownedTeam()
    if (team == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Compra un equipo para acceder a patrocinadores", color = Dim)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        // Resumen económico
        item {
            EmpireCard(borderColor = Gold) {
                Text("💰 Resumen económico", color = Gold,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                FinancialRow("Sponsorships activos", "${r.activeSponsorships.size}", Emerald)
                FinancialRow("Income diario sponsors", "${"%,.0f".format(r.totalSponsorDailyIncome())} €", Emerald)
                FinancialRow("Total ganado lifetime", "${"%,.0f".format(r.totalSponsorEarnings)} €", Gold)
                FinancialRow("Brand value equipo", "${team.brandValue}/100", Sapphire)
            }
        }

        // Contratos activos
        if (r.activeSponsorships.isNotEmpty()) {
            item { Spacer(Modifier.height(8.dp)); SectionTitle("Contratos activos") }
            items(r.activeSponsorships, key = { it.sponsorId }) { active ->
                val sp = SponsorCatalog.byId(active.sponsorId) ?: return@items
                Spacer(Modifier.height(4.dp))
                EmpireCard(borderColor = Color(sp.tier.color.toInt())) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(sp.tier.emoji, fontSize = 24.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${sp.brand}", color = Paper, fontWeight = FontWeight.Bold)
                            Text("${sp.flag} ${sp.country} · ${sp.sector}",
                                color = Dim, fontSize = 11.sp)
                            val daysLeft = (active.expiresOnDay - state.day).coerceAtLeast(0)
                            Text("Expira en $daysLeft días · ${"%,.0f".format(active.totalEarned)} € ganados",
                                color = Sapphire, fontSize = 10.sp)
                            Text("${active.winsDuringContract}🥇 · ${active.podiumsDuringContract}🏆 durante contrato",
                                color = Gold, fontSize = 10.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${"%,.0f".format(sp.baseDailyPay)} €/día",
                                color = Emerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { vm.racingCancelSponsor(sp.id) }) {
                                Text("Romper", color = Ruby, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Mercado de patrocinadores
        item {
            Spacer(Modifier.height(8.dp))
            SectionTitle("Mercado de patrocinadores",
                "Filtrados por tier · brand requerido en cada uno")
        }
        items(SponsorCatalog.all.sortedByDescending { it.baseDailyPay },
            key = { it.id }) { sp ->
            val alreadySigned = r.activeSponsorships.any { it.sponsorId == sp.id }
            val canBrand = team.brandValue >= sp.minBrandRequired
            val canPay = state.company.cash >= sp.signOnFee
            Spacer(Modifier.height(4.dp))
            EmpireCard(borderColor = Color(sp.tier.color.toInt()).copy(alpha = 0.6f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(sp.tier.emoji, fontSize = 22.sp)
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${sp.flag} ${sp.brand}", color = Paper, fontWeight = FontWeight.Bold)
                        Text("${sp.tier.label} · ${sp.sector}", color = Dim, fontSize = 10.sp)
                        Text("Daily ${"%,.0f".format(sp.baseDailyPay)}€ · Win +${"%,.0f".format(sp.winBonus)}€ · Pts +${"%,.0f".format(sp.pointsBonus)}€/pt",
                            color = Sapphire, fontSize = 10.sp)
                        Text("Brand req: ${sp.minBrandRequired} · Prima ${"%,.0f".format(sp.signOnFee)}€ · ${sp.contractDays}d",
                            color = Gold, fontSize = 10.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        when {
                            alreadySigned -> Text("✅ Activo", color = Emerald, fontSize = 11.sp)
                            !canBrand -> Text("Brand bajo", color = Dim, fontSize = 10.sp)
                            !canPay -> Text("Sin fondos", color = Ruby, fontSize = 10.sp)
                            else -> Button(
                                onClick = { vm.racingSignSponsor(sp.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Emerald, contentColor = Ink),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) { Text("FIRMAR", fontSize = 10.sp) }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

// =====================================================================
//                          PERSONAL TÉCNICO
// =====================================================================

@Composable
private fun StaffTab(state: GameState, vm: GameViewModel) {
    val r = state.racing
    val team = r.ownedTeam()
    if (team == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Compra un equipo para contratar personal técnico", color = Dim)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        // Resumen
        item {
            EmpireCard(borderColor = Gold) {
                Text("👨‍🔧 Personal técnico", color = Gold,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Cada rol da bonus al rendimiento del equipo en carrera (cap +20%).",
                    color = Dim, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                FinancialRow("Personal en plantilla", "${r.hiredStaff.size}/5", Emerald)
                FinancialRow("Coste diario total", "-${"%,.0f".format(r.totalStaffDailyCost())} €", Ruby)
                val combinedBonus = r.hiredStaff.mapNotNull { StaffPool.byId(it) }
                    .sumOf { it.bonusMultiplier() - 1.0 }.coerceAtMost(0.20)
                FinancialRow("Bonus rendimiento", "+${"%.1f".format(combinedBonus * 100)}%", Sapphire)
            }
        }

        // Listado por rol
        for (role in StaffRole.values()) {
            item {
                Spacer(Modifier.height(8.dp))
                SectionTitle("${role.emoji} ${role.label}")
            }
            items(StaffPool.byRole(role).sortedByDescending { it.rating },
                key = { it.id }) { staff ->
                val hired = r.hiredStaff.contains(staff.id)
                val roleOccupied = r.hiredStaff.mapNotNull { StaffPool.byId(it) }
                    .any { it.role == role && it.id != staff.id }
                val canPay = state.company.cash >= staff.salaryPerDay * 30
                Spacer(Modifier.height(4.dp))
                EmpireCard(borderColor = if (hired) Gold else InkBorder) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(staff.flag, fontSize = 22.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(staff.name, color = Paper, fontWeight = FontWeight.Bold)
                            Text("${staff.nationality} · Rating ${staff.rating}/100",
                                color = Sapphire, fontSize = 11.sp)
                            Text("Salario ${"%,.0f".format(staff.salaryPerDay)} €/día · Bonus +${"%.1f".format((staff.bonusMultiplier() - 1) * 100)}%",
                                color = Gold, fontSize = 10.sp)
                        }
                        when {
                            hired -> TextButton(onClick = { vm.racingFireStaff(staff.id) }) {
                                Text("Despedir", color = Ruby, fontSize = 11.sp)
                            }
                            roleOccupied -> Text("Rol ocupado", color = Dim, fontSize = 10.sp)
                            !canPay -> Text("Sin fondos", color = Ruby, fontSize = 10.sp)
                            else -> Button(
                                onClick = { vm.racingHireStaff(staff.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Emerald, contentColor = Ink),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) { Text("CONTRATAR", fontSize = 10.sp) }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

// =====================================================================
//                          RECORDS DE CIRCUITO + ESTADÍSTICAS
// =====================================================================

@Composable
private fun RecordsTab(state: GameState) {
    val r = state.racing
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            EmpireCard(borderColor = Gold) {
                Text("📊 Estadísticas globales", color = Gold,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                FinancialRow("Temporada actual", "${r.currentSeason}", Emerald)
                FinancialRow("Carreras corridas (lifetime)", "${r.resultsHistory.size}", Sapphire)
                FinancialRow("Récords establecidos", "${r.circuitRecords.count { it.lapRecordHolder != null }}", Gold)
                FinancialRow("Pilotos con victorias", "${r.drivers.count { it.careerWins > 0 }}", Sapphire)
            }
        }

        // Récords por circuito
        item {
            Spacer(Modifier.height(8.dp))
            SectionTitle("🏁 Récords por circuito",
                "Lap record + último ganador + dominador del circuito")
        }
        items(CircuitCatalog.all, key = { it.id }) { c ->
            val rec = r.recordOf(c.id)
            Spacer(Modifier.height(4.dp))
            EmpireCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.flag, fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, color = Paper, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${c.lengthKm}km · ${c.laps}vueltas · Dif. ${c.difficulty}/10",
                            color = Dim, fontSize = 10.sp)
                        if (rec?.lapRecordHolder != null) {
                            val holder = DriverPool.byId(rec.lapRecordHolder)
                            Text("⏱ Récord: ${"%.3f".format(rec.lapRecordTimeSec)}s · ${holder?.flag} ${holder?.name?.split(" ")?.last()} (T${rec.lapRecordSeason})",
                                color = Gold, fontSize = 10.sp)
                        } else {
                            Text("⏱ Sin récord registrado", color = Dim, fontSize = 10.sp)
                        }
                        if (rec?.lastWinner != null) {
                            val w = DriverPool.byId(rec.lastWinner)
                            Text("🥇 Último: ${w?.flag} ${w?.name?.split(" ")?.last()}",
                                color = Emerald, fontSize = 10.sp)
                        }
                        // King of the circuit (más wins)
                        val king = rec?.winsByDriver?.maxByOrNull { it.value }
                        if (king != null) {
                            val kd = DriverPool.byId(king.key)
                            Text("👑 Dominador: ${kd?.flag} ${kd?.name?.split(" ")?.last()} (${king.value} wins)",
                                color = Sapphire, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Top 10 win leaders all-time
        item {
            Spacer(Modifier.height(14.dp))
            SectionTitle("🏆 Pilotos con más victorias (all-time)")
        }
        items(r.allTimeWinsLeaders().withIndex().toList()) { (idx, d) ->
            Spacer(Modifier.height(3.dp))
            EmpireCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${idx + 1}.", color = Gold, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp))
                    Text(d.flag, fontSize = 18.sp)
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.name, color = Paper, fontWeight = FontWeight.SemiBold)
                        Text("Win rate ${"%.1f".format(d.winRate())}% · ${d.careerStarts} starts · ${d.careerPodiums}🏆 ${d.careerPoles}P ${d.careerFastestLaps}⚡",
                            color = Dim, fontSize = 10.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${d.careerWins} wins", color = Gold, fontWeight = FontWeight.Bold)
                        if (d.longestWinStreak >= 2)
                            Text("🔥 ${d.longestWinStreak}× streak", color = Ruby, fontSize = 10.sp)
                    }
                }
            }
        }

        // Tabla de naciones
        item {
            Spacer(Modifier.height(14.dp))
            SectionTitle("🌍 Tabla de naciones", "Total de victorias por país")
        }
        items(r.nationsMedalTable().take(15).withIndex().toList()) { (idx, triple) ->
            val (flag, country, wins) = triple
            Spacer(Modifier.height(3.dp))
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(InkSoft)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${idx + 1}.", color = Gold, modifier = Modifier.width(28.dp))
                Text(flag, fontSize = 18.sp)
                Spacer(Modifier.width(6.dp))
                Text(country, color = Paper, modifier = Modifier.weight(1f))
                Text("$wins 🏆", color = Gold, fontWeight = FontWeight.Bold)
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

// =====================================================================
//                          HALL OF FAME
// =====================================================================

@Composable
private fun HallOfFameTab(state: GameState) {
    val r = state.racing
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            EmpireCard(borderColor = Gold) {
                Text("🏛️ HALL OF FAME", color = Gold,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Campeones de cada temporada y palmarés histórico de equipos.",
                    color = Dim, fontSize = 11.sp)
            }
        }

        // Campeones por temporada
        item {
            Spacer(Modifier.height(8.dp))
            SectionTitle("👑 Campeones por temporada",
                "${r.hallOfFame.size} temporadas completadas")
        }
        if (r.hallOfFame.isEmpty()) {
            item {
                EmpireCard {
                    Text("Aún no se ha completado ninguna temporada.",
                        color = Dim, fontSize = 12.sp)
                    Text("La temporada actual va por ${r.racesThisSeason}/${RacingConstants.RACES_PER_SEASON} carreras.",
                        color = Dim, fontSize = 11.sp)
                }
            }
        }
        items(r.hallOfFame.sortedByDescending { it.season },
            key = { it.season }) { e ->
            val driver = DriverPool.byId(e.driverChampionId)
            val team = r.teams.find { it.id == e.constructorChampionId }
            val wasOwned = e.playerOwnedTeamId != null
            Spacer(Modifier.height(4.dp))
            EmpireCard(borderColor = if (wasOwned && e.constructorChampionId == e.playerOwnedTeamId) Gold else InkBorder) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("T${e.season}", color = Gold, fontWeight = FontWeight.Bold,
                        fontSize = 16.sp, modifier = Modifier.width(40.dp))
                    Column(Modifier.weight(1f)) {
                        Text("👑 ${driver?.flag} ${driver?.name} — ${e.driverChampionPoints}pts",
                            color = Paper, fontSize = 12.sp)
                        Text("🏁 ${team?.flag} ${team?.name} — ${e.constructorChampionPoints}pts",
                            color = Sapphire, fontSize = 12.sp)
                        if (wasOwned) {
                            val youHadIt = team?.id == e.playerOwnedTeamId
                            Text(if (youHadIt) "✅ ¡TU EQUIPO GANÓ!" else "Tenías ${e.playerOwnedTeamId}",
                                color = if (youHadIt) Emerald else Dim, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Palmarés histórico de equipos
        item {
            Spacer(Modifier.height(14.dp))
            SectionTitle("🏆 Equipos más laureados", "Por championships ganados (incl. histórico)")
        }
        items(r.teamHallOfFame().withIndex().toList()) { (idx, t) ->
            Spacer(Modifier.height(3.dp))
            EmpireCard(borderColor = if (t.id == r.ownedTeamId) Gold else InkBorder) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${idx + 1}.", color = Gold, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp))
                    Box(Modifier.size(20.dp).clip(RoundedCornerShape(4.dp))
                        .background(Color(t.primaryColor.toInt())))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${t.flag} ${t.name}", color = Paper, fontWeight = FontWeight.SemiBold)
                        Text("${t.country} · Fundado ${t.founded} · Brand ${t.brandValue}",
                            color = Dim, fontSize = 10.sp)
                    }
                    Text("${t.championshipsWon}× 👑", color = Gold, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Drivers con más championships (estilo Schumacher/Hamilton)
        item {
            Spacer(Modifier.height(14.dp))
            SectionTitle("👑 Pilotos con más mundiales")
        }
        items(r.drivers.filter { it.championships > 0 }
            .sortedByDescending { it.championships }
            .take(10).withIndex().toList()) { (idx, d) ->
            Spacer(Modifier.height(3.dp))
            EmpireCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${idx + 1}.", color = Gold, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp))
                    Text(d.flag, fontSize = 18.sp)
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.name, color = Paper, fontWeight = FontWeight.SemiBold)
                        Text("${d.careerStarts} starts · ${d.careerWins}🥇 · ${d.careerPodiums}🏆",
                            color = Dim, fontSize = 10.sp)
                    }
                    Text("${d.championships}×", color = Gold, fontWeight = FontWeight.Bold,
                        fontSize = 18.sp)
                }
            }
        }

        item { Spacer(Modifier.height(60.dp)) }
    }
}
