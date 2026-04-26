package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.model.GameState
import com.empiretycoon.game.model.QualityTier
import com.empiretycoon.game.model.ResourceCatalog
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.QualityBadge
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.components.colorForTier
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney

/**
 * Pantalla de inventario por calidad. Agrupa los recursos por tier
 * (con cabecera y emoji) y permite vender un tier concreto.
 */
@Composable
fun QualityInventoryScreen(state: GameState, vm: GameViewModel) {
    val qInv = state.qualityInventory
    val flat = qInv.flatList()
    val byTier: Map<QualityTier, List<Triple<String, QualityTier, Int>>> =
        flat.groupBy { it.second }
    var sellTarget by remember { mutableStateOf<Triple<String, QualityTier, Int>?>(null) }

    Column(Modifier.fillMaxSize()) {
        if (flat.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Aún no se han producido lotes con calidad", color = Dim)
                Spacer(Modifier.height(4.dp))
                Text("Asigna recetas avanzadas (adv_) o crea líneas para empezar.",
                    color = Dim, fontSize = 12.sp)
            }
            return
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Cabecera resumen
            item {
                EmpireCard {
                    SectionTitle("Inventario por calidad",
                        subtitle = "${flat.sumOf { it.third }} unidades en ${byTier.size} calidades")
                }
            }
            QualityTier.ascending.reversed().forEach { tier ->
                val entries = byTier[tier].orEmpty()
                if (entries.isEmpty()) return@forEach
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(tier.emoji, fontSize = 22.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            tier.label,
                            color = colorForTier(tier),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "x${"%.2f".format(tier.mult)} valor",
                            color = Dim, fontSize = 11.sp
                        )
                    }
                }
                items(entries) { triple ->
                    val (rid, _, qty) = triple
                    val res = ResourceCatalog.tryById(rid) ?: return@items
                    val basePrice = state.market.sellPriceOf(rid)
                    val tierPrice = basePrice * tier.mult
                    EmpireCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(res.emoji, fontSize = 24.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(res.name, fontWeight = FontWeight.SemiBold)
                                QualityBadge(tier = tier, compact = true)
                                Text(
                                    "Precio venta: ${tierPrice.fmtMoney()} (base ${basePrice.fmtMoney()})",
                                    color = Dim, fontSize = 11.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("x$qty", fontWeight = FontWeight.Bold,
                                    color = Gold, fontSize = 18.sp)
                                Spacer(Modifier.height(4.dp))
                                Button(
                                    onClick = { sellTarget = triple },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Emerald, contentColor = Ink
                                    )
                                ) { Text("Vender") }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(60.dp)) }
        }
    }

    sellTarget?.let { (rid, tier, qty) ->
        SellQualityDialog(
            resourceId = rid,
            tier = tier,
            available = qty,
            unitPrice = state.market.sellPriceOf(rid) * tier.mult,
            onConfirm = { n ->
                vm.sellQuality(rid, tier, n)
                sellTarget = null
            },
            onClose = { sellTarget = null }
        )
    }
}

@Composable
private fun SellQualityDialog(
    resourceId: String,
    tier: QualityTier,
    available: Int,
    unitPrice: Double,
    onConfirm: (Int) -> Unit,
    onClose: () -> Unit
) {
    var amount by remember { mutableStateOf(1) }
    val res = ResourceCatalog.tryById(resourceId) ?: return
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(res.emoji, fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Vender ${res.name}")
                    QualityBadge(tier = tier, compact = true)
                }
            }
        },
        text = {
            Column {
                Text("Disponible: $available", color = Dim)
                Text("Precio unitario: ${unitPrice.fmtMoney()}", color = Gold)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { amount = (amount - 1).coerceAtLeast(1) }
                    ) { Text("-1") }
                    Spacer(Modifier.width(6.dp))
                    Text("$amount", fontWeight = FontWeight.Bold,
                        modifier = Modifier.widthIn(min = 40.dp))
                    Spacer(Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = { amount = (amount + 1).coerceAtMost(available) }
                    ) { Text("+1") }
                    Spacer(Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = { amount = available }
                    ) { Text("Máx") }
                }
                Spacer(Modifier.height(8.dp))
                Text("Total: ${(unitPrice * amount).fmtMoney()}",
                    fontWeight = FontWeight.Bold, color = Gold)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amount.coerceAtMost(available)) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Emerald, contentColor = Ink)
            ) { Text("Vender") }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Cancelar") }
        },
        containerColor = InkSoft
    )
}
