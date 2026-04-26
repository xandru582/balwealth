package com.empiretycoon.game.world.sprites

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.empiretycoon.game.world.TileType
import kotlin.math.sin

/**
 * Renderer de baldosas v2: cada tile tiene textura procedural rica con
 * variantes según `decoIndex` para evitar el efecto cuadricular.
 *
 * Convenciones:
 *  - GRASS  → tonos verdes con briznas, flores, sombras suaves.
 *  - ROAD   → asfalto con grietas + marcas viales.
 *  - SIDEWALK → losas grandes con junta visible.
 *  - PLAZA  → patrón ornamental geométrico.
 *  - WATER  → ondas animadas + reflejos.
 *  - WALL   → ladrillo con sombra de bisel.
 *  - DOOR   → puerta de madera con marco dorado y manilla.
 *  - FLOOR_INDOOR → parqué.
 *  - FOREST_FLOOR → musgo + hojas caídas.
 *  - RAIL   → vías sobre balasto.
 *  - BRIDGE → tablones.
 */
fun DrawScope.drawTile(
    type: TileType,
    x: Float,
    y: Float,
    tileSize: Float,
    decoIndex: Int,
    animPhase: Float
) {
    val base = Color(type.decoColor1.toInt())
    val deco = Color(type.decoColor2.toInt())

    when (type) {
        TileType.GRASS -> drawGrass(x, y, tileSize, decoIndex, base, deco)
        TileType.ROAD -> drawRoad(x, y, tileSize, base, deco, false, false)
        TileType.ROAD_LINE_H -> drawRoad(x, y, tileSize, base, deco, true, false)
        TileType.ROAD_LINE_V -> drawRoad(x, y, tileSize, base, deco, false, true)
        TileType.SIDEWALK -> drawSidewalk(x, y, tileSize, base, deco)
        TileType.PLAZA_TILE -> drawPlaza(x, y, tileSize, decoIndex, base, deco)
        TileType.WATER -> drawWater(x, y, tileSize, animPhase, decoIndex, base, deco)
        TileType.SAND -> drawSand(x, y, tileSize, decoIndex, base, deco)
        TileType.WALL -> drawWall(x, y, tileSize, base, deco)
        TileType.FLOOR_INDOOR -> drawIndoor(x, y, tileSize, base, deco)
        TileType.DOOR -> drawDoor(x, y, tileSize, base, deco)
        TileType.FOREST_FLOOR -> drawForest(x, y, tileSize, decoIndex, base, deco)
        TileType.RAIL -> drawRail(x, y, tileSize, base, deco)
        TileType.BRIDGE -> drawBridge(x, y, tileSize, base, deco)
    }
}

// =====================================================================
//                          GRASS variantes
// =====================================================================

