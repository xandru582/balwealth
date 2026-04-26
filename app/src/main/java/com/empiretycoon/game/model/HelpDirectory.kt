package com.empiretycoon.game.model

/**
 * Temas de ayuda contextual disponibles dentro del juego. Cada pantalla
 * puede mostrar un botón "?" que abre la ficha del tema correspondiente.
 */
enum class HelpTopic(val displayName: String, val emoji: String) {
    OVERVIEW("Visión general", "🧭"),
    BUILDINGS("Edificios", "🏭"),
    RECIPES("Recetas", "📜"),
    EMPLOYEES("Empleados", "👥"),
    MARKET("Mercado", "🛒"),
    RESEARCH("Investigación", "🧪"),
    STOCKS("Bolsa", "📈"),
    REAL_ESTATE("Inmuebles", "🏘"),
    EVENTS("Eventos", "✨"),
    QUESTS("Misiones", "🎯"),
    PRESTIGE("Prestigio", "🏆")
}

/**
 * Contenido textual asociado a cada tema. Castellano cercano y conciso,
 * 1-2 párrafos por tema. Pensado para mostrarse en un BottomSheet.
 */
object HelpContent {
    val text: Map<HelpTopic, String> = mapOf(
        HelpTopic.OVERVIEW to (
            "Empire Tycoon es un juego de imperio empresarial. Construyes edificios, " +
                "asignas recetas que transforman recursos, contratas empleados y vendes " +
                "los bienes que produces en el mercado.\n\n" +
                "Aparte del negocio, hay una capa RPG: tu personaje sube de nivel, mejora " +
                "atributos y gestiona dinero personal. A medida que creces se abren la " +
                "investigación, la bolsa, el mercado inmobiliario y la salida a bolsa de " +
                "tu propia compañía."
        ),
        HelpTopic.BUILDINGS to (
            "Cada edificio (Granja, Aserradero, Fábrica…) consume recursos y produce otros " +
                "según la receta asignada. Los edificios tienen niveles que aumentan su " +
                "productividad y la capacidad de trabajadores.\n\n" +
                "Para que un edificio produzca necesita: una receta asignada, los insumos " +
                "necesarios en el inventario y al menos un trabajador asignado. Pulsa " +
                "Mejorar para subir su nivel, o Vender para recuperar parte de la inversión."
        ),
        HelpTopic.RECIPES to (
            "Las recetas definen qué transforma cada edificio: insumos -> producto en " +
                "X segundos. Algunas recetas requieren tecnologías investigadas previamente.\n\n" +
                "Activa Auto ON para que la receta vuelva a empezar al terminar cada ciclo. " +
                "Si te quedas sin insumos el edificio se detiene; cómpralos en el mercado " +
                "o produce los intermedios en otros edificios."
        ),
        HelpTopic.EMPLOYEES to (
            "Los empleados son trabajadores que asignas a tus edificios. Cada uno tiene " +
                "una skill (productividad), lealtad y un sueldo mensual.\n\n" +
                "Refresca el listado de candidatos para ver caras nuevas. Ficha pagando una " +
                "prima de fichaje (50% del sueldo). Despedir cuesta una indemnización y " +
                "perjudica ligeramente tu reputación."
        ),
        HelpTopic.MARKET to (
            "El mercado tiene precios dinámicos: factores entre 0,55 y 1,8 que oscilan con " +
                "la oferta y la demanda. Compras al precio +15% y vendes al precio -10%.\n\n" +
                "Vender mucho de un mismo bien hunde su precio temporalmente; comprar lo " +
                "encarece. Diversifica para sacar partido de los picos. Tu carisma y las " +
                "tecnologías de marketing te dan bonificaciones extra a la venta."
        ),
        HelpTopic.RESEARCH to (
            "La investigación gasta caja y tiempo, pero a cambio desbloquea recetas, " +
                "multiplicadores de producción y bonos de venta. Solo puedes investigar " +
                "una tecnología a la vez.\n\n" +
                "Las tecnologías tienen prerrequisitos: Polímeros requiere Metalurgia, " +
                "Semiconductores requiere Polímeros, etc. Tu Inteligencia acelera la " +
                "investigación: cada punto reduce el tiempo de ciclo."
        ),
        HelpTopic.STOCKS to (
            "La bolsa te permite invertir el capital corporativo en empresas cotizadas. " +
                "Cada acción tiene precio, volatilidad y tendencia. Compra barato, vende " +
                "caro… o aguanta para diversificar.\n\n" +
                "Tu Suerte mejora marginalmente los resultados aleatorios del mercado. " +
                "Cuidado con la volatilidad alta: las recompensas son mayores, pero también " +
                "las pérdidas potenciales."
        ),
        HelpTopic.REAL_ESTATE to (
            "Los inmuebles generan rentas diarias pasivas. Pisos, casas, locales y " +
                "rascacielos tienen distintos precios y rendimientos. La renta neta es la " +
                "que cobras tras descontar mantenimiento.\n\n" +
                "Refresca el catálogo para ver nuevas ofertas. Vender una propiedad antes " +
                "de tiempo te devuelve solo el 92% del precio: piensa a largo plazo."
        ),
        HelpTopic.EVENTS to (
            "Los eventos son situaciones aleatorias que aparecen como diálogos modales. " +
                "Cada evento tiene 2-3 opciones con consecuencias distintas: dinero, " +
                "reputación, energía, inventario o XP.\n\n" +
                "No hay decisiones obviamente buenas: lo que parece barato a corto plazo " +
                "puede pasar factura, y a la inversa. Lee bien antes de elegir."
        ),
        HelpTopic.QUESTS to (
            "Las misiones son objetivos de progreso (construir N edificios, ganar X dinero, " +
                "investigar Y tecnologías). Se autocompletan al cumplirse.\n\n" +
                "Cuando aparezca el botón Reclamar, pulsa para cobrar la recompensa: caja, " +
                "reputación y XP de personaje. Mantén un ojo en el panel para no dejar " +
                "premios olvidados."
        ),
        HelpTopic.PRESTIGE to (
            "Cuando tu imperio sea lo bastante grande podrás 'prestigiar': reinicias el " +
                "progreso a cambio de un multiplicador permanente y desbloqueos exclusivos.\n\n" +
                "Es una decisión estratégica: prestigia cuando el ritmo de progreso se " +
                "estanque, no en pleno crecimiento. El bono se acumula entre prestigios."
        )
    )

    fun get(topic: HelpTopic): String =
        text[topic] ?: "Sin contenido disponible para este tema."
}
