package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.random.Random

/**
 * Motor puro: recibe un GameState y devuelve otro. No hace IO ni toca
 * Android directamente. Esto permite testear y reaplicar ticks al cargar.
 */
object GameEngine {

    /** Avanza N segundos de simulación. N alto se usa para "offline progress". */
    fun advanceSeconds(state: GameState, seconds: Long): GameState {
        if (seconds <= 0) return state
        var s = state
        val rng = Random(state.rngSeed xor state.tick)
        // micro-batching: el mercado se actualiza cada 10s para no quemar CPU
        for (i in 0 until seconds) {
            s = advanceOneSecond(s, rng)
        }
        return s
    }

    private fun advanceOneSecond(state: GameState, rng: Random): GameState {
        val nextTick = state.tick + 1
        val prevDay = state.day

        // 1) producción (FIX BUG-07-01/02: ahora resuelve adv_* y deposita
        // calidad en qualityInventory)
        val prod = Production.advance(
            state.company,
            state.research,
            state.player,
            state.qualityInventory,
            state.rngSeed + nextTick
        )
        var company = prod.company
        var qualityInventory = prod.qualityInventory
        var player = state.player.addXp(prod.xpEarned)

        // 2) investigación en curso
        var research = state.research
        research.inProgressId?.let { techId ->
            val left = research.inProgressSecondsLeft - (1.0 +
                    state.player.stats.intelligence * 0.02)
            if (left <= 0) {
                research = research.copy(
                    completed = research.completed + techId,
                    inProgressId = null,
                    inProgressSecondsLeft = 0.0
                )
                player = player.addXp(250)
                company = company.addXp(500)
            } else {
                research = research.copy(inProgressSecondsLeft = left)
            }
        }

        // 3) mercado (cada 10s)
        var market = state.market
        if (nextTick % 10 == 0L) {
            market = Economy.tickMarket(market, rng)
        }

        // 4) bolsa (cada 15s)
        var stocks = state.stocks
        if (nextTick % 15 == 0L) {
            stocks = Economy.tickStocks(stocks, rng)
        }

        // 5) ciclo de día in-game (1.440 ticks)
        var notifications = state.notifications
        var realEstate = state.realEstate
        if (nextTick % 1_440 == 0L) {
            // rentas inmobiliarias
            val rent = realEstate.dailyNet
            if (rent != 0.0) {
                company = company.copy(cash = company.cash + rent)
            }
            // nóminas diarias
            val payroll = Payroll.applyDaily(company, nextTick.toInt() / 1_440)
            company = payroll.company
            notifications = (notifications + payroll.notifications).takeLast(40)

            // churn (rotación)
            val ch = Payroll.churn(company, rng)
            company = ch.company
            notifications = (notifications + ch.notifications).takeLast(40)

            // tiro de evento con 25% de probabilidad si no hay otro activo
            if (state.activeEventId == null && rng.nextDouble() < 0.25) {
                val event = EventPool.pool.random(rng)
                return state.copy(
                    tick = nextTick,
                    player = player,
                    company = company,
                    qualityInventory = qualityInventory,
                    market = market,
                    stocks = stocks,
                    research = research,
                    realEstate = realEstate,
                    activeEventId = event.id,
                    notifications = notifications
                )
            }
        }

        // 6) regeneración de energía del jugador: +1 cada 30s
        if (nextTick % 30 == 0L && player.energy < player.maxEnergy) {
            player = player.withEnergy(+1)
        }

        // 7) autoverificación de misiones
        val (quests, questNotifs, questCashBonus, questXpBonus, questRepBonus) =
            QuestEngine.evaluate(state.quests, company, player, research, realEstate, state.holdings)
        company = company.copy(
            cash = company.cash + questCashBonus,
            reputation = (company.reputation + questRepBonus).coerceIn(0, 100)
        )
        player = player.addXp(questXpBonus)
        if (questNotifs.isNotEmpty()) notifications = (notifications + questNotifs).takeLast(40)

        // ===== Hooks de subsistemas extendidos =====
        var s2 = state.copy(
            tick = nextTick,
            player = player,
            company = company,
            qualityInventory = qualityInventory,
            market = market,
            stocks = stocks,
            research = research,
            realEstate = realEstate,
            notifications = notifications,
            quests = quests
        )

        // 8) Macroeconomía: ciclos diarios + noticias por tick
        if (nextTick % 1_440 == 0L) {
            s2 = s2.copy(economy = EconomicEngine.tickPhase(s2.economy, rng))
            s2 = s2.copy(market = EconomicEngine.applyPhaseToMarket(s2.market, s2.economy.currentPhase, rng))
            s2 = s2.copy(news = EconomicEngine.pruneFeed(s2.news, s2.day + 1))
        }
        EconomicEngine.generateNewsTick(s2.economy, rng, nextTick, s2.day)?.let { item ->
            val (m2, applied) = EconomicEngine.applyNewsToMarket(s2.market, item)
            var newsFeed = s2.news.copy(
                items = (s2.news.items + applied).takeLast(60),
                unreadCount = s2.news.unreadCount + 1
            )
            var notifs = s2.notifications
            if (EconomicEngine.shouldNotify(applied)) {
                notifs = (notifs + EconomicEngine.toGameNotification(applied)).takeLast(40)
            }
            s2 = s2.copy(market = m2, news = newsFeed, notifications = notifs)
        }

        // 9) Contratos: auto-deliver cada 60s + refresh diario
        if (nextTick % 60 == 0L || nextTick % 1_440 == 0L) {
            s2 = ContractsEngine.tickContracts(s2, rng)
        }

        // 10) Banca + IPO + Opciones
        if (nextTick % 1_440 == 0L) s2 = BankingEngine.tickLoans(s2)
        s2 = BankingEngine.maybeRefreshOffers(s2, rng)
        s2 = IpoEngine.tickCompanyStock(s2, rng)
        s2 = OptionsEngine.tickOptions(s2)

        // 11) RRHH: refresh y churn diario, training cada 60s
        if (nextTick % 1_440 == 0L) {
            s2 = HrEngine.refreshApplicants(s2, rng)
            s2 = HrEngine.churnHr(s2, rng)
        }
        if (nextTick % 60 == 0L) {
            s2 = HrEngine.tickTraining(s2)
        }

        // 12) Líneas de producción: orquestación cada 5s
        if (nextTick % 5 == 0L) {
            s2 = ProductionLinesEngine.tickLines(s2, rng)
        }

        // 13) Narrativa: capítulo + side quests + encuentros NPC
        s2 = StorylineEngine.checkChapterTrigger(s2)
        s2 = SideQuestEngine.checkProgress(s2)
        if (nextTick % 1_440 == 0L) {
            s2 = SideQuestEngine.expireOverdue(s2)
            s2 = SideQuestEngine.refreshAvailable(s2, rng)
            s2 = NPCEngine.maybeRandomEncounter(s2, rng)
        }

        // 14) Logros (revisar siempre — son baratos)
        val (achState, achNotifs) = AchievementEngine.evaluate(s2)
        if (achNotifs.isNotEmpty()) {
            s2 = s2.copy(notifications = (s2.notifications + achNotifs).takeLast(40))
        }
        s2 = s2.copy(achievements = achState)

        // 15) Rivales (cada 60s)
        if (nextTick % 60 == 0L) s2 = RivalEngine.checkChallenges(s2)
        if (nextTick % 3_600 == 0L && rng.nextDouble() < 0.4) {
            s2 = RivalEngine.pushTrashTalk(s2, rng)
        }

        // 16) Gerentes: aplicar automatización + refresh diario del pool
        s2 = ManagerEngine.tick(s2, rng)
        if (nextTick % 1_440 == 0L) {
            s2 = ManagerEngine.refreshPool(s2, rng)
        }

        // 17) BalWealth Index: derivar cada 5s + bump contadores diarios
        if (nextTick % 5 == 0L) {
            s2 = s2.copy(balWealth = BalWealth.derive(s2))
        }
        // FIX BUG-09-#6: contadores de permanencia avanzan SOLO al cambio de día
        if (nextTick % 1_440 == 0L) {
            s2 = s2.copy(balWealth = BalWealth.bumpDailyCounters(s2.balWealth))
        }

        // 17a) FAMILIA: aging diario (FIX BUG-03-FAM-01/02).
        // Spouse.daysWith +1; Child.ageDays +1. happiness con leve decay.
        if (nextTick % 1_440 == 0L) {
            val fam = s2.family
            val newSpouse = fam.spouse?.let { sp ->
                sp.copy(
                    daysWith = sp.daysWith + 1,
                    happiness = (sp.happiness - 1).coerceAtLeast(0)  // FIX BUG-03-FAM-04
                )
            }
            val newChildren = fam.children.map { c -> c.copy(ageDays = c.ageDays + 1) }
            s2 = s2.copy(family = fam.copy(spouse = newSpouse, children = newChildren))
        }

        // 17b) DIVIDENDOS de StockCatalog (acciones que el jugador posee).
        // FIX feedback usuario: pagos diarios eran imperceptibles. Repartimos
        // el yield anual en 52 partes (pago semanal cobrado diario), así el
        // efecto se nota en la economía sin esperar 365 días.
        if (nextTick % 1_440 == 0L && s2.holdings.shares.isNotEmpty()) {
            var totalDividends = 0.0
            for (stock in s2.stocks) {
                val shares = s2.holdings.shares[stock.ticker] ?: 0
                if (shares <= 0 || stock.annualDividendYield <= 0) continue
                val daily = stock.price * stock.annualDividendYield / 52.0
                totalDividends += daily * shares
            }
            if (totalDividends > 0.0) {
                s2 = s2.copy(
                    company = s2.company.copy(cash = s2.company.cash + totalDividends),
                    notifications = (s2.notifications + GameNotification(
                        id = System.nanoTime(),
                        timestamp = System.currentTimeMillis(),
                        kind = NotificationKind.SUCCESS,
                        title = "📈 Dividendos cobrados",
                        message = "Has recibido ${"%,.2f".format(totalDividends)} € de tus acciones."
                    )).takeLast(40)
                )
            }
        }

        // 18) Karma echo: suaviza visuales según karma actual
        if (nextTick % 3 == 0L) {
            s2 = s2.copy(karmaEcho = com.empiretycoon.game.world.KarmaEchoEngine.tick(s2.karmaEcho, s2.storyline.karma))
        }

        // 19) NPCs world: poblar SOLO si está vacío y cada 5 minutos máximo.
        // FIX BUG-18-04: si ensurePopulated falla (sin tiles walkable libres),
        // volverá a llamarse en cada tick si no limitamos por tiempo.
        if (s2.npcWorld.walkers.isEmpty() && nextTick % 300L == 0L) {
            s2 = s2.copy(
                npcWorld = com.empiretycoon.game.world.NpcWorldEngine.ensurePopulated(
                    s2.npcWorld, s2.world.grid, target = 18
                )
            )
        }
        if (nextTick % 2 == 0L) {
            val moved = com.empiretycoon.game.world.NpcWorldEngine.tick(s2.npcWorld, s2.world.grid, 0.12f, nextTick)
            s2 = s2.copy(npcWorld = moved)
        }

        // 20) Clima: cambia cada 5-15 minutos in-game
        s2 = s2.copy(weather = com.empiretycoon.game.world.WeatherEngine.tick(s2.weather, nextTick, rng))

        // 21) Tráfico: poblar SOLO si vacío y cada 5 minutos máximo.
        if (s2.traffic.vehicles.isEmpty() && nextTick % 300L == 0L) {
            s2 = s2.copy(
                traffic = com.empiretycoon.game.world.TrafficEngine.ensurePopulated(
                    s2.traffic, s2.world.grid, target = 22
                )
            )
        }
        s2 = s2.copy(
            traffic = com.empiretycoon.game.world.TrafficEngine.tick(s2.traffic, s2.world.grid, 0.12f)
        )

        // 21b) Formula Manager: tick diario (sponsor, salarios, simulación)
        if (s2.racing.unlocked && nextTick % 1_440L == 0L) {
            s2 = RacingEngine.dailyTick(s2, s2.day, rng)
        }

        // 22a) Mascotas que siguen al avatar (FIX BUG-03-PET-01: walkable check)
        s2 = s2.copy(pets = com.empiretycoon.game.world.PetEngine.tick(
            s2.pets, s2.world.avatar.x, s2.world.avatar.y, 0.5f, nextTick, s2.world.grid
        ))

        // 22b) Follower NPC: spawn + tick (FIX BUG-03-FOL-01/02: walkable check)
        s2 = s2.copy(follower = com.empiretycoon.game.world.FollowerEngine.tick(
            s2.follower, s2.world.avatar.x, s2.world.avatar.y, nextTick, s2.world.grid
        ))
        if (nextTick % 5 == 0L) {
            s2 = s2.copy(follower = com.empiretycoon.game.world.FollowerEngine.maybeSpawn(
                s2.follower, s2.world.avatar.x, s2.world.avatar.y, nextTick, rng, s2.world.grid
            ))
        }

        // 22c) UFOs (1 cada ~50 min in-game)
        s2 = s2.copy(ufo = com.empiretycoon.game.world.UfoEngine.tick(
            s2.ufo, s2.world.avatar.x, s2.world.avatar.y, nextTick, rng
        ))

        // 23) Eventos aleatorios al pisar tile (usa pool extendido de 50)
        // FIX BUG-18-05: si hay evento activo > EXPIRE_TICKS, se auto-descarta
        // para no bloquear el spawn de futuros eventos.
        if (s2.worldEvent.activeEventId != null) {
            val held = nextTick - s2.worldEvent.activatedAtTick
            if (held >= com.empiretycoon.game.world.WorldEventState.EXPIRE_TICKS) {
                s2 = s2.copy(worldEvent = s2.worldEvent.copy(activeEventId = null))
            }
        }
        if (nextTick % 60 == 0L && s2.worldEvent.activeEventId == null) {
            val sinceLast = nextTick - s2.worldEvent.lastEventTick
            if (sinceLast >= 60 && rng.nextDouble() < 0.20) {
                val pool = com.empiretycoon.game.world.MoreEventsCatalog.merged
                val available = pool.filter { it.id !in s2.worldEvent.seenIds }
                val ev = if (available.isEmpty()) pool.random(rng) else available.random(rng)
                s2 = s2.copy(worldEvent = s2.worldEvent.copy(
                    activeEventId = ev.id,
                    lastEventTick = nextTick,
                    activatedAtTick = nextTick
                ))
            }
        }

        return s2
    }

