package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Eventos accionariales (dividendos y splits) que afectan a CompanyStock
 * o, en el futuro, a Stock estándar del mercado.
 */
@Serializable
data class StockSplit(
    val ticker: String,
    val ratio: Int,        // p.ej. 2 -> 2x1 split; 3 -> 3x1
    val atTick: Long
)

@Serializable
data class Dividend(
    val ticker: String,
    val amountPerShare: Double,
    val atTick: Long,
    val totalPaid: Double = 0.0
)

/**
 * Jerarquía sellada de eventos bursátiles. Sirve como histórico unificado.
 * Es @Serializable para encajar en el árbol de estado.
 */
@Serializable
sealed class StockEvent {
    abstract val ticker: String
    abstract val atTick: Long

    @Serializable
    data class SplitEvent(
        override val ticker: String,
        override val atTick: Long,
        val ratio: Int
    ) : StockEvent()

    @Serializable
    data class DividendEvent(
        override val ticker: String,
        override val atTick: Long,
        val amountPerShare: Double,
        val totalPaid: Double
    ) : StockEvent()

    @Serializable
    data class ListingEvent(
        override val ticker: String,
        override val atTick: Long,
        val ipoPrice: Double
    ) : StockEvent()

    @Serializable
    data class HaltEvent(
        override val ticker: String,
        override val atTick: Long,
        val reason: String
    ) : StockEvent()
}

/** Helpers para aplicar eventos a CompanyStock. */
object StockEventEngine {

    /** Devuelve un nuevo CompanyStock tras aplicar un split N-a-1. */
    fun applySplit(stock: CompanyStock, split: StockSplit): CompanyStock {
        if (split.ratio <= 1) return stock
        return stock.copy(
            sharesOutstanding = stock.sharesOutstanding * split.ratio,
            sharesPublic = stock.sharesPublic * split.ratio,
            sharesOwnedByPlayer = stock.sharesOwnedByPlayer * split.ratio,
            currentPrice = stock.currentPrice / split.ratio,
            ipoPrice = stock.ipoPrice / split.ratio,
            history = stock.history.map { it / split.ratio },
            splitsApplied = stock.splitsApplied + 1
        )
    }

    /** Calcula y resta el dividendo del cash retenido en la empresa. */
    fun applyDividend(stock: CompanyStock, dividend: Dividend): CompanyStock {
        val totalPaid = dividend.amountPerShare * stock.sharesOutstanding
        return stock.copy(totalDividendsPaid = stock.totalDividendsPaid + totalPaid)
    }
}
