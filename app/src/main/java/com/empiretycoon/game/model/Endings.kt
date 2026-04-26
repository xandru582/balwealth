package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * 11 finales — se desbloquean al completar el capítulo 12 cumpliendo los
 * requisitos de karma, días, logros y patrón de elecciones. Si varios
 * encajan, gana el primero del catálogo (orden = prioridad).
 */
enum class EndingType {
    TYRANT,
    PHILANTROPIST,
    BURNOUT,
    RECLUSE,
    REVOLUTIONARY,
    JESTER,
    PRISONER,
    EXILED,
    SAINT,
    IMMORTAL,
    SECRET
}

@Serializable
data class EndingRequirement(
    val minDays: Int = 180,
    /** Karma del jugador. Inclusivo en ambos extremos. */
    val karmaMin: Int = -100,
    val karmaMax: Int = 100,
    /**
     * Patrón de elecciones requerido. Cada string debe ser
     * "chapter_id:choiceIndex". Si la lista es no nula, todas deben
     * coincidir; si es nula, no se restringe.
     */
    val choicesPattern: List<String>? = null,
    val achievementsRequired: List<String> = emptyList(),
    /** Reputación corporativa requerida. */
    val minReputation: Int = 0
)

@Serializable
data class Ending(
    val type: EndingType,
    val title: String,
    val narrativeText: String,
    val requirements: EndingRequirement,
    val illustrationEmoji: String,
    /** Frase corta para compartir en redes. */
    val shareLine: String
)

object EndingCatalog {

