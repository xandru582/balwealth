package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Arcade — colección de mini-juegos jugables dentro del juego con apuestas
 * en cash de empresa (v17 — siguientes tandas de MEJORAS.md).
 *
 * Filosofía:
 *  - Apuestas pequeñas-medias (50–5.000 €) que no rompen la economía.
 *  - Ratio de retorno calibrado por juego: el snake premia llegar a una
 *    longitud, el 2048 premia tiles altos, etc.
 *  - El historial y mejores marcas se guardan en GameState como cualquier
 *    otra estadística (@Serializable).
 */

/** Identificadores de mini-juegos. Algunos están como stub "próximamente". */
@Serializable
enum class ArcadeGameId(
    val displayName: String,
    val emoji: String,
    val available: Boolean,
    val description: String
) {
    SNAKE("Serpiente", "🐍", true,
        "Clásico arcade. Come comida, no choques con tu cola ni los muros. Apuesta y multiplica si llegas a 10 piezas."),
    GAME_2048("2048", "🔢", false,
        "Combina tiles iguales. Llega a 2048 para multiplicar tu apuesta x15. Próximamente."),
    BREAKOUT("Breakout", "🧱", false,
        "Rompe los ladrillos con la pelota. Próximamente."),
    PONG("Pong", "🏓", false,
        "Duelo contra la IA. El primero a 5 puntos gana. Próximamente."),
    TETRIS("Tetrix", "🟦", false,
        "Apila piezas y elimina líneas. Próximamente.")
}

/** Resultado de una partida individual. */
@Serializable
data class ArcadePlayResult(
    val game: ArcadeGameId,
    val bet: Double,
    val score: Int,
    val winnings: Double,
    /** True si winnings > bet. */
    val won: Boolean,
    val day: Int = 0
)

/** Estadísticas por juego. */
@Serializable
data class ArcadeGameStats(
    val game: ArcadeGameId,
    val highScore: Int = 0,
    val gamesPlayed: Int = 0,
    val totalBet: Double = 0.0,
    val totalWon: Double = 0.0,
    val biggestWin: Double = 0.0
)

/** Estado serializable del subsistema Arcade. */
@Serializable
data class ArcadeState(
    val unlocked: Boolean = false,
    /** Estadísticas por cada ArcadeGameId. */
    val stats: Map<String, ArcadeGameStats> = emptyMap(),
    /** Últimas 20 partidas. */
    val recentPlays: List<ArcadePlayResult> = emptyList(),
    /** Apuesta seleccionada actualmente en el hub (UI sticky). */
    val selectedBet: Double = 100.0,
    /** Lifetime stats. */
    val totalLifetimePlays: Int = 0,
    val totalLifetimeNet: Double = 0.0
) {
    fun statsFor(game: ArcadeGameId): ArcadeGameStats =
        stats[game.name] ?: ArcadeGameStats(game = game)
}

object ArcadeCatalog {

    /** Apuestas mínima y máxima permitidas en cualquier juego. */
    const val MIN_BET = 50.0
    const val MAX_BET = 5_000.0

    /**
     * Calcula recompensa de SNAKE según comida comida:
     *  - 0..3 → pierde apuesta (winnings = 0)
     *  - 4..9 → recupera proporcional (lineal: 4=0.4×, 9=0.9×)
     *  - 10 → 1.5× apuesta
     *  - 15 → 2.5× apuesta
     *  - 25 → 5.0× apuesta
     *  - 40+ → 10× apuesta (cap)
     */
    fun snakeWinnings(bet: Double, food: Int): Double {
        val multiplier = when {
            food <= 3 -> 0.0
            food in 4..9 -> food / 10.0
            food in 10..14 -> 1.5
            food in 15..24 -> 2.5
            food in 25..39 -> 5.0
            else -> 10.0
        }
        return bet * multiplier
    }
}
