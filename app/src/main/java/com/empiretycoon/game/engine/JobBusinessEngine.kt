package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Motor de empresas de oficios (Fase C). Pure GameState -> GameState.
 *
 * Operaciones públicas:
 *   - openBusiness / closeBusiness
 *   - upgradeBusiness
 *   - refreshCandidates / hireEmployee / fireEmployee
 *   - tickAll(state) — productivo + payroll diario, llamado desde GameEngine
 *   - collectRevenue(state, jobId) — mueve treasury del negocio → company.cash
 *
 * Reglas:
 *   - Solo se puede abrir una empresa de un oficio si está unlocked en
 *     state.jobs.
 *   - Cada empleado genera (skill/100)·baseWage cada hora in-game.
 *   - El multiplicador del local (1+level·0.20) se aplica al revenue total.
 *   - Salarios se pagan al cambio de día. Si treasury no llega, los
 *     empleados se desbandan en orden inverso (el más caro primero) hasta
 *     que el resto sea pagable.
 *   - Offline progress capeado a OFFLINE_CAP_TICKS (24h) para evitar farms.
 */
object JobBusinessEngine {

    // ===================== Apertura / cierre =====================

    fun openBusiness(state: GameState, job: JobId): GameState {
        if (!state.jobs.accepted) {
            return notify(state, NotificationKind.ERROR, "Sin acceso",
                "Acepta primero la bolsa de empleo.")
        }
        if (!state.jobs.progressOf(job).unlocked) {
            return notify(state, NotificationKind.ERROR, "🔒 Oficio bloqueado",
                "Necesitas trabajar primero como ${job.displayName} antes de montar empresa.")
        }
        if (state.jobBusinesses.hasBusinessFor(job)) {
            return notify(state, NotificationKind.WARNING, "Ya abierta",
                "Ya tienes una ${job.displayName} en marcha.")
        }
        val fee = JobBusinessCatalog.openingFee(job)
        if (state.company.cash < fee) {
            return notify(state, NotificationKind.ERROR, "Sin fondos",
                "Apertura cuesta ${"%,.0f".format(fee)} € (en caja: ${"%,.0f".format(state.company.cash)} €).")
        }
        val newCompany = state.company.copy(cash = state.company.cash - fee)
        val newBusiness = JobBusinessState(
            jobName = job.name,
            lastProductionTick = state.tick,
            lastPayrollDay = state.day
        )
        val newJB = state.jobBusinesses.copy(
            businesses = state.jobBusinesses.businesses + (job.name to newBusiness)
        )
        return notify(
            state.copy(company = newCompany, jobBusinesses = newJB),
            NotificationKind.SUCCESS,
            "${job.emoji} Empresa abierta: ${job.displayName}",
            "Has pagado ${"%,.0f".format(fee)} € de apertura. Ahora contrata gente y empieza a generar."
        )
    }

    fun closeBusiness(state: GameState, job: JobId): GameState {
        val biz = state.jobBusinesses.businessOf(job) ?: return state
        val refund = JobBusinessCatalog.closingRefund(job) + biz.treasury
        // Indemnización para todos los empleados.
        val severanceTotal = biz.employees.sumOf {
            JobBusinessCatalog.severance(job, it)
        }
        val net = refund - severanceTotal
        val newCompany = state.company.copy(cash = state.company.cash + net)
        val newJB = state.jobBusinesses.copy(
            businesses = state.jobBusinesses.businesses - job.name
        )
        return notify(
            state.copy(company = newCompany, jobBusinesses = newJB),
            if (net >= 0) NotificationKind.WARNING else NotificationKind.ERROR,
            "${job.emoji} Empresa cerrada",
            "Refund + treasury ${"%,.0f".format(refund)} € − indemnizaciones ${"%,.0f".format(severanceTotal)} € = ${"%+,.0f".format(net)} €."
        )
    }

    // ===================== Upgrade =====================

    fun upgradeBusiness(state: GameState, job: JobId): GameState {
        val biz = state.jobBusinesses.businessOf(job)
            ?: return notify(state, NotificationKind.ERROR, "Sin empresa", "No tienes una.")
        if (biz.upgradeLevel >= 5) {
            return notify(state, NotificationKind.WARNING, "Nivel máximo",
                "Tu local ya está al máximo (nivel 5).")
        }
        val cost = JobBusinessCatalog.upgradeCost(job, biz.upgradeLevel)
        if (state.company.cash < cost) {
            return notify(state, NotificationKind.ERROR, "Sin fondos",
                "Mejora cuesta ${"%,.0f".format(cost)} €.")
        }
        val newCompany = state.company.copy(cash = state.company.cash - cost)
        val newBiz = biz.copy(upgradeLevel = biz.upgradeLevel + 1)
        return notify(
            state.copy(
                company = newCompany,
                jobBusinesses = state.jobBusinesses.copy(
                    businesses = state.jobBusinesses.businesses + (job.name to newBiz)
                )
            ),
            NotificationKind.SUCCESS,
            "🏗️ Local mejorado: nivel ${newBiz.upgradeLevel}",
            "Capacidad ahora ${newBiz.maxEmployees} empleados · revenue ×${"%.1f".format(newBiz.upgradeMul)}."
        )
    }

