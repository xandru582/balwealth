package com.empiretycoon.game.world.sprites

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.empiretycoon.game.world.Vehicle
import com.empiretycoon.game.world.Weather
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

fun DrawScope.drawVehicle(vehicle: Vehicle, screenX: Float, screenY: Float, tileSize: Float) {
    val color = Color(vehicle.vehicleKind().color.toInt())
    val verticalCar = abs(vehicle.dy) > abs(vehicle.dx)
    val w = if (verticalCar) tileSize * 0.5f else tileSize * 0.85f
    val h = if (verticalCar) tileSize * 0.85f else tileSize * 0.5f
    val cx = screenX - w / 2f
    val cy = screenY - h / 2f
    // Sombra
    drawRect(
        color = Color(0x55000000),
        topLeft = Offset(cx + 2f, cy + 4f),
        size = Size(w, h)
    )
    // Cuerpo
    drawRect(color = color, topLeft = Offset(cx, cy), size = Size(w, h))
    // Ventanas
    val winColor = Color(0xCC81D4FA)
    if (verticalCar) {
        drawRect(
            color = winColor,
            topLeft = Offset(cx + w * 0.1f, cy + h * 0.15f),
            size = Size(w * 0.8f, h * 0.25f)
        )
        drawRect(
            color = winColor,
            topLeft = Offset(cx + w * 0.1f, cy + h * 0.6f),
            size = Size(w * 0.8f, h * 0.25f)
        )
    } else {
        drawRect(
            color = winColor,
            topLeft = Offset(cx + w * 0.15f, cy + h * 0.1f),
            size = Size(w * 0.25f, h * 0.8f)
        )
        drawRect(
            color = winColor,
            topLeft = Offset(cx + w * 0.6f, cy + h * 0.1f),
            size = Size(w * 0.25f, h * 0.8f)
        )
    }
    // Faros (en la dirección del movimiento)
    val headlight = Color(0xFFFFEB3B)
    when {
        vehicle.dx > 0 -> drawRect(color = headlight, topLeft = Offset(cx + w - 3f, cy + h * 0.4f), size = Size(3f, h * 0.2f))
        vehicle.dx < 0 -> drawRect(color = headlight, topLeft = Offset(cx, cy + h * 0.4f), size = Size(3f, h * 0.2f))
        vehicle.dy > 0 -> drawRect(color = headlight, topLeft = Offset(cx + w * 0.4f, cy + h - 3f), size = Size(w * 0.2f, 3f))
        vehicle.dy < 0 -> drawRect(color = headlight, topLeft = Offset(cx + w * 0.4f, cy), size = Size(w * 0.2f, 3f))
    }
}

fun DrawScope.drawWeatherOverlay(weather: Weather, animPhase: Float, screenSize: Size, seed: Long = 12345L) {
    when (weather) {
        Weather.SUNNY -> { /* nada */ }
        Weather.CLOUDY -> {
            drawRect(
                color = Color(0x66000000),
                topLeft = Offset.Zero,
                size = screenSize
            )
        }
        Weather.RAINY, Weather.STORM -> {
            // Tinte oscuro
            drawRect(
                color = Color(0xAA0F1724).copy(alpha = if (weather == Weather.STORM) 0.6f else 0.4f),
                topLeft = Offset.Zero,
                size = screenSize
            )
            // Gotas de lluvia
            val count = weather.particleCount
            val rng = Random(seed)
            for (i in 0 until count) {
                val baseX = rng.nextFloat() * screenSize.width
                val baseY = rng.nextFloat() * screenSize.height
                val phase = (animPhase + i * 0.13f) % 1f
                val py = (baseY + phase * screenSize.height * 1.4f) % (screenSize.height + 40f) - 20f
                drawRect(
                    color = Color(weather.particleColor.toInt()),
                    topLeft = Offset(baseX - 8f * 0.3f, py),
                    size = Size(2f, 14f)
                )
            }
            // Relámpagos en tormenta
            if (weather == Weather.STORM && (animPhase % 0.13f) < 0.012f) {
                drawRect(
                    color = Color(0x55FFFFFF),
                    topLeft = Offset.Zero,
                    size = screenSize
                )
            }
        }
        Weather.FOG -> {
            // Niebla blanca semi-transparente con bandas
            for (i in 0 until 6) {
                val y = (animPhase * screenSize.height * 0.2f + i * screenSize.height / 6f) % screenSize.height
                drawRect(
                    color = Color(0x55FFFFFF),
                    topLeft = Offset(0f, y),
                    size = Size(screenSize.width, screenSize.height / 12f)
                )
            }
        }
        Weather.SNOW -> {
            drawRect(
                color = Color(0x55FFFFFF),
                topLeft = Offset.Zero,
                size = screenSize
            )
            val rng = Random(seed)
            for (i in 0 until weather.particleCount) {
                val baseX = rng.nextFloat() * screenSize.width
                val baseY = rng.nextFloat() * screenSize.height
                val phase = (animPhase * 0.5f + i * 0.07f) % 1f
                val py = (baseY + phase * screenSize.height) % screenSize.height
                val px = baseX + sin((phase * 6.28f).toDouble()).toFloat() * 12f
                drawCircle(
                    color = Color(0xFFFFFFFF),
                    radius = 3f,
                    center = Offset(px, py)
                )
            }
        }
    }
}

fun DrawScope.drawBird(x: Float, y: Float, animPhase: Float) {
    val flap = if (animPhase < 0.5f) -3f else 3f
    drawLine(
        color = Color(0xFF263238),
        start = Offset(x - 6f, y + flap),
        end = Offset(x, y),
        strokeWidth = 2f
    )
    drawLine(
        color = Color(0xFF263238),
        start = Offset(x, y),
        end = Offset(x + 6f, y + flap),
        strokeWidth = 2f
    )
}
