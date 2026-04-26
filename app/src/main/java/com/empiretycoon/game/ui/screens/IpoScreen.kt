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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.empiretycoon.game.util.fmtPct

@Composable
fun IpoScreen(state: GameState, vm: GameViewModel) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item { IpoStatusCard(state, vm) }
        when (state.ipo.phase) {
            IPOPhase.LOCKED -> item { RequirementsCard(state) }
            IPOPhase.PROSPECTUS -> item { ProspectusCard(state, vm) }
            IPOPhase.ROADSHOW -> item { RoadshowCard(state, vm) }
            IPOPhase.LISTED -> {
                item { ListedCard(state, vm) }
                item { SellDownCard(state, vm) }
                if (state.ipo.dividendHistory.isNotEmpty()) {
                    item { SectionTitle("Historial de dividendos") }
                    items(state.ipo.dividendHistory.takeLast(8), key = { it.atTick }) { d ->
                        DividendRow(d)
                    }
                }
                if (state.ipo.splitHistory.isNotEmpty()) {
                    item { SectionTitle("Historial de splits") }
                    items(state.ipo.splitHistory.takeLast(6), key = { it.atTick }) { s ->
                        SplitRow(s)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun IpoStatusCard(state: GameState, vm: GameViewModel) {
    val ipo = state.ipo
    EmpireCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ipo.phase.emoji, fontSize = 32.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                SectionTitle("Salida a bolsa", subtitle = ipo.phase.displayName)
                if (ipo.projectedValuation > 0)
                    Text("Valoración estimada: ${ipo.projectedValuation.fmtMoney()}",
                        color = Gold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun RequirementsCard(state: GameState) {
    EmpireCard {
        SectionTitle("Requisitos para presentar el folleto")
        Spacer(Modifier.height(6.dp))
        ReqRow("Cash ≥ ${IPOConstraints.MIN_CASH.fmtMoney()}",
            state.company.cash, IPOConstraints.MIN_CASH.toDouble())
        ReqRow("Reputación ≥ ${IPOConstraints.MIN_REPUTATION}",
            state.company.reputation.toDouble(),
            IPOConstraints.MIN_REPUTATION.toDouble())
        ReqRow("Nivel ≥ ${IPOConstraints.MIN_LEVEL}",
            state.company.level.toDouble(),
            IPOConstraints.MIN_LEVEL.toDouble())
    }
}

@Composable
private fun ProspectusCard(state: GameState, vm: GameViewModel) {
    val ipo = state.ipo
    val ticksSince = state.tick - ipo.prospectusFiledAt
    val progress = (ticksSince.toFloat() /
        IPOConstraints.PROSPECTUS_REVIEW_TICKS.toFloat()).coerceIn(0f, 1f)
    val ready = ticksSince >= IPOConstraints.PROSPECTUS_REVIEW_TICKS
    EmpireCard(borderColor = Sapphire) {
        SectionTitle("Folleto presentado",
            subtitle = "El regulador está revisando.")
        Spacer(Modifier.height(6.dp))
        ProgressBarWithLabel(
            progress = progress,
            label = if (ready) "Revisión completada" else "Revisión en curso",
            color = if (ready) Emerald else Sapphire
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { vm.completeRoadshow() },
            enabled = ready,
            colors = ButtonDefaults.buttonColors(
                containerColor = Gold, contentColor = Ink,
                disabledContainerColor = InkBorder, disabledContentColor = Dim
            ),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Iniciar roadshow") }
    }
}

@Composable
private fun RoadshowCard(state: GameState, vm: GameViewModel) {
    val ipo = state.ipo
    val ticksSince = state.tick - ipo.roadshowStartedAt
    val progress = (ticksSince.toFloat() /
        IPOConstraints.ROADSHOW_TICKS.toFloat()).coerceIn(0f, 1f)
    val ready = ticksSince >= IPOConstraints.ROADSHOW_TICKS
    val daysLeft = ((IPOConstraints.ROADSHOW_TICKS - ticksSince).coerceAtLeast(0L) / 1_440L).toInt()
    EmpireCard(borderColor = Gold) {
        SectionTitle("Roadshow inversor",
            subtitle = if (ready) "Listo para listar" else "Faltan $daysLeft días")
        Spacer(Modifier.height(6.dp))
        ProgressBarWithLabel(
            progress = progress,
            label = "Convenciendo a fondos",
            color = Gold
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { vm.listOnExchange() },
            enabled = ready,
            colors = ButtonDefaults.buttonColors(
                containerColor = Gold, contentColor = Ink,
                disabledContainerColor = InkBorder, disabledContentColor = Dim
            ),
            modifier = Modifier.fillMaxWidth()
        ) { Text("¡Toque la campana!", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun ListedCard(state: GameState, vm: GameViewModel) {
    val listed = state.ipo.listed ?: return
    EmpireCard(borderColor = Emerald) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(listed.ticker, fontWeight = FontWeight.Black, fontSize = 22.sp,
                    color = Gold)
                Text(state.company.name, color = Dim, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(listed.currentPrice.fmtMoney(), fontWeight = FontWeight.Bold)
                val change = (listed.currentPrice / listed.ipoPrice) - 1.0
                Text(change.fmtPct(),
                    color = if (change >= 0) Emerald else Ruby, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (listed.history.size >= 2) {
            IpoSparkline(listed.history, if (listed.currentPrice >= listed.ipoPrice) Emerald else Ruby)
            Spacer(Modifier.height(8.dp))
        }
        Row {
            ChipBig("Capitalización", listed.marketCap.fmtMoney(), Gold)
            Spacer(Modifier.width(8.dp))
            ChipBig("Tu participación",
                "${"%.1f".format(listed.playerStakePct * 100)}%", Sapphire)
        }
        Spacer(Modifier.height(6.dp))
        Row {
            ChipBig("Yield dividendo", listed.dividendYield.fmtPct(), Emerald)
            Spacer(Modifier.width(8.dp))
            ChipBig("Splits aplicados", "${listed.splitsApplied}", Paper)
        }
        Spacer(Modifier.height(8.dp))
        Text("Acciones que retienes: ${listed.sharesOwnedByPlayer} de ${listed.sharesOutstanding}",
            color = Dim, fontSize = 11.sp)
        Text("Valor de tu paquete: ${listed.playerStakeValue.fmtMoney()}",
            color = Gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SellDownCard(state: GameState, vm: GameViewModel) {
    val listed = state.ipo.listed ?: return
    var amount by rememberSaveable { mutableStateOf(0L) }
    val maxSell = listed.sharesOwnedByPlayer
    EmpireCard {
        SectionTitle("Vender parte de tu paquete",
            subtitle = "Diluir tu participación a precio de mercado.")
        Spacer(Modifier.height(6.dp))
        Text("Vendes: $amount acciones de $maxSell",
            color = Paper, fontSize = 12.sp)
        Slider(
            value = amount.toFloat(),
            onValueChange = { amount = it.toLong() },
            valueRange = 0f..maxSell.toFloat().coerceAtLeast(1f),
            colors = SliderDefaults.colors(
                thumbColor = Gold,
                activeTrackColor = Gold,
                inactiveTrackColor = InkBorder
            )
        )
        Text("Ingreso estimado: ${(listed.currentPrice * amount).fmtMoney()}",
            color = Emerald, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Row {
            QuickBtn("10%") { amount = (maxSell * 10 / 100).coerceAtLeast(1) }
            Spacer(Modifier.width(6.dp))
            QuickBtn("25%") { amount = (maxSell * 25 / 100).coerceAtLeast(1) }
            Spacer(Modifier.width(6.dp))
            QuickBtn("50%") { amount = (maxSell * 50 / 100).coerceAtLeast(1) }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    vm.sellDownStake(amount)
                    amount = 0
                },
                enabled = amount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Ink)
            ) { Text("Vender", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun DividendRow(d: Dividend) {
    EmpireCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("💰", fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("${d.ticker} · ${"%.4f".format(d.amountPerShare)} €/acción",
                    fontWeight = FontWeight.SemiBold)
                Text("Total: ${d.totalPaid.fmtMoney()}", color = Dim, fontSize = 11.sp)
            }
            Text("T${d.atTick}", color = Dim, fontSize = 10.sp)
        }
    }
}

@Composable
private fun SplitRow(s: StockSplit) {
    EmpireCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✂", fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("${s.ticker} ${s.ratio}x1", fontWeight = FontWeight.SemiBold)
                Text("Stock split", color = Dim, fontSize = 11.sp)
            }
            Text("T${s.atTick}", color = Dim, fontSize = 10.sp)
        }
    }
}

// ============= Helpers UI =============

@Composable
private fun ReqRow(label: String, current: Double, required: Double) {
    val ok = current >= required
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(if (ok) "✔" else "✖",
            color = if (ok) Emerald else Ruby, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text(label, color = Paper, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text("${current.toLong()} / ${required.toLong()}",
            color = if (ok) Emerald else Dim, fontSize = 11.sp)
    }
}

@Composable
private fun ChipBig(label: String, value: String, color: Color) {
    Column {
        Text(label, color = Dim, fontSize = 10.sp)
        Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 13.sp)
    }
}

@Composable
private fun QuickBtn(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = InkBorder, contentColor = Paper),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
    ) { Text(label, fontSize = 11.sp) }
}

@Composable
private fun IpoSparkline(values: List<Double>, color: Color) {
    val min = values.min()
    val max = values.max()
    val range = (max - min).coerceAtLeast(0.01)
    Canvas(Modifier.fillMaxWidth().height(40.dp)) {
        val w = size.width
        val h = size.height
        val step = w / (values.size - 1).coerceAtLeast(1)
        var prev: Offset? = null
        values.forEachIndexed { i, v ->
            val x = i * step
            val y = h - ((v - min) / range * h).toFloat()
            val cur = Offset(x, y)
            prev?.let { p -> drawLine(color, p, cur, 2f, StrokeCap.Round) }
            prev = cur
        }
    }
}
