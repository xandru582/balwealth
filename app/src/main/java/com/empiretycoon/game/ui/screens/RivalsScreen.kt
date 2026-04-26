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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.engine.RivalEngine
import com.empiretycoon.game.model.GameState
import com.empiretycoon.game.model.Rival
import com.empiretycoon.game.ui.components.EmpireCard
import com.empiretycoon.game.ui.components.ProgressBarWithLabel
import com.empiretycoon.game.ui.components.SectionTitle
import com.empiretycoon.game.ui.theme.Dim
import com.empiretycoon.game.ui.theme.Emerald
import com.empiretycoon.game.ui.theme.Gold
import com.empiretycoon.game.ui.theme.Ink
import com.empiretycoon.game.ui.theme.InkBorder
import com.empiretycoon.game.ui.theme.InkSoft
import com.empiretycoon.game.ui.theme.Paper
import com.empiretycoon.game.ui.theme.Ruby
import com.empiretycoon.game.ui.theme.Sapphire
import com.empiretycoon.game.util.fmtMoney

@Composable
fun RivalsScreen(state: GameState, vm: GameViewModel) {
    val active = state.rivals.active.filter { !it.defeated }
    val defeated = state.rivals.defeated

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        item {
            SectionTitle(
                "Rivales",
                "Supera el cash de tu rival actual para vencerlo. Recibirás dinero, XP y reputación."
            )
            Spacer(Modifier.height(8.dp))
        }
        // pulla actual
        state.rivals.lastTrashTalk?.let { line ->
            item {
                EmpireCard(borderColor = Ruby) {
                    Text("Pulla recibida", color = Ruby, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(line, color = Paper, fontSize = 13.sp, fontStyle = FontStyle.Italic)
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { vm.dismissTrashTalk() }) {
                        Text("Tragar saliva", color = Dim, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Activos", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Gold)
                TextButton(onClick = { vm.requestTrashTalk() }) {
                    Text("¡A ver qué dicen!", color = Sapphire, fontSize = 11.sp)
                }
            }
        }
        items(active, key = { it.id }) { rival ->
            RivalCard(rival = rival, state = state, defeated = false)
            Spacer(Modifier.height(8.dp))
        }
        if (defeated.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("Derrotados", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Emerald)
                Spacer(Modifier.height(4.dp))
            }
            items(defeated, key = { it.id }) { rival ->
                RivalCard(rival = rival, state = state, defeated = true)
                Spacer(Modifier.height(8.dp))
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun RivalCard(rival: Rival, state: GameState, defeated: Boolean) {
    val isCurrent = state.rivals.currentChallenge == rival.id && !defeated
    val border = when {
        defeated -> Emerald
        isCurrent -> Gold
        else -> InkBorder
    }
    EmpireCard(borderColor = border) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(InkBorder)
                    .border(1.dp, border, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(rival.portrait, fontSize = 28.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        rival.name,
                        color = Paper,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    if (defeated) BadgeText("DERROTADO", Emerald)
                    else if (isCurrent) BadgeText("OBJETIVO", Gold)
                }
                Text(
                    "${rival.archetype.emoji} ${rival.archetype.displayName}",
                    color = Sapphire,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Lv ${rival.level} · Rep ${rival.reputation}",
                    color = Dim,
                    fontSize = 11.sp
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "\"${rival.taunt}\"",
            color = Dim,
            fontSize = 12.sp,
            fontStyle = FontStyle.Italic
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            rival.traits.take(3).forEach { trait ->
                TraitChip(trait)
                Spacer(Modifier.width(4.dp))
            }
        }
        if (!defeated) {
            Spacer(Modifier.height(8.dp))
            val progress = RivalEngine.progressFor(state, rival)
            ProgressBarWithLabel(
                progress = progress,
                label = "Cash: ${state.company.cash.fmtMoney()} / ${rival.cash.fmtMoney()} " +
                        "(${(progress * 100).toInt()}%)",
                color = if (isCurrent) Gold else Sapphire
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Recompensa: +${rival.rewardCash.fmtMoney()} · +${rival.rewardXp} XP · +${rival.rewardReputation} rep",
                color = Emerald,
                fontSize = 11.sp
            )
        } else {
            Spacer(Modifier.height(6.dp))
            Text(
                "Recompensa cobrada. Otro a la lista.",
                color = Emerald,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BadgeText(text: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(InkBorder)
            .border(1.dp, color, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TraitChip(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Ink)
            .border(1.dp, InkBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = Dim, fontSize = 10.sp)
    }
}
