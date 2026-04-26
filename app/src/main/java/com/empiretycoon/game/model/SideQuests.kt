package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Misiones secundarias — más narrativas y volátiles que las `Quest`s
 * principales. Las da un NPC, expiran tras N días y tienen variedad de
 * objetivos. No reemplazan a `Quest`, conviven.
 */
enum class SideQuestCategory {
    TUTORIAL, ECONOMIC, SOCIAL, EXPLORATION, ROMANCE, RIVAL, FAMILY, CHARITY, CRIMINAL, SPECIAL
}

enum class QuestDifficulty(val displayName: String, val emoji: String, val multiplier: Double) {
    EASY("Fácil", "🟢", 1.0),
    MEDIUM("Media", "🟡", 1.5),
    HARD("Difícil", "🟠", 2.2),
    LEGENDARY("Legendaria", "🔴", 3.5)
}

/**
 * Tipos de objetivo. Sealed para tipado exhaustivo en el motor.
 */
@Serializable
sealed class QuestObjective {
    @Serializable data class ProduceX(val resourceId: String, val qty: Int) : QuestObjective()
    @Serializable data class SellAtPriceAbove(val resourceId: String, val price: Double, val qty: Int) : QuestObjective()
    @Serializable data class AccumulateCash(val amount: Double) : QuestObjective()
    @Serializable data class HireRole(val role: String) : QuestObjective()
    @Serializable data class ReachLevel(val lvl: Int) : QuestObjective()
    @Serializable data class CompleteContracts(val n: Int) : QuestObjective()
    @Serializable data class ResearchTech(val id: String) : QuestObjective()
    @Serializable data class DonateToCharity(val amount: Double) : QuestObjective()
    @Serializable data class DefeatRival(val rivalId: String) : QuestObjective()
    @Serializable data class VisitLocation(val locId: String) : QuestObjective()
    @Serializable data class PassDays(val n: Int) : QuestObjective()
}

@Serializable
data class QuestReward(
    val cash: Double = 0.0,
    val xp: Long = 0,
    val reputation: Int = 0,
    val items: Map<String, Int> = emptyMap(),
    val unlockedNpcId: String? = null,
    val karmaDelta: Int = 0
)

@Serializable
data class SideQuest(
    val id: String,
    val title: String,
    val description: String,
    val giverNpcId: String,
    val objective: QuestObjective,
    val reward: QuestReward,
    val difficulty: QuestDifficulty,
    /** Días in-game para completarla desde que se acepta. */
    val expirationDays: Int = 5,
    val category: SideQuestCategory,
    /** Tick en que se aceptó (lo rellena el motor). */
    val acceptedAtTick: Long = 0,
    /** Día in-game límite (lo rellena el motor al aceptar). */
    val deadlineDay: Int = 0,
    /** Snapshot del estado al aceptar para medir progreso relativo. */
    val baselineCash: Double = 0.0,
    val baselineDay: Int = 0,
    val baselineSold: Map<String, Int> = emptyMap()
)

@Serializable
data class SideQuestsState(
    val active: List<SideQuest> = emptyList(),
    val available: List<SideQuest> = emptyList(),
    val completed: Set<String> = emptySet(),
    val failed: Set<String> = emptySet(),
    /** Día in-game en que se hizo el último refresh para no saturar. */
    val lastRefreshDay: Int = 0
)

object SideQuestCatalog {

