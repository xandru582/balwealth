package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun ResearchScreen(state: GameState, vm: GameViewModel) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                SectionTitle("Laboratorio",
                    subtitle = "Desbloquea recetas y bonificaciones permanentes.")
                val tech = state.research.inProgressId?.let { TechCatalog.byId(it) }
                if (tech != null) {
                    Spacer(Modifier.height(10.dp))
                    Text("En curso: ${tech.name}", color = Emerald, fontWeight = FontWeight.Bold)
                    val frac = 1f - (state.research.inProgressSecondsLeft
                        / tech.researchSeconds.toDouble()).toFloat()
                    ProgressBarWithLabel(frac,
                        label = "${state.research.inProgressSecondsLeft.toInt()}s restantes",
                        color = Emerald)
                } else {
                    Spacer(Modifier.height(6.dp))
                    Text("Sin investigación activa", color = Dim, fontSize = 13.sp)
                }
            }
        }

        items(TechCatalog.all, key = { it.id }) { t ->
            val done = state.research.isCompleted(t.id)
            val active = state.research.inProgressId == t.id
            val prereqOk = t.prerequisites.all { state.research.isCompleted(it) }
            val canStart = state.research.canStart(t)

            EmpireCard(
                borderColor = if (done) Emerald else InkBorder
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (done) "✅" else if (active) "⏳" else "🔬", fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.name, fontWeight = FontWeight.Bold)
                        Text(t.description, color = Dim, fontSize = 12.sp)
                        val bonuses = buildList {
                            if (t.productionBonus > 0) add("+${(t.productionBonus * 100).toInt()}% producción")
                            if (t.marketBonus > 0) add("+${(t.marketBonus * 100).toInt()}% precio venta")
                            if (t.unlocksRecipeIds.isNotEmpty()) add("${t.unlocksRecipeIds.size} recetas")
                        }.joinToString(" · ")
                        if (bonuses.isNotEmpty()) {
                            Text(bonuses, color = Gold, fontSize = 11.sp)
                        }
                        if (t.prerequisites.isNotEmpty()) {
                            val names = t.prerequisites.mapNotNull { TechCatalog.byId(it)?.name }
                                .joinToString(", ")
                            Text("Requiere: $names",
                                color = if (prereqOk) Dim else Ruby, fontSize = 11.sp)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(t.cost.fmtMoney(), fontWeight = FontWeight.Bold,
                            color = if (state.company.cash >= t.cost) Gold else Ruby)
                        Text("${t.researchSeconds}s", color = Dim, fontSize = 10.sp)
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = { vm.startResearch(t.id) },
                            enabled = canStart && state.company.cash >= t.cost,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Sapphire, contentColor = Paper,
                                disabledContainerColor = InkBorder, disabledContentColor = Dim
                            )
                        ) {
                            Text(when {
                                done -> "Hecho"
                                active -> "Activa"
                                else -> "Investigar"
                            })
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}
