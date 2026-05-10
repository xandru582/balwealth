package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.math.max
import kotlin.math.min

/**
 * Motor del sistema de Oficios. Pure GameState -> GameState.
 *
 * Filosofía:
 *  - El cash ganado va al player (caja personal), no a la empresa.
 *    Es el sueldo del personaje. Si quieres invertirlo en la empresa,
 *    lo transfieres con `setPersonalToCompany` (ya existe en GameEngine).
 *  - El nivel del oficio sube cada 100 XP (cap 50). Cada nivel da +5%
 *    de wage. Cap real de wage = baseWage × (1 + 49 × 0.05) = baseWage × 3.45.
 *  - El stat preferido bonifica +0.5% por punto sobre el wage. Con stat
 *    100 (cap teórico), eso es +50%. Sumado al level-cap = baseWage × ~5.2.
 *  - Energía: el turno consume `energyCost`. Si no tienes, no puedes
 *    trabajar.
 *  - El framework NO tiene mini-juegos en este commit. La acción
 *    `workShift` es instantánea. Los mini-juegos se irán añadiendo
 *    en commits futuros y reemplazarán esta función por una flow con
 *    Composable jugable.
 */
object JobsEngine {

    /** XP por turno base. Las acciones del mini-juego pueden multiplicarlo. */
    private const val SHIFT_BASE_XP = 15

    /** XP para el jugador (no del oficio) por turno. */
    private const val PLAYER_XP_PER_SHIFT = 5L

    /** Cap del nivel del oficio. */
    private const val MAX_LEVEL = 50

    /** XP necesaria por nivel. Constante para simplicidad. */
    private const val XP_PER_LEVEL = 100

    fun accept(state: GameState): GameState {
        if (state.jobs.accepted) return state
        return notify(
            state.copy(jobs = state.jobs.copy(accepted = true)),
            NotificationKind.SUCCESS,
            "💼 Bolsa de empleo abierta",
            "Trabajos jugables disponibles. Tu nivel desbloquea oficios automáticamente."
        )
    }

    /**
     * Auto-desbloquea oficios cuyo requiredPlayerLevel haya alcanzado el
     * jugador. Idempotente: solo flipa de unlocked=false a true.
     * Llamar al levelUp del jugador o periódicamente.
     */
    fun checkUnlocks(state: GameState): GameState {
        if (!state.jobs.accepted) return state
        var s = state
        var changed = false
        val newProgressMap = HashMap(s.jobs.progress)
        for (job in JobId.values()) {
            if (s.player.level < job.requiredPlayerLevel) continue
            val cur = newProgressMap[job.name] ?: JobProgress(jobName = job.name)
            if (!cur.unlocked) {
                newProgressMap[job.name] = cur.copy(unlocked = true)
                s = notify(s, NotificationKind.SUCCESS,
                    "${job.emoji} Oficio desbloqueado",
                    "${job.displayName} ya está disponible. ${job.description}")
                changed = true
            }
        }
        return if (changed) s.copy(jobs = s.jobs.copy(progress = newProgressMap)) else s
    }

