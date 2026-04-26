package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Boost que un programa otorga al empleado tras completarlo.
 * `skillDelta` se aplica al `Employee.skill` (model existente, no se modifica).
 * `xpDelta` cae en `EmployeeProfile.xp` y puede provocar subida de nivel.
 */
data class StatBoost(
    val skillDelta: Double,
    val xpDelta: Long,
    val satisfactionDelta: Int = 5,
    val burnoutDelta: Int = -3
)

/**
 * Definición estática de un programa de formación. Reside en TrainingCatalog.
 * `role` opcional indica que sólo aplica a un rol concreto (mentoring, MBA…).
 */
data class TrainingProgram(
    val id: String,
    val name: String,
    val emoji: String,
    val role: EmployeeRole? = null,           // null = abierto a cualquier rol
    val durationDays: Int,                    // días in-game (1.440 ticks/día)
    val costPerEmployee: Double,
    val statBoost: StatBoost,
    val descripcion: String,
    val minimumEducation: Education = Education.NO_DEGREE,
    val minimumLevel: Int = 1,
    val grantsEducation: Education? = null    // si != null, eleva educación si procede
)

/**
 * Snapshot de una formación en curso. Se guarda en HrState.training.active.
 */
@Serializable
data class ActiveTraining(
    val programId: String,
    val employeeIds: List<String>,
    val startedAtTick: Long,
    val endsAtTick: Long
) {
    fun progress(currentTick: Long): Float {
        val total = (endsAtTick - startedAtTick).coerceAtLeast(1L).toFloat()
        val done = (currentTick - startedAtTick).coerceIn(0L, endsAtTick - startedAtTick).toFloat()
        return (done / total).coerceIn(0f, 1f)
    }
}

/**
 * Estado de formación. `history` = nº de programas completados.
 * Se mantiene compacto: la lista activa caduca al cerrar el programa.
 */
@Serializable
data class TrainingState(
    val active: List<ActiveTraining> = emptyList(),
    val history: Int = 0
)

/**
 * Catálogo de programas. >=12 entradas requeridas.
 */
object TrainingCatalog {

