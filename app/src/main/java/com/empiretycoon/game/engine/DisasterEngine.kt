package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.math.max
import kotlin.random.Random

/**
 * Motor de desastres. Cada N días dispara uno semialeatorio. Aplica efectos
 * inmediatos al spawn, mantiene modificadores activos durante la duración,
 * y ofrece ventana de mitigación de 24h.
 */
object DisasterEngine {

    /** Probabilidad por día (cuando ya pasó el cooldown). */
    private const val DAILY_TRIGGER = 0.05

    /** Tick de día (modulo). */
    private const val DAY_TICKS = 1_440L

    fun tickDaily(state: GameState, rng: Random): GameState {
        var s = state
        var ds = s.disasters

        // Pago del seguro
        if (ds.insuranceActive) {
            val cost = ds.insuranceDailyCost
            if (s.company.cash >= cost) {
                s = s.copy(company = s.company.copy(cash = s.company.cash - cost))
            } else {
                ds = ds.copy(insuranceActive = false)
                s = notify(s, NotificationKind.WARNING, "🛡️ Seguro cancelado",
                    "Sin liquidez para la prima. La cobertura ha caducado.")
            }
        }

        // Spawn de desastre
        val daysSinceLast = s.day - ds.lastDisasterDay
        val canSpawn = daysSinceLast >= ds.cooldownDays && ds.active.isEmpty()
        if (canSpawn && rng.nextDouble() < DAILY_TRIGGER) {
            val kind = pickKind(rng)
            val severity = pickSeverity(rng, s.day)
            val disaster = DisasterCatalog.build(kind, severity, s.tick)
            ds = ds.copy(
                active = ds.active + disaster,
                lastDisasterDay = s.day
            )
            s = notify(s, NotificationKind.ERROR,
                disaster.title,
                disaster.description + " Tienes 24h in-game para mitigar.")
        }

        // Avanzar fases y resolver expirados
        val resolved = mutableListOf<DisasterHistory>()
        val newActive = mutableListOf<ActiveDisaster>()
        for (d in ds.active) {
            // Pasar de PENDING_RESPONSE → ACTIVE al expirar la deadline
            val nowPhase = when {
                d.phase == DisasterPhase.PENDING_RESPONSE && s.tick >= d.mitigationDeadlineTick ->
                    DisasterPhase.ACTIVE
                else -> d.phase
            }
            val updated = d.copy(phase = nowPhase)

            // Aplicar daño inicial cuando pasamos a ACTIVE
            val sBefore = s
            if (d.phase == DisasterPhase.PENDING_RESPONSE && nowPhase == DisasterPhase.ACTIVE) {
                s = applyImmediateDamage(s, d, rng)
            }

            // Expirar
            if (s.tick >= d.expiresAtTick) {
                resolved.add(DisasterHistory(
                    id = d.id,
                    kind = d.kind,
                    severity = d.severity,
                    day = s.day,
                    mitigated = d.mitigated,
                    cashLost = max(0.0, sBefore.company.cash - s.company.cash),
                    outcome = if (d.mitigated) "mitigado" else "absorbido"
                ))
                ds = ds.copy(resilienceXp = ds.resilienceXp + xpReward(d.severity))
            } else {
                newActive.add(updated)
            }
        }

        ds = ds.copy(active = newActive, history = (ds.history + resolved).takeLast(40))
        return s.copy(disasters = ds)
    }

    private fun applyImmediateDamage(state: GameState, d: ActiveDisaster, rng: Random): GameState {
        val cov = if (state.disasters.insuranceActive) state.disasters.insuranceCoverage else 0.0
        val sevMag = sevMagnitude(d.severity)
        var s = state
        when (d.kind) {
            DisasterKind.EARTHQUAKE, DisasterKind.HURRICANE, DisasterKind.FIRE -> {
                val buildings = s.company.buildings
                val damaged = (buildings.size * 0.20 * sevMag).toInt().coerceIn(1, buildings.size)
                if (damaged > 0 && buildings.isNotEmpty()) {
                    val toHit = buildings.shuffled(rng).take(damaged).map { it.id }.toSet()
                    val newBuildings = buildings.map { b ->
                        if (b.id in toHit) b.copy(level = max(1, b.level - 1)) else b
                    }
                    s = s.copy(company = s.company.copy(buildings = newBuildings))
                }
            }
            DisasterKind.FLOOD -> {
                val invDmg = 0.20 * sevMag
                val newInv = s.company.inventory.mapValues { (_, qty) -> (qty * (1.0 - invDmg)).toInt() }
                s = s.copy(company = s.company.copy(inventory = newInv))
            }
            DisasterKind.HACK -> {
                val stolen = s.company.cash * (0.05 + 0.05 * sevMag).coerceAtMost(0.20)
                val recovered = stolen * cov
                val net = stolen - recovered
                s = s.copy(company = s.company.copy(cash = s.company.cash - net))
            }
            DisasterKind.FLASH_CRASH -> {
                val newStocks = s.stocks.map {
                    it.copy(price = max(0.10, it.price * (1.0 - 0.20 * sevMag)))
                }
                s = s.copy(stocks = newStocks)
            }
            DisasterKind.ARMED_ROBBERY -> {
                val stolen = s.player.cash * (0.30 + 0.10 * sevMag).coerceAtMost(0.50)
                val recovered = stolen * cov
                val net = stolen - recovered
                s = s.copy(player = s.player.copy(cash = s.player.cash - net))
            }
            DisasterKind.BOYCOTT -> {
                val repHit = (15 * sevMag).toInt()
                s = s.copy(company = s.company.copy(reputation = (s.company.reputation - repHit).coerceAtLeast(0)))
            }
            DisasterKind.PANDEMIC -> {
                // Empleados pierden lealtad (lealtad es 0..1)
                val newEmps = s.company.employees.map {
                    it.copy(loyalty = max(0.0, it.loyalty - 0.10))
                }
                s = s.copy(company = s.company.copy(employees = newEmps))
            }
            else -> { /* sin daño instantáneo extra */ }
        }
        return s
    }

