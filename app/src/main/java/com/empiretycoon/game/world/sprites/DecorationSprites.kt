package com.empiretycoon.game.world.sprites

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin

fun DrawScope.drawTree(x: Float, y: Float, tileSize: Float, kind: Int = 0, season: Int = 0) {
    val trunk = Color(0xFF6D4C41)
    val leaf = when (season) {
        0 -> Color(0xFF388E3C)  // primavera
        1 -> Color(0xFF2E7D32)  // verano
        2 -> Color(0xFFFB8C00)  // otoño
        else -> Color(0xFFCFD8DC)  // invierno
    }
    drawRect(
        color = trunk,
        topLeft = Offset(x + tileSize * 0.4f, y + tileSize * 0.55f),
        size = Size(tileSize * 0.2f, tileSize * 0.45f)
    )
    when (kind) {
        0, 1 -> {
            drawCircle(color = leaf, radius = tileSize * 0.35f, center = Offset(x + tileSize / 2f, y + tileSize * 0.45f))
        }
        2 -> {
            // Pino: triángulo
            for (i in 0 until 3) {
                drawCircle(
                    color = leaf,
                    radius = tileSize * (0.3f - i * 0.05f),
                    center = Offset(x + tileSize / 2f, y + tileSize * (0.2f + i * 0.18f))
                )
            }
        }
    }
}

fun DrawScope.drawFountain(x: Float, y: Float, tileSize: Float, animPhase: Float) {
    val basin = Color(0xFFB0BEC5)
    val water = Color(0xFF1E88E5)
    drawCircle(color = basin, radius = tileSize * 0.45f, center = Offset(x + tileSize / 2f, y + tileSize / 2f))
    drawCircle(color = water, radius = tileSize * 0.3f, center = Offset(x + tileSize / 2f, y + tileSize / 2f))
    val splash = (sin((animPhase * 6.28f).toDouble()) * 0.5 + 0.5).toFloat()
    drawCircle(
        color = water.copy(alpha = 0.7f),
        radius = tileSize * (0.05f + splash * 0.08f),
        center = Offset(x + tileSize / 2f, y + tileSize * (0.4f - splash * 0.1f))
    )
}

fun DrawScope.drawStreetlamp(x: Float, y: Float, tileSize: Float, on: Boolean) {
    drawRect(
        color = Color(0xFF424242),
        topLeft = Offset(x + tileSize * 0.45f, y + tileSize * 0.3f),
        size = Size(tileSize * 0.1f, tileSize * 0.7f)
    )
    drawCircle(
        color = if (on) Color(0xFFFFEB3B) else Color(0xFF616161),
        radius = tileSize * 0.12f,
        center = Offset(x + tileSize / 2f, y + tileSize * 0.25f)
    )
    if (on) {
        drawCircle(
            color = Color(0x55FFEB3B),
            radius = tileSize * 0.3f,
            center = Offset(x + tileSize / 2f, y + tileSize * 0.25f)
        )
    }
}

fun DrawScope.drawBench(x: Float, y: Float, tileSize: Float) {
    drawRect(
        color = Color(0xFF6D4C41),
        topLeft = Offset(x + tileSize * 0.1f, y + tileSize * 0.5f),
        size = Size(tileSize * 0.8f, tileSize * 0.15f)
    )
    drawRect(
        color = Color(0xFF424242),
        topLeft = Offset(x + tileSize * 0.15f, y + tileSize * 0.65f),
        size = Size(tileSize * 0.05f, tileSize * 0.2f)
    )
    drawRect(
        color = Color(0xFF424242),
        topLeft = Offset(x + tileSize * 0.8f, y + tileSize * 0.65f),
        size = Size(tileSize * 0.05f, tileSize * 0.2f)
    )
}
