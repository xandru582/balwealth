package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Motor de heists. Gating por nivel/reputación. El jugador planifica,
 * paga, ejecuta y resuelve probabilísticamente. Los heists tienen cooldown
 * tras ejecución (3 días in-game). El heat sube con cada heist y dispara
 * eventos cuando supera ciertos umbrales.
 */
object HeistEngine {

    private const val DAY_TICKS = 1_440L
    private const val COOLDOWN_DAYS = 3L
    private const val HEAT_DECAY_DAILY = 4

    fun canUnlock(state: GameState): Boolean =
        state.player.level >= 5

    fun unlock(state: GameState): GameState {
        val hs = state.heists
        if (hs.unlocked) return state
        if (!canUnlock(state)) {
            return notify(state, NotificationKind.ERROR, "🔒 Heists bloqueados",
                "Necesitas nivel 5 para acceder al inframundo.")
        }
        return notify(
            state.copy(heists = hs.copy(
                unlocked = true,
                heists = HeistCatalog.freshHeists(),
                crewPool = CrewGenerator.generate(Random(state.tick), seed = 0)
            )),
            NotificationKind.SUCCESS,
            "🦹 Bienvenido al inframundo",
            "Has desbloqueado los heists. Cuidado con el karma — hay caminos sin retorno."
        )
    }

    // ===================== Tick diario =====================

    fun tickDaily(state: GameState, rng: Random): GameState {
        var s = state
        var hs = s.heists
        if (!hs.unlocked) return s

        // Decay de heat
        if (hs.heat > 0) {
            hs = hs.copy(heat = max(0, hs.heat - HEAT_DECAY_DAILY))
        }

        // Si heat >= 70 → evento de policía con cierta probabilidad
        if (hs.heat >= 70 && rng.nextDouble() < 0.3) {
            val seized = s.company.cash * 0.05
            s = s.copy(company = s.company.copy(cash = max(0.0, s.company.cash - seized)))
            s = notify(s, NotificationKind.WARNING, "👮 Embargo policial",
                "El heat te ha pasado factura. Te embargan ${"%,.0f".format(seized)} €.")
            hs = hs.copy(heat = max(0, hs.heat - 15))
        }

        // Refresh del crew pool cada 7 días
        if (s.tick - hs.lastCrewRefreshTick > DAY_TICKS * 7) {
            val newPool = CrewGenerator.generate(rng, seed = s.day)
            // Mantén los reclutados, rota el resto
            val keptIds = hs.recruitedCrew.toSet()
            val keptMembers = hs.crewPool.filter { it.id in keptIds }
            hs = hs.copy(
                crewPool = (keptMembers + newPool).distinctBy { it.id },
                lastCrewRefreshTick = s.tick
            )
        }

        // Actualizar status de heists según unlock conditions y cooldowns
        val newHeists = hs.heists.map { inst ->
            val def = HeistCatalog.byType(inst.type) ?: return@map inst
            val newStatus = when (inst.status) {
                HeistStatus.LOCKED -> {
                    if (s.player.level >= def.unlockLevel && s.company.reputation >= def.unlockReputation)
                        HeistStatus.AVAILABLE
                    else HeistStatus.LOCKED
                }
                HeistStatus.COOLDOWN -> {
                    if (s.tick >= inst.cooldownUntilTick) HeistStatus.AVAILABLE else HeistStatus.COOLDOWN
                }
                else -> inst.status
            }
            inst.copy(status = newStatus)
        }
        hs = hs.copy(heists = newHeists)
        return s.copy(heists = hs)
    }

    // ===================== Acciones del jugador =====================

    fun recruit(state: GameState, crewId: String): GameState {
        val hs = state.heists
        val member = hs.crewPool.find { it.id == crewId } ?: return state
        if (crewId in hs.recruitedCrew) {
            return notify(state, NotificationKind.WARNING, "Ya fichado", "${member.name} ya está en tu tripulación.")
        }
        if (state.company.cash < member.recruitFee) {
            return notify(state, NotificationKind.ERROR, "Sin fondos",
                "Necesitas ${"%,.0f".format(member.recruitFee)} €.")
        }
        val newState = state.copy(
            company = state.company.copy(cash = state.company.cash - member.recruitFee),
            heists = hs.copy(recruitedCrew = hs.recruitedCrew + crewId)
        )
        return notify(newState, NotificationKind.SUCCESS,
            "🤝 Fichaje cerrado",
            "${member.name} (${member.role.displayName}, skill ${member.skill}) se une al equipo.")
    }

