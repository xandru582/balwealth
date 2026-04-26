package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Gerentes que automatizan tareas repetitivas del juego.
 *
 * Cada gerente actúa cada N ticks (configurable según nivel) y aplica
 * decisiones acotadas por su `config`. El jugador puede desactivar sin
 * despedir, ajustar umbrales y mejorar al gerente subiendo su nivel.
 */
enum class ManagerType(
    val displayName: String,
    val emoji: String,
    val description: String,
    val baseSalary: Double,
    val hireBonus: Double
) {
    OPERATIONS(
        "Director de Operaciones",
        "👨‍🔧",
        "Mejora edificios automáticamente cuando hay caja suficiente.",
        baseSalary = 2_500.0,
        hireBonus = 5_000.0
    ),
    SALES(
        "Director Comercial",
        "🧑‍💼",
        "Vende producto en el mercado cuando el precio es favorable.",
        baseSalary = 2_200.0,
        hireBonus = 4_500.0
    ),
    PURCHASING(
        "Director de Compras",
        "📦",
        "Compra materias primas cuando hacen falta y el precio es bueno.",
        baseSalary = 2_200.0,
        hireBonus = 4_500.0
    ),
    HR(
        "Director de RRHH",
        "👥",
        "Contrata, asigna trabajadores libres y despide a poco leales.",
        baseSalary = 1_800.0,
        hireBonus = 3_500.0
    ),
    FINANCE(
        "Director Financiero",
        "💸",
        "Paga préstamos antes de tiempo y mantiene un colchón de seguridad.",
        baseSalary = 2_800.0,
        hireBonus = 6_000.0
    );

    /** Coste de contratación (prima de fichaje). */
    fun signingCost(level: Int = 1): Double = hireBonus * (1 + (level - 1) * 0.5)

    /** Salario mensual escalado por nivel. */
    fun salaryAtLevel(level: Int): Double = baseSalary * (1 + (level - 1) * 0.45)
}

/**
 * Configuración por gerente (umbrales del jugador). Todos los valores
 * tienen un default razonable: el gerente "funciona out of the box".
 */
@Serializable
data class ManagerConfig(
    /** Buffer mínimo de caja a respetar al gastar (compras, mejoras, fichajes). */
    val cashReserve: Double = 5_000.0,
    /** Para Sales: factor de precio mínimo del mercado para vender (1.0 = normal). */
    val sellAtFactor: Double = 1.0,
    /** Para Purchasing: factor de precio máximo para comprar (1.0 = normal). */
    val buyBelowFactor: Double = 1.0,
    /** Para Operations: nivel máximo al que el gerente sube cada edificio. */
    val maxBuildingLevel: Int = 5,
    /** Para Sales/Purchasing: cantidad a mantener mínimo en inventario por recurso. */
    val keepStock: Int = 20,
    /** Para HR: si despide a empleados con lealtad <= X. */
    val autoFireBelowLoyalty: Double = 0.3,
    /** Para HR: contratar candidatos si faltan trabajadores. */
    val autoHireWhenUnderstaffed: Boolean = true,
    /** Para Finance: si la caja excede X veces la reserva, repagar préstamo. */
    val repayLoanAboveCashRatio: Double = 3.0,

    /**
     * Para Sales: lista blanca de IDs de recurso que el gerente PUEDE vender.
     * Si está vacía, vende sólo categorías GOOD/SERVICE/LUXURY (productos
     * finales) — nunca materias primas, comida intermedia o componentes.
     * Por defecto: vacía → política inteligente.
     */
    val sellWhitelist: Set<String> = emptySet(),
    /**
     * Para Sales: si true, NUNCA vende recursos que sean inputs de las
     * recetas activas en tu fábrica (te queda sin material para producir).
     */
    val protectActiveRecipeInputs: Boolean = true,
    /**
     * Para HR: si true, refresca el pool de candidatos cuando hay huecos
     * pero el pool actual está vacío.
     */
    val autoRefreshPoolWhenEmpty: Boolean = true
)

/**
 * Instancia de un gerente. `hired = true` solo si el jugador lo fichó.
 * `enabled = true` significa que el gerente actúa; el jugador puede pausarlo
 * sin despedirlo (sigue cobrando salario).
 */
@Serializable
data class Manager(
    val id: String,
    val type: ManagerType,
    val name: String,
    val level: Int = 1,
    val efficiency: Double = 1.0,        // 0.7-1.5 según nivel y rng
    val hired: Boolean = false,
    val enabled: Boolean = true,
    val hiredAtTick: Long = 0L,
    val config: ManagerConfig = ManagerConfig(),
    /** Tick del último ciclo de actuación (para cooldown). */
    val lastActionTick: Long = 0L,
    /** Estadística: nº de acciones automatizadas que ha realizado. */
    val actionsTaken: Int = 0
) {
    val displayName get() = "${type.displayName} — $name"
    val monthlySalary get() = type.salaryAtLevel(level) * efficiency
    val signingCost get() = type.signingCost(level)
    val portrait get() = type.emoji

    /** Cooldown entre acciones (segundos), reducido con nivel y eficiencia. */
    val cooldownSeconds: Int
        get() = (60.0 / (1.0 + (level - 1) * 0.25) / efficiency).toInt().coerceAtLeast(10)
}

/**
 * Estado completo del subsistema de gerentes.
 * `pool`: candidatos disponibles para contratación (refresca diariamente).
 * `hired`: gerentes contratados activamente.
 */
@Serializable
data class ManagersState(
    val pool: List<Manager> = emptyList(),
    val hired: List<Manager> = emptyList(),
    /** Slot máximo de gerentes contratados — 3 base, ampliable con prestige. */
    val maxSlots: Int = 3
) {
    fun has(type: ManagerType): Boolean = hired.any { it.type == type }
    fun byType(type: ManagerType): Manager? = hired.find { it.type == type }
    val availableSlots: Int get() = (maxSlots - hired.size).coerceAtLeast(0)
}

/** Generador de candidatos. */
object ManagerFactory {
    private val firstNames = listOf(
        "Lucía","Andrea","Marcos","Inés","Fabio","Helena","Bruno","Carla",
        "Hugo","Nuria","Iván","Paula","Diego","Raquel","Adrián","Berta",
        "Cristian","Eva","Manu","Olivia","Pablo","Sofía","Tomás","Vega"
    )
    private val lastNames = listOf(
        "Mendoza","Ortiz","Bravo","Núñez","Salinas","Aguirre","Castaño",
        "Jiménez","Quintana","Vega","Caballero","Lozano","Gallardo","Pacheco",
        "Olmedo","Heredia","Moncada","Salazar","Toledano","Vives"
    )

    fun freshPool(rng: kotlin.random.Random, tier: Int = 1): List<Manager> {
        return ManagerType.values().map { type ->
            val level = (1 + tier / 2).coerceAtLeast(1).coerceAtMost(3)
            val effJitter = rng.nextDouble(0.85, 1.20)
            Manager(
                id = "mgr_${System.nanoTime()}_${type.name}",
                type = type,
                name = "${firstNames.random(rng)} ${lastNames.random(rng)}",
                level = level,
                efficiency = ((effJitter * (1 + (level - 1) * 0.10)) * 100).toInt() / 100.0
            )
        }
    }
}