    // ===================== Candidatos / contratación =====================

    /** Genera 4 candidatos nuevos para todos los negocios abiertos. */
    fun refreshCandidates(state: GameState, rng: Random): GameState {
        if (state.jobBusinesses.businesses.isEmpty()) return state
        val newBusinesses = state.jobBusinesses.businesses.mapValues { (jobName, biz) ->
            val job = runCatching { JobId.valueOf(jobName) }.getOrNull() ?: return@mapValues biz
            biz.copy(candidatePool = generateCandidates(job, rng, state.tick))
        }
        return state.copy(jobBusinesses = state.jobBusinesses.copy(
            businesses = newBusinesses,
            lastCandidatesRefresh = state.tick
        ))
    }

    private fun generateCandidates(job: JobId, rng: Random, tick: Long): List<JobEmployee> {
        return (0 until 4).map {
            val skill = 30 + rng.nextInt(61)  // 30..90
            val first = JobBusinessCatalog.FIRST_NAMES.random(rng)
            val last = JobBusinessCatalog.LAST_NAMES.random(rng)
            JobEmployee(
                id = "jbe_${tick}_${System.nanoTime()}_$it",
                name = "$first $last",
                skill = skill
            )
        }
    }

    fun hireEmployee(state: GameState, job: JobId, candidateId: String): GameState {
        val biz = state.jobBusinesses.businessOf(job)
            ?: return notify(state, NotificationKind.ERROR, "Sin empresa", "No tienes empresa de ${job.displayName}.")
        val candidate = biz.candidatePool.find { it.id == candidateId }
            ?: return state
        if (biz.employees.size >= biz.maxEmployees) {
            return notify(state, NotificationKind.ERROR, "Sin sitio",
                "Capacidad máxima ${biz.maxEmployees}. Sube de nivel el local.")
        }
        val fee = JobBusinessCatalog.signingFee(job, candidate)
        if (state.company.cash < fee) {
            return notify(state, NotificationKind.ERROR, "Sin fondos",
                "Prima de fichaje ${"%,.0f".format(fee)} €.")
        }
        val hired = candidate.copy(hiredAtTick = state.tick)
        val newBiz = biz.copy(
            employees = biz.employees + hired,
            candidatePool = biz.candidatePool.filterNot { it.id == candidateId }
        )
        val newCompany = state.company.copy(cash = state.company.cash - fee)
        return notify(
            state.copy(
                company = newCompany,
                jobBusinesses = state.jobBusinesses.copy(
                    businesses = state.jobBusinesses.businesses + (job.name to newBiz)
                )
            ),
            NotificationKind.SUCCESS,
            "🤝 Fichaje cerrado",
            "${candidate.name} (skill ${candidate.skill}) entra en tu ${job.displayName}."
        )
    }

    fun fireEmployee(state: GameState, job: JobId, employeeId: String): GameState {
        val biz = state.jobBusinesses.businessOf(job) ?: return state
        val emp = biz.employees.find { it.id == employeeId } ?: return state
        val sev = JobBusinessCatalog.severance(job, emp)
        val newCompany = state.company.copy(cash = state.company.cash - sev)
        val newBiz = biz.copy(employees = biz.employees - emp)
        return notify(
            state.copy(
                company = newCompany,
                jobBusinesses = state.jobBusinesses.copy(
                    businesses = state.jobBusinesses.businesses + (job.name to newBiz)
                )
            ),
            NotificationKind.WARNING,
            "Despido",
            "${emp.name} se va con ${"%,.0f".format(sev)} € de indemnización."
        )
    }

    // ===================== Cobrar =====================

    fun collectRevenue(state: GameState, job: JobId): GameState {
        val biz = state.jobBusinesses.businessOf(job) ?: return state
        if (biz.treasury <= 0.01) {
            return notify(state, NotificationKind.WARNING, "Sin caja",
                "El treasury de ${job.displayName} está vacío.")
        }
        val amount = biz.treasury
        val newCompany = state.company.copy(cash = state.company.cash + amount)
        val newBiz = biz.copy(
            treasury = 0.0,
            totalCollected = biz.totalCollected + amount
        )
        return notify(
            state.copy(
                company = newCompany,
                jobBusinesses = state.jobBusinesses.copy(
                    businesses = state.jobBusinesses.businesses + (job.name to newBiz)
                )
            ),
            NotificationKind.SUCCESS,
            "💰 Cash recogido",
            "Has cobrado ${"%,.0f".format(amount)} € de ${job.displayName}."
        )
    }

