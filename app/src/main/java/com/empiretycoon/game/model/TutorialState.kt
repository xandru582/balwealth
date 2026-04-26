package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Pasos del tutorial guiado de Empire Tycoon. El orden refleja el flujo
 * recomendado para que un nuevo jugador aprenda los sistemas de juego.
 *
 * El motor avanza al siguiente paso cuando se cumple la `AdvanceCondition`
 * declarada en el `TutorialSpec` correspondiente.
 */
enum class TutorialStep {
    /** Bienvenida inicial. Mostramos un diálogo modal. */
    WELCOME,
    /** Pedimos al jugador que abra la pestaña 'Imperio'. */
    OPEN_EMPIRE,
    /** Pedimos construir el primer edificio. */
    BUILD_FIRST,
    /** Asignar una receta al edificio recién construido. */
    ASSIGN_RECIPE,
    /** Contratar el primer empleado. */
    HIRE_FIRST,
    /** Asignar el empleado a un edificio (+1 trabajador). */
    ASSIGN_WORKER,
    /** Cambiar a la pestaña 'Mercado'. */
    OPEN_MARKET,
    /** Vender bienes en el mercado para conseguir caja. */
    SELL_GOODS,
    /** Abrir la pestaña 'Ciencia'. */
    OPEN_RESEARCH,
    /** Iniciar la primera tecnología. */
    START_RESEARCH,
    /** Abrir la pestaña 'Tú' (jugador). */
    OPEN_PLAYER,
    /** Entrenar una estadística personal. */
    TRAIN_STAT,
    /** Abrir la pestaña 'Patrimonio'. */
    OPEN_WEALTH,
    /** Comprar la primera acción de bolsa. */
    BUY_STOCK,
    /** Comprar el primer inmueble. */
    BUY_PROPERTY,
    /** Tutorial completado. */
    FINISHED
}

/**
 * Estado serializable del tutorial. Forma parte del save game.
 *
 * - `currentStep`: paso activo en este momento.
 * - `completedSteps`: pasos ya cumplidos (idempotente).
 * - `skipped`: el jugador ha pedido saltar todo el tutorial.
 * - `dismissCount`: nº de veces que se ha cerrado el coachmark sin avanzar
 *    (útil para mostrar el botón "Saltar tutorial" tras varias dudas).
 */
@Serializable
data class TutorialState(
    val currentStep: TutorialStep = TutorialStep.WELCOME,
    val completedSteps: Set<TutorialStep> = emptySet(),
    val skipped: Boolean = false,
    val dismissCount: Int = 0
) {
    val isFinished: Boolean get() = currentStep == TutorialStep.FINISHED
    val isActive: Boolean get() = !skipped && !isFinished
}

/**
 * Condición que dispara el avance al siguiente paso del tutorial.
 * El [com.empiretycoon.game.engine.TutorialEngine] inspecciona las diferencias
 * entre el estado anterior y el nuevo tras cada comando para decidir.
 */
enum class AdvanceCondition {
    /** El jugador toca el botón principal del coachmark. */
    TAP_PRIMARY,
    /** El jugador navega a la pestaña indicada en `targetTab`. */
    TAP_TAB,
    /** El número de edificios aumenta. */
    BUILT_BUILDING,
    /** Algún edificio pasa de no tener receta a tenerla. */
    RECIPE_ASSIGNED,
    /** El número de empleados contratados aumenta. */
    EMPLOYEE_HIRED,
    /** Algún edificio aumenta su número de trabajadores asignados. */
    WORKER_ASSIGNED,
    /** Se ejecuta una transacción de mercado (compra o venta). */
    MARKET_TX,
    /** Se inicia una nueva investigación (`research.inProgressId`). */
    RESEARCH_STARTED,
    /** Una stat del personaje aumenta (entrenamiento). */
    STAT_TRAINED,
    /** Aumentan las acciones de algún ticker. */
    STOCK_BOUGHT,
    /** Aumenta la lista de propiedades en propiedad. */
    PROPERTY_BOUGHT
}

/**
 * Definición narrativa de cada paso. Las `targetWidgetId` se cruzan con la
 * `AnchorRegistry` para dibujar el spotlight encima del widget concreto.
 */
data class TutorialSpec(
    val step: TutorialStep,
    val title: String,
    val message: String,
    val targetTab: String? = null,
    val targetWidgetId: String? = null,
    val dismissable: Boolean = true,
    val primaryAction: String = "Entendido",
    val advanceCondition: AdvanceCondition = AdvanceCondition.TAP_PRIMARY
)

