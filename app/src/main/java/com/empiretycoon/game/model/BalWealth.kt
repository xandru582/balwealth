package com.empiretycoon.game.model

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * **BalWealth Index** — la mecánica central y única del juego.
 *
 * Mide el equilibrio entre 4 ejes:
 *  - **W (Wealth)**: dinero acumulado por la empresa.
 *  - **E (Employee Wellbeing)**: lealtad media de los empleados.
 *  - **C (Community)**: cómo afectas a la ciudad (karma + reputación).
 *  - **M (Mind)**: tu propia felicidad y energía.
 *
 * El INDEX es un número 0..100 que penaliza desequilibrio: ser MUY rico
 * pero con la comunidad/empleados destrozados baja el índice. Ser
 * equilibrado en los 4 ejes lo sube. Es DIFÍCIL de subir y caro de
 * mantener — es el verdadero objetivo del juego.
 *
 * Estado del jugador en este sistema:
 *  - **TYRANT_BUBBLE**: muy rico, todo lo demás bajo. Riesgo: revolución.
 *  - **MARTYR**: muy ético pero pobre y agotado. Riesgo: quiebra.
 *  - **BURNED_OUT**: equilibrio social/wealth, pero salud mental rota.
 *  - **HARMONIC**: índice >= 80 — desbloquea recompensas únicas.
 *  - **GROWING**: estado normal, índice 40-80.
 *  - **STRUGGLING**: índice <= 40, cosas se rompen.
 */
@Serializable
data class BalWealthState(
    val index: Float = 50f,                  // 0..100
    val wealthScore: Float = 50f,
    val employeeScore: Float = 50f,
    val communityScore: Float = 50f,
    val mindScore: Float = 50f,
    val tier: String = BalTier.GROWING.name,
    val highestEverIndex: Float = 50f,
    val daysAtHarmonic: Int = 0,
    val daysAtTyrantBubble: Int = 0,
    val lastEventTick: Long = 0L
)

enum class BalTier(val displayName: String, val emoji: String, val description: String) {
    HARMONIC("Armónico", "🌈", "Equilibrio perfecto entre dinero, gente y mente."),
    GROWING("En crecimiento", "🌱", "Vas bien. Sigue construyendo con cabeza."),
    STRUGGLING("Con dificultades", "⚠️", "Algo se está rompiendo. Reacciona."),
    TYRANT_BUBBLE("Burbuja del tirano", "💀", "Eres rico pero todo a tu alrededor se desmorona."),
    MARTYR("Mártir", "🕊️", "Bondad sin caja. Tus negocios se hunden."),
    BURNED_OUT("Quemado", "🥀", "Tu mente colapsa. No hay riqueza que arregle eso.")
}

/** Funciones puras para calcular BalWealth a partir del [GameState]. */
object BalWealth {

    /**
     * Score 0..100 a partir de un valor "más es mejor" con escala log-suave.
     */
    private fun normLog(value: Double, mid: Double): Float {
        val v = max(0.0, value)
        val score = 50.0 + 25.0 * ln((v + 1.0) / (mid + 1.0))
        return score.coerceIn(0.0, 100.0).toFloat()
    }

    fun computeWealth(state: GameState): Float {
        // 50 ≈ 50.000 €. 1M ≈ 75. 100M ≈ 100.
        return normLog(state.company.cash, 50_000.0)
    }

    fun computeEmployees(state: GameState): Float {
        val emps = state.company.employees
        if (emps.isEmpty()) return 30f  // ningún equipo es señal mediocre
        val avgLoyalty = emps.sumOf { it.loyalty } / emps.size
        return (avgLoyalty * 100f).toFloat().coerceIn(0f, 100f)
    }

    fun computeCommunity(state: GameState): Float {
        // Reputación pesa 60%, karma del storyline 40%
        val rep = state.company.reputation.toFloat().coerceIn(0f, 100f)
        val karma = ((state.storyline.karma + 100f) / 2f).coerceIn(0f, 100f) // -100..100 -> 0..100
        return rep * 0.6f + karma * 0.4f
    }

