package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Motor del mercado cripto. Tickea cada 10s para movimiento de precio,
 * y al cambio de día para drift, sentimiento, rugpulls y stake/mining.
 *
 * Funciones públicas:
 *   - tickPrices(state, rng): movimiento intra-día.
 *   - dailyTick(state, rng): drift diario, rugs, mining payout, stake APY.
 *   - buy(state, sym, qty)/sell(state, sym, qty): trading manual.
 *   - stake(state, sym, qty, days)/unstake(state, sym): bloqueo.
 *   - assignMiners(state, sym, n): empleados a minar.
 *   - claimMining(state, sym): cobrar tokens minados.
 *   - unlock(state): desbloqueo (gating: requiere capital y reputación).
 */
object CryptoEngine {

    /** Cuántos puntos de histórico se guardan (ring buffer). */
    private const val HISTORY_LEN = 60

    /** Spread compra/venta de la pseudo-exchange (cobertura). */
    private const val SPREAD = 0.018

    /** Comisión por operación (sobre notional). */
    private const val FEE = 0.005

    // ===================== Gating =====================

    /** Requisitos para abrir el módulo cripto. */
    fun canUnlock(state: GameState): Boolean =
        state.company.cash >= 250_000.0 && state.company.reputation >= 30

    fun unlock(state: GameState): GameState {
        val crypto = state.crypto
        if (crypto.unlocked) return state
        if (!canUnlock(state)) {
            return notify(state, NotificationKind.ERROR, "🔒 Cripto bloqueada",
                "Necesitas 250.000 € y reputación ≥ 30.")
        }
        return notify(
            state.copy(crypto = crypto.copy(unlocked = true)),
            NotificationKind.SUCCESS,
            "🪙 Mercado cripto abierto",
            "Bienvenido al far west financiero. Cuidado con los rugpulls."
        )
    }

    // ===================== Tick de precios (cada 10s) =====================

    fun tickPrices(state: GameState, rng: Random): GameState {
        if (!state.crypto.unlocked) return state
        val updated = state.crypto.tokens.map { st ->
            val def = CryptoCatalog.byMatching(st.symbol) ?: return@map st
            if (st.rugged) {
                // Después de rug, queda flotando muy bajo con micro-bumps.
                val noise = (rng.nextDouble() - 0.5) * 0.01
                val newPrice = max(def.initialPrice * 0.001, st.price * (1.0 + noise))
                st.copy(price = newPrice, history = pushHistory(st.history, newPrice))
            } else {
                // Random walk geométrico mini (sigma escalada por √(1/144) — 144 ticks/día)
                val sigma = def.volatility / sqrt(144.0)
                val driftStep = (def.drift / 144.0) + st.sentiment * 0.0003
                val u = rng.nextGaussian().coerceIn(-3.0, 3.0)
                val r = driftStep + sigma * u
                val newPrice = max(def.initialPrice * 0.0001, st.price * exp(r))
                st.copy(price = newPrice, history = pushHistory(st.history, newPrice))
            }
        }
        return state.copy(crypto = state.crypto.copy(tokens = updated))
    }

    // ===================== Tick diario =====================

