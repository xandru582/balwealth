package com.empiretycoon.game.world.sprites

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.empiretycoon.game.model.CarBodyType
import com.empiretycoon.game.model.CarModel
import com.empiretycoon.game.model.OwnedCar
import com.empiretycoon.game.world.Facing

/**
 * Renderiza un coche del jugador (mientras conduce) en (x,y) — ocupa más
 * espacio que el avatar para que se note. La forma cambia según body type
 * y los colores del modelo.
 */
fun DrawScope.drawPlayerCar(
    owned: OwnedCar,
    facing: Facing,
    x: Float,
    y: Float,
    tileSize: Float,
    animPhase: Float = 0f
) {
    val model = owned.model()
    val primary = Color((owned.customColor ?: model.primaryColor).toInt())
    val secondary = Color(model.secondaryColor.toInt())
    val accent = Color(model.accentColor.toInt())
    val verticalOrient = facing == Facing.UP || facing == Facing.DOWN

    when (model.body) {
        CarBodyType.HATCHBACK -> drawHatchback(x, y, tileSize, primary, secondary, accent, verticalOrient, facing, model, animPhase)
        CarBodyType.SEDAN -> drawSedan(x, y, tileSize, primary, secondary, accent, verticalOrient, facing, model)
        CarBodyType.SUV -> drawSuv(x, y, tileSize, primary, secondary, accent, verticalOrient, facing, model)
        CarBodyType.COUPE -> drawCoupe(x, y, tileSize, primary, secondary, accent, verticalOrient, facing, model)
        CarBodyType.CONVERTIBLE -> drawConvertible(x, y, tileSize, primary, secondary, accent, verticalOrient, facing, model)
        CarBodyType.SUPERCAR -> drawSupercar(x, y, tileSize, primary, secondary, accent, verticalOrient, facing, model, animPhase)
        CarBodyType.LIMO -> drawLimo(x, y, tileSize, primary, secondary, accent, verticalOrient, facing, model)
        CarBodyType.ELECTRIC_POD -> drawPod(x, y, tileSize, primary, secondary, accent, verticalOrient, facing, model, animPhase)
        CarBodyType.CLASSIC -> drawClassic(x, y, tileSize, primary, secondary, accent, verticalOrient, facing, model)
        CarBodyType.TRUCK -> drawTruck(x, y, tileSize, primary, secondary, accent, verticalOrient, facing, model)
    }
}

// =========================================================================
//                              HELPERS
// =========================================================================

private fun DrawScope.shadow(x: Float, y: Float, w: Float, h: Float) {
    drawRect(
        color = Color(0x66000000),
        topLeft = Offset(x + 3f, y + 5f),
        size = Size(w, h)
    )
}

private fun DrawScope.windows(x: Float, y: Float, w: Float, h: Float, vertical: Boolean) {
    val winColor = Color(0xCC81D4FA)
    val frameColor = Color(0xFF263238)
    if (vertical) {
        drawRect(color = winColor, topLeft = Offset(x + w * 0.18f, y + h * 0.18f), size = Size(w * 0.64f, h * 0.22f))
        drawRect(color = winColor, topLeft = Offset(x + w * 0.18f, y + h * 0.62f), size = Size(w * 0.64f, h * 0.22f))
        drawRect(color = frameColor, topLeft = Offset(x + w * 0.18f, y + h * 0.4f), size = Size(w * 0.64f, h * 0.04f))
    } else {
        drawRect(color = winColor, topLeft = Offset(x + w * 0.16f, y + h * 0.16f), size = Size(w * 0.24f, h * 0.6f))
        drawRect(color = winColor, topLeft = Offset(x + w * 0.6f, y + h * 0.16f), size = Size(w * 0.24f, h * 0.6f))
        drawRect(color = frameColor, topLeft = Offset(x + w * 0.4f, y + h * 0.16f), size = Size(w * 0.04f, h * 0.6f))
    }
}