private fun DrawScope.drawGrass(x: Float, y: Float, t: Float, deco: Int, base: Color, decoColor: Color) {
    val variant = deco and 0x7
    // Fondo con gradient diagonal (sombra sutil) — mucho menos uniforme
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(base.lighten(0.08f), base, base.darken(0.06f)),
            start = Offset(x, y),
            end = Offset(x + t, y + t)
        ),
        topLeft = Offset(x, y),
        size = Size(t, t)
    )

    // Manchas orgánicas de tono distinto (rompe la uniformidad)
    val patches = 2 + (variant and 0x3)
    for (i in 0 until patches) {
        val px = x + ((i * 73 + deco * 13) % 100) / 100f * t
        val py = y + ((i * 51 + deco * 17) % 100) / 100f * t
        val pr = t * (0.10f + ((i * 31) % 7) * 0.02f)
        val tone = if (i % 2 == 0) base.darken(0.08f) else base.lighten(0.06f)
        drawCircle(color = tone.copy(alpha = 0.6f), radius = pr, center = Offset(px, py))
    }

    // Capa frontal sombreada (efecto de hierba alta delante)
    drawRect(
        color = decoColor.copy(alpha = 0.30f),
        topLeft = Offset(x, y + t * 0.65f),
        size = Size(t, t * 0.35f)
    )

    // Briznas de hierba con dos tonos (más volumen)
    val blades = 6 + variant
    for (i in 0 until blades) {
        val bx = x + ((i * 23 + deco * 7) % 14) * (t / 16f)
        val by = y + ((i * 17 + deco * 11) % 12) * (t / 16f) + t * 0.30f
        // Sombra brizna
        drawRect(
            color = decoColor.darken(0.30f),
            topLeft = Offset(bx, by),
            size = Size(t / 28f, t / 7f)
        )
        // Brizna principal
        drawRect(
            color = decoColor,
            topLeft = Offset(bx + 1f, by + t / 64f),
            size = Size(t / 32f, t / 8f)
        )
        // Punta clara
        drawRect(
            color = decoColor.lighten(0.20f),
            topLeft = Offset(bx + 1f, by),
            size = Size(t / 48f, t / 16f)
        )
    }

    // Pequeñas piedrecitas dispersas
    if ((deco and 0x4) != 0) {
        for (i in 0 until 3) {
            val sx = x + ((i * 89 + deco * 11) % 100) / 100f * t
            val sy = y + ((i * 67 + deco * 13) % 100) / 100f * t
            drawCircle(color = Color(0xFF8D8D8D), radius = t * 0.012f, center = Offset(sx, sy))
        }
    }

    // Flores decorativas con corola y centro
    when (deco and 0x18) {
        0x08 -> {
            val cx = x + t * 0.65f; val cy = y + t * 0.4f
            // 5 pétalos
            for (k in 0 until 5) {
                val a = k * 1.2566f
                drawCircle(
                    color = Color(0xFFFFEB3B),
                    radius = t * 0.030f,
                    center = Offset(cx + sin(a.toDouble()).toFloat() * t * 0.035f,
                                    cy + sin((a + 1.57f).toDouble()).toFloat() * t * 0.035f)
                )
            }
            drawCircle(color = Color(0xFFFB8C00), radius = t * 0.022f, center = Offset(cx, cy))
        }
        0x10 -> {
            val cx = x + t * 0.30f; val cy = y + t * 0.50f
            for (k in 0 until 5) {
                val a = k * 1.2566f
                drawCircle(
                    color = Color(0xFFE91E63),
                    radius = t * 0.028f,
                    center = Offset(cx + sin(a.toDouble()).toFloat() * t * 0.032f,
                                    cy + sin((a + 1.57f).toDouble()).toFloat() * t * 0.032f)
                )
            }
            drawCircle(color = Color(0xFFFFFFFF), radius = t * 0.018f, center = Offset(cx, cy))
        }
        0x18 -> {
            val cx = x + t * 0.50f; val cy = y + t * 0.35f
            for (k in 0 until 6) {
                val a = k * 1.047f
                drawCircle(
                    color = Color(0xFFFFFFFF),
                    radius = t * 0.030f,
                    center = Offset(cx + sin(a.toDouble()).toFloat() * t * 0.040f,
                                    cy + sin((a + 1.57f).toDouble()).toFloat() * t * 0.040f)
                )
            }
            drawCircle(color = Color(0xFFFFEB3B), radius = t * 0.020f, center = Offset(cx, cy))
        }
    }
}

// =====================================================================
//                          ROAD / asfalto
// =====================================================================

