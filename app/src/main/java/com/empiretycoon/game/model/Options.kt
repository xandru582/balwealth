package com.empiretycoon.game.model

import kotlinx.serialization.Serializable
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Derivados simplificados sobre acciones del mercado (Stock).
 *
 * Modelo simplificado, NO Black-Scholes real:
 *  - sin tasa libre de riesgo
 *  - volatilidad anualizada estimada a partir del ticker
 *  - precio en función de moneyness y tiempo a vencimiento
 *
 * 1 contrato = `contractSize` acciones (por defecto 100, como en mercado real).
 */
@Serializable
data class CallOption(
    val id: String,
    val ticker: String,
    val strikePrice: Double,
    val expiryTick: Long,
    val premiumPaid: Double,
    val contractSize: Int = 100,
    val openedAtTick: Long = 0L,
    val closed: Boolean = false,
    val exercised: Boolean = false
) {
    fun ticksToExpiry(currentTick: Long): Long = max(0L, expiryTick - currentTick)
    fun isExpired(currentTick: Long): Boolean = currentTick >= expiryTick

    fun intrinsicValue(spotPrice: Double): Double =
        max(0.0, spotPrice - strikePrice) * contractSize

    fun breakeven(): Double = strikePrice + (premiumPaid / contractSize)
}

@Serializable
data class PutOption(
    val id: String,
    val ticker: String,
    val strikePrice: Double,
    val expiryTick: Long,
    val premiumPaid: Double,
    val contractSize: Int = 100,
    val openedAtTick: Long = 0L,
    val closed: Boolean = false,
    val exercised: Boolean = false
) {
    fun ticksToExpiry(currentTick: Long): Long = max(0L, expiryTick - currentTick)
    fun isExpired(currentTick: Long): Boolean = currentTick >= expiryTick

    fun intrinsicValue(spotPrice: Double): Double =
        max(0.0, strikePrice - spotPrice) * contractSize

    fun breakeven(): Double = strikePrice - (premiumPaid / contractSize)
}

@Serializable
data class OptionsBook(
    val calls: List<CallOption> = emptyList(),
    val puts: List<PutOption> = emptyList(),
    val totalRealizedPnL: Double = 0.0,
    val contractsTraded: Int = 0
) {
    fun totalPositions(): Int =
        calls.count { !it.closed } + puts.count { !it.closed }

    fun totalPremiumOpen(): Double =
        calls.filterNot { it.closed }.sumOf { it.premiumPaid } +
        puts.filterNot { it.closed }.sumOf { it.premiumPaid }
}

/**
 * Pricer simplificado tipo "Black-Scholes lite":
 *   premium per share = max(intrinsic, 0) + time_value
 *   time_value = vol * spot * sqrt(T) * 0.4
 * donde T = ticksToExpiry / TICKS_PER_YEAR (asumiendo 365 días * 1.440 ticks).
 *
 * No es financieramente exacto, pero produce primas razonables: caras OTM
 * con mucho tiempo, baratas si vencen pronto, escalan con volatilidad.
 */
object OptionsPricer {

    private const val TICKS_PER_DAY: Double = 1_440.0
    private const val TICKS_PER_YEAR: Double = TICKS_PER_DAY * 365.0
    const val DEFAULT_CONTRACT_SIZE: Int = 100

    /** Comisión que cobra el bróker al abrir. */
    const val BROKERAGE_FEE: Double = 25.0

    /**
     * Prima JUSTA por contrato (no por acción).
     * Multiplicar por contractSize (ya incluido).
     */
    fun fairPremium(
        stock: Stock,
        strike: Double,
        ticksToExpiry: Long,
        isCall: Boolean,
        contractSize: Int = DEFAULT_CONTRACT_SIZE
    ): Double {
        val spot = stock.price
        val vol = stock.volatility.coerceIn(0.01, 1.0)
        val tYears = (ticksToExpiry.toDouble() / TICKS_PER_YEAR).coerceAtLeast(0.0)

        // valor intrínseco por acción
        val intrinsic = if (isCall) max(0.0, spot - strike) else max(0.0, strike - spot)

        // valor temporal por acción: simplificado pero monótono en T y vol
        val timeValue = if (tYears <= 0.0) 0.0 else {
            // ATM-equivalent factor con caída suave si está muy OTM
            val moneyness = if (isCall) spot / strike.coerceAtLeast(0.01)
                            else strike / spot.coerceAtLeast(0.01)
            // Suaviza moneyness para no disparar primas en deep ITM
            val mFactor = exp(-((ln(moneyness)).let { it * it }) * 0.6)
                .coerceIn(0.05, 1.0)
            spot * vol * sqrt(tYears) * 0.4 * mFactor
        }

        val perShare = intrinsic + timeValue
        return perShare * contractSize
    }

    /** Convierte días a ticks de vencimiento desde el tick actual. */
    fun expiryTickFromDays(currentTick: Long, days: Int): Long =
        currentTick + (days.toLong() * TICKS_PER_DAY.toLong())
}

/** Strikes y vencimientos sugeridos en la UI (relativos al spot actual). */
object OptionsCatalog {
    val STRIKE_OFFSETS_PCT: List<Double> = listOf(0.85, 0.95, 1.0, 1.05, 1.15)
    val EXPIRY_DAYS: List<Int> = listOf(7, 14, 30, 60, 90)
}
