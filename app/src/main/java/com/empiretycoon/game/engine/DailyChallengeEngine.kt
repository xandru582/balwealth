package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.random.Random

/**
 * Motor de retos diarios y semanales. Genera 3 retos cada día, 1 reto épico
 * cada 7 días, y aplica recompensas al completarse. Mantiene una racha
 * (streak) que multiplica recompensas hasta x5.
 */
object DailyChallengeEngine {

    private const val DAY_TICKS = 1_440L
    private const val WEEK_TICKS = 1_440L * 7L

    fun tickDaily(state: GameState, rng: Random): GameState {
        var s = state
        // Refresh de retos diarios al cambio de día.
        if (s.dailyChallenges.lastDailyRefreshTick + DAY_TICKS <= s.tick) {
            s = refreshDaily(s, rng)
        }
        if (s.dailyChallenges.lastWeeklyRefreshTick + WEEK_TICKS <= s.tick) {
            s = refreshWeekly(s, rng)
        }
        return s
    }

    /** Llamado por el GameEngine al cambio de día para evaluar y reclamar autom. */
    fun evaluateAndAutoClaim(state: GameState): GameState {
        var s = state
        var dc = s.dailyChallenges
        // Recalcular progreso para retos basados en estado puntual
        val updatedDaily = dc.daily.map { c -> updateProgress(c, s, dc) }
        val updatedWeekly = dc.weekly?.let { updateProgress(it, s, dc) }
        dc = dc.copy(daily = updatedDaily, weekly = updatedWeekly)
        s = s.copy(dailyChallenges = dc)
        // Auto-claim ya no — el jugador reclama manualmente.
        return s
    }

    private fun updateProgress(c: Challenge, s: GameState, dc: DailyChallengeState): Challenge {
        if (c.completed || c.claimed) return c
        val newProgress = when (c.kind) {
            ChallengeKind.EARN_CASH ->
                ((s.company.cash - dc.snapshotCash).coerceAtLeast(0.0)).toLong()
            ChallengeKind.EARN_PERSONAL_CASH ->
                ((s.player.cash - dc.snapshotPersonalCash).coerceAtLeast(0.0)).toLong()
            ChallengeKind.REACH_REPUTATION ->
                s.company.reputation.toLong()
            ChallengeKind.HAPPINESS_THRESHOLD ->
                if (s.player.happiness >= c.target.toInt()) c.target else s.player.happiness.toLong()
            ChallengeKind.NO_CASINO ->
                if (dc.visitedCasinoToday) 0L else c.target
            ChallengeKind.DRIVE_DISTANCE ->
                ((s.world.avatar.x.toLong() + s.world.avatar.y.toLong() + dc.snapshotTilesDriven) - dc.snapshotTilesDriven).coerceAtLeast(0)
            else -> c.progress
        }
        return c.copy(progress = newProgress)
    }

    fun claim(state: GameState, challengeId: String): GameState {
        var s = state
        val dc = s.dailyChallenges
        val daily = dc.daily.map { if (it.id == challengeId && it.completed && !it.claimed) it.copy(claimed = true) else it }
        val weekly = dc.weekly?.let { if (it.id == challengeId && it.completed && !it.claimed) it.copy(claimed = true) else it }
        // Buscar el reto en el id
        val target = dc.daily.firstOrNull { it.id == challengeId } ?: dc.weekly?.takeIf { it.id == challengeId }
        if (target == null || !target.completed || target.claimed) {
            return notify(s, NotificationKind.WARNING, "Reto no disponible",
                "No se puede reclamar (no completado o ya reclamado).")
        }
        val mul = streakMultiplier(dc.streakDays)
        val cash = target.rewardCash * mul
        val xp = (target.rewardXp * mul).toLong()
        val rep = (target.rewardReputation * mul).toInt()
        val karma = target.rewardKarma
        s = s.copy(
            company = s.company.copy(
                cash = s.company.cash + cash,
                reputation = (s.company.reputation + rep).coerceIn(0, 100)
            ),
            player = s.player.addXp(xp),
            dailyChallenges = dc.copy(
                daily = daily,
                weekly = weekly,
                totalCompleted = dc.totalCompleted + 1,
                totalEarnedCash = dc.totalEarnedCash + cash
            )
        )
        if (karma != 0) {
            s = s.copy(storyline = s.storyline.copy(karma = (s.storyline.karma + karma).coerceIn(-100, 100)))
        }
        return notify(s, NotificationKind.SUCCESS,
            "🏅 Reto completado: ${target.title}",
            "Has ganado ${"%,.0f".format(cash)} € · ${xp} XP · racha ×${"%.1f".format(mul)}.")
    }