    // ---------- COMANDOS (acciones del jugador) ----------

    fun buildNew(state: GameState, type: BuildingType): GameState {
        val existingOfType = state.company.buildings.count { it.type == type }
        val cost = type.costAtLevel(existingOfType + 1)
        if (state.company.cash < cost) return notify(state, NotificationKind.ERROR, "Sin fondos",
            "No puedes pagar ${type.displayName} (necesitas ${"%,.0f".format(cost)}).")
        val id = "b_${state.tick}_${state.company.buildings.size}"
        val newB = Building(id = id, type = type)
        val newCompany = state.company.copy(
            buildings = state.company.buildings + newB,
            cash = state.company.cash - cost
        )
        return state.copy(company = newCompany.addXp(50))
    }

    fun upgradeBuilding(state: GameState, buildingId: String): GameState {
        val b = state.company.buildings.find { it.id == buildingId } ?: return state
        val cost = b.type.costAtLevel(b.level + 1)
        if (state.company.cash < cost) return notify(state, NotificationKind.ERROR, "Sin fondos",
            "No puedes mejorar (necesitas ${"%,.0f".format(cost)}).")
        val upgraded = b.copy(level = b.level + 1)
        val bs = state.company.buildings.map { if (it.id == buildingId) upgraded else it }
        return state.copy(company = state.company.copy(
            buildings = bs, cash = state.company.cash - cost).addXp(80))
    }

