package com.empiretycoon.game.world

import kotlinx.serialization.Serializable

/**
 * Estado visual de "karma echo": cuán saturada está la ciudad, cuánta basura,
 * niños jugando, flores, etc. Se deriva continuamente del karma del jugador.
 */
@Serializable
data class KarmaEchoState(
    val saturation: Float = 1f,
    val sunlight: Float = 1f,
    val litterDensity: Float = 0f,
    val flowerDensity: Float = 0.5f,
    val tone: String = KarmaTone.NEUTRAL.name
)

enum class KarmaTone(val displayName: String, val emoji: String) {
    TYRANT("Tiránico", "💀"),
    COLD("Frío", "❄️"),
    NEUTRAL("Neutral", "⚖️"),
    KIND("Amable", "🌿"),
    SAINT("Santo", "👼")
}

object KarmaEchoEngine {
    fun toneFor(karma: Int): KarmaTone = when {
        karma <= -60 -> KarmaTone.TYRANT
        karma <= -20 -> KarmaTone.COLD
        karma >= 60 -> KarmaTone.SAINT
        karma >= 20 -> KarmaTone.KIND
        else -> KarmaTone.NEUTRAL
    }

    /** Smoothly drives echo toward target derived from karma. */
    fun tick(prev: KarmaEchoState, karma: Int): KarmaEchoState {
        val k = karma.coerceIn(-100, 100).toFloat() / 100f  // -1..1
        val targetSaturation = 1f + k * 0.3f      // 0.7 a 1.3
        val targetSunlight = 1f + k * 0.2f        // 0.8 a 1.2
        val targetLitter = (-k * 0.9f).coerceIn(0f, 1f)
        val targetFlowers = (0.5f + k * 0.5f).coerceIn(0f, 1f)
        val tone = toneFor(karma)
        val lerp = 0.05f
        return prev.copy(
            saturation = prev.saturation + (targetSaturation - prev.saturation) * lerp,
            sunlight = prev.sunlight + (targetSunlight - prev.sunlight) * lerp,
            litterDensity = prev.litterDensity + (targetLitter - prev.litterDensity) * lerp,
            flowerDensity = prev.flowerDensity + (targetFlowers - prev.flowerDensity) * lerp,
            tone = tone.name
        )
    }
}