    val all: List<SideQuest> = listOf(

        // ============ TUTORIAL ============
        SideQuest(
            id = "sq_first_steps",
            title = "Primeros pasos del jefe",
            description = "Tu abuela quiere ver con sus propios ojos que tienes oficio. " +
                "\"Hijo, hazme ver que esto va en serio\".",
            giverNpcId = "npc_abuela",
            objective = QuestObjective.AccumulateCash(15_000.0),
            reward = QuestReward(cash = 1_500.0, xp = 200, reputation = 2, karmaDelta = +2),
            difficulty = QuestDifficulty.EASY,
            expirationDays = 7,
            category = SideQuestCategory.TUTORIAL
        ),
        SideQuest(
            id = "sq_first_employee",
            title = "Una nómina, un futuro",
            description = "Pepe te pide que contrates a su sobrina, recién licenciada en ADE.",
            giverNpcId = "npc_pepe",
            objective = QuestObjective.HireRole("any"),
            reward = QuestReward(cash = 800.0, xp = 150, reputation = 3),
            difficulty = QuestDifficulty.EASY,
            expirationDays = 4,
            category = SideQuestCategory.TUTORIAL
        ),

        // ============ ECONÓMICAS ============
        SideQuest(
            id = "sq_primo_emprendedor",
            title = "El primo emprendedor",
            description = "Tu primo Aitor jura que tiene \"la app que va a romper el mercado\". " +
                "Necesita 5.000 €. Te promete ROI en 30 días. Spoiler: te promete.",
            giverNpcId = "npc_tio_beto",
            objective = QuestObjective.PassDays(15),
            reward = QuestReward(cash = 7_500.0, xp = 300, reputation = 1, karmaDelta = +3),
            difficulty = QuestDifficulty.MEDIUM,
            expirationDays = 18,
            category = SideQuestCategory.FAMILY
        ),
        SideQuest(
            id = "sq_acreedor",
            title = "El acreedor",
            description = "Un viejo conocido aparece reclamando una deuda \"de cuando éramos críos\". " +
                "Mejor pagar y olvidar… ¿o investigar?",
            giverNpcId = "npc_caco",
            objective = QuestObjective.DonateToCharity(2_500.0),
            reward = QuestReward(cash = 0.0, xp = 250, reputation = -2, karmaDelta = -3),
            difficulty = QuestDifficulty.MEDIUM,
            expirationDays = 5,
            category = SideQuestCategory.CRIMINAL
        ),
        SideQuest(
            id = "sq_panaderia_local",
            title = "La panadería de la esquina",
            description = "La señora del pan de tu barrio quiere modernizar el horno. " +
                "Te pide 50 panes a precio justo de un golpe.",
            giverNpcId = "npc_pepe",
            objective = QuestObjective.SellAtPriceAbove("bread", 18.0, 50),
            reward = QuestReward(cash = 1_200.0, xp = 220, reputation = 5, karmaDelta = +4),
            difficulty = QuestDifficulty.EASY,
            expirationDays = 6,
            category = SideQuestCategory.SOCIAL
        ),
        SideQuest(
            id = "sq_pedido_grande",
            title = "Pedido gordo",
            description = "Una constructora local necesita 200 tablones la semana que viene. " +
                "Si llegas, abren la puerta a más.",
            giverNpcId = "npc_kowalski",
            objective = QuestObjective.ProduceX("plank", 200),
            reward = QuestReward(cash = 6_500.0, xp = 600, reputation = 8),
            difficulty = QuestDifficulty.MEDIUM,
            expirationDays = 8,
            category = SideQuestCategory.ECONOMIC
        ),
        SideQuest(
            id = "sq_acero_internacional",
            title = "El contrato alemán",
            description = "Una multinacional te pide 500 unidades de acero a precio premium. Demuestra que juegas en otra liga.",
            giverNpcId = "npc_kowalski",
            objective = QuestObjective.SellAtPriceAbove("steel", 70.0, 500),
            reward = QuestReward(cash = 35_000.0, xp = 1_500, reputation = 12),
            difficulty = QuestDifficulty.HARD,
            expirationDays = 12,
            category = SideQuestCategory.ECONOMIC
        ),

        // ============ ROMANCE ============
        SideQuest(
            id = "sq_cita_a_ciegas",
            title = "Cita a ciegas",
            description = "Marina Olarte acepta una cita pero te pide que vayas \"sin móvil ni guardaespaldas\". " +
                "La energía y la conversación serán tu único activo.",
            giverNpcId = "npc_marina",
            objective = QuestObjective.PassDays(2),
            reward = QuestReward(cash = 0.0, xp = 400, reputation = 4, karmaDelta = +5,
                unlockedNpcId = "npc_marina"),
            difficulty = QuestDifficulty.EASY,
            expirationDays = 3,
            category = SideQuestCategory.ROMANCE
        ),
        SideQuest(
            id = "sq_cazafortunas",
            title = "Cazafortunas",
            description = "Una persona muy guapa, muy joven y muy interesada en tu balance " +
                "te invita a cenar. Tu cabeza dice \"no\". Tu vanidad murmura \"a ver…\".",
            giverNpcId = "npc_caco",
            objective = QuestObjective.AccumulateCash(500_000.0),
            reward = QuestReward(cash = -50_000.0, xp = 800, reputation = -5, karmaDelta = -8),
            difficulty = QuestDifficulty.MEDIUM,
            expirationDays = 10,
            category = SideQuestCategory.ROMANCE
        ),
        SideQuest(
            id = "sq_carta_marina",
            title = "Una carta manuscrita",
            description = "Marina te deja una carta sobre el escritorio: si entregas un encargo " +
                "especial de muebles a tiempo, hay velada larga.",
            giverNpcId = "npc_marina",
            objective = QuestObjective.ProduceX("furniture", 30),
            reward = QuestReward(cash = 5_500.0, xp = 600, reputation = 6, karmaDelta = +6),
            difficulty = QuestDifficulty.MEDIUM,
            expirationDays = 7,
            category = SideQuestCategory.ROMANCE
        ),

        // ============ SOCIAL / CHARITY ============
        SideQuest(
            id = "sq_mecenas_arte",
            title = "Mecenas del arte",
            description = "El Museo de Bellas Artes pide patrocinio para una exposición. " +
                "Tu nombre, en bronce, sobre la entrada. La eternidad cuesta poco hoy.",
            giverNpcId = "npc_diputado",
            objective = QuestObjective.DonateToCharity(15_000.0),
            reward = QuestReward(cash = 0.0, xp = 800, reputation = 18, karmaDelta = +12),
            difficulty = QuestDifficulty.MEDIUM,
            expirationDays = 8,
            category = SideQuestCategory.CHARITY
        ),
        SideQuest(
            id = "sq_festival_pueblo",
            title = "Festival del pueblo",
            description = "Tu pueblo te pide patrocinar las fiestas de agosto. Banderitas con tu logo, " +
                "verbena con DJ del barrio.",
            giverNpcId = "npc_pepe",
            objective = QuestObjective.DonateToCharity(3_000.0),
            reward = QuestReward(cash = 0.0, xp = 350, reputation = 12, karmaDelta = +6),
            difficulty = QuestDifficulty.EASY,
            expirationDays = 5,
            category = SideQuestCategory.CHARITY
        ),
        SideQuest(
            id = "sq_comedor_social",
            title = "Comedor social",
            description = "Una ONG necesita material para abrir un comedor en el barrio sur. " +
                "Tú decides cuánto pesa tu firma.",
            giverNpcId = "npc_abuela",
            objective = QuestObjective.DonateToCharity(8_000.0),
            reward = QuestReward(cash = 0.0, xp = 600, reputation = 10, karmaDelta = +15),
            difficulty = QuestDifficulty.MEDIUM,
            expirationDays = 10,
            category = SideQuestCategory.CHARITY
        ),

        // ============ RIVAL / CRIMINAL ============
        SideQuest(
            id = "sq_chivato",
            title = "El chivato",
            description = "Caco \"el Listo\" te ofrece información confidencial sobre Tristán a cambio de un sobre.",
            giverNpcId = "npc_caco",
            objective = QuestObjective.DonateToCharity(4_000.0),
            reward = QuestReward(cash = 12_000.0, xp = 700, reputation = -3, karmaDelta = -10),
            difficulty = QuestDifficulty.MEDIUM,
            expirationDays = 6,
            category = SideQuestCategory.RIVAL
        ),
        SideQuest(
            id = "sq_periodista_molesto",
            title = "El periodista molesto",
            description = "Un periodista mediocre amenaza con publicar mentiras. Pide \"compensación moral\".",
            giverNpcId = "npc_sofia",
            objective = QuestObjective.AccumulateCash(75_000.0),
            reward = QuestReward(cash = -20_000.0, xp = 500, reputation = -2, karmaDelta = -8),
            difficulty = QuestDifficulty.MEDIUM,
            expirationDays = 5,
            category = SideQuestCategory.RIVAL
        ),
        SideQuest(
            id = "sq_sabotaje_rival",
            title = "Sabotaje cruzado",
            description = "Tristán está saboteando entregas. Caco propone devolverla con creces.",
            giverNpcId = "npc_caco",
            objective = QuestObjective.DefeatRival("npc_tristan"),
            reward = QuestReward(cash = 25_000.0, xp = 1_500, reputation = -10, karmaDelta = -25),
            difficulty = QuestDifficulty.HARD,
            expirationDays = 12,
            category = SideQuestCategory.CRIMINAL
        ),
        SideQuest(
            id = "sq_oferta_marcial",
            title = "Una oferta de Don Marcial",
            description = "El viejo te invita a cenar. Te ofrece \"protección\" del puerto. " +
                "No es una pregunta.",
            giverNpcId = "npc_marcial",
            objective = QuestObjective.AccumulateCash(200_000.0),
            reward = QuestReward(cash = 50_000.0, xp = 2_000, reputation = 8, karmaDelta = -15),
            difficulty = QuestDifficulty.HARD,
            expirationDays = 10,
            category = SideQuestCategory.CRIMINAL
        ),

        // ============ EXPLORATION / SPECIAL ============
        SideQuest(
            id = "sq_bruja_acciones",
            title = "La predicción de la Bruja",
            description = "La Bruja del Mercado dice que ALPHA va a subir un 18% en tres días. " +
                "Si compras y aciertas, te lleva a tomar tila a su piso.",
            giverNpcId = "npc_bruja_mercado",
            objective = QuestObjective.PassDays(3),
            reward = QuestReward(cash = 8_000.0, xp = 700, reputation = 2),
            difficulty = QuestDifficulty.MEDIUM,
            expirationDays = 4,
            category = SideQuestCategory.SPECIAL
        ),
        SideQuest(
            id = "sq_visita_mina",
            title = "Visita a la mina",
            description = "Una explotación minera abandonada al norte. Cuentan que aún hay carbón. " +
                "Y otras cosas.",
            giverNpcId = "npc_kowalski",
            objective = QuestObjective.VisitLocation("mina_norte"),
            reward = QuestReward(cash = 4_000.0, xp = 400, reputation = 3,
                items = mapOf("coal" to 30)),
            difficulty = QuestDifficulty.EASY,
            expirationDays = 7,
            category = SideQuestCategory.EXPLORATION
        ),
        SideQuest(
            id = "sq_iniciacion_lia",
            title = "Una hacker en plantilla",
            description = "Lía propone una prueba: si llegas a 25 empleados, viene contigo de cabeza.",
            giverNpcId = "npc_lia",
            objective = QuestObjective.HireRole("any_25"),
            reward = QuestReward(cash = 0.0, xp = 1_200, reputation = 8,
                unlockedNpcId = "npc_lia"),
            difficulty = QuestDifficulty.HARD,
            expirationDays = 20,
            category = SideQuestCategory.SPECIAL
        ),

        // ============ ECONÓMICAS AVANZADAS ============
        SideQuest(
            id = "sq_smartphones_navidad",
            title = "Campaña navideña",
            description = "Una cadena pide 60 smartphones antes de Navidad. Si fallas, no vuelven.",
            giverNpcId = "npc_kowalski",
            objective = QuestObjective.ProduceX("smartphone", 60),
            reward = QuestReward(cash = 28_000.0, xp = 1_400, reputation = 14),
            difficulty = QuestDifficulty.HARD,
            expirationDays = 15,
            category = SideQuestCategory.ECONOMIC
        ),
        SideQuest(
            id = "sq_yate_cliente",
            title = "El cliente del yate",
            description = "Un magnate pide un yate exclusivo. Solo uno. A precio premium.",
            giverNpcId = "npc_marcial",
            objective = QuestObjective.SellAtPriceAbove("yacht", 32_000.0, 1),
            reward = QuestReward(cash = 50_000.0, xp = 3_000, reputation = 18, karmaDelta = +2),
            difficulty = QuestDifficulty.LEGENDARY,
            expirationDays = 25,
            category = SideQuestCategory.ECONOMIC
        ),
        SideQuest(
            id = "sq_joyas_boda",
            title = "Pedido de boda",
            description = "La boda del año en la ciudad. Joyas para todo el séquito de la novia.",
            giverNpcId = "npc_kira",
            objective = QuestObjective.SellAtPriceAbove("jewelry", 1_900.0, 12),
            reward = QuestReward(cash = 15_000.0, xp = 900, reputation = 9, karmaDelta = +3),
            difficulty = QuestDifficulty.HARD,
            expirationDays = 9,
            category = SideQuestCategory.SOCIAL
        ),

        // ============ POLITICAL / BUREAUCRACY ============
        SideQuest(
            id = "sq_subvencion",
            title = "La subvención \"sencilla\"",
            description = "El diputado Cienfuegos sugiere que con \"un detalle\" cae una subvención de 100k.",
            giverNpcId = "npc_diputado",
            objective = QuestObjective.AccumulateCash(50_000.0),
            reward = QuestReward(cash = 100_000.0, xp = 1_200, reputation = -2, karmaDelta = -18),
            difficulty = QuestDifficulty.HARD,
            expirationDays = 10,
            category = SideQuestCategory.CRIMINAL
        ),
        SideQuest(
            id = "sq_inspeccion",
            title = "Inspección de Hacienda",
            description = "El Inspector Luaces tiene tu carpeta sobre la mesa. Sobrevive a la auditoría.",
            giverNpcId = "npc_luaces",
            objective = QuestObjective.PassDays(7),
            reward = QuestReward(cash = 0.0, xp = 1_000, reputation = 6, karmaDelta = +8),
            difficulty = QuestDifficulty.MEDIUM,
            expirationDays = 8,
            category = SideQuestCategory.SPECIAL
        ),

        // ============ ROMANCE EXTRA ============
        SideQuest(
            id = "sq_columna_sofia",
            title = "Columna en El Diario",
            description = "Sofía te pide entrevista profunda y honesta. Sin maquillaje.",
            giverNpcId = "npc_sofia",
            objective = QuestObjective.PassDays(2),
            reward = QuestReward(cash = 0.0, xp = 600, reputation = 12, karmaDelta = +10),
            difficulty = QuestDifficulty.EASY,
            expirationDays = 3,
            category = SideQuestCategory.ROMANCE
        ),

        // ============ INFLUENCER ============
        SideQuest(
            id = "sq_kira_collab",
            title = "Colaboración con Kira",
            description = "Kira propone una colab pagada. Cobra 10k pero te empuja al millón de impresiones.",
            giverNpcId = "npc_kira",
            objective = QuestObjective.AccumulateCash(20_000.0),
            reward = QuestReward(cash = -10_000.0, xp = 800, reputation = 15),
            difficulty = QuestDifficulty.MEDIUM,
            expirationDays = 6,
            category = SideQuestCategory.SOCIAL
        ),

        // ============ FAMILIA ============
        SideQuest(
            id = "sq_tio_beto_quiniela",
            title = "La quiniela del Tío Beto",
            description = "Tu tío jura que esta vez es la buena. Necesita 500 € \"en confianza\".",
            giverNpcId = "npc_tio_beto",
            objective = QuestObjective.AccumulateCash(2_500.0),
            reward = QuestReward(cash = 1_200.0, xp = 200, reputation = 1, karmaDelta = +1),
            difficulty = QuestDifficulty.EASY,
            expirationDays = 4,
            category = SideQuestCategory.FAMILY
        ),
        SideQuest(
            id = "sq_aniv_abuela",
            title = "Aniversario de la abuela",
            description = "Abuela Rosario cumple 80 años. Quiere fiesta familiar y un regalo de los grandes.",
            giverNpcId = "npc_abuela",
            objective = QuestObjective.DonateToCharity(5_000.0),
            reward = QuestReward(cash = 0.0, xp = 700, reputation = 8, karmaDelta = +20),
            difficulty = QuestDifficulty.EASY,
            expirationDays = 5,
            category = SideQuestCategory.FAMILY
        ),

        // ============ NEGOCIO / ESPECIALES ============
        SideQuest(
            id = "sq_software_house",
            title = "Software a medida",
            description = "Una scale-up necesita 20 unidades de software de gestión. Plazos imposibles.",
            giverNpcId = "npc_lia",
            objective = QuestObjective.SellAtPriceAbove("software", 1_100.0, 20),
            reward = QuestReward(cash = 22_000.0, xp = 1_400, reputation = 11),
            difficulty = QuestDifficulty.HARD,
            expirationDays = 10,
            category = SideQuestCategory.ECONOMIC
        ),
        SideQuest(
            id = "sq_circuitos",
            title = "Circuitos para la NASA imitación",
            description = "Una empresa aeroespacial pide 150 circuitos certificados. Reputación garantizada.",
            giverNpcId = "npc_kowalski",
            objective = QuestObjective.ProduceX("circuit", 150),
            reward = QuestReward(cash = 28_000.0, xp = 2_000, reputation = 18, karmaDelta = +3),
            difficulty = QuestDifficulty.LEGENDARY,
            expirationDays = 18,
            category = SideQuestCategory.ECONOMIC
        ),
        SideQuest(
            id = "sq_rep_50",
            title = "El club de los reputados",
            description = "Marcial te promete una invitación al \"Club\" si superas 50 de reputación.",
            giverNpcId = "npc_marcial",
            objective = QuestObjective.AccumulateCash(1.0),
            reward = QuestReward(cash = 5_000.0, xp = 1_000, reputation = 5, karmaDelta = +5),
            difficulty = QuestDifficulty.MEDIUM,
            expirationDays = 14,
            category = SideQuestCategory.SPECIAL
        ),
        SideQuest(
            id = "sq_torre_marina",
            title = "La Torre Olarte",
            description = "Marina diseña un rascacielos. Necesita inversión seria. Y discreción.",
            giverNpcId = "npc_marina",
            objective = QuestObjective.AccumulateCash(2_000_000.0),
            reward = QuestReward(cash = 0.0, xp = 5_000, reputation = 25, karmaDelta = +10),
            difficulty = QuestDifficulty.LEGENDARY,
            expirationDays = 35,
            category = SideQuestCategory.ROMANCE
        ),
        SideQuest(
            id = "sq_lvl15",
            title = "Cumpliendo años",
            description = "Pepe te apuesta un ron en el bar a que no llegas al nivel 15 antes de fin de mes.",
            giverNpcId = "npc_pepe",
            objective = QuestObjective.ReachLevel(15),
            reward = QuestReward(cash = 3_000.0, xp = 800, reputation = 4, karmaDelta = +2),
            difficulty = QuestDifficulty.MEDIUM,
            expirationDays = 25,
            category = SideQuestCategory.SOCIAL
        )
    )

    private val byId = all.associateBy { it.id }
    fun byId(id: String): SideQuest? = byId[id]
    fun byCategory(c: SideQuestCategory): List<SideQuest> = all.filter { it.category == c }
}
