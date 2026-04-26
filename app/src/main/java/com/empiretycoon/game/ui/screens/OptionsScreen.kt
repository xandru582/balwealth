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
fun OptionsScreen(state: GameState, vm: GameViewModel) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = tab,
            containerColor = InkSoft,
            contentColor = Gold
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Comprar") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Mis posiciones") })
        }
        when (tab) {
            0 -> BuyOptionsTab(state, vm)
            1 -> PositionsTab(state, vm)
        }
    }
}

// ===== TAB 0: Comprar =====

@Composable
private fun BuyOptionsTab(state: GameState, vm: GameViewModel) {
    var ticker by rememberSaveable { mutableStateOf(state.stocks.firstOrNull()?.ticker ?: "") }
    var isCall by rememberSaveable { mutableStateOf(true) }
    var strikeMultPct by rememberSaveable { mutableStateOf(100) } // 100 = ATM
    var expiryDays by rememberSaveable { mutableStateOf(30) }

    val stock = state.stocks.find { it.ticker == ticker }
    val strike = stock?.let { it.price * strikeMultPct / 100.0 } ?: 0.0
    val expiryTick = OptionsPricer.expiryTickFromDays(state.tick, expiryDays)
    val ticksToExp = expiryTick - state.tick
    val premium = stock?.let {
        OptionsPricer.fairPremium(it, strike, ticksToExp, isCall)
    } ?: 0.0
    val total = premium + OptionsPricer.BROKERAGE_FEE

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                SectionTitle("Configurar contrato",
                    subtitle = "1 contrato = ${OptionsPricer.DEFAULT_CONTRACT_SIZE} acciones.")
            }
        }
        // Selector de stock
        item {
            EmpireCard {
                Text("Subyacente", color = Dim, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                LazyRowSimple(state.stocks.map { it.ticker }) { t ->
                    FilterChip(
                        selected = ticker == t,
                        onClick = { ticker = t },
                        label = { Text(t) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold,
                            selectedLabelColor = Ink
                        )
                    )
                }
                if (stock != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("${stock.companyName} · spot ${stock.price.fmtMoney()} · vol ${(stock.volatility * 100).toInt()}%",
                        color = Dim, fontSize = 11.sp)
                }
            }
        }
        // Call/Put
        item {
            EmpireCard {
                Text("Tipo", color = Dim, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Row {
                    SegmentedBtn("CALL", isCall, Emerald) { isCall = true }
                    Spacer(Modifier.width(6.dp))
                    SegmentedBtn("PUT", !isCall, Ruby) { isCall = false }
                }
            }
        }
        // Strike
        item {
            EmpireCard {
                Text("Strike (% del spot): $strikeMultPct%", color = Paper, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Slider(
                    value = strikeMultPct.toFloat(),
                    onValueChange = { strikeMultPct = it.toInt() },
                    valueRange = 70f..130f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = Gold,
                        activeTrackColor = Gold,
                        inactiveTrackColor = InkBorder
                    )
                )
                Text("Strike: ${strike.fmtMoney()}", color = Gold, fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold)
            }
        }
        // Vencimiento
        item {
            EmpireCard {
                Text("Vencimiento: $expiryDays días", color = Paper, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                LazyRowSimple(OptionsCatalog.EXPIRY_DAYS.map { it.toString() }) { d ->
                    FilterChip(
                        selected = expiryDays == d.toInt(),
                        onClick = { expiryDays = d.toInt() },
                        label = { Text("${d}d") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Sapphire,
                            selectedLabelColor = Paper
                        )
                    )
                }
            }
        }
        // Resumen y compra
        item {
            EmpireCard(borderColor = Gold) {
                SectionTitle("Resumen")
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Prima", color = Dim, fontSize = 11.sp)
                        Text(premium.fmtMoney(), fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Comisión", color = Dim, fontSize = 11.sp)
                        Text(OptionsPricer.BROKERAGE_FEE.fmtMoney(), fontWeight = FontWeight.SemiBold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Total", color = Dim, fontSize = 11.sp)
                        Text(total.fmtMoney(),
                            fontWeight = FontWeight.Bold,
                            color = if (state.company.cash >= total) Gold else Ruby)
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (stock != null) {
                    val be = if (isCall) strike + premium / OptionsPricer.DEFAULT_CONTRACT_SIZE
                             else strike - premium / OptionsPricer.DEFAULT_CONTRACT_SIZE
                    Text("Breakeven: ${be.fmtMoney()}", color = Sapphire, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (isCall) vm.buyCallOption(ticker, strike, expiryTick)
                        else vm.buyPutOption(ticker, strike, expiryTick)
                    },
                    enabled = stock != null && state.company.cash >= total,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCall) Emerald else Ruby,
                        contentColor = if (isCall) Ink else Paper,
                        disabledContainerColor = InkBorder, disabledContentColor = Dim
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Comprar ${if (isCall) "CALL" else "PUT"}",
                        fontWeight = FontWeight.Bold)
                }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

// ===== TAB 1: Posiciones =====

@Composable
private fun PositionsTab(state: GameState, vm: GameViewModel) {
    val openCalls = state.options.calls.filterNot { it.closed }
    val openPuts = state.options.puts.filterNot { it.closed }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                SectionTitle("Resumen de la mesa")
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Posiciones abiertas", color = Dim, fontSize = 11.sp)
                        Text("${state.options.totalPositions()}", fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Capital expuesto", color = Dim, fontSize = 11.sp)
                        Text(state.options.totalPremiumOpen().fmtMoney(),
                            fontWeight = FontWeight.SemiBold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("P&L realizado", color = Dim, fontSize = 11.sp)
                        Text(state.options.totalRealizedPnL.fmtMoney(),
                            fontWeight = FontWeight.Bold,
                            color = if (state.options.totalRealizedPnL >= 0) Emerald else Ruby)
                    }
                }
            }
        }
        if (openCalls.isEmpty() && openPuts.isEmpty()) {
            item {
                Text("No tienes posiciones abiertas.",
                    color = Dim, modifier = Modifier.padding(16.dp))
            }
        }
        if (openCalls.isNotEmpty()) {
            item { SectionTitle("Calls abiertas") }
            items(openCalls, key = { it.id }) { c -> CallCard(c, state, vm) }
        }
        if (openPuts.isNotEmpty()) {
            item { SectionTitle("Puts abiertas") }
            items(openPuts, key = { it.id }) { p -> PutCard(p, state, vm) }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun CallCard(c: CallOption, state: GameState, vm: GameViewModel) {
    val stock = state.stocks.find { it.ticker == c.ticker }
    val spot = stock?.price ?: 0.0
    val intrinsic = c.intrinsicValue(spot)
    val unrealized = intrinsic - c.premiumPaid
    val daysLeft = c.ticksToExpiry(state.tick) / 1_440L
    val itm = spot > c.strikePrice
    EmpireCard(borderColor = if (itm) Emerald else InkBorder) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("CALL", fontWeight = FontWeight.Black, color = Emerald)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("${c.ticker} @ ${c.strikePrice.fmtMoney()}",
                    fontWeight = FontWeight.SemiBold)
                Text("Spot ${spot.fmtMoney()} · ${if (itm) "ITM" else "OTM"}",
                    color = if (itm) Emerald else Ruby, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(unrealized.fmtMoney(),
                    color = if (unrealized >= 0) Emerald else Ruby,
                    fontWeight = FontWeight.Bold)
                Text("$daysLeft d restantes", color = Dim, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Prima ${c.premiumPaid.fmtMoney()} · BE ${c.breakeven().fmtMoney()}",
            color = Dim, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        Row {
            Button(
                onClick = { vm.exerciseOption(c.id) },
                enabled = itm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Emerald, contentColor = Ink,
                    disabledContainerColor = InkBorder, disabledContentColor = Dim),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) { Text("Ejercer", fontSize = 12.sp) }
            Spacer(Modifier.width(6.dp))
            Text("o deja expirar",
                color = Dim, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterVertically))
        }
    }
}

@Composable
private fun PutCard(p: PutOption, state: GameState, vm: GameViewModel) {
    val stock = state.stocks.find { it.ticker == p.ticker }
    val spot = stock?.price ?: 0.0
    val intrinsic = p.intrinsicValue(spot)
    val unrealized = intrinsic - p.premiumPaid
    val daysLeft = p.ticksToExpiry(state.tick) / 1_440L
    val itm = spot < p.strikePrice
    EmpireCard(borderColor = if (itm) Emerald else InkBorder) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("PUT", fontWeight = FontWeight.Black, color = Ruby)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("${p.ticker} @ ${p.strikePrice.fmtMoney()}",
                    fontWeight = FontWeight.SemiBold)
                Text("Spot ${spot.fmtMoney()} · ${if (itm) "ITM" else "OTM"}",
                    color = if (itm) Emerald else Ruby, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(unrealized.fmtMoney(),
                    color = if (unrealized >= 0) Emerald else Ruby,
                    fontWeight = FontWeight.Bold)
                Text("$daysLeft d restantes", color = Dim, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Prima ${p.premiumPaid.fmtMoney()} · BE ${p.breakeven().fmtMoney()}",
            color = Dim, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        Row {
            Button(
                onClick = { vm.exerciseOption(p.id) },
                enabled = itm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Emerald, contentColor = Ink,
                    disabledContainerColor = InkBorder, disabledContentColor = Dim),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) { Text("Ejercer", fontSize = 12.sp) }
            Spacer(Modifier.width(6.dp))
            Text("o deja expirar",
                color = Dim, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterVertically))
        }
    }
}

// ===== Helpers UI =====

@Composable
private fun SegmentedBtn(label: String, selected: Boolean,
                        accent: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) accent else InkBorder,
            contentColor = if (selected) Ink else Paper
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
    ) { Text(label, fontWeight = FontWeight.Bold) }
}

@Composable
private fun LazyRowSimple(items: List<String>, content: @Composable (String) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(items) { content(it) }
    }
}
