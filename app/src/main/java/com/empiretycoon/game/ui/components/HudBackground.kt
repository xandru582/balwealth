package com.empiretycoon.game.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.empiretycoon.game.ui.theme.Gold
import com.empiretycoon.game.ui.theme.Ink
import com.empiretycoon.game.ui.theme.InkSoft
import com.empiretycoon.game.ui.theme.Midnight
import com.empiretycoon.game.ui.theme.Sapphire
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fondo HUD animado: gradiente radial cuyo origen rota lentamente
 * alrededor del centro, con una bruma sutil de partículas estáticas.
 *
 * Coste: una sola animación infinita en CPU + un Canvas con ~12 círculos.
 */
@Composable
fun HudBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "hudBg")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hudAngle"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6_500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hudPulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f + cos(angle.toDouble()).toFloat() * size.width * 0.18f
            val cy = size.height / 2f + sin(angle.toDouble()).toFloat() * size.height * 0.18f
            val radius = size.maxDimension * 0.95f * pulse
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Midnight.copy(alpha = 0.55f),
                        InkSoft.copy(alpha = 0.85f),
                        Ink
                    ),
                    center = Offset(cx, cy),
                    radius = radius
                ),
                size = size
            )
            // Toques azulados/dorados que rotan en sentido contrario
            val cx2 = size.width / 2f + cos(-angle.toDouble() * 0.5).toFloat() * size.width * 0.30f
            val cy2 = size.height / 2f + sin(-angle.toDouble() * 0.5).toFloat() * size.height * 0.30f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Sapphire.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(cx2, cy2),
                    radius = size.minDimension * 0.45f
                ),
                radius = size.minDimension * 0.45f,
                center = Offset(cx2, cy2)
            )
            val cx3 = size.width / 2f + cos(angle.toDouble() * 0.7 + 1.3).toFloat() * size.width * 0.25f
            val cy3 = size.height / 2f + sin(angle.toDouble() * 0.7 + 1.3).toFloat() * size.height * 0.25f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Gold.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(cx3, cy3),
                    radius = size.minDimension * 0.35f
                ),
                radius = size.minDimension * 0.35f,
                center = Offset(cx3, cy3)
            )
            // Bruma de partículas (estáticas, sin coste extra de animación)
            val seedPositions = listOf(
                0.12f to 0.18f, 0.27f to 0.66f, 0.41f to 0.31f,
                0.55f to 0.78f, 0.66f to 0.22f, 0.78f to 0.59f,
                0.85f to 0.12f, 0.93f to 0.74f, 0.07f to 0.50f,
                0.34f to 0.91f, 0.62f to 0.43f, 0.49f to 0.10f
            )
            seedPositions.forEachIndexed { i, (fx, fy) ->
                val ax = fx * size.width
                val ay = fy * size.height
                val flicker = (0.6f + 0.4f * sin((angle + i.toFloat()).toDouble()).toFloat())
                drawCircle(
                    color = Gold.copy(alpha = 0.06f * flicker),
                    radius = (1.5f + (i % 3) * 0.8f),
                    center = Offset(ax, ay)
                )
            }
        }
        content()
    }
}
