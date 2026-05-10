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
import androidx.compose.ui.window.Dialog
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.model.*
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney

/** Pantalla del comercio internacional. */
@Composable
fun MultiCityScreen(state: GameState, vm: GameViewModel) {
    val mc = state.multiCity
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (!mc.unlocked) {
            MultiCityLocked(state, vm)
        } else {
            MultiCityUnlocked(state, vm)
        }
    }
}

// ===================== LOCKED =====================

@Composable
private fun MultiCityLocked(state: GameState, vm: GameViewModel) {
    SectionTitle(
        "🌐 Imperio global",
        subtitle = "Comercia con 5 ciudades extranjeras. Cada una tiene sus aranceles, demandas y volatilidad."
    )
    Spacer(Modifier.height(12.dp))

    val canUnlock = MultiCityEngineCanUnlock(state)
    EmpireCard(borderColor = if (canUnlock) Sapphire else InkBorder) {
        Text("Requisitos", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Paper)
        Text(
            "💰 Cash mínimo: 1.000.000 € (tienes ${state.company.cash.fmtMoney()})",
            color = if (state.company.cash >= 1_000_000.0) Emerald else Color(0xFFFF7A7A),
            fontSize = 12.sp
        )
        Text(
            "⭐ Reputación: ≥ 50 (tienes ${state.company.reputation})",
            color = if (state.company.reputation >= 50) Emerald else Color(0xFFFF7A7A),
            fontSize = 12.sp
        )
    }
    Spacer(Modifier.height(12.dp))

    EmpireCard {
        Text("Cómo funciona", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Paper)
        Text(
            "1. Abres una ruta a una ciudad (one-time fee = 30 días de mantenimiento).\n" +
                "2. Envías mercancía: pagas un arancel de importación al salir.\n" +
                "3. Tras N días el envío llega y se vende automáticamente al precio remoto.\n" +
                "4. La ciudad cobra arancel de exportación + impuesto local sobre el beneficio neto.\n" +
                "5. El cash limpio entra a tu empresa.",
            color = Dim, fontSize = 12.sp
        )
    }
    Spacer(Modifier.height(16.dp))

    Button(
        onClick = { vm.multiCityUnlock() },
        enabled = canUnlock,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Gold)
    ) {
        Text("🌐 Desbloquear comercio internacional",
            fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

private fun MultiCityEngineCanUnlock(state: GameState): Boolean =
    state.company.cash >= 1_000_000.0 && state.company.reputation >= 50

// ===================== UNLOCKED =====================

@Composable
private fun MultiCityUnlocked(state: GameState, vm: GameViewModel) {
    val mc = state.multiCity
    val openRoutes = mc.routes.count { it.open }

    // ----- Header lifetime -----
    EmpireCard(borderColor = Gold) {
        Text("📊 Resumen", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Gold)
        Spacer(Modifier.height(4.dp))
        Row {
            StatBlock("Rutas abiertas", "$openRoutes / ${mc.routes.size}")
            Spacer(Modifier.width(12.dp))
            StatBlock("En tránsito", "${mc.shipments.size}")
            Spacer(Modifier.width(12.dp))
            StatBlock("Lifetime envíos", "${mc.totalShipped}")
        }
        Spacer(Modifier.height(6.dp))
        Row {
            StatBlock("Bruto lifetime", mc.totalRevenue.fmtMoney())
            Spacer(Modifier.width(12.dp))
            StatBlock("Neto lifetime", mc.totalNet.fmtMoney(), color = Emerald)
        }
    }
    Spacer(Modifier.height(16.dp))

    // ----- Estado: cuál ruta usar para enviar -----
    var shipDialogRouteId by remember { mutableStateOf<String?>(null) }

    // ----- Ciudades extranjeras + sus rutas -----
    SectionTitle("🌍 Ciudades", subtitle = "Cada ciudad tiene 3 oportunidades destacadas y su perfil fiscal.")
    Spacer(Modifier.height(8.dp))
    val foreign = mc.cities.filter { it.id != CityId.HOME }
    for (city in foreign) {
        CityCard(
            city = city,
            routes = mc.routes.filter { it.to == city.id || it.from == city.id },
            onOpen = { id -> vm.multiCityOpenRoute(id) },
            onClose = { id -> vm.multiCityCloseRoute(id) },
            onShipFrom = { id -> shipDialogRouteId = id }
        )
        Spacer(Modifier.height(10.dp))
    }

    // ----- Envíos en tránsito -----
    Spacer(Modifier.height(8.dp))
    SectionTitle("🚚 Envíos en tránsito", subtitle = "Se liquidan automáticamente al llegar.")
    Spacer(Modifier.height(8.dp))
    if (mc.shipments.isEmpty()) {
        EmpireCard {
            Text("No hay envíos en camino. Pulsa 'Enviar' en una ruta abierta para iniciar uno.",
                color = Dim, fontSize = 12.sp)
        }
    } else {
        for (sh in mc.shipments.sortedBy { it.arrivesAtTick }) {
            ShipmentRow(sh, state.tick)
            Spacer(Modifier.height(6.dp))
        }
    }

    // ----- Historial -----
    Spacer(Modifier.height(12.dp))
    SectionTitle("📜 Últimos liquidados")
    Spacer(Modifier.height(6.dp))
    if (mc.history.isEmpty()) {
        EmpireCard {
            Text("Aún no se ha liquidado ningún envío.", color = Dim, fontSize = 12.sp)
        }
    } else {
        for (h in mc.history.takeLast(5).reversed()) {
            HistoryRow(h)
            Spacer(Modifier.height(4.dp))
        }
    }

    // ----- Modal de envío -----
    val routeForDialog = shipDialogRouteId?.let { id -> mc.routes.find { it.id == id } }
    if (routeForDialog != null) {
        ShipDialog(
            state = state,
            route = routeForDialog,
            onDismiss = { shipDialogRouteId = null },
            onConfirm = { resourceId, qty ->
                vm.multiCityShip(routeForDialog.id, resourceId, qty)
                shipDialogRouteId = null
            }
        )
    }
}

@Composable
private fun StatBlock(label: String, value: String, color: Color = Paper) {
    Column {
        Text(label, color = Dim, fontSize = 10.sp)
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// ===================== City card =====================

@Composable
private fun CityCard(
    city: CityMarket,
    routes: List<CityRoute>,
    onOpen: (String) -> Unit,
    onClose: (String) -> Unit,
    onShipFrom: (String) -> Unit
) {
    EmpireCard(borderColor = if (routes.any { it.open }) Gold else InkBorder) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(InkBorder),
                contentAlignment = Alignment.Center
            ) {
                Text(city.id.emoji, fontSize = 28.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(city.id.displayName, fontWeight = FontWeight.Bold, color = Paper, fontSize = 15.sp)
                Text(city.id.country, color = Dim, fontSize = 11.sp)
            }
            // Sentiment dot
            val sentColor = when {
                city.sentiment >= 1.10 -> Emerald
                city.sentiment <= 0.90 -> Color(0xFFFF7A7A)
                else -> Dim
            }
            Text("●", color = sentColor, fontSize = 22.sp)
        }
        Spacer(Modifier.height(6.dp))

        // Top opportunities (3 with highest mul * demand)
        val ops = topOpportunities(city, n = 3)
        if (ops.isNotEmpty()) {
            Text("📈 Oportunidades", color = Dim, fontSize = 11.sp)
            for ((resId, score) in ops) {
                val res = ResourceCatalog.tryById(resId) ?: continue
                Text(
                    "${res.emoji} ${res.name}: x${"%.2f".format(score)}",
                    color = Emerald, fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "FX ${"%.2f".format(city.exchangeRate)} · " +
                "import ${"%.0f".format(city.importTariff * 100)}% · " +
                "export ${"%.0f".format(city.exportTariff * 100)}% · " +
                "tax ${"%.0f".format(city.localTax * 100)}%",
            color = Dim, fontSize = 10.sp
        )

        // Routes
        Spacer(Modifier.height(8.dp))
        for (route in routes) {
            RouteRow(route, onOpen, onClose, onShipFrom)
        }
    }
}

private fun topOpportunities(city: CityMarket, n: Int): List<Pair<String, Double>> {
    val scores = city.priceMultipliers.keys.map { id ->
        val mul = city.priceMultipliers[id] ?: 1.0
        val dem = city.demand[id] ?: 1.0
        id to mul * dem * city.exchangeRate * city.sentiment
    }
    return scores.sortedByDescending { it.second }.take(n)
}

@Composable
private fun RouteRow(
    route: CityRoute,
    onOpen: (String) -> Unit,
    onClose: (String) -> Unit,
    onShipFrom: (String) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(InkBorder)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "${route.from.emoji} → ${route.to.emoji}",
                color = Paper, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
            )
            Text(
                "Mantenimiento ${route.dailyCost.fmtMoney()}/día · tránsito ${route.transitTicks / 1_440L}d · cap ${route.capacityPerShipment}",
                color = Dim, fontSize = 10.sp
            )
        }
        if (route.open) {
            TextButton(onClick = { onShipFrom(route.id) }) {
                Text("Enviar", color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            TextButton(onClick = { onClose(route.id) }) {
                Text("Cerrar", color = Dim, fontSize = 12.sp)
            }
        } else {
            TextButton(onClick = { onOpen(route.id) }) {
                Text(
                    "Abrir (${(route.dailyCost * 30).fmtMoney()})",
                    color = Sapphire, fontWeight = FontWeight.Bold, fontSize = 12.sp
                )
            }
        }
    }
}

// ===================== Shipment row =====================

@Composable
private fun ShipmentRow(sh: CityShipment, nowTick: Long) {
    val res = ResourceCatalog.tryById(sh.resourceId)
    val ticksLeft = sh.ticksLeft(nowTick)
    val daysLeft = ticksLeft / 1_440L
    val hoursLeft = (ticksLeft % 1_440L) / 60L
    EmpireCard(borderColor = Sapphire) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(res?.emoji ?: "📦", fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${sh.qty} × ${res?.name ?: sh.resourceId} → ${sh.to.displayName}",
                    color = Paper, fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    "Llega en ${daysLeft}d ${hoursLeft}h · arancel pagado ${sh.importTariffPaid.fmtMoney()}",
                    color = Dim, fontSize = 11.sp
                )
            }
        }
    }
}

