package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*

/**
 * Motor de opciones: compra, ejercicio y expiración automática.
 * Premium incluye una pequeña fee del bróker.
 */
object OptionsEngine {

    fun buyCall(
        state: GameState,
        ticker: String,
        strike: Double,
        expiryTick: Long,
        contractSize: Int = OptionsPricer.DEFAULT_CONTRACT_SIZE
    ): GameState {
        val stock = state.stocks.find { it.ticker == ticker } ?: return notify(
            state, NotificationKind.ERROR, "Ticker desconocido", "$ticker no existe.")
        if (expiryTick <= state.tick) return notify(state, NotificationKind.WARNING,
            "Vencimiento inválido", "El vencimiento debe ser futuro.")
        val ticksToExpiry = expiryTick - state.tick
        val premium = OptionsPricer.fairPremium(stock, strike, ticksToExpiry,
            isCall = true, contractSize = contractSize)
        val totalCost = premium + OptionsPricer.BROKERAGE_FEE
        if (state.company.cash < totalCost) {
            return notify(state, NotificationKind.ERROR, "Sin fondos",
                "Necesitas ${"%,.0f".format(totalCost)} €.")
        }
        val opt = CallOption(
            id = "call_${state.tick}_${state.options.calls.size}_${(0..9_999).random()}",
            ticker = ticker,
            strikePrice = strike,
            expiryTick = expiryTick,
            premiumPaid = premium,
            contractSize = contractSize,
            openedAtTick = state.tick
        )
        return state.copy(
            company = state.company.copy(cash = state.company.cash - totalCost),
            options = state.options.copy(
                calls = state.options.calls + opt,
                contractsTraded = state.options.contractsTraded + 1
            )
        ).withNotif(NotificationKind.SUCCESS, "Call comprado",
            "$ticker @${"%,.2f".format(strike)} por ${"%,.0f".format(premium)} €.")
    }

    fun buyPut(
        state: GameState,
        ticker: String,
        strike: Double,
        expiryTick: Long,
        contractSize: Int = OptionsPricer.DEFAULT_CONTRACT_SIZE
    ): GameState {
        val stock = state.stocks.find { it.ticker == ticker } ?: return notify(
            state, NotificationKind.ERROR, "Ticker desconocido", "$ticker no existe.")
        if (expiryTick <= state.tick) return notify(state, NotificationKind.WARNING,
            "Vencimiento inválido", "El vencimiento debe ser futuro.")
        val ticksToExpiry = expiryTick - state.tick
        val premium = OptionsPricer.fairPremium(stock, strike, ticksToExpiry,
            isCall = false, contractSize = contractSize)
        val totalCost = premium + OptionsPricer.BROKERAGE_FEE
        if (state.company.cash < totalCost) {
            return notify(state, NotificationKind.ERROR, "Sin fondos",
                "Necesitas ${"%,.0f".format(totalCost)} €.")
        }
        val opt = PutOption(
            id = "put_${state.tick}_${state.options.puts.size}_${(0..9_999).random()}",
            ticker = ticker,
            strikePrice = strike,
            expiryTick = expiryTick,
            premiumPaid = premium,
            contractSize = contractSize,
            openedAtTick = state.tick
        )
        return state.copy(
            company = state.company.copy(cash = state.company.cash - totalCost),
            options = state.options.copy(
                puts = state.options.puts + opt,
                contractsTraded = state.options.contractsTraded + 1
            )
        ).withNotif(NotificationKind.SUCCESS, "Put comprado",
            "$ticker @${"%,.2f".format(strike)} por ${"%,.0f".format(premium)} €.")
    }

    fun exerciseOption(state: GameState, optionId: String): GameState {
        val callIdx = state.options.calls.indexOfFirst { it.id == optionId }
        val putIdx = state.options.puts.indexOfFirst { it.id == optionId }
        return when {
            callIdx >= 0 -> exerciseCall(state, callIdx)
            putIdx >= 0 -> exercisePut(state, putIdx)
            else -> state
        }
    }

