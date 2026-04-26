package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.random.Random

/**
 * Motor de misiones secundarias. Refresca el pool diario (3-4 misiones
 * disponibles a la vez), permite aceptar/abandonar, comprueba progreso
 * y entrega recompensas al reclamar. Las misiones expiran si no se
 * completan en `expirationDays` días.
 */
object SideQuestEngine {

    private const val SLOT_SIZE = 4

    /**
     * Refresca la lista de misiones disponibles si ha cambiado el día
     * desde el último refresh. Mantiene las activas intactas.
     */
    fun refreshAvailable(state: GameState, rng: Random): GameState {
        val s = state.sideQuests
        if (state.day == s.lastRefreshDay && s.available.isNotEmpty()) return state

        // Pool excluye misiones ya completadas, fallidas, activas o
        // que ya están en disponibles para no duplicar.
        val excludeIds = (s.completed + s.failed +
            s.active.map { it.id } +
            s.available.map { it.id }).toSet()
        val pool = SideQuestCatalog.all.filterNot { it.id in excludeIds }
        val picked = pool.shuffled(rng).take(SLOT_SIZE)

        return state.copy(
            sideQuests = s.copy(
                available = picked,
                lastRefreshDay = state.day
            )
        )
    }

    /** El jugador acepta una misión: pasa de available a active. */
    fun acceptSideQuest(state: GameState, id: String): GameState {
        val s = state.sideQuests
        val sq = s.available.find { it.id == id } ?: return state
        if (s.active.size >= 6) {
            // tope de 6 activas para no abrumar
            return notify(state, NotificationKind.WARNING,
                "Demasiadas misiones",
                "Ya tienes 6 misiones activas. Cancela alguna primero.")
        }
        val accepted = sq.copy(
            acceptedAtTick = state.tick,
            deadlineDay = state.day + sq.expirationDays,
            baselineCash = state.company.cash,
            baselineDay = state.day
        )
        return state.copy(
            sideQuests = s.copy(
                active = s.active + accepted,
                available = s.available - sq
            )
        )
    }

    /** Abandona una misión activa (queda como fallida). */
    fun abandonSideQuest(state: GameState, id: String): GameState {
        val s = state.sideQuests
        val sq = s.active.find { it.id == id } ?: return state
        return state.copy(
            sideQuests = s.copy(
                active = s.active - sq,
                failed = s.failed + id
            )
        )
    }

    /**
     * Comprueba el progreso de cada misión activa y marca las cumplidas
     * añadiéndolas a `completed`. NOTA: la misión sigue en `active` hasta
     * que el jugador reclame la recompensa.
     */
    fun checkProgress(state: GameState): GameState {
        val s = state.sideQuests
        val newlyCompleted = mutableSetOf<String>()
        val newNotifs = mutableListOf<GameNotification>()
        for (q in s.active) {
            if (s.completed.contains(q.id)) continue
            if (isFulfilled(q, state)) {
                newlyCompleted += q.id
                newNotifs += GameNotification(
                    id = System.nanoTime() + q.id.hashCode().toLong(),
                    timestamp = System.currentTimeMillis(),
                    kind = NotificationKind.SUCCESS,
                    title = "Misión secundaria cumplida",
                    message = "${q.title}: pasa a cobrar la recompensa."
                )
            }
        }
        if (newlyCompleted.isEmpty()) return state
        return state.copy(
            sideQuests = s.copy(completed = s.completed + newlyCompleted),
            notifications = (state.notifications + newNotifs).takeLast(40)
        )
    }