    fun fireCrew(state: GameState, crewId: String): GameState {
        val hs = state.heists
        if (crewId !in hs.recruitedCrew) return state
        return state.copy(heists = hs.copy(recruitedCrew = hs.recruitedCrew - crewId))
    }

    fun planHeist(
        state: GameState,
        heistInstanceId: String,
        crewIds: List<String>,
        approach: HeistApproach,
        gearSpend: Double
    ): GameState {
        val hs = state.heists
        val inst = hs.heists.find { it.id == heistInstanceId } ?: return state
        if (inst.status != HeistStatus.AVAILABLE) {
            return notify(state, NotificationKind.ERROR, "No disponible",
                "Este heist no está disponible ahora.")
        }
        val def = HeistCatalog.byType(inst.type) ?: return state

        // Verificar tripulación
        val crew = crewIds.mapNotNull { id -> hs.crewPool.find { it.id == id } }
            .filter { it.id in hs.recruitedCrew }
        if (crew.size != crewIds.size) {
            return notify(state, NotificationKind.ERROR, "Tripulación inválida",
                "Algún miembro no está fichado.")
        }
        if (!hasRequiredRoles(crew, def.requiredRoles)) {
            return notify(state, NotificationKind.ERROR, "Roles insuficientes",
                "Necesitas: ${def.requiredRoles.joinToString { it.displayName }}.")
        }
        if (state.company.cash < gearSpend) {
            return notify(state, NotificationKind.ERROR, "Sin fondos para equipo",
                "Necesitas ${"%,.0f".format(gearSpend)} €.")
        }

        val plan = HeistPlan(heistId = inst.id, crewIds = crewIds, approach = approach, gearSpent = gearSpend)
        val newInst = inst.copy(status = HeistStatus.PLANNING, plan = plan)
        val newState = state.copy(
            company = state.company.copy(cash = state.company.cash - gearSpend),
            heists = hs.copy(heists = hs.heists.map { if (it.id == inst.id) newInst else it })
        )
        return notify(newState, NotificationKind.INFO,
            "📋 Plan registrado",
            "Heist ${def.type.displayName} listo. Pulsa EJECUTAR cuando quieras.")
    }

