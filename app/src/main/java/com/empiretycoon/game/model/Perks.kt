package com.empiretycoon.game.model

import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * Sistema de perks (rasgos aleatorios) que se ofrecen al jugador cada 5 niveles.
 *
 * Cada subida de nivel múltiplo de 5 ofrece 3 cartas aleatorias ponderadas por
 * rareza. El jugador elige UNA, las otras se descartan.
 */

@Serializable
enum class Rarity(val displayName: String, val emoji: String, val weight: Int) {
    COMMON   ("Común",     "⚪", 60),
    UNCOMMON ("Inusual",   "🟢", 25),
    RARE     ("Raro",      "🔵", 10),
    EPIC     ("Épico",     "🟣", 4),
    LEGENDARY("Legendario","🟡", 1)
}

@Serializable
data class Perk(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val rarity: Rarity,
    val effects: List<SkillEffect> = emptyList()
)

@Serializable
data class PerksState(
    val ownedPerks: List<String> = emptyList(),
    val pendingChoice: List<String>? = null     // ids ofrecidos esperando elección
) {
    val hasPending: Boolean get() = !pendingChoice.isNullOrEmpty()
}

/**
 * 40+ perks con sabor narrativo y efectos variados.
 * Los ids son únicos y la lista por rareza permite ponderar las tiradas.
 */
object PerkCatalog {

