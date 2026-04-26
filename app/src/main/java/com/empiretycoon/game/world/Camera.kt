package com.empiretycoon.game.world

/**
 * Cámara que sigue al avatar suavemente. Coordenadas en *baldosas*.
 */
data class Camera(
    val centerX: Float,
    val centerY: Float,
    val zoom: Float = 2.0f
) {
    fun follow(avatar: Avatar, lerp: Float = 0.18f): Camera {
        val newX = centerX + (avatar.x - centerX) * lerp
        val newY = centerY + (avatar.y - centerY) * lerp
        return copy(centerX = newX, centerY = newY)
    }
}
