package com.empiretycoon.game.model

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Sistema bancario: tipos de préstamo, ofertas, préstamos activos.
 *
 * Convenciones:
 *  - 1 día in-game = 1.440 ticks (cf. GameState.kt).
 *  - APR = tipo anual nominal aplicado en cuotas diarias.
 *  - dailyPayment cubre intereses + amortización lineal del principal.
 */
enum class LoanType(
    val displayName: String,
    val emoji: String,
    val baseAprMin: Double,
    val baseAprMax: Double,
    val termDaysMin: Int,
    val termDaysMax: Int,
    val maxAmount: Double,
    val description: String
) {
    PERSONAL(
        displayName = "Personal",
        emoji = "👤",
        baseAprMin = 0.06, baseAprMax = 0.12,
        termDaysMin = 30, termDaysMax = 120,
        maxAmount = 250_000.0,
        description = "Crédito al consumo, rápido y de cuantía limitada."
    ),
    BUSINESS(
        displayName = "Empresarial",
        emoji = "🏢",
        baseAprMin = 0.05, baseAprMax = 0.10,
        termDaysMin = 60, termDaysMax = 240,
        maxAmount = 2_500_000.0,
        description = "Línea de crédito corporativa con tipo competitivo."
    ),
    MORTGAGE(
        displayName = "Hipoteca",
        emoji = "🏠",
        baseAprMin = 0.025, baseAprMax = 0.055,
        termDaysMin = 180, termDaysMax = 720,
        maxAmount = 8_000_000.0,
        description = "Largo plazo con colateral inmobiliario."
    ),
    BRIDGE(
        displayName = "Puente",
        emoji = "🌉",
        baseAprMin = 0.10, baseAprMax = 0.18,
        termDaysMin = 14, termDaysMax = 60,
        maxAmount = 1_500_000.0,
        description = "Financiación corta para liquidez urgente."
    ),
    PREDATORY(
        displayName = "Usurero",
        emoji = "🦊",
        baseAprMin = 0.30, baseAprMax = 0.85,
        termDaysMin = 7, termDaysMax = 30,
        maxAmount = 80_000.0,
        description = "Sin preguntas, sin escrúpulos. Cuidado con el plazo."
    );
}

/**
 * Oferta puntual disponible en el banco.
 * No se serializa entera (las ofertas se regeneran), pero se mantiene
 * Serializable por uniformidad con el resto del estado.
 */
@Serializable
data class LoanOffer(
    val id: String,
    val type: LoanType,
    val amount: Double,
    val interestRateAPR: Double,
    val termDays: Int,
    val fixedFee: Double,
    val lenderName: String,
    val requiredReputation: Int = 0,
    val requiredCollateralValue: Double = 0.0
) {
    /** Cuota diaria total estimada (interés + amortización lineal). */
    fun estimatedDailyPayment(): Double {
        if (termDays <= 0) return amount
        val dailyRate = interestRateAPR / 365.0
        val principalSlice = amount / termDays
        val avgInterest = amount * dailyRate * 0.55  // aprox. amortización lineal
        return principalSlice + avgInterest
    }

    fun totalInterest(): Double {
        val daily = interestRateAPR / 365.0
        // suma intereses sobre saldo decreciente lineal
        var total = 0.0
        var remaining = amount
        val slice = amount / max(1, termDays)
        for (d in 0 until termDays) {
            total += remaining * daily
            remaining -= slice
            if (remaining < 0) remaining = 0.0
        }
        return total + fixedFee
    }
}

@Serializable
data class ActiveLoan(
    val id: String,
    val type: LoanType,
    val principal: Double,
    val remainingPrincipal: Double,
    val interestRateAPR: Double,
    val termDays: Int,
    val daysElapsed: Int = 0,
    val dailyPayment: Double,
    val missedPayments: Int = 0,
    val defaulted: Boolean = false,
    val lenderName: String = "Banco"
) {
    val remainingDays: Int get() = max(0, termDays - daysElapsed)
    val isPaidOff: Boolean get() = remainingPrincipal <= 0.01
    val progress: Float get() =
        if (termDays <= 0) 1f else (daysElapsed.toFloat() / termDays).coerceIn(0f, 1f)
}

@Serializable
data class LoansState(
    val offers: List<LoanOffer> = emptyList(),
    val active: List<ActiveLoan> = emptyList(),
    val totalLifetimeInterest: Double = 0.0,
    val totalDefaults: Int = 0,
    val lastOffersTick: Long = -1
) {
    val totalDebt: Double get() = active.filterNot { it.defaulted }.sumOf { it.remainingPrincipal }
    val totalDailyPayment: Double get() =
        active.filterNot { it.defaulted || it.isPaidOff }.sumOf { it.dailyPayment }
}

