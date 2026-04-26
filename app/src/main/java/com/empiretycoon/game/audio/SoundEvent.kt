package com.empiretycoon.game.audio

/**
 * Catálogo de eventos de sonido del juego. Cada evento se sintetiza
 * en tiempo de ejecución vía [SoundEngine] — no se envían assets.
 *
 * El estilo es chiptune ligero: tonos breves con envolvente ADSR
 * suave para evitar clicks. Los eventos multi-tono usan secuencias
 * cortas que evocan (sin replicar) sonidos clásicos como ka-ching,
 * fanfarrias o gritos sorpresa.
 */
enum class SoundEvent {
    CASH_REGISTER,
    COIN_DROP,
    BUILDING_PLACED,
    LEVEL_UP,
    ACHIEVEMENT_UNLOCKED,
    BUTTON_CLICK,
    BUTTON_BACK,
    ERROR,
    WARNING,
    NOTIFICATION,
    EVENT_TRIGGERED,
    RESEARCH_COMPLETED,
    MARKET_SELL,
    MARKET_BUY,
    BANKRUPTCY,
    FANFARE,
    TICK_TOCK,
    DOOR_OPEN,
    GASP,
    APPLAUSE_LIGHT,
    NEW_DAY,
    MENU_SWITCH
}

/**
 * Forma de onda usada al sintetizar una nota. Cada una tiene un
 * timbre claramente distinto:
 *
 *  - [SINE]    → puro, redondo (notificaciones suaves).
 *  - [SQUARE]  → 8-bit / NES (ka-ching, fanfarrias).
 *  - [SAW]     → metálico, brillante (caja registradora, drone).
 *  - [TRI]     → flauta-bell (campanitas, level-up).
 *  - [NOISE]   → percusión, tick, explosiones.
 */
enum class Waveform { SINE, SQUARE, SAW, TRI, NOISE }

/**
 * Definición declarativa de un sonido. [frequencies] y [durations]
 * deben tener la misma longitud > 0; cada par (f, d) es una nota
 * que se reproduce secuencialmente. La duración va en milisegundos.
 *
 * @param volume amplitud relativa 0f..1f sobre la que aún aplica
 *   el master volume del [SoundEngine].
 */
data class SoundDef(
    val event: SoundEvent,
    val frequencies: List<Int>,
    val durations: List<Int>,
    val volume: Float,
    val waveform: Waveform,
    /** Ataque del envelope ADSR en ms. */
    val attackMs: Int = 6,
    /** Decay del envelope ADSR en ms. */
    val decayMs: Int = 24
) {
    init {
        require(frequencies.size == durations.size && frequencies.isNotEmpty()) {
            "frequencies y durations deben tener el mismo tamaño y no estar vacíos"
        }
    }
}

/**
 * Mapa con valores por defecto pensados para que cada evento del
 * juego tenga una identidad sónica reconocible y agradable.
 */
object SoundDefs {

    /** Notas musicales útiles. */
    private const val C5 = 523
    private const val D5 = 587
    private const val E5 = 659
    private const val F5 = 698
    private const val G5 = 784
    private const val A5 = 880
    private const val B5 = 988
    private const val C6 = 1046
    private const val D6 = 1174
    private const val E6 = 1318
    private const val G6 = 1568
    private const val A6 = 1760
    private const val C7 = 2093

