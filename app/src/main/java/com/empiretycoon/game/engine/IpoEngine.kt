package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Motor de IPO: gestiona transición de fases y, una vez cotizando,
 * el movimiento de precio y los pagos de dividendos.
 */
object IpoEngine {

    /** Cada cuántos ticks se actualiza el precio de la acción de la compañía. */
    private const val PRICE_TICK_MOD: Long = 15L

    /** Cada cuántos ticks se paga dividendo (90 días = 90 * 1.440). */
    private const val DIVIDEND_TICK_MOD: Long = 1_440L * 90L

    /** Probabilidad de split cuando el precio supera 5x el IPO. */
    private const val SPLIT_PROBABILITY: Double = 0.10

    // ----------------------- Acciones del jugador -----------------------

    fun fileProspectus(state: GameState): GameState {
        val ipo = state.ipo
        if (ipo.phase != IPOPhase.LOCKED) return notify(state, NotificationKind.WARNING,
            "Ya en marcha", "El proceso de IPO ya ha comenzado.")
        if (!IPOConstraints.canFileProspectus(state)) {
            return notify(state, NotificationKind.ERROR, "Requisitos no cumplidos",
                "Necesitas ${"%,.0f".format(IPOConstraints.MIN_CASH)} €, " +
                "rep ${IPOConstraints.MIN_REPUTATION} y nivel ${IPOConstraints.MIN_LEVEL}.")
        }
        val valuation = IPOState.estimateValuation(state)
        val newIpo = ipo.copy(
            phase = IPOPhase.PROSPECTUS,
            prospectusFiledAt = state.tick,
            projectedValuation = valuation
        )
        return notify(
            state.copy(ipo = newIpo),
            NotificationKind.SUCCESS,
            "Folleto presentado",
            "Valoración estimada: ${"%,.0f".format(valuation)} €."
        )
    }

    fun completeRoadshow(state: GameState): GameState {
        val ipo = state.ipo
        if (ipo.phase != IPOPhase.PROSPECTUS) return notify(state, NotificationKind.WARNING,
            "Folleto no listo", "Primero presenta el folleto.")
        val ticksSinceFile = state.tick - ipo.prospectusFiledAt
        if (ticksSinceFile < IPOConstraints.PROSPECTUS_REVIEW_TICKS) {
            return notify(state, NotificationKind.WARNING, "Folleto en revisión",
                "Espera a que termine la revisión.")
        }
        val newIpo = ipo.copy(
            phase = IPOPhase.ROADSHOW,
            roadshowStartedAt = state.tick
        )
        return notify(state.copy(ipo = newIpo), NotificationKind.INFO,
            "Roadshow iniciado", "Convence a inversores institucionales.")
    }

    fun listOnExchange(state: GameState): GameState {
        val ipo = state.ipo
        if (ipo.phase != IPOPhase.ROADSHOW) return notify(state, NotificationKind.WARNING,
            "Roadshow pendiente", "Inicia el roadshow primero.")
        val ticksSinceRoadshow = state.tick - ipo.roadshowStartedAt
        if (ticksSinceRoadshow < IPOConstraints.ROADSHOW_TICKS) {
            return notify(state, NotificationKind.WARNING, "Aún en roadshow",
                "Espera a que termine el roadshow.")
        }

        val ticker = makeTicker(state.company.name)
        val ipoPrice = IPOState.computeIpoPrice(ipo.projectedValuation)
        val totalShares = IPOConstraints.INITIAL_SHARES_OUTSTANDING
        val publicShares = (totalShares * IPOConstraints.PRIMARY_OFFERING_FLOAT_PCT).toLong()
        val ownerShares = totalShares - publicShares
        val cashRaised = ipoPrice * publicShares * 0.95  // 5% de descuento de banca

        val listed = CompanyStock(
            ticker = ticker,
            sharesOutstanding = totalShares,
            sharesPublic = publicShares,
            sharesOwnedByPlayer = ownerShares,
            currentPrice = ipoPrice,
            ipoPrice = ipoPrice,
            listedAtTick = state.tick,
            dividendYield = 0.02,
            volatility = 0.06,
            history = listOf(ipoPrice)
        )
        val newIpo = ipo.copy(
            phase = IPOPhase.LISTED,
            listed = listed
        )
        val company = state.company.copy(cash = state.company.cash + cashRaised)

        return notify(
            state.copy(company = company, ipo = newIpo),
            NotificationKind.SUCCESS,
            "¡IPO completada!",
            "$ticker debuta a ${"%,.2f".format(ipoPrice)} €. Recaudados ${"%,.0f".format(cashRaised)} €."
        )
    }

    /** Vender más acciones del paquete del dueño en el mercado abierto. */
    fun sellDownStake(state: GameState, sharesToSell: Long): GameState {
        val ipo = state.ipo
        val listed = ipo.listed ?: return notify(state, NotificationKind.WARNING,
            "No cotizada", "La empresa no está en bolsa.")
        val n = sharesToSell.coerceIn(0L, listed.sharesOwnedByPlayer)
        if (n <= 0) return state

        // Cada acción vendida deprime ligeramente el precio (~0.1% por 1% del float).
        val pctOfFloat = n.toDouble() / max(1.0, listed.sharesPublic.toDouble())
        val dropFactor = (1.0 - 0.10 * pctOfFloat).coerceIn(0.85, 1.0)
        val executionPrice = listed.currentPrice * dropFactor

        val proceeds = executionPrice * n
        val updated = listed.copy(
            sharesOwnedByPlayer = listed.sharesOwnedByPlayer - n,
            sharesPublic = listed.sharesPublic + n,
            currentPrice = executionPrice
        )
        val company = state.company.copy(cash = state.company.cash + proceeds)
        return notify(
            state.copy(
                company = company,
                ipo = ipo.copy(listed = updated)
            ),
            NotificationKind.SUCCESS,
            "Venta secundaria",
            "$n acciones vendidas por ${"%,.0f".format(proceeds)} €."
        )
    }

