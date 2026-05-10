package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Sistema de Oficios (v17 — siguientes tandas de MEJORAS.md).
 *
 * Filosofía:
 *  - 40 oficios jugables agrupados en 11 categorías.
 *  - El jugador "trabaja un turno" en un oficio desbloqueado: gasta energía,
 *    gana cash + XP del oficio + XP del jugador. Un turno = 1 hora in-game
 *    (60 ticks) — pero la abstracción del MVP es instantánea: presionas
 *    "Trabajar turno" y se aplica el resultado inmediatamente.
 *  - Cada oficio tiene un atributo del jugador favorecido (preferredStat).
 *    Si tienes ese stat alto, ganas más por turno.
 *  - El nivel del oficio sube con XP: cada 100 XP = +1 nivel (cap 50).
 *    Cada nivel da +5% de ganancia y desbloquea el siguiente oficio
 *    "afín" en cascada.
 *  - **Mini-juegos**: en este commit NO se implementan. El framework deja
 *    el hueco — cada oficio se irá puliendo con su propio mini-juego en
 *    futuras tandas.
 *
 * Todo @Serializable con defaults para que saves antiguos carguen.
 */

/** Atributo preferido por un oficio. Pesa en el cálculo de wage. */
@Serializable
enum class JobStat { INT, STR, CHA, LUC, DEX }

/** Categoría visual / de agrupamiento en el hub. */
@Serializable
enum class JobCategory(val displayName: String, val emoji: String) {
    EMERGENCY("Servicios de emergencia", "🚨"),
    HEALTH("Sanidad", "🏥"),
    EDUCATION("Educación y arte", "🏫"),
    RESTAURANT("Restauración", "🍞"),
    CONSTRUCTION("Construcción y reparación", "🔨"),
    TRANSPORT("Transporte", "🚗"),
    PUBLIC("Servicios públicos", "📮"),
    ANIMALS("Naturaleza y animales", "🐾"),
    TECH("Tecnología", "💻"),
    SPORT("Espectáculo y deporte", "🎬"),
    SPECIAL("Curiosos y especiales", "🕵️")
}

/**
 * Catálogo de 40 oficios con datos balanceados.
 *
 * Convenciones:
 *  - `baseHourlyWage`: € por turno base (un turno = 1h in-game).
 *  - `requiredPlayerLevel`: nivel mínimo del personaje para desbloquear.
 *  - `energyCost`: ⚡ que gasta el jugador por turno.
 *  - `preferredStat`: el que más bonifica el wage (cada punto = +0.5%).
 *  - `description`: 1 línea para el hub.
 *  - `miniGameDescription`: qué mini-juego planeamos. Por ahora informativo.
 *
 * Ordenados por categoría + roughly por progresión.
 */
