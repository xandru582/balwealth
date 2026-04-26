package com.empiretycoon.game.ui.components

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import com.empiretycoon.game.ui.theme.Emerald
import com.empiretycoon.game.ui.theme.Gold
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class CoinParticle(
    val id: Long,
    val startX: Float,
    val drift: Float,
    val emoji: String,
    val rotation: Float,
    val size: Int
)

/**
 * Lluvia de monedas. Cuando [trigger] cambia a true, dispara una tanda de
 * partículas que caen con gravedad y desaparecen. Mantiene el [content]
 * visible debajo siempre (los hijos se renderizan dentro del Box).
 *
 * Limita las partículas activas a 30 a la vez (rendimiento).
 */
@Composable
fun MoneyRain(
    trigger: Boolean,
    durationMs: Int = 1500,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val particles = remember { mutableStateListOf<CoinParticle>() }
    var lastTrigger by remember { mutableStateOf(trigger) }

    LaunchedEffect(trigger) {
        if (trigger != lastTrigger && trigger) {
            // Spawnea entre 14 y 22 partículas
            val count = Random.nextInt(14, 23)
            val nowId = System.currentTimeMillis()
            val emojis = listOf("💰", "💵", "🪙", "✨")
            repeat(count) { i ->
                if (particles.size >= 30) return@repeat
                particles.add(
                    CoinParticle(
                        id = nowId + i,
                        startX = Random.nextFloat(),
                        drift = (Random.nextFloat() - 0.5f) * 0.3f,
                        emoji = emojis.random(),
                        rotation = (Random.nextFloat() - 0.5f) * 90f,
                        size = Random.nextInt(20, 36)
                    )
                )
            }
        }
        lastTrigger = trigger
    }

    Box(modifier = modifier.fillMaxSize()) {
        content()
        particles.toList().forEach { p ->
            FallingCoin(p, durationMs) { particles.remove(p) }
        }
    }
}

@Composable
private fun FallingCoin(
    p: CoinParticle,
    durationMs: Int,
    onEnd: () -> Unit
) {
    val progress = remember(p.id) { Animatable(0f) }
    val alpha = remember(p.id) { Animatable(1f) }

    LaunchedEffect(p.id) {
        coroutineScope {
            launch {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = durationMs,
                        easing = LinearOutSlowInEasing
                    )
                )
            }
            launch {
                delay((durationMs * 0.55).toLong())
                alpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = (durationMs * 0.45).toInt())
                )
            }
        }
        onEnd()
    }

    Box(
        Modifier
            .fillMaxSize()
            .alpha(alpha.value)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val w = constraints.maxWidth
                val h = constraints.maxHeight
                val xBase = (p.startX * w).toInt()
                val xDrift = (p.drift * w * progress.value).toInt()
                val gravity = progress.value * progress.value
                val y = (gravity * h).toInt()
                layout(placeable.width, placeable.height) {
                    placeable.place(xBase + xDrift, y)
                }
            }
    ) {
        Text(
            text = p.emoji,
            fontSize = p.size.sp,
            fontWeight = FontWeight.Black
        )
    }
}

/**
 * Estallido radial verde con chispas en spark. Se reproduce cuando [trigger]
 * cambia a true. Pensado para anclar a un lugar concreto (uso típico:
 * compra exitosa, misión cumplida).
 */
@Composable
fun SuccessBurst(
    trigger: Boolean,
    color: Color = Emerald,
    modifier: Modifier = Modifier
) {
    val ring = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val sparks = remember { mutableStateOf<List<Float>>(emptyList()) }
    var lastTrigger by remember { mutableStateOf(trigger) }

    LaunchedEffect(trigger) {
        if (trigger != lastTrigger && trigger) {
            sparks.value = List(8) { Random.nextFloat() * 360f }
            ring.snapTo(0f)
            alpha.snapTo(1f)
            coroutineScope {
                launch {
                    ring.animateTo(1f, tween(700, easing = LinearOutSlowInEasing))
                }
                launch {
                    delay(280)
                    alpha.animateTo(0f, tween(500))
                }
            }
        }
        lastTrigger = trigger
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (alpha.value <= 0f) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxR = size.minDimension / 2.4f
        val r = maxR * ring.value
        drawCircle(
            color = color.copy(alpha = alpha.value * 0.85f),
            radius = r,
            center = center,
            style = Stroke(width = (4f * (1f - ring.value * 0.6f)))
        )
        // chispas
        sparks.value.forEach { ang ->
            val rad = Math.toRadians(ang.toDouble())
            val sparkR = maxR * 0.85f * ring.value
            val sx = center.x + cos(rad).toFloat() * sparkR
            val sy = center.y + sin(rad).toFloat() * sparkR
            drawCircle(
                color = color.copy(alpha = alpha.value),
                radius = 4f * (1f - ring.value * 0.5f),
                center = Offset(sx, sy)
            )
        }
    }
}

/**
 * Texto flotante que sube y se desvanece en un segundo. Útil para anunciar
 * "+1.000 €" o "+2 XP" sobre algún componente.
 */
@Composable
fun FloatingText(
    text: String,
    color: Color = Gold,
    modifier: Modifier = Modifier
) {
    val offsetY = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    val density = LocalDensity.current

    LaunchedEffect(text) {
        offsetY.snapTo(0f)
        alpha.snapTo(1f)
        coroutineScope {
            launch {
                offsetY.animateTo(
                    targetValue = with(density) { -40.dp.toPx() },
                    animationSpec = tween(1000, easing = LinearOutSlowInEasing)
                )
            }
            launch {
                delay(450)
                alpha.animateTo(0f, tween(550))
            }
        }
    }

    Box(
        modifier = modifier
            .alpha(alpha.value)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(0, offsetY.value.toInt())
                }
            }
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
