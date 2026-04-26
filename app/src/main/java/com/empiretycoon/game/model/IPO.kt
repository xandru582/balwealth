package com.empiretycoon.game.model

import kotlinx.serialization.Serializable
import kotlin.math.max

/**
 * Salida a bolsa (IPO) de la propia compañía del jugador.
 * Fases: LOCKED -> PROSPECTUS -> ROADSHOW -> LISTED.
 */
enum class IPOPhase(val displayName: String, val emoji: String) {
    LOCKED("Bloqueada", "🔒"),
    PROSPECTUS("Folleto", "📄"),
    ROADSHOW("Roadshow", "🎤"),
    LISTED("Cotizando", "📈")
}

/**
 * Requisitos para iniciar el proceso de IPO.
 * Documentados en la UI para guiar al jugador.
 */
object IPOConstraints {
    const val MIN_CASH: Double = 5_000_000.0
    const val MIN_REPUTATION: Int = 70
    const val MIN_LEVEL: Int = 15

    /** Ticks que dura el roadshow (3 días in-game = 4.320 ticks). */
    const val ROADSHOW_TICKS: Long = 1_440L * 3

    /** Ticks de revisión del folleto antes de poder hacer roadshow. */
    const val PROSPECTUS_REVIEW_TICKS: Long = 1_440L * 1

    /** % de la empresa que sale al mercado en el primer flotador. */
    const val PRIMARY_OFFERING_FLOAT_PCT: Double = 0.35

    /** Total de acciones emitidas en el listing inicial. */
    const val INITIAL_SHARES_OUTSTANDING: Long = 1_000_000L

    fun canFileProspectus(state: GameState): Boolean {
        return state.company.cash >= MIN_CASH &&
            state.company.reputation >= MIN_REPUTATION &&
            state.company.level >= MIN_LEVEL
    }
}

/**
 * Acción cotizada de la compañía del jugador. Distinta de Stock (mercado externo).
 * `sharesOwnedByPlayer` representa la fracción que el dueño retiene tras el IPO.
 */
@Serializable
data class CompanyStock(
    val ticker: String,
    val sharesOutstanding: Long,
    val sharesPublic: Long,
    val sharesOwnedByPlayer: Long,
    val currentPrice: Double,
    val ipoPrice: Double,
    val listedAtTick: Long,
    val dividendYield: Double = 0.0,
    val volatility: Double = 0.05,
    val history: List<Double> = emptyList(),
    val totalDividendsPaid: Double = 0.0,
    val splitsApplied: Int = 0
) {
    val marketCap: Double get() = currentPrice * sharesOutstanding
    val playerStakeValue: Double get() = currentPrice * sharesOwnedByPlayer
    val playerStakePct: Double get() =
        if (sharesOutstanding == 0L) 0.0
        else sharesOwnedByPlayer.toDouble() / sharesOutstanding.toDouble()

    fun annualDividendPerShare(): Double = currentPrice * dividendYield

    fun nextHistory(maxPoints: Int = 60, value: Double): List<Double> =
        (history + value).takeLast(maxPoints)
}

@Serializable
data class IPOState(
    val phase: IPOPhase = IPOPhase.LOCKED,
    val prospectusFiledAt: Long = -1L,
    val roadshowStartedAt: Long = -1L,
    val projectedValuation: Double = 0.0,
    val listed: CompanyStock? = null,
    /** historial de eventos accionariales para mostrar en UI */
    val splitHistory: List<StockSplit> = emptyList(),
    val dividendHistory: List<Dividend> = emptyList()
) {
    val isListed: Boolean get() = phase == IPOPhase.LISTED && listed != null

    /** Valoración estimada en función del estado de la empresa. */
    companion object {
        fun estimateValuation(state: GameState): Double {
            // Mezcla cash, reputación, edificios y nivel
            val cashPart = state.company.cash * 1.5
            val realEstatePart = state.realEstate.totalValue
            val reputationMult = 1.0 + (state.company.reputation - 50).coerceAtLeast(0) / 50.0
            val levelMult = 1.0 + state.company.level * 0.05
            val buildingsValue = state.company.buildings.sumOf { b ->
                b.type.baseCost * b.level.toDouble() * 1.6
            }
            val raw = (cashPart + realEstatePart + buildingsValue) *
                reputationMult * levelMult
            return max(IPOConstraints.MIN_CASH * 2.0, raw)
        }

        fun computeIpoPrice(valuation: Double): Double {
            // ipoPrice * INITIAL_SHARES_OUTSTANDING ~= valuation
            return (valuation / IPOConstraints.INITIAL_SHARES_OUTSTANDING)
                .coerceAtLeast(1.0)
        }
    }
}
