package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Catálogo de NPCs con voces distintas. Algunos te ofrecen misiones, otros
 * te causan problemas. Todos te recuerdan que un imperio se hace de
 * relaciones, no solo de cuentas.
 */
enum class NPCRole {
    MENTOR,
    RIVAL,
    ROMANTIC,
    FAMILY,
    EMPLOYEE_SPECIAL,
    JOURNALIST,
    BANKER,
    POLITICIAN,
    MOBSTER,
    CONSULTANT,
    INFLUENCER,
    OLD_FRIEND
}

/** Tonalidad con la que el NPC se dirige a ti. Solo informativa para la UI. */
enum class NPCTone { FORMAL, FRIENDLY, AGGRESSIVE, SARCASTIC, FLATTERING, COLD }

@Serializable
data class NPC(
    val id: String,
    val name: String,
    val role: NPCRole,
    val portrait: String,
    val personality: String,
    /** -100..100, sentimiento del NPC hacia ti (no al revés). */
    val alignment: Int = 0,
    val relevantTags: List<String> = emptyList(),
    val voiceTone: NPCTone = NPCTone.FRIENDLY,
    /** Descripción larga para la pantalla de contactos. */
    val bio: String = "",
    /** Una frase que lo define cuando aparece la primera vez. */
    val signatureLine: String = ""
)

/**
 * Estado serializable: relaciones del jugador con los NPC ya conocidos.
 */
@Serializable
data class NPCsState(
    val known: Map<String, NPCRelationship> = emptyMap()
)

@Serializable
data class NPCRelationship(
    val npcId: String,
    /** 0..100 — escala de amistad, no la misma cosa que `alignment`. */
    val friendshipLevel: Int = 0,
    val lastInteractionTick: Long = 0,
    val encounters: Int = 0,
    val gifted: Int = 0,
    /** Última frase memorable que te dijo. Se actualiza con cada encuentro. */
    val quote: String = ""
)

object NPCCatalog {

