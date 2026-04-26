package com.empiretycoon.game.model

import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * Candidato/a inscrito al portal de empleo de la empresa. La oferta caduca
 * a las 24h in-game (1.440 ticks); ApplicantGenerator se encarga de regenerar.
 *
 *  - askingBonus: prima extra al firmar (one-shot pago en cash)
 *  - portrait: emoji que se usa como avatar en la UI
 *  - prevExperienceYears: aporta XP inicial al perfil del nuevo empleado
 */
@Serializable
data class JobApplicant(
    val id: String,
    val name: String,
    val age: Int,
    val role: EmployeeRole,
    val expectedSalary: Double,
    val education: Education,
    val traits: List<EmployeeTrait>,
    val prevExperienceYears: Int,
    val askingBonus: Double,
    val portrait: String,
    val expiresAtTick: Long
) {
    /** XP de bonificación según experiencia previa. */
    fun startingXp(): Long = (prevExperienceYears * 90L).coerceAtMost(1_500L)

    /** Skill base (compatibilidad con Employee clásico). */
    fun startingSkill(): Double {
        val edu = education.baseSalaryMultiplier
        val traitBoost = traits.fold(1.0) { acc, t -> acc * t.productivityModifier }
        val raw = (0.55 + 0.07 * prevExperienceYears) * edu * traitBoost
        return raw.coerceIn(0.4, 2.4)
    }
}

/**
 * Generador de candidatos: pool diario en función de reputación y nivel.
 * Determinista respecto al `Random` que se le pasa para que la simulación
 * sea reproducible al recargar.
 */
object ApplicantGenerator {

    private val firstNames = listOf(
        "Luis","María","Ana","Jorge","Carla","Pedro","Sara","Iván",
        "Lucía","Diego","Marta","Miguel","Elena","David","Laura",
        "Alba","Rafa","Sofía","Tomás","Olga","Javier","Paula",
        "Andrea","Bruno","Clara","Daniel","Eva","Felipe","Gloria",
        "Héctor","Inés","Joel","Karla","Leonor","Mateo","Nuria"
    )

    private val lastNames = listOf(
        "García","Rodríguez","López","Martínez","Hernández","Pérez",
        "Gómez","Sánchez","Romero","Torres","Ramírez","Vargas",
        "Navarro","Ortiz","Ruiz","Castro","Iglesias","Cano",
        "Domínguez","Vega","Reyes","Molina","Cabrera"
    )

    private val portraits = listOf(
        "🧑", "👩", "👨", "🧔", "👱‍♀️", "👨‍🦱", "👩‍🦰",
        "🧑‍🎓", "👩‍💼", "👨‍💼", "🧑‍🔧", "🧑‍🔬", "👩‍🎨",
        "🧑‍🌾", "🧑‍🍳", "👨‍🔧", "👩‍⚕️", "🧑‍💻"
    )

    /**
     * Genera un pool de candidatos. Tamaño y calidad dependen de
     * la reputación (0..100) y el nivel de la empresa.
     */
    fun pool(
        reputation: Int,
        level: Int,
        rng: Random,
        currentTick: Long = 0L
    ): List<JobApplicant> {
        val tier = (reputation / 18).coerceIn(1, 6)
        val count = (4 + tier).coerceIn(4, 9)
        val expiresAt = currentTick + 1_440L      // caduca en 1 día in-game

        return (0 until count).map { idx ->
            val role = pickRole(level, tier, rng)
            val profile = RoleCatalog.get(role)
            val education = pickEducation(role, level, rng)
            val traits = pickTraits(rng, count = if (rng.nextInt(0, 100) < 25) 2 else 1)
            val years = (rng.nextInt(0, 6 + tier)).coerceAtLeast(0)
            val baseSalary = profile.baseSalary * education.baseSalaryMultiplier
            val traitFactor = traits.fold(1.0) { acc, t -> acc * t.salaryModifier }
            val seniorityFactor = 1.0 + 0.04 * years
            val expected = baseSalary * traitFactor * seniorityFactor *
                rng.nextDouble(0.85, 1.20)
            val bonus = expected * rng.nextDouble(0.25, 0.85)

            JobApplicant(
                id = "app_${currentTick}_${System.nanoTime() % 1_000_000}_$idx",
                name = "${firstNames.random(rng)} ${lastNames.random(rng)}",
                age = (22 + years + rng.nextInt(0, 14)).coerceIn(18, 65),
                role = role,
                expectedSalary = (expected.toInt()).toDouble(),
                education = education,
                traits = traits,
                prevExperienceYears = years,
                askingBonus = (bonus.toInt()).toDouble(),
                portrait = portraits.random(rng),
                expiresAtTick = expiresAt
            )
        }
    }

    private fun pickRole(level: Int, tier: Int, rng: Random): EmployeeRole {
        // Roles disponibles según unlockLevel del catálogo
        val unlocked = RoleCatalog.byRole.values
            .filter { it.unlockLevel <= level && it.role != EmployeeRole.CXO }
            .map { it.role }
        if (unlocked.isEmpty()) return EmployeeRole.LABORER
        // Sesgo: si tier alto, sale más mando intermedio
        val weighted = if (tier >= 4) unlocked + listOf(
            EmployeeRole.MANAGER, EmployeeRole.SUPERVISOR, EmployeeRole.DIRECTOR
        ).filter { it in unlocked } else unlocked
        return weighted.random(rng)
    }

    private fun pickEducation(role: EmployeeRole, level: Int, rng: Random): Education {
        val pool = when (role) {
            EmployeeRole.SCIENTIST -> listOf(Education.BACHELORS, Education.MASTERS, Education.PHD)
            EmployeeRole.ENGINEER -> listOf(Education.HIGHSCHOOL, Education.BACHELORS, Education.MASTERS)
            EmployeeRole.LAWYER -> listOf(Education.BACHELORS, Education.MASTERS)
            EmployeeRole.DIRECTOR, EmployeeRole.EXECUTIVE_ASSISTANT ->
                listOf(Education.BACHELORS, Education.MASTERS, Education.MBA)
            EmployeeRole.LABORER, EmployeeRole.OPERATOR ->
                listOf(Education.NO_DEGREE, Education.HIGHSCHOOL)
            else -> listOf(Education.HIGHSCHOOL, Education.BACHELORS, Education.MASTERS)
        }
        return pool.random(rng)
    }

    private fun pickTraits(rng: Random, count: Int): List<EmployeeTrait> {
        val available = EmployeeTrait.all.toMutableList()
        val out = mutableListOf<EmployeeTrait>()
        repeat(count.coerceAtMost(available.size)) {
            val pick = available.random(rng)
            available.remove(pick)
            out += pick
        }
        return out
    }
}