    val all: List<Perk> = listOf(
        // -------------------------------------------------- COMUNES (15)
        Perk("p_lucky_start",      "Suerte del principiante",
            "+5% en eventos aleatorios.",                 "🎲", Rarity.COMMON,
            listOf(SkillEffect.EventLuck(0.05))),
        Perk("p_haggler",          "Olfato comercial",
            "+3% en venta de mercado.",                  "👃", Rarity.COMMON,
            listOf(SkillEffect.MarketSellBonus(0.03))),
        Perk("p_thrifty",          "Manos cerradas",
            "-3% al comprar materiales.",                "🤏", Rarity.COMMON,
            listOf(SkillEffect.MarketBuyDiscount(0.03))),
        Perk("p_hardworker",       "Trabajador",
            "+3% producción.",                           "🔨", Rarity.COMMON,
            listOf(SkillEffect.ProductionBonus(0.03))),
        Perk("p_rest_well",        "Descanso reparador",
            "+0.02 energía por tick.",                   "💤", Rarity.COMMON,
            listOf(SkillEffect.EnergyRegen(0.02))),
        Perk("p_book_lover",       "Amante de los libros",
            "+5% velocidad I+D.",                        "📖", Rarity.COMMON,
            listOf(SkillEffect.ResearchSpeedup(0.05))),
        Perk("p_savings_jar",      "Bote de ahorros",
            "+500 € de cash inicial.",                   "🏺", Rarity.COMMON,
            listOf(SkillEffect.CashStartBonus(500.0))),
        Perk("p_landlord_amateur", "Casero aficionado",
            "+3% rentas inmobiliarias.",                 "🔑", Rarity.COMMON,
            listOf(SkillEffect.RealEstateRent(0.03))),
        Perk("p_optimist",         "Optimista",
            "Felicidad mínima 25.",                      "😊", Rarity.COMMON,
            listOf(SkillEffect.HappinessFloor(25))),
        Perk("p_bookkeeper",       "Contable diligente",
            "+1 XP por tick.",                           "🧮", Rarity.COMMON,
            listOf(SkillEffect.TickXp(1.0))),
        Perk("p_morning_person",   "Madrugador",
            "+5 energía máxima.",                        "🌅", Rarity.COMMON,
            listOf(SkillEffect.MaxEnergyBonus(5))),
        Perk("p_cheap_bricks",     "Ladrillos baratos",
            "-2% al construir edificios.",               "🧱", Rarity.COMMON,
            listOf(SkillEffect.BuildingDiscount(0.02))),
        Perk("p_team_spirit",      "Espíritu de equipo",
            "-2% en salarios.",                          "🏃‍♂️", Rarity.COMMON,
            listOf(SkillEffect.EmployeeSalaryReduction(0.02))),
        Perk("p_market_eye",       "Ojo de mercado",
            "+3% bolsa.",                                "👁", Rarity.COMMON,
            listOf(SkillEffect.StockGain(0.03))),
        Perk("p_weather_eye",      "Predice el tiempo",
            "+4% suerte.",                               "🌦", Rarity.COMMON,
            listOf(SkillEffect.EventLuck(0.04))),

        // -------------------------------------------------- INUSUALES (12)
        Perk("p_market_savant",    "Genio del mercado",
            "+5% venta y +5% bolsa.",                    "📊", Rarity.UNCOMMON,
            listOf(SkillEffect.MarketSellBonus(0.05), SkillEffect.StockGain(0.05))),
        Perk("p_efficient_lab",    "Laboratorio eficiente",
            "+10% I+D.",                                 "⚗️", Rarity.UNCOMMON,
            listOf(SkillEffect.ResearchSpeedup(0.10))),
        Perk("p_bigger_warehouse", "Almacén ampliado",
            "Más espacio para guardar.",                 "📦", Rarity.UNCOMMON,
            listOf(SkillEffect.CustomFlag("warehouse_plus"))),
        Perk("p_polymath",         "Polímata",
            "+5% I+D, +5% producción.",                  "🎓", Rarity.UNCOMMON,
            listOf(SkillEffect.ResearchSpeedup(0.05), SkillEffect.ProductionBonus(0.05))),
        Perk("p_fan_club",         "Club de fans",
            "+1 reputación al día.",                     "📣", Rarity.UNCOMMON,
            listOf(SkillEffect.CustomFlag("daily_rep"))),
        Perk("p_bargain_hunter",   "Cazaofertas",
            "-6% al comprar.",                           "🏷", Rarity.UNCOMMON,
            listOf(SkillEffect.MarketBuyDiscount(0.06))),
        Perk("p_morale_officer",   "Oficial de moral",
            "Felicidad mínima 40.",                      "🎺", Rarity.UNCOMMON,
            listOf(SkillEffect.HappinessFloor(40))),
        Perk("p_iron_will",        "Voluntad de hierro",
            "+15 energía máxima.",                       "🛡", Rarity.UNCOMMON,
            listOf(SkillEffect.MaxEnergyBonus(15))),
        Perk("p_landlord_pro",     "Casero profesional",
            "+8% rentas.",                               "🏘", Rarity.UNCOMMON,
            listOf(SkillEffect.RealEstateRent(0.08))),
        Perk("p_efficient_pay",    "Nóminas eficientes",
            "-5% en salarios.",                          "💵", Rarity.UNCOMMON,
            listOf(SkillEffect.EmployeeSalaryReduction(0.05))),
        Perk("p_factory_blueprint","Planos de fábrica",
            "+8% producción.",                           "📐", Rarity.UNCOMMON,
            listOf(SkillEffect.ProductionBonus(0.08))),
        Perk("p_smooth_talker",    "Buen orador",
            "+8% en venta.",                             "🎙", Rarity.UNCOMMON,
            listOf(SkillEffect.MarketSellBonus(0.08))),

        // -------------------------------------------------- RAROS (8)
        Perk("p_silver_spoon",     "Cuchara de plata",
            "+5.000 € iniciales y +5% bolsa.",           "🥄", Rarity.RARE,
            listOf(SkillEffect.CashStartBonus(5_000.0), SkillEffect.StockGain(0.05))),
        Perk("p_brilliant",        "Brillante",
            "+15% I+D y +1 XP por tick.",                "💎", Rarity.RARE,
            listOf(SkillEffect.ResearchSpeedup(0.15), SkillEffect.TickXp(1.0))),
        Perk("p_iron_negotiator",  "Negociador de acero",
            "+10% venta, -5% compra.",                   "⚙️", Rarity.RARE,
            listOf(SkillEffect.MarketSellBonus(0.10), SkillEffect.MarketBuyDiscount(0.05))),
        Perk("p_industrial_titan", "Titán industrial",
            "+12% producción.",                          "🏗", Rarity.RARE,
            listOf(SkillEffect.ProductionBonus(0.12))),
        Perk("p_workaholic",       "Adicto al trabajo",
            "+0.1 energía por tick y +20 max.",          "☕", Rarity.RARE,
            listOf(SkillEffect.EnergyRegen(0.10), SkillEffect.MaxEnergyBonus(20))),
        Perk("p_dynasty",          "Dinastía",
            "+10% rentas y +10% bolsa.",                 "👑", Rarity.RARE,
            listOf(SkillEffect.RealEstateRent(0.10), SkillEffect.StockGain(0.10))),
        Perk("p_seer",             "Vidente",
            "+15% suerte en eventos.",                   "🪄", Rarity.RARE,
            listOf(SkillEffect.EventLuck(0.15))),
        Perk("p_unionbreaker",     "Negociador laboral",
            "-12% en salarios.",                         "🤐", Rarity.RARE,
            listOf(SkillEffect.EmployeeSalaryReduction(0.12))),

        // -------------------------------------------------- ÉPICOS (4)
        Perk("p_midas",            "Toque de Midas",
            "+20% bolsa y +10% rentas.",                 "🪙", Rarity.EPIC,
            listOf(SkillEffect.StockGain(0.20), SkillEffect.RealEstateRent(0.10))),
        Perk("p_hyperdrive",       "Sobremarcha",
            "+20% producción y +15% I+D.",               "🚀", Rarity.EPIC,
            listOf(SkillEffect.ProductionBonus(0.20), SkillEffect.ResearchSpeedup(0.15))),
        Perk("p_grandmaster_seller","Vendedor maestro",
            "+18% venta y -10% compra.",                 "🎩", Rarity.EPIC,
            listOf(SkillEffect.MarketSellBonus(0.18), SkillEffect.MarketBuyDiscount(0.10))),
        Perk("p_lucky_star",       "Estrella de la suerte",
            "+25% suerte y +10% bolsa.",                 "🌠", Rarity.EPIC,
            listOf(SkillEffect.EventLuck(0.25), SkillEffect.StockGain(0.10))),

        // -------------------------------------------------- LEGENDARIOS (3)
        Perk("p_legend_industrialist","Industrial legendario",
            "+30% producción, +10% I+D.",                "🏆", Rarity.LEGENDARY,
            listOf(SkillEffect.ProductionBonus(0.30), SkillEffect.ResearchSpeedup(0.10))),
        Perk("p_legend_oligarch",  "Oligarca eterno",
            "+30% bolsa, +20% rentas.",                  "🏛", Rarity.LEGENDARY,
            listOf(SkillEffect.StockGain(0.30), SkillEffect.RealEstateRent(0.20))),
        Perk("p_legend_charismatic","Carisma absoluto",
            "Felicidad min 60, -15% salarios, +1 rep/d.","🌹", Rarity.LEGENDARY,
            listOf(SkillEffect.HappinessFloor(60), SkillEffect.EmployeeSalaryReduction(0.15),
                SkillEffect.CustomFlag("daily_rep")))
    )

    val byId: Map<String, Perk> = all.associateBy { it.id }
    val byRarity: Map<Rarity, List<Perk>> = all.groupBy { it.rarity }

    /** Sortea 3 ids únicos con tirada ponderada por rareza. */
    fun rollChoices(rng: Random, count: Int = 3): List<String> {
        val rolled = mutableListOf<Perk>()
        val pool = all.toMutableList()
        repeat(count) {
            if (pool.isEmpty()) return@repeat
            val totalWeight = pool.sumOf { it.rarity.weight }
            val r = rng.nextInt(totalWeight)
            var acc = 0
            val chosen = pool.first { p ->
                acc += p.rarity.weight
                r < acc
            }
            rolled += chosen
            pool -= chosen
        }
        return rolled.map { it.id }
    }
}
