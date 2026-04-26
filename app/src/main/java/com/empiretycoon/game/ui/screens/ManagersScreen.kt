package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney

@Composable
fun ManagersScreen(state: GameState, vm: GameViewModel) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var configFor by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab, containerColor = InkSoft, contentColor = Gold) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Mi equipo") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Contratar") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Cómo funciona") })
        }
        when (tab) {
            0 -> HiredTab(state, vm, onConfigure = { configFor = it })
            1 -> HireTab(state, vm)
            2 -> HelpTab()
        }
    }

    if (configFor != null) {
        val mgr = state.managers.hired.find { it.id == configFor }
        if (mgr != null) ManagerConfigDialog(mgr, vm) { configFor = null }
        else configFor = null
    }
}

@Composable
private fun HiredTab(state: GameState, vm: GameViewModel, onConfigure: (String) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                SectionTitle(
                    "Tu equipo directivo",
                    subtitle = "${state.managers.hired.size}/${state.managers.maxSlots} plazas usadas · gasto mensual ${state.managers.hired.sumOf { it.monthlySalary }.fmtMoney()}"
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Los gerentes actúan automáticamente cada minuto. Si no quieres que actúen, " +
                        "desactívalos sin despedir (siguen cobrando salario, pero no gastan tu caja).",
                    color = Dim, fontSize = 12.sp
                )
            }
        }
        if (state.managers.hired.isEmpty()) {
            item {
                EmpireCard {
                    Text("Aún no tienes gerentes.", color = Dim, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Pasa a la pestaña 'Contratar' para fichar uno. " +
                            "Cada gerente automatiza un área del juego.",
                        color = Dim, fontSize = 12.sp
                    )
                }
            }
        } else {
            items(state.managers.hired, key = { it.id }) { m ->
                HiredCard(m, vm, onConfigure)
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun HiredCard(m: Manager, vm: GameViewModel, onConfigure: (String) -> Unit) {
    EmpireCard(borderColor = if (m.enabled) Emerald else InkBorder) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(InkBorder),
                contentAlignment = Alignment.Center
            ) {
                Text(m.portrait, fontSize = 30.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(m.name, fontWeight = FontWeight.Bold)
                Text(m.type.displayName, color = Gold, fontSize = 12.sp)
                Text(
                    "Nv. ${m.level} · Eficiencia ${"%.2f".format(m.efficiency)}x · " +
                        "Cooldown ${m.cooldownSeconds}s",
                    color = Dim, fontSize = 11.sp
                )
                Text("Salario ${m.monthlySalary.fmtMoney()}/mes · ${m.actionsTaken} acciones",
                    color = Dim, fontSize = 11.sp)
            }
            Switch(
                checked = m.enabled,
                onCheckedChange = { vm.toggleManagerEnabled(m.id) },
                colors = SwitchDefaults.colors(checkedThumbColor = Gold, checkedTrackColor = InkBorder)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row {
            OutlinedButton(
                onClick = { onConfigure(m.id) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Settings, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Ajustes")
            }
            Spacer(Modifier.width(6.dp))
            Button(
                onClick = { vm.upgradeManager(m.id) },
                colors = ButtonDefaults.buttonColors(containerColor = Sapphire, contentColor = Paper),
                modifier = Modifier.weight(1f)
            ) {
                Text("Mejorar (${(m.signingCost * 1.5).fmtMoney()})")
            }
            Spacer(Modifier.width(6.dp))
            TextButton(onClick = { vm.fireManager(m.id) }) {
                Text("Despedir", color = Ruby)
            }
        }
    }
}

@Composable
private fun HireTab(state: GameState, vm: GameViewModel) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionTitle("Candidatos disponibles")
                        Text(
                            "Pool con ${state.managers.pool.size} gerentes. Refresca diariamente.",
                            color = Dim, fontSize = 12.sp
                        )
                        Text(
                            "Plazas libres: ${state.managers.availableSlots}/${state.managers.maxSlots}",
                            color = if (state.managers.availableSlots == 0) Ruby else Emerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = { vm.refreshManagerPool() }) {
                        Text("Refrescar", color = Sapphire)
                    }
                }
            }
        }
        items(state.managers.pool, key = { it.id }) { m ->
            val alreadyHave = state.managers.has(m.type)
            EmpireCard(
                borderColor = if (alreadyHave) Ruby.copy(alpha = 0.5f) else InkBorder
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(InkBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(m.portrait, fontSize = 26.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(m.name, fontWeight = FontWeight.SemiBold)
                        Text(m.type.displayName, color = Gold, fontSize = 12.sp)
                        Text(m.type.description, color = Dim, fontSize = 11.sp)
                        Text(
                            "Nv. ${m.level} · ${"%.2f".format(m.efficiency)}x · " +
                                "Sueldo ${m.monthlySalary.fmtMoney()}/mes",
                            color = Dim, fontSize = 11.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(m.signingCost.fmtMoney(), fontWeight = FontWeight.Bold,
                            color = if (state.company.cash >= m.signingCost && !alreadyHave) Gold else Ruby)
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = { vm.hireManager(m.id) },
                            enabled = !alreadyHave && state.company.cash >= m.signingCost &&
                                state.managers.availableSlots > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Gold, contentColor = Ink,
                                disabledContainerColor = InkBorder, disabledContentColor = Dim
                            )
                        ) {
                            Text(if (alreadyHave) "Ocupado" else "Fichar")
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun HelpTab() {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        ManagerType.values().forEach { t ->
            item {
                EmpireCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t.emoji, fontSize = 30.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.displayName, fontWeight = FontWeight.Bold, color = Gold)
                            Text(t.description, color = Dim, fontSize = 12.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                explainBehavior(t),
                                color = Paper, fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

private fun explainBehavior(t: ManagerType): String = when (t) {
    ManagerType.OPERATIONS ->
        "Cada minuto, busca el edificio más barato de mejorar y lo sube si la caja no baja " +
            "del colchón configurado. Acepta hasta el nivel máximo que le digas."
    ManagerType.SALES ->
        "Vende el recurso con mejor precio del mercado siempre que el factor de precio " +
            "supere tu umbral. Mantiene una reserva mínima del recurso para no romper la producción."
    ManagerType.PURCHASING ->
        "Compra inputs que faltan en cadenas activas, priorizando los más baratos del mercado. " +
            "Solo si el factor de precio está por debajo de tu umbral y queda caja."
    ManagerType.HR ->
        "Asigna empleados libres a edificios sin staff, despide a los muy desleales y, " +
            "si está activado, contrata candidatos baratos cuando faltan trabajadores."
    ManagerType.FINANCE ->
        "Cuando la caja excede X veces tu colchón, amortiza el préstamo más caro. " +
            "Mantiene siempre el colchón de seguridad antes de pagar."
}

@Composable
private fun ManagerConfigDialog(mgr: Manager, vm: GameViewModel, onClose: () -> Unit) {
    var cashReserve by remember(mgr.id) { mutableStateOf(mgr.config.cashReserve.toString()) }
    var sellAtFactor by remember(mgr.id) { mutableStateOf(mgr.config.sellAtFactor.toString()) }
    var buyBelowFactor by remember(mgr.id) { mutableStateOf(mgr.config.buyBelowFactor.toString()) }
    var maxBuildingLevel by remember(mgr.id) { mutableStateOf(mgr.config.maxBuildingLevel.toString()) }
    var keepStock by remember(mgr.id) { mutableStateOf(mgr.config.keepStock.toString()) }
    var autoFire by remember(mgr.id) { mutableStateOf(mgr.config.autoFireBelowLoyalty.toString()) }
    var autoHire by remember(mgr.id) { mutableStateOf(mgr.config.autoHireWhenUnderstaffed) }
    var loanRatio by remember(mgr.id) { mutableStateOf(mgr.config.repayLoanAboveCashRatio.toString()) }
    var protectInputs by remember(mgr.id) { mutableStateOf(mgr.config.protectActiveRecipeInputs) }
    var whitelist by remember(mgr.id) { mutableStateOf(mgr.config.sellWhitelist) }

    com.empiretycoon.game.ui.components.CompactDialog(
        title = mgr.type.displayName,
        icon = mgr.portrait,
        onDismiss = onClose,
        footer = {
            TextButton(onClick = onClose) { Text("Cancelar", color = Dim) }
            Spacer(Modifier.width(4.dp))
            TextButton(onClick = {
                val newCfg = ManagerConfig(
                    cashReserve = cashReserve.toDoubleOrNull() ?: mgr.config.cashReserve,
                    sellAtFactor = sellAtFactor.toDoubleOrNull() ?: mgr.config.sellAtFactor,
                    buyBelowFactor = buyBelowFactor.toDoubleOrNull() ?: mgr.config.buyBelowFactor,
                    maxBuildingLevel = maxBuildingLevel.toIntOrNull() ?: mgr.config.maxBuildingLevel,
                    keepStock = keepStock.toIntOrNull() ?: mgr.config.keepStock,
                    autoFireBelowLoyalty = autoFire.toDoubleOrNull() ?: mgr.config.autoFireBelowLoyalty,
                    autoHireWhenUnderstaffed = autoHire,
                    repayLoanAboveCashRatio = loanRatio.toDoubleOrNull() ?: mgr.config.repayLoanAboveCashRatio,
                    protectActiveRecipeInputs = protectInputs,
                    sellWhitelist = whitelist
                )
                vm.updateManagerConfig(mgr.id, newCfg)
                onClose()
            }) { Text("Guardar", color = Gold) }
        }
    ) {
        // Body scrollable para no desbordarse
        androidx.compose.foundation.layout.Column(
            Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
            Text("Ajustes de ${mgr.name}", color = Dim, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            ConfigField("Reserva mínima caja (€)", cashReserve) { cashReserve = it }
            when (mgr.type) {
                ManagerType.OPERATIONS ->
                    ConfigField("Nivel máximo edificio", maxBuildingLevel) { maxBuildingLevel = it }
                ManagerType.SALES -> {
                    ConfigField("Vender si factor ≥", sellAtFactor) { sellAtFactor = it }
                    ConfigField("Mantener stock", keepStock) { keepStock = it }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Proteger inputs activos", color = Paper,
                            modifier = Modifier.weight(1f), fontSize = 12.sp)
                        Switch(
                            checked = protectInputs,
                            onCheckedChange = { protectInputs = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Gold)
                        )
                    }
                    Text(
                        if (whitelist.isEmpty()) "Whitelist vacía → solo bienes finales"
                        else "Whitelist: ${whitelist.size} recursos",
                        color = Dim, fontSize = 10.sp
                    )
                    WhitelistEditor(
                        currentWhitelist = whitelist,
                        onChange = { whitelist = it }
                    )
                }
                ManagerType.PURCHASING ->
                    ConfigField("Comprar si factor ≤", buyBelowFactor) { buyBelowFactor = it }
                ManagerType.HR -> {
                    ConfigField("Despedir si lealtad ≤", autoFire) { autoFire = it }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Auto-contratar", color = Paper,
                            modifier = Modifier.weight(1f), fontSize = 12.sp)
                        Switch(
                            checked = autoHire,
                            onCheckedChange = { autoHire = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Gold)
                        )
                    }
                }
                ManagerType.FINANCE ->
                    ConfigField("Amortizar si caja > N × reserva", loanRatio) { loanRatio = it }
            }
        }
    }
}

@Composable
private fun ConfigField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

@Composable
private fun WhitelistEditor(
    currentWhitelist: Set<String>,
    onChange: (Set<String>) -> Unit
) {
    val resourcesByCat = ResourceCatalog.all.groupBy { it.category }
    Column(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(top = 6.dp)
    ) {
        androidx.compose.foundation.lazy.LazyColumn {
            resourcesByCat.forEach { (cat, list) ->
                item {
                    Text(
                        cat.name,
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                items(list, key = { it.id }) { res ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${res.emoji} ${res.name}", color = Paper, fontSize = 11.sp,
                            modifier = Modifier.weight(1f))
                        Switch(
                            checked = res.id in currentWhitelist,
                            onCheckedChange = { checked ->
                                onChange(
                                    if (checked) currentWhitelist + res.id
                                    else currentWhitelist - res.id
                                )
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Gold)
                        )
                    }
                }
            }
        }
    }
}
