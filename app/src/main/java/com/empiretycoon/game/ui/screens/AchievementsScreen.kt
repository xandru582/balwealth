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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.engine.AchievementEngine
import com.empiretycoon.game.model.*
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.ProgressBarWithLabel
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.*
import com.empiretycoon.game.util.fmtMoney

@Composable
fun AchievementsScreen(state: GameState, vm: GameViewModel) {
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val all = AchievementCatalog.all
    val unlocked = state.achievements.unlocked
    val claimed = state.achievements.claimedAchievements

    val filtered = remember(filter, state.achievements) {
        if (filter == null) all
        else all.filter { it.category.name == filter }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            EmpireCard {
                SectionTitle(
                    "Logros",
                    subtitle = "Hitos del imperio. Reclama recompensas al desbloquear."
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${unlocked.size} / ${all.size} desbloqueados",
                            color = Gold, fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "${claimed.size} reclamados",
                            color = Dim, fontSize = 12.sp
                        )
                    }
                    val pct = unlocked.size.toFloat() / all.size.coerceAtLeast(1).toFloat()
                    Text(
                        "${(pct * 100).toInt()}%",
                        color = Emerald,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                ProgressBarWithLabel(
                    progress = unlocked.size.toFloat() / all.size.coerceAtLeast(1).toFloat(),
                    color = Emerald
                )
            }
        }

        item {
            CategoryChips(
                selected = filter,
                onSelect = { filter = it }
            )
        }

        items(filtered, key = { it.id }) { ach ->
            AchievementCard(
                ach = ach,
                state = state,
                onClaim = { vm.claimAchievement(ach.id) }
            )
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun CategoryChips(
    selected: String?,
    onSelect: (String?) -> Unit
) {
    val cats = AchievementCategory.values().toList()
    LazyRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            FilterChipPill(
                label = "Todos",
                selected = selected == null,
                onClick = { onSelect(null) },
                tint = Gold
            )
        }
        items(cats, key = { it.name }) { c ->
            FilterChipPill(
                label = "${c.emoji} ${c.displayName}",
                selected = selected == c.name,
                onClick = { onSelect(c.name) },
                tint = when (c) {
                    AchievementCategory.WEALTH -> Gold
                    AchievementCategory.PRODUCTION -> Sapphire
                    AchievementCategory.EMPIRE -> Emerald
                    AchievementCategory.CHARACTER -> Ruby
                    AchievementCategory.MARKET -> Sapphire
                    AchievementCategory.REAL_ESTATE -> Emerald
                    AchievementCategory.RESEARCH -> Sapphire
                    AchievementCategory.SOCIAL -> Gold
                    AchievementCategory.MILESTONE -> Gold
                    AchievementCategory.SECRET -> Ruby
                }
            )
        }
    }
}

@Composable
private fun FilterChipPill(
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
private fun AchievementCard(
    ach: Achievement,
    state: GameState,
    onClaim: () -> Unit
) {
    val isUnlocked = state.achievements.isUnlocked(ach.id)
    val isClaimed = state.achievements.isClaimed(ach.id)
    val rawProg = AchievementEngine.progressFor(ach, state)
    val storedProg = state.achievements.progressOf(ach.id)
    val progress = maxOf(rawProg, storedProg)
    val progressFraction = (progress.toFloat() / ach.threshold.coerceAtLeast(1).toFloat())
        .coerceIn(0f, 1f)

    val shouldHide = ach.hidden && !isUnlocked
    val title = if (shouldHide) "???" else ach.title
    val description = if (shouldHide) "Logro secreto. Sigue jugando para descubrirlo."
        else ach.description
    val emoji = if (shouldHide) "❓" else ach.emoji

    val borderColor = when {
        isClaimed -> Emerald
        isUnlocked -> Gold
        else -> InkBorder
    }
    val titleColor = when {
        isClaimed -> Emerald
        isUnlocked -> Gold
        shouldHide -> Dim
        else -> Paper
    }
    val emojiAlpha = if (isUnlocked) 1f else 0.45f

    EmpireCard(borderColor = borderColor) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isUnlocked) borderColor.copy(alpha = 0.18f) else InkBorder.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    emoji,
                    fontSize = 24.sp,
                    modifier = Modifier.alpha(emojiAlpha)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )
                    if (isClaimed) {
                        Spacer(Modifier.width(6.dp))
                        Text("✅", fontSize = 12.sp)
                    }
                }
                Text(
                    description,
                    color = Dim,
                    fontSize = 12.sp
                )
                if (!shouldHide) {
                    Spacer(Modifier.height(6.dp))
                    val cap = progress.coerceAtMost(ach.threshold)
                    Text(
                        "$cap / ${ach.threshold} · ${ach.category.displayName}",
                        color = Dim, fontSize = 10.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    ProgressBarWithLabel(
                        progressFraction,
                        color = if (isUnlocked) Gold else Sapphire
                    )
                }
            }
        }

        if (!shouldHide) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (ach.rewardCash > 0) {
                    RewardChip("💰", ach.rewardCash.fmtMoney(), Gold)
                    Spacer(Modifier.width(4.dp))
                }
                if (ach.rewardXp > 0) {
                    RewardChip("⭐", "${ach.rewardXp} XP", Emerald)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onClaim,
                    enabled = isUnlocked && !isClaimed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold, contentColor = Ink,
                        disabledContainerColor = InkBorder,
                        disabledContentColor = Dim
                    )
                ) {
                    Text(when {
                        isClaimed -> "Reclamado"
                        isUnlocked -> "Reclamar"
                        else -> "Bloqueado"
                    })
                }
            }
        }
    }
}

@Composable
private fun RewardChip(emoji: String, text: String, tint: Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(tint.copy(alpha = 0.15f))
            .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 12.sp)
        Spacer(Modifier.width(4.dp))
        Text(text, color = tint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