    fun demolish(state: GameState, buildingId: String): GameState {
        val b = state.company.buildings.find { it.id == buildingId } ?: return state
        val refund = b.type.baseCost * 0.3 * b.level
        val emps = state.company.employees.map {
            if (it.assignedBuildingId == buildingId) it.copy(assignedBuildingId = null) else it
        }
        val company = state.company.copy(
            buildings = state.company.buildings.filterNot { it.id == buildingId },
            cash = state.company.cash + refund,
            employees = emps
        )
        return state.copy(company = company)
    }

    fun setRecipe(state: GameState, buildingId: String, recipeId: String?): GameState {
        val bs = state.company.buildings.map {
            if (it.id == buildingId) it.copy(currentRecipeId = recipeId, progressSeconds = 0.0) else it
        }
        return state.copy(company = state.company.copy(buildings = bs))
    }

    fun toggleAutoRestart(state: GameState, buildingId: String): GameState {
        val bs = state.company.buildings.map {
            if (it.id == buildingId) it.copy(autoRestart = !it.autoRestart) else it
        }
        return state.copy(company = state.company.copy(buildings = bs))
    }

    fun assignWorkersDelta(state: GameState, buildingId: String, delta: Int): GameState {
        val b = state.company.buildings.find { it.id == buildingId } ?: return state
        val newAssigned = (b.assignedWorkers + delta).coerceIn(0, b.workerCapacity)
        // si incrementamos, necesitamos empleados libres
        val freeEmployees = state.company.employees.count { it.assignedBuildingId == null }
        val effectiveDelta = newAssigned - b.assignedWorkers
        if (effectiveDelta > freeEmployees) return state
        // asignar/desasignar ids concretos
        val emps = state.company.employees.toMutableList()
        if (effectiveDelta > 0) {
            var remaining = effectiveDelta
            for (i in emps.indices) {
                if (remaining == 0) break
                if (emps[i].assignedBuildingId == null) {
                    emps[i] = emps[i].copy(assignedBuildingId = buildingId)
                    remaining--
                }
            }
        } else if (effectiveDelta < 0) {
            var remaining = -effectiveDelta
            for (i in emps.indices) {
                if (remaining == 0) break
                if (emps[i].assignedBuildingId == buildingId) {
                    emps[i] = emps[i].copy(assignedBuildingId = null)
                    remaining--
                }
            }
        }
        val bs = state.company.buildings.map {
            if (it.id == buildingId) it.copy(assignedWorkers = newAssigned) else it
        }
        return state.copy(company = state.company.copy(buildings = bs, employees = emps))
    }

