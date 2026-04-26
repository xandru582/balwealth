package com.empiretycoon.game.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.ui.theme.Ink
import com.empiretycoon.game.ui.theme.Paper
import com.empiretycoon.game.ui.theme.Ruby

/**
 * Burbuja roja con número. Aparece sólo si [count] > 0. Cuando el conteo
 * incrementa hace un pop de escala. Se diseña para superponerse a un icono
 * (parent debe usar Box con Alignment).
 */
@Composable
fun AnimatedBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1f) }
    var lastCount by remember { mutableStateOf(count) }

    LaunchedEffect(count) {
        if (count > lastCount) {
            scale.snapTo(1f)
            scale.animateTo(1.45f, tween(140))
            scale.animateTo(1f, tween(220))
        }
        lastCount = count
    }

    AnimatedVisibility(
        visible = count > 0,
        enter = scaleIn(tween(180)) + fadeIn(tween(180)),
        exit = scaleOut(tween(140)) + fadeOut(tween(140)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .scale(scale.value)
                .sizeIn(minWidth = 16.dp, minHeight = 16.dp)
                .clip(CircleShape)
                .background(Ruby)
                .border(1.dp, Ink, CircleShape)
                .padding(horizontal = 4.dp, vertical = 1.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                color = Paper,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
