package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import java.util.Calendar

/**
 * Motor del sistema de logros — puro y sin efectos colaterales.
 * Recibe el estado completo y devuelve el `AchievementsState` actualizado
 * junto con las notificaciones de desbloqueo. El reclamo de recompensas
 * se maneja en `GameEngine.claimAchievement` (acción del jugador).
 */
object AchievementEngine {

    /**
     * Recorre el catálogo entero, computa progreso, y desbloquea aquellos
     * cuyo progreso alcance el umbral por primera vez.
     */
    fun evaluate(state: GameState): Pair<AchievementsState, List<GameNotification>> {
        val current = state.achievements
        val notifs = mutableListOf<GameNotification>()
        val progressMap = HashMap<String, Long>(current.progressMap)
        val unlockedSet = HashSet(current.unlocked)

        for (ach in AchievementCatalog.all) {
            val prog = progressFor(ach, state).coerceAtLeast(progressMap[ach.id] ?: 0L)
            // mantenemos el máximo histórico (algunos progresos pueden bajar — p.ej. cash)
            val newProg = if (isMonotonic(ach.id))
                maxOf(progressMap[ach.id] ?: 0L, prog)
            else prog
            progressMap[ach.id] = newProg

            if (!unlockedSet.contains(ach.id) && newProg >= ach.threshold) {
                unlockedSet += ach.id
                notifs += GameNotification(
                    id = System.nanoTime() + ach.id.hashCode().toLong(),
                    timestamp = System.currentTimeMillis(),
                    kind = NotificationKind.SUCCESS,
                    title = "¡Logro desbloqueado!",
                    message = "${ach.emoji} ${ach.title} — reclama tu recompensa."
                )
            }
        }

        return AchievementsState(
            unlocked = unlockedSet,
            claimedAchievements = current.claimedAchievements,
            progressMap = progressMap
        ) to notifs
    }