// ===================== History row =====================

@Composable
private fun HistoryRow(h: CityShipmentResult) {
    val res = ResourceCatalog.tryById(h.resourceId)
    val net = h.net
    val color = if (net >= 0) Emerald else Color(0xFFFF7A7A)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(InkBorder)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(res?.emoji ?: "📦", fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            "Día ${h.day}: ${h.qty} ${res?.name ?: h.resourceId} → ${h.to.displayName}",
            color = Paper, fontSize = 11.sp, modifier = Modifier.weight(1f)
        )
        Text(net.fmtMoney(), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ===================== Ship dialog =====================

@Composable
private fun ShipDialog(
    state: GameState,
    route: CityRoute,
    onDismiss: () -> Unit,
    onConfirm: (resourceId: String, qty: Int) -> Unit
) {
    val inventoryEntries = state.company.inventory
        .filterValues { it > 0 }
        .toList()
        .sortedByDescending { it.second }
    var selected by remember { mutableStateOf(inventoryEntries.firstOrNull()?.first) }
    var qtyText by remember { mutableStateOf("100") }
    val destCity = MultiCityCatalog.cityById(state.multiCity, route.to)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(InkSoft)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    "🚚 Enviar a ${route.to.displayName}",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Gold
                )
                Text(
                    "Cap por envío: ${route.capacityPerShipment} · tránsito ${route.transitTicks / 1_440L}d",
                    color = Dim, fontSize = 11.sp
                )
                Spacer(Modifier.height(12.dp))

                if (inventoryEntries.isEmpty()) {
                    Text("No tienes inventario para enviar.", color = Dim, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Cerrar")
                    }
                } else {
                    Text("Recurso", color = Dim, fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    Column(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                        for ((id, qty) in inventoryEntries) {
                            val res = ResourceCatalog.tryById(id) ?: continue
                            val isSel = selected == id
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Gold.copy(alpha = 0.15f) else InkBorder)
                                    .clickable { selected = id }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(res.emoji, fontSize = 16.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(res.name, color = Paper, fontSize = 12.sp,
                                    modifier = Modifier.weight(1f))
                                Text("×$qty", color = Dim, fontSize = 11.sp)
                                if (isSel) {
                                    Spacer(Modifier.width(6.dp))
                                    Text("✓", color = Gold, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { v -> qtyText = v.filter { it.isDigit() }.take(6) },
                        label = { Text("Cantidad", color = Dim) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))

                    val qty = qtyText.toIntOrNull() ?: 0
                    val have = selected?.let { state.inventoryOf(it) } ?: 0
                    val baseCost = selected?.let { state.market.buyPriceOf(it) } ?: 0.0
                    val tariff = baseCost * qty * (destCity?.importTariff ?: 0.0)
                    Text(
                        "Tienes $have · arancel a pagar ahora: ${tariff.fmtMoney()}",
                        color = Dim, fontSize = 11.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    Row {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar", color = Dim)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val r = selected
                                val q = qty
                                if (r != null && q > 0) onConfirm(r, q)
                            },
                            enabled = selected != null && qty in 1..route.capacityPerShipment &&
                                qty <= (selected?.let { state.inventoryOf(it) } ?: 0) &&
                                state.company.cash >= tariff,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold)
                        ) {
                            Text("Enviar", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Texto orientativo del beneficio aproximado: precio remoto × cantidad × demanda × FX − aranceles − impuesto.",
                        color = Dim, fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
