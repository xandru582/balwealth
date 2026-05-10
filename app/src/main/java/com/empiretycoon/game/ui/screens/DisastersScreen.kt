package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.engine.MitigationStrategy
import com.empiretycoon.game.model.*
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney

/** Pantalla de gestión de desastres y seguro. */
@Composable
fun DisastersScreen(state: GameState, vm: GameViewModel) {
    val ds = state.disasters

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SectionTitle(
            "🌪️ Sala de crisis",
            subtitle = "Desastres dinámicos. Mitiga a tiempo o paga el precio."
        )
        Spacer(Modifier.height(8.dp))

        // Insurance
        EmpireCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("🛡️ Seguro empresarial", fontWeight = FontWeight.Bold)
                    Text(
                        "Prima: ${ds.insuranceDailyCost.fmtMoney()}/día · cobertura ${(ds.insuranceCoverage * 100).toInt()}%",
                        color = Dim, fontSize = 12.sp
                    )
                }
                Switch(
                    checked = ds.insuranceActive,
                    onCheckedChange = { vm.disasterToggleInsurance(it) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        EmpireCard {
            Text("Resiliencia", fontWeight = FontWeight.Bold, color = Gold)
            Spacer(Modifier.height(4.dp))
            Text("XP de resiliencia: ${ds.resilienceXp}")
            Text("Desastres superados: ${ds.history.size}", color = Dim, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))

        if (ds.active.isEmpty()) {
            EmpireCard {
                Text("✅ Sin crisis activas", fontWeight = FontWeight.Bold, color = Emerald)
                Text("Todo bajo control. Los desastres aparecen cada 30+ días.",
                    color = Dim, fontSize = 12.sp)
            }
        } else {
            for (d in ds.active) {
                ActiveDisasterCard(d, state, vm)
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle("📋 Histórico")
        Spacer(Modifier.height(6.dp))
        if (ds.history.isEmpty()) {
            Text("Sin desastres registrados todavía.", color = Dim, fontSize = 12.sp)
        } else {
            for (h in ds.history.takeLast(8).reversed()) {
                EmpireCard {
                    Text("Día ${h.day} · ${h.kind.name}", fontWeight = FontWeight.Bold)
                    Text(
                        "Severidad ${h.severity.name} · ${h.outcome} · pérdidas ${h.cashLost.fmtMoney()}",
                        color = Dim, fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ActiveDisasterCard(d: ActiveDisaster, state: GameState, vm: GameViewModel) {
    EmpireCard(borderColor = Ruby) {
        Text(d.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ruby)
        Text(d.description, color = Dim, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text("Severidad: ${d.severity.name} · Fase: ${d.phase.name}",
            color = Dim, fontSize = 11.sp)
        if (d.productionMul != 1.0)
            Text("Producción ×${"%.2f".format(d.productionMul)}", color = Gold, fontSize = 11.sp)
        if (d.sellPriceMul != 1.0)
            Text("Precios venta ×${"%.2f".format(d.sellPriceMul)}", color = Gold, fontSize = 11.sp)
        if (d.buyPriceMul != 1.0)
            Text("Precios compra ×${"%.2f".format(d.buyPriceMul)}", color = Gold, fontSize = 11.sp)

        if (d.phase == DisasterPhase.PENDING_RESPONSE && !d.mitigated) {
            Spacer(Modifier.height(8.dp))
            Text("⏳ Tienes 24h in-game para mitigar:",
                color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            for (strat in MitigationStrategy.values()) {
                TextButton(onClick = { vm.disasterMitigate(d.id, strat) }) {
                    Text("${strat.emoji} ${strat.label}")
                }
            }
        } else if (d.mitigated) {
            Text("✅ Mitigación aplicada", color = Emerald, fontSize = 12.sp)
        }
    }
}
