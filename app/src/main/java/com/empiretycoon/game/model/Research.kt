package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Nodo tecnológico. `prerequisites` bloquea hasta investigar previos.
 * `cost` se paga de golpe. Al completar, agrega bonificaciones globales.
 */
@Serializable
data class Technology(
    val id: String,
    val name: String,
    val description: String,
    val cost: Double,
    val researchSeconds: Int,
    val prerequisites: List<String> = emptyList(),
    val productionBonus: Double = 0.0,    // +% a todas las producciones
    val marketBonus: Double = 0.0,        // +% al precio de venta
    val unlocksRecipeIds: List<String> = emptyList()
)

object TechCatalog {
    val all: List<Technology> = listOf(
        Technology("agronomy", "Agronomía",
            "Mejora técnicas de cultivo y producción alimentaria.",
            cost = 1_500.0, researchSeconds = 180,
            productionBonus = 0.05),

        Technology("metallurgy", "Metalurgia",
            "Desbloquea acero y refina el uso de metales.",
            cost = 6_000.0, researchSeconds = 360,
            unlocksRecipeIds = listOf("smelt_steel")),

        Technology("polymers", "Polímeros",
            "Producción de plástico a partir de petróleo.",
            cost = 9_500.0, researchSeconds = 420,
            prerequisites = listOf("metallurgy"),
            unlocksRecipeIds = listOf("refine_plastic")),

        Technology("oil_drilling", "Extracción petrolífera",
            "Permite perforar pozos de petróleo.",
            cost = 12_000.0, researchSeconds = 480,
            prerequisites = listOf("metallurgy"),
            unlocksRecipeIds = listOf("pump_oil")),

        Technology("electrochemistry", "Electroquímica",
            "Baterías comerciales para múltiples productos.",
            cost = 18_000.0, researchSeconds = 520,
            prerequisites = listOf("polymers"),
            unlocksRecipeIds = listOf("make_battery")),

        Technology("semiconductors", "Semiconductores",
            "Fabricación de chips y circuitos integrados.",
            cost = 35_000.0, researchSeconds = 720,
            prerequisites = listOf("polymers"),
            unlocksRecipeIds = listOf("make_silicon", "make_circuit")),

        Technology("consumer_electronics", "Electrónica de consumo",
            "Smartphones y dispositivos para el mercado masivo.",
            cost = 65_000.0, researchSeconds = 900,
            prerequisites = listOf("semiconductors", "electrochemistry"),
            unlocksRecipeIds = listOf("build_smartphone"),
            marketBonus = 0.05),

        Technology("automotive", "Industria automotriz",
            "Producción en masa de vehículos.",
            cost = 120_000.0, researchSeconds = 1200,
            prerequisites = listOf("metallurgy", "polymers"),
            unlocksRecipeIds = listOf("build_car"),
            productionBonus = 0.05),

        Technology("software_eng", "Ingeniería de software",
            "Desarrollo profesional de software.",
            cost = 22_000.0, researchSeconds = 600,
            prerequisites = listOf("semiconductors"),
            unlocksRecipeIds = listOf("develop_software")),

        Technology("luxury_craft", "Artesanía de lujo",
            "Joyería y piezas exclusivas.",
            cost = 40_000.0, researchSeconds = 600,
            prerequisites = listOf("metallurgy"),
            unlocksRecipeIds = listOf("craft_jewelry"),
            marketBonus = 0.08),

        Technology("naval_eng", "Ingeniería naval",
            "Diseño y construcción de embarcaciones de recreo.",
            cost = 250_000.0, researchSeconds = 1600,
            prerequisites = listOf("automotive"),
            unlocksRecipeIds = listOf("build_yacht")),

        Technology("lean_mgmt", "Gestión lean",
            "Procesos optimizados en toda la empresa.",
            cost = 45_000.0, researchSeconds = 700,
            productionBonus = 0.12),

        Technology("marketing_1", "Marketing digital",
            "Mayor visibilidad y precios de venta.",
            cost = 14_000.0, researchSeconds = 480,
            marketBonus = 0.07),

        Technology("automation", "Automatización",
            "Robots en la línea reducen tiempo de ciclo.",
            cost = 95_000.0, researchSeconds = 900,
            prerequisites = listOf("semiconductors", "lean_mgmt"),
            productionBonus = 0.20)
    )

    private val byId = all.associateBy { it.id }
    fun byId(id: String): Technology? = byId[id]
}

/** Estado de investigación en el save game. */
@Serializable
data class ResearchState(
    val completed: Set<String> = emptySet(),
    val inProgressId: String? = null,
    val inProgressSecondsLeft: Double = 0.0
) {
    fun isCompleted(tech: String) = completed.contains(tech)
    fun canStart(tech: Technology): Boolean =
        !isCompleted(tech.id) &&
        inProgressId == null &&
        tech.prerequisites.all { isCompleted(it) }
}
