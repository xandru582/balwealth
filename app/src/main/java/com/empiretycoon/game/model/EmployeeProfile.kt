package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Rasgos de personalidad / desempeño que modulan productividad, salario,
 * lealtad y eventos sociales del empleado.
 */
@Serializable
enum class EmployeeTrait(
    val displayName: String,
    val emoji: String,
    val description: String,
    val productivityModifier: Double = 1.0,
    val salaryModifier: Double = 1.0,
    val burnoutModifier: Int = 0,
    val satisfactionModifier: Int = 0
) {
    LAZY("Vago", "😴",
        "Hace lo justo. -10% productividad.",
        productivityModifier = 0.90),
    EFFICIENT("Eficiente", "⚙️",
        "+8% productividad estable.",
        productivityModifier = 1.08),
    GENIUS("Genio", "🧠",
        "+20% productividad pero exige más sueldo.",
        productivityModifier = 1.20, salaryModifier = 1.15),
    NEGOTIATOR("Negociador", "🤝",
        "Acepta peor salario al firmar.",
        salaryModifier = 0.92),
    LOYAL("Leal", "🛡️",
        "Difícilmente abandona la empresa.",
        satisfactionModifier = +5),
    GREEDY("Codicioso", "💸",
        "Pide subidas constantes.",
        salaryModifier = 1.10, satisfactionModifier = -3),
    INSPIRING("Inspirador", "🌟",
        "Sube la moral del equipo.",
        satisfactionModifier = +4),
    INNOVATIVE("Innovador", "💡",
        "+10% en investigación y diseño.",
        productivityModifier = 1.10),
    RELIABLE("Fiable", "📐",
        "Cero ausencias. Reduce burnout.",
        burnoutModifier = -5),
    CHARISMATIC("Carismático", "😎",
        "+5% precio de venta cuando es comercial.",
        satisfactionModifier = +2),
    HOTHEAD("Cabezota", "🔥",
        "Genera fricción, sube burnout.",
        burnoutModifier = +6, satisfactionModifier = -4),
    METHODICAL("Metódico", "🧾",
        "Reduce errores y costes operativos.",
        productivityModifier = 1.05);

    companion object {
        val all: List<EmployeeTrait> = values().toList()
    }
}

/**
 * Nivel formativo del empleado. Sube vía programas de formación o
 * fichajes externos. Influye en salario esperado y techo de ascenso.
 */
@Serializable
enum class Education(
    val displayName: String,
    val emoji: String,
    val rank: Int,
    val baseSalaryMultiplier: Double
) {
    NO_DEGREE("Sin titulación", "📕", 0, 0.85),
    HIGHSCHOOL("Bachillerato", "📗", 1, 1.00),
    BACHELORS("Grado", "📘", 2, 1.15),
    MASTERS("Máster", "📙", 3, 1.30),
    PHD("Doctorado", "📔", 4, 1.55),
    MBA("MBA", "🎓", 5, 1.65);

    fun isAtLeast(other: Education): Boolean = this.rank >= other.rank

    companion object {
        val orderedAscending: List<Education> = values().sortedBy { it.rank }
    }
}

/**
 * Perfil extendido del empleado. Vive en `HrState.profiles[employeeId]`
 * y se complementa con el `Employee` clásico (que NO modificamos).
 *
 *  - level/xp gobiernan la curva de ascenso
 *  - traits son las "skills" cualitativas
 *  - hiredAtTick / lastPromotionTick permiten timers (cooldowns, antigüedad)
 *  - vacationDaysLeft, satisfaction y burnout alimentan la rotación
 */
@Serializable
data class EmployeeProfile(
    val employeeId: String,
    val role: EmployeeRole,
    val level: Int = 1,
    val xp: Long = 0,
    val traits: List<EmployeeTrait> = emptyList(),
    val education: Education = Education.HIGHSCHOOL,
    val hiredAtTick: Long = 0L,
    val lastPromotionTick: Long = 0L,
    val vacationDaysLeft: Int = 18,
    val satisfactionScore: Int = 70,    // 0..100
    val burnoutRisk: Int = 10           // 0..100
) {
    /** XP necesaria para subir al siguiente nivel del rol. */
    fun xpForNextLevel(): Long =
        (450 * Math.pow(1.45, (level - 1).toDouble())).toLong()

    /** Suma de modificadores de rasgos sobre productividad. */
    fun traitProductivity(): Double =
        traits.fold(1.0) { acc, t -> acc * t.productivityModifier }

    /** Suma de modificadores de rasgos sobre salario. */
    fun traitSalary(): Double =
        traits.fold(1.0) { acc, t -> acc * t.salaryModifier }

    /** ¿Está saturado? riesgo alto de baja inminente. */
    val isBurnedOut: Boolean get() = burnoutRisk >= 80
    val isHappy: Boolean get() = satisfactionScore >= 70

    /** Empuja XP y comprueba subidas de nivel. Devuelve perfil modificado. */
    fun addXp(delta: Long): EmployeeProfile {
        if (delta <= 0) return this
        var x = xp + delta
        var lvl = level
        while (x >= (450 * Math.pow(1.45, (lvl - 1).toDouble())).toLong()) {
            x -= (450 * Math.pow(1.45, (lvl - 1).toDouble())).toLong()
            lvl++
        }
        return copy(xp = x, level = lvl)
    }

    /** Devuelve copia con satisfacción y burnout acotados a 0..100. */
    fun withMood(satDelta: Int = 0, burnDelta: Int = 0): EmployeeProfile = copy(
        satisfactionScore = (satisfactionScore + satDelta).coerceIn(0, 100),
        burnoutRisk = (burnoutRisk + burnDelta).coerceIn(0, 100)
    )
}

/**
 * Estado completo del subsistema HR. Se almacena dentro del GameState
 * raíz y se serializa con todo lo demás.
 *
 *  - profiles: ficha rica por empleado (clave = Employee.id)
 *  - training: programas activos y métricas históricas
 *  - executives: asignación al C-suite
 *  - applicants: candidatos generados (renovados a diario)
 */
@Serializable
data class HrState(
    val profiles: Map<String, EmployeeProfile> = emptyMap(),
    val training: TrainingState = TrainingState(),
    val executives: ExecutiveTeam = ExecutiveTeam(),
    val applicants: List<JobApplicant> = emptyList(),
    val lastApplicantRefreshTick: Long = -1L
) {
    fun profile(empId: String): EmployeeProfile? = profiles[empId]
    fun withProfile(p: EmployeeProfile): HrState =
        copy(profiles = profiles + (p.employeeId to p))
    fun withoutProfile(empId: String): HrState =
        copy(profiles = profiles - empId)
}
