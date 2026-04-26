package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Roles profesionales que puede ocupar un empleado dentro de la empresa.
 *
 * Cada rol determina:
 *  - su salario base y crecimiento por nivel
 *  - los edificios donde aporta más productividad (specialty)
 *  - bonos pasivos (perks) que se acumulan en el motor de RRHH
 *  - si puede o no escalar a la cúpula (CXO)
 */
@Serializable
enum class EmployeeRole {
    LABORER,
    OPERATOR,
    SUPERVISOR,
    MANAGER,
    DIRECTOR,
    ENGINEER,
    SCIENTIST,
    MARKETER,
    SALESPERSON,
    ACCOUNTANT,
    LAWYER,
    EXECUTIVE_ASSISTANT,
    CXO
}

/**
 * Especialidad sectorial: indica en qué tipo de edificios el rol rinde más.
 * `ANY` significa que aplica a todos por igual (p.ej. dirección general).
 */
@Serializable
enum class BuildingSpecialty {
    ANY,
    RAW_MATERIAL,
    MANUFACTURING,
    SERVICE,
    FINANCIAL,
    RESEARCH;

    /** Mapea un BuildingType del juego a la especialidad sectorial dominante. */
    companion object {
        fun fromBuilding(type: BuildingType): BuildingSpecialty = when (type) {
            BuildingType.FARM,
            BuildingType.SAWMILL,
            BuildingType.MINE -> RAW_MATERIAL
            BuildingType.BAKERY,
            BuildingType.SMELTER,
            BuildingType.REFINERY,
            BuildingType.FACTORY,
            BuildingType.SHIPYARD,
            BuildingType.JEWELRY -> MANUFACTURING
            BuildingType.OFFICE -> SERVICE
            BuildingType.WAREHOUSE -> ANY
        }
    }
}

/**
 * Ficha estática del rol. Es catálogo de reglas; nunca se serializa por sí mismo.
 */
data class RoleProfile(
    val role: EmployeeRole,
    val displayName: String,
    val emoji: String,
    val baseSalary: Double,
    val salaryGrowthPerLevel: Double, // multiplicador acumulado por nivel
    val productivityContribution: Double, // factor de productividad base (1.0 = neutro)
    val perks: List<String>,
    val specialty: BuildingSpecialty,
    val canBecomeExecutive: Boolean = false,
    val unlockLevel: Int = 1
) {
    /** Salario para el nivel dado (mensual), aplicando el crecimiento compuesto. */
    fun salaryAtLevel(level: Int): Double =
        baseSalary * Math.pow(salaryGrowthPerLevel, (level - 1).coerceAtLeast(0).toDouble())

    /** Multiplicador de productividad efectivo según especialidad y edificio. */
    fun specialtyMultiplier(building: BuildingType?): Double {
        if (building == null) return 1.0
        if (specialty == BuildingSpecialty.ANY) return 1.0
        return if (BuildingSpecialty.fromBuilding(building) == specialty) 1.15 else 1.0
    }
}

/**
 * Catálogo central de roles. La UI y el motor consultan aquí en lugar de
 * hardcodear datos. Añadir un rol = añadir entrada y enum.
 */
object RoleCatalog {

