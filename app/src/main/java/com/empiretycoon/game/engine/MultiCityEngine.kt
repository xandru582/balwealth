package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.math.max
import kotlin.random.Random

/**
 * Motor de comercio internacional (v17). Pure GameState -> GameState.
 *
 * Operaciones públicas:
 *   - canUnlock / unlock
 *   - tickDaily(state, rng): mueve mercados, paga rutas, liquida envíos
 *   - openRoute / closeRoute
 *   - ship(state, routeId, resourceId, qty)
 *
 * Reglas de seguridad:
 *   - qty <= 0 rechazado.
 *   - Solo se exporta desde inventario en HOME.
 *   - El cash NUNCA puede caer al pagar mantenimiento o aranceles si no hay
 *     fondos: la ruta se cancela / el envío no se hace, con mensaje claro.
 *   - El multiplicador efectivo está clamp 0.4..2.5 para evitar valores raros
 *     tras muchos ticks de drift.
 */
object MultiCityEngine {

    private const val DAY_TICKS = 1_440L

    // ===================== Gating =====================

    fun canUnlock(state: GameState): Boolean =
        state.company.cash >= 1_000_000.0 && state.company.reputation >= 50

    fun unlock(state: GameState): GameState {
        val mc = state.multiCity
        if (mc.unlocked) return state
        if (!canUnlock(state)) {
            return notify(state, NotificationKind.ERROR, "🌐 Imperio global bloqueado",
                "Necesitas 1.000.000 € en caja y reputación ≥ 50.")
        }
        return notify(
            state.copy(multiCity = mc.copy(unlocked = true)),
            NotificationKind.SUCCESS,
            "🌐 Imperio global desbloqueado",
            "Ahora puedes abrir rutas a 5 mercados extranjeros. Cada ciudad tiene aranceles, demandas y volatilidad propias."
        )
    }

    // ===================== Tick diario =====================

    fun tickDaily(state: GameState, rng: Random): GameState {
        if (!state.multiCity.unlocked) return state
        var s = state
        var mc = s.multiCity

        // 1) Drift de mercados extranjeros (no toca HOME).
        val newCities = mc.cities.map { city ->
            if (city.id == CityId.HOME) city else driftCity(city, rng)
        }

        // 2) Pago de mantenimiento de rutas abiertas. Si no hay liquidez,
        //    cerramos automáticamente la ruta más cara.
        var company = s.company
        var routes = mc.routes
        val openRoutes = routes.filter { it.open }
        val totalDailyCost = openRoutes.sumOf { it.dailyCost }
        if (totalDailyCost > 0.0) {
            if (company.cash >= totalDailyCost) {
                company = company.copy(cash = company.cash - totalDailyCost)
            } else {
                // Cerramos la ruta más cara y seguimos.
                val toClose = openRoutes.maxByOrNull { it.dailyCost }
                if (toClose != null) {
                    routes = routes.map { if (it.id == toClose.id) it.copy(open = false) else it }
                    s = notify(s, NotificationKind.WARNING,
                        "🚧 Ruta cancelada por impago",
                        "Sin liquidez para pagar el mantenimiento de ${toClose.from.displayName} → ${toClose.to.displayName}.")
                }
            }
        }

        // 3) Liquidar envíos que llegan hoy.
        val arriving = mc.shipments.filter { it.arrivesAtTick <= s.tick }
        val pending = mc.shipments.filter { it.arrivesAtTick > s.tick }
        var totalGross = 0.0
        var totalNet = 0.0
        var totalQty = 0L
        val newHistoryEntries = mutableListOf<CityShipmentResult>()

        for (sh in arriving) {
            val destCity = newCities.find { it.id == sh.to } ?: continue
            val baseSell = state.market.sellPriceOf(sh.resourceId)
            val effective = destCity.effectiveSellPrice(baseSell, sh.resourceId)
            val gross = effective * sh.qty
            val exportTariff = gross * destCity.exportTariff
            val grossAfterExport = gross - exportTariff
            val cogs = sh.baseCostAtDeparture * sh.qty + sh.importTariffPaid
            val taxableProfit = max(0.0, grossAfterExport - cogs)
            val localTax = taxableProfit * destCity.localTax
            val net = grossAfterExport - localTax

            company = company.copy(cash = company.cash + net)
            totalGross += gross
            totalNet += net
            totalQty += sh.qty.toLong()

            newHistoryEntries += CityShipmentResult(
                resourceId = sh.resourceId,
                qty = sh.qty,
                to = sh.to,
                gross = gross,
                exportTariff = exportTariff,
                localTax = localTax,
                net = net,
                day = s.day
            )
        }

        if (arriving.isNotEmpty()) {
            s = notify(s, NotificationKind.SUCCESS,
                "📦 Envíos liquidados (${arriving.size})",
                "Bruto ${"%,.0f".format(totalGross)} € · neto ${"%,.0f".format(totalNet)} €.")
        }

        mc = mc.copy(
            cities = newCities,
            routes = routes,
            shipments = pending,
            history = (mc.history + newHistoryEntries).takeLast(30),
            lastDailyTick = s.tick,
            totalShipped = mc.totalShipped + totalQty,
            totalRevenue = mc.totalRevenue + totalGross,
            totalNet = mc.totalNet + totalNet
        )

        return s.copy(multiCity = mc, company = company)
    }