private fun DrawScope.drawRoad(x: Float, y: Float, t: Float, base: Color, deco: Color, lineH: Boolean, lineV: Boolean) {
    // Asfalto con gradient diagonal (subtil moteado)
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(base.lighten(0.08f), base, base.darken(0.12f)),
            start = Offset(x, y),
            end = Offset(x + t, y + t)
        ),
        topLeft = Offset(x, y),
        size = Size(t, t)
    )

    // Mancha de aceite sutil (variedad por seed implícito)
    val seedX = ((x.toInt() * 37 + y.toInt() * 17) and 0x7)
    if (seedX < 3) {
        drawCircle(
            color = base.darken(0.20f).copy(alpha = 0.3f),
            radius = t * 0.12f,
            center = Offset(x + t * 0.3f, y + t * 0.6f)
        )
    }

    // Grietas y texturas finas (varias)
    drawLine(color = base.darken(0.30f),
        start = Offset(x + t * 0.10f, y + t * 0.20f),
        end = Offset(x + t * 0.35f, y + t * 0.42f), strokeWidth = 1f)
    drawLine(color = base.darken(0.25f),
        start = Offset(x + t * 0.65f, y + t * 0.30f),
        end = Offset(x + t * 0.85f, y + t * 0.55f), strokeWidth = 1f)
    drawLine(color = base.darken(0.20f),
        start = Offset(x + t * 0.20f, y + t * 0.75f),
        end = Offset(x + t * 0.45f, y + t * 0.85f), strokeWidth = 1f)

    // Marca vial — DASHED (más realista que línea continua)
    if (lineH) {
        // Línea central discontinua amarilla
        for (i in 0 until 3) {
            val sx = x + t * (0.10f + i * 0.30f)
            drawRect(
                color = Color(0xFFFFEB3B),
                topLeft = Offset(sx, y + t * 0.46f),
                size = Size(t * 0.20f, t * 0.05f)
            )
            // Sombra de línea (depth)
            drawRect(
                color = Color(0xFFB8860B),
                topLeft = Offset(sx, y + t * 0.51f),
                size = Size(t * 0.20f, 1.5f)
            )
        }
    } else if (lineV) {
        for (i in 0 until 3) {
            val sy = y + t * (0.10f + i * 0.30f)
            drawRect(
                color = Color(0xFFFFEB3B),
                topLeft = Offset(x + t * 0.46f, sy),
                size = Size(t * 0.05f, t * 0.20f)
            )
            drawRect(
                color = Color(0xFFB8860B),
                topLeft = Offset(x + t * 0.51f, sy),
                size = Size(1.5f, t * 0.20f)
            )
        }
    }

    // Pequeños puntos blancos reflectantes en los bordes (catadióptricos)
    drawCircle(color = Color(0xFFFFFFFF).copy(alpha = 0.7f), radius = t * 0.012f,
        center = Offset(x + t * 0.05f, y + t * 0.5f))
    drawCircle(color = Color(0xFFFFFFFF).copy(alpha = 0.7f), radius = t * 0.012f,
        center = Offset(x + t * 0.95f, y + t * 0.5f))
}

// =====================================================================
//                          SIDEWALK / aceras
// =====================================================================

private fun DrawScope.drawSidewalk(x: Float, y: Float, t: Float, base: Color, deco: Color) {
    // Fondo con leve gradiente
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(base.lighten(0.05f), base, base.darken(0.04f)),
            start = Offset(x, y), end = Offset(x + t, y + t)
        ),
        topLeft = Offset(x, y), size = Size(t, t)
    )

    // Cuadrícula 2x2 de losas con bevel (sombra superior derecha + brillo inferior izquierda)
    val lineColor = deco
    // Líneas de junta principales
    drawLine(color = lineColor, start = Offset(x + t / 2f, y), end = Offset(x + t / 2f, y + t), strokeWidth = 1.5f)
    drawLine(color = lineColor, start = Offset(x, y + t / 2f), end = Offset(x + t, y + t / 2f), strokeWidth = 1.5f)
    // Sombra interna de junta (depth)
    drawLine(color = lineColor.darken(0.4f),
        start = Offset(x + t / 2f + 0.5f, y), end = Offset(x + t / 2f + 0.5f, y + t),
        strokeWidth = 0.8f)
    drawLine(color = lineColor.darken(0.4f),
        start = Offset(x, y + t / 2f + 0.5f), end = Offset(x + t, y + t / 2f + 0.5f),
        strokeWidth = 0.8f)

    // Bevel: cada losa tiene highlight superior y sombra inferior
    val s = t / 2f
    for (rr in 0 until 2) for (cc in 0 until 2) {
        val sx = x + cc * s
        val sy = y + rr * s
        // Highlight superior izquierdo
        drawLine(color = base.lighten(0.18f),
            start = Offset(sx + 1f, sy + 1f), end = Offset(sx + s - 1f, sy + 1f), strokeWidth = 1f)
        drawLine(color = base.lighten(0.18f),
            start = Offset(sx + 1f, sy + 1f), end = Offset(sx + 1f, sy + s - 1f), strokeWidth = 1f)
        // Sombra inferior derecha
        drawRect(color = base.darken(0.10f),
            topLeft = Offset(sx + s - 2f, sy + 1f), size = Size(1.5f, s - 2f))
        drawRect(color = base.darken(0.10f),
            topLeft = Offset(sx + 1f, sy + s - 2f), size = Size(s - 2f, 1.5f))
    }

    // Pequeños puntos texturales (granito)
    val seedV = ((x.toInt() * 13 + y.toInt() * 7) and 0xF)
    if (seedV < 10) {
        for (i in 0 until 4) {
            val px = x + ((i * 79 + seedV * 13) % 100) / 100f * t
            val py = y + ((i * 53 + seedV * 11) % 100) / 100f * t
            drawCircle(color = base.darken(0.20f).copy(alpha = 0.4f),
                radius = t * 0.010f, center = Offset(px, py))
        }
    }
}

