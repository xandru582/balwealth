package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Sistema de árbol de habilidades estilo Skyrim/Diablo.
 *
 * El jugador gana 1 punto de habilidad por cada nivel ganado, y +1 extra
 * por cada 5 niveles. Las habilidades se desbloquean por ramas (5 ramas),
 * con tiers ascendentes (1..4) y prerequisitos sobre habilidades del mismo
 * tier o inferior.
 *
 * Las habilidades aplican efectos pasivos que el [com.empiretycoon.game.engine.SkillEngine]
 * agrega y consume el resto del juego (producción, mercado, eventos…).
 */

/** Ramas del árbol de habilidades. */
@Serializable
enum class SkillBranch(val displayName: String, val emoji: String, val tagline: String) {
    LEADERSHIP("Liderazgo",   "👑", "Empleados productivos, salarios bajos."),
    FINANCE   ("Finanzas",    "💰", "Bolsa, cash inicial, intereses."),
    NEGOCIATION("Negociación","🤝", "Vender más caro, comprar más barato."),
    TECHNOLOGY("Tecnología",  "🔬", "I+D rápida, fábricas eficientes."),
    INTUITION ("Intuición",   "🔮", "Suerte, eventos, lotería.")
}

/**
 * Efecto que un nodo de habilidad (o perk) aporta al estado del jugador.
 *
 * Sealed para que cada sumando sea fuertemente tipado. El motor agrega los
 * efectos por clase concreta al consultar bonificaciones (ver [com.empiretycoon.game.engine.SkillEngine.aggregateEffects]).
 */
@Serializable
sealed class SkillEffect {
    @Serializable data class ProductionBonus(val pct: Double) : SkillEffect()
    @Serializable data class MarketSellBonus(val pct: Double) : SkillEffect()
    @Serializable data class MarketBuyDiscount(val pct: Double) : SkillEffect()
    @Serializable data class ResearchSpeedup(val pct: Double) : SkillEffect()
    @Serializable data class EnergyRegen(val perTick: Double) : SkillEffect()
    @Serializable data class HappinessFloor(val floor: Int) : SkillEffect()
    @Serializable data class EmployeeSalaryReduction(val pct: Double) : SkillEffect()
    @Serializable data class RealEstateRent(val pct: Double) : SkillEffect()
    @Serializable data class StockGain(val pct: Double) : SkillEffect()
    @Serializable data class EventLuck(val pct: Double) : SkillEffect()
    @Serializable data class TickXp(val amount: Double) : SkillEffect()
    @Serializable data class CashStartBonus(val cash: Double) : SkillEffect()
    @Serializable data class MaxEnergyBonus(val amount: Int) : SkillEffect()
    @Serializable data class BuildingDiscount(val pct: Double) : SkillEffect()
    @Serializable data class CustomFlag(val name: String) : SkillEffect()

    companion object {
        const val KEY_PRODUCTION = "production"
        const val KEY_MARKET_SELL = "market_sell"
        const val KEY_MARKET_BUY = "market_buy"
        const val KEY_RESEARCH = "research"
        const val KEY_ENERGY_REGEN = "energy_regen"
        const val KEY_HAPPINESS_FLOOR = "happiness_floor"
        const val KEY_SALARY_REDUCTION = "salary_reduction"
        const val KEY_REAL_ESTATE = "real_estate"
        const val KEY_STOCK_GAIN = "stock_gain"
        const val KEY_EVENT_LUCK = "event_luck"
        const val KEY_TICK_XP = "tick_xp"
        const val KEY_CASH_START = "cash_start"
        const val KEY_MAX_ENERGY = "max_energy"
        const val KEY_BUILDING_DISCOUNT = "building_discount"
    }
}

/** Nodo del árbol de habilidades. */
@Serializable
data class Skill(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val branch: SkillBranch,
    val tier: Int,                          // 1..4
    val cost: Int,                          // puntos de habilidad para desbloquear
    val prerequisites: List<String> = emptyList(),
    val effects: List<SkillEffect> = emptyList()
)

