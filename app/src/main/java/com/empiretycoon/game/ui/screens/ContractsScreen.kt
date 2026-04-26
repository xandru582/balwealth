package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.empiretycoon.game.ui.components.ProgressBarWithLabel
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney
import com.empiretycoon.game.util.fmtTimeSeconds

@Composable
fun ContractsScreen(state: GameState, vm: GameViewModel) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab, containerColor = InkSoft, contentColor = Gold) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0 },
                text = { Text("Ofertas (${state.contracts.offers.size})") }
            )
            Tab(
                selected = tab == 1,
                onClick = { tab = 1 },
                text = { Text("Activos (${state.contracts.accepted.size})") }
            )
            Tab(
                selected = tab == 2,
                onClick = { tab = 2 },
                text = { Text("Histórico") }
            )
        }
        when (tab) {
            0 -> OffersTab(state, vm)
            1 -> AcceptedTab(state, vm)
            2 -> HistoryTab(state)
        }
    }
}

@Composable
private fun OffersTab(state: GameState, vm: GameViewModel) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                SectionTitle(
                    "Ofertas B2B",
                    subtitle = "Las ofertas se renuevan cada día (~24 min)."
                )
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { vm.refreshContracts() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Sapphire, contentColor = Paper
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Actualizar lista") }
            }
        }
        if (state.contracts.offers.isEmpty()) {
            item {
                Text(
                    "No hay ofertas activas. Vuelve más tarde.",
                    color = Dim,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(state.contracts.offers, key = { it.id }) { c ->
                OfferCard(c, vm)
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun OfferCard(c: Contract, vm: GameViewModel) {
    EmpireCard(borderColor = Gold.copy(alpha = 0.45f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.clientLogo, fontSize = 28.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(c.clientName, fontWeight = FontWeight.Bold)
                Text(
                    "Tier ${c.tier} · ${c.totalRequested} uds.",
                    color = Dim, fontSize = 11.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    c.totalPaymentEstimate.fmtMoney(),
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                    fontSize = 14.sp
                )
                Text("+${c.bonusOnTime.fmtMoney()} bonus",
                    color = Emerald, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Column {
            for ((rid, qty) in c.items) {
                val res = ResourceCatalog.tryById(rid) ?: continue
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Ink)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(res.emoji)
                    Spacer(Modifier.width(8.dp))
                    Text(res.name, color = Paper, modifier = Modifier.weight(1f),
                        fontSize = 12.sp)
                    Text(
                        "x$qty @ ${(c.paymentPerUnit[rid] ?: 0.0).fmtMoney()}",
                        color = Dim, fontSize = 11.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Plazo: ${c.deadlineSeconds.toInt().fmtTimeSeconds()}",
                color = Dim, fontSize = 11.sp
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Multa: ${c.penaltyMissed.fmtMoney()}",
                color = Ruby, fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        Row {
            Button(
                onClick = { vm.acceptContract(c.id) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Emerald, contentColor = Ink
                )
            ) { Text("Aceptar") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { vm.rejectContract(c.id) },
                modifier = Modifier.weight(1f)
            ) { Text("Descartar") }
        }
    }
}

@Composable
private fun AcceptedTab(state: GameState, vm: GameViewModel) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        if (state.contracts.accepted.isEmpty()) {
            item {
                Text(
                    "Aún no has aceptado ningún contrato.",
                    color = Dim,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(state.contracts.accepted, key = { it.id }) { c ->
                AcceptedCard(state, c, vm)
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun AcceptedCard(state: GameState, c: Contract, vm: GameViewModel) {
    val left = c.secondsLeft(state.tick)
    val urgencyColor = when {
        left < 240L -> Ruby           // <4 min reales
        left < 720L -> Color(0xFFFFB05A)
        else -> Emerald
    }
    EmpireCard(borderColor = urgencyColor.copy(alpha = 0.7f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.clientLogo, fontSize = 26.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(c.clientName, fontWeight = FontWeight.Bold)
                Text(
                    "Pago: ${c.totalPaymentEstimate.fmtMoney()} · Bonus ${c.bonusOnTime.fmtMoney()}",
                    color = Dim, fontSize = 11.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "⏱ ${left.toInt().fmtTimeSeconds()}",
                    color = urgencyColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text("Multa: ${c.penaltyMissed.fmtMoney()}",
                    color = Ruby, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        ProgressBarWithLabel(
            progress = c.progress,
            label = "Progreso global ${(c.progress * 100).toInt()}%",
            color = Emerald
        )
        Spacer(Modifier.height(6.dp))
        for ((rid, needed) in c.items) {
            val res = ResourceCatalog.tryById(rid) ?: continue
            val have = state.company.inventory[rid] ?: 0
            val delivered = c.deliveredQty[rid] ?: 0
            val remaining = (needed - delivered).coerceAtLeast(0)
            val partial = if (needed == 0) 0f else (delivered.toFloat() / needed)

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Ink)
                    .border(1.dp, InkBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(res.emoji)
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(res.name, color = Paper, fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold)
                        Text(
                            "$delivered / $needed (stock $have)",
                            color = Dim, fontSize = 10.sp
                        )
                    }
                    if (remaining > 0 && have > 0) {
                        TextButton(onClick = {
                            vm.deliverContract(c.id, rid, minOf(have, remaining))
                        }) {
                            Text("Entregar", color = Gold, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { partial.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (partial >= 1f) Emerald else Sapphire,
                    trackColor = InkBorder
                )
            }
        }
    }
}

@Composable
private fun HistoryTab(state: GameState) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                SectionTitle("Resumen B2B")
                Spacer(Modifier.height(6.dp))
                StatRow("Contratos completados", "${state.contracts.completedTotal}")
                StatRow("Contratos vencidos", "${state.contracts.expiredTotal}")
                StatRow("Ingresos totales", state.contracts.totalEarnings.fmtMoney())
                StatRow("Reputación actual", "${state.company.reputation}/100")
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Dim, modifier = Modifier.weight(1f), fontSize = 12.sp)
        Text(value, color = Paper, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