    /** Cobra la recompensa: aplica al estado, retira de active. */
    fun claimReward(state: GameState, id: String): GameState {
        val s = state.sideQuests
        if (!s.completed.contains(id)) return state
        val q = s.active.find { it.id == id } ?: return state

        val r = q.reward
        var company = state.company.copy(
            cash = state.company.cash + r.cash,
            reputation = (state.company.reputation + r.reputation).coerceIn(0, 100)
        )
        if (r.items.isNotEmpty()) {
            val inv = HashMap(company.inventory)
            for ((k, v) in r.items) inv[k] = (inv[k] ?: 0) + v
            company = company.copy(inventory = inv)
        }
        var player = state.player.addXp(r.xp)
        var storyline = state.storyline
        if (r.karmaDelta != 0) {
            storyline = storyline.copy(
                karma = (storyline.karma + r.karmaDelta).coerceIn(-100, 100)
            )
        }
        val notif = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.SUCCESS,
            title = "Recompensa cobrada",
            message = "${q.title}: gracias por el trabajo."
        )
        return state.copy(
            company = company,
            player = player,
            storyline = storyline,
            sideQuests = s.copy(
                active = s.active - q
            ),
            notifications = (state.notifications + notif).takeLast(40)
        )
    }

    /** Marca como falladas las activas que pasaron su deadline. */
    fun expireOverdue(state: GameState): GameState {
        val s = state.sideQuests
        val expired = s.active.filter { state.day > it.deadlineDay && !s.completed.contains(it.id) }
        if (expired.isEmpty()) return state
        val notifs = expired.map { q ->
            GameNotification(
                id = System.nanoTime() + q.id.hashCode().toLong(),
                timestamp = System.currentTimeMillis(),
                kind = NotificationKind.WARNING,
                title = "Misión expirada",
                message = "${q.title}: el plazo ha vencido."
            )
        }
        return state.copy(
            sideQuests = s.copy(
                active = s.active - expired.toSet(),
                failed = s.failed + expired.map { it.id }
            ),
            notifications = (state.notifications + notifs).takeLast(40)
        )
    }

    // ---------- progreso por tipo de objetivo ----------

    fun progressOf(q: SideQuest, state: GameState): Pair<Long, Long> {
        return when (val o = q.objective) {
            is QuestObjective.AccumulateCash -> {
                val current = (state.company.cash - q.baselineCash).coerceAtLeast(0.0)
                current.toLong() to o.amount.toLong()
            }
            is QuestObjective.ProduceX -> {
                val have = state.company.inventory[o.resourceId] ?: 0
                have.toLong() to o.qty.toLong()
            }
            is QuestObjective.SellAtPriceAbove -> {
                // Aproximación: usamos cash acumulado / precio mínimo como medida
                val sold = ((state.company.cash - q.baselineCash) / o.price).coerceAtLeast(0.0)
                sold.toLong().coerceAtMost(o.qty.toLong()) to o.qty.toLong()
            }
            is QuestObjective.HireRole -> {
                val have = state.company.employees.size.toLong()
                if (o.role == "any_25") have to 25L
                else have to 1L
            }
            is QuestObjective.ReachLevel -> state.player.level.toLong() to o.lvl.toLong()
            is QuestObjective.CompleteContracts -> 0L to o.n.toLong()
            is QuestObjective.ResearchTech -> {
                if (state.research.completed.contains(o.id)) 1L to 1L else 0L to 1L
            }
            is QuestObjective.DonateToCharity -> {
                // No hay tracking de donaciones reales: aproximación por reputación ganada
                val gain = (state.company.reputation - 30).coerceAtLeast(0).toLong()
                gain to (o.amount / 1_000.0).toLong().coerceAtLeast(1L)
            }
            is QuestObjective.DefeatRival -> {
                val a = state.storyline.alignments[o.rivalId] ?: 0
                val cur = (-a).coerceAtLeast(0).toLong()
                cur to 50L
            }
            is QuestObjective.VisitLocation -> 0L to 1L
            is QuestObjective.PassDays -> {
                val passed = (state.day - q.baselineDay).toLong().coerceAtLeast(0)
                passed to o.n.toLong()
            }
        }
    }

    private fun isFulfilled(q: SideQuest, state: GameState): Boolean {
        val (cur, target) = progressOf(q, state)
        return cur >= target
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
