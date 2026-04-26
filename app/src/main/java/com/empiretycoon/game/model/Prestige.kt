package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Categorías de perks del prestigio para agrupar en la UI.
 */
enum class PrestigePerkCategory(val displayName: String, val emoji: String) {
    PRODUCTION("Producción", "🏭"),
    ECONOMY("Economía", "💰"),
    PERSONNEL("Personal", "👥"),
    LUCK("Suerte", "🍀"),
    META("Meta", "✨")
}

/**
 * Perk comprable con puntos de prestigio. `multiplier` se interpreta según el id.
 */
data class PrestigePerk(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val cost: Int,
    val multiplier: Double,
    val category: PrestigePerkCategory
)

/**
 * Estado del prestigio. `prestigePoints` son los puntos disponibles para
 * gastar en perks; `totalPointsEarned` es el histórico (de cara a logros
 * futuros). `lifetimeCash` y `lifetimeProductionUnits` se mantienen entre
 * renacimientos como métricas vitalicias.
 */
@Serializable
data class PrestigeState(
    val prestigeLevel: Int = 0,
    val prestigePoints: Long = 0,
    val totalPointsEarned: Long = 0,
    val perks: Set<String> = emptySet(),
    val lifetimeCash: Double = 0.0,
    val lifetimeProductionUnits: Long = 0
) {
    fun owns(perkId: String): Boolean = perks.contains(perkId)
}

object PrestigePerkCatalog {
    val all: List<PrestigePerk> = listOf(
        // ===== PRODUCTION =====
        PrestigePerk("perk_prod_25",
            "Manos en la masa",
            "+25% productividad inicial en todos los edificios.",
            "🛠️", cost = 1, multiplier = 0.25,
            category = PrestigePerkCategory.PRODUCTION),

        PrestigePerk("perk_prod_50",
            "Operación afinada",
            "+50% productividad inicial en todos los edificios.",
            "⚡", cost = 3, multiplier = 0.50,
            category = PrestigePerkCategory.PRODUCTION),

        PrestigePerk("perk_prod_100",
            "Línea automatizada",
            "+100% productividad inicial. Industria de élite.",
            "🤖", cost = 8, multiplier = 1.0,
            category = PrestigePerkCategory.PRODUCTION),

        PrestigePerk("perk_storage_2x",
            "Logística avanzada",
            "Doble capacidad de almacenamiento desde el inicio.",
            "📦", cost = 2, multiplier = 2.0,
            category = PrestigePerkCategory.PRODUCTION),

        // ===== ECONOMY =====
        PrestigePerk("perk_starter_50k",
            "Capital inicial",
            "Empieza con +50.000 en caja.",
            "💵", cost = 1, multiplier = 50_000.0,
            category = PrestigePerkCategory.ECONOMY),

        PrestigePerk("perk_starter_500k",
            "Capital élite",
            "Empieza con +500.000 en caja.",
            "💰", cost = 4, multiplier = 500_000.0,
            category = PrestigePerkCategory.ECONOMY),

        PrestigePerk("perk_market_15",
            "Negociador",
            "+15% al precio de venta en el mercado.",
            "📈", cost = 3, multiplier = 0.15,
            category = PrestigePerkCategory.ECONOMY),

        PrestigePerk("perk_buy_discount",
            "Compras al por mayor",
            "−10% en precios de compra del mercado.",
            "🛒", cost = 2, multiplier = 0.10,
            category = PrestigePerkCategory.ECONOMY),

        PrestigePerk("perk_rent_boost",
            "Casero pro",
            "+20% en rentas inmobiliarias.",
            "🏠", cost = 3, multiplier = 0.20,
            category = PrestigePerkCategory.ECONOMY),

        // ===== PERSONNEL =====
        PrestigePerk("perk_starter_level",
            "Empresa establecida",
            "Empiezas con la empresa en nivel 3.",
            "🏢", cost = 2, multiplier = 3.0,
            category = PrestigePerkCategory.PERSONNEL),

        PrestigePerk("perk_extra_employee_slot",
            "Slot extra de empleados",
            "Puedes tener un edificio extra activo desde el primer día.",
            "👥", cost = 2, multiplier = 1.0,
            category = PrestigePerkCategory.PERSONNEL),

        PrestigePerk("perk_loyal_team",
            "Equipo leal",
            "Los empleados arrancan con +20% de lealtad.",
            "🤝", cost = 2, multiplier = 0.20,
            category = PrestigePerkCategory.PERSONNEL),

        PrestigePerk("perk_cheap_payroll",
            "Sueldos justos",
            "−15% en gastos de nómina.",
            "💼", cost = 3, multiplier = 0.15,
            category = PrestigePerkCategory.PERSONNEL),

        // ===== LUCK =====
        PrestigePerk("perk_lucky_events",
            "Suerte del fundador",
            "Mejor RNG en eventos: más positivos, menos negativos.",
            "🍀", cost = 4, multiplier = 0.25,
            category = PrestigePerkCategory.LUCK),

        PrestigePerk("perk_critical_research",
            "Eureka",
            "10% de probabilidad de completar investigaciones al instante.",
            "💡", cost = 3, multiplier = 0.10,
            category = PrestigePerkCategory.LUCK),

        // ===== META =====
        PrestigePerk("perk_xp_boost",
            "Aprendizaje acelerado",
            "+25% XP en todas las acciones.",
            "📚", cost = 2, multiplier = 0.25,
            category = PrestigePerkCategory.META),

        PrestigePerk("perk_offline_2x",
            "Sin descanso",
            "El progreso offline se duplica.",
            "🌙", cost = 5, multiplier = 2.0,
            category = PrestigePerkCategory.META),

        PrestigePerk("perk_speed_unlock",
            "Velocidad x16",
            "Desbloquea el modo de velocidad x16.",
            "⏩", cost = 6, multiplier = 16.0,
            category = PrestigePerkCategory.META)
    )

    private val byId = all.associateBy { it.id }
    fun byId(id: String): PrestigePerk? = byId[id]

    fun byCategory(c: PrestigePerkCategory): List<PrestigePerk> =
        all.filter { it.category == c }
}