    val all: List<TrainingProgram> = listOf(
        TrainingProgram(
            id = "induction",
            name = "Onboarding",
            emoji = "🎒",
            role = null,
            durationDays = 2,
            costPerEmployee = 250.0,
            statBoost = StatBoost(skillDelta = 0.05, xpDelta = 80),
            descripcion = "Curso de bienvenida e integración cultural."
        ),
        TrainingProgram(
            id = "sales_basics",
            name = "Ventas 101",
            emoji = "🛒",
            role = EmployeeRole.SALESPERSON,
            durationDays = 4,
            costPerEmployee = 600.0,
            statBoost = StatBoost(skillDelta = 0.10, xpDelta = 200),
            descripcion = "Técnicas de venta y manejo de objeciones."
        ),
        TrainingProgram(
            id = "leadership",
            name = "Liderazgo",
            emoji = "🧭",
            role = null,
            durationDays = 6,
            costPerEmployee = 1_400.0,
            statBoost = StatBoost(skillDelta = 0.12, xpDelta = 320, satisfactionDelta = 8),
            descripcion = "Liderazgo situacional y feedback efectivo.",
            minimumLevel = 3
        ),
        TrainingProgram(
            id = "mba",
            name = "MBA Ejecutivo",
            emoji = "🎓",
            role = null,
            durationDays = 30,
            costPerEmployee = 14_000.0,
            statBoost = StatBoost(skillDelta = 0.30, xpDelta = 1_400, satisfactionDelta = 10),
            descripcion = "Programa intensivo de gestión empresarial.",
            minimumEducation = Education.BACHELORS,
            minimumLevel = 5,
            grantsEducation = Education.MBA
        ),
        TrainingProgram(
            id = "tech_advanced",
            name = "Técnico avanzado",
            emoji = "🛠️",
            role = EmployeeRole.ENGINEER,
            durationDays = 8,
            costPerEmployee = 1_900.0,
            statBoost = StatBoost(skillDelta = 0.18, xpDelta = 420),
            descripcion = "Mantenimiento predictivo y mejora continua."
        ),
        TrainingProgram(
            id = "mindfulness",
            name = "Mindfulness corporativo",
            emoji = "🧘",
            role = null,
            durationDays = 3,
            costPerEmployee = 320.0,
            statBoost = StatBoost(skillDelta = 0.02, xpDelta = 60,
                satisfactionDelta = 14, burnoutDelta = -18),
            descripcion = "Reduce el burnout y mejora la retención."
        ),
        TrainingProgram(
            id = "marketing",
            name = "Marketing digital",
            emoji = "📱",
            role = EmployeeRole.MARKETER,
            durationDays = 5,
            costPerEmployee = 800.0,
            statBoost = StatBoost(skillDelta = 0.10, xpDelta = 230),
            descripcion = "SEO, paid media y CRM avanzado."
        ),
        TrainingProgram(
            id = "research_lab",
            name = "Laboratorio I+D",
            emoji = "🔬",
            role = EmployeeRole.SCIENTIST,
            durationDays = 10,
            costPerEmployee = 2_400.0,
            statBoost = StatBoost(skillDelta = 0.20, xpDelta = 520),
            descripcion = "Metodología científica y patentes."
        ),
        TrainingProgram(
            id = "compliance",
            name = "Compliance & Legal",
            emoji = "📜",
            role = EmployeeRole.LAWYER,
            durationDays = 4,
            costPerEmployee = 950.0,
            statBoost = StatBoost(skillDelta = 0.10, xpDelta = 240),
            descripcion = "Normativa, auditoría y prevención de sanciones."
        ),
        TrainingProgram(
            id = "finance_pro",
            name = "Finanzas avanzadas",
            emoji = "📈",
            role = EmployeeRole.ACCOUNTANT,
            durationDays = 5,
            costPerEmployee = 1_050.0,
            statBoost = StatBoost(skillDelta = 0.12, xpDelta = 280),
            descripcion = "Análisis, fiscalidad internacional y reporting."
        ),
        TrainingProgram(
            id = "soft_skills",
            name = "Soft skills pro",
            emoji = "🗣️",
            role = null,
            durationDays = 3,
            costPerEmployee = 380.0,
            statBoost = StatBoost(skillDelta = 0.04, xpDelta = 90, satisfactionDelta = 6),
            descripcion = "Comunicación, negociación y trabajo en equipo."
        ),
        TrainingProgram(
            id = "cxo_track",
            name = "Pista C-Suite",
            emoji = "👔",
            role = null,
            durationDays = 21,
            costPerEmployee = 8_500.0,
            statBoost = StatBoost(skillDelta = 0.25, xpDelta = 950, satisfactionDelta = 10),
            descripcion = "Itinerario para futuros directivos.",
            minimumEducation = Education.MASTERS,
            minimumLevel = 8
        ),
        TrainingProgram(
            id = "diversity",
            name = "Diversidad e inclusión",
            emoji = "🌍",
            role = null,
            durationDays = 2,
            costPerEmployee = 220.0,
            statBoost = StatBoost(skillDelta = 0.02, xpDelta = 70, satisfactionDelta = 7),
            descripcion = "Cultura inclusiva y prevención del acoso."
        ),
        TrainingProgram(
            id = "lean_six_sigma",
            name = "Lean Six Sigma",
            emoji = "🟢",
            role = null,
            durationDays = 12,
            costPerEmployee = 3_200.0,
            statBoost = StatBoost(skillDelta = 0.18, xpDelta = 600),
            descripcion = "Optimización de procesos y reducción de variabilidad.",
            minimumLevel = 4
        )
    )

    fun byId(id: String): TrainingProgram? = all.firstOrNull { it.id == id }

    /** Programas a los que un empleado dado podría apuntarse. */
    fun availableFor(profile: EmployeeProfile): List<TrainingProgram> = all.filter { p ->
        (p.role == null || p.role == profile.role) &&
            profile.education.rank >= p.minimumEducation.rank &&
            profile.level >= p.minimumLevel
    }
}
