package com.empiretycoon.game.world.sprites

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.empiretycoon.game.world.Facing
import kotlin.math.abs

/**
 * Dibuja un NPC procedural en (x, y) escalado por [scale]. La paleta y
 * proporciones se derivan de [seed] de forma determinista.
 */
fun DrawScope.drawNpc(
    seed: Long,
    x: Float,
    y: Float,
    scale: Float = 4f,
    walkPhase: Float = 0f,
    facing: Facing = Facing.DOWN
) {
    val pc = PixelCanvas(this, scale, x, y)

    val skin = npcSkinFor(seed)
    val skinDark = skin.darken(0.18f)
    val hair = npcHairFor(seed)
    val shirt = npcShirtFor(seed)
    val shirtDark = shirt.darken(0.25f)
    val pants = npcPantsFor(seed)
    val outline = Color(0xFF1A1F2A)

    val step = if (walkPhase < 0.5f) 0 else 1

    // Sombra
    pc.rect(3, 15, 10, 1, Color(0x55000000))

    // Piernas
    val legA = if (step == 0) 0 else 1
    val legB = if (step == 0) 1 else 0
    pc.rect(5, 12 + legA, 2, 3, pants)
    pc.rect(9, 12 + legB, 2, 3, pants)

    // Torso
    pc.rect(4, 8, 8, 4, shirt)
    pc.outline(4, 8, 8, 4, shirtDark)

    // Brazos
    pc.rect(3, 9, 1, 3, skin)
    pc.rect(12, 9, 1, 3, skin)

    // Cabeza
    pc.rect(5, 3, 6, 5, skin)
    pc.outline(5, 3, 6, 5, skinDark)

    // Pelo
    pc.rect(4, 2, 8, 2, hair)
    pc.outline(4, 2, 8, 2, hair.darken(0.2f))

    // Cara según facing
    when (facing) {
        Facing.DOWN -> {
            pc.pixel(6, 5, outline)
            pc.pixel(9, 5, outline)
            pc.rect(7, 7, 2, 1, skinDark)
        }
        Facing.UP -> { /* nuca */ }
        Facing.LEFT -> {
            pc.pixel(6, 5, outline)
            pc.rect(6, 7, 2, 1, skinDark)
        }
        Facing.RIGHT -> {
            pc.pixel(9, 5, outline)
            pc.rect(8, 7, 2, 1, skinDark)
        }
    }
}

private fun npcSkinFor(seed: Long): Color {
    val palette = listOf(
        Color(0xFFFAD5A5), Color(0xFFE0AC69), Color(0xFFC68642),
        Color(0xFF8D5524), Color(0xFFFFE4B5), Color(0xFFD2A679)
    )
    return palette[(abs(seed) % palette.size).toInt()]
}

private fun npcHairFor(seed: Long): Color {
    val palette = listOf(
        Color(0xFF3E2723), Color(0xFF000000), Color(0xFFFDD835),
        Color(0xFF5D4037), Color(0xFFD32F2F), Color(0xFF455A64)
    )
    return palette[(abs(seed shr 8) % palette.size).toInt()]
}

private fun npcShirtFor(seed: Long): Color {
    val palette = listOf(
        Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047),
        Color(0xFFFFB300), Color(0xFF8E24AA), Color(0xFF00ACC1),
        Color(0xFFE91E63), Color(0xFF546E7A)
    )
    return palette[(abs(seed shr 16) % palette.size).toInt()]
}

private fun npcPantsFor(seed: Long): Color {
    val palette = listOf(
        Color(0xFF263238), Color(0xFF3E2723), Color(0xFF1A237E),
        Color(0xFF212121), Color(0xFF4527A0)
    )
    return palette[(abs(seed shr 24) % palette.size).toInt()]
}
