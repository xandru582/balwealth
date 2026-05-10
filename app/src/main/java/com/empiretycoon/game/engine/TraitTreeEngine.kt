package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*

/**
 * Motor del árbol de talentos. Pure GameState -> GameState.
 *
 * Reglas:
 *  - Para desbloquear un trait, hay que tener desbloqueado el de tier
 *    inmediatamente anterior en su misma rama.
 *  - Cuesta `resilienceXp` (la moneda que emite DisasterEngine al
 *    superar desastres). Si no tienes suficiente, falla con notificación.
 *  - No hay refund: comprar un trait es definitivo.
 *
 * Otros engines pueden leer multiplicadores via
 * `state.traitTree.multiplierFor(type)`.
 */
object TraitTreeEngine {

    fun canUnlock(state: GameState, traitId: String): Pair<Boolean, String?> {
        val trait = TraitCatalog.byId(traitId)
            ?: return false to "Trait desconocido: $traitId"
        if (state.traitTree.isUnlocked(traitId)) return false to "Ya desbloqueado"
        val prev = TraitCatalog.previousOf(trait)
        if (prev != null && !state.traitTree.isUnlocked(prev)) {
            val prevTrait = TraitCatalog.byId(prev)
            return false to "Requiere primero: ${prevTrait?.displayName ?: prev}"
        }
        if (state.disasters.resilienceXp < trait.cost) {
            return false to "Necesitas ${trait.cost} Resilience XP (tienes ${state.disasters.resilienceXp})"
        }
        return true to null
    }

    fun unlock(state: GameState, traitId: String): GameState {
        val (ok, reason) = canUnlock(state, traitId)
        val trait = TraitCatalog.byId(traitId) ?: return state
        if (!ok) {
            return notify(state, NotificationKind.ERROR, "🔒 No disponible",
                reason ?: "No se puede desbloquear ahora.")
        }
        val newDisasters = state.disasters.copy(
            resilienceXp = state.disasters.resilienceXp - trait.cost
        )
        val newTree = state.traitTree.copy(
            unlockedIds = state.traitTree.unlockedIds + traitId
        )
        return notify(
            state.copy(disasters = newDisasters, traitTree = newTree),
            NotificationKind.SUCCESS,
            "${trait.branch.emoji} ${trait.displayName} desbloqueado",
            "${trait.description}. Coste: ${trait.cost} Resilience XP."
        )
    }

    private fun notify(state: GameState, kind: NotificationKind, title: String, msg: String): GameState {
        val n = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = kind,
            title = title,
            message = msg
        )
        return state.copy(notifications = (state.notifications + n).takeLast(40))
    }
}
