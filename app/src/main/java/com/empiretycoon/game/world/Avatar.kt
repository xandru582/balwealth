package com.empiretycoon.game.world

import kotlinx.serialization.Serializable

/** Dirección a la que mira el avatar (define qué baldosa "está delante"). */
enum class Facing { UP, DOWN, LEFT, RIGHT }

/**
 * Apariencia personalizable del avatar. Los enteros son valores ARGB
 * (0xAARRGGBB) listos para `Color(...)` en Compose. `hairStyle` y `accessory`
 * son índices que el renderer mapea a sprites/formas.
 */
@Serializable
data class AvatarLook(
    val skinTone: Int = 0xFFE0AC69.toInt(),
    val hairColor: Int = 0xFF3E2723.toInt(),
    val shirtColor: Int = 0xFF1E88E5.toInt(),
    val pantsColor: Int = 0xFF263238.toInt(),
    val hairStyle: Int = 0,   // 0..5
    val accessory: Int = 2    // 0=hat, 1=glasses, 2=none, 3=both
)

/**
 * Posición y estado de animación del avatar.
 *
 * - [x] / [y] están en *unidades de baldosa* (no píxeles); el renderer los
 *   multiplica por el tamaño en píxeles de la baldosa.
 * - [walkPhase] avanza linealmente con el movimiento y se enrolla en [0, 1)
 *   para impulsar el ciclo de pasos del sprite.
 */
@Serializable
data class Avatar(
    val x: Float,
    val y: Float,
    val facing: Facing = Facing.DOWN,
    val walking: Boolean = false,
    val walkPhase: Float = 0f,
    val look: AvatarLook = AvatarLook()
) {
    /** Centro entero de la baldosa que el avatar ocupa actualmente. */
    val tileX: Int get() = x.toInt()
    val tileY: Int get() = y.toInt()

    /** Coords enteras de la baldosa que el avatar tiene "delante". */
    fun frontTile(): Pair<Int, Int> = when (facing) {
        Facing.UP    -> tileX to (tileY - 1)
        Facing.DOWN  -> tileX to (tileY + 1)
        Facing.LEFT  -> (tileX - 1) to tileY
        Facing.RIGHT -> (tileX + 1) to tileY
    }
}