/**
 * Estado serializable del árbol del jugador.
 */
@Serializable
data class SkillTreeState(
    val unlockedSkills: Set<String> = emptySet(),
    val availablePoints: Int = 0,
    val totalEarnedPoints: Int = 0
) {
    fun has(id: String) = id in unlockedSkills

    fun canUnlock(skill: Skill): Boolean {
        if (has(skill.id)) return false
        if (availablePoints < skill.cost) return false
        return skill.prerequisites.all { it in unlockedSkills }
    }
}

/**
 * Catálogo del juego con todas las habilidades disponibles.
 *
 * Convenciones:
 *   id = "<branch_short>_<tier>_<short>", ej. "lead_1_motivator"
 *   tier 1 sin prereqs (puerta de entrada de la rama), tier 2..4 encadenados.
 *   coste de puntos: 1 (T1), 2 (T2), 3 (T3), 5 (T4).
 */
object SkillTreeCatalog {

    val all: List<Skill> = listOf(
        // -------------------------------------------------------------- LEADERSHIP
        Skill(
            id = "lead_1_motivator",
            name = "Motivador",
            description = "Tus empleados rinden +5% en producción.",
            emoji = "📣",
            branch = SkillBranch.LEADERSHIP,
            tier = 1, cost = 1,
            effects = listOf(SkillEffect.ProductionBonus(0.05))
        ),
        Skill(
            id = "lead_1_negotiator_pay",
            name = "Salario justo",
            description = "Reduces los salarios mensuales un 4%.",
            emoji = "💸",
            branch = SkillBranch.LEADERSHIP,
            tier = 1, cost = 1,
            effects = listOf(SkillEffect.EmployeeSalaryReduction(0.04))
        ),
        Skill(
            id = "lead_2_team_leader",
            name = "Líder de equipo",
            description = "+8% producción adicional cuando hay 5+ empleados.",
            emoji = "👥",
            branch = SkillBranch.LEADERSHIP,
            tier = 2, cost = 2,
            prerequisites = listOf("lead_1_motivator"),
            effects = listOf(
                SkillEffect.ProductionBonus(0.08),
                SkillEffect.CustomFlag("team_synergy")
            )
        ),
        Skill(
            id = "lead_2_hr_master",
            name = "Maestro de RR.HH.",
            description = "-6% más al pago de salarios y +1 reputación al fichar.",
            emoji = "📝",
            branch = SkillBranch.LEADERSHIP,
            tier = 2, cost = 2,
            prerequisites = listOf("lead_1_negotiator_pay"),
            effects = listOf(
                SkillEffect.EmployeeSalaryReduction(0.06),
                SkillEffect.CustomFlag("hr_master")
            )
        ),
        Skill(
            id = "lead_3_charisma",
            name = "Aura de carisma",
            description = "Reputación pasiva: +1 cada día in-game.",
            emoji = "✨",
            branch = SkillBranch.LEADERSHIP,
            tier = 3, cost = 3,
            prerequisites = listOf("lead_2_team_leader", "lead_2_hr_master"),
            effects = listOf(SkillEffect.CustomFlag("daily_rep"))
        ),
        Skill(
            id = "lead_3_morale_boost",
            name = "Moral alta",
            description = "Felicidad mínima 35. Tu equipo nunca está quemado.",
            emoji = "🔥",
            branch = SkillBranch.LEADERSHIP,
            tier = 3, cost = 3,
            prerequisites = listOf("lead_2_team_leader"),
            effects = listOf(SkillEffect.HappinessFloor(35))
        ),
        Skill(
            id = "lead_4_visionary",
            name = "Visionario",
            description = "+12% producción global. Tu liderazgo es absoluto.",
            emoji = "👏",
            branch = SkillBranch.LEADERSHIP,
            tier = 4, cost = 5,
            prerequisites = listOf("lead_3_charisma", "lead_3_morale_boost"),
            effects = listOf(
                SkillEffect.ProductionBonus(0.12),
                SkillEffect.EmployeeSalaryReduction(0.05)
            )
        ),

        // -------------------------------------------------------------- FINANCE
        Skill(
            id = "fin_1_savings",
            name = "Ahorrador",
            description = "Cash extra al inicio: +1.000 €.",
            emoji = "🏦",
            branch = SkillBranch.FINANCE,
            tier = 1, cost = 1,
            effects = listOf(SkillEffect.CashStartBonus(1_000.0))
        ),
        Skill(
            id = "fin_1_broker",
            name = "Aprendiz de bróker",
            description = "+4% a los rendimientos de bolsa.",
            emoji = "📈",
            branch = SkillBranch.FINANCE,
            tier = 1, cost = 1,
            effects = listOf(SkillEffect.StockGain(0.04))
        ),
        Skill(
            id = "fin_2_compound",
            name = "Interés compuesto",
            description = "+6% adicional a la bolsa.",
            emoji = "💹",
            branch = SkillBranch.FINANCE,
            tier = 2, cost = 2,
            prerequisites = listOf("fin_1_broker"),
            effects = listOf(SkillEffect.StockGain(0.06))
        ),
        Skill(
            id = "fin_2_thrifty",
            name = "Tacaño profesional",
            description = "Edificios -5% más baratos.",
            emoji = "🧾",
            branch = SkillBranch.FINANCE,
            tier = 2, cost = 2,
            prerequisites = listOf("fin_1_savings"),
            effects = listOf(SkillEffect.BuildingDiscount(0.05))
        ),
        Skill(
            id = "fin_3_loan_shark",
            name = "Negociador de bancos",
            description = "Intereses de préstamos -25%.",
            emoji = "🏦",
            branch = SkillBranch.FINANCE,
            tier = 3, cost = 3,
            prerequisites = listOf("fin_2_compound"),
            effects = listOf(SkillEffect.CustomFlag("loan_discount"))
        ),
        Skill(
            id = "fin_3_landlord",
            name = "Casero exigente",
            description = "+10% rentas de inmuebles.",
            emoji = "🏠",
            branch = SkillBranch.FINANCE,
            tier = 3, cost = 3,
            prerequisites = listOf("fin_2_thrifty"),
            effects = listOf(SkillEffect.RealEstateRent(0.10))
        ),
        Skill(
            id = "fin_4_oligarch",
            name = "Oligarca",
            description = "+15% bolsa, +10% rentas, -8% en construcción.",
            emoji = "👑",
            branch = SkillBranch.FINANCE,
            tier = 4, cost = 5,
            prerequisites = listOf("fin_3_loan_shark", "fin_3_landlord"),
            effects = listOf(
                SkillEffect.StockGain(0.15),
                SkillEffect.RealEstateRent(0.10),
                SkillEffect.BuildingDiscount(0.08)
            )
        ),

        // -------------------------------------------------------------- NEGOCIATION
        Skill(
            id = "neg_1_haggle",
            name = "Regateador",
            description = "+3% en venta de mercado.",
            emoji = "💬",
            branch = SkillBranch.NEGOCIATION,
            tier = 1, cost = 1,
            effects = listOf(SkillEffect.MarketSellBonus(0.03))
        ),
        Skill(
            id = "neg_1_bargain",
            name = "Cazador de ofertas",
            description = "-3% al comprar materiales.",
            emoji = "🛒",
            branch = SkillBranch.NEGOCIATION,
            tier = 1, cost = 1,
            effects = listOf(SkillEffect.MarketBuyDiscount(0.03))
        ),
        Skill(
            id = "neg_2_silver_tongue",
            name = "Lengua de plata",
            description = "+5% más al precio de venta.",
            emoji = "🗣",
            branch = SkillBranch.NEGOCIATION,
            tier = 2, cost = 2,
            prerequisites = listOf("neg_1_haggle"),
            effects = listOf(SkillEffect.MarketSellBonus(0.05))
        ),
        Skill(
            id = "neg_2_supply_chain",
            name = "Cadena de suministro",
            description = "-5% más en compras.",
            emoji = "🚚",
            branch = SkillBranch.NEGOCIATION,
            tier = 2, cost = 2,
            prerequisites = listOf("neg_1_bargain"),
            effects = listOf(SkillEffect.MarketBuyDiscount(0.05))
        ),
        Skill(
            id = "neg_3_contract_king",
            name = "Rey de los contratos",
            description = "+8% venta y +1 XP de empresa por cada venta.",
            emoji = "📜",
            branch = SkillBranch.NEGOCIATION,
            tier = 3, cost = 3,
            prerequisites = listOf("neg_2_silver_tongue"),
            effects = listOf(
                SkillEffect.MarketSellBonus(0.08),
                SkillEffect.CustomFlag("contract_king")
            )
        ),
        Skill(
            id = "neg_3_wholesale",
            name = "Compra al por mayor",
            description = "-7% más en compras y +5% capacidad de almacén.",
            emoji = "🏬",
            branch = SkillBranch.NEGOCIATION,
            tier = 3, cost = 3,
            prerequisites = listOf("neg_2_supply_chain"),
            effects = listOf(
                SkillEffect.MarketBuyDiscount(0.07),
                SkillEffect.CustomFlag("warehouse_plus")
            )
        ),
        Skill(
            id = "neg_4_tycoon",
            name = "Magnate del trato",
            description = "+10% venta, -10% compra. El mercado te conoce.",
            emoji = "🎯",
            branch = SkillBranch.NEGOCIATION,
            tier = 4, cost = 5,
            prerequisites = listOf("neg_3_contract_king", "neg_3_wholesale"),
            effects = listOf(
                SkillEffect.MarketSellBonus(0.10),
                SkillEffect.MarketBuyDiscount(0.10)
            )
        ),

        // -------------------------------------------------------------- TECHNOLOGY
        Skill(
            id = "tech_1_research",
            name = "Mente curiosa",
            description = "+8% velocidad de investigación.",
            emoji = "📚",
            branch = SkillBranch.TECHNOLOGY,
            tier = 1, cost = 1,
            effects = listOf(SkillEffect.ResearchSpeedup(0.08))
        ),
        Skill(
            id = "tech_1_factory",
            name = "Ingeniero industrial",
            description = "+4% producción global.",
            emoji = "🏭",
            branch = SkillBranch.TECHNOLOGY,
            tier = 1, cost = 1,
            effects = listOf(SkillEffect.ProductionBonus(0.04))
        ),
        Skill(
            id = "tech_2_lab_rat",
            name = "Rata de laboratorio",
            description = "+12% velocidad I+D.",
            emoji = "🧪",
            branch = SkillBranch.TECHNOLOGY,
            tier = 2, cost = 2,
            prerequisites = listOf("tech_1_research"),
            effects = listOf(SkillEffect.ResearchSpeedup(0.12))
        ),
        Skill(
            id = "tech_2_automation",
            name = "Automatización básica",
            description = "+6% producción y +1 XP por tick.",
            emoji = "🤖",
            branch = SkillBranch.TECHNOLOGY,
            tier = 2, cost = 2,
            prerequisites = listOf("tech_1_factory"),
            effects = listOf(
                SkillEffect.ProductionBonus(0.06),
                SkillEffect.TickXp(1.0)
            )
        ),
        Skill(
            id = "tech_3_innovator",
            name = "Innovador",
            description = "+18% I+D y desbloquea recetas avanzadas.",
            emoji = "💡",
            branch = SkillBranch.TECHNOLOGY,
            tier = 3, cost = 3,
            prerequisites = listOf("tech_2_lab_rat"),
            effects = listOf(
                SkillEffect.ResearchSpeedup(0.18),
                SkillEffect.CustomFlag("advanced_recipes")
            )
        ),
        Skill(
            id = "tech_3_robotics",
            name = "Robótica",
            description = "+10% producción y descuento del 4% al construir.",
            emoji = "🦷",
            branch = SkillBranch.TECHNOLOGY,
            tier = 3, cost = 3,
            prerequisites = listOf("tech_2_automation"),
            effects = listOf(
                SkillEffect.ProductionBonus(0.10),
                SkillEffect.BuildingDiscount(0.04)
            )
        ),
        Skill(
            id = "tech_4_singularity",
            name = "Singularidad",
            description = "+25% I+D y +15% producción. Tu fábrica se gestiona sola.",
            emoji = "🌟",
            branch = SkillBranch.TECHNOLOGY,
            tier = 4, cost = 5,
            prerequisites = listOf("tech_3_innovator", "tech_3_robotics"),
            effects = listOf(
                SkillEffect.ResearchSpeedup(0.25),
                SkillEffect.ProductionBonus(0.15)
            )
        ),

        // -------------------------------------------------------------- INTUITION
        Skill(
            id = "int_1_lucky",
            name = "Pie derecho",
            description = "+5% suerte en eventos aleatorios.",
            emoji = "🍀",
            branch = SkillBranch.INTUITION,
            tier = 1, cost = 1,
            effects = listOf(SkillEffect.EventLuck(0.05))
        ),
        Skill(
            id = "int_1_stamina",
            name = "Resistencia",
            description = "+10 al máximo de energía.",
            emoji = "⚡",
            branch = SkillBranch.INTUITION,
            tier = 1, cost = 1,
            effects = listOf(SkillEffect.MaxEnergyBonus(10))
        ),
        Skill(
            id = "int_2_omen",
            name = "Sexto sentido",
            description = "+10% suerte y +1 XP por tick por intuición.",
            emoji = "🔮",
            branch = SkillBranch.INTUITION,
            tier = 2, cost = 2,
            prerequisites = listOf("int_1_lucky"),
            effects = listOf(
                SkillEffect.EventLuck(0.10),
                SkillEffect.TickXp(1.0)
            )
        ),
        Skill(
            id = "int_2_athletic",
            name = "Atleta",
            description = "+0.05 energía por tick (recupera más rápido).",
            emoji = "🏃",
            branch = SkillBranch.INTUITION,
            tier = 2, cost = 2,
            prerequisites = listOf("int_1_stamina"),
            effects = listOf(SkillEffect.EnergyRegen(0.05))
        ),
        Skill(
            id = "int_3_lottery",
            name = "Tocado por la suerte",
            description = "+15% suerte. La lotería te quiere.",
            emoji = "🎰",
            branch = SkillBranch.INTUITION,
            tier = 3, cost = 3,
            prerequisites = listOf("int_2_omen"),
            effects = listOf(
                SkillEffect.EventLuck(0.15),
                SkillEffect.CustomFlag("lottery_ticket")
            )
        ),
        Skill(
            id = "int_3_zen",
            name = "Zen",
            description = "Felicidad mínima 50, +20 energía máxima.",
            emoji = "🧘",
            branch = SkillBranch.INTUITION,
            tier = 3, cost = 3,
            prerequisites = listOf("int_2_athletic"),
            effects = listOf(
                SkillEffect.HappinessFloor(50),
                SkillEffect.MaxEnergyBonus(20)
            )
        ),
        Skill(
            id = "int_4_oracle",
            name = "Oráculo",
            description = "+25% suerte. Sabes lo que va a pasar.",
            emoji = "🔯",
            branch = SkillBranch.INTUITION,
            tier = 4, cost = 5,
            prerequisites = listOf("int_3_lottery", "int_3_zen"),
            effects = listOf(
                SkillEffect.EventLuck(0.25),
                SkillEffect.CustomFlag("oracle_vision")
            )
        )
    )

    val byId: Map<String, Skill> = all.associateBy { it.id }

    fun byBranch(branch: SkillBranch): List<Skill> =
        all.filter { it.branch == branch }.sortedBy { it.tier }

    fun byTier(branch: SkillBranch, tier: Int): List<Skill> =
        all.filter { it.branch == branch && it.tier == tier }
}