    /**
     * Trabaja un turno (1h in-game) en el oficio dado. Sin mini-juego en
     * este commit — recompensa instantánea calculada con el wage modulado
     * por nivel del oficio + stat preferido + nivel del jugador.
     */
    fun workShift(state: GameState, job: JobId): GameState {
        if (!state.jobs.accepted) {
            return notify(state, NotificationKind.ERROR, "Sin acceso",
                "Acepta primero la bolsa de empleo.")
        }
        val cur = state.jobs.progressOf(job)
        if (!cur.unlocked) {
            return notify(state, NotificationKind.ERROR, "🔒 Oficio bloqueado",
                "Necesitas nivel ${job.requiredPlayerLevel} (tu nivel: ${state.player.level}).")
        }
        if (state.player.energy < job.energyCost) {
            return notify(state, NotificationKind.WARNING, "Sin energía",
                "Necesitas ${job.energyCost} ⚡ y tienes ${state.player.energy}. Descansa.")
        }

        val statValue = statValueFor(state.player, job.preferredStat)
        val wageBase = computeWage(job, cur.level, statValue, state.player.level)
        val wage = wageBase * state.traitTree.multiplierFor(TraitEffectType.JOB_WAGE_MUL)

        // XP del oficio + posible level-up.
        val newXp = cur.xpInLevel + SHIFT_BASE_XP
        val (finalLevel, finalXp) = if (cur.level < MAX_LEVEL && newXp >= XP_PER_LEVEL) {
            val gained = newXp / XP_PER_LEVEL
            val newLevel = min(MAX_LEVEL, cur.level + gained)
            val carry = if (newLevel >= MAX_LEVEL) 0 else newXp % XP_PER_LEVEL
            newLevel to carry
        } else {
            cur.level to newXp
        }

        val updatedProgress = cur.copy(
            level = finalLevel,
            xpInLevel = finalXp,
            shiftsWorked = cur.shiftsWorked + 1,
            totalEarned = cur.totalEarned + wage,
            lastShiftTick = state.tick
        )

        val shiftResult = JobShiftResult(
            jobName = job.name,
            cashEarned = wage,
            xpGained = SHIFT_BASE_XP,
            level = finalLevel,
            day = state.day
        )

        val newPlayer = state.player
            .copy(cash = state.player.cash + wage)
            .withEnergy(-job.energyCost)
            .addXp(PLAYER_XP_PER_SHIFT)

        val newJobs = state.jobs.copy(
            progress = state.jobs.progress + (job.name to updatedProgress),
            totalShifts = state.jobs.totalShifts + 1,
            totalEarned = state.jobs.totalEarned + wage,
            recentShifts = (state.jobs.recentShifts + shiftResult).takeLast(20)
        )

        val leveledUp = finalLevel > cur.level
        val title = if (leveledUp)
            "${job.emoji} ¡Subiste a nivel $finalLevel en ${job.displayName}!"
        else
            "${job.emoji} Turno completado: ${job.displayName}"
        val msg = "Has ganado ${"%,.2f".format(wage)} € + $SHIFT_BASE_XP XP de oficio. " +
            "Energía: -${job.energyCost} ⚡."

        return notify(
            state.copy(player = newPlayer, jobs = newJobs),
            if (leveledUp) NotificationKind.SUCCESS else NotificationKind.INFO,
            title,
            msg
        )
    }

    /** Cálculo de wage modulado por nivel-oficio + stat + nivel jugador. */
    private fun computeWage(job: JobId, jobLevel: Int, statValue: Int, playerLevel: Int): Double {
        val levelMul = 1.0 + (jobLevel - 1) * 0.05
        val statMul = 1.0 + statValue.coerceAtLeast(0) * 0.005
        val playerMul = 1.0 + (playerLevel - 1) * 0.01
        return job.baseHourlyWage * levelMul * statMul * playerMul
    }

    /** Lee el stat correspondiente del player. */
    private fun statValueFor(player: Player, stat: JobStat): Int = when (stat) {
        JobStat.INT -> player.stats.intelligence
        JobStat.STR -> player.stats.strength
        JobStat.CHA -> player.stats.charisma
        JobStat.LUC -> player.stats.luck
        JobStat.DEX -> player.stats.dexterity
    }

    /** Preview del wage sin aplicarlo (para UI). */
    fun previewWage(state: GameState, job: JobId): Double {
        val cur = state.jobs.progressOf(job)
        val statValue = statValueFor(state.player, job.preferredStat)
        return computeWage(job, cur.level, statValue, state.player.level) *
            state.traitTree.multiplierFor(TraitEffectType.JOB_WAGE_MUL)
    }