    fun execute(state: GameState, heistInstanceId: String): GameState {
        val hs = state.heists
        val inst = hs.heists.find { it.id == heistInstanceId } ?: return state
        if (inst.status != HeistStatus.PLANNING || inst.plan == null) return state
        val def = HeistCatalog.byType(inst.type) ?: return state
        val rng = Random(state.tick xor inst.id.hashCode().toLong())

        // Fórmula de éxito:
        //   teamSkillAvg / 99
        //   + approach matching (loud bonus si requiere SHARPSHOOTER, stealth si HACKER, negotiate baja heat)
        //   + gear bonus (gearSpend / 200_000)
        //   - difficulty / 100
        val crew = inst.plan.crewIds.mapNotNull { id -> hs.crewPool.find { it.id == id } }
        val teamAvgSkill = if (crew.isNotEmpty()) crew.sumOf { it.skill } / crew.size.toDouble() else 0.0
        val approachBonus = when (inst.plan.approach) {
            HeistApproach.LOUD -> if (CrewRole.SHARPSHOOTER in crew.map { it.role }) 0.10 else -0.05
            HeistApproach.STEALTH -> if (CrewRole.HACKER in crew.map { it.role }) 0.10 else -0.05
            HeistApproach.NEGOTIATE -> if (CrewRole.LEADER in crew.map { it.role }) 0.05 else -0.10
        }
        val gearBonus = (inst.plan.gearSpent / 250_000.0).coerceAtMost(0.30)
        val luckBonus = (state.player.stats.luck - 5) * 0.005

        val score = (teamAvgSkill / 99.0) + approachBonus + gearBonus + luckBonus -
            (def.baseDifficulty / 100.0)

        val r = rng.nextDouble()
        val outcome = when {
            r < 0.15 + score * 0.20 -> HeistOutcome.PERFECT
            r < 0.65 + score * 0.20 -> HeistOutcome.SUCCESS
            r < 0.85 -> HeistOutcome.ESCAPE
            else -> HeistOutcome.DISASTER
        }

        val (rewardMul, heatMul, lossMul) = when (outcome) {
            HeistOutcome.PERFECT -> Triple(1.5, 0.6, 0.0)
            HeistOutcome.SUCCESS -> Triple(1.0, 1.0, 0.0)
            HeistOutcome.ESCAPE -> Triple(0.3, 1.4, 0.5)
            HeistOutcome.DISASTER -> Triple(0.0, 1.8, 1.5)
        }

        val grossLoot = def.baseReward * rewardMul
        val cutPaid = grossLoot * crew.sumOf { it.cutPct }
        val net = max(0.0, grossLoot - cutPaid)
        val gearLoss = if (outcome == HeistOutcome.DISASTER) inst.plan.gearSpent * lossMul else 0.0
        val heatGain = (def.heatBase * heatMul).toInt()

        var s = state.copy(
            company = state.company.copy(cash = state.company.cash + net - gearLoss),
            storyline = state.storyline.copy(karma = (state.storyline.karma + def.karmaImpact).coerceIn(-100, 100))
        )

        // Si DISASTER → algunos miembros caen presos
        var newPool = hs.crewPool
        var newRecruited = hs.recruitedCrew
        if (outcome == HeistOutcome.DISASTER) {
            val fallen = crew.filter { rng.nextDouble() < 0.5 }
            for (f in fallen) {
                newPool = newPool.map { if (it.id == f.id) it.copy(available = false) else it }
                newRecruited = newRecruited - f.id
            }
            if (fallen.isNotEmpty()) {
                s = notify(s, NotificationKind.ERROR,
                    "👮 ${fallen.size} miembros detenidos",
                    "Tu equipo paga el precio: ${fallen.joinToString { it.name }}.")
            }
        }

        val cooldownUntil = s.tick + COOLDOWN_DAYS * DAY_TICKS
        val newInst = inst.copy(
            status = HeistStatus.COOLDOWN,
            outcome = outcome,
            payoutCash = net,
            cooldownUntilTick = cooldownUntil
        )
        val newHeists = hs.heists.map { if (it.id == inst.id) newInst else it }
        s = s.copy(heists = hs.copy(
            heists = newHeists,
            crewPool = newPool,
            recruitedCrew = newRecruited,
            heat = (hs.heat + heatGain).coerceIn(0, 100),
            totalHeists = hs.totalHeists + 1,
            totalLoot = hs.totalLoot + net,
            perfectRuns = if (outcome == HeistOutcome.PERFECT) hs.perfectRuns + 1 else hs.perfectRuns,
            disasters = if (outcome == HeistOutcome.DISASTER) hs.disasters + 1 else hs.disasters
        ))
        return notify(s,
            when (outcome) {
                HeistOutcome.PERFECT -> NotificationKind.SUCCESS
                HeistOutcome.SUCCESS -> NotificationKind.SUCCESS
                HeistOutcome.ESCAPE -> NotificationKind.WARNING
                HeistOutcome.DISASTER -> NotificationKind.ERROR
            },
            "${outcome.emoji} Heist resuelto: ${def.type.displayName}",
            "Resultado: ${outcome.displayName} · botín neto ${"%,.0f".format(net)} € · heat +$heatGain.")
    }

    private fun hasRequiredRoles(crew: List<CrewMember>, required: List<CrewRole>): Boolean {
        val rolesMutable = crew.map { it.role }.toMutableList()
        for (r in required) {
            val idx = rolesMutable.indexOf(r)
            if (idx < 0) return false
            rolesMutable.removeAt(idx)
        }
        return true
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
