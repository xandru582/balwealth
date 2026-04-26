package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Clasificación de recursos por tipo, usada para filtrar el mercado y
 * agrupar en la UI.
 */
enum class ResourceCategory { RAW, FOOD, MATERIAL, COMPONENT, GOOD, SERVICE, LUXURY }

/**
 * Recurso del juego: materia prima, producto manufacturado, servicio, etc.
 * @property basePrice precio base, el mercado lo modula con oferta/demanda.
 */
@Serializable
data class Resource(
    val id: String,
    val name: String,
    val category: ResourceCategory,
    val basePrice: Double,
    val emoji: String
)

/** Catálogo estático de recursos. El id es estable y usado como clave. */
object ResourceCatalog {
    val all: List<Resource> = listOf(
        // RAW
        Resource("seed", "Semillas", ResourceCategory.RAW, 2.0, "\uD83C\uDF31"),
        Resource("water", "Agua", ResourceCategory.RAW, 0.5, "\uD83D\uDCA7"),
        Resource("wood_log", "Troncos", ResourceCategory.RAW, 4.0, "\uD83E\uDEB5"),
        Resource("iron_ore", "Mineral de hierro", ResourceCategory.RAW, 8.0, "\u26CF\uFE0F"),
        Resource("coal", "Carbón", ResourceCategory.RAW, 6.0, "\u26AB"),
        Resource("oil", "Petróleo", ResourceCategory.RAW, 25.0, "\uD83D\uDEE2\uFE0F"),
        Resource("silicon", "Silicio", ResourceCategory.RAW, 30.0, "\uD83E\uDEA8"),

        // FOOD
        Resource("wheat", "Trigo", ResourceCategory.FOOD, 6.0, "\uD83C\uDF3E"),
        Resource("flour", "Harina", ResourceCategory.FOOD, 12.0, "\uD83C\uDF5E"),
        Resource("bread", "Pan", ResourceCategory.FOOD, 22.0, "\uD83C\uDF5E"),
        Resource("milk", "Leche", ResourceCategory.FOOD, 8.0, "\uD83E\uDD5B"),
        Resource("cheese", "Queso", ResourceCategory.FOOD, 28.0, "\uD83E\uDDC0"),

        // MATERIAL
        Resource("plank", "Tablones", ResourceCategory.MATERIAL, 14.0, "\uD83E\uDEB5"),
        Resource("iron_ingot", "Lingote de hierro", ResourceCategory.MATERIAL, 28.0, "\uD83E\uDDF1"),
        Resource("steel", "Acero", ResourceCategory.MATERIAL, 55.0, "\u2699\uFE0F"),
        Resource("plastic", "Plástico", ResourceCategory.MATERIAL, 35.0, "\uD83E\uDDEA"),
        Resource("glass", "Vidrio", ResourceCategory.MATERIAL, 30.0, "\uD83E\uDDEA"),

        // COMPONENT
        Resource("gear", "Engranaje", ResourceCategory.COMPONENT, 70.0, "\u2699\uFE0F"),
        Resource("circuit", "Circuito", ResourceCategory.COMPONENT, 120.0, "\uD83E\uDDE0"),
        Resource("battery", "Batería", ResourceCategory.COMPONENT, 95.0, "\uD83D\uDD0B"),
        Resource("engine", "Motor", ResourceCategory.COMPONENT, 260.0, "\uD83D\uDD27"),

        // GOOD
        Resource("furniture", "Muebles", ResourceCategory.GOOD, 180.0, "\uD83E\uDE91"),
        Resource("bicycle", "Bicicleta", ResourceCategory.GOOD, 340.0, "\uD83D\uDEB2"),
        Resource("smartphone", "Smartphone", ResourceCategory.GOOD, 620.0, "\uD83D\uDCF1"),
        Resource("car", "Automóvil", ResourceCategory.GOOD, 3400.0, "\uD83D\uDE97"),

        // SERVICE
        Resource("consulting", "Consultoría", ResourceCategory.SERVICE, 450.0, "\uD83E\uDDD1\u200D\uD83D\uDCBC"),
        Resource("software", "Software", ResourceCategory.SERVICE, 900.0, "\uD83D\uDCBB"),

        // LUXURY
        Resource("jewelry", "Joyas", ResourceCategory.LUXURY, 1800.0, "\uD83D\uDC8E"),
        Resource("yacht", "Yate", ResourceCategory.LUXURY, 28000.0, "\u26F5")
    )

    private val byId: Map<String, Resource> = all.associateBy { it.id }
    fun byId(id: String): Resource = byId[id] ?: error("Recurso no encontrado: $id")
    fun tryById(id: String): Resource? = byId[id]
}
