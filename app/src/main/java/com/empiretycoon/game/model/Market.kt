package com.empiretycoon.game.model

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/**
 * Estado del mercado global. Cada recurso oscila en torno a su precio base
 * siguiendo un factor multiplicativo de demanda-oferta.
 */
@Serializable
data class Market(
    /** Map<resourceId, factor> con factores entre 0.55 y 1.8. */
    val priceFactors: Map<String, Double> = emptyMap(),
    /** Tendencia muy suave por recurso que persiste varios ticks. */
    val priceTrend: Map<String, Double> = emptyMap()
) {
    fun priceOf(resourceId: String): Double {
        val base = ResourceCatalog.byId(resourceId).basePrice
        val f = priceFactors[resourceId] ?: 1.0
        return base * f
    }

    fun buyPriceOf(resourceId: String): Double = priceOf(resourceId) * 1.15
    fun sellPriceOf(resourceId: String): Double = priceOf(resourceId) * 0.90

    companion object {
        fun fresh(): Market {
            val factors = ResourceCatalog.all.associate { it.id to 1.0 }
            val trend = ResourceCatalog.all.associate { it.id to 0.0 }
            return Market(factors, trend)
        }
    }
}

/** Acciones del mercado bursátil: precio por tick, volatilidad, participaciones. */
@Serializable
data class Stock(
    val ticker: String,
    val companyName: String,
    val price: Double,
    val volatility: Double,
    val trend: Double = 0.0,
    val priceHistory: List<Double> = emptyList(),
    /** Yield anual (0.04 = 4% del precio repartido al año). */
    val annualDividendYield: Double = 0.0
)

/** Posiciones del jugador en cada acción. */
@Serializable
data class StockHoldings(
    val shares: Map<String, Int> = emptyMap(),
    val avgCost: Map<String, Double> = emptyMap()
)

object StockCatalog {
    // FIX feedback usuario: dividendos demasiado bajos (1.5-6% APR es realista
    // pero notas casi cero a corto plazo). Subimos yields a 8-22% APR para
    // que se sientan en la economía del juego.
    fun starter(): List<Stock> = listOf(
        Stock("AGRX", "AgriCorp",       42.0,  0.04,  annualDividendYield = 0.18),
        Stock("STEL", "SteelWorks Inc.", 88.0, 0.05,  annualDividendYield = 0.15),
        Stock("OILN", "NorthOil",       135.0, 0.07,  annualDividendYield = 0.22),
        Stock("CHIP", "ChipForge",      320.0, 0.09,  annualDividendYield = 0.08),
        Stock("CARM", "Carmotive",      210.0, 0.08,  annualDividendYield = 0.13),
        Stock("LUXE", "LuxeBrands",     410.0, 0.06,  annualDividendYield = 0.10),
        Stock("SOFT", "CoderCo",        180.0, 0.10,  annualDividendYield = 0.06),
        Stock("NAVE", "Navegare",        95.0, 0.055, annualDividendYield = 0.17)
    )
}

/** Clamp helpers para el motor económico. */
internal fun clampFactor(v: Double): Double = min(1.8, max(0.55, v))
