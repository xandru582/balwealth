package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Preferencias de audio y háptica del usuario. Se persisten dentro
 * del [GameState] para que viajen junto al save.
 *
 *  - [musicEnabled] está deshabilitado por defecto: la música ambient
 *    procedural es relativamente cara y muchos jugadores prefieren
 *    silencio mientras juegan a un tycoon en segundo plano.
 */
@Serializable
data class AudioSettings(
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val masterVolume: Float = 0.7f,
    val musicEnabled: Boolean = false
)
