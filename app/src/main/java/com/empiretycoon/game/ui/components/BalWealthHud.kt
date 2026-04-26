package com.empiretycoon.game.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.model.BalTier
import com.empiretycoon.game.model.BalWealthState
import com.empiretycoon.game.ui.theme.*

/**
 * HUD único de BalWealth: rosco de equilibrio entre Wealth/Employees/
 * Community/Mind. Tap para ver detalle.
 */
@Composable
fun BalWealthHud(
    state: BalWealthState,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {}
) {
    val tier = runCatching { BalTier.valueOf(state.tier) }.getOrDefault(BalTier.GROWING)
    val tierColor = when (tier) {
        BalTier.HARMONIC -> Emerald
        BalTier.GROWING -> Sapphire
        BalTier.STRUGGLING -> Gold
        BalTier.TYRANT_BUBBLE -> Ruby
        BalTier.MARTYR -> Sapphire
        BalTier.BURNED_OUT -> Color(0xFF8E24AA)
    }

    val animIndex by animateFloatAsState(state.index, label = "balIdx")

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(InkSoft)
            .border(1.dp, tierColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .clickable { onTap() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rosco de equilibrio
        Canvas(Modifier.size(46.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension / 2f - 4f
            // Fondo
            drawCircle(
                color = InkBorder,
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 6f)
            )
            // 4 cuartos según los 4 ejes
            val scores = listOf(
                state.wealthScore to Gold,
                state.employeeScore to Emerald,
                state.communityScore to Sapphire,
                state.mindScore to Color(0xFFCE93D8)
            )
            val sweep = 360f / scores.size
            scores.forEachIndexed { i, (score, color) ->
                val frac = (score / 100f).coerceIn(0f, 1f)
                drawArc(
                    brush = Brush.linearGradient(listOf(color, color.copy(alpha = 0.4f))),
                    startAngle = -90f + i * sweep,
                    sweepAngle = sweep * frac,
                    useCenter = false,
                    topLeft = Offset(cx - r, cy - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(width = 6f, cap = StrokeCap.Butt)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                "${animIndex.toInt()}",
                color = tierColor,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
            Text(
                "${tier.emoji} ${tier.displayName}",
                color = Dim,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Pantalla detallada del BalWealth Index — explica cada eje. */
@Composable
fun BalWealthDetail(state: BalWealthState) {
    val tier = runCatching { BalTier.valueOf(state.tier) }.getOrDefault(BalTier.GROWING)
    Column(Modifier.padding(16.dp)) {
        Text(
            "Índice BalWealth",
            color = Gold,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp
        )
        Text(
            "El equilibrio entre tu riqueza, equipo, comunidad y mente.",
            color = Dim,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "${state.index.toInt()} / 100  ${tier.emoji}",
            color = Paper,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )
        Text(
            tier.description,
            color = Dim,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(16.dp))
        AxisBar("Riqueza (W)", state.wealthScore, Gold)
        AxisBar("Equipo (E)", state.employeeScore, Emerald)
        AxisBar("Comunidad (C)", state.communityScore, Sapphire)
        AxisBar("Mente (M)", state.mindScore, Color(0xFFCE93D8))
        Spacer(Modifier.height(12.dp))
        Text(
            "El índice penaliza el desequilibrio. Ser rico a costa de los demás te hunde. Mantén los 4 cuadrantes altos para alcanzar el estado Armónico (≥80) y desbloquear bonificaciones únicas.",
            color = Dim,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Mejor histórico: ${state.highestEverIndex.toInt()}",
            color = Gold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AxisBar(label: String, value: Float, color: Color) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row {
            Text(label, color = Paper, modifier = Modifier.weight(1f), fontSize = 12.sp)
            Text("${value.toInt()}", color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(InkBorder)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(value / 100f)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(listOf(color.copy(alpha = 0.6f), color))
                    )
            )
        }
    }
}
