package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.random.Random

/**
 * Motor de automatización: cada tick aplica las decisiones de los gerentes
 * contratados y activos, respetando sus cooldowns y configuración.
 *
 * Diseño:
 * - Cada gerente actúa como mucho una vez cada `cooldownSeconds` ticks.
 * - Las decisiones son acotadas (1 venta, 1 compra, 1 upgrade por ciclo)
 *   para evitar spikes y para que sean visibles al jugador.
 * - El gerente NUNCA gasta por debajo del `cashReserve` configurado.
 */
object ManagerEngine {

    /** Aplica todos los gerentes activos. Llamar cada tick. */
    fun tick(state: GameState, rng: Random): GameState {
        if (state.managers.hired.isEmpty()) return state
        var s = state
        for (mgr in state.managers.hired.toList()) {
            if (!mgr.enabled || !mgr.hired) continue
            val ticksSinceLast = state.tick - mgr.lastActionTick
            if (ticksSinceLast < mgr.cooldownSeconds) continue
            s = when (mgr.type) {
                ManagerType.OPERATIONS -> applyOperations(s, mgr)
                ManagerType.SALES -> applySales(s, mgr)
                ManagerType.PURCHASING -> applyPurchasing(s, mgr)
                ManagerType.HR -> applyHr(s, mgr, rng)
                ManagerType.FINANCE -> applyFinance(s, mgr)
            }
        }
        return s
    }

    /** Refresca el pool de candidatos (diario). */
    fun refreshPool(state: GameState, rng: Random): GameState {
        val tier = (state.company.reputation / 25).coerceIn(1, 4)
        val pool = ManagerFactory.freshPool(rng, tier)
        return state.copy(managers = state.managers.copy(pool = pool))
    }

