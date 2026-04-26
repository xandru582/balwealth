package com.empiretycoon.game.engine

import com.empiretycoon.game.model.GameNotification
import com.empiretycoon.game.model.GameState
import com.empiretycoon.game.model.NotificationKind
import com.empiretycoon.game.model.Rival
import com.empiretycoon.game.model.RivalRoster
import com.empiretycoon.game.model.RivalsState
import kotlin.random.Random

/**
 * Motor de rivales. Comprueba si el jugador supera el umbral de cash
 * de algún rival activo y lo marca como derrotado, otorgando recompensas.
 */
object RivalEngine {

    /** Si el roster está vacío, lo inicializa. */
    fun ensureInitialized(state: GameState): GameState {
        if (state.rivals.active.isNotEmpty() || state.rivals.defeated.isNotEmpty()) return state
        val fresh = RivalRoster.freshState()
        return state.copy(rivals = fresh)
    }

    /**
     * Comprueba la liquidez del jugador (cash empresa) frente a los rivales.
     * Si supera al rival actual lo marca como derrotado y aplica recompensas.
     * También recalcula el "currentChallenge" hacia el siguiente rival.
     */
    fun checkChallenges(state: GameState): GameState {
        if (state.rivals.active.isEmpty()) return state
        val playerCash = state.company.cash
        val active = state.rivals.active
        val justDefeated = active.filter { !it.defeated && playerCash >= it.cash }
        if (justDefeated.isEmpty()) return state.copy(
            rivals = state.rivals.copy(
                currentChallenge = pickNextChallenge(active.filter { !it.defeated })
            )
        )

        var s = state
        var newActive = active.toMutableList()
        var newDefeated = state.rivals.defeated.toMutableList()
        var totalCash = 0.0
        var totalXp = 0L
        var totalRep = 0
        for (r in justDefeated) {
            val flagged = r.copy(defeated = true)
            newActive.removeAll { it.id == r.id }
            newDefeated += flagged
            totalCash += r.rewardCash
            totalXp += r.rewardXp
            totalRep += r.rewardReputation
            s = notify(
                s, NotificationKind.SUCCESS,
                "Rival derrotado",
                "${r.portrait} ${r.name} ya no te hace sombra. " +
                    "+${"%,.0f".format(r.rewardCash)} €, +${r.rewardXp} XP."
            )
        }
        val company = s.company.copy(
            cash = s.company.cash + totalCash,
            reputation = (s.company.reputation + totalRep).coerceIn(0, 100)
        )
        val player = s.player.addXp(totalXp)
        val rivals = s.rivals.copy(
            active = newActive,
            defeated = newDefeated,
            currentChallenge = pickNextChallenge(newActive)
        )
        return s.copy(company = company, player = player, rivals = rivals)
    }

    /**
     * Devuelve una pulla aleatoria del rival actual del jugador. El llamante
     * la usa para mostrarla en notificaciones o sobre la UI.
     */
    fun rivalTrashTalk(state: GameState, rng: Random): String? {
        val talk = RivalRoster.trashTalkFor(state.rivals, rng) ?: return null
        return talk
    }

    /**
     * Genera una pulla y la guarda en RivalsState.lastTrashTalk para que la
     * UI la muestre. Devuelve el GameState actualizado.
     */
    fun pushTrashTalk(state: GameState, rng: Random): GameState {
        val talk = rivalTrashTalk(state, rng) ?: return state
        return state.copy(rivals = state.rivals.copy(lastTrashTalk = talk))
    }

    /** Limpia la última pulla del estado. */
    fun clearTrashTalk(state: GameState): GameState =
        state.copy(rivals = state.rivals.copy(lastTrashTalk = null))

    /** Calcula el porcentaje de progreso del jugador hacia un rival concreto. */
    fun progressFor(state: GameState, rival: Rival): Float {
        if (rival.cash <= 0.0) return 1f
        return (state.company.cash / rival.cash).toFloat().coerceIn(0f, 1f)
    }

    /** Devuelve el rival inmediatamente superior al jugador (próximo objetivo). */
    private fun pickNextChallenge(active: List<Rival>): String? {
        return active
            .filter { !it.defeated }
            .sortedBy { it.cash }
            .firstOrNull()
            ?.id
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
