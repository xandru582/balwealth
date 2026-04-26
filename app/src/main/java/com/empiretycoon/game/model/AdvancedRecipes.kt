package com.empiretycoon.game.model

/**
 * Catálogo extendido de recetas avanzadas. NO modifica el RecipeCatalog
 * existente; provee un superset accesible vía `mergedAll()`.
 *
 * Convenio: todos los ids llevan el prefijo `adv_` para evitar colisiones
 * y simplificar filtros en la UI ("recetas avanzadas / con calidad").
 *
 * Estas recetas:
 *  - Suelen consumir más insumos que su análoga base.
 *  - Producen menos unidades pero de mayor valor implícito (la calidad
 *    final la decide el QualityEngine al ejecutar la receta).
 *  - Requieren investigación específica cuando aplica.
 */
object AdvancedRecipeCatalog {

    val all: List<Recipe> = listOf(

        // ----- Panadería / Granja artesanal -----
        Recipe(
            id = "adv_artisan_bread",
            name = "Pan artesanal",
            buildingType = BuildingType.BAKERY,
            inputs = mapOf("flour" to 3, "water" to 2, "milk" to 1),
            outputs = mapOf("bread" to 4),
            seconds = 55
        ),
        Recipe(
            id = "adv_aged_cheese",
            name = "Queso curado",
            buildingType = BuildingType.BAKERY,
            inputs = mapOf("milk" to 5),
            outputs = mapOf("cheese" to 2),
            seconds = 95
        ),
        Recipe(
            id = "adv_premium_flour",
            name = "Harina premium",
            buildingType = BuildingType.BAKERY,
            inputs = mapOf("wheat" to 5),
            outputs = mapOf("flour" to 3),
            seconds = 45
        ),

        // ----- Aserradero -----
        Recipe(
            id = "adv_premium_planks",
            name = "Tablones de roble",
            buildingType = BuildingType.SAWMILL,
            inputs = mapOf("wood_log" to 4),
            outputs = mapOf("plank" to 5),
            seconds = 50
        ),

        // ----- Mina (variantes) -----
        Recipe(
            id = "adv_pure_iron_ore",
            name = "Mineral seleccionado",
            buildingType = BuildingType.MINE,
            inputs = emptyMap(),
            outputs = mapOf("iron_ore" to 3),
            seconds = 70
        ),
        Recipe(
            id = "adv_anthracite_coal",
            name = "Antracita",
            buildingType = BuildingType.MINE,
            inputs = emptyMap(),
            outputs = mapOf("coal" to 4),
            seconds = 65
        ),

        // ----- Fundición -----
        Recipe(
            id = "adv_surgical_steel",
            name = "Acero quirúrgico",
            buildingType = BuildingType.SMELTER,
            inputs = mapOf("iron_ingot" to 3, "coal" to 3, "glass" to 1),
            outputs = mapOf("steel" to 2),
            seconds = 85,
            requiredResearch = "metallurgy"
        ),
        Recipe(
            id = "adv_optical_glass",
            name = "Vidrio óptico",
            buildingType = BuildingType.SMELTER,
            inputs = mapOf("coal" to 2, "iron_ore" to 1),
            outputs = mapOf("glass" to 3),
            seconds = 60
        ),
        Recipe(
            id = "adv_forged_iron",
            name = "Hierro forjado",
            buildingType = BuildingType.SMELTER,
            inputs = mapOf("iron_ore" to 4, "coal" to 2),
            outputs = mapOf("iron_ingot" to 2),
            seconds = 75
        ),

        // ----- Refinería -----
        Recipe(
            id = "adv_eco_plastic",
            name = "Plástico ecológico",
            buildingType = BuildingType.REFINERY,
            inputs = mapOf("oil" to 1, "water" to 3),
            outputs = mapOf("plastic" to 2),
            seconds = 65,
            requiredResearch = "polymers"
        ),
        Recipe(
            id = "adv_pure_silicon",
            name = "Silicio ultrapuro",
            buildingType = BuildingType.REFINERY,
            inputs = mapOf("coal" to 3, "glass" to 2),
            outputs = mapOf("silicon" to 2),
            seconds = 95,
            requiredResearch = "semiconductors"
        ),

        // ----- Fábrica -----
        Recipe(
            id = "adv_precision_gear",
            name = "Engranajes de precisión",
            buildingType = BuildingType.FACTORY,
            inputs = mapOf("steel" to 2),
            outputs = mapOf("gear" to 3),
            seconds = 60
        ),
        Recipe(
            id = "adv_long_battery",
            name = "Batería de larga duración",
            buildingType = BuildingType.FACTORY,
            inputs = mapOf("plastic" to 2, "iron_ingot" to 2, "silicon" to 1),
            outputs = mapOf("battery" to 2),
            seconds = 75,
            requiredResearch = "electrochemistry"
        ),
        Recipe(
            id = "adv_quantum_circuit",
            name = "Circuitos cuánticos",
            buildingType = BuildingType.FACTORY,
            inputs = mapOf("silicon" to 2, "plastic" to 1, "glass" to 1),
            outputs = mapOf("circuit" to 3),
            seconds = 90,
            requiredResearch = "semiconductors"
        ),
        Recipe(
            id = "adv_high_torque_engine",
            name = "Motor de alto par",
            buildingType = BuildingType.FACTORY,
            inputs = mapOf("gear" to 5, "steel" to 3, "circuit" to 1),
            outputs = mapOf("engine" to 1),
            seconds = 110
        ),
        Recipe(
            id = "adv_build_furniture_premium",
            name = "Muebles de ebanistería",
            buildingType = BuildingType.FACTORY,
            inputs = mapOf("plank" to 6, "iron_ingot" to 1),
            outputs = mapOf("furniture" to 1),
            seconds = 90
        ),
        Recipe(
            id = "adv_build_bicycle_carbon",
            name = "Bicicleta de carbono",
            buildingType = BuildingType.FACTORY,
            inputs = mapOf("steel" to 1, "plastic" to 3, "gear" to 2),
            outputs = mapOf("bicycle" to 1),
            seconds = 100
        ),
        Recipe(
            id = "adv_build_smartphone_titanium",
            name = "Smartphone titanio",
            buildingType = BuildingType.FACTORY,
            inputs = mapOf("circuit" to 4, "battery" to 2, "glass" to 2, "steel" to 1),
            outputs = mapOf("smartphone" to 1),
            seconds = 130,
            requiredResearch = "consumer_electronics"
        ),
        Recipe(
            id = "adv_build_ev",
            name = "Coche eléctrico",
            buildingType = BuildingType.FACTORY,
            inputs = mapOf("battery" to 6, "steel" to 6, "plastic" to 6, "circuit" to 2, "glass" to 2),
            outputs = mapOf("car" to 1),
            seconds = 170,
            requiredResearch = "automotive"
        ),
        Recipe(
            id = "adv_build_car_luxury",
            name = "Coche de lujo",
            buildingType = BuildingType.FACTORY,
            inputs = mapOf("engine" to 2, "steel" to 10, "plastic" to 5, "glass" to 3, "furniture" to 1),
            outputs = mapOf("car" to 1),
            seconds = 200,
            requiredResearch = "automotive"
        ),
        Recipe(
            id = "adv_make_powerpack",
            name = "Powerpack residencial",
            buildingType = BuildingType.FACTORY,
            inputs = mapOf("battery" to 4, "circuit" to 1, "plastic" to 2),
            outputs = mapOf("battery" to 6),
            seconds = 95,
            requiredResearch = "electrochemistry"
        ),

        // ----- Oficina -----
        Recipe(
            id = "adv_dev_software_b2b",
            name = "Software empresarial",
            buildingType = BuildingType.OFFICE,
            inputs = mapOf("circuit" to 2, "consulting" to 1),
            outputs = mapOf("software" to 2),
            seconds = 110,
            requiredResearch = "software_eng"
        ),
        Recipe(
            id = "adv_strategic_consulting",
            name = "Consultoría estratégica",
            buildingType = BuildingType.OFFICE,
            inputs = mapOf("software" to 1),
            outputs = mapOf("consulting" to 3),
            seconds = 70
        ),

        // ----- Joyería -----
        Recipe(
            id = "adv_craft_jewelry_premium",
            name = "Joyería de alta gama",
            buildingType = BuildingType.JEWELRY,
            inputs = mapOf("iron_ingot" to 2, "glass" to 2, "steel" to 1),
            outputs = mapOf("jewelry" to 1),
            seconds = 140,
            requiredResearch = "luxury_craft"
        ),
        Recipe(
            id = "adv_couture_jewelry",
            name = "Joyería de autor",
            buildingType = BuildingType.JEWELRY,
            inputs = mapOf("iron_ingot" to 1, "glass" to 3, "plastic" to 1),
            outputs = mapOf("jewelry" to 2),
            seconds = 165,
            requiredResearch = "luxury_craft"
        ),

        // ----- Astillero -----
        Recipe(
            id = "adv_build_yacht_eco",
            name = "Yate ecológico",
            buildingType = BuildingType.SHIPYARD,
            inputs = mapOf("steel" to 18, "plastic" to 8, "battery" to 6, "engine" to 2, "furniture" to 5),
            outputs = mapOf("yacht" to 1),
            seconds = 260,
            requiredResearch = "naval_eng"
        ),
        Recipe(
            id = "adv_build_yacht_luxury",
            name = "Yate de lujo",
            buildingType = BuildingType.SHIPYARD,
            inputs = mapOf("steel" to 25, "plastic" to 12, "engine" to 3, "furniture" to 8, "jewelry" to 1),
            outputs = mapOf("yacht" to 1),
            seconds = 300,
            requiredResearch = "naval_eng"
        )
    )

    /** Devuelve el catálogo completo (base + avanzado), útil para la UI. */
    fun mergedAll(): List<Recipe> = RecipeCatalog.all + all

    fun byId(id: String): Recipe? =
        all.firstOrNull { it.id == id } ?: RecipeCatalog.byId(id)

    fun forBuilding(type: BuildingType): List<Recipe> =
        mergedAll().filter { it.buildingType == type }

    /** ¿Es una receta "avanzada" (con sistema de calidad)? */
    fun isAdvanced(recipeId: String): Boolean = recipeId.startsWith("adv_")
}