    // ----------------------- Tick -----------------------

    /**
     * Mueve precio (cada PRICE_TICK_MOD ticks), aplica dividendos y splits.
     * Llamar una vez por segundo simulado; internamente filtra por mod.
     */
    fun tickCompanyStock(state: GameState, rng: Random): GameState {
        val ipo = state.ipo
        val listed = ipo.listed ?: return state
        if (ipo.phase != IPOPhase.LISTED) return state

        var s = state
        var stock = listed

        // Mueve precio cada PRICE_TICK_MOD ticks
        if (state.tick % PRICE_TICK_MOD == 0L) {
            val performance = companyPerformance(state)
            val drift = performance * 0.0008  // sesgo según salud de la empresa
            val noise = rng.nextDouble(-1.0, 1.0) * stock.volatility
            val factor = (1.0 + drift + noise * 0.1).coerceIn(0.85, 1.20)
            val newPrice = (stock.currentPrice * factor).coerceAtLeast(0.5)
            stock = stock.copy(
                currentPrice = newPrice,
                history = stock.nextHistory(value = newPrice)
            )
        }

        // Dividendos (cada 90 días si yield > 0)
        if (state.tick > 0 && state.tick % DIVIDEND_TICK_MOD == 0L && stock.dividendYield > 0) {
            val annual = stock.annualDividendPerShare()
            val perShareThisQuarter = annual / 4.0
            val totalToPay = perShareThisQuarter * stock.sharesOutstanding
            val maxAfford = state.company.cash * 0.4  // máximo 40% del cash de la empresa
            val actualTotal = min(totalToPay, maxAfford).coerceAtLeast(0.0)
            if (actualTotal > 0.0) {
                val perShareActual = actualTotal / max(1.0, stock.sharesOutstanding.toDouble())
                val toPlayer = perShareActual * stock.sharesOwnedByPlayer
                stock = stock.copy(totalDividendsPaid = stock.totalDividendsPaid + actualTotal)
                val dvd = Dividend(stock.ticker, perShareActual, state.tick, actualTotal)
                s = s.copy(
                    company = s.company.copy(cash = s.company.cash - actualTotal + toPlayer),
                    ipo = s.ipo.copy(
                        dividendHistory = (s.ipo.dividendHistory + dvd).takeLast(20)
                    )
                )
                val n = GameNotification(
                    id = System.nanoTime(),
                    timestamp = System.currentTimeMillis(),
                    kind = NotificationKind.SUCCESS,
                    title = "Dividendo pagado",
                    message = "${stock.ticker}: ${"%,.4f".format(perShareActual)} € por acción."
                )
                s = s.copy(notifications = (s.notifications + n).takeLast(40))
            }
        }

        // Splits si el precio se ha disparado y la suerte acompaña
        if (stock.currentPrice >= stock.ipoPrice * 5.0 && rng.nextDouble() < SPLIT_PROBABILITY &&
            state.tick % 1_440L == 0L) {
            val ratio = 2
            val split = StockSplit(stock.ticker, ratio, state.tick)
            stock = StockEventEngine.applySplit(stock, split)
            s = s.copy(
                ipo = s.ipo.copy(
                    splitHistory = (s.ipo.splitHistory + split).takeLast(10)
                )
            )
            val n = GameNotification(
                id = System.nanoTime(),
                timestamp = System.currentTimeMillis(),
                kind = NotificationKind.INFO,
                title = "Split ${ratio}x1",
                message = "${stock.ticker} divide acciones."
            )
            s = s.copy(notifications = (s.notifications + n).takeLast(40))
        }

        return s.copy(ipo = s.ipo.copy(listed = stock))
    }

    // ----------------------- Helpers -----------------------

    private fun companyPerformance(state: GameState): Double {
        // Mezcla cash, reputación, ratio de empleados asignados.
        val reputationFactor = (state.company.reputation - 50) / 50.0  // -1..1
        val cashFactor = if (state.company.cash > 1_000_000) 0.5 else if (state.company.cash < 0) -1.0 else 0.0
        val workerFactor = if (state.company.employees.isEmpty()) 0.0 else
            state.company.totalWorkers.toDouble() / state.company.employees.size - 0.5
        return reputationFactor + cashFactor + workerFactor
    }

    private fun makeTicker(name: String): String {
        val cleaned = name.uppercase().replace(Regex("[^A-Z]"), "")
        return when {
            cleaned.length >= 4 -> cleaned.substring(0, 4)
            cleaned.length in 1..3 -> cleaned.padEnd(4, 'X')
            else -> "MYCO"
        }
    }

    private fun notify(state: GameState, kind: NotificationKind, title: String, msg: String): GameState {
        val n = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = kind,
            title = title,
            message = msg
        )
        return state.copy(notifications = (state.notifications + n).takeLast(40))
    }
}
