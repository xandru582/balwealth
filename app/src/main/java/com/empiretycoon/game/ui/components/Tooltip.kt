package com.empiretycoon.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.ui.theme.Gold
import com.empiretycoon.game.ui.theme.InkSoft
import com.empiretycoon.game.ui.theme.Paper

/**
 * Globo informativo (tooltip) anclado a un `Rect` de pantalla. Usa una
 * capa oscura semitransparente para atenuar el resto de la UI y dibuja
 * una flechita apuntando al ancla.
 *
 * @param visible si false, no se renderiza nada.
 * @param message texto del tooltip (1-2 frases idealmente).
 * @param anchorBounds rect del widget al que apunta.
 * @param onDismiss llamado al tocar fuera o pulsar la X.
 * @param primaryActionLabel etiqueta del botón principal.
 * @param onPrimary acción del botón principal.
 */
@Composable
fun Tooltip(
    visible: Boolean,
    message: String,
    anchorBounds: Rect,
    onDismiss: () -> Unit,
    primaryActionLabel: String = "Entendido",
    onPrimary: () -> Unit
) {
    if (!visible) return

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
    ) {
        // Calcular posición del balloon: debajo del anchor si hay sitio, si no encima.
        val density = LocalDensity.current
        val balloonMaxWidth = 280.dp
        val balloonMaxWidthPx = with(density) { balloonMaxWidth.toPx() }
        val arrowSize = 10.dp
        val arrowSizePx = with(density) { arrowSize.toPx() }

        // Posición preferida: justo debajo del anchor, centrado horizontalmente.
        val anchorCenterX = anchorBounds.center.x
        var balloonX = anchorCenterX - balloonMaxWidthPx / 2f
        if (balloonX < 16f) balloonX = 16f
        val balloonY = anchorBounds.bottom + arrowSizePx

        Box(
            Modifier
                .offset { IntOffset(balloonX.toInt(), balloonY.toInt()) }
                .widthIn(max = balloonMaxWidth)
                .clip(RoundedCornerShape(12.dp))
                .background(InkSoft)
                .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(12.dp)
                .pointerInput(Unit) {
                    // Tragar taps dentro del globo para no cerrar.
                    detectTapGestures(onTap = { })
                }
        ) {
            Column {
                Text(
                    message,
                    color = Paper,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.padding(end = 4.dp)) {
                        TextButton(onClick = onDismiss) {
                            Text("Cerrar", color = Color(0xFFB0B6BD))
                        }
                    }
                    TextButton(onClick = onPrimary) {
                        Text(primaryActionLabel, color = Gold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Flecha apuntando al ancla
        Canvas(
            Modifier
                .fillMaxSize()
        ) {
            val arrowTip = Offset(anchorCenterX, anchorBounds.bottom)
            val arrowBase = Offset(anchorCenterX, anchorBounds.bottom + arrowSizePx)
            val path = Path().apply {
                moveTo(arrowTip.x, arrowTip.y)
                lineTo(arrowBase.x - arrowSizePx, arrowBase.y)
                lineTo(arrowBase.x + arrowSizePx, arrowBase.y)
                close()
            }
            drawPath(path, color = InkSoft)
            drawPath(path, color = Gold.copy(alpha = 0.5f), style = Stroke(width = 2f))
        }
    }
}