    fun dailyTick(state: GameState, rng: Random): GameState {
        if (!state.crypto.unlocked) return state
        var s = state
        var crypto = s.crypto
        var news = crypto.newsFeed.toMutableList()

        // 1) Rugpulls
        var survivedThisTick = 0
        val newTokens = crypto.tokens.map { st ->
            val def = CryptoCatalog.byMatching(st.symbol) ?: return@map st
            if (!st.rugged && def.rugChancePerDay > 0 && rng.nextDouble() < def.rugChancePerDay) {
                news.add(CryptoNewsItem(
                    tick = s.tick,
                    timestamp = System.currentTimeMillis(),
                    symbol = st.symbol,
                    title = "💀 ${def.name} (${def.symbol}) — RUGPULL",
                    body = "El equipo ha vaciado el contrato. Precio -90%. Holders rotos.",
                    kind = "RUGPULL"
                ))
                // Survivor: el jugador ALGUNA VEZ compró este token (wasEverBought) y
                // ahora tiene posición ~0. El check anterior usaba avgCost>0 pero
                // sell() resetea avgCost a 0.0 al vender 100%, así que se perdía la pista.
                val h = crypto.holding(st.symbol)
                if (h != null && h.wasEverBought && h.amount <= 0.000001) survivedThisTick++
                st.copy(price = st.price * 0.10, rugged = true, sentiment = -1.0)
            } else {
                // Decay de sentimiento hacia 0
                val newSentiment = st.sentiment * 0.85
                // Whale moves aleatorios (5% por token/día)
                if (rng.nextDouble() < 0.05) {
                    val pump = rng.nextBoolean()
                    val mag = 0.05 + rng.nextDouble() * 0.15
                    val signed = if (pump) mag else -mag
                    news.add(CryptoNewsItem(
                        tick = s.tick,
                        timestamp = System.currentTimeMillis(),
                        symbol = st.symbol,
                        title = "${if (pump) "🐳" else "🦀"} Whale move en ${def.symbol}",
                        body = "Una ballena ha ${if (pump) "comprado" else "vendido"} fuerte. Mercado reaccionando.",
                        kind = if (pump) "PUMP" else "DUMP"
                    ))
                    st.copy(price = max(0.0001, st.price * (1.0 + signed)), sentiment = newSentiment + signed)
                } else {
                    st.copy(sentiment = newSentiment)
                }
            }
        }

        // 2) Mining payout (al cierre del día)
        var company = s.company
        var holdings = crypto.holdings.toMutableList()
        var totalAutoSellRevenue = 0.0
        val autoSellLog = mutableListOf<Pair<String, Pair<Double, Double>>>() // symbol -> (qty, € obtenidos)
        for (i in holdings.indices) {
            val h = holdings[i]
            val def = CryptoCatalog.byMatching(h.symbol) ?: continue
            if (h.minersAssigned > 0 && def.miningDifficulty > 0) {
                // Tokens producidos = miners / difficulty
                val produced = h.minersAssigned.toDouble() / def.miningDifficulty
                val tokenState = newTokens.find { it.symbol == h.symbol }
                val rugged = tokenState?.rugged == true

                if (h.autoSellMining && !rugged) {
                    // Auto-sell: claim + sell todo lo minado (incluido lo pendiente
                    // anterior) al precio actual del token. NO toca h.amount.
                    val totalToSell = h.miningPending + produced
                    val price = tokenState?.price ?: 0.0
                    val proceeds = totalToSell * price
                    if (totalToSell > 0 && proceeds > 0) {
                        totalAutoSellRevenue += proceeds
                        autoSellLog += h.symbol to (totalToSell to proceeds)
                    }
                    holdings[i] = h.copy(miningPending = 0.0)
                } else {
                    // Acumula en pending como siempre.
                    holdings[i] = h.copy(miningPending = h.miningPending + produced)
                }
            }
        }
        if (totalAutoSellRevenue > 0.5) {
            company = company.copy(cash = company.cash + totalAutoSellRevenue)
            val detail = autoSellLog.joinToString(", ") { (sym, qp) ->
                "${"%,.4f".format(qp.first)} $sym → ${"%,.0f".format(qp.second)} €"
            }
            news.add(CryptoNewsItem(
                tick = s.tick,
                timestamp = System.currentTimeMillis(),
                symbol = "*",
                title = "🤖 Auto-mining: +${"%,.0f".format(totalAutoSellRevenue)} €",
                body = "Producción vendida: $detail",
                kind = "AUTOSELL"
            ))
        }

        // 3) Stake APY (paga al bloque diario, prorrateado 1/365)
        var totalStakeReward = 0.0
        for (i in holdings.indices) {
            val h = holdings[i]
            val def = CryptoCatalog.byMatching(h.symbol) ?: continue
            if (h.staked > 0 && def.stakeApy > 0) {
                val daily = def.stakeApy / 365.0
                val reward = h.staked * daily
                holdings[i] = h.copy(staked = h.staked + reward)
                val price = newTokens.find { it.symbol == h.symbol }?.price ?: 0.0
                totalStakeReward += reward * price
            }
        }
        if (totalStakeReward > 0.5) {
            news.add(CryptoNewsItem(
                tick = s.tick,
                timestamp = System.currentTimeMillis(),
                symbol = "*",
                title = "💰 Recompensas de staking",
                body = "Has recibido ${"%,.2f".format(totalStakeReward)} € equivalentes en stake APY.",
                kind = "STAKE"
            ))
        }

        // 4) Auto-unlock de stakes vencidos (no movemos el balance, solo aviso)
        for (i in holdings.indices) {
            val h = holdings[i]
            if (h.staked > 0 && s.tick >= h.stakeUnlockTick && h.stakeUnlockTick > 0) {
                news.add(CryptoNewsItem(
                    tick = s.tick,
                    timestamp = System.currentTimeMillis(),
                    symbol = h.symbol,
                    title = "🔓 Stake liberado: ${h.symbol}",
                    body = "Puedes hacer unstake sin penalización.",
                    kind = "STAKE"
                ))
                holdings[i] = h.copy(stakeUnlockTick = 0L)
            }
        }

        // Trim feed
        val trimmedNews = news.takeLast(30)

        crypto = crypto.copy(
            tokens = newTokens,
            holdings = holdings,
            newsFeed = trimmedNews,
            lastDailyTick = s.tick
        )
        // Propaga rugpulls como notificaciones globales
        val rugNotifs = trimmedNews
            .filter { it.kind == "RUGPULL" && it.tick == s.tick }
            .map {
                GameNotification(
                    id = System.nanoTime(),
                    timestamp = it.timestamp,
                    kind = NotificationKind.ERROR,
                    title = it.title,
                    message = it.body
                )
            }
        var notifs = s.notifications
        if (rugNotifs.isNotEmpty()) {
            notifs = (notifs + rugNotifs).takeLast(40)
        }
        s = s.copy(crypto = crypto, company = company, notifications = notifs)

        // 5) Stat: rugpulls sobrevividos detectados en el loop de rug arriba
        if (survivedThisTick > 0) {
            s = s.copy(crypto = s.crypto.copy(
                rugpullsSurvived = s.crypto.rugpullsSurvived + survivedThisTick
            ))
        }

        // 6) Nómina diaria de mineros agregados. Si no hay cash suficiente,
        // se despiden mineros en cascada hasta que la nómina sea pagable.
        val totalMinersBefore = totalMiners(s)
        if (totalMinersBefore > 0) {
            val dailyCost = totalMinersBefore * (MINER_MONTHLY_SALARY / 30.0)
            if (s.company.cash >= dailyCost) {
                s = s.copy(company = s.company.copy(cash = s.company.cash - dailyCost))
            } else {
                // Cash insuficiente: paga lo que pueda y despide proporcionalmente.
                val dailyPerMiner = MINER_MONTHLY_SALARY / 30.0
                val canPayFor = (s.company.cash / dailyPerMiner).toInt().coerceAtLeast(0)
                val toFire = (totalMinersBefore - canPayFor).coerceAtLeast(0)
                val payable = canPayFor * dailyPerMiner
                // Repartir despidos proporcional a minersAssigned por token.
                val firePerToken = HashMap<String, Int>()
                var remaining = toFire
                val sortedHoldings = s.crypto.holdings.sortedByDescending { it.minersAssigned }
                for (h in sortedHoldings) {
                    if (remaining <= 0) break
                    val share = (toFire.toDouble() * h.minersAssigned / totalMinersBefore).toInt()
                        .coerceAtMost(h.minersAssigned)
                    firePerToken[h.symbol] = share
                    remaining -= share
                }
                // Si quedan despidos por redondeo, ir quitando del primero.
                if (remaining > 0) {
                    val firstSym = sortedHoldings.firstOrNull()?.symbol
                    if (firstSym != null) {
                        firePerToken[firstSym] = (firePerToken[firstSym] ?: 0) + remaining
                    }
                }
                val newHoldings = s.crypto.holdings.map { h ->
                    val fired = firePerToken[h.symbol] ?: 0
                    if (fired <= 0) h else h.copy(minersAssigned = (h.minersAssigned - fired).coerceAtLeast(0))
                }
                val n = GameNotification(
                    id = System.nanoTime(),
                    timestamp = System.currentTimeMillis(),
                    kind = NotificationKind.WARNING,
                    title = "💸 Despidos masivos de mineros",
                    message = "$toFire mineros despedidos por impago. Pagaste " +
                        "${"%,.0f".format(payable)} €. Te quedan ${totalMinersBefore - toFire} mineros."
                )
                s = s.copy(
                    company = s.company.copy(cash = (s.company.cash - payable).coerceAtLeast(0.0)),
                    crypto = s.crypto.copy(holdings = newHoldings),
                    notifications = (s.notifications + n).takeLast(40)
                )
            }
        }

        return s
    }