    fun markCasinoVisited(state: GameState): GameState {
        return state.copy(dailyChallenges = state.dailyChallenges.copy(visitedCasinoToday = true))
    }

    // ===================== Refresh =====================

    private fun refreshDaily(state: GameState, rng: Random): GameState {
        var s = state
        val dc = s.dailyChallenges

        // Si todos los 3 estaban completos y reclamados → racha + 1
        val allClaimed = dc.daily.size >= 3 && dc.daily.all { it.claimed }
        val newStreak = if (dc.lastDailyRefreshTick > 0 && allClaimed) dc.streakDays + 1 else 0
        val bestStreak = maxOf(dc.bestStreak, newStreak)

        val unlockTier = unlockTier(s)
        val pool = pickThree(rng, unlockTier, s)
        val newDaily = pool.mapIndexed { i, c ->
            c.copy(id = "ch_${s.tick}_$i", expiresAtTick = s.tick + DAY_TICKS)
        }
        s = s.copy(dailyChallenges = dc.copy(
            daily = newDaily,
            lastDailyRefreshTick = s.tick - (s.tick % DAY_TICKS),
            streakDays = newStreak,
            bestStreak = bestStreak,
            visitedCasinoToday = false,
            snapshotCash = s.company.cash,
            snapshotPersonalCash = s.player.cash,
            snapshotTilesDriven = (s.world.avatar.x.toLong() + s.world.avatar.y.toLong())
        ))
        return notify(s, NotificationKind.INFO,
            "🌅 Nuevos retos diarios",
            "3 retos disponibles. Racha actual: ×${"%.1f".format(streakMultiplier(newStreak))}.")
    }

    private fun refreshWeekly(state: GameState, rng: Random): GameState {
        var s = state
        val dc = s.dailyChallenges
        val unlockTier = unlockTier(s)
        val ch = generateOne(rng, ChallengeRarity.EPIC, unlockTier, s, weekly = true).copy(
            id = "wch_${s.tick}",
            expiresAtTick = s.tick + WEEK_TICKS,
            weekly = true,
            rewardCash = 500_000.0 * unlockTier,
            rewardXp = 5_000L * unlockTier.toLong(),
            rewardReputation = 5,
            rewardKarma = 1
        )
        s = s.copy(dailyChallenges = dc.copy(
            weekly = ch,
            lastWeeklyRefreshTick = s.tick
        ))
        return notify(s, NotificationKind.INFO,
            "👑 Nuevo reto semanal",
            "${ch.title} — ${"%,.0f".format(ch.rewardCash)} € de recompensa.")
    }

    private fun streakMultiplier(streak: Int): Double {
        // 0 → 1.0; 1-2 → 1.2; 3-5 → 1.6; 6-10 → 2.4; 11-20 → 3.6; 21+ → 5.0
        return when {
            streak <= 0 -> 1.0
            streak <= 2 -> 1.2
            streak <= 5 -> 1.6
            streak <= 10 -> 2.4
            streak <= 20 -> 3.6
            else -> 5.0
        }
    }

    private fun unlockTier(state: GameState): Int {
        // Tier 1..5 según level del jugador
        return (state.player.level / 5).coerceIn(1, 5)
    }

    private fun pickThree(rng: Random, tier: Int, state: GameState): List<Challenge> {
        val kinds = ChallengeKind.values().toList().shuffled(rng).take(3)
        return kinds.map { generateOne(rng, ChallengeRarity.COMMON, tier, state, weekly = false) }
    }

    private fun generateOne(
        rng: Random,
        rarity: ChallengeRarity,
        tier: Int,
        state: GameState,
        weekly: Boolean
    ): Challenge {
        val rarityMul = when (rarity) {
            ChallengeRarity.COMMON -> 1.0
            ChallengeRarity.RARE -> 1.8
            ChallengeRarity.EPIC -> 3.5
            ChallengeRarity.LEGENDARY -> 7.0
        }
        val tierMul = tier.toDouble()
        val kind = ChallengeKind.values().random(rng)
        val (title, desc, target, cash, xp) = template(kind, tier, weekly)
        return Challenge(
            id = "tmp",
            kind = kind,
            title = title,
            description = desc,
            target = target,
            rarity = rarity,
            rewardCash = cash * rarityMul,
            rewardXp = (xp * rarityMul * tierMul).toLong(),
            rewardReputation = if (rng.nextDouble() < 0.30) 1 else 0,
            rewardKarma = if (rng.nextDouble() < 0.20) 1 else 0,
            expiresAtTick = state.tick + DAY_TICKS
        )
    }

