package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Modos de balanceo del flujo entre edificios de una misma línea.
 *  - JUST_IN_TIME: produce sólo cuando la siguiente etapa va a consumir.
 *  - BUFFER_HEAVY: prefiere acumular intermedios para absorber picos.
 *  - MAX_THROUGHPUT: produce siempre que pueda; el motor empuja al máximo.
 */
enum class BalancingMode(val displayName: String, val emoji: String, val description: String) {
    JUST_IN_TIME("Justo a tiempo", "🎯", "Sin stock intermedio: minimiza el almacén"),
    BUFFER_HEAVY("Búfer alto", "📦", "Acumula intermedios para no parar nunca"),
    MAX_THROUGHPUT("Máxima salida", "🚀", "Empuja a tope con el mayor coste")
}

/**
 * Línea de producción: secuencia ordenada de edificios que ejecutan recetas
 * encadenadas. La línea no anula el comportamiento normal del edificio: lo
 * "empuja" a través del motor de líneas (orquesta autocompras y balanceo).
 */
@Serializable
data class ProductionLine(
    val id: String,
    val name: String,
    /** Edificios de la línea, en orden de ejecución. */
    val buildingIds: List<String>,
    /** Mapa edificio -> id de receta a ejecutar. */
    val recipeIdsPerBuilding: Map<String, String>,
    val balancingModeName: String = BalancingMode.JUST_IN_TIME.name,
    val enabled: Boolean = true,
    val createdTick: Long = 0
) {
    val balancingMode: BalancingMode
        get() = BalancingMode.values().firstOrNull { it.name == balancingModeName }
            ?: BalancingMode.JUST_IN_TIME

    /** Receta del edificio en la posición `idx` (o null si no aplica). */
    fun recipeAt(idx: Int): String? =
        buildingIds.getOrNull(idx)?.let { recipeIdsPerBuilding[it] }
}

/** Estado raíz del subsistema de líneas. */
@Serializable
data class ProductionLinesState(
    val lines: List<ProductionLine> = emptyList(),
    /** Presets desbloqueados (id) — sólo informativo, los presets son catálogo estático. */
    val unlockedPresets: List<String> = emptyList()
) {
    fun byId(id: String): ProductionLine? = lines.firstOrNull { it.id == id }

    fun upsert(line: ProductionLine): ProductionLinesState {
        val replaced = lines.map { if (it.id == line.id) line else it }
        val finalList = if (replaced.any { it.id == line.id }) replaced else replaced + line
        return copy(lines = finalList)
    }

    fun remove(lineId: String): ProductionLinesState =
        copy(lines = lines.filterNot { it.id == lineId })

    fun toggle(lineId: String): ProductionLinesState =
        copy(lines = lines.map { if (it.id == lineId) it.copy(enabled = !it.enabled) else it })

    companion object {
        val Empty: ProductionLinesState = ProductionLinesState()
    }
}

/**
 * Plantilla pública (no serializable) que describe una "línea típica":
 * qué tipos de edificios hacen falta y qué recetas se encadenan. Sirve
 * para el flujo "Crear línea" en la UI: el jugador elige un preset y
 * asigna sus edificios concretos.
 */
data class LinePreset(
    val id: String,
    val name: String,
    val description: String,
    /** Recetas del catálogo (RecipeCatalog + AdvancedRecipeCatalog) a encadenar. */
    val recipeChain: List<String>,
    /** Tipos de edificio que requiere, en el mismo orden que `recipeChain`. */
    val requiredBuildingTypes: List<BuildingType>,
    val emoji: String = "🏭",
    val recommendedBalancing: BalancingMode = BalancingMode.JUST_IN_TIME
) {
    val stages: Int get() = recipeChain.size
}

