package com.empiretycoon.game.engine

import com.empiretycoon.game.model.GameNotification
import com.empiretycoon.game.model.GameState
import com.empiretycoon.game.model.NotificationKind
import com.empiretycoon.game.model.PerkCatalog
import com.empiretycoon.game.model.PerksState
import com.empiretycoon.game.model.SkillEffect
import kotlin.random.Random

/**
 * Motor de perks. Sortea opciones cuando el jugador alcanza un nivel
 * múltiplo de 5 y aplica el efecto del perk elegido.
 */
object PerkEngine {

    /** Intervalo de niveles entre tiradas de perk. */
    const val LEVEL_INTERVAL = 5

    /**
     * Llamar cuando el jugador acaba de subir varios niveles. Si cruza un
     * múltiplo de 5 (sin elección pendiente ya), genera 3 cartas.
     */
    fun rollPerkChoice(state: GameState, rng: Random): GameState {
        if (state.perks.hasPending) return state
        // Solo abrimos elección si NO se ha abierto aún para este nivel.
        // Heurística: si el nivel actual es múltiplo de 5 y no hay pending, abrimos.
        if (state.player.level == 1) return state
        if (state.player.level % LEVEL_INTERVAL != 0) return state

        val ids = PerkCatalog.rollChoices(rng, count = 3)
        val perks = state.perks.copy(pendingChoice = ids)
        return state.copy(perks = perks).let {
            notify(it, NotificationKind.SUCCESS,
                "¡Subes a nivel ${state.player.level}!",
                "Elige un perk de los tres ofrecidos.")
        }
    }

    /**
     * Aplica el perk elegido. Limpia las opciones pendientes.
     */
    fun selectPerk(state: GameState, perkId: String): GameState {
        val pending = state.perks.pendingChoice ?: return state
        if (perkId !in pending) return state
        val perk = PerkCatalog.byId[perkId] ?: return state

        val newPerks = state.perks.copy(
            ownedPerks = state.perks.ownedPerks + perkId,
            pendingChoice = null
        )

        var s = state.copy(perks = newPerks)
        s = applyOneShotEffects(s, perk.effects)
        return notify(s, NotificationKind.SUCCESS,
            "Perk elegido",
            "${perk.emoji} ${perk.name} (${perk.rarity.displayName})")
    }

    /** Rechaza/cierra el diálogo (descarta la elección). Útil para tests. */
    fun discardPending(state: GameState): GameState =
        state.copy(perks = state.perks.copy(pendingChoice = null))

    // ---------------------------------- helpers

    private fun applyOneShotEffects(state: GameState, effects: List<SkillEffect>): GameState {
        var s = state
        for (e in effects) {
            when (e) {
                is SkillEffect.CashStartBonus -> {
                    s = s.copy(company = s.company.copy(cash = s.company.cash + e.cash))
                }
                is SkillEffect.MaxEnergyBonus -> {
                    val p = s.player.copy(
                        maxEnergy = s.player.maxEnergy + e.amount,
                        energy = s.player.energy + e.amount
                    )
                    s = s.copy(player = p)
                }
                else -> Unit
            }
        }
        return s
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
