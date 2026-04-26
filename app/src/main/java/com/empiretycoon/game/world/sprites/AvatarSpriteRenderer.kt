package com.empiretycoon.game.world.sprites

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.empiretycoon.game.world.AvatarLook
import com.empiretycoon.game.world.Facing

/**
 * Dibuja el avatar 16x16 escalado según [scale]. Animación 2 frames.
 */
fun DrawScope.drawAvatar(
    look: AvatarLook,
    facing: Facing,
    walkPhase: Float,
    x: Float,
    y: Float,
    scale: Float = 4f
) {
    val pc = PixelCanvas(this, scale, x, y)
    val step = if (walkPhase < 0.5f) 0 else 1

    val skin = Color(look.skinTone)
    val skinDark = skin.darken(0.18f)
    val hair = Color(look.hairColor)
    val hairDark = hair.darken(0.25f)
    val shirt = Color(look.shirtColor)
    val shirtDark = shirt.darken(0.25f)
    val pants = Color(look.pantsColor)
    val pantsDark = pants.darken(0.25f)
    val outline = Color(0xFF1A1F2A)
    val white = Color(0xFFFFFFFF)
    val pupil = Color(0xFF111111)

    // Sombra
    pc.rect(3, 15, 10, 1, Color(0x55000000))

    // Piernas (dos frames de caminata)
    val legA = if (step == 0) 0 else 1
    val legB = if (step == 0) 1 else 0
    pc.rect(5, 12 + legA, 2, 3, pants)
    pc.rect(9, 12 + legB, 2, 3, pants)
    pc.outline(5, 12 + legA, 2, 3, pantsDark)
    pc.outline(9, 12 + legB, 2, 3, pantsDark)

    // Torso (camiseta)
    pc.rect(4, 8, 8, 4, shirt)
    pc.outline(4, 8, 8, 4, shirtDark)
    // Brazos
    pc.rect(3, 9, 1, 3, skin)
    pc.rect(12, 9, 1, 3, skin)

    // Cabeza
    pc.rect(5, 3, 6, 5, skin)
    pc.outline(5, 3, 6, 5, skinDark)

    // Pelo según facing
    when (facing) {
        Facing.DOWN -> {
            pc.rect(4, 2, 8, 2, hair)
            pc.rect(5, 4, 1, 1, hair)
            pc.rect(10, 4, 1, 1, hair)
        }
        Facing.UP -> {
            pc.rect(4, 2, 8, 4, hair)
        }
        Facing.LEFT -> {
            pc.rect(4, 2, 8, 3, hair)
            pc.rect(4, 5, 1, 2, hair)
        }
        Facing.RIGHT -> {
            pc.rect(4, 2, 8, 3, hair)
            pc.rect(11, 5, 1, 2, hair)
        }
    }
    pc.outline(4, 2, 8, 2, hairDark)

    // Cara según dirección
    when (facing) {
        Facing.DOWN -> {
            // Ojos
            pc.rect(6, 5, 1, 1, white)
            pc.rect(9, 5, 1, 1, white)
            pc.rect(6, 5, 1, 1, pupil)
            pc.rect(9, 5, 1, 1, pupil)
            // Boca
            pc.rect(7, 7, 2, 1, skinDark)
        }
        Facing.UP -> {
            // Solo nuca, sin cara
        }
        Facing.LEFT -> {
            pc.rect(6, 5, 1, 1, pupil)
            pc.rect(6, 7, 2, 1, skinDark)
        }
        Facing.RIGHT -> {
            pc.rect(9, 5, 1, 1, pupil)
            pc.rect(8, 7, 2, 1, skinDark)
        }
    }

    // Accesorios
    when (look.accessory) {
        0 -> { // gorra
            pc.rect(4, 1, 8, 2, hairDark)
            pc.rect(11, 2, 2, 1, hairDark)
        }
        1 -> { // gafas
            pc.rect(5, 5, 2, 1, outline)
            pc.rect(9, 5, 2, 1, outline)
            pc.pixel(7, 5, outline)
            pc.pixel(8, 5, outline)
        }
        3 -> { // chistera
            pc.rect(4, 0, 8, 1, outline)
            pc.rect(5, 1, 6, 1, outline)
        }
    }
}
