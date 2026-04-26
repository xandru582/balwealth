package com.empiretycoon.game.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.empiretycoon.game.ui.theme.Gold

/**
 * Pulso de escala continua. Cuando [active] es true, el componente respira
 * suavemente. Cuando es false vuelve a 1f. Útil para llamar la atención
 * sobre botones de "cobrar", "subir nivel", etc.
 *
 * El parámetro [color] se ignora actualmente (sólo se anima la escala) pero
 * mantiene la firma estable para futuras variantes con halo coloreado.
 */
@Suppress("UNUSED_PARAMETER")
fun Modifier.pulse(
    active: Boolean = true,
    color: Color = Gold,
    sizeBoost: Float = 0.05f
): Modifier = composed {
    if (!active) return@composed this
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f + sizeBoost,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    this.scale(scale)
}

/**
 * Sacudida horizontal rápida cuando [trigger] cambia de valor. Útil para
 * marcar errores ("dinero insuficiente"). Devuelve a la posición original
 * en ~400 ms.
 */
fun Modifier.shake(trigger: Boolean): Modifier = composed {
    val shakeAmount = remember { Animatable(0f) }
    var lastTrigger by remember { mutableStateOf(trigger) }
    LaunchedEffect(trigger) {
        if (trigger != lastTrigger) {
            lastTrigger = trigger
            val pattern = floatArrayOf(-12f, 12f, -8f, 8f, -4f, 4f, 0f)
            for (v in pattern) {
                shakeAmount.animateTo(v, tween(durationMillis = 55))
            }
        }
    }
    this.offset(x = shakeAmount.value.dp)
}

/**
 * Brillo que recorre el contenido en diagonal. No se reposiciona si el
 * componente cambia de tamaño porque depende del tiempo de animación.
 */
fun Modifier.shimmer(
    color: Color = Gold
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerPhase"
    )
    this.drawWithContent {
        drawContent()
        val w = size.width
        val h = size.height
        val sweep = w * 1.5f
        val x = -sweep + (w + sweep) * phase
        val brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                color.copy(alpha = 0.0f),
                color.copy(alpha = 0.35f),
                color.copy(alpha = 0.0f),
                Color.Transparent
            ),
            start = Offset(x, 0f),
            end = Offset(x + sweep / 2f, h)
        )
        drawRect(brush = brush, blendMode = BlendMode.Plus)
    }
}
