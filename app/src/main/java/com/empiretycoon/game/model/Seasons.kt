package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Event Seasons — temporadas y festivales (v17 — siguientes tandas de
 * MEJORAS.md). Cada temporada dura 7 días in-game y se rota cíclicamente:
 *
 *   1-7   día → HALLOWEEN  🎃 (truco o trato — bonos de eventos al pisar tile)
 *   8-14  día → CHRISTMAS  🎄 (regalos diarios + nevada en mundo)
 *   15-21 día → NEW_YEAR   🎆 (fuegos artificiales + bonus prestigio)
 *   22-28 día → SUMMER     ☀️ (vacaciones — más turistas/clientes)
 *   29-30 día → OFFSEASON  (descanso)
 *
 * Cada temporada activa unos modifiers GLOBALES que el resto de engines
 * pueden consultar via `state.seasons.activeModifiers`. No tocamos el
 * render ni el GameEngine de world directamente para evitar regresión —
 * solo expón información, los demás sistemas la usan opt-in.
 *
 * Recompensas: cada vez que el jugador "completa" una temporada (la vive
 * entera por primera vez) gana una reward única (badge + bonus permanente
 * pequeño). Los completados se persisten lifetime.
 */

@Serializable
enum class SeasonId(
    val displayName: String,
    val emoji: String,
    val tagline: String,
    /** Día del ciclo en que comienza (módulo SEASON_CYCLE_DAYS). */
    val startDay: Int,
    /** Días que dura. */
    val durationDays: Int
) {
    HALLOWEEN("Halloween", "🎃",
        "Trick or treat. Eventos al pisar tile multiplicados ×1.5.",
        startDay = 1, durationDays = 7),
    CHRISTMAS("Navidad", "🎄",
        "Regalos diarios y bonificación de ventas +12%.",
        startDay = 8, durationDays = 7),
    NEW_YEAR("Año Nuevo", "🎆",
        "Fin de año: bonus +25% al XP del jugador.",
        startDay = 15, durationDays = 7),
    SUMMER("Verano", "☀️",
        "Turistas: bonus +18% a renta de inmuebles y propinas.",
        startDay = 22, durationDays = 7),
    OFFSEASON("Temporada baja", "🌫️",
        "Días tranquilos. Sin modificadores activos.",
        startDay = 29, durationDays = 2)
}

/** Modificadores que una temporada activa puede aplicar a otros sistemas. */
@Serializable
data class SeasonModifiers(
    /** Multiplicador a recompensas de eventos al pisar tile. 1.0 = neutral. */
    val worldEventRewardMul: Double = 1.0,
    /** Multiplicador a precios de venta del mercado. */
    val marketSellMul: Double = 1.0,
    /** Multiplicador a XP que recibe el jugador por toda acción. */
    val playerXpMul: Double = 1.0,
    /** Multiplicador a renta diaria de inmuebles. */
    val realEstateRentMul: Double = 1.0,
    /** Si la temporada activa una nevada visual constante. */
    val snowfall: Boolean = false,
    /** Si la temporada activa fuegos artificiales nocturnos. */
    val fireworks: Boolean = false
) {
    companion object {
        val NEUTRAL = SeasonModifiers()
        val HALLOWEEN = SeasonModifiers(worldEventRewardMul = 1.5)
        val CHRISTMAS = SeasonModifiers(marketSellMul = 1.12, snowfall = true)
        val NEW_YEAR = SeasonModifiers(playerXpMul = 1.25, fireworks = true)
        val SUMMER = SeasonModifiers(realEstateRentMul = 1.18)
        val OFFSEASON = NEUTRAL
    }
}

/** Una recompensa única por completar una temporada. */
@Serializable
data class SeasonReward(
    val seasonId: SeasonId,
    val title: String,
    val description: String,
    val cashBonus: Double = 0.0,
    val xpBonus: Long = 0L
)

/** Estado del subsistema de temporadas. */
@Serializable
data class SeasonsState(
    /** Temporada activa AHORA (puede ser OFFSEASON). */
    val activeSeasonName: String = SeasonId.HALLOWEEN.name,
    /** Día del ciclo (1..SEASON_CYCLE_DAYS). */
    val cycleDay: Int = 1,
    /**
     * Cuántas veces hemos completado cada temporada (para perks de "lifetime
     * collector"). Se incrementa cuando termina la temporada habiendo
     * estado activa al menos 1 día.
     */
    val completedCount: Map<String, Int> = emptyMap(),
    /** Recompensas individuales obtenidas (1 por temporada lifetime). */
    val claimedRewards: List<SeasonReward> = emptyList(),
    /** Tick del último análisis. */
    val lastTickedDay: Int = -1
) {
    val activeSeason: SeasonId
        get() = runCatching { SeasonId.valueOf(activeSeasonName) }
            .getOrDefault(SeasonId.OFFSEASON)

    val activeModifiers: SeasonModifiers
        get() = when (activeSeason) {
            SeasonId.HALLOWEEN -> SeasonModifiers.HALLOWEEN
            SeasonId.CHRISTMAS -> SeasonModifiers.CHRISTMAS
            SeasonId.NEW_YEAR -> SeasonModifiers.NEW_YEAR
            SeasonId.SUMMER -> SeasonModifiers.SUMMER
            SeasonId.OFFSEASON -> SeasonModifiers.OFFSEASON
        }
}

object SeasonsCatalog {
    /** Duración total del ciclo: 7 + 7 + 7 + 7 + 2 = 30 días. */
    const val CYCLE_DAYS = 30

    /** Devuelve la temporada activa para un día del ciclo (1..30). */
    fun seasonForDay(cycleDay: Int): SeasonId {
        val d = ((cycleDay - 1) % CYCLE_DAYS) + 1
        return when {
            d in 1..7 -> SeasonId.HALLOWEEN
            d in 8..14 -> SeasonId.CHRISTMAS
            d in 15..21 -> SeasonId.NEW_YEAR
            d in 22..28 -> SeasonId.SUMMER
            else -> SeasonId.OFFSEASON
        }
    }

    /** Plantilla de recompensa por temporada. */
    fun rewardTemplate(season: SeasonId): SeasonReward = when (season) {
        SeasonId.HALLOWEEN -> SeasonReward(
            seasonId = season,
            title = "🎃 Cazafantasmas",
            description = "Has sobrevivido al Halloween. Bonus único de cash + XP.",
            cashBonus = 50_000.0,
            xpBonus = 200L
        )
        SeasonId.CHRISTMAS -> SeasonReward(
            seasonId = season,
            title = "🎄 Espíritu navideño",
            description = "Has pasado las fiestas. Bonus único.",
            cashBonus = 80_000.0,
            xpBonus = 250L
        )
        SeasonId.NEW_YEAR -> SeasonReward(
            seasonId = season,
            title = "🎆 Brindis del 1 de enero",
            description = "Año nuevo, vida nueva. Bonus de XP grande.",
            cashBonus = 30_000.0,
            xpBonus = 500L
        )
        SeasonId.SUMMER -> SeasonReward(
            seasonId = season,
            title = "☀️ Veraneante",
            description = "Has disfrutado del verano. Bonus de inmuebles.",
            cashBonus = 100_000.0,
            xpBonus = 150L
        )
        SeasonId.OFFSEASON -> SeasonReward(
            seasonId = season,
            title = "🌫️ Pausa creativa",
            description = "Reposo entre temporadas.",
            cashBonus = 0.0,
            xpBonus = 50L
        )
    }
}