@Serializable
enum class JobId(
    val displayName: String,
    val emoji: String,
    val category: JobCategory,
    val description: String,
    val miniGameDescription: String,
    val baseHourlyWage: Double,
    val requiredPlayerLevel: Int,
    val energyCost: Int,
    val preferredStat: JobStat
) {
    // ===== EMERGENCY =====
    POLICE_OFFICER(
        "Policía", "🚓", JobCategory.EMERGENCY,
        "Patrulla la ciudad y atrapa ladrones.",
        "Tap-reaction sobre sospechosos en una vista top-down.",
        baseHourlyWage = 28.0, requiredPlayerLevel = 2, energyCost = 14, preferredStat = JobStat.STR
    ),
    FIREFIGHTER(
        "Bombero", "🚒", JobCategory.EMERGENCY,
        "Apaga incendios antes de que se propaguen.",
        "Drag&drop de mangueras con timer de propagación.",
        baseHourlyWage = 32.0, requiredPlayerLevel = 3, energyCost = 18, preferredStat = JobStat.STR
    ),
    PARAMEDIC(
        "Paramédico", "🚑", JobCategory.EMERGENCY,
        "Atiende emergencias médicas sobre el terreno.",
        "Arrastrar al paciente + secuencia QTE de RCP.",
        baseHourlyWage = 30.0, requiredPlayerLevel = 4, energyCost = 16, preferredStat = JobStat.DEX
    ),
    K9_OFFICER(
        "K-9 Officer", "🦮", JobCategory.EMERGENCY,
        "Adiestra y trabaja con perros policía.",
        "Rhythm game para órdenes al perro policía.",
        baseHourlyWage = 26.0, requiredPlayerLevel = 5, energyCost = 12, preferredStat = JobStat.CHA
    ),

    // ===== HEALTH =====
    DOCTOR(
        "Médico (cirujano)", "👨‍⚕️", JobCategory.HEALTH,
        "Realiza cirugías delicadas con precisión.",
        "Precision-tap sobre puntos del paciente con tiempo límite.",
        baseHourlyWage = 75.0, requiredPlayerLevel = 12, energyCost = 18, preferredStat = JobStat.INT
    ),
    DENTIST(
        "Dentista", "🦷", JobCategory.HEALTH,
        "Limpiezas, empastes, extracciones.",
        "Timing-based tap (limpieza/empaste).",
        baseHourlyWage = 55.0, requiredPlayerLevel = 8, energyCost = 14, preferredStat = JobStat.DEX
    ),
    PHARMACIST(
        "Farmacéutico", "💊", JobCategory.HEALTH,
        "Dispensa recetas y aconseja a pacientes.",
        "Combinar viales en orden correcto según receta.",
        baseHourlyWage = 38.0, requiredPlayerLevel = 6, energyCost = 10, preferredStat = JobStat.INT
    ),

    // ===== EDUCATION =====
    TEACHER(
        "Profesor", "👨‍🏫", JobCategory.EDUCATION,
        "Da clase a un grupo de estudiantes.",
        "Trivia con tiempo límite.",
        baseHourlyWage = 22.0, requiredPlayerLevel = 3, energyCost = 12, preferredStat = JobStat.INT
    ),
    PAINTER(
        "Pintor", "🎨", JobCategory.EDUCATION,
        "Pinta cuadros por encargo.",
        "Trazos en orden (memory + drag).",
        baseHourlyWage = 35.0, requiredPlayerLevel = 5, energyCost = 12, preferredStat = JobStat.DEX
    ),
    LIBRARIAN(
        "Bibliotecario", "📚", JobCategory.EDUCATION,
        "Cataloga y organiza la biblioteca.",
        "Sort puzzle por categorías.",
        baseHourlyWage = 18.0, requiredPlayerLevel = 1, energyCost = 8, preferredStat = JobStat.INT
    ),
    ACTOR(
        "Actor de teatro", "🎭", JobCategory.EDUCATION,
        "Interpreta papeles en el teatro.",
        "Secuencia de gestos sincronizados con guion.",
        baseHourlyWage = 40.0, requiredPlayerLevel = 7, energyCost = 14, preferredStat = JobStat.CHA
    ),

    // ===== RESTAURANT =====
    BAKER(
        "Panadero", "🥖", JobCategory.RESTAURANT,
        "Hornea pan y bollería desde la madrugada.",
        "Timing del horno + balance de ingredientes.",
        baseHourlyWage = 16.0, requiredPlayerLevel = 1, energyCost = 12, preferredStat = JobStat.DEX
    ),
    CHEF(
        "Chef de restaurante", "👨‍🍳", JobCategory.RESTAURANT,
        "Gestiona la cocina y las comandas.",
        "Cola de tickets con prioridad y combos.",
        baseHourlyWage = 45.0, requiredPlayerLevel = 8, energyCost = 16, preferredStat = JobStat.DEX
    ),
    PIZZAIOLO(
        "Pizzero", "🍕", JobCategory.RESTAURANT,
        "Estira la masa, pone los toppings, al horno.",
        "Estirar masa + colocar toppings + horno.",
        baseHourlyWage = 24.0, requiredPlayerLevel = 4, energyCost = 12, preferredStat = JobStat.DEX
    ),
    BARISTA(
        "Barista", "☕", JobCategory.RESTAURANT,
        "Café especial, latte art, atención al cliente.",
        "Drag patterns para latte art.",
        baseHourlyWage = 18.0, requiredPlayerLevel = 2, energyCost = 8, preferredStat = JobStat.CHA
    ),
    ICE_CREAM_SELLER(
        "Heladero", "🍦", JobCategory.RESTAURANT,
        "Vende helados con balance perfecto.",
        "Stack + balance de bolas en cucurucho.",
        baseHourlyWage = 14.0, requiredPlayerLevel = 1, energyCost = 6, preferredStat = JobStat.DEX
    ),

    // ===== CONSTRUCTION =====
    BRICKLAYER(
        "Albañil", "🔨", JobCategory.CONSTRUCTION,
        "Levanta paredes ladrillo a ladrillo.",
        "Tetris-like de ladrillos en pared.",
        baseHourlyWage = 22.0, requiredPlayerLevel = 2, energyCost = 18, preferredStat = JobStat.STR
    ),
    CARPENTER(
        "Carpintero", "🪚", JobCategory.CONSTRUCTION,
        "Fabrica muebles a medida.",
        "Cortes precisos con swipe.",
        baseHourlyWage = 28.0, requiredPlayerLevel = 4, energyCost = 14, preferredStat = JobStat.DEX
    ),
    CAR_MECHANIC(
        "Mecánico de coches", "🔧", JobCategory.CONSTRUCTION,
        "Repara averías y revisa vehículos.",
        "Identificar pieza rota + reemplazo.",
        baseHourlyWage = 32.0, requiredPlayerLevel = 5, energyCost = 14, preferredStat = JobStat.INT
    ),
    ELECTRICIAN(
        "Electricista", "⚡", JobCategory.CONSTRUCTION,
        "Instala y arregla circuitos.",
        "Puzzle de cables sin cortocircuitar.",
        baseHourlyWage = 30.0, requiredPlayerLevel = 5, energyCost = 12, preferredStat = JobStat.INT
    ),
    PLUMBER(
        "Fontanero", "🚿", JobCategory.CONSTRUCTION,
        "Tuberías, fugas y reparaciones.",
        "Pipe-puzzle (estilo Pipe Mania).",
        baseHourlyWage = 26.0, requiredPlayerLevel = 4, energyCost = 12, preferredStat = JobStat.INT
    ),

    // ===== TRANSPORT =====
    TAXI_DRIVER(
        "Taxista", "🚕", JobCategory.TRANSPORT,
        "Recoge pasajeros y los lleva a destino.",
        "Pickup + dropoff con ruta más corta.",
        baseHourlyWage = 20.0, requiredPlayerLevel = 2, energyCost = 10, preferredStat = JobStat.LUC
    ),
    TRUCKER(
        "Camionero", "🚚", JobCategory.TRANSPORT,
        "Entregas largas y aparcamiento difícil.",
        "Parking inverso de camión.",
        baseHourlyWage = 26.0, requiredPlayerLevel = 4, energyCost = 12, preferredStat = JobStat.DEX
    ),
    AIRLINE_PILOT(
        "Piloto de avión", "✈️", JobCategory.TRANSPORT,
        "Despegues y aterrizajes en condiciones variables.",
        "Balance de palanca durante despegue/aterrizaje.",
        baseHourlyWage = 90.0, requiredPlayerLevel = 18, energyCost = 16, preferredStat = JobStat.DEX
    ),
    TRAIN_DRIVER(
        "Maquinista de tren", "🚂", JobCategory.TRANSPORT,
        "Conduce el tren con paradas puntuales.",
        "Timing exacto en paradas + frenado.",
        baseHourlyWage = 32.0, requiredPlayerLevel = 6, energyCost = 12, preferredStat = JobStat.DEX
    ),
    RACING_DRIVER(
        "Piloto de carreras", "🏎️", JobCategory.TRANSPORT,
        "Compite en circuitos profesionales.",
        "Drift + timing en circuito (link a Formula Manager).",
        baseHourlyWage = 70.0, requiredPlayerLevel = 12, energyCost = 18, preferredStat = JobStat.DEX
    ),

    // ===== PUBLIC =====
    POSTMAN(
        "Cartero", "📮", JobCategory.PUBLIC,
        "Reparte correo por la ciudad.",
        "Encontrar buzones en el mapa con ruta limitada.",
        baseHourlyWage = 16.0, requiredPlayerLevel = 1, energyCost = 12, preferredStat = JobStat.DEX
    ),
    GARBAGE_COLLECTOR(
        "Recolector de basura", "🚛", JobCategory.PUBLIC,
        "Recoge contenedores antes de que se desborden.",
        "Tap timing en contenedores antes de derrame.",
        baseHourlyWage = 18.0, requiredPlayerLevel = 1, energyCost = 16, preferredStat = JobStat.STR
    ),
    GARDENER(
        "Jardinero municipal", "🌳", JobCategory.PUBLIC,
        "Poda, riega y mantiene los parques.",
        "Trim + water sequence con timing.",
        baseHourlyWage = 14.0, requiredPlayerLevel = 1, energyCost = 10, preferredStat = JobStat.DEX
    ),

    // ===== ANIMALS =====
    VET(
        "Veterinario", "🐶", JobCategory.ANIMALS,
        "Cuida animales enfermos.",
        "Examina + diagnostica + receta.",
        baseHourlyWage = 50.0, requiredPlayerLevel = 9, energyCost = 14, preferredStat = JobStat.INT
    ),
    FARMER(
        "Granjero", "🌾", JobCategory.ANIMALS,
        "Siembra, riega y cosecha.",
        "Tap rhythm para sembrar + regar + cosechar.",
        baseHourlyWage = 14.0, requiredPlayerLevel = 1, energyCost = 14, preferredStat = JobStat.STR
    ),
    FISHERMAN(
        "Pescador", "🌊", JobCategory.ANIMALS,
        "Pesca al amanecer en el puerto.",
        "Tug-of-war con peces + timing del anzuelo.",
        baseHourlyWage = 22.0, requiredPlayerLevel = 3, energyCost = 12, preferredStat = JobStat.LUC
    ),

    // ===== TECH =====
    PROGRAMMER(
        "Programador", "💻", JobCategory.TECH,
        "Encuentra bugs y entrega features.",
        "Encontrar el bug en código simple.",
        baseHourlyWage = 60.0, requiredPlayerLevel = 8, energyCost = 10, preferredStat = JobStat.INT
    ),
    UI_DESIGNER(
        "Diseñador UI/UX", "📱", JobCategory.TECH,
        "Maqueta interfaces para apps y webs.",
        "Snap-to-grid de elementos en wireframe.",
        baseHourlyWage = 48.0, requiredPlayerLevel = 6, energyCost = 8, preferredStat = JobStat.DEX
    ),
    STREAMER(
        "Streamer / Gamer Pro", "🎮", JobCategory.TECH,
        "Gana torneos y entretiene a tu audiencia.",
        "Speed-tap react contra IA.",
        baseHourlyWage = 35.0, requiredPlayerLevel = 4, energyCost = 8, preferredStat = JobStat.CHA
    ),

    // ===== SPORT =====
    FOOTBALL_PLAYER(
        "Futbolista", "⚽", JobCategory.SPORT,
        "Marca penaltis con efecto.",
        "Tap+drag para chutar penaltis con dirección.",
        baseHourlyWage = 100.0, requiredPlayerLevel = 14, energyCost = 18, preferredStat = JobStat.STR
    ),
    BOXER(
        "Boxeador", "🥊", JobCategory.SPORT,
        "Combate por rounds en el ring.",
        "Dodge + counter en rhythm vs IA.",
        baseHourlyWage = 80.0, requiredPlayerLevel = 12, energyCost = 20, preferredStat = JobStat.STR
    ),
    FILM_DIRECTOR(
        "Director de cine", "🎬", JobCategory.SPORT,
        "Rueda escenas y gestiona el rodaje.",
        "Gestiona crew + timing de tomas.",
        baseHourlyWage = 65.0, requiredPlayerLevel = 10, energyCost = 12, preferredStat = JobStat.CHA
    ),

    // ===== SPECIAL =====
    DETECTIVE(
        "Detective privado", "🦸", JobCategory.SPECIAL,
        "Resuelve casos buscando pistas.",
        "Encuentra pistas en escena (find-the-clue).",
        baseHourlyWage = 55.0, requiredPlayerLevel = 9, energyCost = 12, preferredStat = JobStat.INT
    ),
    ILLUSIONIST(
        "Ilusionista", "🧙", JobCategory.SPECIAL,
        "Espectáculos de magia ante el público.",
        "Secuencia de cartas en orden bajo presión.",
        baseHourlyWage = 45.0, requiredPlayerLevel = 7, energyCost = 10, preferredStat = JobStat.CHA
    );

    /**
     * Tag de mini-juego implementado o pendiente — actualizar al añadir
     * cada uno. Si false, "Trabajar 1h" usa el flujo instantáneo
     * (JobsEngine.workShift). Si true, JobsScreen lanza el Composable
     * jugable correspondiente y reporta el resultado vía
     * JobsEngine.workShiftWithScore.
     */
    val miniGameImplemented: Boolean
        get() = when (this) {
            POLICE_OFFICER, FIREFIGHTER, BAKER, CHEF,
            TAXI_DRIVER, CAR_MECHANIC, PROGRAMMER, DETECTIVE,
            BOXER, FISHERMAN, FOOTBALL_PLAYER, STREAMER,
            PAINTER, PHARMACIST, TEACHER, FARMER,
            LIBRARIAN, DENTIST, POSTMAN, ICE_CREAM_SELLER,
            PARAMEDIC, K9_OFFICER, ACTOR,
            PIZZAIOLO, BARISTA, GARBAGE_COLLECTOR,
            BRICKLAYER, CARPENTER, PLUMBER -> true
            else -> false
        }
}