    private fun exerciseCall(state: GameState, idx: Int): GameState {
        val opt = state.options.calls[idx]
        if (opt.closed) return state
        val stock = state.stocks.find { it.ticker == opt.ticker } ?: return state
        val intrinsic = opt.intrinsicValue(stock.price)
        val pnl = intrinsic - opt.premiumPaid
        val closed = opt.copy(closed = true, exercised = true)
        val newCalls = state.options.calls.toMutableList().also { it[idx] = closed }
        val newOpts = state.options.copy(
            calls = newCalls,
            totalRealizedPnL = state.options.totalRealizedPnL + pnl
        )
        return state.copy(
            company = state.company.copy(cash = state.company.cash + intrinsic),
            options = newOpts
        ).withNotif(
            if (pnl >= 0) NotificationKind.SUCCESS else NotificationKind.WARNING,
            "Call ejercido",
            "${opt.ticker}: P&L ${"%,.0f".format(pnl)} €."
        )
    }

    private fun exercisePut(state: GameState, idx: Int): GameState {
        val opt = state.options.puts[idx]
        if (opt.closed) return state
        val stock = state.stocks.find { it.ticker == opt.ticker } ?: return state
        val intrinsic = opt.intrinsicValue(stock.price)
        val pnl = intrinsic - opt.premiumPaid
        val closed = opt.copy(closed = true, exercised = true)
        val newPuts = state.options.puts.toMutableList().also { it[idx] = closed }
        val newOpts = state.options.copy(
            puts = newPuts,
            totalRealizedPnL = state.options.totalRealizedPnL + pnl
        )
        return state.copy(
            company = state.company.copy(cash = state.company.cash + intrinsic),
            options = newOpts
        ).withNotif(
            if (pnl >= 0) NotificationKind.SUCCESS else NotificationKind.WARNING,
            "Put ejercido",
            "${opt.ticker}: P&L ${"%,.0f".format(pnl)} €."
        )
    }

    /**
     * Expira automáticamente lo que ha vencido. ITM se liquida (efectivo),
     * OTM se cierra a 0. P&L se acumula en totalRealizedPnL.
     */
    fun tickOptions(state: GameState): GameState {
        val ob = state.options
        if (ob.calls.isEmpty() && ob.puts.isEmpty()) return state

        var company = state.company
        var realized = 0.0
        val notifs = mutableListOf<GameNotification>()

        val newCalls = ob.calls.map { c ->
            if (c.closed || !c.isExpired(state.tick)) c else {
                val stock = state.stocks.find { it.ticker == c.ticker }
                val intrinsic = if (stock != null) c.intrinsicValue(stock.price) else 0.0
                if (intrinsic > 0.0) {
                    company = company.copy(cash = company.cash + intrinsic)
                    notifs += notifMsg(NotificationKind.SUCCESS, "Call expirado ITM",
                        "${c.ticker}: ${"%,.0f".format(intrinsic)} € liquidados.")
                } else {
                    notifs += notifMsg(NotificationKind.WARNING, "Call expirado OTM",
                        "${c.ticker}: prima perdida.")
                }
                realized += intrinsic - c.premiumPaid
                c.copy(closed = true, exercised = intrinsic > 0)
            }
        }
        val newPuts = ob.puts.map { p ->
            if (p.closed || !p.isExpired(state.tick)) p else {
                val stock = state.stocks.find { it.ticker == p.ticker }
                val intrinsic = if (stock != null) p.intrinsicValue(stock.price) else 0.0
                if (intrinsic > 0.0) {
                    company = company.copy(cash = company.cash + intrinsic)
                    notifs += notifMsg(NotificationKind.SUCCESS, "Put expirado ITM",
                        "${p.ticker}: ${"%,.0f".format(intrinsic)} € liquidados.")
                } else {
                    notifs += notifMsg(NotificationKind.WARNING, "Put expirado OTM",
                        "${p.ticker}: prima perdida.")
                }
                realized += intrinsic - p.premiumPaid
                p.copy(closed = true, exercised = intrinsic > 0)
            }
        }

        val newOpts = ob.copy(
            calls = newCalls,
            puts = newPuts,
            totalRealizedPnL = ob.totalRealizedPnL + realized
        )
        val newNotifs = (state.notifications + notifs).takeLast(40)
        return state.copy(company = company, options = newOpts, notifications = newNotifs)
    }

    // ----------------------- Helpers -----------------------

    private fun notifMsg(kind: NotificationKind, title: String, msg: String): GameNotification =
        GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = kind,
            title = title,
            message = msg
        )

    private fun GameState.withNotif(kind: NotificationKind, title: String, msg: String): GameState {
        val n = notifMsg(kind, title, msg)
        return copy(notifications = (notifications + n).takeLast(40))
    }

    private fun notify(state: GameState, kind: NotificationKind, title: String, msg: String): GameState =
        state.withNotif(kind, title, msg)
}