private fun DrawScope.wheels(x: Float, y: Float, w: Float, h: Float, vertical: Boolean) {
    val tire = Color(0xFF000000)
    val rim = Color(0xFFB0BEC5)
    if (vertical) {
        drawCircle(color = tire, radius = h * 0.08f, center = Offset(x + w * 0.12f, y + h * 0.22f))
        drawCircle(color = tire, radius = h * 0.08f, center = Offset(x + w * 0.88f, y + h * 0.22f))
        drawCircle(color = tire, radius = h * 0.08f, center = Offset(x + w * 0.12f, y + h * 0.78f))
        drawCircle(color = tire, radius = h * 0.08f, center = Offset(x + w * 0.88f, y + h * 0.78f))
        drawCircle(color = rim, radius = h * 0.04f, center = Offset(x + w * 0.12f, y + h * 0.22f))
        drawCircle(color = rim, radius = h * 0.04f, center = Offset(x + w * 0.88f, y + h * 0.22f))
        drawCircle(color = rim, radius = h * 0.04f, center = Offset(x + w * 0.12f, y + h * 0.78f))
        drawCircle(color = rim, radius = h * 0.04f, center = Offset(x + w * 0.88f, y + h * 0.78f))
    } else {
        drawCircle(color = tire, radius = w * 0.07f, center = Offset(x + w * 0.22f, y + h * 0.88f))
        drawCircle(color = tire, radius = w * 0.07f, center = Offset(x + w * 0.78f, y + h * 0.88f))
        drawCircle(color = tire, radius = w * 0.07f, center = Offset(x + w * 0.22f, y + h * 0.12f))
        drawCircle(color = tire, radius = w * 0.07f, center = Offset(x + w * 0.78f, y + h * 0.12f))
        drawCircle(color = rim, radius = w * 0.035f, center = Offset(x + w * 0.22f, y + h * 0.88f))
        drawCircle(color = rim, radius = w * 0.035f, center = Offset(x + w * 0.78f, y + h * 0.88f))
        drawCircle(color = rim, radius = w * 0.035f, center = Offset(x + w * 0.22f, y + h * 0.12f))
        drawCircle(color = rim, radius = w * 0.035f, center = Offset(x + w * 0.78f, y + h * 0.12f))
    }
}

private fun DrawScope.headlights(
    x: Float, y: Float, w: Float, h: Float,
    facing: Facing, ledStyle: Boolean = false
) {
    val color = if (ledStyle) Color(0xFFE3F2FD) else Color(0xFFFFEB3B)
    val glow = if (ledStyle) Color(0x55BBDEFB) else Color(0x55FFEB3B)
    val sz = if (ledStyle) 5f else 4f
    when (facing) {
        Facing.RIGHT -> {
            drawRect(color = color, topLeft = Offset(x + w - sz, y + h * 0.2f), size = Size(sz, h * 0.15f))
            drawRect(color = color, topLeft = Offset(x + w - sz, y + h * 0.65f), size = Size(sz, h * 0.15f))
            drawRect(color = glow, topLeft = Offset(x + w - sz - 3f, y + h * 0.2f), size = Size(3f, h * 0.6f))
        }
        Facing.LEFT -> {
            drawRect(color = color, topLeft = Offset(x, y + h * 0.2f), size = Size(sz, h * 0.15f))
            drawRect(color = color, topLeft = Offset(x, y + h * 0.65f), size = Size(sz, h * 0.15f))
            drawRect(color = glow, topLeft = Offset(x + sz, y + h * 0.2f), size = Size(3f, h * 0.6f))
        }
        Facing.DOWN -> {
            drawRect(color = color, topLeft = Offset(x + w * 0.2f, y + h - sz), size = Size(w * 0.15f, sz))
            drawRect(color = color, topLeft = Offset(x + w * 0.65f, y + h - sz), size = Size(w * 0.15f, sz))
            drawRect(color = glow, topLeft = Offset(x + w * 0.2f, y + h - sz - 3f), size = Size(w * 0.6f, 3f))
        }
        Facing.UP -> {
            drawRect(color = color, topLeft = Offset(x + w * 0.2f, y), size = Size(w * 0.15f, sz))
            drawRect(color = color, topLeft = Offset(x + w * 0.65f, y), size = Size(w * 0.15f, sz))
            drawRect(color = glow, topLeft = Offset(x + w * 0.2f, y + sz), size = Size(w * 0.6f, 3f))
        }
    }
}