// =====================================================================
//                          PLAZA ornamental
// =====================================================================

private fun DrawScope.drawPlaza(x: Float, y: Float, t: Float, deco: Int, base: Color, decoColor: Color) {
    // Fondo con gradient sutil
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(base.lighten(0.06f), base, base.darken(0.04f)),
            start = Offset(x, y), end = Offset(x + t, y + t)
        ),
        topLeft = Offset(x, y), size = Size(t, t)
    )
    val cx = x + t / 2f
    val cy = y + t / 2f

    // Marco exterior cuadrado
    drawRect(color = decoColor.darken(0.15f),
        topLeft = Offset(x + t * 0.04f, y + t * 0.04f),
        size = Size(t * 0.92f, t * 0.92f),
        style = Stroke(width = 1.5f))

    // Cuadrado interno girado 45° (rombo) — simulado con 4 líneas
    val inner = t * 0.40f
    drawLine(color = decoColor, start = Offset(cx, cy - inner), end = Offset(cx + inner, cy), strokeWidth = 2f)
    drawLine(color = decoColor, start = Offset(cx + inner, cy), end = Offset(cx, cy + inner), strokeWidth = 2f)
    drawLine(color = decoColor, start = Offset(cx, cy + inner), end = Offset(cx - inner, cy), strokeWidth = 2f)
    drawLine(color = decoColor, start = Offset(cx - inner, cy), end = Offset(cx, cy - inner), strokeWidth = 2f)

    // Rayos dorados desde el centro hacia las esquinas
    val r = t * 0.42f
    for (i in 0 until 8) {
        val ang = i * 0.785398f
        val sx = cx + sin(ang.toDouble()).toFloat() * r
        val sy = cy + sin((ang + 1.5708f).toDouble()).toFloat() * r
        drawLine(
            color = Color(0xFFFFD700).copy(alpha = 0.55f),
            start = Offset(cx, cy),
            end = Offset(sx, sy),
            strokeWidth = if (i % 2 == 0) 2f else 1f
        )
    }

    // Medallón central con 3 anillos
    drawCircle(color = decoColor.darken(0.20f), radius = t * 0.14f, center = Offset(cx, cy))
    drawCircle(color = base.lighten(0.05f), radius = t * 0.11f, center = Offset(cx, cy))
    drawCircle(color = Color(0xFFFFD700), radius = t * 0.08f, center = Offset(cx, cy))
    drawCircle(color = Color(0xFFFFF59D), radius = t * 0.04f, center = Offset(cx, cy))

    // 4 azulejos decorativos en las esquinas (variados por deco)
    val cornerColors = listOf(
        Color(0xFF1565C0), Color(0xFFE65100), Color(0xFF6A1B9A), Color(0xFF2E7D32),
        Color(0xFFC62828), Color(0xFFFFB300)
    )
    val corners = listOf(
        Offset(x + t * 0.15f, y + t * 0.15f),
        Offset(x + t * 0.85f, y + t * 0.15f),
        Offset(x + t * 0.15f, y + t * 0.85f),
        Offset(x + t * 0.85f, y + t * 0.85f)
    )
    for ((i, p) in corners.withIndex()) {
        val color = cornerColors[(deco + i) % cornerColors.size]
        drawRect(color = color, topLeft = Offset(p.x - t * 0.05f, p.y - t * 0.05f), size = Size(t * 0.10f, t * 0.10f))
        drawRect(color = color.lighten(0.20f), topLeft = Offset(p.x - t * 0.04f, p.y - t * 0.04f), size = Size(t * 0.04f, t * 0.04f))
        drawRect(color = color.darken(0.20f), topLeft = Offset(p.x, p.y), size = Size(t * 0.04f, t * 0.04f))
    }
}

// =====================================================================
//                          WATER animada
// =====================================================================

