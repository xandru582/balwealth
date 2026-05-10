package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Empresas de oficios (Fase C de MEJORAS.md jobs roadmap).
 *
 * El jugador puede abrir una empresa de cualquier oficio que tenga
 * desbloqueado, contratar empleados especializados, y la empresa
 * produce ingresos pasivos mientras está corriendo el game loop
 * (incluso offline progress, capeado a 24h).
 *
 * Modelo:
 *  - **JobEmployee**: trabajador especializado en un oficio concreto, con
 *    skill 30..90, salario diario proporcional a skill y baseHourlyWage.
 *  - **JobBusinessState**: una empresa abierta (treasury propio,
 *    upgradeLevel, lista de empleados, lifetime stats).
 *  - **JobBusinessesState**: mapa por jobId.name + pool de candidatos
 *    refrescables por oficio.
 *
 * Cada negocio tiene SU PROPIO treasury — el jugador "cobra" para mover
 * cash del business a `state.company.cash`. Salarios se pagan al cierre
 * de cada día desde el treasury del negocio. Si el treasury no llega,
 * los empleados se van automáticamente.
 *
 * Todo @Serializable con defaults para que saves antiguos carguen.
 */

/** Empleado especializado en un oficio. */
@Serializable
data class JobEmployee(
    val id: String,
    val name: String,
    /** Skill 30..90 — modula tanto los ingresos como el salario. */
    val skill: Int,
    /** Tick en que entró a trabajar (informativo). */
    val hiredAtTick: Long = 0L
) {
    /** Salario diario de este empleado en €. Depende del wage base del oficio. */
    fun dailySalary(jobBaseWage: Double): Double = jobBaseWage * skill * 0.05

    /** Ingreso por hora que aporta este empleado al business. */
    fun hourlyRevenue(jobBaseWage: Double): Double = jobBaseWage * (skill / 100.0)
}

/** Una empresa concreta del jugador para un oficio. */
@Serializable
data class JobBusinessState(
    val jobName: String,
    /** Cash acumulado en el negocio (sin transferir a la empresa principal). */
    val treasury: Double = 0.0,
    /** Nivel del local 1..5. Cada nivel multiplica revenue x(1 + 0.20·level). */
    val upgradeLevel: Int = 1,
    /** Empleados contratados. Capacidad = 2 + upgradeLevel (max 7). */
    val employees: List<JobEmployee> = emptyList(),
    /** Pool de candidatos disponibles para contratar. */
    val candidatePool: List<JobEmployee> = emptyList(),
    /** Tick del último cálculo de producción para acumulado correcto. */
    val lastProductionTick: Long = 0L,
    /** Tick del último pago de salarios (para que no se duplique). */
    val lastPayrollDay: Int = -1,
    /** Lifetime totals. */
    val totalEarned: Double = 0.0,
    val totalPaidSalaries: Double = 0.0,
    val totalCollected: Double = 0.0
) {
    /** Capacidad de empleados según upgradeLevel: 2..7. */
    val maxEmployees: Int get() = 2 + upgradeLevel

    /** Multiplicador de revenue por nivel del local. */
    val upgradeMul: Double get() = 1.0 + upgradeLevel * 0.20

    /** Coste diario total de salarios. */
    fun dailySalaries(jobBaseWage: Double): Double =
        employees.sumOf { it.dailySalary(jobBaseWage) }

    /** Ingreso por hora total que generan los empleados. */
    fun hourlyRevenue(jobBaseWage: Double): Double =
        employees.sumOf { it.hourlyRevenue(jobBaseWage) } * upgradeMul
}

/** Estado global del subsistema. */
@Serializable
data class JobBusinessesState(
    /** Map por JobId.name → estado del negocio (si está abierto). */
    val businesses: Map<String, JobBusinessState> = emptyMap(),
    /** Tick del último refresh de candidatos (para limitar refresh). */
    val lastCandidatesRefresh: Long = 0L
) {
    fun businessOf(job: JobId): JobBusinessState? = businesses[job.name]
    fun hasBusinessFor(job: JobId): Boolean = job.name in businesses

    /** Lifetime totals derivados. */
    val totalLifetimeEarned: Double get() = businesses.values.sumOf { it.totalEarned }
    val totalLifetimePaid: Double get() = businesses.values.sumOf { it.totalPaidSalaries }
    val totalLifetimeCollected: Double get() = businesses.values.sumOf { it.totalCollected }
    val openCount: Int get() = businesses.size
    val totalEmployees: Int get() = businesses.values.sumOf { it.employees.size }
}

/**
 * Costes y constantes de la economía del sistema. Reutilizables desde
 * UI para previews y desde Engine para validación.
 */
object JobBusinessCatalog {
    /** Coste de apertura: baseHourlyWage × 100. */
    fun openingFee(job: JobId): Double = job.baseHourlyWage * 100.0

    /** Coste de mejora del local al siguiente nivel. */
    fun upgradeCost(job: JobId, fromLevel: Int): Double =
        job.baseHourlyWage * 80.0 * (fromLevel + 1)

    /** Coste de fichar a un candidato (signing bonus = 1 día de salario). */
    fun signingFee(job: JobId, candidate: JobEmployee): Double =
        candidate.dailySalary(job.baseHourlyWage)

    /** Indemnización al despedir (2 días). */
    fun severance(job: JobId, employee: JobEmployee): Double =
        employee.dailySalary(job.baseHourlyWage) * 2.0

    /** Refund por cerrar el negocio (50% de la fee de apertura). */
    fun closingRefund(job: JobId): Double = openingFee(job) * 0.50

    /** Cap de horas offline acumulables: 24h = 1.440 ticks. */
    const val OFFLINE_CAP_TICKS = 1_440L

    /** Lista de nombres genéricos para generación procedural. */
    val FIRST_NAMES = listOf(
        "Iris", "Hugo", "Sora", "Ace", "Marta", "Liam", "Noa", "Aitor",
        "Lucía", "Dani", "Vega", "Bruno", "Olivia", "Mateo", "Inés"
    )
    val LAST_NAMES = listOf(
        "Beltrán", "Castaño", "Domínguez", "Espina", "Folch", "Garrido",
        "Hervás", "Iturbe", "Jáuregui", "Linares", "Montoya", "Núñez"
    )
}