/** Estadística por oficio para el jugador. */
@Serializable
data class JobProgress(
    val jobName: String,
    /** Nivel del oficio 1..50. */
    val level: Int = 1,
    /** XP acumulada en el nivel actual. Cada 100 = +1 nivel. */
    val xpInLevel: Int = 0,
    /** Total lifetime de turnos trabajados. */
    val shiftsWorked: Int = 0,
    /** Total lifetime de cash ganado. */
    val totalEarned: Double = 0.0,
    /** Tick del último turno (para enfriar fatiga, futuro). */
    val lastShiftTick: Long = 0L,
    /** Si el jugador llegó a desbloquearlo alguna vez. */
    val unlocked: Boolean = false
)

/** Estado serializable del subsistema Jobs. */
@Serializable
data class JobsState(
    /** Si el jugador ha aceptado el sistema (tutorial mínimo). */
    val accepted: Boolean = false,
    /** Mapa por jobId.name. */
    val progress: Map<String, JobProgress> = emptyMap(),
    /** Total de turnos trabajados en TODOS los oficios. */
    val totalShifts: Int = 0,
    /** Total lifetime de cash ganado por trabajar. */
    val totalEarned: Double = 0.0,
    /** Últimos turnos (para feed/historial). */
    val recentShifts: List<JobShiftResult> = emptyList()
) {
    fun progressOf(job: JobId): JobProgress =
        progress[job.name] ?: JobProgress(jobName = job.name)
}

/** Resultado de un turno trabajado. */
@Serializable
data class JobShiftResult(
    val jobName: String,
    val cashEarned: Double,
    val xpGained: Int,
    val level: Int,
    val day: Int = 0
)