    // --- Mercado ---

    fun marketBuy(state: GameState, resourceId: String, qty: Int): GameState {
        val price = state.market.buyPriceOf(resourceId)
        val total = price * qty
        if (state.company.cash < total)
            return notify(state, NotificationKind.ERROR, "Compra rechazada",
                "Fondos insuficientes.")
        if (state.company.inventoryCount() + qty > state.company.effectiveCapacity())
            return notify(state, NotificationKind.WARNING, "Sin espacio",
                "El almacén está lleno.")
        val inv = state.company.inventory + (resourceId to (state.inventoryOf(resourceId) + qty))
        val company = state.company.copy(
            cash = state.company.cash - total,
            inventory = inv
        )
        return state.copy(
            company = company,
            market = Economy.applyPurchase(state.market, resourceId, qty)
        )
    }

    fun marketSell(state: GameState, resourceId: String, qty: Int): GameState {
        val have = state.inventoryOf(resourceId)
        if (have < qty) return state
        val rawPrice = state.market.sellPriceOf(resourceId)
        // bono de marketing + carisma
        val mktBonus = 1.0 +
            state.research.completed.sumOf { id -> TechCatalog.byId(id)?.marketBonus ?: 0.0 } +
            state.player.stats.charisma.coerceAtMost(100) * 0.003
        val price = rawPrice * mktBonus
        val total = price * qty
        val inv = state.company.inventory + (resourceId to (have - qty))
        val company = state.company.copy(
            cash = state.company.cash + total,
            inventory = inv
        ).addXp((total / 20).toLong())
        return state.copy(
            company = company,
            market = Economy.applySale(state.market, resourceId, qty)
        )
    }