private fun DrawScope.drawWater(x: Float, y: Float, t: Float, animPhase: Float, deco: Int, base: Color, decoColor: Color) {
    // Gradient de agua profunda
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(base.lighten(0.18f), base.lighten(0.05f), base, decoColor.darken(0.10f)),
            startY = y,
            endY = y + t
        ),
        topLeft = Offset(x, y),
        size = Size(t, t)
    )

    // Capas de cáusticos / shimmering
    for (i in 0 until 5) {
        val phase = (animPhase * 6.28f + i * 1.2f + deco).toDouble()
        val py = y + t * (0.15f + i * 0.18f) + sin(phase).toFloat() * t * 0.05f
        val shimmer = (sin((phase * 2).toDouble()).toFloat() * 0.3f + 0.7f)
        drawLine(
            color = Color(0xFFFFFFFF).copy(alpha = 0.35f * shimmer),
            start = Offset(x + t * 0.10f + sin((phase * 1.7).toDouble()).toFloat() * 4f, py),
            end = Offset(x + t * 0.85f + sin((phase * 2.1).toDouble()).toFloat() * 4f, py),
            strokeWidth = 1.5f
        )
    }

    // Ondas circulares más grandes (slow ripples)
    for (i in 0 until 2) {
        val rPhase = ((animPhase + i * 0.5f + deco * 0.05f) % 1f)
        val cx = x + t * (0.3f + i * 0.4f)
        val cy = y + t * (0.4f + i * 0.2f)
        drawCircle(
            color = Color(0xCCFFFFFF).copy(alpha = (1f - rPhase) * 0.4f),
            radius = t * (0.08f + rPhase * 0.18f),
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
        )
    }

    // Reflejo del sol con destello
    drawCircle(color = Color(0x55FFFFFF), radius = t * 0.10f,
        center = Offset(x + t * 0.7f, y + t * 0.30f))
    drawCircle(color = Color(0xCCFFFFFF), radius = t * 0.04f,
        center = Offset(x + t * 0.7f, y + t * 0.30f))

    // Foam horizontal sutil arriba (espuma)
    drawLine(
        color = Color(0xCCFFFFFF).copy(alpha = 0.3f),
        start = Offset(x, y + t * 0.05f),
        end = Offset(x + t, y + t * 0.05f),
        strokeWidth = 1f
    )
}

// =====================================================================
//                          SAND playa
// =====================================================================

private fun DrawScope.drawSand(x: Float, y: Float, t: Float, deco: Int, base: Color, decoColor: Color) {
    // Arena con gradiente diagonal (cálido)
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(base.lighten(0.10f), base, base.darken(0.08f)),
            start = Offset(x, y), end = Offset(x + t, y + t)
        ),
        topLeft = Offset(x, y), size = Size(t, t)
    )

    // Ondulaciones de duna (líneas curvas suaves usando arcos)
    drawArc(color = base.darken(0.10f).copy(alpha = 0.45f),
        startAngle = 0f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(x - t * 0.20f, y + t * 0.30f), size = Size(t * 0.80f, t * 0.20f),
        style = Stroke(width = 1.5f))
    drawArc(color = base.darken(0.08f).copy(alpha = 0.40f),
        startAngle = 0f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(x + t * 0.30f, y + t * 0.55f), size = Size(t * 0.70f, t * 0.18f),
        style = Stroke(width = 1.5f))

    // Granos brillantes (puntos micro)
    for (i in 0 until 12) {
        val sx = x + ((i * 17 + deco * 7) % 100) / 100f * t
        val sy = y + ((i * 13 + deco * 11) % 100) / 100f * t
        drawCircle(color = decoColor, radius = t / 80f, center = Offset(sx, sy))
        // Puntos más oscuros (mineral)
        if (i % 3 == 0) {
            drawCircle(color = Color(0xFF8D6E63), radius = t / 100f, center = Offset(sx + 1f, sy + 1f))
        }
    }

    // Concha si bits indican
    if ((deco and 0x4) != 0) {
        val sx = x + t * 0.65f; val sy = y + t * 0.55f
        // Concha en abanico
        drawArc(color = Color(0xFFFFE0B2), startAngle = 0f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(sx - t * 0.06f, sy - t * 0.04f), size = Size(t * 0.12f, t * 0.08f))
        drawArc(color = Color(0xFFE0B07A), startAngle = 0f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(sx - t * 0.06f, sy - t * 0.04f), size = Size(t * 0.12f, t * 0.08f),
            style = Stroke(width = 1.2f))
        // Líneas radiales
        for (k in 0 until 5) {
            val ang = 3.14f * (k / 4f)
            drawLine(color = Color(0xFFBF8B5E),
                start = Offset(sx, sy),
                end = Offset(sx + sin(ang.toDouble()).toFloat() * t * 0.05f,
                             sy - sin((ang + 1.57f).toDouble()).toFloat() * t * 0.04f),
                strokeWidth = 0.8f)
        }
    }

    // Estrella de mar si otros bits
    if ((deco and 0x8) != 0) {
        val cx = x + t * 0.30f; val cy = y + t * 0.30f
        for (k in 0 until 5) {
            val ang = k * 1.2566f
            val ex = cx + sin(ang.toDouble()).toFloat() * t * 0.08f
            val ey = cy + sin((ang + 1.57f).toDouble()).toFloat() * t * 0.08f
            drawLine(color = Color(0xFFFB8C00),
                start = Offset(cx, cy), end = Offset(ex, ey), strokeWidth = 4f)
        }
        drawCircle(color = Color(0xFFE65100), radius = t * 0.025f, center = Offset(cx, cy))
    }

    // Pisadas ocasionales (huellas)
    if ((deco and 0x10) != 0) {
        val fx = x + t * 0.50f
        // Dos huellas en diagonal
        drawArc(color = base.darken(0.20f).copy(alpha = 0.6f),
            startAngle = 0f, sweepAngle = 360f, useCenter = true,
            topLeft = Offset(fx - t * 0.030f, y + t * 0.30f), size = Size(t * 0.060f, t * 0.080f))
        drawArc(color = base.darken(0.20f).copy(alpha = 0.6f),
            startAngle = 0f, sweepAngle = 360f, useCenter = true,
            topLeft = Offset(fx + t * 0.030f, y + t * 0.50f), size = Size(t * 0.060f, t * 0.080f))
    }
}

