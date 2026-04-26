package com.empiretycoon.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.model.TutorialSpec
import com.empiretycoon.game.ui.theme.Dim
import com.empiretycoon.game.ui.theme.Gold
import com.empiretycoon.game.ui.theme.Ink
import com.empiretycoon.game.ui.theme.InkSoft
import com.empiretycoon.game.ui.theme.Paper
import com.empiretycoon.game.ui.theme.Ruby

/**
 * Coachmark a pantalla completa con efecto "spotlight": oscurece todo salvo
 * un agujero (rectángulo redondeado) alrededor del `anchorBounds`.
 *
 * Usa un `Path` con operación DIFFERENCE para recortar el agujero. Si no hay
 * ancla, oscurece toda la pantalla y muestra solo la card central.
 *
 * @param spec definición narrativa del paso actual.
 * @param anchorBounds rect del widget objetivo (puede ser null en pasos sin ancla).
 * @param onSkip cierra el coachmark (solo el actual; equivale a "Entendido").
 * @param onPrimary acción primaria (avanza el paso si la condición es TAP_PRIMARY).
 * @param onSkipAll desactiva por completo el tutorial.
 */
@Composable
fun Coachmark(
    spec: TutorialSpec,
    anchorBounds: Rect?,
    onSkip: () -> Unit,
    onPrimary: () -> Unit,
    onSkipAll: () -> Unit
) {
    val density = LocalDensity.current
    val cornerPx = with(density) { 12.dp.toPx() }
    val paddingPx = with(density) { 6.dp.toPx() }

    // FIX BUG-08-05: si no hay anchor, el coachmark queda "stuck" porque no
    // hay nada que tappear. Hacemos que siempre se pueda cerrar con tap fuera
    // de la card cuando el anchor está ausente.
    val canDismissOutside = spec.dismissable || anchorBounds == null
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(canDismissOutside) {
                detectTapGestures(
                    onTap = { if (canDismissOutside) onSkip() }
                )
            }
    ) {
        // Capa oscura con agujero usando Path.op DIFFERENCE
        Canvas(Modifier.fillMaxSize()) {
            val full = Path().apply {
                addRect(Rect(Offset.Zero, size))
            }
            val cutout = Path()
            if (anchorBounds != null && anchorBounds.width > 0f && anchorBounds.height > 0f) {
                val expanded = Rect(
                    left = (anchorBounds.left - paddingPx).coerceAtLeast(0f),
                    top = (anchorBounds.top - paddingPx).coerceAtLeast(0f),
                    right = (anchorBounds.right + paddingPx).coerceAtMost(size.width),
                    bottom = (anchorBounds.bottom + paddingPx).coerceAtMost(size.height)
                )
                cutout.addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        rect = expanded,
                        radiusX = cornerPx,
                        radiusY = cornerPx
                    )
                )
            }
            val result = Path().apply {
                op(full, cutout, PathOperation.Difference)
            }
            drawPath(result, color = Color.Black.copy(alpha = 0.72f))

            // Borde dorado alrededor del foco
            if (anchorBounds != null) {
                drawPath(
                    cutout,
                    color = Gold,
                    style = Stroke(width = 3f)
                )
            }
        }

        // Card con el mensaje, posicionada arriba o abajo según la posición del ancla
        CoachmarkCard(
            spec = spec,
            anchorBounds = anchorBounds,
            onSkip = onSkip,
            onPrimary = onPrimary,
            onSkipAll = onSkipAll
        )
    }
}

@Composable
private fun CoachmarkCard(
    spec: TutorialSpec,
    anchorBounds: Rect?,
    onSkip: () -> Unit,
    onPrimary: () -> Unit,
    onSkipAll: () -> Unit
) {
    val density = LocalDensity.current
    val cardMaxWidth = 320.dp

    var cardSize by remember { mutableStateOf(Size.Zero) }
    var screenSize by remember { mutableStateOf(Size.Zero) }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                val sz = coords.size
                screenSize = Size(sz.width.toFloat(), sz.height.toFloat())
            }
    ) {
        val offset = computeCardOffset(
            anchorBounds = anchorBounds,
            cardSize = cardSize,
            screenSize = screenSize,
            density = density
        )
        Box(
            Modifier
                .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
                .widthIn(max = cardMaxWidth)
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(InkSoft)
                .border(2.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                .padding(16.dp)
                .onGloballyPositioned { coords ->
                    val sz = coords.size
                    cardSize = Size(sz.width.toFloat(), sz.height.toFloat())
                }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { /* swallow */ })
                }
        ) {
            Column {
                Text(
                    spec.title,
                    color = Gold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    spec.message,
                    color = Paper,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onSkipAll) {
                        Text("Saltar tutorial", color = Ruby, fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(0.dp))
                    Row(
                        Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // FIX BUG-08-05: "Después" siempre visible si no hay
                        // anchor — el usuario no tiene cómo avanzar de otro modo.
                        if (spec.dismissable || anchorBounds == null) {
                            TextButton(onClick = onSkip) {
                                Text("Después", color = Dim, fontSize = 12.sp)
                            }
                            Spacer(Modifier.width(4.dp))
                        }
                        Button(
                            onClick = onPrimary,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Gold, contentColor = Ink
                            )
                        ) {
                            Text(spec.primaryAction, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Calcula la posición de la card en píxeles. Coloca la tarjeta:
 * - Debajo del anchor si hay espacio.
 * - Encima del anchor si no hay espacio debajo.
 * - Centrada en pantalla si no hay anchor.
 */
private fun computeCardOffset(
    anchorBounds: Rect?,
    cardSize: Size,
    screenSize: Size,
    density: androidx.compose.ui.unit.Density
): Offset {
    if (cardSize == Size.Zero || screenSize == Size.Zero) return Offset.Zero
    if (anchorBounds == null) {
        val x = (screenSize.width - cardSize.width) / 2f
        val y = (screenSize.height - cardSize.height) / 2f
        return Offset(x.coerceAtLeast(0f), y.coerceAtLeast(0f))
    }
    val gap = with(density) { 14.dp.toPx() }
    val candidateBelow = anchorBounds.bottom + gap
    val candidateAbove = anchorBounds.top - gap - cardSize.height
    val y = when {
        candidateBelow + cardSize.height <= screenSize.height -> candidateBelow
        candidateAbove >= 0f -> candidateAbove
        else -> (screenSize.height - cardSize.height) / 2f
    }
    val anchorCx = anchorBounds.center.x
    var x = anchorCx - cardSize.width / 2f
    if (x < 0f) x = 0f
    if (x + cardSize.width > screenSize.width) x = screenSize.width - cardSize.width
    return Offset(x, y)
}