// =========================================================================
//                              BODIES
// =========================================================================

private fun DrawScope.bodyFor(model: CarModel, vertical: Boolean, tileSize: Float): Pair<Float, Float> {
    val baseW = if (vertical) tileSize * 0.8f else tileSize * 1.5f
    val baseH = if (vertical) tileSize * 1.5f else tileSize * 0.8f
    val scale = when (model.body) {
        CarBodyType.SUPERCAR -> 1.05f
        CarBodyType.LIMO -> 1.25f
        CarBodyType.SUV -> 1.1f
        CarBodyType.TRUCK -> 1.15f
        CarBodyType.ELECTRIC_POD -> 0.85f
        else -> 1f
    }
    return baseW * scale to baseH * scale
}

private fun DrawScope.drawHatchback(x: Float, y: Float, t: Float, p: Color, s: Color, a: Color, vertical: Boolean, facing: Facing, model: CarModel, animPhase: Float) {
    val (w, h) = bodyFor(model, vertical, t)
    val cx = x - w / 2f; val cy = y - h / 2f
    shadow(cx, cy, w, h)
    drawRect(
        brush = Brush.verticalGradient(listOf(p, p.darken(0.15f))),
        topLeft = Offset(cx, cy), size = Size(w, h)
    )
    drawRect(color = s, topLeft = Offset(cx, cy + h * 0.5f), size = Size(w, h * 0.05f))
    windows(cx, cy, w, h, vertical)
    wheels(cx, cy, w, h, vertical)
    headlights(cx, cy, w, h, facing, model.hasLEDLights)
}

private fun DrawScope.drawSedan(x: Float, y: Float, t: Float, p: Color, s: Color, a: Color, vertical: Boolean, facing: Facing, model: CarModel) {
    val (w, h) = bodyFor(model, vertical, t)
    val cx = x - w / 2f; val cy = y - h / 2f
    shadow(cx, cy, w, h)
    drawRect(
        brush = Brush.verticalGradient(listOf(p.lighten(0.1f), p, p.darken(0.15f))),
        topLeft = Offset(cx, cy), size = Size(w, h)
    )
    drawRect(color = s, topLeft = Offset(cx, cy + h * 0.55f), size = Size(w, h * 0.05f))
    windows(cx, cy, w, h, vertical)
    wheels(cx, cy, w, h, vertical)
    headlights(cx, cy, w, h, facing, model.hasLEDLights)
    drawRect(color = a, topLeft = Offset(cx, cy + h * 0.85f), size = Size(w, 2f))
}

private fun DrawScope.drawSuv(x: Float, y: Float, t: Float, p: Color, s: Color, a: Color, vertical: Boolean, facing: Facing, model: CarModel) {
    val (w, h) = bodyFor(model, vertical, t)
    val cx = x - w / 2f; val cy = y - h / 2f
    shadow(cx, cy, w, h)
    drawRect(
        brush = Brush.verticalGradient(listOf(p, p.darken(0.2f))),
        topLeft = Offset(cx, cy), size = Size(w, h)
    )
    drawRect(color = s, topLeft = Offset(cx, cy), size = Size(w, h * 0.04f))
    windows(cx, cy, w, h, vertical)
    wheels(cx, cy, w, h, vertical)
    headlights(cx, cy, w, h, facing, model.hasLEDLights)
    drawRect(color = a, topLeft = Offset(cx + w * 0.05f, cy + h * 0.08f), size = Size(w * 0.9f, 2f))
    drawRect(color = a, topLeft = Offset(cx + w * 0.05f, cy + h * 0.12f), size = Size(w * 0.9f, 2f))
}

