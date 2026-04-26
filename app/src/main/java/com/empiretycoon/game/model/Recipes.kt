package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Receta de producción. Los ingredientes se consumen al inicio del ciclo;
 * los outputs aparecen al completarse. `seconds` es tiempo real de ciclo en
 * el nivel 1 del edificio (se reduce con niveles y tecnologías).
 */
@Serializable
data class Recipe(
    val id: String,
    val name: String,
    val buildingType: BuildingType,
    val inputs: Map<String, Int>,
    val outputs: Map<String, Int>,
    val seconds: Int,
    val requiredResearch: String? = null
)

object RecipeCatalog {

    val all: List<Recipe> = listOf(
        // --- Granja ---
        Recipe(
            id = "grow_wheat", name = "Cultivar trigo",
            buildingType = BuildingType.FARM,
            inputs = mapOf("seed" to 2, "water" to 3),
            outputs = mapOf("wheat" to 4),
            seconds = 45
        ),
        Recipe(
            id = "milk_cows", name = "Ordeñar vacas",
            buildingType = BuildingType.FARM,
            inputs = mapOf("water" to 2),
            outputs = mapOf("milk" to 3),
            seconds = 40
        ),

        // --- Aserradero ---
        Recipe(
            id = "chop_logs", name = "Talar madera",
            buildingType = BuildingType.SAWMILL,
            inputs = emptyMap(),
            outputs = mapOf("wood_log" to 3),
            seconds = 30
        ),
        Recipe(
            id = "make_planks", name = "Hacer tablones",
            buildingType = BuildingType.SAWMILL,
            inputs = mapOf("wood_log" to 2),
            outputs = mapOf("plank" to 3),
            seconds = 35
        ),

        // --- Mina ---
        Recipe(
            id = "mine_iron", name = "Extraer hierro",
            buildingType = BuildingType.MINE,
            inputs = emptyMap(),
            outputs = mapOf("iron_ore" to 2),
            seconds = 50
        ),
        Recipe(
            id = "mine_coal", name = "Extraer carbón",
            buildingType = BuildingType.MINE,
            inputs = emptyMap(),
            outputs = mapOf("coal" to 3),
            seconds = 45
        ),
        Recipe(
            id = "pump_oil", name = "Bombear petróleo",
            buildingType = BuildingType.MINE,
            inputs = emptyMap(),
            outputs = mapOf("oil" to 1),
            seconds = 60,
            requiredResearch = "oil_drilling"
        ),

        // --- Molino (panadería base) ---
        Recipe(
            id = "mill_flour", name = "Moler harina",
            buildingType = BuildingType.BAKERY,
            inputs = mapOf("wheat" to 3),
            outputs = mapOf("flour" to 2),
            seconds = 30
        ),
        Recipe(
            id = "bake_bread", name = "Hornear pan",
            buildingType = BuildingType.BAKERY,
            inputs = mapOf("flour" to 2, "water" to 1),
            outputs = mapOf("bread" to 3),
            seconds = 40
        ),
        Recipe(
            id = "make_cheese", name = "Producir queso",
            buildingType = BuildingType.BAKERY,
            inputs = mapOf("milk" to 3),
            outputs = mapOf("cheese" to 1),
            seconds = 60
        ),

        // --- Fundición ---
        Recipe(
            id = "smelt_iron", name = "Fundir hierro",
            buildingType = BuildingType.SMELTER,
            inputs = mapOf("iron_ore" to 2, "coal" to 1),
            outputs = mapOf("iron_ingot" to 1),
            seconds = 55
        ),
        Recipe(
            id = "smelt_steel", name = "Producir acero",
            buildingType = BuildingType.SMELTER,
            inputs = mapOf("iron_ingot" to 2, "coal" to 2),
            outputs = mapOf("steel" to 1),
            seconds = 65,
            requiredResearch = "metallurgy"
        ),
        Recipe(
            id = "make_glass", name = "Fabricar vidrio",
            buildingType = BuildingType.SMELTER,
            inputs = mapOf("coal" to 1),
            outputs = mapOf("glass" to 2),
            seconds = 40
        ),

        // --- Refinería ---
        Recipe(
            id = "refine_plastic", name = "Refinar plástico",
            buildingType = BuildingType.REFINERY,
            inputs = mapOf("oil" to 2),
            outputs = mapOf("plastic" to 3),
            seconds = 60,
            requiredResearch = "polymers"
        ),
        Recipe(
            id = "make_silicon", name = "Purificar silicio",
            buildingType = BuildingType.REFINERY,
            inputs = mapOf("coal" to 2, "glass" to 1),
            outputs = mapOf("silicon" to 1),
            seconds = 70,
            requiredResearch = "semiconductors"
        ),

        // --- Fábrica ---
        Recipe(
            id = "make_gear", name = "Producir engranajes",
            buildingType = BuildingType.FACTORY,
            inputs = mapOf("steel" to 1),
            outputs = mapOf("gear" to 2),
            seconds = 45
        ),
        Recipe(
            id = "make_battery", name = "Ensamblar baterías",
            buildingType = BuildingType.FACTORY,
            inputs = mapOf("plastic" to 1, "iron_ingot" to 1),
            outputs = mapOf("battery" to 1),
            seconds = 55,
            requiredResearch = "electrochemistry"
        ),
        Recipe(
            id = "make_circuit", name = "Fabricar circuitos",
            buildingType = BuildingType.FACTORY,
            inputs = mapOf("silicon" to 1, "plastic" to 1),
            outputs = mapOf("circuit" to 2),
            seconds = 65,
            requiredResearch = "semiconductors"
        ),
        Recipe(
            id = "assemble_engine", name = "Ensamblar motor",
            buildingType = BuildingType.FACTORY,
            inputs = mapOf("gear" to 3, "steel" to 2),
            outputs = mapOf("engine" to 1),
            seconds = 80
        ),
        Recipe(
            id = "build_furniture", name = "Construir muebles",
            buildingType = BuildingType.FACTORY,
            inputs = mapOf("plank" to 4),
            outputs = mapOf("furniture" to 1),
            seconds = 60
        ),
        Recipe(
            id = "build_bicycle", name = "Ensamblar bicicleta",
            buildingType = BuildingType.FACTORY,
            inputs = mapOf("steel" to 2, "plastic" to 1),
            outputs = mapOf("bicycle" to 1),
            seconds = 75
        ),
        Recipe(
            id = "build_smartphone", name = "Ensamblar smartphone",
            buildingType = BuildingType.FACTORY,
            inputs = mapOf("circuit" to 2, "battery" to 1, "glass" to 1, "plastic" to 1),
            outputs = mapOf("smartphone" to 1),
            seconds = 90,
            requiredResearch = "consumer_electronics"
        ),
        Recipe(
            id = "build_car", name = "Ensamblar automóvil",
            buildingType = BuildingType.FACTORY,
            inputs = mapOf("engine" to 1, "steel" to 8, "plastic" to 4, "glass" to 2),
            outputs = mapOf("car" to 1),
            seconds = 140,
            requiredResearch = "automotive"
        ),

        // --- Oficina (servicios) ---
        Recipe(
            id = "offer_consulting", name = "Vender consultoría",
            buildingType = BuildingType.OFFICE,
            inputs = emptyMap(),
            outputs = mapOf("consulting" to 1),
            seconds = 50
        ),
        Recipe(
            id = "develop_software", name = "Desarrollar software",
            buildingType = BuildingType.OFFICE,
            inputs = mapOf("circuit" to 1),
            outputs = mapOf("software" to 1),
            seconds = 80,
            requiredResearch = "software_eng"
        ),

        // --- Taller joyero ---
        Recipe(
            id = "craft_jewelry", name = "Tallar joyas",
            buildingType = BuildingType.JEWELRY,
            inputs = mapOf("iron_ingot" to 1, "glass" to 1),
            outputs = mapOf("jewelry" to 1),
            seconds = 110,
            requiredResearch = "luxury_craft"
        ),

        // --- Astillero ---
        Recipe(
            id = "build_yacht", name = "Construir yate",
            buildingType = BuildingType.SHIPYARD,
            inputs = mapOf("steel" to 20, "plastic" to 10, "engine" to 2, "furniture" to 4),
            outputs = mapOf("yacht" to 1),
            seconds = 240,
            requiredResearch = "naval_eng"
        )
    )

    private val byId = all.associateBy { it.id }
    fun byId(id: String): Recipe? = byId[id]
    fun forBuilding(type: BuildingType): List<Recipe> = all.filter { it.buildingType == type }
}
