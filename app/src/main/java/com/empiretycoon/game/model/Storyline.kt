package com.empiretycoon.game.model

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Arco narrativo principal — 12 capítulos cinemáticos que cuentan el ascenso
 * (o caída) de tu imperio. Cada capítulo se desbloquea al cumplir
 * requisitos económicos / temporales, ofrece una decisión moral con efectos
 * cuantitativos sobre el estado, y empuja al jugador hacia uno de los 11
 * finales posibles.
 *
 * Estilo: prosa breve y dramática, en español, con un toque de ironía.
 */
@Serializable
data class StoryChapter(
    val id: String,
    val title: String,
    val intro: String,
    val outro: String,
    val requirements: ChapterReq,
    val choices: List<StoryChoice>,
    val illustrationEmoji: String
)

@Serializable
data class ChapterReq(
    val minLevel: Int = 1,
    val minCash: Double = 0.0,
    val requiredQuestId: String? = null,
    val daysSinceStart: Int = 0
)

@Serializable
data class StoryChoice(
    val label: String,
    val consequenceText: String,
    val effects: List<StoryEffect> = emptyList(),
    val leadsToChapterId: String? = null
)

/**
 * Efectos discretos que aplica un capítulo. Modelados como `sealed class`
 * polimórfica para que el motor pueda iterar y aplicar cada uno con tipado.
 */
@Serializable
sealed class StoryEffect {
    @Serializable data class CashDelta(val amount: Double) : StoryEffect()
    @Serializable data class ReputationDelta(val d: Int) : StoryEffect()
    @Serializable data class HappinessDelta(val d: Int) : StoryEffect()
    @Serializable data class KarmaDelta(val d: Int) : StoryEffect()
    @Serializable data class EnergyDelta(val d: Int) : StoryEffect()
    @Serializable data class UnlockAchievement(val id: String) : StoryEffect()
    @Serializable data class GivePerk(val id: String) : StoryEffect()
    @Serializable data class DamageRival(val id: String, val amount: Int = 10) : StoryEffect()
    @Serializable data class AlignWithNPC(val id: String, val amount: Int = 10) : StoryEffect()
}

/**
 * Estado serializable del arco narrativo. Vive dentro de `GameState`.
 *
 * - `currentChapterId`: capítulo a mostrar al abrir la pantalla. Si es nulo
 *   y aún no se cumplen los requisitos, no se muestra nada.
 * - `choicesMade`: chapter_id -> índice de elección, para reconstruir la
 *   "huella moral" del jugador en los finales.
 * - `karma`: -100..100 (avaricia .. integridad). Lo deriva el motor.
 * - `alignments`: relaciones con NPC clave acumuladas durante la historia.
 */
@Serializable
data class StorylineState(
    val currentChapterId: String = "ch_prologue",
    val completedChapters: List<String> = emptyList(),
    val choicesMade: Map<String, Int> = emptyMap(),
    val karma: Int = 0,
    val alignments: Map<String, Int> = emptyMap(),
    val achievedEndingType: String? = null
)

object StoryArc {