    private fun driftCity(city: CityMarket, rng: Random): CityMarket {
        val muls = city.priceMultipliers.mapValues { (_, v) ->
            val drift = (rng.nextDouble() - 0.5) * city.volatility * 2.0
            (v * (1.0 + drift)).coerceIn(0.40, 2.50)
        }
        val dem = city.demand.mapValues { (_, v) ->
            val drift = (rng.nextDouble() - 0.5) * 0.10
            (v * (1.0 + drift)).coerceIn(0.30, 2.00)
        }
        // Sentiment hace boom/bust ocasional. 5% de los días reroll.
        val newSentiment = if (rng.nextDouble() < 0.05) {
            (city.sentiment + (rng.nextDouble() - 0.5) * 0.25).coerceIn(0.70, 1.40)
        } else {
            // Reversión a la media (1.0)
            city.sentiment + (1.0 - city.sentiment) * 0.05
        }
        return city.copy(priceMultipliers = muls, demand = dem, sentiment = newSentiment)
    }

    // ===================== Acciones del jugador =====================

    fun openRoute(state: GameState, routeId: String): GameState {
        val mc = state.multiCity
        val route = mc.routes.find { it.id == routeId } ?: return state
        if (route.open) return state
        // Apertura: 30 días de mantenimiento como "fianza".
        val openingFee = route.dailyCost * 30.0
        if (state.company.cash < openingFee) {
            return notify(state, NotificationKind.ERROR, "Sin fondos",
                "Abrir esta ruta cuesta ${"%,.0f".format(openingFee)} € (30 días de mantenimiento).")
        }
        val newRoutes = mc.routes.map { if (it.id == routeId) it.copy(open = true) else it }
        val newCompany = state.company.copy(cash = state.company.cash - openingFee)
        return notify(
            state.copy(company = newCompany, multiCity = mc.copy(routes = newRoutes)),
            NotificationKind.SUCCESS,
            "🚚 Ruta abierta: ${route.from.displayName} → ${route.to.displayName}",
            "Mantenimiento ${"%,.0f".format(route.dailyCost)} €/día · tránsito ${route.transitTicks / DAY_TICKS} días."
        )
    }

    fun closeRoute(state: GameState, routeId: String): GameState {
        val mc = state.multiCity
        val route = mc.routes.find { it.id == routeId } ?: return state
        if (!route.open) return state
        // Si hay envíos en tránsito por esta ruta, los dejamos completar (no
        // los cancelamos para no penalizar al jugador). Solo cortamos
        // mantenimiento.
        val newRoutes = mc.routes.map { if (it.id == routeId) it.copy(open = false) else it }
        return notify(
            state.copy(multiCity = mc.copy(routes = newRoutes)),
            NotificationKind.WARNING,
            "🚧 Ruta cerrada",
            "${route.from.displayName} → ${route.to.displayName}. Los envíos ya en camino se completarán."
        )
    }

    fun ship(
        state: GameState,
        routeId: String,
        resourceId: String,
        qty: Int
    ): GameState {
        if (!state.multiCity.unlocked) return state
        if (qty <= 0) {
            return notify(state, NotificationKind.ERROR, "Cantidad inválida",
                "qty debe ser > 0.")
        }
        val mc = state.multiCity
        val route = mc.routes.find { it.id == routeId } ?: return state
        if (!route.open) {
            return notify(state, NotificationKind.ERROR, "Ruta cerrada",
                "Abre la ruta primero.")
        }
        if (qty > route.capacityPerShipment) {
            return notify(state, NotificationKind.ERROR, "Capacidad excedida",
                "Cap por envío: ${route.capacityPerShipment} unidades.")
        }
        val res = ResourceCatalog.tryById(resourceId) ?: return notify(state,
            NotificationKind.ERROR, "Recurso desconocido", "ID: $resourceId.")
        val have = state.inventoryOf(resourceId)
        if (have < qty) {
            return notify(state, NotificationKind.ERROR, "Sin inventario",
                "Tienes ${have} de ${res.name}.")
        }
        val destCity = mc.cities.find { it.id == route.to } ?: return state
        // Coste interno = lo que costaría comprar este recurso ahora en HOME.
        val baseCost = state.market.buyPriceOf(resourceId)
        val importTariff = baseCost * qty * destCity.importTariff
        if (state.company.cash < importTariff) {
            return notify(state, NotificationKind.ERROR, "Sin fondos para arancel",
                "Necesitas ${"%,.0f".format(importTariff)} € de arancel de importación.")
        }
        val newInv = state.company.inventory + (resourceId to (have - qty))
        val newCompany = state.company.copy(
            inventory = newInv,
            cash = state.company.cash - importTariff
        )
        val shipment = CityShipment(
            id = "sh_${state.tick}_${System.nanoTime()}",
            routeId = routeId,
            resourceId = resourceId,
            qty = qty,
            baseCostAtDeparture = baseCost,
            importTariffPaid = importTariff,
            arrivesAtTick = state.tick + route.transitTicks,
            from = route.from,
            to = route.to
        )
        val newMc = mc.copy(shipments = mc.shipments + shipment)
        return notify(
            state.copy(company = newCompany, multiCity = newMc),
            NotificationKind.INFO,
            "🚚 Envío en ruta",
            "${qty} ${res.name} → ${destCity.id.displayName}. Llega en ${route.transitTicks / DAY_TICKS} días."
        )
    }

    // ===================== Helpers =====================

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