    // --- Investigación ---

    fun startResearch(state: GameState, techId: String): GameState {
        val tech = TechCatalog.byId(techId) ?: return state
        if (!state.research.canStart(tech)) return state
        if (state.company.cash < tech.cost) return notify(state,
            NotificationKind.ERROR, "Investigación cara",
            "No tienes ${"%,.0f".format(tech.cost)} en caja.")
        val company = state.company.copy(cash = state.company.cash - tech.cost)
        val res = state.research.copy(
            inProgressId = techId,
            inProgressSecondsLeft = tech.researchSeconds.toDouble()
        )
        return state.copy(company = company, research = res)
    }

    // --- Bolsa ---

    fun buyShares(state: GameState, ticker: String, qty: Int): GameState {
        // FIX BUG-04-09: qty <= 0 = exploit free money. Bloqueamos.
        if (qty <= 0) return state
        val s = state.stocks.find { it.ticker == ticker } ?: return state
        // Guard contra precio inválido (BUG-04-02)
        if (s.price <= 0.0 || s.price.isNaN() || s.price.isInfinite()) return state
        val total = s.price * qty
        if (state.company.cash < total) return notify(state, NotificationKind.ERROR,
            "Orden rechazada", "Fondos insuficientes para comprar $qty x ${s.ticker}.")
        val company = state.company.copy(cash = state.company.cash - total)
        val currShares = state.holdings.shares[ticker] ?: 0
        val currAvg = state.holdings.avgCost[ticker] ?: 0.0
        val newShares = currShares + qty
        val newAvg = if (newShares == 0) 0.0
            else (currAvg * currShares + s.price * qty) / newShares
        val holdings = state.holdings.copy(
            shares = state.holdings.shares + (ticker to newShares),
            avgCost = state.holdings.avgCost + (ticker to newAvg)
        )
        return state.copy(company = company, holdings = holdings)
    }

