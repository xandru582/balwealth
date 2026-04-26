package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Categorías de logros para filtrar y agrupar en la UI.
 */
enum class AchievementCategory(val displayName: String, val emoji: String) {
    WEALTH("Riqueza", "💰"),
    PRODUCTION("Producción", "🏭"),
    EMPIRE("Imperio", "🏢"),
    CHARACTER("Personaje", "🧑"),
    MARKET("Mercado", "📈"),
    REAL_ESTATE("Inmuebles", "🏠"),
    RESEARCH("Ciencia", "🔬"),
    SOCIAL("Social", "🌟"),
    MILESTONE("Hitos", "🏆"),
    SECRET("Secreto", "❓")
}

/**
 * Definición estática de un logro. `threshold` es el valor que debe alcanzar
 * el progreso para desbloquearlo. `hidden` oculta título/descripción hasta que
 * se cumple. Las recompensas se cobran al reclamar desde la UI.
 */
@Serializable
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val category: AchievementCategory,
    val threshold: Long,
    val rewardXp: Long = 0,
    val rewardCash: Double = 0.0,
    val hidden: Boolean = false
)

/**
 * Estado serializable del sistema de logros: qué se ha desbloqueado, qué se
 * ha reclamado y cuánto progreso lleva cada uno (para mostrar barras).
 */
@Serializable
data class AchievementsState(
    val unlocked: Set<String> = emptySet(),
    val claimedAchievements: Set<String> = emptySet(),
    val progressMap: Map<String, Long> = emptyMap()
) {
    fun isUnlocked(id: String) = unlocked.contains(id)
    fun isClaimed(id: String) = claimedAchievements.contains(id)
    fun progressOf(id: String): Long = progressMap[id] ?: 0L
}

/**
 * Catálogo estático con 50+ logros repartidos por categorías. Los ids
 * son estables y se usan también en `AchievementEngine` para evaluar progreso.
 */