    // ===================== Acciones del jugador =====================

    fun buy(state: GameState, symbol: String, qty: Double): GameState {
        if (!state.crypto.unlocked) return state
        if (qty <= 0.0) return notify(state, NotificationKind.ERROR, "Cantidad inválida", "qty debe ser > 0.")
        val tok = state.crypto.token(symbol) ?: return state
        if (tok.rugged) return notify(state, NotificationKind.ERROR, "Token muerto", "$symbol fue rugpulleado.")
        val askPrice = tok.price * (1.0 + SPREAD)
        val notional = askPrice * qty
        val total = notional * (1.0 + FEE)
        if (state.company.cash < total) {
            return notify(state, NotificationKind.ERROR, "Sin fondos",
                "Necesitas ${"%,.2f".format(total)} €.")
        }
        val h = state.crypto.holdingOrEmpty(symbol)
        val totalAmount = h.amount + qty
        val newAvg = if (totalAmount > 0) {
            (h.amount * h.avgCost + qty * askPrice) / totalAmount
        } else askPrice
        val updatedH = h.copy(amount = totalAmount, avgCost = newAvg, wasEverBought = true)
        val newHoldings = upsertHolding(state.crypto.holdings, updatedH)
        val newCompany = state.company.copy(cash = state.company.cash - total)
        return notify(
            state.copy(company = newCompany, crypto = state.crypto.copy(holdings = newHoldings)),
            NotificationKind.SUCCESS,
            "🪙 Compra cripto",
            "Has comprado ${"%,.4f".format(qty)} $symbol por ${"%,.2f".format(total)} €."
        )
    }