    val all: List<Ending> = listOf(

        // ---------- SAINT ----------
        Ending(
            type = EndingType.SAINT,
            title = "El Santo del Mercado",
            illustrationEmoji = "😇",
            narrativeText = "Cierras la sede una mañana de mayo y caminas hacia el comedor " +
                "social que tu fundación abrió hace tres años. Te reconocen. Te abrazan. " +
                "Una niña te tira del abrigo y te enseña un dibujo: tu cara, sonriendo, " +
                "rodeada de manos. Hay gente que muere rica. Tú elegiste morir necesario.",
            requirements = EndingRequirement(
                minDays = 180,
                karmaMin = 70,
                karmaMax = 100,
                minReputation = 80
            ),
            shareLine = "He alcanzado el final SANTO en Empire Tycoon. Karma máximo, legado eterno."
        ),

        // ---------- PHILANTROPIST ----------
        Ending(
            type = EndingType.PHILANTROPIST,
            title = "El Filántropo",
            illustrationEmoji = "🤝",
            narrativeText = "La fundación lleva tu nombre. Diez hospitales, tres universidades, " +
                "una red de comedores en barrios olvidados. Caminas sin escolta. Saludas " +
                "al portero por su nombre. Tu cuenta corriente es modesta para tu tamaño. " +
                "Tu nombre será leído mucho después de tu muerte.",
            requirements = EndingRequirement(
                minDays = 180,
                karmaMin = 40,
                karmaMax = 100,
                minReputation = 60
            ),
            shareLine = "Final FILÁNTROPO en Empire Tycoon. El imperio que dejé pesa más por sus huellas."
        ),

        // ---------- REVOLUTIONARY ----------
        Ending(
            type = EndingType.REVOLUTIONARY,
            title = "El Revolucionario",
            illustrationEmoji = "✊",
            narrativeText = "Pasaste tus acciones a un fondo cooperativo. La empresa, ahora " +
                "propiedad de sus trabajadores, prospera. Te invitan al aniversario y " +
                "subes al escenario sin discurso preparado: \"Dejé de ser dueño para ser " +
                "padre\". Aplauden. Y nadie sabe que aún cobras un sueldo simbólico, " +
                "porque era tu manera de no soltar del todo.",
            requirements = EndingRequirement(
                minDays = 200,
                karmaMin = 50,
                karmaMax = 100,
                choicesPattern = listOf("ch_ipo:2", "ch_charity_or_monopoly:0")
            ),
            shareLine = "Final REVOLUCIONARIO. Repartí el imperio antes que el imperio me repartiera a mí."
        ),

        // ---------- IMMORTAL ----------
        Ending(
            type = EndingType.IMMORTAL,
            title = "El Inmortal",
            illustrationEmoji = "👑",
            narrativeText = "Tu nombre es una marca, una calle, un edificio en cada capital. " +
                "Cuando te mueras, los chavales seguirán ofendiéndose por las cosas que " +
                "dijiste y peleándose por las que hiciste. Has trascendido. Has fracasado " +
                "en pasar desapercibido. Has triunfado en todo lo demás.",
            requirements = EndingRequirement(
                minDays = 250,
                karmaMin = -20,
                karmaMax = 60,
                minReputation = 90,
                achievementsRequired = listOf("ach_cash_1b", "ach_rep_max")
            ),
            shareLine = "Final INMORTAL en Empire Tycoon. Mi nombre se grabará. Tu opinión, no."
        ),

        // ---------- TYRANT ----------
        Ending(
            type = EndingType.TYRANT,
            title = "El Tirano",
            illustrationEmoji = "👹",
            narrativeText = "Tu imperio cubre cinco continentes. Tienes a la prensa, a la política " +
                "y al cártel del puerto. Despiertas en un palacete con cámaras donde otros " +
                "tienen ventanas. Tu hija no quiere hablar contigo. Tu plantilla te llama " +
                "\"el faraón\" en privado. Has ganado. La pregunta es qué.",
            requirements = EndingRequirement(
                minDays = 180,
                karmaMin = -100,
                karmaMax = -40,
                minReputation = 50
            ),
            shareLine = "Final TIRANO. He construido un imperio que da más miedo del que sufrí."
        ),

        // ---------- PRISONER ----------
        Ending(
            type = EndingType.PRISONER,
            title = "El Preso de Lujo",
            illustrationEmoji = "⛓️",
            narrativeText = "El Inspector Luaces, el destino, y un dossier de doscientas páginas. " +
                "Estás en una cárcel de mínima seguridad, con tu propia celda, gimnasio y " +
                "biblioteca. Sigues firmando contratos por la mañana y haciendo yoga por " +
                "la tarde. La libertad es relativa cuando tu cuenta es absoluta.",
            requirements = EndingRequirement(
                minDays = 150,
                karmaMin = -100,
                karmaMax = -50,
                achievementsRequired = listOf("ach_hidden_bankrupt")
            ),
            shareLine = "Final PRESO en Empire Tycoon. El imperio cae. La conciencia, no."
        ),

        // ---------- BURNOUT ----------
        Ending(
            type = EndingType.BURNOUT,
            title = "El Quemado",
            illustrationEmoji = "🔥",
            narrativeText = "Te despiertas un martes y no puedes salir de la cama. Tu psiquiatra " +
                "te firma una baja indefinida. Vendes la mayoría a precio de saldo. " +
                "Te mudas a una casa pequeña en la sierra. La empresa sigue, sin ti. Tú " +
                "también, por fin, aprendes a seguir sin ella.",
            requirements = EndingRequirement(
                minDays = 150,
                karmaMin = -30,
                karmaMax = 40
            ),
            shareLine = "Final QUEMADO. Construí un imperio. Me costó la salud. Aprendí tarde."
        ),

        // ---------- RECLUSE ----------
        Ending(
            type = EndingType.RECLUSE,
            title = "El Recluso del Faro",
            illustrationEmoji = "🌅",
            narrativeText = "Compras un faro en la costa norte. Cierras tus redes. Despides " +
                "al asistente. Tu correo electrónico se llena pero ya nadie te encuentra. " +
                "Lees, caminas con perros, escribes un libro que nunca publicarás. " +
                "Has aprendido lo difícil: no todo pide respuesta.",
            requirements = EndingRequirement(
                minDays = 200,
                karmaMin = 0,
                karmaMax = 60,
                choicesPattern = listOf("ch_legacy:2")
            ),
            shareLine = "Final RECLUSO. He desaparecido del mapa. Por fin estoy en mí."
        ),

        // ---------- EXILED ----------
        Ending(
            type = EndingType.EXILED,
            title = "El Exiliado",
            illustrationEmoji = "✈️",
            narrativeText = "Aterrizas en Dubái con tres maletas y un par de cuentas en el Caribe. " +
                "La justicia internacional te busca con fines decorativos. Cenas con jeques, " +
                "vendes consultoría a regímenes que nunca te firmarán contratos por escrito. " +
                "Cada noche, en el balcón, miras hacia el oeste y no sabes si lloras o suspiras.",
            requirements = EndingRequirement(
                minDays = 170,
                karmaMin = -80,
                karmaMax = -30
            ),
            shareLine = "Final EXILIADO. Mi imperio cabía en una maleta. La culpa, no entró."
        ),

        // ---------- JESTER ----------
        Ending(
            type = EndingType.JESTER,
            title = "El Bufón",
            illustrationEmoji = "🤡",
            narrativeText = "Tus excentricidades son legendarias. Compras un equipo de fútbol, " +
                "un país pequeño, una flota de globos aerostáticos. Sales en programas " +
                "de entretenimiento. Tus accionistas te toleran porque ganas más que avergüenzas. " +
                "Eres entretenimiento puro. Y entretenido.",
            requirements = EndingRequirement(
                minDays = 180,
                karmaMin = -20,
                karmaMax = 40,
                minReputation = 30,
                achievementsRequired = listOf("ach_yacht_owner")
            ),
            shareLine = "Final BUFÓN. He construido un imperio. Y un circo. Y los dos siguen abiertos."
        ),

        // ---------- SECRET ----------
        Ending(
            type = EndingType.SECRET,
            title = "El Final Oculto",
            illustrationEmoji = "🌌",
            narrativeText = "Una mañana cualquiera, sales del despacho a por café y no vuelves. " +
                "Las cámaras te pierden en la calle. La policía abre un caso, lo cierra, " +
                "lo reabre. Diez años después, en Bali, un periodista cree haberte " +
                "fotografiado pintando acuarelas. La foto no es nítida. Tu sonrisa, sí.",
            requirements = EndingRequirement(
                minDays = 365,
                karmaMin = -10,
                karmaMax = 30,
                achievementsRequired = listOf("ach_hidden_night_owl", "ach_day_100")
            ),
            shareLine = "Final SECRETO desbloqueado en Empire Tycoon. Solo el 1% lo verá."
        )
    )

    fun byType(t: EndingType): Ending? = all.firstOrNull { it.type == t }
}
