package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Economía: paseo aleatorio acotado (random walk) del factor de precio.
 * La tendencia cambia de signo lentamente. Cada tick el factor se corrige
 * hacia la tendencia con ruido multiplicativo pequeño.
 */
object Economy {

    private const val REVERSION = 0.004    // tira hacia 1.0
    private const val NOISE = 0.015

    fun tickMarket(market: Market, rng: Random): Market {
        val newFactors = HashMap<String, Double>(market.priceFactors.size)
        val newTrends = HashMap<String, Double>(market.priceTrend.size)

        for (res in ResourceCatalog.all) {
            val f = market.priceFactors[res.id] ?: 1.0
            val t = market.priceTrend[res.id] ?: 0.0

            val noise = (rng.nextDouble() - 0.5) * 2 * NOISE
            val reversion = (1.0 - f) * REVERSION
            var nf = f + t + reversion + noise
            nf = clampFactor(nf)

            // la tendencia cambia raramente para simular ciclos
            val nt = if (rng.nextDouble() < 0.003) {
                (rng.nextDouble() - 0.5) * 0.012
            } else t * 0.995

            newFactors[res.id] = nf
            newTrends[res.id] = nt
        }
        return market.copy(priceFactors = newFactors, priceTrend = newTrends)
    }

    /** Vender recursos al mercado: aumenta oferta => baja factor. */
    fun applySale(market: Market, resourceId: String, qty: Int): Market {
        val f = market.priceFactors[resourceId] ?: 1.0
        val impact = min(0.05, qty * 0.0008)
        val nf = clampFactor(f - impact)
        return market.copy(priceFactors = market.priceFactors + (resourceId to nf))
    }

    /** Comprar del mercado: aumenta demanda => sube factor. */
    fun applyPurchase(market: Market, resourceId: String, qty: Int): Market {
        val f = market.priceFactors[resourceId] ?: 1.0
        val impact = min(0.05, qty * 0.0008)
        val nf = clampFactor(f + impact)
        return market.copy(priceFactors = market.priceFactors + (resourceId to nf))
    }

    /** Un paso de evolución de la bolsa. Trend ocasional + ruido. */
    fun tickStocks(stocks: List<Stock>, rng: Random): List<Stock> {
        return stocks.map { s ->
            val shock = (rng.nextDouble() - 0.5) * 2 * s.volatility
            val trendDecay = s.trend * 0.98
            val newTrend = if (rng.nextDouble() < 0.01) {
                (rng.nextDouble() - 0.5) * s.volatility * 1.5
            } else trendDecay
            val mult = 1.0 + shock + newTrend
            val np = max(1.0, s.price * mult)
            val hist = (s.priceHistory + np).takeLast(60)
            s.copy(price = np, trend = newTrend, priceHistory = hist)
        }
    }
}
