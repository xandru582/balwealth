package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.math.min
import kotlin.random.Random

/**
 * Motor de contratos B2B. Refresca la oferta cada día in-game, aplica
 * deadlines (expirando los que se pasan de plazo) y permite entregas
 * manuales o automáticas desde el inventario.
 *
 * Toda función es pura: GameState -> GameState.
 */
object ContractsEngine {

    /**
     * Tick diario: si toca, refresca ofertas; chequea expiraciones y completa
     * los contratos que estén ya cumplidos. Aplica también auto-entrega
     * desde inventario para todos los contratos aceptados.
     */
    fun tickContracts(state: GameState, rng: Random): GameState {
        var s = state
        val contracts = state.contracts

        // 1) Auto-entrega desde inventario para contratos aceptados
        s = autoDeliverFromInventory(s)

        // 2) Procesa cada contrato aceptado: ¿se completó? ¿expiró?
        var accepted = s.contracts.accepted.toMutableList()
        var company = s.company
        var notifications = s.notifications
        var completedTotal = s.contracts.completedTotal
        var expiredTotal = s.contracts.expiredTotal
        var totalEarnings = s.contracts.totalEarnings

        val toRemove = mutableListOf<String>()
        for (c in accepted) {
            when {
                c.isFulfilled() && !c.completed -> {
                    // pago + bonus por puntualidad
                    val pay = c.totalPaymentEstimate + c.bonusOnTime
                    company = company.copy(
                        cash = company.cash + pay,
                        reputation = (company.reputation + 1).coerceAtMost(100)
                    ).addXp(120)
                    notifications = (notifications + GameNotification(
                        id = System.nanoTime(),
                        timestamp = System.currentTimeMillis(),
                        kind = NotificationKind.SUCCESS,
                        title = "Contrato completado: ${c.clientName}",
                        message = "Has cobrado ${"%,.0f".format(pay)} (incluye bonus)."
                    )).takeLast(40)
                    toRemove += c.id
                    completedTotal++
                    totalEarnings += pay
                }
                c.secondsLeft(s.tick) <= 0 && !c.completed -> {
                    // FIX BUG-16-01 / BUG-07-09: la multa no puede dejar cash
                    // negativo. Cobra como mucho lo que tenga el jugador y el
                    // resto se descuenta de reputación adicional.
                    val actualPenalty = minOf(company.cash, c.penaltyMissed).coerceAtLeast(0.0)
                    val unpaid = c.penaltyMissed - actualPenalty
                    val extraRepHit = if (unpaid > 0.0) -3 else 0
                    company = company.copy(
                        cash = company.cash - actualPenalty,
                        reputation = (company.reputation - 2 + extraRepHit).coerceAtLeast(0)
                    )
                    notifications = (notifications + GameNotification(
                        id = System.nanoTime(),
                        timestamp = System.currentTimeMillis(),
                        kind = NotificationKind.ERROR,
                        title = "Contrato vencido: ${c.clientName}",
                        message = if (unpaid > 0.0)
                            "Sin fondos para la multa completa. Pagas ${"%,.0f".format(actualPenalty)} y pierdes reputación extra."
                        else
                            "Pagas una multa de ${"%,.0f".format(actualPenalty)}."
                    )).takeLast(40)
                    toRemove += c.id
                    expiredTotal++
                }
            }
        }
        accepted = accepted.filter { it.id !in toRemove }.toMutableList()

        // 3) Refresco de la oferta una vez al día
        var newContracts = s.contracts.copy(
            accepted = accepted,
            completedTotal = completedTotal,
            expiredTotal = expiredTotal,
            totalEarnings = totalEarnings
        )
        val ticksSinceRefresh = s.tick - newContracts.lastRefreshTick
        if (newContracts.lastRefreshTick == 0L || ticksSinceRefresh >= 1_440L) {
            newContracts = ContractGenerator.refreshOffers(s, newContracts, rng)
        }

        return s.copy(
            company = company,
            contracts = newContracts,
            notifications = notifications
        )
    }

    /**
     * Auto-entrega: para cada contrato aceptado, drena del inventario lo
     * necesario hasta cumplir su pedido. Ignora contratos ya completos.
     */
    fun autoDeliverFromInventory(state: GameState): GameState {
        val accepted = state.contracts.accepted
        if (accepted.isEmpty()) return state
        val inventory = HashMap(state.company.inventory)
        val updated = accepted.map { c ->
            if (c.completed || c.expired) c
            else {
                val newDelivered = HashMap(c.deliveredQty)
                for ((rid, needTotal) in c.items) {
                    val already = newDelivered[rid] ?: 0
                    val remaining = needTotal - already
                    if (remaining <= 0) continue
                    val have = inventory[rid] ?: 0
                    if (have <= 0) continue
                    val takeQty = min(have, remaining)
                    inventory[rid] = have - takeQty
                    newDelivered[rid] = already + takeQty
                }
                c.copy(deliveredQty = newDelivered)
            }
        }
        return state.copy(
            company = state.company.copy(inventory = inventory),
            contracts = state.contracts.copy(accepted = updated)
        )
    }

    /** Acepta una oferta (la mueve de offers a accepted con accepted=true). */
    fun acceptContract(state: GameState, contractId: String): GameState {
        val offer = state.contracts.offers.find { it.id == contractId } ?: return state
        if (state.contracts.accepted.any { it.id == contractId }) return state
        val accepted = offer.copy(
            accepted = true,
            // re-marca tick de creación para alinear plazo con ahora
            createdAtTick = state.tick
        )
        val notif = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.INFO,
            title = "Contrato aceptado",
            message = "${offer.clientLogo} ${offer.clientName}: ${offer.totalRequested} uds."
        )
        return state.copy(
            contracts = state.contracts.copy(
                offers = state.contracts.offers.filterNot { it.id == contractId },
                accepted = state.contracts.accepted + accepted
            ),
            notifications = (state.notifications + notif).takeLast(40)
        )
    }

    /** Rechaza una oferta sin penalización (la quita del listado). */
    fun rejectContract(state: GameState, contractId: String): GameState {
        return state.copy(
            contracts = state.contracts.copy(
                offers = state.contracts.offers.filterNot { it.id == contractId }
            )
        )
    }

    /**
     * Entrega manual: descuenta unidades del inventario de la empresa y las
     * acumula en el contrato. Si supera lo necesario, recorta a lo pendiente.
     */
    fun deliverContract(
        state: GameState,
        contractId: String,
        resourceId: String,
        qty: Int
    ): GameState {
        if (qty <= 0) return state
        val c = state.contracts.accepted.find { it.id == contractId } ?: return state
        val needed = c.items[resourceId] ?: return state
        val already = c.deliveredQty[resourceId] ?: 0
        val remaining = needed - already
        if (remaining <= 0) return state
        val have = state.company.inventory[resourceId] ?: 0
        val give = min(min(qty, have), remaining)
        if (give <= 0) return state
        val newInv = state.company.inventory + (resourceId to (have - give))
        val newDelivered = c.deliveredQty + (resourceId to (already + give))
        val newC = c.copy(deliveredQty = newDelivered)
        val accepted = state.contracts.accepted.map {
            if (it.id == contractId) newC else it
        }
        return state.copy(
            company = state.company.copy(inventory = newInv),
            contracts = state.contracts.copy(accepted = accepted)
        )
    }

    /** Refresca manualmente la oferta (acción del jugador). */
    fun forceRefresh(state: GameState, rng: Random): GameState {
        val updated = ContractGenerator.refreshOffers(state, state.contracts, rng)
        return state.copy(contracts = updated)
    }
}
