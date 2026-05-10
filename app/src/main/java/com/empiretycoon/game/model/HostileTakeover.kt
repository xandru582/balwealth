package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Estado de las adquisiciones hostiles (Hostile Takeovers).
 *
 * El jugador puede comprar el 51% de un rival pagando un premium sobre su
 * cash. El rival puede tener una defensa que cambia el resultado:
 *  - POISON_PILL  → encarece la operación (recargo del 25%).
 *  - WHITE_KNIGHT → otro rival contraoferta y la operación falla, devolviendo el 80% del coste.
 *  - GOLDEN_PARACHUTE → la operación va, pero el rival escapa con un 30% del coste como bonus.
 *  - NONE         → adquisición limpia con bonus completo (no defense).
 *
 * Coste base = rival.cash × 0.51 × 1.20 (premium del 20%).
 *
 * Una vez completada (cualquier resolución), el rival se mueve a `defeated`
 * y deja de hacer sombra al jugador.
 */
@Serializable
enum class TakeoverDefense(val displayName: String, val emoji: String) {
    NONE("Sin defensa", "✅"),
    POISON_PILL("Poison Pill", "💊"),
    WHITE_KNIGHT("Caballero Blanco", "🛡"),
    GOLDEN_PARACHUTE("Paracaídas Dorado", "🪂")
}

@Serializable
enum class TakeoverOutcome {
    SUCCESS,
    FAILED_REFUND,
    SUCCESS_PARTIAL_LOSS
}

@Serializable
data class TakeoverRecord(
    val rivalId: String,
    val rivalName: String,
    val costPaid: Double,
    val defense: TakeoverDefense,
    val outcome: TakeoverOutcome,
    val atTick: Long
)

@Serializable
data class HostileTakeoverState(
    val history: List<TakeoverRecord> = emptyList(),
    val cooldownUntilTick: Long = -1L
) {
    fun isOnCooldown(tick: Long): Boolean = tick < cooldownUntilTick
    fun ticksLeftCooldown(tick: Long): Long = (cooldownUntilTick - tick).coerceAtLeast(0L)
}

object HostileTakeoverConstants {
    /** % de la empresa rival que se compra al lanzar la oferta hostil. */
    const val CONTROLLING_STAKE: Double = 0.51

    /** Premium sobre el cash del rival a pagar al lanzar la oferta. */
    const val OFFER_PREMIUM: Double = 1.20

    /** Recargo aplicado si el rival activa Poison Pill. */
    const val POISON_PILL_MARKUP: Double = 0.25

    /** % devuelto al jugador si White Knight bloquea la operación. */
    const val WHITE_KNIGHT_REFUND: Double = 0.80

    /** % del coste que el rival se lleva en Golden Parachute (la operación va). */
    const val GOLDEN_PARACHUTE_LOSS: Double = 0.30

    /** Ticks de cooldown tras una takeover (3 días in-game). */
    const val COOLDOWN_TICKS: Long = 1_440L * 3

    /** Reputación mínima para poder lanzar una takeover. */
    const val MIN_REPUTATION: Int = 60

    /** Coste base de una takeover sobre un rival concreto. */
    fun baseCost(rivalCash: Double): Double = rivalCash * CONTROLLING_STAKE * OFFER_PREMIUM
}