// =====================================================================
//                          WALL ladrillo
// =====================================================================

private fun DrawScope.drawWall(x: Float, y: Float, t: Float, base: Color, deco: Color) {
    drawRect(color = base, topLeft = Offset(x, y), size = Size(t, t))
    val brickH = t / 4f
    for (i in 0 until 4) {
        val by = y + i * brickH
        // Línea horizontal de mortero
        drawRect(
            color = deco.lighten(0.15f),
            topLeft = Offset(x, by),
            size = Size(t, t / 32f)
        )
        // Líneas verticales alternadas
        val offset = if (i % 2 == 0) 0f else t / 2f
        drawRect(
            color = deco.lighten(0.15f),
            topLeft = Offset(x + offset, by),
            size = Size(t / 32f, brickH)
        )
        if (offset != 0f) {
            // Doble vertical para ladrillo lateral
            drawRect(
                color = deco.lighten(0.15f),
                topLeft = Offset(x + (offset + t / 2f) % t, by),
                size = Size(t / 32f, brickH)
            )
        }
    }
    // Sombra superior interior (bisel)
    drawRect(
        color = Color(0x33FFFFFF),
        topLeft = Offset(x, y),
        size = Size(t, 2f)
    )
    drawRect(
        color = Color(0x33000000),
        topLeft = Offset(x, y + t - 2f),
        size = Size(t, 2f)
    )
}

// =====================================================================
//                          INDOOR parqué
// =====================================================================

private fun DrawScope.drawIndoor(x: Float, y: Float, t: Float, base: Color, deco: Color) {
    drawRect(color = base, topLeft = Offset(x, y), size = Size(t, t))
    for (i in 0 until 4) {
        val ly = y + i * (t / 4f)
        drawRect(color = deco, topLeft = Offset(x, ly), size = Size(t, t / 64f))
        // Vetas de madera
        drawLine(
            color = deco.copy(alpha = 0.3f),
            start = Offset(x + t * 0.25f, ly + t / 16f),
            end = Offset(x + t * 0.7f, ly + t / 16f),
            strokeWidth = 1f
        )
    }
}

// =====================================================================
//                          DOOR puerta ornamentada
// =====================================================================

