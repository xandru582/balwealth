package com.empiretycoon.game.model

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Atributos del personaje (estilo Torn City).
 * Rango práctico 1–200. Afectan bonificaciones globales y actividades.
 */
@Serializable
data class PlayerStats(
    val intelligence: Int = 5,   // investigación más rápida, gestión
    val strength: Int = 5,       // trabajos manuales, salud
    val charisma: Int = 5,       // mejores ventas, contratación
    val luck: Int = 5,           // eventos positivos, bolsa
    val dexterity: Int = 5       // velocidad de producción
) {
    val total: Int get() = intelligence + strength + charisma + luck + dexterity
}

/**
 * Jugador — estado RPG.
 * Energía: 0–100, se consume con actividades (entrenar, trabajar).
 * Felicidad: 0–100, afecta productividad global si baja.
 */
@Serializable
data class Player(
    val name: String = "Empresario",
    val level: Int = 1,
    val xp: Long = 0,
    val energy: Int = 100,
    val maxEnergy: Int = 100,
    val happiness: Int = 80,
    val stats: PlayerStats = PlayerStats(),
    val cash: Double = 0.0            // dinero personal (distinto al corporativo)
) {
    fun xpForNextLevel(): Long = (100 * 1.35.pow(level - 1)).toLong()

    fun addXp(amount: Long): Player {
        if (amount <= 0) return this
        var newXp = xp + amount
        var newLevel = level
        // FIX P0: la versión anterior tenía precedencia rota:
        //   `100 * 1.35.pow(...).toLong()` truncaba antes de multiplicar.
        //   Resultado: el check y el resto usaban valores DISTINTOS y el
        //   bucle podía dejar xp negativo o conceder niveles de más.
        while (true) {
            val cost = (100.0 * 1.35.pow(newLevel - 1)).toLong()
            if (cost <= 0 || newXp < cost) break
            newXp -= cost
            newLevel++
            // Hard cap para evitar overflow numérico en runs muy largos.
            if (newLevel >= 999) break
        }
        return copy(xp = newXp, level = newLevel)
    }

    fun withEnergy(delta: Int): Player =
        copy(energy = min(maxEnergy, max(0, energy + delta)))

    fun withHappiness(delta: Int): Player =
        copy(happiness = min(100, max(0, happiness + delta)))
}