    fun sellShares(state: GameState, ticker: String, qty: Int): GameState {
        // FIX BUG-04-09: qty negativo = sell -10 → cash += 10*price (exploit)
        if (qty <= 0) return state
        val s = state.stocks.find { it.ticker == ticker } ?: return state
        val have = state.holdings.shares[ticker] ?: 0
        if (have < qty) return state
        val total = s.price * qty
        val company = state.company.copy(cash = state.company.cash + total)
        val newShares = have - qty
        val holdings = state.holdings.copy(
            shares = state.holdings.shares + (ticker to newShares)
        )
        return state.copy(company = company, holdings = holdings)
    }

    // --- Contratación ---

    fun refreshCandidates(state: GameState): GameState {
        val rng = Random(state.rngSeed xor state.tick)
        val tier = (state.company.reputation / 20).coerceIn(1, 5)
        val c = EmployeeFactory.generateCandidates(tier, rng, count = 6)
        return state.copy(candidates = c)
    }

    fun hire(state: GameState, candidateId: String): GameState {
        val c = state.candidates.find { it.id == candidateId } ?: return state
        val signingCost = c.monthlySalary * 0.5
        if (state.company.cash < signingCost)
            return notify(state, NotificationKind.ERROR, "Contratación fallida",
                "No puedes pagar la prima de fichaje.")
        val company = state.company.copy(
            cash = state.company.cash - signingCost,
            employees = state.company.employees + c
        )
        return state.copy(
            company = company,
            candidates = state.candidates - c
        )
    }

    fun fire(state: GameState, employeeId: String): GameState {
        val emp = state.company.employees.find { it.id == employeeId } ?: return state
        val severance = emp.monthlySalary
        val company = state.company.copy(
            cash = state.company.cash - severance,
            employees = state.company.employees - emp,
            buildings = state.company.buildings.map {
                if (emp.assignedBuildingId == it.id)
                    it.copy(assignedWorkers = (it.assignedWorkers - 1).coerceAtLeast(0))
                else it
            },
            reputation = (state.company.reputation - 1).coerceAtLeast(0)
        )
        return state.copy(company = company)
    }

    // --- Inmuebles ---

    fun refreshRealEstate(state: GameState): GameState {
        val rng = Random(state.rngSeed xor state.tick)
        val count = 6
        val available = (0 until count).map {
            val type = PropertyType.values().random(rng)
            val vr = rng.nextDouble(0.85, 1.25)
            Property(
                id = "p_${state.tick}_$it",
                type = type,
                nickname = listOf(
                    "Centro","Ensanche","Playa","Barrio Alto",
                    "Orilla","Casco Viejo","La Colina","Junto al río",
                    "Torre Norte","Plaza Mayor").random(rng),
                purchasePrice = type.basePrice * vr,
                rentPerDay = type.baseRentPerDay * rng.nextDouble(0.9, 1.25),
                maintenancePerDay = type.baseRentPerDay * 0.1
            )
        }
        return state.copy(realEstate = state.realEstate.copy(available = available))
    }

    fun buyProperty(state: GameState, propertyId: String): GameState {
        val p = state.realEstate.available.find { it.id == propertyId } ?: return state
        if (state.company.cash < p.purchasePrice) return notify(state,
            NotificationKind.ERROR, "Compra rechazada", "Faltan fondos.")
        val company = state.company.copy(cash = state.company.cash - p.purchasePrice)
        val re = state.realEstate.copy(
            owned = state.realEstate.owned + p,
            available = state.realEstate.available - p
        )
        return state.copy(company = company, realEstate = re)
    }

    fun sellProperty(state: GameState, propertyId: String): GameState {
        val p = state.realEstate.owned.find { it.id == propertyId } ?: return state
        val sellPrice = p.purchasePrice * 0.92
        val company = state.company.copy(cash = state.company.cash + sellPrice)
        val re = state.realEstate.copy(
            owned = state.realEstate.owned - p
        )
        return state.copy(company = company, realEstate = re)
    }