    // ===================== Tick principal =====================

    /**
     * Llamado por GameEngine cada N ticks. Actualiza producción acumulada y
     * paga salarios al cambio de día.
     */
    fun tickAll(state: GameState): GameState {
        val jb = state.jobBusinesses
        if (jb.businesses.isEmpty()) return state
        var s = state
        for ((jobName, biz) in jb.businesses) {
            val job = runCatching { JobId.valueOf(jobName) }.getOrNull() ?: continue
            s = tickBusiness(s, job, biz)
        }
        return s
    }

    private fun tickBusiness(state: GameState, job: JobId, biz: JobBusinessState): GameState {
        // 1) Producción acumulada desde lastProductionTick (capeada a 24h offline)
        val rawTicksElapsed = (state.tick - biz.lastProductionTick).coerceAtLeast(0L)
        val ticksElapsed = min(rawTicksElapsed, JobBusinessCatalog.OFFLINE_CAP_TICKS)
        // 1 hora in-game = 60 ticks. revenue por hora = sum(employees) * upgradeMul.
        val revenuePerTick = biz.hourlyRevenue(job.baseHourlyWage) / 60.0
        val produced = revenuePerTick * ticksElapsed
        var newBiz = biz.copy(
            treasury = biz.treasury + produced,
            totalEarned = biz.totalEarned + produced,
            lastProductionTick = state.tick
        )

        // 2) Si cambió el día, pagar salarios
        // FIX P0: cap simétrico al offline (24h = 1 día). Sin esto, si el
        // jugador deja la app cerrada 5 días, se cobra revenue de 1 día
        // pero salarios de 5 → balance roto en su contra. El cap
        // OFFLINE_CAP_TICKS = 1440 ticks = 1 día, así que pagamos máximo
        // 1 día de nómina si el jugador estuvo días sin abrir.
        if (state.day > newBiz.lastPayrollDay) {
            val rawDaysToPay = (state.day - newBiz.lastPayrollDay).coerceAtLeast(1)
            val maxDaysFromCap = (JobBusinessCatalog.OFFLINE_CAP_TICKS / 1_440L).toInt().coerceAtLeast(1)
            val daysToPay = rawDaysToPay.coerceAtMost(maxDaysFromCap)
            // Pagamos 1 día de salarios; si pasaron varios días offline, pagamos
            // por cada día (proporcional). Treasury puede acabar negativo si
            // los empleados son caros — entonces los desbandamos en orden de
            // costo descendente hasta que el resto sea pagable.
            val salariesPerDay = newBiz.dailySalaries(job.baseHourlyWage)
            val totalSalaries = salariesPerDay * daysToPay
            if (newBiz.treasury >= totalSalaries) {
                newBiz = newBiz.copy(
                    treasury = newBiz.treasury - totalSalaries,
                    totalPaidSalaries = newBiz.totalPaidSalaries + totalSalaries,
                    lastPayrollDay = state.day
                )
            } else {
                // No hay treasury — empleados desbandan en orden de coste
                // descendente hasta que el resto sea pagable.
                var remainingTreasury = newBiz.treasury
                val emps = newBiz.employees.sortedByDescending { it.dailySalary(job.baseHourlyWage) }.toMutableList()
                while (emps.isNotEmpty() && remainingTreasury < emps.sumOf { it.dailySalary(job.baseHourlyWage) * daysToPay }) {
                    emps.removeAt(0)  // el más caro se va
                }
                val paid = emps.sumOf { it.dailySalary(job.baseHourlyWage) * daysToPay }
                val firedCount = newBiz.employees.size - emps.size
                newBiz = newBiz.copy(
                    employees = emps,
                    treasury = (remainingTreasury - paid).coerceAtLeast(0.0),
                    totalPaidSalaries = newBiz.totalPaidSalaries + paid,
                    lastPayrollDay = state.day
                )
                if (firedCount > 0) {
                    return notify(
                        state.copy(jobBusinesses = state.jobBusinesses.copy(
                            businesses = state.jobBusinesses.businesses + (job.name to newBiz)
                        )),
                        NotificationKind.ERROR,
                        "${job.emoji} ${job.displayName}: empleados se van",
                        "$firedCount empleados se han ido por impago. Cobra antes para evitarlo."
                    )
                }
            }
        }

        return state.copy(jobBusinesses = state.jobBusinesses.copy(
            businesses = state.jobBusinesses.businesses + (job.name to newBiz)
        ))
    }

    private fun notify(state: GameState, kind: NotificationKind, title: String, msg: String): GameState {
        val n = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = kind, title = title, message = msg
        )
        return state.copy(notifications = (state.notifications + n).takeLast(40))
    }

    @Suppress("unused") private val keepImports = max(0, 0)
}
