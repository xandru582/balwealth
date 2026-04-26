package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*

/**
 * Marca misiones como completadas cuando se cumple la condición. No entrega
 * recompensa automáticamente — el jugador reclama desde la UI.
 */
object QuestEngine {

    data class Result(
        val quests: List<Quest>,
        val notifications: List<GameNotification>,
        val cashBonus: Double,
        val xpBonus: Long,
        val reputationBonus: Int
    )

    fun evaluate(
        quests: List<Quest>,
        company: Company,
        player: Player,
        research: ResearchState,
        realEstate: RealEstatePortfolio,
        holdings: StockHoldings
    ): Result {
        val notifs = mutableListOf<GameNotification>()
        val updated = quests.map { q ->
            if (q.completed) q
            else {
                val now = check(q, company, player, research, realEstate, holdings)
                if (now) {
                    notifs += GameNotification(
                        id = System.nanoTime() + q.id.hashCode().toLong(),
                        timestamp = System.currentTimeMillis(),
                        kind = NotificationKind.SUCCESS,
                        title = "Objetivo cumplido",
                        message = "${q.title}: ya puedes cobrar la recompensa."
                    )
                    q.copy(completed = true)
                } else q
            }
        }
        return Result(updated, notifs, 0.0, 0, 0)
    }

    private fun check(
        q: Quest, c: Company, p: Player,
        r: ResearchState, re: RealEstatePortfolio, h: StockHoldings
    ): Boolean = when (q.id) {
        "q_first_building" -> c.buildings.isNotEmpty()
        "q_hire_first" -> c.employees.isNotEmpty()
        "q_first_sale" -> c.cash > 10_100.0   // arranca en 10k, cualquier venta supera
        "q_first_tech" -> r.completed.isNotEmpty()
        "q_cash_100k" -> c.cash >= 100_000.0
        "q_cash_1m" -> c.cash >= 1_000_000.0
        "q_first_property" -> re.owned.isNotEmpty()
        "q_portfolio_10" -> h.shares.values.sum() >= 10
        "q_level_10" -> p.level >= 10
        "q_5_buildings" -> c.buildings.size >= 5
        else -> false
    }
}
