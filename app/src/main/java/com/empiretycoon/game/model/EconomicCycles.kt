package com.empiretycoon.game.model

import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * Fases del ciclo económico macro. Cada fase aplica un multiplicador global
 * de demanda y un multiplicador de volatilidad sobre los factores del mercado.
 *
 * El motor de [com.empiretycoon.game.engine.EconomicEngine] hace transitar el
 * estado entre fases mediante una cadena de Markov: cada día in-game se tira
 * un dado contra la matriz de probabilidades.
 */
enum class EconomicPhase(
    val displayName: String,
    val description: String,
    val emoji: String,
    /** Multiplicador aplicado a la demanda agregada (factor base 1.0). */
    val globalDemandMultiplier: Double,
    /** Multiplicador del ruido del random walk del mercado. */
    val volatilityMultiplier: Double,
    /** Duración aproximada en días in-game (referencia narrativa). */
    val daysApprox: Int
) {
    BOOM(
        displayName = "Auge",
        description = "Crecimiento rápido, optimismo, demanda alta y precios al alza.",
        emoji = "🚀",
        globalDemandMultiplier = 1.18,
        volatilityMultiplier = 1.10,
        daysApprox = 14
    ),
    RECOVERY(
        displayName = "Recuperación",
        description = "Salida de la recesión: demanda creciente, precios estabilizándose.",
        emoji = "🌱",
        globalDemandMultiplier = 1.06,
        volatilityMultiplier = 0.95,
        daysApprox = 10
    ),
    NORMAL(
        displayName = "Normal",
        description = "Mercado estable, sin grandes sobresaltos.",
        emoji = "⚖️",
        globalDemandMultiplier = 1.00,
        volatilityMultiplier = 1.00,
        daysApprox = 18
    ),
    RECESSION(
        displayName = "Recesión",
        description = "Demanda débil, contracción, precios a la baja.",
        emoji = "📉",
        globalDemandMultiplier = 0.86,
        volatilityMultiplier = 1.20,
        daysApprox = 9
    ),
    DEPRESSION(
        displayName = "Depresión",
        description = "Crisis profunda, demanda mínima y desconfianza generalizada.",
        emoji = "🔥",
        globalDemandMultiplier = 0.72,
        volatilityMultiplier = 1.35,
        daysApprox = 7
    ),
    BUBBLE(
        displayName = "Burbuja",
        description = "Especulación extrema: precios disparados sin sustento.",
        emoji = "🎈",
        globalDemandMultiplier = 1.32,
        volatilityMultiplier = 1.45,
        daysApprox = 6
    ),
    CRASH(
        displayName = "Crash",
        description = "Desplome súbito tras la burbuja, pánico vendedor.",
        emoji = "💥",
        globalDemandMultiplier = 0.65,
        volatilityMultiplier = 1.80,
        daysApprox = 4
    );

    companion object {
        fun fromOrdinalSafe(ord: Int): EconomicPhase =
            values().getOrNull(ord) ?: NORMAL
    }
}

/**
 * Estado serializable del ciclo económico: fase actual, días dentro de la
 * fase, días desde el último crash (para evitar crashes encadenados),
 * un índice de PIB nominal (acumula crecimiento), inflación anualizada
 * y la fuerza de la tendencia (-1..1).
 */
@Serializable
data class EconomicState(
    val currentPhase: EconomicPhase = EconomicPhase.NORMAL,
    val daysInPhase: Int = 0,
    val daysSinceLastCrash: Int = 60,
    val gdpIndex: Double = 100.0,
    val inflation: Double = 0.02,
    /** Fuerza de tendencia agregada del ciclo: -1 (recesivo) .. +1 (alcista). */
    val trendStrength: Double = 0.0,
    /** Historial reciente de fases (últimas 8) para la UI. */
    val recentPhases: List<EconomicPhase> = emptyList()
) {
    val phaseTrendIndicator: String
        get() = when {
            trendStrength > 0.45 -> "📈"
            trendStrength < -0.45 -> "📉"
            else -> "➡️"
        }
}

/**
 * Matriz de transición de Markov por fase (probabilidades sumando ~1.0).
 *
 * Cada [EconomicPhase] define a qué fases puede saltar el día siguiente.
 * Las probabilidades se aplican sólo cuando se intenta una transición:
 * los chequeos diarios penalizan las fases muy recientes para evitar
 * cambios bruscos cada día.
 */
object EconomicTransitions {

    /** Probabilidad por día de intentar una transición. Si no, se queda. */
    const val DAILY_TRANSITION_PROB: Double = 0.18

    /** Pesos relativos de transición desde cada fase. */
    val matrix: Map<EconomicPhase, Map<EconomicPhase, Double>> = mapOf(
        EconomicPhase.BOOM to mapOf(
            EconomicPhase.BOOM to 0.55,
            EconomicPhase.NORMAL to 0.20,
            EconomicPhase.BUBBLE to 0.18,
            EconomicPhase.RECESSION to 0.05,
            EconomicPhase.RECOVERY to 0.02
        ),
        EconomicPhase.RECOVERY to mapOf(
            EconomicPhase.NORMAL to 0.45,
            EconomicPhase.BOOM to 0.30,
            EconomicPhase.RECOVERY to 0.18,
            EconomicPhase.RECESSION to 0.07
        ),
        EconomicPhase.NORMAL to mapOf(
            EconomicPhase.NORMAL to 0.55,
            EconomicPhase.BOOM to 0.18,
            EconomicPhase.RECESSION to 0.15,
            EconomicPhase.RECOVERY to 0.08,
            EconomicPhase.BUBBLE to 0.04
        ),
        EconomicPhase.RECESSION to mapOf(
            EconomicPhase.RECESSION to 0.45,
            EconomicPhase.RECOVERY to 0.25,
            EconomicPhase.NORMAL to 0.15,
            EconomicPhase.DEPRESSION to 0.13,
            EconomicPhase.CRASH to 0.02
        ),
        EconomicPhase.DEPRESSION to mapOf(
            EconomicPhase.DEPRESSION to 0.40,
            EconomicPhase.RECOVERY to 0.40,
            EconomicPhase.RECESSION to 0.18,
            EconomicPhase.CRASH to 0.02
        ),
        EconomicPhase.BUBBLE to mapOf(
            EconomicPhase.BUBBLE to 0.40,
            EconomicPhase.CRASH to 0.32,
            EconomicPhase.BOOM to 0.18,
            EconomicPhase.RECESSION to 0.10
        ),
        EconomicPhase.CRASH to mapOf(
            EconomicPhase.RECESSION to 0.55,
            EconomicPhase.DEPRESSION to 0.30,
            EconomicPhase.CRASH to 0.10,
            EconomicPhase.RECOVERY to 0.05
        )
    )

    /** Toma una muestra aleatoria de la fila de transición de [from]. */
    fun sampleNext(from: EconomicPhase, rng: Random): EconomicPhase {
        val row = matrix[from] ?: return EconomicPhase.NORMAL
        val total = row.values.sum()
        var roll = rng.nextDouble() * total
        for ((phase, weight) in row) {
            roll -= weight
            if (roll <= 0.0) return phase
        }
        return row.keys.last()
    }
}