private fun DrawScope.drawCoupe(x: Float, y: Float, t: Float, p: Color, s: Color, a: Color, vertical: Boolean, facing: Facing, model: CarModel) {
    val (w, h) = bodyFor(model, vertical, t)
    val cx = x - w / 2f; val cy = y - h / 2f
    shadow(cx, cy, w, h)
    drawRect(
        brush = Brush.verticalGradient(listOf(p.lighten(0.15f), p, p.darken(0.2f))),
        topLeft = Offset(cx, cy + h * 0.05f), size = Size(w, h * 0.9f)
    )
    drawRect(color = p.darken(0.1f), topLeft = Offset(cx + w * 0.15f, cy + h * 0.12f), size = Size(w * 0.7f, h * 0.35f))
    windows(cx, cy, w, h, vertical)
    wheels(cx, cy, w, h, vertical)
    headlights(cx, cy, w, h, facing, model.hasLEDLights)
    if (model.hasSpoiler) {
        drawRect(color = a, topLeft = Offset(cx + w * 0.1f, cy + h * 0.95f), size = Size(w * 0.8f, 4f))
        drawRect(color = a, topLeft = Offset(cx + w * 0.15f, cy + h * 0.92f), size = Size(4f, h * 0.05f))
        drawRect(color = a, topLeft = Offset(cx + w * 0.81f, cy + h * 0.92f), size = Size(4f, h * 0.05f))
    }
}

private fun DrawScope.drawConvertible(x: Float, y: Float, t: Float, p: Color, s: Color, a: Color, vertical: Boolean, facing: Facing, model: CarModel) {
    val (w, h) = bodyFor(model, vertical, t)
    val cx = x - w / 2f; val cy = y - h / 2f
    shadow(cx, cy, w, h)
    drawRect(
        brush = Brush.verticalGradient(listOf(p, p.darken(0.15f))),
        topLeft = Offset(cx, cy), size = Size(w, h)
    )
    drawRect(color = Color(0xCC81D4FA), topLeft = Offset(cx + w * 0.18f, cy + h * 0.18f), size = Size(w * 0.64f, h * 0.12f))
    drawRect(color = a, topLeft = Offset(cx + w * 0.2f, cy + h * 0.35f), size = Size(w * 0.6f, h * 0.3f))
    drawCircle(color = Color(0xFFE0AC69), radius = h * 0.04f, center = Offset(cx + w * 0.35f, cy + h * 0.5f))
    drawCircle(color = Color(0xFFE0AC69), radius = h * 0.04f, center = Offset(cx + w * 0.65f, cy + h * 0.5f))
    wheels(cx, cy, w, h, vertical)
    headlights(cx, cy, w, h, facing, model.hasLEDLights)
}