    fun computeMind(state: GameState): Float {
        // Felicidad (0..100) y energía relativa
        val happy = state.player.happiness.toFloat().coerceIn(0f, 100f)
        val energyPct = (state.player.energy.toFloat() / state.player.maxEnergy.toFloat() * 100f).coerceIn(0f, 100f)
        return happy * 0.7f + energyPct * 0.3f
    }

    /**
     * Índice global: media penalizada por desviación. Un equilibrio alto
     * (todos los ejes ≥ 60 con varianza baja) da puntuación cerca de 100.
     */
    fun computeIndex(w: Float, e: Float, c: Float, m: Float): Float {
        val avg = (w + e + c + m) / 4f
        val diffs = listOf(w - avg, e - avg, c - avg, m - avg)
        val variance = diffs.sumOf { (it * it).toDouble() }.toFloat() / 4f
        // Penalización lineal por varianza alta: cada 100 de varianza ≈ -10 en el índice.
        val penalty = (variance / 10f).coerceIn(0f, 40f)
        return (avg - penalty).coerceIn(0f, 100f)
    }

    fun tierFor(index: Float, w: Float, e: Float, c: Float, m: Float): BalTier {
        return when {
            index >= 80f -> BalTier.HARMONIC
            w >= 75f && (c <= 25f || e <= 25f) -> BalTier.TYRANT_BUBBLE
            (c >= 70f || e >= 70f) && w <= 25f -> BalTier.MARTYR
            m <= 20f -> BalTier.BURNED_OUT
            index <= 40f -> BalTier.STRUGGLING
            else -> BalTier.GROWING
        }
    }

    fun derive(state: GameState): BalWealthState {
        val w = computeWealth(state)
        val e = computeEmployees(state)
        val c = computeCommunity(state)
        val m = computeMind(state)
        val idx = computeIndex(w, e, c, m)
        val tier = tierFor(idx, w, e, c, m)
        val prev = state.balWealth
        // Suavizado: muévete como mucho 1 unidad por tick para que se sienta orgánico
        val smoothIdx = prev.index + (idx - prev.index).coerceIn(-1f, 1f)
        val smoothW = prev.wealthScore + (w - prev.wealthScore).coerceIn(-1f, 1f)
        val smoothE = prev.employeeScore + (e - prev.employeeScore).coerceIn(-1f, 1f)
        val smoothC = prev.communityScore + (c - prev.communityScore).coerceIn(-1f, 1f)
        val smoothM = prev.mindScore + (m - prev.mindScore).coerceIn(-1f, 1f)
        return prev.copy(
            index = smoothIdx,
            wealthScore = smoothW,
            employeeScore = smoothE,
            communityScore = smoothC,
            mindScore = smoothM,
            tier = tier.name,
            highestEverIndex = max(prev.highestEverIndex, smoothIdx),
            daysAtHarmonic = prev.daysAtHarmonic,
            daysAtTyrantBubble = prev.daysAtTyrantBubble
        )
    }

    /** Bonificación de XP por equilibrio (premia mantener el balance). */
    fun xpBonus(state: BalWealthState): Long {
        return ((state.index - 40f).coerceAtLeast(0f) / 5f).toLong()
    }

    /** Penalización: si TYRANT_BUBBLE más de 14 días, hay revolución. */
    fun isRevolutionRisk(state: BalWealthState): Boolean = state.daysAtTyrantBubble >= 14
    fun isBurnoutRisk(state: BalWealthState): Boolean = state.tier == BalTier.BURNED_OUT.name

    /**
     * FIX BUG-09-#6: incrementa contadores de permanencia en cada tier una vez
     * al día. Llamar SOLO al cambio de día (cada 1.440 ticks). Si el tier ya no
     * coincide, los contadores se reinician a 0.
     */
    fun bumpDailyCounters(state: BalWealthState): BalWealthState {
        val isHarmonic = state.tier == BalTier.HARMONIC.name
        val isTyrant = state.tier == BalTier.TYRANT_BUBBLE.name
        return state.copy(
            daysAtHarmonic = if (isHarmonic) state.daysAtHarmonic + 1 else 0,
            daysAtTyrantBubble = if (isTyrant) state.daysAtTyrantBubble + 1 else 0
        )
    }
}