    val all: List<NPC> = listOf(

        NPC(
            id = "npc_marcial",
            name = "Don Marcial",
            role = NPCRole.MOBSTER,
            portrait = "🎩",
            personality = "Mafioso paternalista, viejo dinero, nuevas reglas.",
            voiceTone = NPCTone.FORMAL,
            relevantTags = listOf("mentor", "mafia", "empresario", "padrino"),
            bio = "Empresario \"clásico\" del puerto. Fuma cigarros que valen lo que tu coche. " +
                "Habla bajo, pero todos callan cuando habla.",
            signatureLine = "Hijo, los principios cuestan caro. Por eso pocos los compran."
        ),

        NPC(
            id = "npc_sofia",
            name = "Sofía Rovira",
            role = NPCRole.JOURNALIST,
            portrait = "📝",
            personality = "Periodista mordaz, idealista y con una agenda propia.",
            voiceTone = NPCTone.SARCASTIC,
            relevantTags = listOf("prensa", "romance", "destape"),
            bio = "Premio Ortega y Gasset. Pluma afilada y sonrisa desarmante. " +
                "Si te llama es porque ya sabe la respuesta.",
            signatureLine = "Si no me invita a un café, le invito yo a una portada."
        ),

        NPC(
            id = "npc_tio_beto",
            name = "Tío Beto",
            role = NPCRole.FAMILY,
            portrait = "🍷",
            personality = "Familiar entrañable y descontrolado. Se gasta la pensión en lotería.",
            voiceTone = NPCTone.FRIENDLY,
            relevantTags = listOf("familia", "préstamo", "alcohol"),
            bio = "Hermano de tu padre. Te quiere, pero también quiere que le pagues una ronda.",
            signatureLine = "Sobrino, si tienes pasta, ponme una caña. Si no, ponme dos."
        ),

        NPC(
            id = "npc_luaces",
            name = "Inspector Luaces",
            role = NPCRole.POLITICIAN,
            portrait = "🕵️",
            personality = "Inspector de Hacienda obsesionado con tus libros.",
            voiceTone = NPCTone.COLD,
            relevantTags = listOf("hacienda", "auditoría", "burocracia"),
            bio = "Funcionario de carrera. Conoce cada hueco del código tributario. " +
                "No tiene amigos. Tampoco quiere.",
            signatureLine = "Buenos días. Me han dicho que tiene usted creatividad. Vamos a comprobarlo."
        ),

        NPC(
            id = "npc_tristan",
            name = "Tristán Velasco",
            role = NPCRole.RIVAL,
            portrait = "🦊",
            personality = "Heredero rico, ambicioso y sin escrúpulos.",
            voiceTone = NPCTone.AGGRESSIVE,
            relevantTags = listOf("rival", "élite", "OPA"),
            bio = "Tercera generación de empresarios. No tiene hambre, pero tiene ego. " +
                "Su hobby es ganarle a los \"recién llegados\" como tú.",
            signatureLine = "Lo nuestro no es competencia. Es paciencia mientras te equivocas."
        ),

        NPC(
            id = "npc_bruja_mercado",
            name = "La Bruja del Mercado",
            role = NPCRole.CONSULTANT,
            portrait = "🔮",
            personality = "Anciana enigmática que predice movimientos bursátiles.",
            voiceTone = NPCTone.SARCASTIC,
            relevantTags = listOf("bolsa", "predicción", "secreto"),
            bio = "Vive en un piso del Casco Viejo lleno de gatos y pantallas Bloomberg. " +
                "Nadie sabe cómo lo sabe. Acierta el 70% de las veces.",
            signatureLine = "Te lo digo yo, hijo: las acciones bajan cuando lloran las gaviotas."
        ),

        NPC(
            id = "npc_pepe",
            name = "Pepe el Tabernero",
            role = NPCRole.OLD_FRIEND,
            portrait = "🍻",
            personality = "Amigo de toda la vida, lleno de chismes y consejos no pedidos.",
            voiceTone = NPCTone.FRIENDLY,
            relevantTags = listOf("rumor", "amistad", "barrio"),
            bio = "Lleva la taberna del barrio. Sabe quién se acuesta con quién y quién " +
                "debe a quién. Mejor red de inteligencia barata que tienes.",
            signatureLine = "Tú tranqui, que aquí me entero antes que la Guardia Civil."
        ),

        NPC(
            id = "npc_marina",
            name = "Marina Olarte",
            role = NPCRole.ROMANTIC,
            portrait = "🌹",
            personality = "Arquitecta brillante. Se enamora de proyectos, no de gente.",
            voiceTone = NPCTone.FORMAL,
            relevantTags = listOf("romance", "arquitectura", "talento"),
            bio = "Estudios en Yale, oficina propia, lista de espera de tres años. " +
                "Quiere construir un edificio inolvidable. Tú, también.",
            signatureLine = "Si no construyes legado, ¿para qué levantas paredes?"
        ),

        NPC(
            id = "npc_caco",
            name = "Caco \"el Listo\"",
            role = NPCRole.MOBSTER,
            portrait = "🎲",
            personality = "Truhan de poca monta con conexiones de mucha.",
            voiceTone = NPCTone.SARCASTIC,
            relevantTags = listOf("mafia", "estraperlo", "información"),
            bio = "Con un brazo siempre en cabestrillo, frase siempre en guardia. " +
                "Vende información y compra silencios.",
            signatureLine = "¿Tú quieres saber, o quieres no saber pero por si acaso?"
        ),

        NPC(
            id = "npc_garrido",
            name = "Doña Carmen Garrido",
            role = NPCRole.BANKER,
            portrait = "💼",
            personality = "Banquera de hierro. Sonríe cuando aprieta.",
            voiceTone = NPCTone.FORMAL,
            relevantTags = listOf("banco", "préstamo", "interés"),
            bio = "Directora regional. Se sabe el balance de tu empresa mejor que tú.",
            signatureLine = "Dinero presto a quien lo merece. Lo cobro a quien me lo pide."
        ),

        NPC(
            id = "npc_kira",
            name = "Kira Saldaña",
            role = NPCRole.INFLUENCER,
            portrait = "📱",
            personality = "Influencer de 2.4M de seguidores. Frívola por fuera, calculadora por dentro.",
            voiceTone = NPCTone.FLATTERING,
            relevantTags = listOf("redes", "marketing", "imagen"),
            bio = "Una historia suya puede subir tus acciones un 5% o hundirlas un 12%. " +
                "Cobra en metálico y en menciones.",
            signatureLine = "Te hago viral, bonito, pero la viralidad también tiene factura."
        ),

        NPC(
            id = "npc_nestor",
            name = "Néstor el Becario",
            role = NPCRole.EMPLOYEE_SPECIAL,
            portrait = "🤓",
            personality = "Becario brillante, terriblemente leal, peligrosamente honesto.",
            voiceTone = NPCTone.FRIENDLY,
            relevantTags = listOf("plantilla", "fidelidad", "futuro"),
            bio = "Llegó por una práctica de tres meses. Lleva tres años. Te conoce mejor que tu sombra.",
            signatureLine = "Jefa/jefe, me lo aprendí todo de memoria, por si acaso."
        ),

        NPC(
            id = "npc_kowalski",
            name = "Mira Kowalski",
            role = NPCRole.CONSULTANT,
            portrait = "📊",
            personality = "Consultora estratégica polaca. Habla en bullet points.",
            voiceTone = NPCTone.COLD,
            relevantTags = listOf("estrategia", "expansión", "datos"),
            bio = "Big Four. Cobra 1.500 € la hora y los aprovecha al segundo.",
            signatureLine = "Tres opciones. Una es la correcta. Las otras dos son carísimas."
        ),

        NPC(
            id = "npc_diputado",
            name = "El Diputado Cienfuegos",
            role = NPCRole.POLITICIAN,
            portrait = "🏛️",
            personality = "Político del color que toque. Buen apretón de manos, mejor memoria de favores.",
            voiceTone = NPCTone.FLATTERING,
            relevantTags = listOf("política", "favores", "subvención"),
            bio = "Ofrece licencias rápidas a cambio de \"un detalle\". Reza mientras lo dice.",
            signatureLine = "Aquí, entre amigos, se solucionan las cosas. Y si no somos amigos… nos hacemos."
        ),

        NPC(
            id = "npc_abuela",
            name = "Abuela Rosario",
            role = NPCRole.FAMILY,
            portrait = "👵",
            personality = "Tu abuela. Sabia, severa, llena de refranes.",
            voiceTone = NPCTone.FRIENDLY,
            relevantTags = listOf("familia", "tradición", "consejo"),
            bio = "Te crió. Sabe lo que vales antes que tú. Y te lo recuerda.",
            signatureLine = "Cría cuervos, hijo, pero échales de comer aceitunas."
        ),

        NPC(
            id = "npc_lia",
            name = "Lía la Hacker",
            role = NPCRole.EMPLOYEE_SPECIAL,
            portrait = "💻",
            personality = "Hacker reformada. Quizá. Te debe un favor, quizá tú a ella.",
            voiceTone = NPCTone.SARCASTIC,
            relevantTags = listOf("ciberseguridad", "secreto", "talento"),
            bio = "Encapuchada en sudaderas, ojos rojos por las pantallas. Cobra en cripto.",
            signatureLine = "Jefe, todo está cifrado. Hasta tus problemas."
        )
    )

    private val byId = all.associateBy { it.id }
    fun byId(id: String): NPC? = byId[id]
    fun byRole(role: NPCRole): List<NPC> = all.filter { it.role == role }
}
