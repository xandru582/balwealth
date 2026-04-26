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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.model.QualityTier
import com.empiretycoon.game.ui.theme.*

/**
 * Color asociado a cada tier para el chip. Mantiene la paleta del juego.
 */
fun colorForTier(t: QualityTier): Color = when (t) {
    QualityTier.POOR -> Dim
    QualityTier.STANDARD -> Sapphire
    QualityTier.GOOD -> Emerald
    QualityTier.PREMIUM -> Gold
    QualityTier.ULTRA -> Color(0xFF9D4EDD)
    QualityTier.MASTERWORK -> Ruby
}

/**
 * Badge compacto con emoji + etiqueta del tier.
 * Por defecto muestra texto: úsalo en cards y listas.
 */
@Composable
fun QualityBadge(
    tier: QualityTier,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    compact: Boolean = false
) {
    val tint = colorForTier(tier)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(if (compact) 6.dp else 8.dp))
            .background(tint.copy(alpha = 0.18f))
            .border(1.dp, tint.copy(alpha = 0.55f),
                RoundedCornerShape(if (compact) 6.dp else 8.dp))
            .padding(
                horizontal = if (compact) 5.dp else 8.dp,
                vertical = if (compact) 2.dp else 4.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(tier.emoji, fontSize = if (compact) 11.sp else 13.sp)
        if (showLabel) {
            Spacer(Modifier.width(4.dp))
            Text(
                tier.label,
                color = tint,
                fontWeight = FontWeight.SemiBold,
                style = if (compact) MaterialTheme.typography.labelSmall
                else MaterialTheme.typography.labelMedium
            )
        }
    }
}

/**
 * Variante compacta para inventario / chips inline.
 * Muestra emoji + cantidad + etiqueta.
 */
@Composable
fun QualityCountBadge(
    tier: QualityTier,
    qty: Int,
    modifier: Modifier = Modifier
) {
    val tint = colorForTier(tier)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(tier.emoji, fontSize = 12.sp)
        Spacer(Modifier.width(4.dp))
        Text(
            tier.label,
            color = tint,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "x$qty",
            color = Paper,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}