    /**
     * Variante de [workShift] usada cuando un mini-juego ha terminado.
     * `scoreMul` es un multiplicador en [0.5, 1.5] derivado del rendimiento:
     *   - score 0%   → 0.5× wage (te llevas algo por intentarlo).
     *   - score 50%  → 1.0× wage (rendimiento medio).
     *   - score 100% → 1.5× wage (clavado).
     *
     * Misma validación de unlock + energy + level-up que workShift, pero
     * sin caps: si haces un score perfecto el wage final puede llegar a
     * 1.5× del normal.
     */
    fun workShiftWithScore(state: GameState, job: JobId, scoreMul: Double): GameState {
        if (!state.jobs.accepted) {
            return notify(state, NotificationKind.ERROR, "Sin acceso",
                "Acepta primero la bolsa de empleo.")
        }
        val cur = state.jobs.progressOf(job)
        if (!cur.unlocked) {
            return notify(state, NotificationKind.ERROR, "🔒 Oficio bloqueado",
                "Necesitas nivel ${job.requiredPlayerLevel} (tu nivel: ${state.player.level}).")
        }
        if (state.player.energy < job.energyCost) {
            return notify(state, NotificationKind.WARNING, "Sin energía",
                "Necesitas ${job.energyCost} ⚡ y tienes ${state.player.energy}.")
        }

        val statValue = statValueFor(state.player, job.preferredStat)
        val baseWage = computeWage(job, cur.level, statValue, state.player.level) *
            state.traitTree.multiplierFor(TraitEffectType.JOB_WAGE_MUL)
        val safeMul = scoreMul.coerceIn(0.5, 1.5)
        val wage = baseWage * safeMul

        // XP del oficio escala con score: rinde mejor → más XP.
        val shiftXp = (SHIFT_BASE_XP * safeMul).toInt().coerceAtLeast(5)

        val newXp = cur.xpInLevel + shiftXp
        val (finalLevel, finalXp) = if (cur.level < MAX_LEVEL && newXp >= XP_PER_LEVEL) {
            val gained = newXp / XP_PER_LEVEL
            val newLevel = min(MAX_LEVEL, cur.level + gained)
            val carry = if (newLevel >= MAX_LEVEL) 0 else newXp % XP_PER_LEVEL
            newLevel to carry
        } else {
            cur.level to newXp
        }

        val updatedProgress = cur.copy(
            level = finalLevel,
            xpInLevel = finalXp,
            shiftsWorked = cur.shiftsWorked + 1,
            totalEarned = cur.totalEarned + wage,
            lastShiftTick = state.tick
        )

        val shiftResult = JobShiftResult(
            jobName = job.name,
            cashEarned = wage,
            xpGained = shiftXp,
            level = finalLevel,
            day = state.day
        )

        val newPlayer = state.player
            .copy(cash = state.player.cash + wage)
            .withEnergy(-job.energyCost)
            .addXp(PLAYER_XP_PER_SHIFT)

        val newJobs = state.jobs.copy(
            progress = state.jobs.progress + (job.name to updatedProgress),
            totalShifts = state.jobs.totalShifts + 1,
            totalEarned = state.jobs.totalEarned + wage,
            recentShifts = (state.jobs.recentShifts + shiftResult).takeLast(20)
        )

        val perfPct = (safeMul * 100).toInt() - 50
        val rating = when {
            safeMul >= 1.40 -> "🌟 Brillante"
            safeMul >= 1.10 -> "👏 Bien"
            safeMul >= 0.90 -> "🙂 Aceptable"
            safeMul >= 0.70 -> "😬 Justo"
            else -> "💤 Flojo"
        }
        val leveledUp = finalLevel > cur.level
        val title = if (leveledUp)
            "${job.emoji} ¡Subiste a nivel $finalLevel!"
        else
            "${job.emoji} Turno: ${job.displayName} — $rating"
        val msg = "Has ganado ${"%,.2f".format(wage)} € (${if (perfPct >= 0) "+" else ""}$perfPct% sobre base) " +
            "+ $shiftXp XP. Energía: -${job.energyCost} ⚡."

        return notify(
            state.copy(player = newPlayer, jobs = newJobs),
            if (leveledUp) NotificationKind.SUCCESS
            else if (safeMul >= 1.10) NotificationKind.SUCCESS
            else if (safeMul >= 0.90) NotificationKind.INFO
            else NotificationKind.WARNING,
            title, msg
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

    @Suppress("unused")
    private val keepImports = max(0, 0)
}
