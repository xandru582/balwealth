package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import com.empiretycoon.game.util.fmtNumber
import com.empiretycoon.game.util.fmtPct

/**
 * Pantalla del mercado cripto. Lista los 6 tokens, muestra histórico
 * sparkline, y permite operar (buy/sell/stake/mining).
 */
@Composable
fun CryptoScreen(state: GameState, vm: GameViewModel) {
    val crypto = state.crypto

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SectionTitle(
            "🪙 Mercado cripto",
            subtitle = "Volatilidad pura. Cuidado con los rugpulls."
        )
        Spacer(Modifier.height(8.dp))

        if (!crypto.unlocked) {
            EmpireCard {
                Text("🔒 Mercado bloqueado", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Necesitas 250.000 € en caja y reputación ≥ 30 para acceder.",
                    color = Dim, fontSize = 13.sp
                )
                Spacer(Modifier.height(10.dp))
                val canUnlock = state.company.cash >= 250_000.0 && state.company.reputation >= 30
                Button(onClick = vm::cryptoUnlock, enabled = canUnlock) {
                    Text(if (canUnlock) "Desbloquear cripto" else "Aún no cumples requisitos")
                }
            }
            return
        }

        // Resumen de portfolio
        EmpireCard {
            val totalLiquid = crypto.holdings.sumOf { h ->
                val price = crypto.token(h.symbol)?.price ?: 0.0
                (h.amount + h.staked + h.miningPending) * price
            }
            Text("Tu portfolio cripto", fontWeight = FontWeight.Bold, color = Gold)
            Spacer(Modifier.height(4.dp))
            Text("Valor estimado: ${totalLiquid.fmtMoney()}")
            Text("PnL realizado lifetime: ${crypto.realizedPnl.fmtMoney()}", color = Dim, fontSize = 12.sp)
            Text("Rugpulls sobrevividos: ${crypto.rugpullsSurvived}", color = Dim, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))

        for (tok in crypto.tokens) {
            val def = CryptoCatalog.byMatching(tok.symbol) ?: continue
            val holding = crypto.holdingOrEmpty(tok.symbol)
            CryptoTokenCard(tok, def, holding, vm)
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(12.dp))
        SectionTitle("📰 Feed cripto")
        Spacer(Modifier.height(6.dp))
        if (crypto.newsFeed.isEmpty()) {
            Text("Sin noticias por ahora.", color = Dim, fontSize = 12.sp)
        } else {
            for (item in crypto.newsFeed.takeLast(8).reversed()) {
                EmpireCard {
                    Text(item.title, fontWeight = FontWeight.Bold)
                    Text(item.body, color = Dim, fontSize = 12.sp)
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun CryptoTokenCard(
    state: CryptoState,
    def: CryptoToken,
    holding: CryptoHolding,
    vm: GameViewModel
) {
    var qtyText by remember(state.symbol) { mutableStateOf("0") }
    var stakeDays by remember(state.symbol) { mutableStateOf("7") }

    val isUp = state.history.size >= 2 && state.history.last() >= state.history[state.history.size - 2]
    val color = when {
        state.rugged -> Ruby
        isUp -> Emerald
        else -> Ruby
    }

    EmpireCard(borderColor = color.copy(alpha = 0.6f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(def.emoji, fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("${def.name} (${def.symbol})", fontWeight = FontWeight.Bold)
                Text(
                    "${state.price.fmtNumber()} € · vol ${(def.volatility * 100).toInt()}%",
                    color = Dim, fontSize = 12.sp
                )
            }
            if (state.rugged) {
                Text("RUGGED 💀", color = Ruby, fontWeight = FontWeight.Black)
            } else {
                Sparkline(state.history, color)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Posición: ${holding.amount.fmtNumber()} líquido · ${holding.staked.fmtNumber()} stake · ${holding.miningPending.fmtNumber()} pendiente",
            color = Dim, fontSize = 11.sp
        )

        if (!state.rugged) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("Cantidad") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    TextButton(onClick = {
                        qtyText.toDoubleOrNull()?.let { vm.cryptoBuy(state.symbol, it) }
                    }) { Text("Comprar", color = Emerald) }
                    TextButton(onClick = {
                        qtyText.toDoubleOrNull()?.let { vm.cryptoSell(state.symbol, it) }
                    }) { Text("Vender", color = Ruby) }
                }
            }
            // Stake row
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = stakeDays,
                    onValueChange = { stakeDays = it },
                    label = { Text("Días stake (APY ${(def.stakeApy * 100).toInt()}%)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    val q = qtyText.toDoubleOrNull() ?: 0.0
                    val d = stakeDays.toIntOrNull() ?: 7
                    vm.cryptoStake(state.symbol, q, d)
                }) { Text("Stake", color = Sapphire) }
                if (holding.staked > 0) {
                    TextButton(onClick = { vm.cryptoUnstake(state.symbol) }) {
                        Text("Unstake", color = Gold)
                    }
                }
            }
            // Mining row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "⛏️ Mineros: ${holding.minersAssigned} · pendiente ${holding.miningPending.fmtNumber()}",
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { vm.cryptoAssignMiners(state.symbol, -1) }) { Text("-1") }
                TextButton(onClick = { vm.cryptoAssignMiners(state.symbol, +1) }) { Text("+1") }
                if (holding.miningPending > 0.000001) {
                    TextButton(onClick = { vm.cryptoClaimMining(state.symbol) }) {
                        Text("Claim", color = Emerald)
                    }
                }
            }
        }
    }
}

@Composable
private fun Sparkline(history: List<Double>, color: Color) {
    if (history.size < 2) return
    val mn = history.min()
    val mx = history.max()
    val range = (mx - mn).takeIf { it > 0 } ?: 1.0
    Canvas(
        Modifier
            .size(width = 80.dp, height = 28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(InkSoft)
    ) {
        val w = size.width
        val h = size.height
        val n = history.size
        val step = w / (n - 1).coerceAtLeast(1)
        var prev = Offset(0f, h - ((history[0] - mn) / range).toFloat() * h)
        for (i in 1 until n) {
            val x = i * step
            val y = h - ((history[i] - mn) / range).toFloat() * h
            val cur = Offset(x, y)
            drawLine(color, prev, cur, strokeWidth = 1.5f)
            prev = cur
        }
    }
}
