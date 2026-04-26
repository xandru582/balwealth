package com.empiretycoon.game.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.ui.components.pulse
import com.empiretycoon.game.ui.components.shimmer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.empiretycoon.game.ui.theme.Dim
import com.empiretycoon.game.ui.theme.Gold
import com.empiretycoon.game.ui.theme.Ink
import com.empiretycoon.game.ui.theme.Midnight
import com.empiretycoon.game.ui.theme.Paper

/**
 * Pantalla de bienvenida con logo animado, glow detrás y texto pulsante de
 * "Toca para empezar". Devuelve mediante [onFinish] al cabo de [autoMs] ms
 * o al primer toque del usuario.
 */
@Composable
fun SplashScreen(
    autoMs: Long = 2_500L,
    onFinish: () -> Unit
) {
    val logoScale = remember { Animatable(0.4f) }
    val logoAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    var dismissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { logoAlpha.animateTo(1f, tween(550)) }
            launch { logoScale.animateTo(1f, tween(700, easing = LinearOutSlowInEasing)) }
            launch {
                delay(450)
                subtitleAlpha.animateTo(1f, tween(700))
            }
        }
        delay(autoMs)
        if (!dismissing) {
            dismissing = true
            onFinish()
        }
    }

    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Midnight, Ink)
                )
            )
            .clickable(
                indication = null,
                interactionSource = interaction
            ) {
                if (!dismissing) {
                    dismissing = true
                    onFinish()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Halo dorado
        Canvas(Modifier.size(360.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Gold.copy(alpha = 0.32f),
                        Gold.copy(alpha = 0.08f),
                        Color.Transparent
                    )
                ),
                radius = size.minDimension / 2f
            )
        }

        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🏢",
                fontSize = 84.sp,
                modifier = Modifier
                    .alpha(logoAlpha.value)
                    .scale(logoScale.value)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "EMPIRE TYCOON",
                color = Gold,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(logoAlpha.value)
                    .scale(logoScale.value)
                    .shimmer(color = Gold)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Construye tu imperio, paso a paso",
                color = Paper,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )
            Spacer(Modifier.height(48.dp))
            Text(
                text = "Toca para empezar",
                color = Dim,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .alpha(subtitleAlpha.value)
                    .pulse(active = true, sizeBoost = 0.06f)
            )
        }
    }
}