    /** Contratar un gerente del pool. */
    fun hire(state: GameState, managerId: String): GameState {
        val mgr = state.managers.pool.find { it.id == managerId } ?: return state
        if (state.managers.availableSlots <= 0) {
            return notify(state, NotificationKind.WARNING, "Sin plazas",
                "Has llenado tus plazas de gerentes. Despide a uno o sube tu nivel de prestigio.")
        }
        if (state.managers.has(mgr.type)) {
            return notify(state, NotificationKind.WARNING, "Puesto ocupado",
                "Ya tienes un ${mgr.type.displayName}. Despide al actual primero.")
        }
        if (state.company.cash < mgr.signingCost) {
            return notify(state, NotificationKind.ERROR, "Sin fondos",
                "Faltan ${"%,.0f".format(mgr.signingCost - state.company.cash)} € para fichar.")
        }
        val hired = mgr.copy(hired = true, hiredAtTick = state.tick, enabled = true)
        val newState = state.managers.copy(
            pool = state.managers.pool.filterNot { it.id == managerId },
            hired = state.managers.hired + hired
        )
        val notif = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.SUCCESS,
            title = "${mgr.type.emoji} Nuevo fichaje",
            message = "${mgr.name} se incorpora como ${mgr.type.displayName}."
        )
        return state.copy(
            managers = newState,
            company = state.company.copy(cash = state.company.cash - mgr.signingCost),
            notifications = (state.notifications + notif).takeLast(40)
        )
    }

    fun fire(state: GameState, managerId: String): GameState {
        val mgr = state.managers.hired.find { it.id == managerId } ?: return state
        val severance = mgr.monthlySalary * 1.5
        val notif = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.INFO,
            title = "${mgr.type.emoji} Despido",
            message = "${mgr.name} ha sido despedido. Indemnización: ${"%,.0f".format(severance)} €."
        )
        return state.copy(
            managers = state.managers.copy(
                hired = state.managers.hired.filterNot { it.id == managerId }
            ),
            company = state.company.copy(cash = state.company.cash - severance),
            notifications = (state.notifications + notif).takeLast(40)
        )
    }

    fun toggleEnabled(state: GameState, managerId: String): GameState {
        val updated = state.managers.hired.map {
            if (it.id == managerId) it.copy(enabled = !it.enabled) else it
        }
        return state.copy(managers = state.managers.copy(hired = updated))
    }

    fun updateConfig(state: GameState, managerId: String, config: ManagerConfig): GameState {
        val updated = state.managers.hired.map {
            if (it.id == managerId) it.copy(config = config) else it
        }
        return state.copy(managers = state.managers.copy(hired = updated))
    }

    /** Subir nivel del gerente (cuesta cash creciente). */
    fun upgradeManager(state: GameState, managerId: String): GameState {
        val mgr = state.managers.hired.find { it.id == managerId } ?: return state
        val cost = mgr.signingCost * 1.5
        if (state.company.cash < cost) {
            return notify(state, NotificationKind.ERROR, "Sin fondos",
                "Faltan ${"%,.0f".format(cost - state.company.cash)} € para mejorar al gerente.")
        }
        val upgraded = mgr.copy(
            level = mgr.level + 1,
            efficiency = (mgr.efficiency * 1.10).coerceAtMost(2.0)
        )
        val updated = state.managers.hired.map {
            if (it.id == managerId) upgraded else it
        }
        return state.copy(
            managers = state.managers.copy(hired = updated),
            company = state.company.copy(cash = state.company.cash - cost)
        )
    }

    // ======================================================================
    //                          ACCIONES POR TIPO
    // ======================================================================

    /** Operations: mejorar 1 edificio si caja > 2x coste y level < maxLevel. */
    private fun applyOperations(state: GameState, mgr: Manager): GameState {
        val candidates = state.company.buildings
            .filter { it.level < mgr.config.maxBuildingLevel }
            .sortedBy { it.type.costAtLevel(it.level + 1) }  // primero el más barato
        for (b in candidates) {
            val cost = b.type.costAtLevel(b.level + 1)
            val cashAfter = state.company.cash - cost
            if (cashAfter >= mgr.config.cashReserve) {
                val updated = b.copy(level = b.level + 1)
                val newBuildings = state.company.buildings.map { if (it.id == b.id) updated else it }
                val notif = GameNotification(
                    id = System.nanoTime(),
                    timestamp = System.currentTimeMillis(),
                    kind = NotificationKind.INFO,
                    title = "${mgr.portrait} Mejora automática",
                    message = "${b.type.displayName} subido a nivel ${updated.level} (${"%,.0f".format(cost)} €)"
                )
                return state.copy(
                    company = state.company.copy(
                        buildings = newBuildings,
                        cash = state.company.cash - cost
                    ).addXp(80),
                    managers = bumpAction(state.managers, mgr.id, state.tick),
                    notifications = (state.notifications + notif).takeLast(40)
                )
            }
        }
        return state.copy(managers = bumpAction(state.managers, mgr.id, state.tick))
    }

    /** Sales: vender 1 lote del recurso con mejor precio en este momento.
     *  Respeta whitelist y protege inputs de recetas activas. */
    private fun applySales(state: GameState, mgr: Manager): GameState {
        // Inputs en uso por recetas activas (no se venden si protectActiveRecipeInputs)
        val activeInputIds: Set<String> = if (mgr.config.protectActiveRecipeInputs) {
            state.company.buildings.mapNotNull { b ->
                b.currentRecipeId?.let { AdvancedRecipeCatalog.byId(it) }
            }.flatMap { it.inputs.keys }.toSet()
        } else emptySet()

        // Política de selección
        val whitelist = mgr.config.sellWhitelist
        val candidates = state.company.inventory
            .filter { it.value > mgr.config.keepStock }
            .filterKeys { id ->
                val res = ResourceCatalog.tryById(id) ?: return@filterKeys false
                // Si el jugador definió whitelist, sólo esos
                if (whitelist.isNotEmpty()) return@filterKeys id in whitelist
                // Sin whitelist: solo bienes finales (GOOD/SERVICE/LUXURY)
                val sellable = res.category == ResourceCategory.GOOD ||
                    res.category == ResourceCategory.SERVICE ||
                    res.category == ResourceCategory.LUXURY
                if (!sellable) return@filterKeys false
                if (id in activeInputIds) return@filterKeys false
                true
            }

        if (candidates.isEmpty()) return state.copy(managers = bumpAction(state.managers, mgr.id, state.tick))
        val best = candidates.maxByOrNull { (id, _) ->
            state.market.priceFactors[id] ?: 1.0
        } ?: return state.copy(managers = bumpAction(state.managers, mgr.id, state.tick))
        val resourceId = best.key
        val have = best.value
        val factor = state.market.priceFactors[resourceId] ?: 1.0
        if (factor < mgr.config.sellAtFactor) {
            return state.copy(managers = bumpAction(state.managers, mgr.id, state.tick))
        }
        val toSell = (have - mgr.config.keepStock).coerceIn(1, 50)
        val raw = state.market.sellPriceOf(resourceId)
        val mktBonus = 1.0 +
            state.research.completed.sumOf { id -> TechCatalog.byId(id)?.marketBonus ?: 0.0 } +
            state.player.stats.charisma.coerceAtMost(100) * 0.003
        val price = raw * mktBonus
        val total = price * toSell
        val newInv = state.company.inventory + (resourceId to (have - toSell))
        val company = state.company.copy(
            cash = state.company.cash + total,
            inventory = newInv
        ).addXp((total / 25).toLong())
        val notif = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.ECONOMY,
            title = "${mgr.portrait} Venta automática",
            message = "$toSell × ${ResourceCatalog.tryById(resourceId)?.name ?: resourceId} a ${"%,.2f".format(price)} €/ud"
        )
        return state.copy(
            company = company,
            market = Economy.applySale(state.market, resourceId, toSell),
            managers = bumpAction(state.managers, mgr.id, state.tick),
            notifications = (state.notifications + notif).takeLast(40)
        )
    }

    /** Purchasing: comprar el input más necesario por la cadena de producción. */
    private fun applyPurchasing(state: GameState, mgr: Manager): GameState {
        // Inputs requeridos por las recetas activas
        val needed = HashMap<String, Int>()
        for (b in state.company.buildings) {
            val r = b.currentRecipeId?.let { AdvancedRecipeCatalog.byId(it) } ?: continue
            for ((id, q) in r.inputs) {
                needed[id] = (needed[id] ?: 0) + q * 5
            }
        }
        // Recurso con mayor déficit
        val deficits = needed.mapNotNull { (id, want) ->
            val have = state.company.inventory[id] ?: 0
            val gap = want - have
            if (gap > 0) Triple(id, gap, state.market.priceFactors[id] ?: 1.0) else null
        }.sortedWith(compareBy({ it.third }, { -it.second }))  // primero precios bajos y mayor déficit
        for ((resourceId, gap, factor) in deficits) {
            if (factor > mgr.config.buyBelowFactor) continue
            val price = state.market.buyPriceOf(resourceId)
            val maxQty = ((state.company.cash - mgr.config.cashReserve) / price).toInt().coerceAtMost(50)
            val toBuy = minOf(gap, maxQty).coerceAtLeast(0)
            if (toBuy <= 0) continue
            val total = price * toBuy
            val freeSpace = state.company.effectiveCapacity() - state.company.inventoryCount()
            if (toBuy > freeSpace) continue
            val newInv = state.company.inventory + (resourceId to ((state.company.inventory[resourceId] ?: 0) + toBuy))
            val company = state.company.copy(
                cash = state.company.cash - total,
                inventory = newInv
            )
            val notif = GameNotification(
                id = System.nanoTime(),
                timestamp = System.currentTimeMillis(),
                kind = NotificationKind.ECONOMY,
                title = "${mgr.portrait} Compra automática",
                message = "$toBuy × ${ResourceCatalog.tryById(resourceId)?.name ?: resourceId} a ${"%,.2f".format(price)} €/ud"
            )
            return state.copy(
                company = company,
                market = Economy.applyPurchase(state.market, resourceId, toBuy),
                managers = bumpAction(state.managers, mgr.id, state.tick),
                notifications = (state.notifications + notif).takeLast(40)
            )
        }
        return state.copy(managers = bumpAction(state.managers, mgr.id, state.tick))
    }

    /** HR: asigna empleados libres a edificios sin staff; despide poco leales. */
    private fun applyHr(state: GameState, mgr: Manager, rng: Random): GameState {
        var s = state
        // 1) Despedir los muy desleales si está configurado
        val toFire = s.company.employees.filter { it.loyalty <= mgr.config.autoFireBelowLoyalty }
        if (toFire.isNotEmpty()) {
            val first = toFire.first()
            s = GameEngine.fire(s, first.id)
            return s.copy(managers = bumpAction(s.managers, mgr.id, state.tick))
        }

        // 2) Asignar empleados libres a edificios sin trabajadores
        val freeEmployees = s.company.employees.count { it.assignedBuildingId == null }
        if (freeEmployees > 0) {
            val needsStaff = s.company.buildings
                .filter { it.assignedWorkers < it.workerCapacity && it.type != BuildingType.WAREHOUSE }
                .sortedByDescending { it.level }
            for (b in needsStaff) {
                s = GameEngine.assignWorkersDelta(s, b.id, +1)
                return s.copy(managers = bumpAction(s.managers, mgr.id, state.tick))
            }
        }

        // 3) Si está configurado, contratar candidatos para cubrir huecos
        if (mgr.config.autoHireWhenUnderstaffed) {
            val totalCapacity = s.company.buildings
                .filter { it.type != BuildingType.WAREHOUSE }
                .sumOf { it.workerCapacity }
            val totalEmployees = s.company.employees.size
            if (totalEmployees < totalCapacity) {
                // Si el pool está vacío, refrescar candidatos automáticamente
                if (s.candidates.isEmpty() && mgr.config.autoRefreshPoolWhenEmpty) {
                    s = GameEngine.refreshCandidates(s)
                }
                // Buscar al más barato que podamos pagar
                val candidate = s.candidates
                    .filter { s.company.cash >= it.monthlySalary * 0.5 + mgr.config.cashReserve }
                    .minByOrNull { it.monthlySalary }
                if (candidate != null) {
                    s = GameEngine.hire(s, candidate.id)
                    // Si el contrato funcionó, intentamos asignarlo a un edificio sin staff
                    val newEmp = s.company.employees.find { it.id == candidate.id }
                    if (newEmp != null && newEmp.assignedBuildingId == null) {
                        val needsStaff = s.company.buildings.firstOrNull {
                            it.assignedWorkers < it.workerCapacity && it.type != BuildingType.WAREHOUSE
                        }
                        if (needsStaff != null) {
                            s = GameEngine.assignWorkersDelta(s, needsStaff.id, +1)
                        }
                    }
                    return s.copy(managers = bumpAction(s.managers, mgr.id, state.tick))
                }
            }
        }
        return s.copy(managers = bumpAction(s.managers, mgr.id, state.tick))
    }

    /** Finance: si caja > N x reserva, repaga préstamos parcialmente. */
    private fun applyFinance(state: GameState, mgr: Manager): GameState {
        val ratio = state.company.cash / mgr.config.cashReserve.coerceAtLeast(1.0)
        if (ratio < mgr.config.repayLoanAboveCashRatio) {
            return state.copy(managers = bumpAction(state.managers, mgr.id, state.tick))
        }
        // Préstamo más caro primero
        val loan = state.loans.active.maxByOrNull { it.interestRateAPR } ?: run {
            return state.copy(managers = bumpAction(state.managers, mgr.id, state.tick))
        }
        val excess = state.company.cash - mgr.config.cashReserve * mgr.config.repayLoanAboveCashRatio
        val payment = minOf(excess, loan.remainingPrincipal)
        if (payment <= 0) return state.copy(managers = bumpAction(state.managers, mgr.id, state.tick))
        val s2 = BankingEngine.repayLoan(state, loan.id, payment)
        val notif = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.ECONOMY,
            title = "${mgr.portrait} Repago automático",
            message = "Has amortizado ${"%,.0f".format(payment)} € del préstamo ${loan.type.name}"
        )
        return s2.copy(
            managers = bumpAction(s2.managers, mgr.id, state.tick),
            notifications = (s2.notifications + notif).takeLast(40)
        )
    }

    // ======================================================================
    //                              HELPERS
    // ======================================================================

    private fun bumpAction(ms: ManagersState, id: String, tick: Long): ManagersState {
        return ms.copy(hired = ms.hired.map {
            if (it.id == id) it.copy(lastActionTick = tick, actionsTaken = it.actionsTaken + 1)
            else it
        })
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