    val byRole: Map<EmployeeRole, RoleProfile> = mapOf(
        EmployeeRole.LABORER to RoleProfile(
            role = EmployeeRole.LABORER,
            displayName = "Peón",
            emoji = "👷", // construction worker
            baseSalary = 320.0,
            salaryGrowthPerLevel = 1.10,
            productivityContribution = 1.00,
            perks = listOf("Mano de obra polivalente"),
            specialty = BuildingSpecialty.RAW_MATERIAL
        ),
        EmployeeRole.OPERATOR to RoleProfile(
            role = EmployeeRole.OPERATOR,
            displayName = "Operario",
            emoji = "🔧", // wrench
            baseSalary = 480.0,
            salaryGrowthPerLevel = 1.12,
            productivityContribution = 1.10,
            perks = listOf("+10% velocidad en cadena de producción"),
            specialty = BuildingSpecialty.MANUFACTURING
        ),
        EmployeeRole.SUPERVISOR to RoleProfile(
            role = EmployeeRole.SUPERVISOR,
            displayName = "Supervisor",
            emoji = "👨‍💼", // man office worker
            baseSalary = 780.0,
            salaryGrowthPerLevel = 1.15,
            productivityContribution = 1.05,
            perks = listOf("+5% moral del equipo asignado", "Reduce ausencias"),
            specialty = BuildingSpecialty.ANY,
            unlockLevel = 3
        ),
        EmployeeRole.MANAGER to RoleProfile(
            role = EmployeeRole.MANAGER,
            displayName = "Mánager",
            emoji = "💼", // briefcase
            baseSalary = 1_400.0,
            salaryGrowthPerLevel = 1.18,
            productivityContribution = 1.08,
            perks = listOf("+8% eficiencia plantilla a su cargo"),
            specialty = BuildingSpecialty.ANY,
            unlockLevel = 5
        ),
        EmployeeRole.DIRECTOR to RoleProfile(
            role = EmployeeRole.DIRECTOR,
            displayName = "Director/a",
            emoji = "🎯", // direct hit / target
            baseSalary = 3_200.0,
            salaryGrowthPerLevel = 1.20,
            productivityContribution = 1.12,
            perks = listOf("+12% productividad global de su división"),
            specialty = BuildingSpecialty.ANY,
            canBecomeExecutive = true,
            unlockLevel = 10
        ),
        EmployeeRole.ENGINEER to RoleProfile(
            role = EmployeeRole.ENGINEER,
            displayName = "Ingeniero/a",
            emoji = "👨‍🔧", // man mechanic
            baseSalary = 1_650.0,
            salaryGrowthPerLevel = 1.16,
            productivityContribution = 1.20,
            perks = listOf("+20% en fábricas y refinerías", "Reduce averías"),
            specialty = BuildingSpecialty.MANUFACTURING,
            unlockLevel = 4
        ),
        EmployeeRole.SCIENTIST to RoleProfile(
            role = EmployeeRole.SCIENTIST,
            displayName = "Científico/a",
            emoji = "🧑‍🔬", // scientist
            baseSalary = 2_100.0,
            salaryGrowthPerLevel = 1.18,
            productivityContribution = 1.05,
            perks = listOf("+15% velocidad de I+D", "Desbloquea proyectos"),
            specialty = BuildingSpecialty.RESEARCH,
            unlockLevel = 6
        ),
        EmployeeRole.MARKETER to RoleProfile(
            role = EmployeeRole.MARKETER,
            displayName = "Marketing",
            emoji = "📣", // megaphone
            baseSalary = 1_350.0,
            salaryGrowthPerLevel = 1.15,
            productivityContribution = 1.00,
            perks = listOf("+8% precio de venta en mercado"),
            specialty = BuildingSpecialty.SERVICE
        ),
        EmployeeRole.SALESPERSON to RoleProfile(
            role = EmployeeRole.SALESPERSON,
            displayName = "Comercial",
            emoji = "🤝", // handshake
            baseSalary = 1_100.0,
            salaryGrowthPerLevel = 1.13,
            productivityContribution = 1.00,
            perks = listOf("+6% conversión de oportunidades", "Bonus por trato cerrado"),
            specialty = BuildingSpecialty.SERVICE
        ),
        EmployeeRole.ACCOUNTANT to RoleProfile(
            role = EmployeeRole.ACCOUNTANT,
            displayName = "Contable",
            emoji = "📊", // bar chart
            baseSalary = 1_250.0,
            salaryGrowthPerLevel = 1.14,
            productivityContribution = 1.00,
            perks = listOf("-4% costes operativos", "Reduce errores fiscales"),
            specialty = BuildingSpecialty.FINANCIAL,
            unlockLevel = 3
        ),
        EmployeeRole.LAWYER to RoleProfile(
            role = EmployeeRole.LAWYER,
            displayName = "Abogado/a",
            emoji = "⚖️", // scales
            baseSalary = 2_400.0,
            salaryGrowthPerLevel = 1.18,
            productivityContribution = 1.00,
            perks = listOf("Mitiga eventos negativos", "Reduce sanciones"),
            specialty = BuildingSpecialty.FINANCIAL,
            unlockLevel = 7
        ),
        EmployeeRole.EXECUTIVE_ASSISTANT to RoleProfile(
            role = EmployeeRole.EXECUTIVE_ASSISTANT,
            displayName = "Asistente ejecutivo",
            emoji = "🗂️", // card index dividers
            baseSalary = 1_600.0,
            salaryGrowthPerLevel = 1.17,
            productivityContribution = 1.02,
            perks = listOf("Acelera decisiones de la cúpula", "Camino al CXO"),
            specialty = BuildingSpecialty.ANY,
            canBecomeExecutive = true,
            unlockLevel = 8
        ),
        EmployeeRole.CXO to RoleProfile(
            role = EmployeeRole.CXO,
            displayName = "Ejecutivo C-Suite",
            emoji = "👔", // necktie
            baseSalary = 9_000.0,
            salaryGrowthPerLevel = 1.25,
            productivityContribution = 1.30,
            perks = listOf("Bono global a toda la compañía", "Slot exclusivo"),
            specialty = BuildingSpecialty.ANY,
            canBecomeExecutive = true,
            unlockLevel = 15
        )
    )

