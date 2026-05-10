package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.math.max

/**
 * Motor de Arcade. Pure GameState -> GameState.
 *
 * Flujo de una partida:
 *   1. selectBet — el jugador ajusta su apuesta dentro del rango permitido.
 *   2. placeBet  — bloquea el cash de la apuesta (lo descuenta).
 *   3. (Composable juega y al terminar llama a)
 *      finishPlay(score, winnings) — añade winnings al cash, actualiza stats.
 *
 * Notas:
 *   - placeBet falla si no hay cash o el juego no está disponible.
 *   - finishPlay sigue funcionando aunque el jugador haya cerrado la app
 *     entre placeBet y el final: el bet ya está descontado, las winnings se
 *     entregan según el resultado reportado por el composable.
 *   - Si la app se mata sin reportar finishPlay, el bet queda cobrado pero
 *     no devuelto: el jugador sólo pierde lo apostado, equivalente a
 *     "perder la partida". Esto es intencional para no dar exploits.
 */
object ArcadeEngine {

    fun canUnlock(state: GameState): Boolean = state.player.level >= 2

    fun unlock(state: GameState): GameState {
        if (state.arcade.unlocked) return state
        if (!canUnlock(state)) {
            return notify(state, NotificationKind.ERROR, "🔒 Arcade bloqueado",
                "Necesitas nivel 2 para acceder al Arcade.")
        }
        return notify(
            state.copy(arcade = state.arcade.copy(unlocked = true)),
            NotificationKind.SUCCESS,
            "🎮 Arcade abierto",
            "Mini-juegos con apuestas. Empieza con la Serpiente — los demás llegan en futuras tandas."
        )
    }

    fun selectBet(state: GameState, bet: Double): GameState {
        val clamped = bet.coerceIn(ArcadeCatalog.MIN_BET, ArcadeCatalog.MAX_BET)
        return state.copy(arcade = state.arcade.copy(selectedBet = clamped))
    }

    /**
     * Bloquea el cash de la apuesta. Devuelve el state actualizado.
     * Si falla, el state vuelve con una notificación y SIN cobro.
     */
    fun placeBet(state: GameState, game: ArcadeGameId, bet: Double): GameState {
        if (!state.arcade.unlocked) return state
        if (!game.available) {
            return notify(state, NotificationKind.ERROR, "🚧 Próximamente",
                "${game.displayName} estará disponible en una futura tanda.")
        }
        val clamped = bet.coerceIn(ArcadeCatalog.MIN_BET, ArcadeCatalog.MAX_BET)
        if (state.company.cash < clamped) {
            return notify(state, NotificationKind.ERROR, "Sin fondos",
                "Necesitas ${"%,.0f".format(clamped)} € para apostar.")
        }
        return state.copy(
            company = state.company.copy(cash = state.company.cash - clamped),
            arcade = state.arcade.copy(selectedBet = clamped)
        )
    }

    /**
     * Reporta el resultado de la partida y entrega winnings (si las hay).
     * Actualiza stats y recentPlays.
     */
    fun finishPlay(
        state: GameState,
        game: ArcadeGameId,
        bet: Double,
        score: Int,
        winnings: Double
    ): GameState {
        if (!state.arcade.unlocked) return state
        val safeWinnings = max(0.0, winnings)
        val ar = state.arcade
        val won = safeWinnings > bet
        val net = safeWinnings - bet

        val stats = ar.statsFor(game)
        val newStats = stats.copy(
            highScore = max(stats.highScore, score),
            gamesPlayed = stats.gamesPlayed + 1,
            totalBet = stats.totalBet + bet,
            totalWon = stats.totalWon + safeWinnings,
            biggestWin = max(stats.biggestWin, safeWinnings)
        )
        val statsMap = ar.stats + (game.name to newStats)

        val play = ArcadePlayResult(
            game = game,
            bet = bet,
            score = score,
            winnings = safeWinnings,
            won = won,
            day = state.day
        )
        val recents = (ar.recentPlays + play).takeLast(20)

        val newCash = state.company.cash + safeWinnings
        val title = when {
            won && safeWinnings >= bet * 5.0 -> "🎰 ¡JACKPOT en ${game.displayName}!"
            won -> "🏆 Victoria en ${game.displayName}"
            safeWinnings > 0.0 -> "🤝 Te llevas algo de ${game.displayName}"
            else -> "💸 Mala racha en ${game.displayName}"
        }
        val msg = "Apuesta ${"%,.0f".format(bet)} € · " +
            "score $score · " +
            "ganas ${"%,.0f".format(safeWinnings)} € (neto ${"%+,.0f".format(net)} €)."
        val kind = if (won) NotificationKind.SUCCESS
            else if (safeWinnings > 0.0) NotificationKind.WARNING
            else NotificationKind.ERROR

        return notify(
            state.copy(
                company = state.company.copy(cash = newCash),
                arcade = ar.copy(
                    stats = statsMap,
                    recentPlays = recents,
                    totalLifetimePlays = ar.totalLifetimePlays + 1,
                    totalLifetimeNet = ar.totalLifetimeNet + net
                )
            ),
            kind, title, msg
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