private fun DrawScope.drawDoor(x: Float, y: Float, t: Float, base: Color, deco: Color) {
    // Marco dorado alrededor
    drawRect(
        color = Color(0xFFFFD166),
        topLeft = Offset(x + t * 0.08f, y),
        size = Size(t * 0.84f, t)
    )
    // Cuerpo de la puerta
    drawRect(
        color = base,
        topLeft = Offset(x + t * 0.15f, y + t * 0.08f),
        size = Size(t * 0.7f, t * 0.92f)
    )
    // Veta superior decorativa
    drawRect(
        color = deco,
        topLeft = Offset(x + t * 0.15f, y + t * 0.08f),
        size = Size(t * 0.7f, t * 0.04f)
    )
    drawRect(
        color = deco,
        topLeft = Offset(x + t * 0.15f, y + t * 0.92f),
        size = Size(t * 0.7f, t * 0.04f)
    )
    // Detalle paneles
    drawRect(
        color = base.darken(0.15f),
        topLeft = Offset(x + t * 0.25f, y + t * 0.20f),
        size = Size(t * 0.5f, t * 0.25f)
    )
    drawRect(
        color = base.darken(0.15f),
        topLeft = Offset(x + t * 0.25f, y + t * 0.55f),
        size = Size(t * 0.5f, t * 0.25f)
    )
    // Manilla dorada con brillo
    drawCircle(
        color = Color(0xFFFFD700),
        radius = t * 0.06f,
        center = Offset(x + t * 0.72f, y + t * 0.5f)
    )
    drawCircle(
        color = Color(0xFFFFFFFF),
        radius = t * 0.02f,
        center = Offset(x + t * 0.7f, y + t * 0.48f)
    )
    // Indicador "Pulsa A" sutil arriba
    drawCircle(
        color = Color(0xCCFFEB3B),
        radius = t * 0.06f,
        center = Offset(x + t * 0.5f, y - t * 0.06f)
    )
}

// =====================================================================
//                          FOREST musgo + hojas
// =====================================================================

private fun DrawScope.drawForest(x: Float, y: Float, t: Float, deco: Int, base: Color, decoColor: Color) {
    // Suelo de bosque con gradiente irregular
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(base.lighten(0.05f), base, base.darken(0.10f)),
            start = Offset(x, y), end = Offset(x + t, y + t)
        ),
        topLeft = Offset(x, y), size = Size(t, t)
    )

    // Manchas grandes de musgo (3-4 capas)
    drawCircle(color = decoColor.darken(0.10f), radius = t * 0.22f, center = Offset(x + t * 0.30f, y + t * 0.30f))
    drawCircle(color = decoColor, radius = t * 0.18f, center = Offset(x + t * 0.30f, y + t * 0.30f))
    drawCircle(color = decoColor.lighten(0.10f), radius = t * 0.10f, center = Offset(x + t * 0.28f, y + t * 0.28f))

    drawCircle(color = decoColor.darken(0.10f), radius = t * 0.18f, center = Offset(x + t * 0.70f, y + t * 0.70f))
    drawCircle(color = decoColor, radius = t * 0.14f, center = Offset(x + t * 0.70f, y + t * 0.70f))

    // Pequeñas piedras
    drawCircle(color = Color(0xFF6D4C41), radius = t * 0.025f, center = Offset(x + t * 0.45f, y + t * 0.55f))
    drawCircle(color = Color(0xFF8D6E63), radius = t * 0.020f, center = Offset(x + t * 0.65f, y + t * 0.40f))
    drawCircle(color = Color(0xFF5D4037), radius = t * 0.015f, center = Offset(x + t * 0.18f, y + t * 0.78f))

    // Ramas pequeñas (líneas marrones)
    drawLine(color = Color(0xFF5D4037),
        start = Offset(x + t * 0.10f, y + t * 0.50f),
        end = Offset(x + t * 0.30f, y + t * 0.55f), strokeWidth = 1.5f)
    drawLine(color = Color(0xFF6D4C41),
        start = Offset(x + t * 0.65f, y + t * 0.20f),
        end = Offset(x + t * 0.80f, y + t * 0.30f), strokeWidth = 1.2f)

    // Setas/hongos pequeños según deco bits
    if ((deco and 0x4) != 0) {
        // Hongo rojo estilo amanita
        val mx = x + t * 0.60f; val my = y + t * 0.50f
        drawRect(color = Color(0xFFEEEEEE), topLeft = Offset(mx - t * 0.015f, my), size = Size(t * 0.030f, t * 0.06f))
        drawArc(color = Color(0xFFD32F2F), startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(mx - t * 0.05f, my - t * 0.03f), size = Size(t * 0.10f, t * 0.06f))
        // Puntos blancos del sombrero
        drawCircle(color = Color(0xFFFFFFFF), radius = t * 0.008f, center = Offset(mx - t * 0.020f, my - t * 0.005f))
        drawCircle(color = Color(0xFFFFFFFF), radius = t * 0.008f, center = Offset(mx + t * 0.015f, my - t * 0.010f))
        drawCircle(color = Color(0xFFFFFFFF), radius = t * 0.006f, center = Offset(mx, my + t * 0.005f))
    }
    if ((deco and 0x8) != 0) {
        // Hongo marrón pequeño
        val mx = x + t * 0.25f; val my = y + t * 0.60f
        drawRect(color = Color(0xFFD7CCC8), topLeft = Offset(mx - t * 0.012f, my), size = Size(t * 0.024f, t * 0.05f))
        drawArc(color = Color(0xFF6D4C41), startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(mx - t * 0.040f, my - t * 0.020f), size = Size(t * 0.080f, t * 0.040f))
    }

    // Hojas caídas con forma de gota (más realistas que solo círculos)
    val leafColors = listOf(Color(0xFFFB8C00), Color(0xFFE65100), Color(0xFFD32F2F),
                            Color(0xFFFFB300), Color(0xFF6D4C41))
    val numLeaves = 3 + ((deco shr 4) and 0x3)
    for (i in 0 until numLeaves) {
        val lx = x + ((i * 71 + deco * 17) % 90) / 100f * t + t * 0.05f
        val ly = y + ((i * 53 + deco * 13) % 90) / 100f * t + t * 0.05f
        val leafColor = leafColors[(deco + i) % leafColors.size]
        // Hoja oval (drawArc full)
        drawArc(color = leafColor, startAngle = 0f, sweepAngle = 360f, useCenter = true,
            topLeft = Offset(lx - t * 0.025f, ly - t * 0.015f), size = Size(t * 0.050f, t * 0.030f))
        // Vena central
        drawLine(color = leafColor.darken(0.30f),
            start = Offset(lx - t * 0.020f, ly), end = Offset(lx + t * 0.020f, ly),
            strokeWidth = 0.8f)
    }
}

