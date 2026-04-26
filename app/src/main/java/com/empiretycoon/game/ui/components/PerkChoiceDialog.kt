package com.empiretycoon.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.model.Perk
import com.empiretycoon.game.model.PerkCatalog
import com.empiretycoon.game.model.Rarity
import com.empiretycoon.game.ui.theme.Dim
import com.empiretycoon.game.ui.theme.Emerald
import com.empiretycoon.game.ui.theme.Gold
import com.empiretycoon.game.ui.theme.Ink
import com.empiretycoon.game.ui.theme.InkBorder
import com.empiretycoon.game.ui.theme.InkSoft
import com.empiretycoon.game.ui.theme.Paper
import com.empiretycoon.game.ui.theme.Ruby
import com.empiretycoon.game.ui.theme.Sapphire

/**
 * Diálogo modal mostrado cuando hay perks pendientes para elegir.
 *
 * Recibe los IDs de los 3 perks ofrecidos y un callback de selección.
 */
@Composable
fun PerkChoiceDialog(
    perkIds: List<String>,
    onPick: (String) -> Unit
) {
    val perks = perkIds.mapNotNull { PerkCatalog.byId[it] }
    AlertDialog(
        onDismissRequest = { /* obligatorio elegir */ },
        confirmButton = { },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎁", fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Text("¡Elige un perk!", color = Paper, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    "Has subido un nivel importante. Estas tres cartas se han " +
                        "barajado para ti. Elige una con cabeza.",
                    color = Dim,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                perks.forEach { perk ->
                    PerkCard(perk = perk, onClick = { onPick(perk.id) })
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        containerColor = InkSoft
    )
}

@Composable
private fun PerkCard(perk: Perk, onClick: () -> Unit) {
    val border = rarityColor(perk.rarity)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Ink)
            .border(2.dp, border, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(InkBorder),
                contentAlignment = Alignment.Center
            ) {
                Text(perk.emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    perk.name,
                    color = Paper,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(perk.rarity.emoji, fontSize = 12.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        perk.rarity.displayName,
                        color = border,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            perk.description,
            color = Dim,
            fontSize = 12.sp
        )
    }
}

private fun rarityColor(rarity: Rarity): Color = when (rarity) {
    Rarity.COMMON -> Dim
    Rarity.UNCOMMON -> Emerald
    Rarity.RARE -> Sapphire
    Rarity.EPIC -> Ruby
    Rarity.LEGENDARY -> Gold
}