    private fun template(kind: ChallengeKind, tier: Int, weekly: Boolean): Quintuple {
        val mul = if (weekly) 7L else 1L
        return when (kind) {
            ChallengeKind.EARN_CASH -> Quintuple(
                "💰 Gana ${100_000L * tier * mul} € hoy",
                "Genera ese cash con tu empresa antes de medianoche.",
                100_000L * tier * mul, 25_000.0 * tier, 200L
            )
            ChallengeKind.EARN_PERSONAL_CASH -> Quintuple(
                "💼 Gana ${10_000L * tier * mul} € personales",
                "Engorda tu cuenta personal trabajando.",
                10_000L * tier * mul, 5_000.0 * tier, 100L
            )
            ChallengeKind.BUILD_N -> Quintuple(
                "🏗️ Construye ${1L + mul / 2} edificios",
                "Expande tu imperio físico.",
                1L + mul / 2, 30_000.0 * tier, 250L
            )
            ChallengeKind.UPGRADE_N -> Quintuple(
                "⬆️ Mejora ${2L * mul} edificios",
                "Sube de nivel tus instalaciones.",
                2L * mul, 25_000.0 * tier, 200L
            )
            ChallengeKind.HIRE_N -> Quintuple(
                "👥 Contrata ${2L * mul} empleados",
                "Refuerza la plantilla.",
                2L * mul, 15_000.0 * tier, 150L
            )
            ChallengeKind.RESEARCH_TECH -> Quintuple(
                "🔬 Investiga 1 tecnología",
                "Completa cualquier investigación.",
                1L, 50_000.0 * tier, 400L
            )
            ChallengeKind.SELL_SHARES -> Quintuple(
                "📉 Vende ${10L * mul} acciones",
                "Liquida posiciones en bolsa.",
                10L * mul, 20_000.0 * tier, 150L
            )
            ChallengeKind.BUY_SHARES -> Quintuple(
                "📈 Compra ${20L * mul} acciones",
                "Aumenta tu cartera bursátil.",
                20L * mul, 20_000.0 * tier, 150L
            )
            ChallengeKind.PRODUCE_ITEMS -> Quintuple(
                "🏭 Produce ${50L * tier * mul} items",
                "Que las cadenas escupan.",
                50L * tier * mul, 30_000.0 * tier, 250L
            )
            ChallengeKind.WIN_RACE -> Quintuple(
                "🏁 Gana 1 carrera F1",
                "Tu equipo Formula debe ganar.",
                1L, 100_000.0 * tier, 800L
            )
            ChallengeKind.REACH_REPUTATION -> Quintuple(
                "🌟 Alcanza reputación ${(40 + tier * 5)}",
                "Consigue prestigio público.",
                (40 + tier * 5).toLong(), 40_000.0 * tier, 300L
            )
            ChallengeKind.DRIVE_DISTANCE -> Quintuple(
                "🚗 Conduce ${100L * mul} tiles",
                "Recorre la ciudad.",
                100L * mul, 10_000.0 * tier, 100L
            )
            ChallengeKind.TRAIN_STATS -> Quintuple(
                "💪 Entrena ${3L * mul} puntos de stats",
                "Mejora tu personaje.",
                3L * mul, 15_000.0 * tier, 200L
            )
            ChallengeKind.HAPPINESS_THRESHOLD -> Quintuple(
                "😄 Mantén felicidad ≥ 70",
                "No te deprimas hoy.",
                70L, 20_000.0 * tier, 150L
            )
            ChallengeKind.NO_CASINO -> Quintuple(
                "🚫🎰 Día sin casino",
                "Aguanta sin entrar a la ruleta.",
                1L, 35_000.0 * tier, 250L
            )
            ChallengeKind.CRYPTO_TRADE -> Quintuple(
                "🪙 Haz ${3L * mul} trades de cripto",
                "Compra o vende tokens.",
                3L * mul, 25_000.0 * tier, 200L
            )
            ChallengeKind.SURVIVE_DISASTER -> Quintuple(
                "🌪️ Supera 1 desastre",
                "Mitiga uno o aguanta hasta la resolución.",
                1L, 80_000.0 * tier, 500L
            )
            ChallengeKind.QUEST_COMPLETE -> Quintuple(
                "🎯 Completa ${1L + mul / 2} misiones",
                "Avanza la storyline o secundarias.",
                1L + mul / 2, 30_000.0 * tier, 300L
            )
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

    private data class Quintuple(
        val title: String,
        val desc: String,
        val target: Long,
        val cash: Double,
        val xp: Long
    )
}
