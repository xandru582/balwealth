package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.model.*
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney

/** Pantalla de heists. Tres tabs: heists / tripulación / planificación activa. */
@Composable
fun HeistsScreen(state: GameState, vm: GameViewModel) {
    val hs = state.heists

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SectionTitle(
            "🦹 Inframundo",
            subtitle = "High risk, high reward. Karma -, heat +."
        )
        Spacer(Modifier.height(8.dp))

        if (!hs.unlocked) {
            EmpireCard {
                Text("🔒 Heists bloqueados", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Necesitas nivel ≥ 5 para acceder al inframundo.",
                    color = Dim, fontSize = 13.sp
                )
                Spacer(Modifier.height(10.dp))
                val canUnlock = state.player.level >= 5
                Button(onClick = vm::heistsUnlock, enabled = canUnlock) {
                    Text(if (canUnlock) "Entrar al inframundo" else "Aún no tienes nivel suficiente")
                }
            }
            return
        }

        // Stats
        EmpireCard {
            Text("Estadísticas", fontWeight = FontWeight.Bold, color = Gold)
            Text("Heists totales: ${hs.totalHeists} · perfectos: ${hs.perfectRuns} · desastres: ${hs.disasters}",
                color = Dim, fontSize = 12.sp)
            Text("Botín lifetime: ${hs.totalLoot.fmtMoney()}", color = Dim, fontSize = 12.sp)
            val heatColor = when {
                hs.heat >= 70 -> Ruby
                hs.heat >= 40 -> Gold
                else -> Emerald
            }
            Text("🚨 Heat: ${hs.heat}/100", color = heatColor, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))

        SectionTitle("👥 Tripulación")
        Spacer(Modifier.height(6.dp))
        EmpireCard {
            Text("Pool disponible (rota cada 7 días)", color = Dim, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            val recruitedSet = hs.recruitedCrew.toSet()
            for (m in hs.crewPool.filter { it.available }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${m.role.emoji} ${m.name}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            "Skill ${m.skill} · cut ${(m.cutPct * 100).toInt()}% · fee ${m.recruitFee.fmtMoney()}",
                            color = Dim, fontSize = 11.sp
                        )
                    }
                    if (m.id in recruitedSet) {
                        TextButton(onClick = { vm.heistFireCrew(m.id) }) {
                            Text("Despedir", color = Ruby)
                        }
                    } else {
                        TextButton(onClick = { vm.heistRecruit(m.id) }) {
                            Text("Fichar", color = Emerald)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        SectionTitle("🎯 Heists disponibles")
        Spacer(Modifier.height(6.dp))
        for (h in hs.heists) {
            val def = HeistCatalog.byType(h.type) ?: continue
            HeistCard(h, def, hs, vm)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HeistCard(
    inst: HeistInstance,
    def: HeistDef,
    hs: HeistState,
    vm: GameViewModel
) {
    var selectedCrew by remember(inst.id) { mutableStateOf<List<String>>(emptyList()) }
    var approach by remember(inst.id) { mutableStateOf(HeistApproach.STEALTH) }
    var gear by remember(inst.id) { mutableStateOf("50000") }

    val border = when (inst.status) {
        HeistStatus.LOCKED -> Dim
        HeistStatus.AVAILABLE -> Sapphire
        HeistStatus.PLANNING -> Gold
        HeistStatus.EXECUTING -> Gold
        HeistStatus.COMPLETED -> Emerald
        HeistStatus.COOLDOWN -> Ruby.copy(alpha = 0.5f)
    }
    EmpireCard(borderColor = border) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(def.type.emoji, fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(def.type.displayName, fontWeight = FontWeight.Bold)
                Text(
                    "Botín base ${def.baseReward.fmtMoney()} · dificultad ${def.baseDifficulty}",
                    color = Dim, fontSize = 11.sp
                )
            }
            Text(inst.status.name, color = Gold, fontSize = 11.sp)
        }
        Text(def.description, color = Dim, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "Roles: ${def.requiredRoles.joinToString { it.emoji }}",
            color = Dim, fontSize = 11.sp
        )
        if (inst.outcome != null) {
            Text(
                "Último resultado: ${inst.outcome.emoji} ${inst.outcome.displayName} · ${inst.payoutCash.fmtMoney()}",
                color = Gold, fontSize = 11.sp
            )
        }

        when (inst.status) {
            HeistStatus.LOCKED -> {
                Spacer(Modifier.height(4.dp))
                Text("🔒 Requiere lvl ${def.unlockLevel} · rep ${def.unlockReputation}",
                    color = Dim, fontSize = 11.sp)
            }
            HeistStatus.AVAILABLE -> {
                Spacer(Modifier.height(8.dp))
                Text("Selecciona tripulación:", color = Dim, fontSize = 11.sp)
                Row {
                    for (id in hs.recruitedCrew) {
                        val m = hs.crewPool.find { it.id == id } ?: continue
                        val selected = id in selectedCrew
                        Box(
                            Modifier
                                .padding(end = 4.dp, top = 2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) Gold.copy(alpha = 0.3f) else InkSoft)
                                .border(1.dp, if (selected) Gold else InkBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    selectedCrew = if (selected) selectedCrew - id else selectedCrew + id
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text("${m.role.emoji} ${m.name.take(8)}", fontSize = 10.sp)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row {
                    for (a in HeistApproach.values()) {
                        TextButton(onClick = { approach = a }) {
                            Text(
                                "${a.emoji} ${a.displayName}",
                                color = if (approach == a) Gold else Dim,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = gear,
                    onValueChange = { gear = it },
                    label = { Text("Gasto en equipo (€)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        val g = gear.toDoubleOrNull() ?: 0.0
                        vm.heistPlan(inst.id, selectedCrew, approach, g)
                    },
                    enabled = selectedCrew.isNotEmpty()
                ) {
                    Text("Planificar")
                }
            }
            HeistStatus.PLANNING -> {
                Spacer(Modifier.height(6.dp))
                Button(onClick = { vm.heistExecute(inst.id) }) {
                    Text("🔥 EJECUTAR HEIST", color = Ruby)
                }
            }
            HeistStatus.COOLDOWN -> {
                Text("⏳ En cooldown", color = Ruby, fontSize = 11.sp)
            }
            else -> { /* status: COMPLETED, EXECUTING — sin acción */ }
        }
    }
}