    fun sell(state: GameState, symbol: String, qty: Double): GameState {
        if (!state.crypto.unlocked) return state
        if (qty <= 0.0) return notify(state, NotificationKind.ERROR, "Cantidad inválida", "qty debe ser > 0.")
        val tok = state.crypto.token(symbol) ?: return state
        val h = state.crypto.holdingOrEmpty(symbol)
        if (h.amount < qty - 0.000001) {
            return notify(state, NotificationKind.ERROR, "Sin saldo",
                "Solo tienes ${"%,.4f".format(h.amount)} $symbol.")
        }
        val bidPrice = tok.price * (1.0 - SPREAD)
        val notional = bidPrice * qty
        val proceeds = notional * (1.0 - FEE)
        val realizedPnl = (bidPrice - h.avgCost) * qty
        val newAmount = h.amount - qty
        val newH = h.copy(amount = newAmount, avgCost = if (newAmount <= 0.000001) 0.0 else h.avgCost)
        val newHoldings = upsertHolding(state.crypto.holdings, newH)
        val newCompany = state.company.copy(cash = state.company.cash + proceeds)
        return notify(
            state.copy(
                company = newCompany,
                crypto = state.crypto.copy(
                    holdings = newHoldings,
                    realizedPnl = state.crypto.realizedPnl + realizedPnl
                )
            ),
            if (realizedPnl >= 0) NotificationKind.SUCCESS else NotificationKind.WARNING,
            "🪙 Venta cripto",
            "Has vendido $symbol por ${"%,.2f".format(proceeds)} € (PnL ${"%+,.2f".format(realizedPnl)} €)."
        )
    }

    fun stake(state: GameState, symbol: String, qty: Double, days: Int): GameState {
        if (!state.crypto.unlocked) return state
        if (qty <= 0.0) return notify(state, NotificationKind.ERROR, "Cantidad inválida", "qty debe ser > 0.")
        if (days <= 0) return notify(state, NotificationKind.ERROR, "Días inválidos", "Los días de stake deben ser > 0.")
        val def = CryptoCatalog.byMatching(symbol) ?: return state
        val h = state.crypto.holdingOrEmpty(symbol)
        if (h.amount < qty) return notify(state, NotificationKind.ERROR, "Sin saldo", "Necesitas $qty $symbol líquidos.")
        if (h.staked + qty > def.stakeCap) {
            return notify(state, NotificationKind.ERROR, "Cap excedido",
                "El cap de stake para $symbol es ${def.stakeCap}.")
        }
        val unlockTick = state.tick + days.toLong() * 1_440L
        val newH = h.copy(amount = h.amount - qty, staked = h.staked + qty, stakeUnlockTick = unlockTick)
        return notify(
            state.copy(crypto = state.crypto.copy(holdings = upsertHolding(state.crypto.holdings, newH))),
            NotificationKind.SUCCESS,
            "🔒 Stake activado",
            "$qty $symbol bloqueados $days días al ${"%.1f".format(def.stakeApy * 100)}% APY."
        )
    }