    // --- Eventos ---

    fun resolveEvent(state: GameState, choiceIndex: Int): GameState {
        val ev = EventPool.pool.find { it.id == state.activeEventId } ?: return state
        val c = ev.choices.getOrNull(choiceIndex) ?: return state
        var company = state.company.copy(
            cash = state.company.cash + c.cashDelta,
            reputation = (state.company.reputation + c.reputationDelta).coerceIn(0, 100)
        )
        if (c.inventoryDelta.isNotEmpty()) {
            val inv = HashMap(company.inventory)
            for ((k, v) in c.inventoryDelta) inv[k] = (inv[k] ?: 0) + v
            company = company.copy(inventory = inv)
        }
        var player = state.player.addXp(c.xpDelta)
            .withEnergy(c.energyDelta)
            .withHappiness(c.happinessDelta)
        val notif = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.EVENT,
            title = ev.title,
            message = c.message.ifBlank { c.label }
        )
        return state.copy(
            company = company,
            player = player,
            activeEventId = null,
            notifications = (state.notifications + notif).takeLast(40)
        )
    }

    // --- Jugador / entrenamiento ---

    fun train(state: GameState, which: String): GameState {
        val player = state.player
        val energyCost = 10
        if (player.energy < energyCost) return notify(state, NotificationKind.WARNING,
            "Sin energía", "Necesitas descansar.")
        val stats = player.stats
        val newStats = when (which) {
            "int" -> stats.copy(intelligence = stats.intelligence + 1)
            "str" -> stats.copy(strength = stats.strength + 1)
            "cha" -> stats.copy(charisma = stats.charisma + 1)
            "luc" -> stats.copy(luck = stats.luck + 1)
            "dex" -> stats.copy(dexterity = stats.dexterity + 1)
            else -> stats
        }
        val newPlayer = player.copy(stats = newStats)
            .withEnergy(-energyCost)
            .addXp(20)
        return state.copy(player = newPlayer)
    }

    fun rest(state: GameState): GameState {
        val p = state.player.withEnergy(+30).withHappiness(+5)
        return state.copy(player = p)
    }

    fun work(state: GameState): GameState {
        val p = state.player
        if (p.energy < 15) return notify(state, NotificationKind.WARNING,
            "Sin energía", "Descansa antes de trabajar.")
        val salary = 30.0 + p.stats.intelligence * 5 + p.stats.charisma * 3
        val player = p.withEnergy(-15).addXp(25).copy(cash = p.cash + salary)
        return state.copy(player = player)
    }

    fun setPersonalToCompany(state: GameState, amount: Double): GameState {
        val a = amount.coerceAtMost(state.player.cash).coerceAtLeast(0.0)
        val p = state.player.copy(cash = state.player.cash - a)
        val c = state.company.copy(cash = state.company.cash + a)
        return state.copy(player = p, company = c)
    }

    fun withdrawToPersonal(state: GameState, amount: Double): GameState {
        val a = amount.coerceAtMost(state.company.cash).coerceAtLeast(0.0)
        val p = state.player.copy(cash = state.player.cash + a)
        val c = state.company.copy(cash = state.company.cash - a)
        return state.copy(player = p, company = c)
    }

    fun rename(state: GameState, player: String?, company: String?): GameState {
        return state.copy(
            player = player?.let { state.player.copy(name = it) } ?: state.player,
            company = company?.let { state.company.copy(name = it) } ?: state.company
        )
    }

    fun togglePause(state: GameState): GameState = state.copy(paused = !state.paused)

    fun setSpeed(state: GameState, speed: Int): GameState =
        state.copy(speedMultiplier = speed.coerceIn(1, 8))

    fun claimQuest(state: GameState, questId: String): GameState {
        val q = state.quests.find { it.id == questId && it.completed && !it.claimed }
            ?: return state
        val quests = state.quests.map { if (it.id == questId) it.copy(claimed = true) else it }
        val company = state.company.copy(
            cash = state.company.cash + q.rewardCash,
            reputation = (state.company.reputation + q.rewardReputation).coerceIn(0, 100)
        )
        val player = state.player.addXp(q.rewardXp)
        val notif = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.SUCCESS,
            title = "Misión completada",
            message = "${q.title}: recompensa cobrada."
        )
        return state.copy(
            quests = quests, company = company, player = player,
            notifications = (state.notifications + notif).takeLast(40)
        )
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
