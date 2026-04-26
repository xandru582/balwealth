package com.empiretycoon.game.world.sprites

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.empiretycoon.game.model.BuildingType

/**
 * Dibuja un edificio detallado en (x,y) ocupando 3x3 baldosas.
 * Mejoras: gradiente vertical en pared, tejas en tejado, ventanas con marco
 * y halo cálido cuando están iluminadas, alféizar, puerta con peldaños.
 */
fun DrawScope.drawBuildingExterior(
    buildingType: BuildingType,
    x: Float,
    y: Float,
    tileSize: Float,
    level: Int = 1,
    active: Boolean = true,
    animPhase: Float = 0f
) {
    val w = tileSize * 3f
    val h = tileSize * 3f
    val (wallColor, roofColor, accentColor) = palette(buildingType)

    // Sombra del edificio en el suelo (elíptica)
    drawArc(
        color = Color(0x77000000), startAngle = 0f, sweepAngle = 360f, useCenter = true,
        topLeft = Offset(x + tileSize * 0.05f, y + h - tileSize * 0.18f),
        size = Size(w * 0.95f, tileSize * 0.32f)
    )

    val floors = (1 + level / 2).coerceAtMost(5)
    val bodyH = h * 0.7f
    val bodyTop = y + h - bodyH
    val bodyLeft = x + tileSize * 0.1f
    val bodyW = w - tileSize * 0.2f

    // CUERPO con gradiente vertical (más claro arriba, más oscuro abajo) +
    // gradiente horizontal sutil (luz desde la izquierda)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(wallColor.lighten(0.10f), wallColor, wallColor.darken(0.12f)),
            startY = bodyTop,
            endY = bodyTop + bodyH
        ),
        topLeft = Offset(bodyLeft, bodyTop),
        size = Size(bodyW, bodyH)
    )
    // Sombra lateral derecha (depth)
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color.Transparent, Color(0x33000000))
        ),
        topLeft = Offset(bodyLeft, bodyTop),
        size = Size(bodyW, bodyH)
    )

    // BORDES DE PISO horizontal entre niveles
    val floorH = bodyH / floors
    for (f in 1 until floors) {
        drawRect(
            color = wallColor.darken(0.18f),
            topLeft = Offset(bodyLeft, bodyTop + f * floorH - tileSize * 0.02f),
            size = Size(bodyW, tileSize * 0.04f)
        )
    }

    // TEJADO inclinado (trapezoide visual) con tejas de líneas
    val roofH = tileSize * 0.4f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(roofColor.lighten(0.15f), roofColor, roofColor.darken(0.10f))
        ),
        topLeft = Offset(x, bodyTop - roofH),
        size = Size(w, roofH + tileSize * 0.05f)
    )
    // Líneas de tejas
    for (i in 0 until 4) {
        drawRect(
            color = roofColor.darken(0.25f),
            topLeft = Offset(x, bodyTop - roofH + i * (roofH / 4f)),
            size = Size(w, 1.5f)
        )
    }
    // Borde frontal del tejado (sombra inferior)
    drawRect(
        color = Color(0x55000000),
        topLeft = Offset(x, bodyTop - tileSize * 0.06f),
        size = Size(w, tileSize * 0.04f)
    )

    // VENTANAS con marco, alféizar y halo cálido si iluminadas (active)
    // De noche (active && hora oscura) las ventanas brillan; aquí no tenemos
    // hora, pero usamos active como proxy: si hay producción, hay luces.
    val windowOn = active
    val windowGlow = Color(0xFFFFEE8C)         // amarillo cálido
    val windowOff = Color(0xFF263238)          // gris azulado apagado
    val windowFrameColor = wallColor.darken(0.30f)
    val cols = 3
    val rows = floors
    val padX = tileSize * 0.3f
    val padTop = tileSize * 0.15f
    val winW = (bodyW - padX * 2f) / (cols * 1.4f)
    val winH = (bodyH - tileSize * 0.85f) / (rows * 1.5f)
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if (r == rows - 1 && c == cols / 2) continue   // hueco para puerta
            val wx = bodyLeft + padX + c * (winW * 1.4f)
            val wy = bodyTop + padTop + r * (winH * 1.5f)
            // Marco
            drawRect(
                color = windowFrameColor,
                topLeft = Offset(wx - 1.5f, wy - 1.5f),
                size = Size(winW + 3f, winH + 3f)
            )
            // Cristal con gradiente (reflejo)
            drawRect(
                brush = if (windowOn)
                    Brush.verticalGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFD54F), Color(0xFFFFB300)))
                else
                    Brush.verticalGradient(listOf(Color(0xFF455A64), Color(0xFF263238))),
                topLeft = Offset(wx, wy),
                size = Size(winW, winH)
            )
            // Cruz divisoria
            drawRect(
                color = windowFrameColor,
                topLeft = Offset(wx + winW * 0.48f, wy),
                size = Size(winW * 0.04f, winH)
            )
            drawRect(
                color = windowFrameColor,
                topLeft = Offset(wx, wy + winH * 0.48f),
                size = Size(winW, winH * 0.04f)
            )
            // Alféizar (sill)
            drawRect(
                color = wallColor.darken(0.25f),
                topLeft = Offset(wx - 2f, wy + winH),
                size = Size(winW + 4f, 3f)
            )
            // Halo cálido si iluminada
            if (windowOn) {
                drawCircle(
                    color = windowGlow.copy(alpha = 0.20f),
                    radius = winW * 0.7f,
                    center = Offset(wx + winW / 2f, wy + winH / 2f)
                )
            }
        }
    }

    // PUERTA con peldaños
    val doorW = tileSize * 0.55f
    val doorH = tileSize * 0.75f
    val doorX = x + w / 2f - doorW / 2f
    val doorY = y + h - doorH - tileSize * 0.05f
    // Peldaños
    drawRect(color = Color(0xFFB0BEC5),
        topLeft = Offset(doorX - tileSize * 0.05f, doorY + doorH - tileSize * 0.04f),
        size = Size(doorW + tileSize * 0.10f, tileSize * 0.06f))
    drawRect(color = Color(0xFF90A4AE),
        topLeft = Offset(doorX - tileSize * 0.08f, doorY + doorH + tileSize * 0.02f),
        size = Size(doorW + tileSize * 0.16f, tileSize * 0.05f))
    // Marco de la puerta
    drawRect(
        color = accentColor.darken(0.20f),
        topLeft = Offset(doorX - 2f, doorY - 2f),
        size = Size(doorW + 4f, doorH + 4f)
    )
    // Hoja de la puerta con gradiente
    drawRect(
        brush = Brush.verticalGradient(listOf(accentColor.lighten(0.10f), accentColor, accentColor.darken(0.15f))),
        topLeft = Offset(doorX, doorY),
        size = Size(doorW, doorH)
    )
    // Vidrio superior de la puerta
    drawRect(
        color = if (windowOn) Color(0xFFFFE082) else Color(0xFF263238),
        topLeft = Offset(doorX + doorW * 0.18f, doorY + doorH * 0.10f),
        size = Size(doorW * 0.64f, doorH * 0.30f)
    )
    // Manilla
    drawCircle(color = Color(0xFFFFD700), radius = tileSize * 0.025f,
        center = Offset(doorX + doorW * 0.85f, doorY + doorH * 0.55f))

    // Cartel/letrero con borde + texto simulado
    val signY = bodyTop - tileSize * 0.04f
    drawRect(
        brush = Brush.verticalGradient(listOf(accentColor.lighten(0.10f), accentColor, accentColor.darken(0.12f))),
        topLeft = Offset(x + tileSize * 0.4f, signY),
        size = Size(w - tileSize * 0.8f, tileSize * 0.20f)
    )
    // Borde del cartel
    drawRect(color = Color(0xFF263238),
        topLeft = Offset(x + tileSize * 0.4f, signY),
        size = Size(w - tileSize * 0.8f, 1.5f))
    drawRect(color = Color(0xFF263238),
        topLeft = Offset(x + tileSize * 0.4f, signY + tileSize * 0.20f - 1.5f),
        size = Size(w - tileSize * 0.8f, 1.5f))
    // Tres trazos blancos simulando el nombre del local
    for (i in 0 until 3) {
        drawRect(color = Color(0xCCFFFFFF),
            topLeft = Offset(x + tileSize * 0.5f + i * tileSize * 0.55f, signY + tileSize * 0.07f),
            size = Size(tileSize * 0.40f, tileSize * 0.06f))
    }

    // Humo de chimenea si fábrica/refinería/fundición y active
    if (active && (buildingType == BuildingType.FACTORY ||
        buildingType == BuildingType.REFINERY ||
        buildingType == BuildingType.SMELTER)) {
        val cx = x + w * 0.75f
        val cy = y + h - bodyH - tileSize * 0.3f
        drawRect(
            color = Color(0xFF424242),
            topLeft = Offset(cx, cy - tileSize * 0.4f),
            size = Size(tileSize * 0.2f, tileSize * 0.4f)
        )
        for (i in 0 until 3) {
            val phase = (animPhase + i * 0.3f) % 1f
            drawCircle(
                color = Color(0x88AAAAAA).copy(alpha = (1f - phase) * 0.7f),
                radius = tileSize * (0.1f + phase * 0.2f),
                center = Offset(cx + tileSize * 0.1f, cy - tileSize * 0.5f - phase * tileSize * 0.6f)
            )
        }
    }

    // Bandera si nivel alto
    if (level >= 4) {
        val fx = x + w / 2f
        val fy = y + h - bodyH - tileSize * 0.3f
        drawRect(
            color = Color(0xFF424242),
            topLeft = Offset(fx, fy - tileSize * 0.5f),
            size = Size(tileSize * 0.04f, tileSize * 0.5f)
        )
        drawRect(
            color = accentColor,
            topLeft = Offset(fx, fy - tileSize * 0.5f),
            size = Size(tileSize * 0.3f, tileSize * 0.2f)
        )
    }
}