private fun DrawScope.drawSupercar(x: Float, y: Float, t: Float, p: Color, s: Color, a: Color, vertical: Boolean, facing: Facing, model: CarModel, animPhase: Float) {
    val (w, h) = bodyFor(model, vertical, t)
    val cx = x - w / 2f; val cy = y - h / 2f
    shadow(cx, cy, w, h)
    // Cuerpo bajo, ancho, agresivo
    drawRect(
        brush = Brush.verticalGradient(listOf(p.lighten(0.2f), p, p.darken(0.25f))),
        topLeft = Offset(cx, cy + h * 0.1f), size = Size(w, h * 0.85f)
    )
    // Cabina pequeña en el medio
    drawRect(color = Color(0xCC81D4FA), topLeft = Offset(cx + w * 0.25f, cy + h * 0.2f), size = Size(w * 0.5f, h * 0.3f))
    // Difusores y accents
    drawRect(color = a, topLeft = Offset(cx + w * 0.05f, cy + h * 0.7f), size = Size(w * 0.9f, 3f))
    drawRect(color = a, topLeft = Offset(cx + w * 0.05f, cy + h * 0.95f), size = Size(w * 0.9f, 4f))
    wheels(cx, cy, w, h, vertical)
    headlights(cx, cy, w, h, facing, true)  // siempre LED
    // Spoiler grande
    if (model.hasSpoiler) {
        drawRect(color = a, topLeft = Offset(cx + w * 0.05f, cy + h * 1.0f), size = Size(w * 0.9f, 5f))
        drawRect(color = a, topLeft = Offset(cx + w * 0.1f, cy + h * 0.95f), size = Size(5f, h * 0.07f))
        drawRect(color = a, topLeft = Offset(cx + w * 0.85f, cy + h * 0.95f), size = Size(5f, h * 0.07f))
    }
    // Llamas del escape (solo si super super)
    if (model.topSpeed > 10f) {
        val flame = (animPhase * 6.28f) % 1f
        drawCircle(color = Color(0xFFFF5722).copy(alpha = 1f - flame), radius = w * 0.04f * flame, center = Offset(cx + w * 0.2f, cy + h * 1.05f))
        drawCircle(color = Color(0xFFFFEB3B).copy(alpha = 1f - flame), radius = w * 0.03f * flame, center = Offset(cx + w * 0.8f, cy + h * 1.05f))
    }
}

private fun DrawScope.drawLimo(x: Float, y: Float, t: Float, p: Color, s: Color, a: Color, vertical: Boolean, facing: Facing, model: CarModel) {
    val (w, h) = bodyFor(model, vertical, t)
    val cx = x - w / 2f; val cy = y - h / 2f
    shadow(cx, cy, w, h)
    // Largo: extendemos h aún más
    val extraH = h * 1.15f
    drawRect(
        brush = Brush.verticalGradient(listOf(p, p.darken(0.15f))),
        topLeft = Offset(cx, cy), size = Size(w, extraH)
    )
    // Múltiples ventanillas
    if (vertical) {
        for (i in 0 until 4) {
            drawRect(
                color = Color(0xCC0F1724),  // tintadas
                topLeft = Offset(cx + w * 0.15f, cy + extraH * (0.15f + i * 0.18f)),
                size = Size(w * 0.7f, extraH * 0.13f)
            )
        }
    } else {
        for (i in 0 until 4) {
            drawRect(
                color = Color(0xCC0F1724),
                topLeft = Offset(cx + w * (0.15f + i * 0.18f), cy + extraH * 0.18f),
                size = Size(w * 0.13f, extraH * 0.5f)
            )
        }
    }
    wheels(cx, cy, w, extraH, vertical)
    headlights(cx, cy, w, extraH, facing, model.hasLEDLights)
    // Bandera dorada (limo de magnate)
    drawRect(color = a, topLeft = Offset(cx + w * 0.5f, cy - 6f), size = Size(2f, 8f))
    drawRect(color = a, topLeft = Offset(cx + w * 0.5f, cy - 6f), size = Size(8f, 4f))
}

private fun DrawScope.drawPod(x: Float, y: Float, t: Float, p: Color, s: Color, a: Color, vertical: Boolean, facing: Facing, model: CarModel, animPhase: Float) {
    val (w, h) = bodyFor(model, vertical, t)
    val cx = x - w / 2f; val cy = y - h / 2f
    shadow(cx, cy, w, h)
    // Forma redondeada (cápsula)
    drawArc(
        brush = Brush.verticalGradient(listOf(p.lighten(0.2f), p, p.darken(0.2f))),
        startAngle = 0f, sweepAngle = 360f, useCenter = true,
        topLeft = Offset(cx, cy), size = Size(w, h)
    )
    // Cristal grande
    drawArc(
        color = Color(0xCC81D4FA),
        startAngle = 180f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(cx + w * 0.15f, cy + h * 0.15f),
        size = Size(w * 0.7f, h * 0.5f),
        style = Stroke(width = h * 0.3f)
    )
    wheels(cx, cy, w, h, vertical)
    headlights(cx, cy, w, h, facing, true)
    // Pulso eléctrico animado
    val pulse = (animPhase * 6.28f) % 1f
    drawCircle(
        color = Color(0xFF03A9F4).copy(alpha = (1f - pulse) * 0.5f),
        radius = w * (0.3f + pulse * 0.3f),
        center = Offset(x, y)
    )
}

