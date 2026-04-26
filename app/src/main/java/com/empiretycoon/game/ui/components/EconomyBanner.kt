package com.empiretycoon.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.model.EconomicPhase
import com.empiretycoon.game.model.EconomicState
import com.empiretycoon.game.ui.theme.*

/** Color de acento por fase económica. */
fun EconomicPhase.accentColor(): Color = when (this) {
    EconomicPhase.BOOM -> Emerald
    EconomicPhase.RECOVERY -> Sapphire
    EconomicPhase.NORMAL -> Gold
    EconomicPhase.RECESSION -> Color(0xFFFFB05A)
    EconomicPhase.DEPRESSION -> Ruby
    EconomicPhase.BUBBLE -> Color(0xFFE158D2)
    EconomicPhase.CRASH -> Color(0xFFFF3B30)
}

/**
 * Banner reutilizable que resume la fase económica actual. Pensado para la
 * cabecera del Home, Mercado o pantallas con contexto macro.
 */
@Composable
fun EconomyBanner(
    econ: EconomicState,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val phase = econ.currentPhase
    val accent = phase.accentColor()
    val bgGradient = Brush.horizontalGradient(
        listOf(
            accent.copy(alpha = 0.18f),
            InkSoft
        )
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgGradient)
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = if (compact) 8.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(phase.emoji, fontSize = if (compact) 22.sp else 26.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    phase.displayName.uppercase(),
                    color = accent,
                    fontWeight = FontWeight.Black,
                    fontSize = if (compact) 13.sp else 15.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    econ.phaseTrendIndicator,
                    fontSize = 12.sp
                )
            }
            if (!compact) {
                Text(
                    phase.description,
                    color = Dim,
                    fontSize = 11.sp,
                    maxLines = 2
                )
            }
        }
        if (!compact) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "PIB ${"%.1f".format(econ.gdpIndex)}",
                    color = Paper,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Inf ${"%.2f".format(econ.inflation * 100)}%",
                    color = Dim,
                    fontSize = 11.sp
                )
                Text(
                    "Día ${econ.daysInPhase}",
                    color = Dim,
                    fontSize = 10.sp
                )
            }
        } else {
            Text(
                "PIB ${"%.0f".format(econ.gdpIndex)}",
                color = Paper,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Variante mini para barras de info: sólo emoji + nombre. */
@Composable
fun EconomyChip(econ: EconomicState, modifier: Modifier = Modifier) {
    val phase = econ.currentPhase
    val accent = phase.accentColor()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(InkSoft)
            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(phase.emoji)
        Spacer(Modifier.width(4.dp))
        Text(
            phase.displayName,
            color = accent,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