object LinePresetCatalog {
    val all: List<LinePreset> = listOf(
        LinePreset(
            id = "bread_chain",
            name = "Cadena del pan",
            description = "Granja produce trigo y la panadería lo convierte en pan.",
            recipeChain = listOf("grow_wheat", "mill_flour", "bake_bread"),
            requiredBuildingTypes = listOf(BuildingType.FARM, BuildingType.BAKERY, BuildingType.BAKERY),
            emoji = "🍞",
            recommendedBalancing = BalancingMode.JUST_IN_TIME
        ),
        LinePreset(
            id = "steel_empire",
            name = "Imperio del acero",
            description = "Mineral y carbón → lingote → acero → engranajes → motores.",
            recipeChain = listOf("mine_iron", "mine_coal", "smelt_iron", "smelt_steel", "make_gear", "assemble_engine"),
            requiredBuildingTypes = listOf(
                BuildingType.MINE, BuildingType.MINE, BuildingType.SMELTER,
                BuildingType.SMELTER, BuildingType.FACTORY, BuildingType.FACTORY
            ),
            emoji = "⚙️",
            recommendedBalancing = BalancingMode.BUFFER_HEAVY
        ),
        LinePreset(
            id = "tech_corp",
            name = "Tech Corp",
            description = "Silicio + plástico → circuitos → smartphones de alta gama.",
            recipeChain = listOf("make_silicon", "refine_plastic", "make_circuit", "build_smartphone"),
            requiredBuildingTypes = listOf(
                BuildingType.REFINERY, BuildingType.REFINERY, BuildingType.FACTORY, BuildingType.FACTORY
            ),
            emoji = "📱",
            recommendedBalancing = BalancingMode.MAX_THROUGHPUT
        ),
        LinePreset(
            id = "mediterranean_shipyards",
            name = "Astilleros del Mediterráneo",
            description = "Refinería → acero → engranajes → astilleros: yates de lujo.",
            recipeChain = listOf("refine_plastic", "smelt_steel", "make_gear", "assemble_engine", "build_furniture", "build_yacht"),
            requiredBuildingTypes = listOf(
                BuildingType.REFINERY, BuildingType.SMELTER, BuildingType.FACTORY,
                BuildingType.FACTORY, BuildingType.FACTORY, BuildingType.SHIPYARD
            ),
            emoji = "🛥️",
            recommendedBalancing = BalancingMode.BUFFER_HEAVY
        ),
        LinePreset(
            id = "auto_industry",
            name = "Industria automotriz",
            description = "Acero, plástico, motores y vidrio para coches en cadena.",
            recipeChain = listOf("smelt_steel", "refine_plastic", "make_glass", "assemble_engine", "build_car"),
            requiredBuildingTypes = listOf(
                BuildingType.SMELTER, BuildingType.REFINERY, BuildingType.SMELTER,
                BuildingType.FACTORY, BuildingType.FACTORY
            ),
            emoji = "🚗",
            recommendedBalancing = BalancingMode.BUFFER_HEAVY
        ),
        LinePreset(
            id = "electric_drive",
            name = "Movilidad eléctrica",
            description = "Baterías, plástico y silicio para vehículos eléctricos.",
            recipeChain = listOf("refine_plastic", "make_battery", "make_circuit", "adv_build_ev"),
            requiredBuildingTypes = listOf(
                BuildingType.REFINERY, BuildingType.FACTORY, BuildingType.FACTORY, BuildingType.FACTORY
            ),
            emoji = "🔋",
            recommendedBalancing = BalancingMode.MAX_THROUGHPUT
        ),
        LinePreset(
            id = "luxury_jewelry",
            name = "Lujo de joyería",
            description = "Lingotes y vidrio finísimo para joyas premium.",
            recipeChain = listOf("smelt_iron", "make_glass", "adv_craft_jewelry_premium"),
            requiredBuildingTypes = listOf(
                BuildingType.SMELTER, BuildingType.SMELTER, BuildingType.JEWELRY
            ),
            emoji = "💎",
            recommendedBalancing = BalancingMode.JUST_IN_TIME
        ),
        LinePreset(
            id = "consulting_house",
            name = "Casa de consultoría",
            description = "Oficina como motor: consultoría + software empresarial.",
            recipeChain = listOf("offer_consulting", "develop_software", "adv_dev_software_b2b"),
            requiredBuildingTypes = listOf(
                BuildingType.OFFICE, BuildingType.OFFICE, BuildingType.OFFICE
            ),
            emoji = "🧑‍💼",
            recommendedBalancing = BalancingMode.MAX_THROUGHPUT
        ),
        LinePreset(
            id = "wood_crafts",
            name = "Artesanía en madera",
            description = "Aserradero y muebles de calidad: tablones → muebles premium.",
            recipeChain = listOf("chop_logs", "make_planks", "adv_build_furniture_premium"),
            requiredBuildingTypes = listOf(
                BuildingType.SAWMILL, BuildingType.SAWMILL, BuildingType.FACTORY
            ),
            emoji = "🪑",
            recommendedBalancing = BalancingMode.JUST_IN_TIME
        ),
        LinePreset(
            id = "dairy_artisan",
            name = "Lácteos artesanos",
            description = "Granja + panadería para queso y pan artesano.",
            recipeChain = listOf("milk_cows", "make_cheese", "adv_artisan_bread"),
            requiredBuildingTypes = listOf(
                BuildingType.FARM, BuildingType.BAKERY, BuildingType.BAKERY
            ),
            emoji = "🧀",
            recommendedBalancing = BalancingMode.JUST_IN_TIME
        ),
        LinePreset(
            id = "energy_grid",
            name = "Red energética",
            description = "Refinería de combustibles + baterías para una red verde.",
            recipeChain = listOf("pump_oil", "refine_plastic", "make_battery", "adv_make_powerpack"),
            requiredBuildingTypes = listOf(
                BuildingType.MINE, BuildingType.REFINERY, BuildingType.FACTORY, BuildingType.FACTORY
            ),
            emoji = "⚡",
            recommendedBalancing = BalancingMode.MAX_THROUGHPUT
        ),
        LinePreset(
            id = "premium_phones",
            name = "Smartphones premium",
            description = "Cadena completa para móviles de titanio.",
            recipeChain = listOf("smelt_steel", "make_silicon", "make_circuit", "make_battery", "adv_build_smartphone_titanium"),
            requiredBuildingTypes = listOf(
                BuildingType.SMELTER, BuildingType.REFINERY,
                BuildingType.FACTORY, BuildingType.FACTORY, BuildingType.FACTORY
            ),
            emoji = "🛠️",
            recommendedBalancing = BalancingMode.MAX_THROUGHPUT
        )
    )

    fun byId(id: String): LinePreset? = all.firstOrNull { it.id == id }
}
