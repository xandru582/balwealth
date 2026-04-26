package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
fun WealthScreen(state: GameState, vm: GameViewModel) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = tab,
            containerColor = InkSoft,
            contentColor = Gold
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Bolsa") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Inmuebles") })
        }
        when (tab) {
            0 -> StocksTab(state, vm)
            1 -> RealEstateTab(state, vm)
        }
    }
}

// ===== Bolsa =====

@Composable
private fun StocksTab(state: GameState, vm: GameViewModel) {
    val portfolio = state.stocks.sumOf { s ->
        (state.holdings.shares[s.ticker] ?: 0) * s.price
    }
    val cost = state.stocks.sumOf { s ->
        (state.holdings.shares[s.ticker] ?: 0) * (state.holdings.avgCost[s.ticker] ?: 0.0)
    }
    val pnl = portfolio - cost

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                SectionTitle("Tu cartera")
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Valor actual", color = Dim, fontSize = 11.sp)
                        Text(portfolio.fmtMoney(), fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("P&L", color = Dim, fontSize = 11.sp)
                        Text(pnl.fmtMoney(),
                            color = if (pnl >= 0) Emerald else Ruby,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(state.stocks, key = { it.ticker }) { s -> StockRow(s, state, vm) }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun StockRow(s: Stock, state: GameState, vm: GameViewModel) {
    val shares = state.holdings.shares[s.ticker] ?: 0
    val avg = state.holdings.avgCost[s.ticker] ?: 0.0
    EmpireCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${s.ticker} · ${s.companyName}", fontWeight = FontWeight.Bold)
                Text("Volatilidad ${(s.volatility*100).toInt()}%",
                    color = Dim, fontSize = 11.sp)
                if (shares > 0) {
                    Text("Posees $shares · coste medio ${avg.fmtMoney()}",
                        color = Emerald, fontSize = 11.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(s.price.fmtMoney(), fontWeight = FontWeight.Bold,
                    color = if (s.trend >= 0) Emerald else Ruby)
                if (s.priceHistory.isNotEmpty()) {
                    Sparkline(s.priceHistory, if (s.trend >= 0) Emerald else Ruby)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        var showCustom by remember(s.ticker) { mutableStateOf(false) }
        Row {
            Button(
                onClick = { vm.buyShares(s.ticker, 1) },
                enabled = state.company.cash >= s.price,
                colors = ButtonDefaults.buttonColors(containerColor = Sapphire, contentColor = Paper),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) { Text("+1", fontSize = 12.sp) }
            Spacer(Modifier.width(4.dp))
            Button(
                onClick = { vm.buyShares(s.ticker, 10) },
                enabled = state.company.cash >= s.price * 10,
                colors = ButtonDefaults.buttonColors(containerColor = Sapphire, contentColor = Paper),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) { Text("+10", fontSize = 12.sp) }
            Spacer(Modifier.width(4.dp))
            // Botón Max: compra todas las que pueda con la caja actual
            val maxBuy = (state.company.cash / s.price).toInt().coerceAtLeast(0)
            Button(
                onClick = { if (maxBuy > 0) vm.buyShares(s.ticker, maxBuy) },
                enabled = maxBuy >= 1,
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) { Text("MAX ($maxBuy)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(4.dp))
            // Botón "..." para introducir cantidad personalizada
            OutlinedButton(
                onClick = { showCustom = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) { Text("…", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(4.dp))
        Row {
            Button(
                onClick = { vm.sellShares(s.ticker, 1) },
                enabled = shares >= 1,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Ink),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) { Text("Vender 1", fontSize = 12.sp) }
            Spacer(Modifier.width(4.dp))
            Button(
                onClick = { vm.sellShares(s.ticker, 10) },
                enabled = shares >= 10,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Ink),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) { Text("Vender 10", fontSize = 12.sp) }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { vm.sellShares(s.ticker, shares) },
                enabled = shares >= 1,
                colors = ButtonDefaults.buttonColors(containerColor = Ruby, contentColor = Paper),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) { Text("Vender TODO ($shares)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }

        if (showCustom) {
            val affordable = (state.company.cash / s.price).toInt().coerceAtLeast(0)
            CustomQuantityDialog(
                title = "Comprar acciones de ${s.ticker}",
                pricePerUnit = s.price,
                cash = state.company.cash,
                maxAffordable = affordable,
                maxAvailable = Int.MAX_VALUE,
                onConfirm = { qty ->
                    vm.buyShares(s.ticker, qty)
                    showCustom = false
                },
                onCancel = { showCustom = false }
            )
        }
    }
}

@Composable
private fun CustomQuantityDialog(
    title: String,
    pricePerUnit: Double,
    cash: Double,
    maxAffordable: Int,
    maxAvailable: Int,
    onConfirm: (Int) -> Unit,
    onCancel: () -> Unit
) {
    var input by remember { mutableStateOf("1") }
    val qty = input.toIntOrNull() ?: 0
    val total = qty * pricePerUnit
    val canBuy = qty in 1..maxAffordable && qty <= maxAvailable
    com.empiretycoon.game.ui.components.CompactDialog(
        title = title,
        onDismiss = onCancel,
        footer = {
            TextButton(onClick = onCancel) { Text("Cancelar", color = Dim) }
            Spacer(Modifier.width(4.dp))
            TextButton(onClick = { if (canBuy) onConfirm(qty) }, enabled = canBuy) {
                Text("Comprar", color = if (canBuy) Gold else Dim)
            }
        }
    ) {
        Text("Caja: ${cash.fmtMoney()} · Precio: ${pricePerUnit.fmtMoney()}",
            color = Dim, fontSize = 11.sp)
        Text("Máx asequible: $maxAffordable", color = Dim, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it.filter { c -> c.isDigit() }.take(9) },
            label = { Text("Cantidad", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            )
        )
        Spacer(Modifier.height(4.dp))
        Row {
            listOf(10, 50, 100, 500).forEach { q ->
                OutlinedButton(
                    onClick = { input = q.toString() },
                    modifier = Modifier.padding(end = 4.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) { Text("$q", fontSize = 10.sp) }
            }
            OutlinedButton(
                onClick = { input = maxAffordable.toString() },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold)
            ) { Text("MAX", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Total: ${total.fmtMoney()}",
            color = if (canBuy) Emerald else Ruby,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        if (qty > maxAffordable) {
            Text("No tienes suficiente caja.", color = Ruby, fontSize = 10.sp)
        }
    }
}

@Composable
private fun Sparkline(values: List<Double>, color: androidx.compose.ui.graphics.Color) {
    val min = values.min()
    val max = values.max()
    val range = (max - min).coerceAtLeast(0.01)
    Canvas(Modifier.size(80.dp, 22.dp)) {
        val w = size.width
        val h = size.height
        val step = w / (values.size - 1).coerceAtLeast(1)
        var prev: Offset? = null
        values.forEachIndexed { i, v ->
            val x = i * step
            val y = h - ((v - min) / range * h).toFloat()
            val cur = Offset(x, y)
            prev?.let { p -> drawLine(color, p, cur, 1.5f, StrokeCap.Round) }
            prev = cur
        }
    }
}

// ===== Inmuebles =====

@Composable
private fun RealEstateTab(state: GameState, vm: GameViewModel) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionTitle("Patrimonio inmobiliario",
                            subtitle = "Rentas pasivas diarias.")
                        Text("Net diario: ${state.realEstate.dailyNet.fmtMoney()}",
                            color = if (state.realEstate.dailyNet >= 0) Emerald else Ruby,
                            fontWeight = FontWeight.Bold)
                        Text("Valor total: ${state.realEstate.totalValue.fmtMoney()}",
                            color = Dim, fontSize = 12.sp)
                    }
                    TextButton(onClick = { vm.refreshRealEstate() }) {
                        Text("Nuevas ofertas", color = Sapphire)
                    }
                }
            }
        }

        item { SectionTitle("Mis propiedades") }
        if (state.realEstate.owned.isEmpty()) {
            item { Text("No tienes inmuebles.", color = Dim, modifier = Modifier.padding(16.dp)) }
        } else {
            items(state.realEstate.owned, key = { it.id }) { p ->
                EmpireCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.type.emoji, fontSize = 28.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${p.type.displayName} — ${p.nickname}",
                                fontWeight = FontWeight.SemiBold)
                            Text("Compra ${p.purchasePrice.fmtMoney()}",
                                color = Dim, fontSize = 11.sp)
                            Text("Renta/día ${p.rentPerDay.fmtMoney()} · Mant ${p.maintenancePerDay.fmtMoney()}",
                                color = Emerald, fontSize = 11.sp)
                        }
                        TextButton(onClick = { vm.sellProperty(p.id) }) {
                            Text("Vender", color = Ruby)
                        }
                    }
                }
            }
        }

        item { SectionTitle("En venta") }
        items(state.realEstate.available, key = { it.id }) { p ->
            EmpireCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.type.emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${p.type.displayName} — ${p.nickname}",
                            fontWeight = FontWeight.SemiBold)
                        Text("Renta/día ${p.rentPerDay.fmtMoney()}",
                            color = Emerald, fontSize = 11.sp)
                        val roi = if (p.purchasePrice > 0)
                            (p.rentPerDay - p.maintenancePerDay) * 365 / p.purchasePrice * 100 else 0.0
                        Text("ROI ~ ${"%.1f".format(roi)}% anual",
                            color = Dim, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(p.purchasePrice.fmtMoney(),
                            fontWeight = FontWeight.Bold,
                            color = if (state.company.cash >= p.purchasePrice) Gold else Ruby)
                        Button(
                            onClick = { vm.buyProperty(p.id) },
                            enabled = state.company.cash >= p.purchasePrice,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Gold, contentColor = Ink,
                                disabledContainerColor = InkBorder, disabledContentColor = Dim
                            )
                        ) { Text("Comprar") }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}