    fun get(role: EmployeeRole): RoleProfile = byRole.getValue(role)

    /** Roles a los que un rol concreto puede ascender. */
    fun promotionPath(role: EmployeeRole): List<EmployeeRole> = when (role) {
        EmployeeRole.LABORER -> listOf(EmployeeRole.OPERATOR, EmployeeRole.SUPERVISOR)
        EmployeeRole.OPERATOR -> listOf(EmployeeRole.SUPERVISOR, EmployeeRole.ENGINEER)
        EmployeeRole.SUPERVISOR -> listOf(EmployeeRole.MANAGER)
        EmployeeRole.MANAGER -> listOf(EmployeeRole.DIRECTOR)
        EmployeeRole.DIRECTOR -> listOf(EmployeeRole.CXO)
        EmployeeRole.ENGINEER -> listOf(EmployeeRole.MANAGER, EmployeeRole.DIRECTOR)
        EmployeeRole.SCIENTIST -> listOf(EmployeeRole.DIRECTOR)
        EmployeeRole.MARKETER -> listOf(EmployeeRole.MANAGER)
        EmployeeRole.SALESPERSON -> listOf(EmployeeRole.MANAGER, EmployeeRole.MARKETER)
        EmployeeRole.ACCOUNTANT -> listOf(EmployeeRole.MANAGER, EmployeeRole.DIRECTOR)
        EmployeeRole.LAWYER -> listOf(EmployeeRole.DIRECTOR)
        EmployeeRole.EXECUTIVE_ASSISTANT -> listOf(EmployeeRole.CXO)
        EmployeeRole.CXO -> emptyList()
    }

    /** Lista ordenada para mostrar en pickers. */
    val orderedForPicker: List<EmployeeRole> = listOf(
        EmployeeRole.LABORER, EmployeeRole.OPERATOR, EmployeeRole.SUPERVISOR,
        EmployeeRole.MANAGER, EmployeeRole.DIRECTOR, EmployeeRole.ENGINEER,
        EmployeeRole.SCIENTIST, EmployeeRole.MARKETER, EmployeeRole.SALESPERSON,
        EmployeeRole.ACCOUNTANT, EmployeeRole.LAWYER, EmployeeRole.EXECUTIVE_ASSISTANT,
        EmployeeRole.CXO
    )
}