    val chapters: List<StoryChapter> = listOf(

        // ---------- 1. PRÓLOGO ----------
        StoryChapter(
            id = "ch_prologue",
            title = "El garaje y un sueño",
            illustrationEmoji = "🛠️",
            intro = "El olor a aceite del garaje de tus padres. Una mesa de madera, " +
                "una silla coja y una idea que no te deja dormir. Tu madre te trae " +
                "café. Tu padre, en silencio, asiente. Hoy fundas la empresa que " +
                "cambiará tu vida. ¿O te tragará entero?",
            outro = "El primer paso siempre se da con miedo. El segundo, con prisa.",
            requirements = ChapterReq(minLevel = 1, daysSinceStart = 0),
            choices = listOf(
                StoryChoice(
                    label = "Voy con todo: pido un préstamo familiar",
                    consequenceText = "Tu padre suspira y te firma un cheque. No esperes que te lo perdone.",
                    effects = listOf(
                        StoryEffect.CashDelta(5_000.0),
                        StoryEffect.HappinessDelta(-3),
                        StoryEffect.KarmaDelta(-2),
                        StoryEffect.AlignWithNPC("npc_tio_beto", -5)
                    )
                ),
                StoryChoice(
                    label = "Empezaré con lo justo, sin deber nada",
                    consequenceText = "Difícil, pero tu integridad permanece intacta.",
                    effects = listOf(
                        StoryEffect.HappinessDelta(+5),
                        StoryEffect.KarmaDelta(+5)
                    )
                ),
                StoryChoice(
                    label = "Convenzo a un amigo de invertir",
                    consequenceText = "Pepe el Tabernero pone 2.000 \"por la confianza\".",
                    effects = listOf(
                        StoryEffect.CashDelta(2_000.0),
                        StoryEffect.AlignWithNPC("npc_pepe", +10),
                        StoryEffect.KarmaDelta(0)
                    )
                )
            )
        ),

        // ---------- 2. PRIMER MILLÓN ----------
        StoryChapter(
            id = "ch_first_million",
            title = "Siete cifras a media noche",
            illustrationEmoji = "💰",
            intro = "Es la una de la madrugada. Miras la cuenta corporativa y " +
                "cuentas los ceros tres veces para asegurarte. Un millón. " +
                "Tu primer millón. Sales al balcón con un cigarro que no fumas, " +
                "porque te lo mereces.",
            outro = "Lo que hagas con el primer millón define los siguientes diez.",
            requirements = ChapterReq(minLevel = 5, minCash = 1_000_000.0, daysSinceStart = 7),
            choices = listOf(
                StoryChoice(
                    label = "Reinvertir todo en la empresa",
                    consequenceText = "Cero comisión, tope ambición. La empresa lo absorbe.",
                    effects = listOf(
                        StoryEffect.ReputationDelta(+5),
                        StoryEffect.KarmaDelta(+3),
                        StoryEffect.UnlockAchievement("ach_cash_1m")
                    )
                ),
                StoryChoice(
                    label = "Comprar el yate de tus sueños",
                    consequenceText = "El yate llega un martes. La envidia, el miércoles.",
                    effects = listOf(
                        StoryEffect.CashDelta(-150_000.0),
                        StoryEffect.HappinessDelta(+15),
                        StoryEffect.KarmaDelta(-5),
                        StoryEffect.ReputationDelta(-2)
                    )
                ),
                StoryChoice(
                    label = "Mitad para ti, mitad para el equipo",
                    consequenceText = "Reparto justo. Tu plantilla nunca lo olvidará.",
                    effects = listOf(
                        StoryEffect.CashDelta(-75_000.0),
                        StoryEffect.HappinessDelta(+8),
                        StoryEffect.KarmaDelta(+8),
                        StoryEffect.ReputationDelta(+8)
                    )
                )
            )
        ),

        // ---------- 3. PRIMER EQUIPO ----------
        StoryChapter(
            id = "ch_first_team",
            title = "Ya no estás solo",
            illustrationEmoji = "👥",
            intro = "Cinco curriculums sobre la mesa, cinco vidas que pueden cambiar " +
                "según firmes o no. La pregunta deja de ser \"¿qué hago yo?\" y " +
                "pasa a ser \"¿quién soy yo cuando otros dependen de mí?\".",
            outro = "El primer empleado es el espejo más honesto que tendrás nunca.",
            requirements = ChapterReq(minLevel = 7, daysSinceStart = 12),
            choices = listOf(
                StoryChoice(
                    label = "Contrato a los mejores aunque cuesten un riñón",
                    consequenceText = "Talento de élite, costes elevados, ego saciado.",
                    effects = listOf(
                        StoryEffect.CashDelta(-15_000.0),
                        StoryEffect.ReputationDelta(+6),
                        StoryEffect.KarmaDelta(+2)
                    )
                ),
                StoryChoice(
                    label = "Becarios con sueldo simbólico",
                    consequenceText = "Ahorras hoy, pagas mañana. La rotación dirá la última palabra.",
                    effects = listOf(
                        StoryEffect.CashDelta(+5_000.0),
                        StoryEffect.ReputationDelta(-7),
                        StoryEffect.KarmaDelta(-8),
                        StoryEffect.HappinessDelta(-2)
                    )
                ),
                StoryChoice(
                    label = "Apuesto por gente humilde con hambre",
                    consequenceText = "Dales ala, no los sueltes. Volverán convertidos en lobos.",
                    effects = listOf(
                        StoryEffect.ReputationDelta(+3),
                        StoryEffect.KarmaDelta(+5),
                        StoryEffect.AlignWithNPC("npc_pepe", +5)
                    )
                )
            )
        ),

        // ---------- 4. APARECE EL RIVAL ----------
        StoryChapter(
            id = "ch_rival_appears",
            title = "El heredero",
            illustrationEmoji = "🎭",
            intro = "Tristán Velasco. Apellido con escudo, traje de tres mil euros y " +
                "una sonrisa de tiburón en piscina infantil. \"Ay, nuestra empresita\", " +
                "te suelta en una gala benéfica. \"Qué tierna\". Y ya sabes: hay rival.",
            outro = "Algunos enemigos no se eligen. Te eligen ellos.",
            requirements = ChapterReq(minLevel = 10, daysSinceStart = 18),
            choices = listOf(
                StoryChoice(
                    label = "Le sigo el juego en público y le ataco en privado",
                    consequenceText = "El rey del doble filo. Reputación intacta, conciencia, no tanto.",
                    effects = listOf(
                        StoryEffect.KarmaDelta(-6),
                        StoryEffect.DamageRival("npc_tristan", 15),
                        StoryEffect.AlignWithNPC("npc_tristan", -20)
                    )
                ),
                StoryChoice(
                    label = "Le ofrezco una alianza honesta",
                    consequenceText = "Se ríe en tu cara, pero algunos ven nobleza.",
                    effects = listOf(
                        StoryEffect.ReputationDelta(+4),
                        StoryEffect.KarmaDelta(+5),
                        StoryEffect.AlignWithNPC("npc_tristan", +5)
                    )
                ),
                StoryChoice(
                    label = "Compito sin mirar atrás, que pierda el peor",
                    consequenceText = "Empieza la guerra fría. Que gane el mercado.",
                    effects = listOf(
                        StoryEffect.KarmaDelta(0),
                        StoryEffect.DamageRival("npc_tristan", 5),
                        StoryEffect.ReputationDelta(+1)
                    )
                )
            )
        ),

        // ---------- 5. CRISIS DE PRENSA ----------
        StoryChapter(
            id = "ch_pr_crisis",
            title = "Portada incómoda",
            illustrationEmoji = "📰",
            intro = "Sofía Rovira, periodista incómoda y atractiva en igual medida, " +
                "te desayuna con una portada: \"¿Sueldos justos en " +
                "${"\${empresa}"}?\". Tres horas para responder. " +
                "Tu jefe de prensa tiembla. Tú piensas, rápido.",
            outro = "La verdad es elástica, pero solo hasta que se rompe.",
            requirements = ChapterReq(minLevel = 12, daysSinceStart = 25),
            choices = listOf(
                StoryChoice(
                    label = "Convoco rueda de prensa, reconozco errores y subo salarios",
                    consequenceText = "Coraje. La calle te aplaude. La cuenta llora.",
                    effects = listOf(
                        StoryEffect.CashDelta(-25_000.0),
                        StoryEffect.ReputationDelta(+12),
                        StoryEffect.KarmaDelta(+10),
                        StoryEffect.HappinessDelta(+5),
                        StoryEffect.AlignWithNPC("npc_sofia", +15)
                    )
                ),
                StoryChoice(
                    label = "Compro silencio: oferta económica al medio",
                    consequenceText = "Dinero por papel. La portada cambia. La sospecha queda.",
                    effects = listOf(
                        StoryEffect.CashDelta(-50_000.0),
                        StoryEffect.KarmaDelta(-15),
                        StoryEffect.ReputationDelta(-3),
                        StoryEffect.AlignWithNPC("npc_sofia", -20)
                    )
                ),
                StoryChoice(
                    label = "Niego todo y echo la culpa a un becario",
                    consequenceText = "El becario llora. Tú duermes mal. La prensa muerde más fuerte.",
                    effects = listOf(
                        StoryEffect.ReputationDelta(-15),
                        StoryEffect.KarmaDelta(-25),
                        StoryEffect.HappinessDelta(-10)
                    )
                )
            )
        ),

        // ---------- 6. EXPANSIÓN INTERNACIONAL ----------
        StoryChapter(
            id = "ch_international",
            title = "Banderas en el mapa",
            illustrationEmoji = "🌍",
            intro = "Un consultor de Zúrich entra en tu despacho con un mapa y un café " +
                "frío. Marca tres países en rojo: México, Alemania, Singapur. " +
                "\"Decida usted dónde plantar la bandera primero. Yo cobro igual\".",
            outro = "El mundo es grande, pero los problemas viajan en business class.",
            requirements = ChapterReq(minLevel = 15, minCash = 5_000_000.0, daysSinceStart = 35),
            choices = listOf(
                StoryChoice(
                    label = "México: cercanía cultural y costes bajos",
                    consequenceText = "Empezamos por casa. Riesgo medido.",
                    effects = listOf(
                        StoryEffect.CashDelta(-200_000.0),
                        StoryEffect.ReputationDelta(+8),
                        StoryEffect.KarmaDelta(+2)
                    )
                ),
                StoryChoice(
                    label = "Alemania: ingeniería seria, sindicatos serios",
                    consequenceText = "Calidad germánica. Burocracia germánica.",
                    effects = listOf(
                        StoryEffect.CashDelta(-450_000.0),
                        StoryEffect.ReputationDelta(+15),
                        StoryEffect.KarmaDelta(+5)
                    )
                ),
                StoryChoice(
                    label = "Singapur: paraíso fiscal, tigres a tu alrededor",
                    consequenceText = "Te ahorras impuestos. Cuidado con quién acaba siendo presa.",
                    effects = listOf(
                        StoryEffect.CashDelta(-300_000.0),
                        StoryEffect.ReputationDelta(-5),
                        StoryEffect.KarmaDelta(-12)
                    )
                )
            )
        ),

        // ---------- 7. AVANCE TECNOLÓGICO O ESCÁNDALO ----------
        StoryChapter(
            id = "ch_tech_breakthrough",
            title = "El descubrimiento",
            illustrationEmoji = "🧪",
            intro = "Tu equipo de I+D ha logrado algo gordo: reducir costes en un 40% " +
                "con un proceso patentable. Pero hay un detalle: el método pasa de " +
                "puntillas por una norma medioambiental. \"Es legal, jefe. Casi\".",
            outro = "La innovación es preguntarse \"¿se puede?\". La ética, \"¿se debe?\".",
            requirements = ChapterReq(minLevel = 18, daysSinceStart = 50),
            choices = listOf(
                StoryChoice(
                    label = "Patentar y publicar: estándar limpio del sector",
                    consequenceText = "Te aplauden. Y te copian. La fama compensa.",
                    effects = listOf(
                        StoryEffect.ReputationDelta(+18),
                        StoryEffect.KarmaDelta(+15),
                        StoryEffect.UnlockAchievement("ach_5_tech"),
                        StoryEffect.HappinessDelta(+8)
                    )
                ),
                StoryChoice(
                    label = "Lanzar en silencio mientras nadie mira",
                    consequenceText = "Beneficios brutales, secreto a voces.",
                    effects = listOf(
                        StoryEffect.CashDelta(+500_000.0),
                        StoryEffect.KarmaDelta(-15),
                        StoryEffect.ReputationDelta(-5)
                    )
                ),
                StoryChoice(
                    label = "Vender la patente a un competidor",
                    consequenceText = "Una bolsa pesada. Una conciencia ligera. ¿O al revés?",
                    effects = listOf(
                        StoryEffect.CashDelta(+1_500_000.0),
                        StoryEffect.KarmaDelta(-8),
                        StoryEffect.ReputationDelta(-10),
                        StoryEffect.AlignWithNPC("npc_tristan", +10)
                    )
                )
            )
        ),

        // ---------- 8. SALIDA A BOLSA ----------
        StoryChapter(
            id = "ch_ipo",
            title = "El sonido de la campana",
            illustrationEmoji = "🔔",
            intro = "Wall Street. Una campana de bronce que vibra cuando la golpean. " +
                "Tu nombre en la pantalla. Las acciones suben un 18% en quince " +
                "minutos. Tu padre te llama llorando. Tú también, en privado.",
            outro = "Cotizar es ganar libertad y firmar un contrato con el ruido.",
            requirements = ChapterReq(minLevel = 22, minCash = 20_000_000.0, daysSinceStart = 70),
            choices = listOf(
                StoryChoice(
                    label = "Mantengo mayoría: yo decido",
                    consequenceText = "Conservas el timón. Los inversores aceptarán por ahora.",
                    effects = listOf(
                        StoryEffect.ReputationDelta(+10),
                        StoryEffect.KarmaDelta(+5),
                        StoryEffect.HappinessDelta(+10)
                    )
                ),
                StoryChoice(
                    label = "Vendo la mitad y me hago multimillonario",
                    consequenceText = "Te haces inmensamente rico. Pero ya no es tu empresa, es de ellos.",
                    effects = listOf(
                        StoryEffect.CashDelta(+15_000_000.0),
                        StoryEffect.KarmaDelta(-5),
                        StoryEffect.ReputationDelta(-3)
                    )
                ),
                StoryChoice(
                    label = "Reparto acciones gratis a empleados fundadores",
                    consequenceText = "Ningún CEO ha hecho llorar de alegría a tantos a la vez.",
                    effects = listOf(
                        StoryEffect.CashDelta(-2_000_000.0),
                        StoryEffect.ReputationDelta(+25),
                        StoryEffect.KarmaDelta(+25),
                        StoryEffect.HappinessDelta(+15)
                    )
                )
            )
        ),

        // ---------- 9. INTENTO DE OPA HOSTIL ----------
        StoryChapter(
            id = "ch_hostile_takeover",
            title = "Lobos en la puerta",
            illustrationEmoji = "🐺",
            intro = "Una OPA hostil. Un fondo extranjero acumula acciones en silencio " +
                "y de pronto, sobre tu mesa, una propuesta de adquisición \"amistosa\" " +
                "que de amistosa tiene lo justo. Don Marcial, tu mentor, te llama: " +
                "\"Hijo, esto se mata o te mata\".",
            outro = "Los lobos no muerden cuando ven dientes. Muerden cuando los muestras tarde.",
            requirements = ChapterReq(minLevel = 25, daysSinceStart = 90),
            choices = listOf(
                StoryChoice(
                    label = "Resistencia legal pública con apoyo popular",
                    consequenceText = "Eres David. La calle, tu honda. Caro pero épico.",
                    effects = listOf(
                        StoryEffect.CashDelta(-2_000_000.0),
                        StoryEffect.ReputationDelta(+20),
                        StoryEffect.KarmaDelta(+12),
                        StoryEffect.AlignWithNPC("npc_marcial", +10)
                    )
                ),
                StoryChoice(
                    label = "Alianza turbia con Don Marcial para amedrentarlos",
                    consequenceText = "Algunas amenazas no se firman. Pero los lobos se van con el rabo entre las patas.",
                    effects = listOf(
                        StoryEffect.KarmaDelta(-20),
                        StoryEffect.ReputationDelta(-5),
                        StoryEffect.AlignWithNPC("npc_marcial", +25),
                        StoryEffect.DamageRival("npc_tristan", 25)
                    )
                ),
                StoryChoice(
                    label = "Acepto la OPA: cobro y me marcho",
                    consequenceText = "Una jubilación dorada antes de tiempo.",
                    effects = listOf(
                        StoryEffect.CashDelta(+30_000_000.0),
                        StoryEffect.KarmaDelta(-8),
                        StoryEffect.ReputationDelta(-15),
                        StoryEffect.HappinessDelta(-15)
                    )
                )
            )
        ),

        // ---------- 10. ADQUISICIONES ----------
        StoryChapter(
            id = "ch_acquisition_spree",
            title = "Comprando el tablero",
            illustrationEmoji = "♟️",
            intro = "El consejo te trae una lista: doce empresas pequeñas que se pueden " +
                "comprar baratas. \"Las absorbemos, despedimos, optimizamos\". Una " +
                "operación de manual. Nada nuevo bajo el sol. Tú miras la lista y " +
                "ves doce historias.",
            outro = "Cuando compras una empresa, compras también sus sombras.",
            requirements = ChapterReq(minLevel = 30, minCash = 50_000_000.0, daysSinceStart = 120),
            choices = listOf(
                StoryChoice(
                    label = "Compras todas y haces una purga rápida",
                    consequenceText = "Eficiencia inmediata. Despidos masivos. Beneficios récord.",
                    effects = listOf(
                        StoryEffect.CashDelta(-20_000_000.0),
                        StoryEffect.KarmaDelta(-25),
                        StoryEffect.ReputationDelta(-10),
                        StoryEffect.HappinessDelta(-8)
                    )
                ),
                StoryChoice(
                    label = "Compras tres y mantienes la plantilla",
                    consequenceText = "Crecimiento sostenible. Inversores rumian. Empleados respiran.",
                    effects = listOf(
                        StoryEffect.CashDelta(-8_000_000.0),
                        StoryEffect.ReputationDelta(+15),
                        StoryEffect.KarmaDelta(+15)
                    )
                ),
                StoryChoice(
                    label = "Rechazas todo: crecer orgánico",
                    consequenceText = "El consejo no entiende. Tú duermes en paz.",
                    effects = listOf(
                        StoryEffect.ReputationDelta(+5),
                        StoryEffect.KarmaDelta(+10),
                        StoryEffect.HappinessDelta(+12)
                    )
                )
            )
        ),

        // ---------- 11. FUNDACIÓN BENÉFICA O MONOPOLIO ----------
        StoryChapter(
            id = "ch_charity_or_monopoly",
            title = "El espejo retrovisor",
            illustrationEmoji = "🖋️",
            intro = "Cumples 50 años. La revista Forbes te llama el \"Imperio Iberico\" " +
                "y dedica veinte páginas a tu vida. Lees tu propia historia y, por " +
                "primera vez, te preguntas qué legado quieres dejar realmente.",
            outro = "Lo que dejes detrás te definirá más que lo que llegues a comprar.",
            requirements = ChapterReq(minLevel = 35, minCash = 80_000_000.0, daysSinceStart = 150),
            choices = listOf(
                StoryChoice(
                    label = "Crear una fundación benéfica con la mitad de mi fortuna",
                    consequenceText = "El dinero deja de ser tuyo. Tu nombre se vuelve eterno.",
                    effects = listOf(
                        StoryEffect.CashDelta(-40_000_000.0),
                        StoryEffect.ReputationDelta(+30),
                        StoryEffect.KarmaDelta(+30),
                        StoryEffect.HappinessDelta(+25),
                        StoryEffect.UnlockAchievement("ach_rep_max")
                    )
                ),
                StoryChoice(
                    label = "Aplastar a la competencia hasta el monopolio",
                    consequenceText = "Eres dueño del tablero. Y de cada peón.",
                    effects = listOf(
                        StoryEffect.CashDelta(+25_000_000.0),
                        StoryEffect.KarmaDelta(-30),
                        StoryEffect.ReputationDelta(-15),
                        StoryEffect.DamageRival("npc_tristan", 50)
                    )
                ),
                StoryChoice(
                    label = "Repartir dividendos extraordinarios a accionistas",
                    consequenceText = "Bolsillos contentos. Conciencia neutral.",
                    effects = listOf(
                        StoryEffect.CashDelta(-15_000_000.0),
                        StoryEffect.ReputationDelta(+5),
                        StoryEffect.KarmaDelta(0)
                    )
                )
            )
        ),

        // ---------- 12. LEGADO ----------
        StoryChapter(
            id = "ch_legacy",
            title = "El último capítulo lo escribes tú",
            illustrationEmoji = "📜",
            intro = "Hay un sillón de cuero, una ventana enorme, y la ciudad encendida " +
                "a tus pies. Te sientas, miras el horizonte, y eliges. Lo que decidas " +
                "ahora será tu epílogo.",
            outro = "El final no es un punto. Es un eco.",
            requirements = ChapterReq(minLevel = 40, daysSinceStart = 180),
            choices = listOf(
                StoryChoice(
                    label = "Pasarle el imperio a la siguiente generación y retirarme",
                    consequenceText = "Te marchas en silencio. Te recuerdan a gritos.",
                    effects = listOf(
                        StoryEffect.HappinessDelta(+30),
                        StoryEffect.KarmaDelta(+10),
                        StoryEffect.ReputationDelta(+15)
                    )
                ),
                StoryChoice(
                    label = "Seguir al timón hasta el último día",
                    consequenceText = "El imperio sin ti es un esqueleto. Lo sabes. Te quedas.",
                    effects = listOf(
                        StoryEffect.HappinessDelta(-10),
                        StoryEffect.KarmaDelta(-5),
                        StoryEffect.ReputationDelta(+5)
                    )
                ),
                StoryChoice(
                    label = "Liquidar todo y desaparecer del mapa",
                    consequenceText = "Una mañana ya no estás. Solo queda tu firma en un cheque al portador.",
                    effects = listOf(
                        StoryEffect.CashDelta(+10_000_000.0),
                        StoryEffect.ReputationDelta(-20),
                        StoryEffect.HappinessDelta(+5),
                        StoryEffect.KarmaDelta(0)
                    )
                )
            )
        )
    )