object AchievementCatalog {
    val all: List<Achievement> = listOf(
        // ===== WEALTH (10) =====
        Achievement("ach_cash_1k",
            "Hucha llena", "Acumula 1.000 en caja.",
            "💵", AchievementCategory.WEALTH,
            threshold = 1_000, rewardXp = 50, rewardCash = 100.0),

        Achievement("ach_cash_10k",
            "Pequeño ahorro", "Acumula 10.000 en caja.",
            "💰", AchievementCategory.WEALTH,
            threshold = 10_000, rewardXp = 150, rewardCash = 500.0),

        Achievement("ach_cash_100k",
            "Seis cifras", "Acumula 100.000 en caja.",
            "💳", AchievementCategory.WEALTH,
            threshold = 100_000, rewardXp = 400, rewardCash = 2_500.0),

        Achievement("ach_cash_1m",
            "Primer millón", "Acumula 1.000.000 en caja.",
            "🏅", AchievementCategory.WEALTH,
            threshold = 1_000_000, rewardXp = 1_500, rewardCash = 25_000.0),

        Achievement("ach_cash_10m",
            "Diez millones", "Acumula 10.000.000 en caja.",
            "🥇", AchievementCategory.WEALTH,
            threshold = 10_000_000, rewardXp = 5_000, rewardCash = 200_000.0),

        Achievement("ach_cash_100m",
            "Cien millones", "Acumula 100.000.000 en caja.",
            "👑", AchievementCategory.WEALTH,
            threshold = 100_000_000, rewardXp = 15_000, rewardCash = 1_500_000.0),

        Achievement("ach_cash_1b",
            "Mil millones", "Acumula 1.000.000.000 en caja. Eres leyenda.",
            "💎", AchievementCategory.WEALTH,
            threshold = 1_000_000_000, rewardXp = 50_000, rewardCash = 10_000_000.0),

        Achievement("ach_personal_50k",
            "Bolsillos llenos", "Acumula 50.000 en cash personal.",
            "💷", AchievementCategory.WEALTH,
            threshold = 50_000, rewardXp = 400, rewardCash = 2_000.0),

        Achievement("ach_loan_repaid",
            "Sin deudas", "Reduce tu deuda bancaria a cero teniéndola.",
            "🧾", AchievementCategory.WEALTH,
            threshold = 1, rewardXp = 600, rewardCash = 5_000.0),

        Achievement("ach_total_assets",
            "Patrimonio total", "Suma 5.000.000 entre caja, inmuebles y bolsa.",
            "🏦", AchievementCategory.WEALTH,
            threshold = 5_000_000, rewardXp = 2_500, rewardCash = 100_000.0),

        // ===== PRODUCTION (8) =====
        Achievement("ach_first_recipe",
            "Cinta en marcha", "Asigna tu primera receta a un edificio.",
            "⚙️", AchievementCategory.PRODUCTION,
            threshold = 1, rewardXp = 80, rewardCash = 300.0),

        Achievement("ach_multitarea",
            "Multitarea", "Ten 10 edificios produciendo al mismo tiempo.",
            "🔄", AchievementCategory.PRODUCTION,
            threshold = 10, rewardXp = 1_200, rewardCash = 20_000.0),

        Achievement("ach_specialist_100",
            "Especialista", "Posee 100 unidades del mismo recurso a la vez.",
            "📦", AchievementCategory.PRODUCTION,
            threshold = 100, rewardXp = 300, rewardCash = 1_500.0),

        Achievement("ach_specialist_1000",
            "Acaparador", "Posee 1.000 unidades del mismo recurso a la vez.",
            "🏬", AchievementCategory.PRODUCTION,
            threshold = 1_000, rewardXp = 1_800, rewardCash = 30_000.0),

        Achievement("ach_inventory_full",
            "Almacén lleno", "Llena por completo el almacén.",
            "📥", AchievementCategory.PRODUCTION,
            threshold = 1, rewardXp = 250),

        Achievement("ach_diversify_5",
            "Pequeño catálogo", "Ten 5 recursos diferentes en stock.",
            "🌐", AchievementCategory.PRODUCTION,
            threshold = 5, rewardXp = 200, rewardCash = 1_000.0),

        Achievement("ach_diversify_15",
            "Variedad industrial", "Ten 15 recursos diferentes en stock.",
            "🎯", AchievementCategory.PRODUCTION,
            threshold = 15, rewardXp = 900, rewardCash = 8_000.0),

        Achievement("ach_diversify_25",
            "Conglomerado total", "Ten 25 recursos diferentes en stock.",
            "🗺️", AchievementCategory.PRODUCTION,
            threshold = 25, rewardXp = 3_500, rewardCash = 60_000.0),

        // ===== EMPIRE (8) =====
        Achievement("ach_first_building",
            "Primer ladrillo", "Construye tu primer edificio.",
            "🏗️", AchievementCategory.EMPIRE,
            threshold = 1, rewardXp = 100, rewardCash = 500.0),

        Achievement("ach_5_buildings",
            "Pequeño imperio", "Construye 5 edificios.",
            "🏘️", AchievementCategory.EMPIRE,
            threshold = 5, rewardXp = 300, rewardCash = 2_500.0),

        Achievement("ach_10_buildings",
            "Imperio en marcha", "Construye 10 edificios.",
            "🏬", AchievementCategory.EMPIRE,
            threshold = 10, rewardXp = 800, rewardCash = 10_000.0),

        Achievement("ach_20_buildings",
            "Imperio Industrial", "Construye 20 edificios.",
            "🏭", AchievementCategory.EMPIRE,
            threshold = 20, rewardXp = 2_500, rewardCash = 75_000.0),

        Achievement("ach_one_each",
            "Conglomerado", "Posee al menos 1 edificio de cada tipo.",
            "🏯", AchievementCategory.EMPIRE,
            threshold = 11, rewardXp = 4_000, rewardCash = 200_000.0),

        Achievement("ach_max_level_b",
            "Maestro constructor", "Sube un edificio al nivel 10.",
            "🏗️", AchievementCategory.EMPIRE,
            threshold = 10, rewardXp = 1_500, rewardCash = 25_000.0),

        Achievement("ach_workforce_25",
            "Plantilla sólida", "Contrata 25 empleados.",
            "👥", AchievementCategory.EMPIRE,
            threshold = 25, rewardXp = 1_200, rewardCash = 15_000.0),

        Achievement("ach_workforce_50",
            "Gran corporación", "Contrata 50 empleados.",
            "🏛️", AchievementCategory.EMPIRE,
            threshold = 50, rewardXp = 4_000, rewardCash = 75_000.0),

        // ===== CHARACTER (8) =====
        Achievement("ach_level_5",
            "Aprendiz", "Alcanza el nivel 5 del personaje.",
            "🎓", AchievementCategory.CHARACTER,
            threshold = 5, rewardXp = 200, rewardCash = 800.0),

        Achievement("ach_level_10",
            "Veterano", "Alcanza el nivel 10 del personaje.",
            "⭐", AchievementCategory.CHARACTER,
            threshold = 10, rewardXp = 600, rewardCash = 3_500.0),

        Achievement("ach_level_25",
            "Experto curtido", "Alcanza el nivel 25 del personaje.",
            "🏅", AchievementCategory.CHARACTER,
            threshold = 25, rewardXp = 2_500, rewardCash = 30_000.0),

        Achievement("ach_level_50",
            "Maestro", "Alcanza el nivel 50 del personaje.",
            "🏆", AchievementCategory.CHARACTER,
            threshold = 50, rewardXp = 8_000, rewardCash = 150_000.0),

        Achievement("ach_charisma_50",
            "Mr. Carisma", "Alcanza 50 puntos de carisma.",
            "🎙️", AchievementCategory.CHARACTER,
            threshold = 50, rewardXp = 1_500, rewardCash = 12_000.0),

        Achievement("ach_intelligence_50",
            "Cerebro privilegiado", "Alcanza 50 puntos de inteligencia.",
            "🧠", AchievementCategory.CHARACTER,
            threshold = 50, rewardXp = 1_500, rewardCash = 12_000.0),

        Achievement("ach_stats_total_100",
            "Personaje completo", "Suma 100 puntos entre todos tus stats.",
            "💪", AchievementCategory.CHARACTER,
            threshold = 100, rewardXp = 1_000, rewardCash = 8_000.0),

        Achievement("ach_happy_max",
            "Vida plena", "Alcanza 100 de felicidad.",
            "😊", AchievementCategory.CHARACTER,
            threshold = 100, rewardXp = 500, rewardCash = 2_500.0),

        // ===== MARKET (6) =====
        Achievement("ach_market_10_tx",
            "Comerciante novato", "Realiza 10 transacciones de mercado.",
            "🛒", AchievementCategory.MARKET,
            threshold = 10, rewardXp = 200, rewardCash = 800.0),

        Achievement("ach_market_100_tx",
            "Maestro del mercado", "Realiza 100 transacciones de mercado.",
            "📜", AchievementCategory.MARKET,
            threshold = 100, rewardXp = 1_500, rewardCash = 15_000.0),

        Achievement("ach_sold_1000",
            "A granel", "Vende 1.000 unidades de un mismo recurso.",
            "📦", AchievementCategory.MARKET,
            threshold = 1_000, rewardXp = 1_000, rewardCash = 8_000.0),

        Achievement("ach_invertor",
            "Inversor", "Posee 10 acciones o más en bolsa.",
            "📈", AchievementCategory.MARKET,
            threshold = 10, rewardXp = 250, rewardCash = 1_000.0),

        Achievement("ach_trader",
            "Trader", "Posee 50 acciones o más en bolsa.",
            "💹", AchievementCategory.MARKET,
            threshold = 50, rewardXp = 1_200, rewardCash = 12_000.0),

        Achievement("ach_bursatil",
            "Bursátil", "Acumula 100.000 invertidos en bolsa.",
            "🏦", AchievementCategory.MARKET,
            threshold = 100_000, rewardXp = 2_500, rewardCash = 25_000.0),

        // ===== REAL_ESTATE (5) =====
        Achievement("ach_first_property",
            "Casero", "Compra tu primer inmueble.",
            "🏠", AchievementCategory.REAL_ESTATE,
            threshold = 1, rewardXp = 250, rewardCash = 1_500.0),

        Achievement("ach_5_properties",
            "Inmobiliaria", "Posee 5 inmuebles.",
            "🏢", AchievementCategory.REAL_ESTATE,
            threshold = 5, rewardXp = 1_000, rewardCash = 12_000.0),

        Achievement("ach_10_properties",
            "Magnate inmobiliario", "Posee 10 inmuebles.",
            "🏘️", AchievementCategory.REAL_ESTATE,
            threshold = 10, rewardXp = 4_000, rewardCash = 80_000.0),

        Achievement("ach_skyscraper",
            "Toca el cielo", "Posee al menos 1 rascacielos.",
            "🌆", AchievementCategory.REAL_ESTATE,
            threshold = 1, rewardXp = 5_000, rewardCash = 100_000.0),

        Achievement("ach_rent_5k",
            "Rentista", "Genera 5.000 al día por rentas netas.",
            "💸", AchievementCategory.REAL_ESTATE,
            threshold = 5_000, rewardXp = 2_000, rewardCash = 30_000.0),

        // ===== RESEARCH (4) =====
        Achievement("ach_first_tech",
            "Innovador", "Completa tu primera investigación.",
            "🔬", AchievementCategory.RESEARCH,
            threshold = 1, rewardXp = 200, rewardCash = 1_000.0),

        Achievement("ach_5_tech",
            "Experto", "Completa 5 tecnologías.",
            "🧪", AchievementCategory.RESEARCH,
            threshold = 5, rewardXp = 1_000, rewardCash = 10_000.0),

        Achievement("ach_10_tech",
            "Científico", "Completa 10 tecnologías.",
            "👨‍🔬", AchievementCategory.RESEARCH,
            threshold = 10, rewardXp = 3_000, rewardCash = 50_000.0),

        Achievement("ach_all_tech",
            "Genio", "Completa TODAS las tecnologías.",
            "🎓", AchievementCategory.RESEARCH,
            threshold = TechCatalog.all.size.toLong(),
            rewardXp = 10_000, rewardCash = 500_000.0),

        // ===== SOCIAL (3) =====
        Achievement("ach_rep_50",
            "Reputado", "Alcanza 50 de reputación corporativa.",
            "🌟", AchievementCategory.SOCIAL,
            threshold = 50, rewardXp = 600, rewardCash = 3_000.0),

        Achievement("ach_rep_80",
            "Marca de prestigio", "Alcanza 80 de reputación corporativa.",
            "👑", AchievementCategory.SOCIAL,
            threshold = 80, rewardXp = 2_500, rewardCash = 25_000.0),

        Achievement("ach_rep_max",
            "Icónico", "Alcanza 100 de reputación corporativa.",
            "💎", AchievementCategory.SOCIAL,
            threshold = 100, rewardXp = 6_000, rewardCash = 80_000.0),

        // ===== MILESTONE (5) =====
        Achievement("ach_yacht_owner",
            "Yate Privado", "Posee un yate en tu inventario.",
            "⛵", AchievementCategory.MILESTONE,
            threshold = 1, rewardXp = 3_000, rewardCash = 50_000.0),

        Achievement("ach_car_owner",
            "Coche Propio", "Posee un automóvil en tu inventario.",
            "🚗", AchievementCategory.MILESTONE,
            threshold = 1, rewardXp = 800, rewardCash = 5_000.0),

        Achievement("ach_jewelry_owner",
            "Toque de lujo", "Posee joyas en tu inventario.",
            "💍", AchievementCategory.MILESTONE,
            threshold = 1, rewardXp = 600, rewardCash = 3_000.0),

        Achievement("ach_day_30",
            "Un mes en el cargo", "Sobrevive 30 días in-game.",
            "📅", AchievementCategory.MILESTONE,
            threshold = 30, rewardXp = 2_000, rewardCash = 20_000.0),

        Achievement("ach_day_100",
            "Centenario", "Sobrevive 100 días in-game.",
            "🎉", AchievementCategory.MILESTONE,
            threshold = 100, rewardXp = 8_000, rewardCash = 150_000.0),

        // ===== SECRET (4 hidden) =====
        Achievement("ach_hidden_bankrupt",
            "Quiebra técnica", "Llegaste a tener menos de 0 en caja… sigues vivo.",
            "💀", AchievementCategory.SECRET,
            threshold = 1, rewardXp = 500, rewardCash = 5_000.0,
            hidden = true),

        Achievement("ach_hidden_no_employees",
            "Sin empleados", "Mantén tu imperio activo sin un solo empleado.",
            "🤖", AchievementCategory.SECRET,
            threshold = 1, rewardXp = 800, rewardCash = 8_000.0,
            hidden = true),

        Achievement("ach_hidden_night_owl",
            "Trasnochador", "Juega entre las 3 y las 4 de la madrugada (mundo real).",
            "🌙", AchievementCategory.SECRET,
            threshold = 1, rewardXp = 1_500, rewardCash = 15_000.0,
            hidden = true),

        Achievement("ach_hidden_speedster",
            "Velocidad luz", "Juega 30 segundos seguidos a velocidad x8.",
            "⚡", AchievementCategory.SECRET,
            threshold = 30, rewardXp = 1_000, rewardCash = 10_000.0,
            hidden = true)
    )

    private val byId = all.associateBy { it.id }
    fun byId(id: String): Achievement? = byId[id]

    fun byCategory(c: AchievementCategory): List<Achievement> =
        all.filter { it.category == c }
}
