package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.model.GameState
import com.empiretycoon.game.model.Skill
import com.empiretycoon.game.model.SkillBranch
import com.empiretycoon.game.model.SkillTreeCatalog
import com.empiretycoon.game.model.SkillTreeState
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.components.StatPill
import com.empiretycoon.game.ui.theme.Dim
import com.empiretycoon.game.ui.theme.Emerald
import com.empiretycoon.game.ui.theme.Gold
import com.empiretycoon.game.ui.theme.Ink
import com.empiretycoon.game.ui.theme.InkBorder
import com.empiretycoon.game.ui.theme.InkSoft
import com.empiretycoon.game.ui.theme.Paper

@Composable
fun SkillTreeScreen(state: GameState, vm: GameViewModel) {
    var selectedBranch by remember { mutableStateOf(SkillBranch.LEADERSHIP) }
    val tree = state.skillTree

    Column(
        Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        SectionTitle(
            "Árbol de habilidades",
            "Gana 1 punto por nivel (+1 cada 5). Total ganados: ${tree.totalEarnedPoints}."
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatPill(
                label = "Puntos disp.",
                value = "${tree.availablePoints}",
                emoji = "⚡"
            )
            Spacer(Modifier.width(8.dp))
            StatPill(
                label = "Desbloqueadas",
                value = "${tree.unlockedSkills.size}/${SkillTreeCatalog.all.size}",
                emoji = "📜"
            )
        }
        Spacer(Modifier.height(10.dp))

        // pestañas de ramas
        ScrollableTabRow(
            selectedTabIndex = SkillBranch.values().indexOf(selectedBranch),
            containerColor = InkSoft,
            edgePadding = 0.dp
        ) {
            SkillBranch.values().forEachIndexed { idx, b ->
                Tab(
                    selected = b == selectedBranch,
                    onClick = { selectedBranch = b },
                    selectedContentColor = Gold,
                    unselectedContentColor = Dim,
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(b.emoji)
                            Spacer(Modifier.width(4.dp))
                            Text(b.displayName, fontSize = 12.sp)
                        }
                    }
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        Text(
            selectedBranch.tagline,
            color = Dim,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(6.dp))

        BranchTree(branch = selectedBranch, tree = tree, onUnlock = vm::unlockSkill)
    }
}

@Composable
private fun BranchTree(
    branch: SkillBranch,
    tree: SkillTreeState,
    onUnlock: (String) -> Unit
) {
    val byTier = (1..4).associateWith { SkillTreeCatalog.byTier(branch, it) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        for (tier in 1..4) {
            val skills = byTier[tier].orEmpty()
            if (skills.isEmpty()) continue
            TierHeader(tier = tier)
            // pintamos los nodos del tier en filas de 2
            val rows = skills.chunked(2)
            for (row in rows) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    for (s in row) {
                        SkillNode(
                            skill = s,
                            tree = tree,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            onUnlock = onUnlock
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (tier < 4) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .width(2.dp)
                            .height(18.dp)
                            .background(InkBorder)
                    )
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun TierHeader(tier: Int) {
    val title = when (tier) {
        1 -> "Tier 1 — Aprendiz"
        2 -> "Tier 2 — Veterano"
        3 -> "Tier 3 — Experto"
        4 -> "Tier 4 — Maestro"
        else -> "Tier $tier"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = Gold,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(InkBorder)
        )
    }
}

@Composable
private fun SkillNode(
    skill: Skill,
    tree: SkillTreeState,
    modifier: Modifier = Modifier,
    onUnlock: (String) -> Unit
) {
    val owned = tree.has(skill.id)
    val unlockable = tree.canUnlock(skill)
    val borderColor = when {
        owned -> Emerald
        unlockable -> Gold
        else -> InkBorder
    }
    val containerColor = when {
        owned -> InkSoft
        unlockable -> InkSoft
        else -> Ink
    }
    val textColor = if (owned || unlockable) Paper else Dim
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = unlockable) { onUnlock(skill.id) }
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(skill.emoji, fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    skill.name,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    "Tier ${skill.tier} · coste ${skill.cost}",
                    color = Dim,
                    fontSize = 11.sp
                )
            }
            StatusBadge(owned = owned, unlockable = unlockable)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            skill.description,
            color = if (owned) Paper else Dim,
            fontSize = 12.sp
        )
        if (skill.prerequisites.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            val prereqsNames = skill.prerequisites.mapNotNull { id ->
                SkillTreeCatalog.byId[id]?.name
            }.joinToString(", ")
            Text(
                "Requiere: $prereqsNames",
                color = Dim,
                fontSize = 10.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Composable
private fun StatusBadge(owned: Boolean, unlockable: Boolean) {
    val (label, color) = when {
        owned -> "✔" to Emerald
        unlockable -> "★" to Gold
        else -> "🔒" to Dim
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(InkBorder)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

