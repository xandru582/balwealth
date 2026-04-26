package com.empiretycoon.game.ui.components

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.empiretycoon.game.ui.theme.Emerald
import com.empiretycoon.game.ui.theme.Ruby
import com.empiretycoon.game.util.fmtMoney
import com.empiretycoon.game.util.fmtInt

/**
 * Cuenta el dinero con interpolación suave entre valor previo y valor nuevo.
 * Cuando el valor sube, parpadea en verde. Cuando baja, en rojo. Tras la
 * transición vuelve al [color] indicado.
 */
@Composable
fun AnimatedMoneyCounter(
    value: Double,
    color: Color,
    fontSize: TextUnit = 16.sp,
    modifier: Modifier = Modifier
) {
    var previous by remember { mutableStateOf(value) }
    var direction by remember { mutableStateOf(0) } // -1 = baja, 0 = neutro, 1 = sube

    val animated by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(durationMillis = 700, easing = LinearOutSlowInEasing),
        label = "moneyAnim"
    )

    LaunchedEffect(value) {
        direction = when {
            value > previous + 0.5 -> 1
            value < previous - 0.5 -> -1
            else -> 0
        }
        previous = value
        delay(700)
        direction = 0
    }

    val targetColor = when (direction) {
        1 -> Emerald
        -1 -> Ruby
        else -> color
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 350),
        label = "moneyColor"
    )

    Text(
        text = animated.toDouble().fmtMoney(),
        color = animatedColor,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        modifier = modifier
    )
}

/**
 * Cuenta entera con animación suave entre valores. Útil para reputación,
 * energía, niveles, etc.
 */
@Composable
fun AnimatedIntCounter(
    value: Int,
    suffix: String = "",
    color: Color,
    fontSize: TextUnit = 14.sp,
    modifier: Modifier = Modifier
) {
    var previous by remember { mutableStateOf(value) }
    var direction by remember { mutableStateOf(0) }

    val animated by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "intAnim"
    )

    LaunchedEffect(value) {
        direction = when {
            value > previous -> 1
            value < previous -> -1
            else -> 0
        }
        previous = value
        delay(600)
        direction = 0
    }

    val targetColor = when (direction) {
        1 -> Emerald
        -1 -> Ruby
        else -> color
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 300),
        label = "intColor"
    )

    Crossfade(
        targetState = animated.toInt(),
        animationSpec = tween(durationMillis = 200),
        label = "intCrossfade",
        modifier = modifier
    ) { v ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = v.fmtInt(),
                color = animatedColor,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize
            )
            if (suffix.isNotEmpty()) {
                Spacer(Modifier.width(2.dp))
                Text(
                    text = suffix,
                    color = animatedColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = fontSize
                )
            }
        }
    }
}