/**
 * Genera ofertas según reputación y flujo de caja.
 * Reputación alta -> mejor APR y montos mayores.
 * Cash flow alto  -> ofertas más sustanciales.
 */
object LoanOfferGenerator {

    private val LENDER_NAMES_RETAIL = listOf(
        "BancaIberia", "Caja del Norte", "EuroCrédito",
        "Gala Finanzas", "Banco del Sol", "MutuaCash"
    )
    private val LENDER_NAMES_CORP = listOf(
        "Atlas Capital", "Goldhart Bros.", "Northstar IB",
        "Pirámide Holding", "Volans Lending"
    )
    private val LENDER_NAMES_SHADY = listOf(
        "Don Julio", "El Tiburón", "PrestaYá",
        "Mr. Vega", "Crédito Express 24h"
    )

    fun generate(reputation: Int, cashFlow: Double, rng: Random): List<LoanOffer> {
        val rep = reputation.coerceIn(0, 100)
        val out = mutableListOf<LoanOffer>()

        // Personal: siempre disponible
        out += offer(LoanType.PERSONAL, rep, cashFlow, rng,
            lenderPool = LENDER_NAMES_RETAIL,
            id = "ln_p_${rng.nextInt(100_000, 999_999)}")

        // Empresarial: requiere algo de reputación
        if (rep >= 25) {
            out += offer(LoanType.BUSINESS, rep, cashFlow, rng,
                lenderPool = LENDER_NAMES_CORP,
                id = "ln_b_${rng.nextInt(100_000, 999_999)}")
        }

        // Hipoteca: solo a partir de buena reputación
        if (rep >= 40) {
            out += offer(LoanType.MORTGAGE, rep, cashFlow, rng,
                lenderPool = LENDER_NAMES_CORP,
                id = "ln_m_${rng.nextInt(100_000, 999_999)}").copy(
                    requiredCollateralValue = 0.4 // se interpreta como % del préstamo
                )
        }

        // Puente: si el cashflow no es horrible
        if (cashFlow > -5_000.0 || rep >= 30) {
            out += offer(LoanType.BRIDGE, rep, cashFlow, rng,
                lenderPool = LENDER_NAMES_RETAIL,
                id = "ln_g_${rng.nextInt(100_000, 999_999)}")
        }

        // Usurero: siempre presente como tentación
        out += offer(LoanType.PREDATORY, rep, cashFlow, rng,
            lenderPool = LENDER_NAMES_SHADY,
            id = "ln_x_${rng.nextInt(100_000, 999_999)}")

        return out
    }

    private fun offer(
        type: LoanType,
        reputation: Int,
        cashFlow: Double,
        rng: Random,
        lenderPool: List<String>,
        id: String
    ): LoanOffer {
        // APR: a más reputación, más cerca del mínimo
        val repFactor = (reputation / 100.0).coerceIn(0.0, 1.0)
        val apr = type.baseAprMax - (type.baseAprMax - type.baseAprMin) *
            (0.4 + 0.6 * repFactor) * rng.nextDouble(0.85, 1.15)

        // Plazo: aleatorio dentro del rango del tipo
        val term = rng.nextInt(type.termDaysMin, type.termDaysMax + 1)

        // Monto: escala con reputación y cashflow positivo
        val cashFlowBoost = (cashFlow.coerceIn(0.0, 50_000.0) / 50_000.0)
        val sizeFactor = 0.15 + 0.85 * (0.3 * repFactor + 0.4 * cashFlowBoost +
            0.3 * rng.nextDouble(0.4, 1.0))
        val amount = (type.maxAmount * sizeFactor).coerceAtLeast(type.maxAmount * 0.05)

        // Comisión fija: 0.3% – 2.5% del principal
        val fee = amount * rng.nextDouble(0.003, 0.025)

        // Reputación requerida: tipos serios la piden
        val reqRep = when (type) {
            LoanType.MORTGAGE -> 40
            LoanType.BUSINESS -> 25
            LoanType.BRIDGE -> 20
            LoanType.PERSONAL -> 0
            LoanType.PREDATORY -> 0
        }

        return LoanOffer(
            id = id,
            type = type,
            amount = roundTo(amount, 1_000.0),
            interestRateAPR = max(type.baseAprMin, min(type.baseAprMax, apr)),
            termDays = term,
            fixedFee = roundTo(fee, 50.0),
            lenderName = lenderPool.random(rng),
            requiredReputation = reqRep
        )
    }

    private fun roundTo(v: Double, step: Double): Double {
        if (step <= 0) return v
        return (Math.round(v / step) * step)
    }
}

/** Penalización por mora (incrementa interés y suma misses). */
object LoanPenalties {
    const val DAILY_PENALTY_MULT = 1.5
    const val DEFAULT_AT_MISSES = 5
    const val DEFAULT_REP_PENALTY = 12
}
