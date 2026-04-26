package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
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
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney

@Composable
fun MarketScreen(state: GameState, vm: GameViewModel) {
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val scroll = rememberScrollState()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = filter == null,
                onClick = { filter = null },
                label = { Text("Todo") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Gold,
                    selectedLabelColor = Ink
                )
            )
            ResourceCategory.values().forEach { cat ->
                FilterChip(
                    selected = filter == cat.name,
                    onClick = { filter = if (filter == cat.name) null else cat.name },
                    label = { Text(cat.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Gold,
                        selectedLabelColor = Ink
                    )
                )
            }
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            val items = ResourceCatalog.all.filter { filter == null || it.category.name == filter }
            items(items, key = { it.id }) { r -> ResourceRow(r, state, vm) }
            item { Spacer(Modifier.height(60.dp)) }
        }
    }
}

@Composable
private fun ResourceRow(r: Resource, state: GameState, vm: GameViewModel) {
    val factor = state.market.priceFactors[r.id] ?: 1.0
    val trend = state.market.priceTrend[r.id] ?: 0.0
    val buy = state.market.buyPriceOf(r.id)
    val sell = state.market.sellPriceOf(r.id)
    val have = state.inventoryOf(r.id)

    var expanded by remember(r.id) { mutableStateOf(false) }

    EmpireCard {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(r.emoji, fontSize = 24.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(r.name, fontWeight = FontWeight.SemiBold)
                Text(r.category.name, color = Dim, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.market.priceOf(r.id).fmtMoney(),
                        fontWeight = FontWeight.Bold,
                        color = when {
                            factor > 1.1 -> Emerald
                            factor < 0.9 -> Ruby
                            else -> Paper
                        }
                    )
                    Icon(
                        if (trend > 0) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                        null,
                        tint = if (trend > 0) Emerald else Ruby,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text("Stock: $have", color = Dim, fontSize = 11.sp)
            }
            Spacer(Modifier.width(4.dp))
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "▲" else "▼", color = Gold)
            }
        }
        if (expanded) {
            Spacer(Modifier.height(6.dp))
            Column {
                Text("Compra: ${buy.fmtMoney()} · Venta: ${sell.fmtMoney()}",
                    color = Dim, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row {
                    QtyButton("Comprar 1") { vm.buy(r.id, 1) }
                    Spacer(Modifier.width(6.dp))
                    QtyButton("x10") { vm.buy(r.id, 10) }
                    Spacer(Modifier.width(6.dp))
                    QtyButton("x50") { vm.buy(r.id, 50) }
                }
                Spacer(Modifier.height(6.dp))
                Row {
                    SellButton("Vender 1", have >= 1) { vm.sell(r.id, 1) }
                    Spacer(Modifier.width(6.dp))
                    SellButton("x10", have >= 10) { vm.sell(r.id, 10) }
                    Spacer(Modifier.width(6.dp))
                    SellButton("Todo", have >= 1) { vm.sell(r.id, have) }
                }
            }
        }
    }
}

@Composable
private fun QtyButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Sapphire, contentColor = Paper),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) { Text(label, fontSize = 12.sp) }
}

@Composable
private fun SellButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Emerald, contentColor = Ink,
            disabledContainerColor = InkBorder, disabledContentColor = Dim
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) { Text(label, fontSize = 12.sp) }
}
