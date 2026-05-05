package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Retos diarios y semanales rotatorios. Cada 24h se generan 3 nuevos +
 * eventualmente 1 reto semanal. Completar varios días seguidos da racha
 * con multiplicador acumulativo.
 */

@Serializable
enum class ChallengeKind {
    EARN_CASH,           // gana X € en 1 día
    EARN_PERSONAL_CASH,  // gana X € en cuenta personal
    BUILD_N,             // construye N edificios
    UPGRADE_N,           // mejora N edificios
    HIRE_N,              // contrata N empleados
    RESEARCH_TECH,       // completa una tech
    SELL_SHARES,         // vende acciones
    BUY_SHARES,          // compra acciones
    PRODUCE_ITEMS,       // produce N items
    WIN_RACE,            // gana 1 carrera F1
    REACH_REPUTATION,    // alcanza reputación N
    DRIVE_DISTANCE,      // conduce X tiles
    TRAIN_STATS,         // entrena N puntos
    HAPPINESS_THRESHOLD, // mantén felicidad >= N
    NO_CASINO,           // no entres al casino
    CRYPTO_TRADE,        // realiza N trades de cripto
    SURVIVE_DISASTER,    // supera 1 desastre
    QUEST_COMPLETE       // completa N misiones
}

@Serializable
enum class ChallengeRarity { COMMON, RARE, EPIC, LEGENDARY }

@Serializable
data class Challenge(
    val id: String,
    val kind: ChallengeKind,
    val title: String,
    val description: String,
    val target: Long,
    /** Progreso actual. */
    val progress: Long = 0,
    val rarity: ChallengeRarity = ChallengeRarity.COMMON,
    /** Recompensas. */
    val rewardCash: Double = 0.0,
    val rewardXp: Long = 0L,
    val rewardReputation: Int = 0,
    val rewardKarma: Int = 0,
    val claimed: Boolean = false,
    val expiresAtTick: Long,
    /** Si es semanal, true. */
    val weekly: Boolean = false
) {
    val completed: Boolean get() = progress >= target
    val percent: Float get() = (progress.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
}

@Serializable
data class DailyChallengeState(
    val daily: List<Challenge> = emptyList(),
    val weekly: Challenge? = null,
    /** Tick del último refresh diario (módulo de día). */
    val lastDailyRefreshTick: Long = -1L,
    val lastWeeklyRefreshTick: Long = -1L,
    /** Días consecutivos completando los 3 retos diarios. */
    val streakDays: Int = 0,
    /** Día más alto de racha alcanzado. */
    val bestStreak: Int = 0,
    /** Total acumulado de retos completados (lifetime). */
    val totalCompleted: Int = 0,
    /** Total acumulado € ganados con retos. */
    val totalEarnedCash: Double = 0.0,
    /** Para detectar progress diario, snapshot de algunos contadores al inicio. */
    val snapshotCash: Double = 0.0,
    val snapshotPersonalCash: Double = 0.0,
    val snapshotTilesDriven: Long = 0L,
    /** Contador "no casino" para reto NO_CASINO. */
    val visitedCasinoToday: Boolean = false
)