    // ===================== Mitigación (acciones del jugador) =====================

    fun mitigate(state: GameState, disasterId: String, strategy: MitigationStrategy): GameState {
        val ds = state.disasters
        val d = ds.active.find { it.id == disasterId } ?: return state
        if (d.phase != DisasterPhase.PENDING_RESPONSE) {
            return notify(state, NotificationKind.ERROR, "Fuera de plazo",
                "La ventana de mitigación ha cerrado.")
        }
        val cost = mitigationCost(d, strategy)
        if (state.company.cash < cost) {
            return notify(state, NotificationKind.ERROR, "Sin fondos",
                "Necesitas ${"%,.0f".format(cost)} € para esta acción.")
        }
        val (prodImp, sellImp, buyImp) = mitigationBoost(strategy)
        val newD = d.copy(
            mitigated = true,
            productionMul = (d.productionMul + prodImp).coerceAtMost(1.0),
            sellPriceMul = (d.sellPriceMul + sellImp).coerceIn(0.1, 2.0),
            buyPriceMul = (d.buyPriceMul - buyImp).coerceAtLeast(0.5)
        )
        val newActive = ds.active.map { if (it.id == disasterId) newD else it }
        val updated = state.copy(
            disasters = ds.copy(active = newActive),
            company = state.company.copy(cash = state.company.cash - cost)
        )
        return notify(updated, NotificationKind.SUCCESS,
            "🛠️ Mitigación aplicada",
            "${strategy.label} activada. Daños reducidos.")
    }

    private fun mitigationCost(d: ActiveDisaster, strat: MitigationStrategy): Double {
        val sev = sevMagnitude(d.severity)
        return when (strat) {
            MitigationStrategy.EMERGENCY_RESPONSE -> 80_000.0 * sev
            MitigationStrategy.PR_CAMPAIGN -> 50_000.0 * sev
            MitigationStrategy.DONATIONS -> 120_000.0 * sev
            MitigationStrategy.SECURITY_UPGRADE -> 60_000.0 * sev
            MitigationStrategy.TECH_RESPONSE -> 100_000.0 * sev
        }
    }

    private fun mitigationBoost(strat: MitigationStrategy): Triple<Double, Double, Double> = when (strat) {
        MitigationStrategy.EMERGENCY_RESPONSE -> Triple(0.20, 0.0, 0.0)
        MitigationStrategy.PR_CAMPAIGN -> Triple(0.0, 0.15, 0.0)
        MitigationStrategy.DONATIONS -> Triple(0.10, 0.10, 0.0)
        MitigationStrategy.SECURITY_UPGRADE -> Triple(0.0, 0.0, 0.15)
        MitigationStrategy.TECH_RESPONSE -> Triple(0.15, 0.05, 0.0)
    }

    // ===================== Insurance =====================

    fun toggleInsurance(state: GameState, on: Boolean): GameState {
        val cost = state.disasters.insuranceDailyCost
        if (on && state.company.cash < cost * 7) {
            return notify(state, NotificationKind.ERROR, "Sin fondos",
                "Necesitas al menos ${"%,.0f".format(cost * 7)} € (1 semana de prima).")
        }
        return notify(
            state.copy(disasters = state.disasters.copy(insuranceActive = on)),
            NotificationKind.SUCCESS,
            if (on) "🛡️ Seguro contratado" else "Seguro cancelado",
            if (on) "Cobertura ${"%.0f".format(state.disasters.insuranceCoverage * 100)}% activa."
            else "Has cancelado la póliza."
        )
    }

    // ===================== Helpers =====================

    private fun pickKind(rng: Random): DisasterKind {
        return DisasterKind.values().random(rng)
    }

    private fun pickSeverity(rng: Random, day: Int): DisasterSeverity {
        val r = rng.nextDouble()
        // Curva: cuanto más avanza el juego, más probable es severidad alta
        val midThreshold = 0.55 - (day.coerceAtMost(180) / 600.0)
        return when {
            r < midThreshold -> DisasterSeverity.LOW
            r < 0.85 -> DisasterSeverity.MEDIUM
            r < 0.97 -> DisasterSeverity.HIGH
            else -> DisasterSeverity.CATASTROPHIC
        }
    }

    private fun sevMagnitude(s: DisasterSeverity): Double = when (s) {
        DisasterSeverity.LOW -> 0.5
        DisasterSeverity.MEDIUM -> 1.0
        DisasterSeverity.HIGH -> 1.5
        DisasterSeverity.CATASTROPHIC -> 2.2
    }

    private fun xpReward(s: DisasterSeverity): Int = when (s) {
        DisasterSeverity.LOW -> 1
        DisasterSeverity.MEDIUM -> 3
        DisasterSeverity.HIGH -> 7
        DisasterSeverity.CATASTROPHIC -> 15
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

enum class MitigationStrategy(val label: String, val emoji: String) {
    EMERGENCY_RESPONSE("Respuesta de emergencia", "🚨"),
    PR_CAMPAIGN("Campaña de PR", "📣"),
    DONATIONS("Donaciones a la comunidad", "🤝"),
    SECURITY_UPGRADE("Mejora de seguridad", "🔒"),
    TECH_RESPONSE("Equipo técnico de respuesta", "💻")
}