    private val byId = chapters.associateBy { it.id }
    fun byId(id: String): StoryChapter? = byId[id]

    /** Devuelve el siguiente capítulo en orden lineal o null si era el último. */
    fun nextOf(currentId: String): StoryChapter? {
        val idx = chapters.indexOfFirst { it.id == currentId }
        if (idx < 0) return null
        return chapters.getOrNull(idx + 1)
    }
}

/**
 * Modulo para registrar la jerarquía polimorfica de StoryEffect en kotlinx
 * serialization. Si en futuro se serializa StorylineState completo con
 * efectos embebidos, registra esto en el Json builder. Por ahora StorylineState
 * solo guarda IDs/decisiones, así que es una salvaguarda preventiva.
 */
val StoryEffectsModule: SerializersModule = SerializersModule {
    polymorphic(StoryEffect::class) {
        subclass(StoryEffect.CashDelta::class)
        subclass(StoryEffect.ReputationDelta::class)
        subclass(StoryEffect.HappinessDelta::class)
        subclass(StoryEffect.KarmaDelta::class)
        subclass(StoryEffect.EnergyDelta::class)
        subclass(StoryEffect.UnlockAchievement::class)
        subclass(StoryEffect.GivePerk::class)
        subclass(StoryEffect.DamageRival::class)
        subclass(StoryEffect.AlignWithNPC::class)
    }
}