/**
 * Guion completo del tutorial. Castellano cercano y accionable, con verbos
 * imperativos en cada mensaje para indicar exactamente qué hacer.
 */
object TutorialScript {

    /** Identificadores de tabs (deben coincidir con `Tab.id` en `Root.kt`). */
    object Tabs {
        const val HOME = "home"
        const val EMPIRE = "fact"
        const val MARKET = "market"
        const val RESEARCH = "research"
        const val WEALTH = "wealth"
        const val PLAYER = "player"
        const val MORE = "more"
    }

    /** Identificadores de anclas para `Modifier.anchor(id)`. */
    object Widgets {
        const val TAB_EMPIRE = "tab_empire"
        const val TAB_MARKET = "tab_market"
        const val TAB_RESEARCH = "tab_research"
        const val TAB_PLAYER = "tab_player"
        const val TAB_WEALTH = "tab_wealth"

        const val BUILD_BUTTON = "empire_build_button"
        const val ASSIGN_RECIPE_BUTTON = "empire_assign_recipe"
        const val HIRE_BUTTON = "empire_hire_button"
        const val ADD_WORKER_BUTTON = "empire_add_worker"

        const val MARKET_SELL_BUTTON = "market_sell_button"

        const val RESEARCH_START_BUTTON = "research_start_button"

        const val PLAYER_TRAIN_BUTTON = "player_train_button"

        const val WEALTH_BUY_STOCK = "wealth_buy_stock"
        const val WEALTH_BUY_PROPERTY = "wealth_buy_property"
    }

