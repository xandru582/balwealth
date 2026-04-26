package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.engine.PrestigeEngine
import com.empiretycoon.game.model.*
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.components.StatPill
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney

@Composable
fun PrestigeScreen(state: GameState, vm: GameViewModel) {
    var confirmRebirth by remember { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val pointsIfRebirth = remember(state) { PrestigeEngine.computePoints(state) }

    val perks = remember(filter) {
        if (filter == null) PrestigePerkCatalog.all
        else PrestigePerkCatalog.all.filter { it.category.name == filter }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard(borderColor = Gold) {
                SectionTitle(
                    "Prestigio",
                    subtitle = "Renace para empezar de nuevo y ganar perks permanentes."
                )
                Spacer(Modifier.height(10.dp))
                Row {
                    StatPill(
                        "Nivel prestigio",
                        "${state.prestige.prestigeLevel}",
                        emoji = "🌟",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    StatPill(
                        "Puntos disp.",
                        "${state.prestige.prestigePoints}",
                        emoji = "✨",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    StatPill(
                        "Total ganados",
                        "${state.prestige.totalPointsEarned}",
                        emoji = "🏆",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    StatPill(
                        "Cash vital.",
                        state.prestige.lifetimeCash.fmtMoney(),
                        emoji = "💰",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            EmpireCard(borderColor = Ruby) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥", fontSize = 30.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Renacer ahora",
                            fontWeight = FontWeight.Bold, color = Ruby
                        )
                        Text(
                            "Si renaces ahora ganarías:",
                            color = Dim, fontSize = 12.sp
                        )
                        Text(
                            "+$pointsIfRebirth puntos de prestigio",
                            color = Gold, fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                        Text(
                            "Caja: ${state.company.cash.fmtMoney()} · Edif: ${state.company.buildings.size} · Lv: ${state.player.level}",
                            color = Dim, fontSize = 11.sp
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { confirmRebirth = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = pointsIfRebirth > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Ruby, contentColor = Paper,
                        disabledContainerColor = InkBorder,
                        disabledContentColor = Dim
                    )
                ) {
                    Text(
                        "RENACER",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            }
        }

        item { SectionTitle("Perks permanentes") }

        item {
            PerkCategoryChips(
                selected = filter,
                onSelect = { filter = it }
            )
        }

        items(perks, key = { it.id }) { p ->
            PerkCard(
                perk = p,
                state = state,
                onBuy = { vm.buyPrestigePerk(p.id) }
            )
        }
        item { Spacer(Modifier.height(60.dp)) }
    }

    if (confirmRebirth) {
        AlertDialog(
            onDismissRequest = { confirmRebirth = false },
            confirmButton = {
                TextButton(onClick = {
                    confirmRebirth = false
                    vm.rebirth()
                }) {
                    Text("RENACER", color = Ruby, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRebirth = false }) {
                    Text("Cancelar", color = Gold)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("¿Renacer?", fontWeight = FontWeight.Black)
                }
            },
            text = {
                Column {
                    Text(
                        "Vas a perder:",
                        color = Paper, fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("• Toda la caja y empleados", color = Ruby, fontSize = 13.sp)
                    Text("• Todos los edificios e inventario", color = Ruby, fontSize = 13.sp)
                    Text("• Inmuebles y acciones", color = Ruby, fontSize = 13.sp)
                    Text("• Investigaciones completadas", color = Ruby, fontSize = 13.sp)
                    Text("• Misiones reclamadas", color = Ruby, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Conservas:",
                        color = Paper, fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("• Logros y progreso", color = Emerald, fontSize = 13.sp)
                    Text("• Nivel de prestigio: ${state.prestige.prestigeLevel} → ${state.prestige.prestigeLevel + 1}",
                        color = Emerald, fontSize = 13.sp)
                    Text("• Puntos de prestigio actuales y los nuevos (+$pointsIfRebirth)",
                        color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("• Perks ya comprados", color = Emerald, fontSize = 13.sp)
                }
            },
            containerColor = InkSoft
        )
    }
}

@Composable
private fun PerkCategoryChips(
    selected: String?,
    onSelect: (String?) -> Unit
) {
    val cats = PrestigePerkCategory.values().toList()
    LazyRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            ChipPill(
                label = "Todos",
                selected = selected == null,
                onClick = { onSelect(null) },
                tint = Gold
            )
        }
        items(cats, key = { it.name }) { c ->
            ChipPill(
                label = "${c.emoji} ${c.displayName}",
                selected = selected == c.name,
                onClick = { onSelect(c.name) },
                tint = when (c) {
                    PrestigePerkCategory.PRODUCTION -> Sapphire
                    PrestigePerkCategory.ECONOMY -> Gold
                    PrestigePerkCategory.PERSONNEL -> Emerald
                    PrestigePerkCategory.LUCK -> Ruby
                    PrestigePerkCategory.META -> Gold
                }
            )
        }
    }
}

@Composable
private fun ChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tint: Color
) {
    val bg = if (selected) tint.copy(alpha = 0.18f) else InkSoft
    val border = if (selected) tint else InkBorder
    Surface(
        onClick = onClick,
        color = bg,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, border)
    ) {
        Text(
            label,
            color = if (selected) tint else Dim,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun PerkCard(
    perk: PrestigePerk,
    state: GameState,
    onBuy: () -> Unit
) {
    val owned = state.prestige.owns(perk.id)
    val canAfford = state.prestige.prestigePoints >= perk.cost
    val borderColor = if (owned) Emerald else InkBorder

    EmpireCard(borderColor = borderColor) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (owned) Emerald.copy(alpha = 0.18f)
                        else InkBorder.copy(alpha = 0.4f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(perk.emoji, fontSize = 24.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        perk.name,
                        fontWeight = FontWeight.Bold,
                        color = if (owned) Emerald else Paper
                    )
                    if (owned) {
                        Spacer(Modifier.width(6.dp))
                        Text("✅", fontSize = 12.sp)
                    }
                }
                Text(
                    perk.description,
                    color = Dim, fontSize = 12.sp
                )
                Text(
                    perk.category.displayName,
                    color = Dim, fontSize = 10.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✨", fontSize = 14.sp)
                    Spacer(Modifier.width(2.dp))
                    Text(
                        "${perk.cost}",
                        fontWeight = FontWeight.Bold,
                        color = if (canAfford || owned) Gold else Ruby,
                        fontSize = 16.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onBuy,
                    enabled = !owned && canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold, contentColor = Ink,
                        disabledContainerColor = InkBorder,
                        disabledContentColor = Dim
                    )
                ) {
                    Text(if (owned) "Activo" else "Comprar")
                }
            }
        }
    }
}
