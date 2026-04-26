package com.empiretycoon.game.world.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Joystick virtual: drag dentro de un círculo de [size] dp y reporta el
 * vector normalizado (-1..1) por cada eje. Al soltar, llama a [onRelease].
 */
@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    sizeDp: Int = 140,
    onMove: (dx: Float, dy: Float) -> Unit,
    onRelease: () -> Unit
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    Canvas(
        modifier = modifier
            .size(sizeDp.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        thumbOffset = offset - center
                        report(thumbOffset, size.width / 2f, onMove)
                    },
                    onDrag = { change, _ ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val raw = change.position - center
                        val maxR = size.width / 2f
                        val mag = sqrt(raw.x * raw.x + raw.y * raw.y)
                        thumbOffset = if (mag > maxR) raw * (maxR / mag) else raw
                        report(thumbOffset, maxR, onMove)
                    },
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onRelease()
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        onRelease()
                    }
                )
            }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = min(size.width, size.height) / 2f
        // Glow ring
        drawCircle(
            color = Color(0x33FFD166),
            radius = outerR,
            center = Offset(cx, cy)
        )
        // Base
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xCC162133), Color(0xAA0F1724)),
                center = Offset(cx, cy),
                radius = outerR
            ),
            radius = outerR * 0.92f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color(0x55FFD166),
            radius = outerR * 0.92f,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )
        // Thumb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFD166), Color(0xFFD18C00)),
                center = Offset(cx + thumbOffset.x, cy + thumbOffset.y),
                radius = outerR * 0.35f
            ),
            radius = outerR * 0.32f,
            center = Offset(cx + thumbOffset.x, cy + thumbOffset.y)
        )
    }
}

private fun report(thumb: Offset, maxR: Float, onMove: (Float, Float) -> Unit) {
    val nx = (thumb.x / maxR).coerceIn(-1f, 1f)
    val ny = (thumb.y / maxR).coerceIn(-1f, 1f)
    onMove(nx, ny)
}