    fun unstake(state: GameState, symbol: String): GameState {
        if (!state.crypto.unlocked) return state
        val h = state.crypto.holdingOrEmpty(symbol)
        if (h.staked <= 0) return state
        val penalty = if (state.tick < h.stakeUnlockTick) 0.10 else 0.0
        val released = h.staked * (1.0 - penalty)
        val burned = h.staked - released
        val newH = h.copy(amount = h.amount + released, staked = 0.0, stakeUnlockTick = 0L)
        val msg = if (penalty > 0) {
            "Saliste antes del unlock. Se quemó ${"%,.4f".format(burned)} $symbol como penalización."
        } else {
            "Has recuperado ${"%,.4f".format(released)} $symbol sin penalización."
        }
        return notify(
            state.copy(crypto = state.crypto.copy(holdings = upsertHolding(state.crypto.holdings, newH))),
            if (penalty > 0) NotificationKind.WARNING else NotificationKind.SUCCESS,
            "🔓 Unstake",
            msg
        )
    }

    /**
     * +1/-1 sobre el contador de mineros del token. PERF FIX: ya no consulta
     * empleados de la empresa — los mineros son un agregado (Int) y no
     * Employee individuales, así que escala a millones sin romper el motor.
     *  - delta > 0: contrata UN minero nuevo si hay cash (cobra MINER_HIRE_COST).
     *  - delta < 0: simplemente decrementa el contador (despido sin coste).
     */
    fun assignMiners(state: GameState, symbol: String, delta: Int): GameState {
        if (!state.crypto.unlocked) return state
        val h = state.crypto.holdingOrEmpty(symbol)
        if (delta > 0) {
            // Cada +1 cuesta MINER_HIRE_COST.
            val canAfford = (state.company.cash / MINER_HIRE_COST).toInt()
            val hireN = delta.coerceAtMost(canAfford)
            if (hireN <= 0) return state
            val totalCost = hireN * MINER_HIRE_COST
            val newH = h.copy(minersAssigned = h.minersAssigned + hireN)
            return state.copy(
                company = state.company.copy(cash = state.company.cash - totalCost),
                crypto = state.crypto.copy(holdings = upsertHolding(state.crypto.holdings, newH))
            )
        } else if (delta < 0) {
            val newCount = (h.minersAssigned + delta).coerceAtLeast(0)
            val newH = h.copy(minersAssigned = newCount)
            return state.copy(crypto = state.crypto.copy(holdings = upsertHolding(state.crypto.holdings, newH)))
        }
        return state
    }

    fun claimMining(state: GameState, symbol: String): GameState {
        if (!state.crypto.unlocked) return state
        val h = state.crypto.holdingOrEmpty(symbol)
        if (h.miningPending <= 0.000001) return state
        val claimed = h.miningPending
        val newH = h.copy(miningPending = 0.0, amount = h.amount + claimed)
        return notify(
            state.copy(crypto = state.crypto.copy(holdings = upsertHolding(state.crypto.holdings, newH))),
            NotificationKind.SUCCESS,
            "⛏️ Mining claim",
            "Has reclamado ${"%,.4f".format(claimed)} $symbol minados."
        )
    }

    // ===================== Contratación directa de mineros =====================

    /** Coste único de fichaje por cada minero contratado vía atajo crypto. */
    const val MINER_HIRE_COST: Double = 800.0
    /** Salario mensual fijo de un minero contratado por el atajo. */
    const val MINER_MONTHLY_SALARY: Double = 350.0
    /** Skill base de un minero (suficiente para minar, no para fábricas top). */
    const val MINER_SKILL: Double = 0.7

    /** Cuántos mineros puede permitirse contratar el jugador con el cash actual. */
    fun maxAffordableMiners(state: GameState): Int {
        if (state.company.cash <= 0) return 0
        return (state.company.cash / MINER_HIRE_COST).toInt()
    }