    val steps: List<TutorialSpec> = listOf(
        TutorialSpec(
            step = TutorialStep.WELCOME,
            title = "¡Bienvenido a Empire Tycoon!",
            message = "Vas a construir un imperio empresarial desde cero. Te enseñaré las claves en pocos pasos. Pulsa Empezar para comenzar.",
            primaryAction = "Empezar",
            advanceCondition = AdvanceCondition.TAP_PRIMARY
        ),
        TutorialSpec(
            step = TutorialStep.OPEN_EMPIRE,
            title = "Tu primer paso: el imperio",
            message = "Toca la pestaña Imperio en la barra inferior. Ahí podrás construir edificios, asignar recetas y gestionar tu plantilla.",
            targetTab = Tabs.EMPIRE,
            targetWidgetId = Widgets.TAB_EMPIRE,
            primaryAction = "Ir a Imperio",
            advanceCondition = AdvanceCondition.TAP_TAB
        ),
        TutorialSpec(
            step = TutorialStep.BUILD_FIRST,
            title = "Levanta tu primer edificio",
            message = "En la sección Construir verás tipos de edificios. Una Granja es ideal para empezar: pulsa Construir y comprométete con tu futuro imperio.",
            targetTab = Tabs.EMPIRE,
            targetWidgetId = Widgets.BUILD_BUTTON,
            primaryAction = "Construir",
            advanceCondition = AdvanceCondition.BUILT_BUILDING
        ),
        TutorialSpec(
            step = TutorialStep.ASSIGN_RECIPE,
            title = "Pon a producir tu edificio",
            message = "Vuelve a Edificios y asigna una receta al que acabas de construir. Sin receta, no produce nada.",
            targetTab = Tabs.EMPIRE,
            targetWidgetId = Widgets.ASSIGN_RECIPE_BUTTON,
            primaryAction = "Asignar receta",
            advanceCondition = AdvanceCondition.RECIPE_ASSIGNED
        ),
        TutorialSpec(
            step = TutorialStep.HIRE_FIRST,
            title = "Contrata a tu primer empleado",
            message = "Sin manos, no hay producción. Ve a Empleados y ficha al primer candidato pulsando Fichar.",
            targetTab = Tabs.EMPIRE,
            targetWidgetId = Widgets.HIRE_BUTTON,
            primaryAction = "Fichar",
            advanceCondition = AdvanceCondition.EMPLOYEE_HIRED
        ),
        TutorialSpec(
            step = TutorialStep.ASSIGN_WORKER,
            title = "Asigna trabajadores al edificio",
            message = "Vuelve a Edificios y pulsa +1 en la tarjeta del edificio para enviar al empleado a trabajar.",
            targetTab = Tabs.EMPIRE,
            targetWidgetId = Widgets.ADD_WORKER_BUTTON,
            primaryAction = "Asignar",
            advanceCondition = AdvanceCondition.WORKER_ASSIGNED
        ),
        TutorialSpec(
            step = TutorialStep.OPEN_MARKET,
            title = "Hora de vender",
            message = "Cuando produzcas mercancía la verás en tu inventario. Abre la pestaña Mercado para venderla.",
            targetTab = Tabs.MARKET,
            targetWidgetId = Widgets.TAB_MARKET,
            primaryAction = "Ir al Mercado",
            advanceCondition = AdvanceCondition.TAP_TAB
        ),
        TutorialSpec(
            step = TutorialStep.SELL_GOODS,
            title = "Vende y haz caja",
            message = "Despliega un recurso con stock y pulsa una opción de Vender. Pequeñas ventas constantes engrasan la economía.",
            targetTab = Tabs.MARKET,
            targetWidgetId = Widgets.MARKET_SELL_BUTTON,
            primaryAction = "Vender",
            advanceCondition = AdvanceCondition.MARKET_TX
        ),
        TutorialSpec(
            step = TutorialStep.OPEN_RESEARCH,
            title = "Investigación: tu ventaja",
            message = "Las tecnologías desbloquean recetas y multiplicadores. Ve a la pestaña Ciencia.",
            targetTab = Tabs.RESEARCH,
            targetWidgetId = Widgets.TAB_RESEARCH,
            primaryAction = "Ir a Ciencia",
            advanceCondition = AdvanceCondition.TAP_TAB
        ),
        TutorialSpec(
            step = TutorialStep.START_RESEARCH,
            title = "Empieza a investigar",
            message = "Elige una tecnología disponible y pulsa Investigar. Pagarás un coste y la barra de progreso comenzará.",
            targetTab = Tabs.RESEARCH,
            targetWidgetId = Widgets.RESEARCH_START_BUTTON,
            primaryAction = "Investigar",
            advanceCondition = AdvanceCondition.RESEARCH_STARTED
        ),
        TutorialSpec(
            step = TutorialStep.OPEN_PLAYER,
            title = "También cuentas tú",
            message = "Tu personaje crece como cualquier RPG. Abre la pestaña Tú para verlo.",
            targetTab = Tabs.PLAYER,
            targetWidgetId = Widgets.TAB_PLAYER,
            primaryAction = "Ir a Tú",
            advanceCondition = AdvanceCondition.TAP_TAB
        ),
        TutorialSpec(
            step = TutorialStep.TRAIN_STAT,
            title = "Entrena una stat",
            message = "Gasta energía para subir Inteligencia, Carisma o cualquier otra. Cada stat impacta a un sistema del juego.",
            targetTab = Tabs.PLAYER,
            targetWidgetId = Widgets.PLAYER_TRAIN_BUTTON,
            primaryAction = "Entrenar",
            advanceCondition = AdvanceCondition.STAT_TRAINED
        ),
        TutorialSpec(
            step = TutorialStep.OPEN_WEALTH,
            title = "Diversifica tu patrimonio",
            message = "Acciones e inmuebles te dan ingresos pasivos. Ve a la pestaña Patrimonio.",
            targetTab = Tabs.WEALTH,
            targetWidgetId = Widgets.TAB_WEALTH,
            primaryAction = "Ir a Patrimonio",
            advanceCondition = AdvanceCondition.TAP_TAB
        ),
        TutorialSpec(
            step = TutorialStep.BUY_STOCK,
            title = "Compra tu primera acción",
            message = "Elige una empresa cotizada y pulsa Comprar. Si su precio sube, ganarás capital. Si baja, paciencia.",
            targetTab = Tabs.WEALTH,
            targetWidgetId = Widgets.WEALTH_BUY_STOCK,
            primaryAction = "Comprar",
            advanceCondition = AdvanceCondition.STOCK_BOUGHT
        ),
        TutorialSpec(
            step = TutorialStep.BUY_PROPERTY,
            title = "Adquiere un inmueble",
            message = "Las propiedades generan rentas diarias. Pulsa Comprar en alguna oferta del catálogo inmobiliario.",
            targetTab = Tabs.WEALTH,
            targetWidgetId = Widgets.WEALTH_BUY_PROPERTY,
            primaryAction = "Comprar",
            advanceCondition = AdvanceCondition.PROPERTY_BOUGHT
        ),
        TutorialSpec(
            step = TutorialStep.FINISHED,
            title = "¡Has completado el tutorial!",
            message = "Ya conoces los pilares del juego. Ahora optimiza, expande y conviértete en un magnate. Buena suerte.",
            primaryAction = "Empezar a jugar",
            dismissable = false,
            advanceCondition = AdvanceCondition.TAP_PRIMARY
        )
    )

    /** Devuelve la spec de un paso concreto, o la del WELCOME por defecto. */
    fun specOf(step: TutorialStep): TutorialSpec =
        steps.find { it.step == step } ?: steps.first()
}
