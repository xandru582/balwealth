package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/** Elección de un evento con efectos cuantitativos sobre el estado. */
@Serializable
data class EventChoice(
    val label: String,
    val cashDelta: Double = 0.0,
    val reputationDelta: Int = 0,
    val happinessDelta: Int = 0,
    val energyDelta: Int = 0,
    val xpDelta: Long = 0,
    val inventoryDelta: Map<String, Int> = emptyMap(),
    /** Mensaje a mostrar tras elegir. */
    val message: String = ""
)

@Serializable
data class GameEvent(
    val id: String,
    val title: String,
    val description: String,
    val choices: List<EventChoice>,
    val icon: String = "\uD83D\uDCCB"
)

object EventPool {
    val pool: List<GameEvent> = listOf(
        GameEvent(
            id = "bank_offer", title = "Oferta del banco",
            description = "Un banquero te ofrece 50.000 con 3% de interés.",
            icon = "\uD83C\uDFE6",
            choices = listOf(
                EventChoice("Aceptar préstamo", cashDelta = 50_000.0, reputationDelta = -2, message = "Préstamo aceptado."),
                EventChoice("Rechazar", message = "Prefieres crecer con recursos propios.")
            )
        ),
        GameEvent(
            id = "journalist", title = "Periodista en la puerta",
            description = "Un periodista quiere una entrevista. Puede ir bien… o muy mal.",
            icon = "\uD83E\uDDD1\u200D\uD83D\uDCBB",
            choices = listOf(
                EventChoice("Conceder entrevista", reputationDelta = 5, energyDelta = -10, message = "La entrevista se viraliza positivamente."),
                EventChoice("Declinar", message = "Sin riesgos, sin recompensas.")
            )
        ),
        GameEvent(
            id = "feria", title = "Feria del sector",
            description = "Puedes montar un stand por 3.500.",
            icon = "\uD83C\uDFAA",
            choices = listOf(
                EventChoice("Reservar stand", cashDelta = -3_500.0, reputationDelta = 6, xpDelta = 200, message = "Nuevos contactos y visibilidad."),
                EventChoice("Pasar", message = "Otra vez será.")
            )
        ),
        GameEvent(
            id = "sabotage", title = "Sabotaje en la línea",
            description = "Un rumor habla de sabotaje. Investigar cuesta energía.",
            icon = "\uD83D\uDEE1\uFE0F",
            choices = listOf(
                EventChoice("Investigar a fondo", energyDelta = -25, reputationDelta = 3, message = "Detectas al culpable y cierras la brecha."),
                EventChoice("Ignorar", happinessDelta = -10, message = "Te preocupa pero decides esperar.")
            )
        ),
        GameEvent(
            id = "market_crash", title = "Miedo en los mercados",
            description = "Hay caídas en cadena en el mercado de materias primas.",
            icon = "\uD83D\uDCC9",
            choices = listOf(
                EventChoice("Vender stock rápido", cashDelta = 2_500.0, message = "Liquidas parte del stock para cubrirte."),
                EventChoice("Mantener posiciones", message = "Apuestas por la recuperación.")
            )
        ),
        GameEvent(
            id = "charity", title = "Causa benéfica",
            description = "Una ONG pide una donación.",
            icon = "\uD83D\uDC96",
            choices = listOf(
                EventChoice("Donar 1.000", cashDelta = -1_000.0, reputationDelta = 4, happinessDelta = 6, message = "La prensa aplaude el gesto."),
                EventChoice("Donar 5.000", cashDelta = -5_000.0, reputationDelta = 10, happinessDelta = 12, message = "Te conviertes en imagen de la causa."),
                EventChoice("No donar", happinessDelta = -3, message = "Quizá más adelante.")
            )
        ),
        GameEvent(
            id = "lottery", title = "Ticket olvidado",
            description = "Encuentras un boleto de lotería en el bolsillo.",
            icon = "\uD83C\uDFAB",
            choices = listOf(
                EventChoice("Comprobar", cashDelta = 1_250.0, happinessDelta = 4, message = "¡Premio menor! 1.250 a tu bolsillo."),
                EventChoice("Tirarlo", message = "No crees en la suerte.")
            )
        ),
        GameEvent(
            id = "poaching", title = "Talento fichado",
            description = "Otra empresa intenta llevarse a tu plantilla con ofertas mejores.",
            icon = "\uD83E\uDD1D",
            choices = listOf(
                EventChoice("Subir salarios", cashDelta = -3_000.0, reputationDelta = 2, message = "Tu equipo se siente valorado."),
                EventChoice("No ceder", reputationDelta = -4, message = "Algunos empleados bajan su lealtad.")
            )
        )
    )
}
