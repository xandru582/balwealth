package com.empiretycoon.game.world.sprites

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.max

/**
 * DSL ligero pixel-art sobre [DrawScope] de Compose. Cada "pixel" es un
 * cuadrado de [pixelSize] píxeles reales, lo que permite escalar el sprite.
 */
class PixelCanvas(
    val drawScope: DrawScope,
    val pixelSize: Float,
    private val originX: Float = 0f,
    private val originY: Float = 0f
) {
    fun pixel(x: Int, y: Int, color: Color) {
        drawScope.drawRect(
            color = color,
            topLeft = Offset(originX + x * pixelSize, originY + y * pixelSize),
            size = Size(pixelSize, pixelSize)
        )
    }

    fun rect(x: Int, y: Int, w: Int, h: Int, color: Color) {
        if (w <= 0 || h <= 0) return
        drawScope.drawRect(
            color = color,
            topLeft = Offset(originX + x * pixelSize, originY + y * pixelSize),
            size = Size(w * pixelSize, h * pixelSize)
        )
    }

    fun outline(x: Int, y: Int, w: Int, h: Int, color: Color, thickness: Int = 1) {
        if (w <= 0 || h <= 0) return
        val t = thickness.coerceAtLeast(1)
        rect(x, y, w, t, color)
        rect(x, y + h - t, w, t, color)
        rect(x, y + t, t, max(0, h - 2 * t), color)
        rect(x + w - t, y + t, t, max(0, h - 2 * t), color)
    }

    fun fillCircle(cx: Int, cy: Int, r: Int, color: Color) {
        if (r <= 0) { pixel(cx, cy, color); return }
        val rSq = r * r
        for (dy in -r..r) for (dx in -r..r) {
            if (dx * dx + dy * dy <= rSq) pixel(cx + dx, cy + dy, color)
        }
    }

    fun translated(dx: Int, dy: Int): PixelCanvas =
        PixelCanvas(drawScope, pixelSize, originX + dx * pixelSize, originY + dy * pixelSize)

    companion object {
        /** Pseudo-random determinista [0,1). */
        fun seedRand(seed: Long, salt: Int): Float {
            var s = seed xor (salt.toLong() * 0x9E3779B97F4A7C15uL.toLong())
            s = s xor (s ushr 30)
            s *= 0xBF58476D1CE4E5B9uL.toLong()
            s = s xor (s ushr 27)
            s *= 0x94D049BB133111EBuL.toLong()
            s = s xor (s ushr 31)
            return ((s ushr 33).toFloat() / (1 shl 30).toFloat()).let {
                val v = it - it.toInt()
                if (v < 0f) v + 1f else v
            }
        }
    }
}

/** Oscurece un Color por la cantidad indicada (0..1). */
fun Color.darken(amount: Float = 0.2f): Color {
    val a = amount.coerceIn(0f, 1f)
    return Color(
        red = red * (1f - a),
        green = green * (1f - a),
        blue = blue * (1f - a),
        alpha = alpha
    )
}

/** Aclara un Color por la cantidad indicada (0..1). */
fun Color.lighten(amount: Float = 0.2f): Color {
    val a = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (1f - red) * a,
        green = green + (1f - green) * a,
        blue = blue + (1f - blue) * a,
        alpha = alpha
    )
}