    /**
     * Progreso actual del logro dado, en la unidad correspondiente
     * a su `threshold`. Devuelve un `Long` ya convertido cuando aplica.
     */
    fun progressFor(achievement: Achievement, state: GameState): Long = when (achievement.id) {
        // ---------- WEALTH ----------
        "ach_cash_1k", "ach_cash_10k", "ach_cash_100k", "ach_cash_1m",
        "ach_cash_10m", "ach_cash_100m", "ach_cash_1b" ->
            state.company.cash.toLong().coerceAtLeast(0)
        "ach_personal_50k" -> state.player.cash.toLong().coerceAtLeast(0)
        "ach_loan_repaid" ->
            if (state.loanPrincipal == 0.0
                && (state.achievements.progressMap["__loan_seen"] ?: 0L) == 1L) 1 else 0
        "ach_total_assets" -> {
            val stockVal = state.stocks.sumOf { s ->
                (state.holdings.shares[s.ticker] ?: 0) * s.price
            }
            (state.company.cash + state.realEstate.totalValue + stockVal).toLong()
        }

        // ---------- PRODUCTION ----------
        "ach_first_recipe" ->
            if (state.company.buildings.any { it.currentRecipeId != null }) 1 else 0
        "ach_multitarea" ->
            state.company.buildings.count { it.currentRecipeId != null }.toLong()
        "ach_specialist_100", "ach_specialist_1000" ->
            (state.company.inventory.values.maxOrNull() ?: 0).toLong()
        "ach_inventory_full" ->
            if (state.company.effectiveCapacity() > 0
                && state.company.inventoryCount() >= state.company.effectiveCapacity()) 1 else 0
        "ach_diversify_5", "ach_diversify_15", "ach_diversify_25" ->
            state.company.inventory.count { it.value > 0 }.toLong()

        // ---------- EMPIRE ----------
        "ach_first_building", "ach_5_buildings", "ach_10_buildings", "ach_20_buildings" ->
            state.company.buildings.size.toLong()
        "ach_one_each" ->
            state.company.buildings.map { it.type }.distinct().size.toLong()
        "ach_max_level_b" ->
            (state.company.buildings.maxOfOrNull { it.level } ?: 0).toLong()
        "ach_workforce_25", "ach_workforce_50" ->
            state.company.employees.size.toLong()

        // ---------- CHARACTER ----------
        "ach_level_5", "ach_level_10", "ach_level_25", "ach_level_50" ->
            state.player.level.toLong()
        "ach_charisma_50" -> state.player.stats.charisma.toLong()
        "ach_intelligence_50" -> state.player.stats.intelligence.toLong()
        "ach_stats_total_100" -> state.player.stats.total.toLong()
        "ach_happy_max" -> state.player.happiness.toLong()

        // ---------- MARKET ----------
        "ach_market_10_tx", "ach_market_100_tx" ->
            state.achievements.progressMap["__market_tx"] ?: 0L
        "ach_sold_1000" ->
            state.achievements.progressMap["__sold_max"] ?: 0L
        "ach_invertor", "ach_trader" ->
            state.holdings.shares.values.sum().toLong()
        "ach_bursatil" -> {
            val invested = state.holdings.shares.entries.sumOf { (t, q) ->
                (state.holdings.avgCost[t] ?: 0.0) * q
            }
            invested.toLong()
        }

        // ---------- REAL_ESTATE ----------
        "ach_first_property", "ach_5_properties", "ach_10_properties" ->
            state.realEstate.owned.size.toLong()
        "ach_skyscraper" ->
            state.realEstate.owned.count { it.type == PropertyType.SKYSCRAPER }.toLong()
        "ach_rent_5k" -> state.realEstate.dailyNet.toLong()

        // ---------- RESEARCH ----------
        "ach_first_tech", "ach_5_tech", "ach_10_tech", "ach_all_tech" ->
            state.research.completed.size.toLong()

        // ---------- SOCIAL ----------
        "ach_rep_50", "ach_rep_80", "ach_rep_max" ->
            state.company.reputation.toLong()

        // ---------- MILESTONE ----------
        "ach_yacht_owner" -> (state.company.inventory["yacht"] ?: 0).toLong()
        "ach_car_owner" -> (state.company.inventory["car"] ?: 0).toLong()
        "ach_jewelry_owner" -> (state.company.inventory["jewelry"] ?: 0).toLong()
        "ach_day_30", "ach_day_100" -> state.day.toLong()

        // ---------- SECRET ----------
        "ach_hidden_bankrupt" ->
            if (state.company.cash < 0.0) 1 else (state.achievements.progressMap[achievement.id] ?: 0L)
        "ach_hidden_no_employees" ->
            if (state.company.buildings.isNotEmpty()
                && state.company.employees.isEmpty()
                && state.tick > 600) 1 else 0
        "ach_hidden_night_owl" -> {
            val cal = Calendar.getInstance()
            val h = cal.get(Calendar.HOUR_OF_DAY)
            if (h == 3) 1 else (state.achievements.progressMap[achievement.id] ?: 0L)
        }
        "ach_hidden_speedster" ->
            if (state.speedMultiplier >= 8)
                (state.achievements.progressMap[achievement.id] ?: 0L) + 1
            else state.achievements.progressMap[achievement.id] ?: 0L

        else -> 0L
    }

    /**
     * Algunos progresos deben mantener su máximo histórico para que el
     * jugador no pierda el desbloqueo si el valor baja (por ejemplo cash).
     */
    private fun isMonotonic(id: String): Boolean = when (id) {
        "ach_cash_1k", "ach_cash_10k", "ach_cash_100k", "ach_cash_1m",
        "ach_cash_10m", "ach_cash_100m", "ach_cash_1b",
        "ach_personal_50k", "ach_total_assets",
        "ach_specialist_100", "ach_specialist_1000",
        "ach_charisma_50", "ach_intelligence_50",
        "ach_stats_total_100", "ach_happy_max",
        "ach_rep_50", "ach_rep_80", "ach_rep_max",
        "ach_rent_5k", "ach_bursatil",
        "ach_max_level_b" -> true
        else -> false
    }
}
