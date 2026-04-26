package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Misiones guiadas — dan hilo narrativo y recompensas concretas.
 * El sistema las completa automáticamente cuando se cumple la condición.
 */
@Serializable
data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val goalDescription: String,
    val rewardCash: Double = 0.0,
    val rewardXp: Long = 0,
    val rewardReputation: Int = 0,
    val completed: Boolean = false,
    val claimed: Boolean = false
)

object QuestCatalog {
    val all: List<Quest> = listOf(
        Quest("q_first_building",
            "Primer paso",
            "Toda gran empresa empieza con una instalación.",
            "Construye tu primer edificio.",
            rewardCash = 500.0, rewardXp = 100, rewardReputation = 2),

        Quest("q_hire_first",
            "Nómina en marcha",
            "Los edificios solos no producen.",
            "Contrata tu primer empleado.",
            rewardCash = 300.0, rewardXp = 80),

        Quest("q_first_sale",
            "Primer ingreso",
            "El mercado espera tus productos.",
            "Vende algo en el mercado.",
            rewardXp = 120, rewardReputation = 3),

        Quest("q_first_tech",
            "Innovación",
            "La tecnología es tu ventaja competitiva.",
            "Completa tu primera investigación.",
            rewardXp = 200, rewardReputation = 3),

        Quest("q_cash_100k",
            "Seis cifras",
            "Cien mil en caja, un hito simbólico.",
            "Acumula 100.000 en caja de empresa.",
            rewardXp = 400, rewardReputation = 8),

        Quest("q_cash_1m",
            "Millonario",
            "Siete cifras. Ya juegas en otra liga.",
            "Acumula 1.000.000 en caja de empresa.",
            rewardXp = 2_000, rewardReputation = 20),

        Quest("q_first_property",
            "Ingresos pasivos",
            "El rentismo también es una estrategia.",
            "Compra tu primer inmueble.",
            rewardXp = 300, rewardReputation = 5),

        Quest("q_portfolio_10",
            "Inversor",
            "Diversifica tu patrimonio en bolsa.",
            "Posee 10 acciones o más.",
            rewardXp = 250),

        Quest("q_level_10",
            "Veterano",
            "Experiencia amplia en los negocios.",
            "Alcanza el nivel 10 del personaje.",
            rewardCash = 5_000.0, rewardXp = 500, rewardReputation = 10),

        Quest("q_5_buildings",
            "Conglomerado",
            "Diversificación productiva.",
            "Ten 5 edificios activos.",
            rewardCash = 4_000.0, rewardXp = 400, rewardReputation = 6)
    )
}