private fun DrawScope.drawClassic(x: Float, y: Float, t: Float, p: Color, s: Color, a: Color, vertical: Boolean, facing: Facing, model: CarModel) {
    val (w, h) = bodyFor(model, vertical, t)
    val cx = x - w / 2f; val cy = y - h / 2f
    shadow(cx, cy, w, h)
    drawRect(
        brush = Brush.verticalGradient(listOf(p.lighten(0.1f), p, p.darken(0.2f))),
        topLeft = Offset(cx, cy), size = Size(w, h)
    )
    // Curvas redondeadas en esquinas (simulado con arcos)
    drawArc(color = p, startAngle = 180f, sweepAngle = 90f, useCenter = true,
        topLeft = Offset(cx, cy), size = Size(w * 0.3f, h * 0.3f))
    drawArc(color = p, startAngle = 270f, sweepAngle = 90f, useCenter = true,
        topLeft = Offset(cx + w * 0.7f, cy), size = Size(w * 0.3f, h * 0.3f))
    // Cristal frontal grande (clásico)
    drawRect(color = Color(0xDDB3E5FC), topLeft = Offset(cx + w * 0.2f, cy + h * 0.2f), size = Size(w * 0.6f, h * 0.3f))
    // Cromados característicos
    drawRect(color = a, topLeft = Offset(cx + w * 0.05f, cy + h * 0.55f), size = Size(w * 0.9f, 4f))
    drawRect(color = a, topLeft = Offset(cx + w * 0.1f, cy + h * 0.7f), size = Size(w * 0.8f, 3f))
    // Logo central decorativo
    drawCircle(color = a, radius = w * 0.05f, center = Offset(cx + w * 0.5f, cy + h * 0.85f))
    wheels(cx, cy, w, h, vertical)
    headlights(cx, cy, w, h, facing, false)
}

private fun DrawScope.drawTruck(x: Float, y: Float, t: Float, p: Color, s: Color, a: Color, vertical: Boolean, facing: Facing, model: CarModel) {
    val (w, h) = bodyFor(model, vertical, t)
    val cx = x - w / 2f; val cy = y - h / 2f
    shadow(cx, cy, w, h)
    // Cabina + caja trasera
    if (vertical) {
        // Cabina arriba
        drawRect(color = p, topLeft = Offset(cx, cy), size = Size(w, h * 0.45f))
        // Caja
        drawRect(color = s, topLeft = Offset(cx + w * 0.05f, cy + h * 0.45f), size = Size(w * 0.9f, h * 0.5f))
        drawRect(color = a, topLeft = Offset(cx + w * 0.05f, cy + h * 0.45f), size = Size(w * 0.9f, 3f))
    } else {
        drawRect(color = p, topLeft = Offset(cx, cy), size = Size(w * 0.45f, h))
        drawRect(color = s, topLeft = Offset(cx + w * 0.45f, cy + h * 0.05f), size = Size(w * 0.55f, h * 0.9f))
        drawRect(color = a, topLeft = Offset(cx + w * 0.45f, cy + h * 0.05f), size = Size(3f, h * 0.9f))
    }
    windows(cx, cy, w, h * 0.45f, vertical)
    wheels(cx, cy, w, h, vertical)
    headlights(cx, cy, w, h, facing, model.hasLEDLights)
}
