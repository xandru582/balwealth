package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.engine.TraitTreeEngine
import com.empiretycoon.game.model.*
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*

/**
 * Pantalla del árbol de talentos. 5 ramas en pestañas, cada una con 12
 * traits encadenados linealmente. Coste en Resilience XP (lo emite
 * DisasterEngine al superar desastres).
 */
@Composable
fun TraitTreeScreen(state: GameState, vm: GameViewModel) {
    val tt = state.traitTree
    val resilienceXp = state.disasters.resilienceXp
    var selectedBranch by remember { mutableStateOf(TraitBranch.MAGNATE) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SectionTitle(
            "🧬 Árbol de talentos",
            subtitle = "60 traits en 5 ramas. Permanentes. Coste en Resilience XP de desastres."
        )
        Spacer(Modifier.height(12.dp))

        // Header
        EmpireCard(borderColor = Gold) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Resilience XP disponible", color = Dim, fontSize = 11.sp)
                    Text("$resilienceXp", color = Gold, fontSize = 22.sp,
                        fontWeight = FontWeight.Black)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Traits desbloqueados", color = Dim, fontSize = 11.sp)
                    Text("${tt.unlockedIds.size} / 60", color = Sapphire, fontSize = 18.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Tabs por rama
        Row(modifier = Modifier.fillMaxWidth()) {
            for (branch in TraitBranch.values()) {
                val unlockedInBranch = tt.unlockedInBranch(branch)
                val isSelected = selectedBranch == branch
                Box(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) Color(branch.accentArgb).copy(alpha = 0.85f)
                            else InkBorder
                        )
                        .clickable { selectedBranch = branch }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(branch.emoji, fontSize = 22.sp)
                        Text("$unlockedInBranch/12",
                            color = if (isSelected) Color.Black else Dim,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Header de la rama activa
        val branch = selectedBranch
        EmpireCard(borderColor = Color(branch.accentArgb)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(branch.emoji, fontSize = 36.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(branch.displayName,
                        fontWeight = FontWeight.Black, color = Color(branch.accentArgb),
                        fontSize = 20.sp)
                    Text(branch.tagline, color = Dim, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // Traits 1..12 de la rama
        val traits = TraitCatalog.byBranch(branch)
        for (trait in traits) {
            TraitRow(
                trait = trait,
                state = state,
                onUnlock = { vm.traitTreeUnlock(trait.id) }
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun TraitRow(
    trait: TraitDefinition,
    state: GameState,
    onUnlock: () -> Unit
) {
    val tt = state.traitTree
    val isUnlocked = tt.isUnlocked(trait.id)
    val prev = TraitCatalog.previousOf(trait)
    val prevUnlocked = prev == null || tt.isUnlocked(prev)
    val canAfford = state.disasters.resilienceXp >= trait.cost
    val unlockable = !isUnlocked && prevUnlocked && canAfford

    val border = when {
        isUnlocked -> Color(trait.branch.accentArgb)
        unlockable -> Gold
        else -> InkBorder
    }
    EmpireCard(borderColor = border) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(InkBorder),
                contentAlignment = Alignment.Center
            ) {
                Text("${trait.tier}", color = Color(trait.branch.accentArgb),
                    fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(trait.displayName,
                    color = if (isUnlocked) Color(trait.branch.accentArgb) else Paper,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(trait.description, color = Dim, fontSize = 11.sp)
                val statusLabel = when {
                    isUnlocked -> "✅ Desbloqueado"
                    !prevUnlocked -> "🔒 Falta tier ${trait.tier - 1}"
                    !canAfford -> "💸 Necesitas ${trait.cost} XP"
                    else -> "Disponible — coste ${trait.cost} XP"
                }
                Text(statusLabel,
                    color = when {
                        isUnlocked -> Emerald
                        unlockable -> Gold
                        else -> Color(0xFFFF7A7A)
                    },
                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start)
            }
            if (!isUnlocked && prevUnlocked) {
                Button(
                    onClick = onUnlock,
                    enabled = unlockable,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) {
                    Text("Comprar", fontSize = 11.sp,
                        color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