    /**
     * Contrata `count` mineros para el token `symbol`. PERF FIX: los mineros
     * son un AGREGADO en `holding.minersAssigned` (Int), NO Employee individuales.
     * Esto permite escalar a millones de mineros sin romper el motor (antes
     * cada `groupBy {assignedBuildingId}` recorría todos los empleados cada
     * tick, con miles de mineros eso bloqueaba la app).
     *
     * Si `count` excede el cash disponible, se ajusta a lo permitido.
     * Si count <= 0 o el cripto está bloqueado, retorna sin cambios.
     */
    fun hireMiners(state: GameState, symbol: String, count: Int): GameState {
        if (!state.crypto.unlocked) {
            return notify(state, NotificationKind.ERROR, "🔒 Crypto bloqueado",
                "Aún no has desbloqueado el mercado cripto.")
        }
        if (count <= 0) return state
        val def = CryptoCatalog.byMatching(symbol)
            ?: return notify(state, NotificationKind.ERROR, "Token desconocido",
                "El símbolo $symbol no existe.")
        val affordable = maxAffordableMiners(state)
        val hireN = count.coerceAtMost(affordable)
        if (hireN <= 0) {
            return notify(state, NotificationKind.ERROR, "Sin fondos",
                "No tienes para fichar ni un minero (cuestan ${"%,.0f".format(MINER_HIRE_COST)} € cada uno).")
        }

        val totalCost = hireN * MINER_HIRE_COST

        // Solo se actualiza el contador del token — sin Employees individuales.
        val curHolding = state.crypto.holdingOrEmpty(symbol)
        val newH = curHolding.copy(minersAssigned = curHolding.minersAssigned + hireN)
        val newHoldings = upsertHolding(state.crypto.holdings, newH)

        val newCompany = state.company.copy(cash = state.company.cash - totalCost)

        val expectedDailyTokens = if (def.miningDifficulty > 0)
            hireN.toDouble() / def.miningDifficulty else 0.0

        return notify(
            state.copy(
                company = newCompany,
                crypto = state.crypto.copy(holdings = newHoldings)
            ),
            NotificationKind.SUCCESS,
            "⛏️ +$hireN mineros contratados (${def.symbol})",
            "Coste de fichaje: ${"%,.0f".format(totalCost)} €. " +
                "Nómina mensual: ${"%,.0f".format(hireN * MINER_MONTHLY_SALARY)} €. " +
                "Producción esperada: ${"%,.4f".format(expectedDailyTokens)} ${def.symbol}/día."
        )
    }

    /** Total de mineros agregados en todos los tokens. */
    fun totalMiners(state: GameState): Int =
        state.crypto.holdings.sumOf { it.minersAssigned }

    /** Coste DIARIO total de la nómina de mineros agregados. */
    fun dailyMinerPayroll(state: GameState): Double =
        totalMiners(state) * (MINER_MONTHLY_SALARY / 30.0)

    /**
     * Atajo "gasta todo el cash en mineros para este token". Calcula el
     * máximo permitido por el cash y los contrata.
     */
    fun hireMaxMiners(state: GameState, symbol: String): GameState {
        val n = maxAffordableMiners(state)
        if (n <= 0) {
            return notify(state, NotificationKind.ERROR, "Sin fondos",
                "Necesitas al menos ${"%,.0f".format(MINER_HIRE_COST)} € para fichar 1 minero.")
        }
        return hireMiners(state, symbol, n)
    }

    /**
     * Activa/desactiva el auto-sell de mining para un token. Cuando está
     * activo, en cada `dailyTick` el mineral producido se vende al precio
     * actual y el cash entra directamente a la empresa, sin pasar por
     * `miningPending` ni requerir Claim del jugador. Funciona offline.
     */
    fun toggleAutoSellMining(state: GameState, symbol: String, enabled: Boolean): GameState {
        if (!state.crypto.unlocked) return state
        if (CryptoCatalog.byMatching(symbol) == null) return state
        val h = state.crypto.holdingOrEmpty(symbol)
        if (h.autoSellMining == enabled) return state
        val newH = h.copy(autoSellMining = enabled)
        val msg = if (enabled)
            "Auto-mine activado en $symbol. Cada día se venderá la producción al precio actual y el cash entrará en caja, también offline."
        else "Auto-mine desactivado en $symbol. La producción se acumula en pending y necesita Claim manual."
        return notify(
            state.copy(crypto = state.crypto.copy(holdings = upsertHolding(state.crypto.holdings, newH))),
            if (enabled) NotificationKind.SUCCESS else NotificationKind.INFO,
            "🤖 Auto-mine $symbol",
            msg
        )
    }

    // ===================== Helpers =====================

    private fun upsertHolding(list: List<CryptoHolding>, h: CryptoHolding): List<CryptoHolding> {
        val idx = list.indexOfFirst { it.symbol == h.symbol }
        return if (idx >= 0) list.toMutableList().also { it[idx] = h }
        else list + h
    }

    private fun pushHistory(history: List<Double>, v: Double): List<Double> {
        val merged = history + v
        return if (merged.size > HISTORY_LEN) merged.takeLast(HISTORY_LEN) else merged
    }

    private fun Random.nextGaussian(): Double {
        // Box-Muller (no usamos java.util.Random aquí para no desincronizar el seed).
        val u1 = nextDouble().coerceAtLeast(1e-9)
        val u2 = nextDouble()
        return sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)
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