    val map: Map<SoundEvent, SoundDef> = mapOf(
        // "ka-ching": dos tonos brillantes ascendentes
        SoundEvent.CASH_REGISTER to SoundDef(
            event = SoundEvent.CASH_REGISTER,
            frequencies = listOf(C6, E6, G6, C7),
            durations = listOf(45, 45, 60, 110),
            volume = 0.85f, waveform = Waveform.SQUARE
        ),
        // moneda cayendo: ping corto + reflejo agudo
        SoundEvent.COIN_DROP to SoundDef(
            event = SoundEvent.COIN_DROP,
            frequencies = listOf(A6, E6),
            durations = listOf(70, 90),
            volume = 0.75f, waveform = Waveform.TRI
        ),
        // edificio colocado: golpe grave seguido de afirmación
        SoundEvent.BUILDING_PLACED to SoundDef(
            event = SoundEvent.BUILDING_PLACED,
            frequencies = listOf(120, 220, 330),
            durations = listOf(80, 60, 100),
            volume = 0.85f, waveform = Waveform.SQUARE,
            attackMs = 4, decayMs = 30
        ),
        // level-up: arpegio mayor brillante
        SoundEvent.LEVEL_UP to SoundDef(
            event = SoundEvent.LEVEL_UP,
            frequencies = listOf(C5, E5, G5, C6, E6, G6),
            durations = listOf(60, 60, 60, 60, 60, 140),
            volume = 0.9f, waveform = Waveform.SQUARE
        ),
        // logro: 3 notas + nota final brillante
        SoundEvent.ACHIEVEMENT_UNLOCKED to SoundDef(
            event = SoundEvent.ACHIEVEMENT_UNLOCKED,
            frequencies = listOf(G5, B5, D6, G6, D6, G6),
            durations = listOf(80, 80, 80, 100, 80, 180),
            volume = 0.95f, waveform = Waveform.SQUARE
        ),
        // click corto y seco
        SoundEvent.BUTTON_CLICK to SoundDef(
            event = SoundEvent.BUTTON_CLICK,
            frequencies = listOf(1200),
            durations = listOf(28),
            volume = 0.45f, waveform = Waveform.SQUARE,
            attackMs = 2, decayMs = 16
        ),
        // back / cancelar: tono descendente
        SoundEvent.BUTTON_BACK to SoundDef(
            event = SoundEvent.BUTTON_BACK,
            frequencies = listOf(900, 600),
            durations = listOf(28, 50),
            volume = 0.5f, waveform = Waveform.SQUARE,
            attackMs = 2, decayMs = 18
        ),
        // error: tritono descendente disonante
        SoundEvent.ERROR to SoundDef(
            event = SoundEvent.ERROR,
            frequencies = listOf(440, 311),
            durations = listOf(110, 180),
            volume = 0.75f, waveform = Waveform.SAW,
            attackMs = 3, decayMs = 80
        ),
        // warning: dos tonos pulsantes
        SoundEvent.WARNING to SoundDef(
            event = SoundEvent.WARNING,
            frequencies = listOf(880, 660, 880, 660),
            durations = listOf(60, 60, 60, 80),
            volume = 0.7f, waveform = Waveform.SQUARE
        ),
        // notificación neutra
        SoundEvent.NOTIFICATION to SoundDef(
            event = SoundEvent.NOTIFICATION,
            frequencies = listOf(C6, G6),
            durations = listOf(80, 110),
            volume = 0.6f, waveform = Waveform.SINE
        ),
        // evento aleatorio: misterio, tritono ascendente
        SoundEvent.EVENT_TRIGGERED to SoundDef(
            event = SoundEvent.EVENT_TRIGGERED,
            frequencies = listOf(440, 622, 880),
            durations = listOf(80, 80, 160),
            volume = 0.75f, waveform = Waveform.TRI
        ),
        // research: campanada doble
        SoundEvent.RESEARCH_COMPLETED to SoundDef(
            event = SoundEvent.RESEARCH_COMPLETED,
            frequencies = listOf(E6, B5, E6, A6),
            durations = listOf(70, 70, 70, 220),
            volume = 0.85f, waveform = Waveform.TRI
        ),
        // venta exitosa: brillo ascendente
        SoundEvent.MARKET_SELL to SoundDef(
            event = SoundEvent.MARKET_SELL,
            frequencies = listOf(E6, A6),
            durations = listOf(60, 100),
            volume = 0.75f, waveform = Waveform.SQUARE
        ),
        // compra: tono firme grave-medio
        SoundEvent.MARKET_BUY to SoundDef(
            event = SoundEvent.MARKET_BUY,
            frequencies = listOf(A5, E5),
            durations = listOf(70, 100),
            volume = 0.7f, waveform = Waveform.SQUARE
        ),
        // bancarrota: descenso siniestro
        SoundEvent.BANKRUPTCY to SoundDef(
            event = SoundEvent.BANKRUPTCY,
            frequencies = listOf(220, 196, 174, 146, 110),
            durations = listOf(140, 140, 140, 160, 320),
            volume = 0.95f, waveform = Waveform.SAW,
            attackMs = 8, decayMs = 120
        ),
        // fanfarria triunfal
        SoundEvent.FANFARE to SoundDef(
            event = SoundEvent.FANFARE,
            frequencies = listOf(C5, C5, C5, C5, G5, F5, E5, F5, G5),
            durations = listOf(110, 60, 110, 110, 110, 110, 110, 110, 220),
            volume = 0.95f, waveform = Waveform.SQUARE
        ),
        // reloj tic-tac percutivo
        SoundEvent.TICK_TOCK to SoundDef(
            event = SoundEvent.TICK_TOCK,
            frequencies = listOf(2400, 1200),
            durations = listOf(20, 20),
            volume = 0.4f, waveform = Waveform.NOISE,
            attackMs = 1, decayMs = 12
        ),
        // puerta abriéndose: chirrido descendente
        SoundEvent.DOOR_OPEN to SoundDef(
            event = SoundEvent.DOOR_OPEN,
            frequencies = listOf(700, 500, 350),
            durations = listOf(80, 80, 160),
            volume = 0.6f, waveform = Waveform.SAW,
            attackMs = 6, decayMs = 80
        ),
        // gasp: subida rápida + sostenida
        SoundEvent.GASP to SoundDef(
            event = SoundEvent.GASP,
            frequencies = listOf(440, 660, 880),
            durations = listOf(40, 40, 220),
            volume = 0.7f, waveform = Waveform.NOISE,
            attackMs = 8, decayMs = 80
        ),
        // aplauso ligero: ráfagas de ruido
        SoundEvent.APPLAUSE_LIGHT to SoundDef(
            event = SoundEvent.APPLAUSE_LIGHT,
            frequencies = listOf(1200, 1400, 1100, 1500, 1300, 1200),
            durations = listOf(30, 25, 30, 25, 30, 30),
            volume = 0.55f, waveform = Waveform.NOISE,
            attackMs = 1, decayMs = 18
        ),
        // nuevo día: campanada + bell suave
        SoundEvent.NEW_DAY to SoundDef(
            event = SoundEvent.NEW_DAY,
            frequencies = listOf(C5, G5, C6, E6),
            durations = listOf(110, 110, 110, 260),
            volume = 0.7f, waveform = Waveform.TRI
        ),
        // cambio de menú: pequeño slide
        SoundEvent.MENU_SWITCH to SoundDef(
            event = SoundEvent.MENU_SWITCH,
            frequencies = listOf(800, 1100),
            durations = listOf(22, 32),
            volume = 0.4f, waveform = Waveform.SQUARE,
            attackMs = 2, decayMs = 14
        )
    )
}