// =====================================================================
//                          RAIL vías + balasto
// =====================================================================

private fun DrawScope.drawRail(x: Float, y: Float, t: Float, base: Color, deco: Color) {
    // Balasto (piedras)
    drawRect(color = base, topLeft = Offset(x, y), size = Size(t, t))
    for (i in 0 until 8) {
        val sx = x + ((i * 13) % 14) * (t / 16f)
        val sy = y + ((i * 7) % 14) * (t / 16f)
        drawCircle(color = base.lighten(0.15f), radius = t / 32f, center = Offset(sx, sy))
    }
    // Traviesas de madera
    for (i in 0 until 3) {
        val ty = y + t * (0.1f + i * 0.4f)
        drawRect(
            color = Color(0xFF6D4C41),
            topLeft = Offset(x + t * 0.05f, ty),
            size = Size(t * 0.9f, t * 0.08f)
        )
    }
    // Rieles (líneas verticales metálicas)
    drawRect(
        color = deco,
        topLeft = Offset(x + t * 0.22f, y),
        size = Size(t * 0.05f, t)
    )
    drawRect(
        color = deco,
        topLeft = Offset(x + t * 0.73f, y),
        size = Size(t * 0.05f, t)
    )
}

// =====================================================================
//                          BRIDGE tablones
// =====================================================================

private fun DrawScope.drawBridge(x: Float, y: Float, t: Float, base: Color, deco: Color) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(base.lighten(0.05f), base, base.darken(0.05f)),
            startY = y,
            endY = y + t
        ),
        topLeft = Offset(x, y),
        size = Size(t, t)
    )
    // Tablones
    for (i in 0 until 5) {
        val py = y + i * (t / 5f)
        drawRect(color = deco, topLeft = Offset(x, py), size = Size(t, t / 64f))
        // Clavos
        drawCircle(color = Color(0xFF263238), radius = t / 64f, center = Offset(x + t * 0.15f, py + t / 10f))
        drawCircle(color = Color(0xFF263238), radius = t / 64f, center = Offset(x + t * 0.85f, py + t / 10f))
    }
}