private fun palette(t: BuildingType): Triple<Color, Color, Color> = when (t) {
    BuildingType.FARM      -> Triple(Color(0xFF8D6E63), Color(0xFFD32F2F), Color(0xFFFBC02D))
    BuildingType.SAWMILL   -> Triple(Color(0xFF6D4C41), Color(0xFF4E342E), Color(0xFF8D6E63))
    BuildingType.MINE      -> Triple(Color(0xFF424242), Color(0xFF212121), Color(0xFFFFC107))
    BuildingType.BAKERY    -> Triple(Color(0xFFFFE0B2), Color(0xFFE65100), Color(0xFFFFCA28))
    BuildingType.SMELTER   -> Triple(Color(0xFF455A64), Color(0xFF263238), Color(0xFFFF7043))
    BuildingType.REFINERY  -> Triple(Color(0xFF607D8B), Color(0xFF37474F), Color(0xFF03A9F4))
    BuildingType.FACTORY   -> Triple(Color(0xFF5C6BC0), Color(0xFF303F9F), Color(0xFFFFEB3B))
    BuildingType.OFFICE    -> Triple(Color(0xFF90CAF9), Color(0xFF1565C0), Color(0xFF263238))
    BuildingType.JEWELRY   -> Triple(Color(0xFFCE93D8), Color(0xFF8E24AA), Color(0xFFFFD700))
    BuildingType.SHIPYARD  -> Triple(Color(0xFF80DEEA), Color(0xFF006064), Color(0xFFFFFFFF))
    BuildingType.WAREHOUSE -> Triple(Color(0xFF9E9E9E), Color(0xFF424242), Color(0xFFEF6C00))
}
