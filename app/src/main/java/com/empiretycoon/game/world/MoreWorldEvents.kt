package com.empiretycoon.game.world

/**
 * Pool extendido de eventos del mundo. Sumados a [WorldEventCatalog.all]
 * mediante [MoreEventsCatalog.merged]. 38 eventos adicionales con sabor
 * narrativo: encuentros, dilemas morales, oportunidades, misterio.
 */
object MoreEventsCatalog {

    val extra: List<WorldEvent> = listOf(
        WorldEvent("we_lottery_big", "Boleto premiado", "Encuentras un cupón premiado en una papelera. Vale 5.000 €.", "🎟️",
            listOf(
                WorldEventChoice("Cobrarlo", cashDelta = 5_000.0, karmaDelta = -1, resultMessage = "5.000 € al bolsillo. Karma -1 por no buscar al dueño."),
                WorldEventChoice("Buscar al dueño", reputationDelta = +6, karmaDelta = +5, resultMessage = "El dueño te llora de gratitud. Eres famoso por un día.")
            )),
        WorldEvent("we_pickpocket", "Carterista", "Notas que alguien intenta meterte la mano en el bolsillo.", "🦹",
            listOf(
                WorldEventChoice("Atraparle (gym test)", energyDelta = -15, cashDelta = 50.0, karmaDelta = +1, resultMessage = "Le inmovilizas y la policía te recompensa."),
                WorldEventChoice("Dejarlo escapar", cashDelta = -120.0, karmaDelta = +1, resultMessage = "Pierdes 120 € pero quizá necesitaba más.")
            )),
        WorldEvent("we_homeless_kid", "Niño de la calle", "Un niño descalzo te mira con la mano tendida.", "👦",
            listOf(
                WorldEventChoice("Comprarle zapatos (200 €)", cashDelta = -200.0, karmaDelta = +12, happinessDelta = +15, resultMessage = "Sus ojos brillan al estrenarlos."),
                WorldEventChoice("Darle dinero (50 €)", cashDelta = -50.0, karmaDelta = +5, resultMessage = "Lo guarda en el calcetín."),
                WorldEventChoice("Apartarle", karmaDelta = -8, happinessDelta = -5, reputationDelta = -3, resultMessage = "Te sientes mal contigo mismo.")
            )),
        WorldEvent("we_rich_offer", "Oferta turbia", "Un trajeado te ofrece 10.000 € por información sobre tu rival.", "💼",
            listOf(
                WorldEventChoice("Aceptar", cashDelta = 10_000.0, karmaDelta = -10, reputationDelta = -5, resultMessage = "El sobre cambia de manos."),
                WorldEventChoice("Negarse", karmaDelta = +3, reputationDelta = +2, resultMessage = "Hay cosas que no se compran.")
            )),
        WorldEvent("we_old_lady", "Anciana en el cruce", "Una abuela duda en cruzar la calle.", "👵",
            listOf(
                WorldEventChoice("Ayudarla", energyDelta = -3, karmaDelta = +3, happinessDelta = +5, resultMessage = "Te llama 'mi alma'."),
                WorldEventChoice("Continuar", karmaDelta = -1, resultMessage = "Llegas tarde igualmente.")
            )),
        WorldEvent("we_street_chess", "Partida de ajedrez", "Un viejo en un parque te reta a una partida.", "♟️",
            listOf(
                WorldEventChoice("Jugar (gana)", happinessDelta = +6, xpDelta = 50, resultMessage = "Le ganas en 18 jugadas."),
                WorldEventChoice("Pasar", resultMessage = "Continúas tu camino.")
            )),
        WorldEvent("we_food_truck", "Food truck", "Un food truck huele increíble. Cuesta 25 €.", "🌮",
            listOf(
                WorldEventChoice("Comer", cashDelta = -25.0, energyDelta = +20, happinessDelta = +6, resultMessage = "El mejor taco de tu vida."),
                WorldEventChoice("No tengo hambre", resultMessage = "Sigues tu camino.")
            )),
        WorldEvent("we_lost_dog_owner", "Cartel de perro perdido", "Ofrecen 500 € por encontrar a 'Lulu', un caniche blanco.", "🐩",
            listOf(
                WorldEventChoice("Buscarla", energyDelta = -20, cashDelta = 500.0, karmaDelta = +5, resultMessage = "La encuentras en una alcantarilla. Estaba bien."),
                WorldEventChoice("No es mi problema", karmaDelta = -1, resultMessage = "Continúas.")
            )),
        WorldEvent("we_film_extra", "Rodaje en la calle", "Un director te ofrece 200 € para ser extra en una escena.", "🎬",
            listOf(
                WorldEventChoice("Aceptar", cashDelta = 200.0, energyDelta = -10, happinessDelta = +5, xpDelta = 20, resultMessage = "Tres tomas y a tomar café."),
                WorldEventChoice("No tengo tiempo", resultMessage = "El director encoge los hombros.")
            )),
        WorldEvent("we_secret_door", "Puerta misteriosa", "Una puerta sin rótulo, llamas... y se abre.", "🚪",
            listOf(
                WorldEventChoice("Entrar", happinessDelta = +3, xpDelta = 50, karmaDelta = +1, resultMessage = "Es una sociedad secreta filántropa. Te dan un pin."),
                WorldEventChoice("Marcharse", resultMessage = "El misterio queda.")
            )),
        WorldEvent("we_protest_anti", "Protesta contra ti", "Una pequeña protesta lleva carteles con tu cara tachada.", "📣",
            listOf(
                WorldEventChoice("Hablar con ellos", reputationDelta = +3, karmaDelta = +3, energyDelta = -10, resultMessage = "Te escuchan. Quizá no eres el demonio que pintan."),
                WorldEventChoice("Llamar a seguridad", reputationDelta = -8, karmaDelta = -5, resultMessage = "La protesta crece tras tu reacción."),
                WorldEventChoice("Pasar discreto", reputationDelta = -1, resultMessage = "Sigues caminando.")
            )),
        WorldEvent("we_charity_marathon", "Maratón benéfica", "Te invitan a participar 'por la cara'. 100 € por kilómetro.", "🏃",
            listOf(
                WorldEventChoice("10 km (1.000 €)", cashDelta = -1_000.0, karmaDelta = +6, reputationDelta = +5, energyDelta = -30, resultMessage = "Tres días sin poder bajar escaleras. Pero feliz."),
                WorldEventChoice("5 km (500 €)", cashDelta = -500.0, karmaDelta = +3, reputationDelta = +3, energyDelta = -15, resultMessage = "Suficiente."),
                WorldEventChoice("Pasar", reputationDelta = -1, resultMessage = "Otra vez será.")
            )),
        WorldEvent("we_paparazzi", "Paparazzi al acecho", "Un fotógrafo te sigue tomando fotos.", "📸",
            listOf(
                WorldEventChoice("Posar", reputationDelta = +4, happinessDelta = +3, resultMessage = "Saldrás en la prensa rosa."),
                WorldEventChoice("Romperle la cámara (200 €)", cashDelta = -200.0, reputationDelta = -8, karmaDelta = -3, resultMessage = "Pleito en marcha."),
                WorldEventChoice("Esconderte", resultMessage = "Te metes en una tienda. Sale por la puerta de atrás.")
            )),
        WorldEvent("we_tax_audit", "Inspección sorpresa", "Un inspector llega con una carpeta gruesa.", "📋",
            listOf(
                WorldEventChoice("Cooperar plenamente", energyDelta = -25, karmaDelta = +2, reputationDelta = +1, resultMessage = "Pasas la inspección. Limpio."),
                WorldEventChoice("Sobornar (1.500 €)", cashDelta = -1_500.0, karmaDelta = -8, resultMessage = "El sobre cambia de manos. Mira hacia otro lado.")
            )),
        WorldEvent("we_tinder_match", "Match en la app", "Tu cita responde: ¿Tomamos algo?", "💕",
            listOf(
                WorldEventChoice("Café (30 €)", cashDelta = -30.0, happinessDelta = +12, energyDelta = -10, xpDelta = 30, resultMessage = "Buena conversación. Quizá hay segunda cita."),
                WorldEventChoice("Cena cara (250 €)", cashDelta = -250.0, happinessDelta = +20, energyDelta = -20, xpDelta = 50, resultMessage = "Una velada inolvidable."),
                WorldEventChoice("Cancelar", happinessDelta = -3, resultMessage = "No estabas para citas.")
            )),
        WorldEvent("we_yacht_party", "Fiesta de barco", "Un yate en el puerto te invita a subir.", "⛵",
            listOf(
                WorldEventChoice("Subirse", energyDelta = -25, happinessDelta = +18, reputationDelta = +5, karmaDelta = -2, resultMessage = "Champán, vistas y resaca."),
                WorldEventChoice("Educadamente no", resultMessage = "El sector finanzas te llama.")
            )),
        WorldEvent("we_gym_challenge", "Reto en el gimnasio", "Un culturista te reta a un pulso (apuesta 200 €).", "💪",
            listOf(
                WorldEventChoice("Aceptar (puede salir mal)", cashDelta = -200.0, energyDelta = -15, happinessDelta = -3, resultMessage = "Pierdes en 4 segundos. Tu codo cruje."),
                WorldEventChoice("Reírte y pasar", resultMessage = "El culturista te aplaude por sensato.")
            )),
        WorldEvent("we_artist_commission", "Comisión artística", "Un pintor famoso te ofrece un retrato por 3.000 €.", "🖼️",
            listOf(
                WorldEventChoice("Encargar", cashDelta = -3_000.0, reputationDelta = +6, happinessDelta = +12, resultMessage = "Será una obra de arte familiar."),
                WorldEventChoice("Demasiado caro", resultMessage = "Quizá en otra ocasión.")
            )),
        WorldEvent("we_sad_clown", "Payaso triste", "Un payaso pide ayuda. Su acto no funciona.", "🤡",
            listOf(
                WorldEventChoice("Reírte fuerte (mentira piadosa)", happinessDelta = -2, karmaDelta = +3, resultMessage = "Le cambias el día."),
                WorldEventChoice("Darle 50 € de propina", cashDelta = -50.0, karmaDelta = +5, happinessDelta = +5, resultMessage = "Llora de gratitud."),
                WorldEventChoice("Ignorar", karmaDelta = -2, resultMessage = "El payaso baja la cabeza.")
            )),
        WorldEvent("we_celebrity_chef", "Chef famoso", "Un chef Michelin te invita a probar un plato experimental gratis.", "👨‍🍳",
            listOf(
                WorldEventChoice("Probar", happinessDelta = +10, energyDelta = +15, xpDelta = 30, resultMessage = "Es la mejor experiencia gastronómica de tu vida."),
                WorldEventChoice("Educadamente no", resultMessage = "Era el momento perfecto.")
            )),
        WorldEvent("we_lottery_friend", "Amigo afortunado", "Un viejo amigo te llama: 'He ganado la lotería! Ven a celebrarlo.'", "🎉",
            listOf(
                WorldEventChoice("Ir (regalo 200 €)", cashDelta = -200.0, happinessDelta = +12, karmaDelta = +2, resultMessage = "La fiesta del año."),
                WorldEventChoice("No puedo", happinessDelta = -3, resultMessage = "Le mandas un mensaje frío.")
            )),
        WorldEvent("we_book_signing", "Firma de libros", "Un autor que te admira te firma su libro gratis.", "📚",
            listOf(
                WorldEventChoice("Aceptarlo", happinessDelta = +5, xpDelta = 20, resultMessage = "Lo dedica a 'el visionario que cambió mi pueblo'."),
                WorldEventChoice("No leo", resultMessage = "Sigues caminando.")
            )),
        WorldEvent("we_drug_dealer", "Trapicheo", "Un tipo te ofrece sustancias ilegales con 'descuento'.", "💊",
            listOf(
                WorldEventChoice("Comprar (riesgo)", cashDelta = -300.0, karmaDelta = -8, energyDelta = +15, happinessDelta = +5, resultMessage = "Pasas una noche extraña. La policía vino al barrio."),
                WorldEventChoice("Pasar (denunciar)", reputationDelta = +3, karmaDelta = +3, resultMessage = "Llamas a la policía discretamente.")
            )),
        WorldEvent("we_flashmob", "Flashmob inesperado", "De repente todo el mundo en la plaza empieza a bailar.", "💃",
            listOf(
                WorldEventChoice("Unirte", happinessDelta = +12, energyDelta = -10, karmaDelta = +1, resultMessage = "Te grabas en el video viral del día."),
                WorldEventChoice("Mirar y disfrutar", happinessDelta = +5, resultMessage = "Pones tu mejor sonrisa.")
            )),
        WorldEvent("we_business_card", "Tarjeta misteriosa", "Encuentras una tarjeta sin nombre, solo un número de teléfono.", "📇",
            listOf(
                WorldEventChoice("Llamar", energyDelta = -5, xpDelta = 30, resultMessage = "Una voz dice: 'Te estábamos esperando.' Cuelga."),
                WorldEventChoice("Tirarla", resultMessage = "La basura está cerca.")
            )),
        WorldEvent("we_orphan_school", "Escuela de huérfanos", "La directora te recibe: 'Necesitamos un patrocinador.'", "🏫",
            listOf(
                WorldEventChoice("Donar 5.000 €", cashDelta = -5_000.0, karmaDelta = +12, reputationDelta = +8, happinessDelta = +15, resultMessage = "Pondrán tu nombre en el aula nueva."),
                WorldEventChoice("Donar 1.000 €", cashDelta = -1_000.0, karmaDelta = +5, reputationDelta = +3, resultMessage = "Apreciado pero modesto."),
                WorldEventChoice("Pasar", karmaDelta = -3, resultMessage = "Sigues tu día.")
            )),
        WorldEvent("we_busker_prophet", "Mendigo profeta", "Un viejo te dice: 'En tres días, todo cambiará.'", "🧙",
            listOf(
                WorldEventChoice("Tomarlo en serio", xpDelta = 30, resultMessage = "Lo recordarás."),
                WorldEventChoice("Reír y pasar", resultMessage = "Profecías de borracho.")
            )),
        WorldEvent("we_stolen_bike", "Bici en venta sospechosa", "Te ofrecen una bici cara por 100 € (claramente robada).", "🚴",
            listOf(
                WorldEventChoice("Comprarla", cashDelta = -100.0, karmaDelta = -5, resultMessage = "Una ganga... robada."),
                WorldEventChoice("Negarse", karmaDelta = +2, resultMessage = "El vendedor escapa.")
            )),
        WorldEvent("we_political_campaign", "Campaña política", "Un candidato pide donaciones para su partido.", "🗳️",
            listOf(
                WorldEventChoice("Donar 2.000 €", cashDelta = -2_000.0, reputationDelta = +5, karmaDelta = -2, resultMessage = "Agradecido. Quizá te recuerda en el futuro."),
                WorldEventChoice("Pasar", resultMessage = "No te interesa la política.")
            )),
        WorldEvent("we_homeless_offer", "Oferta inesperada", "Un mendigo te ofrece un consejo bursátil 'a cambio de un café'.", "💡",
            listOf(
                WorldEventChoice("Café y consejo (10 €)", cashDelta = -10.0, xpDelta = 50, karmaDelta = +2, resultMessage = "Te susurra el ticker. Sus ojos saben cosas."),
                WorldEventChoice("No", resultMessage = "Sigues. Quizá te equivocaste.")
            )),
        WorldEvent("we_family_visit", "Visita familiar", "Tu madre llama: '¿Cuándo vienes?'", "👩‍👦",
            listOf(
                WorldEventChoice("Ir (cancelar planes)", energyDelta = -20, happinessDelta = +25, karmaDelta = +5, resultMessage = "Te hace tu plato favorito."),
                WorldEventChoice("Excusa", happinessDelta = -10, karmaDelta = -3, resultMessage = "Le sientes triste por teléfono.")
            )),
        WorldEvent("we_old_employee", "Ex-empleado", "Un ex-empleado tuyo te encuentra. 'Me arruinaste la vida.'", "😡",
            listOf(
                WorldEventChoice("Pedirle perdón", karmaDelta = +5, happinessDelta = -3, resultMessage = "Te escupe pero te sientes mejor."),
                WorldEventChoice("Reembolsarle 1.000 €", cashDelta = -1_000.0, karmaDelta = +8, reputationDelta = +3, resultMessage = "Llora. Te lo agradece."),
                WorldEventChoice("Defender tu acción", karmaDelta = -3, reputationDelta = -2, resultMessage = "Se va furioso.")
            )),
        WorldEvent("we_alien_pamphlet", "Pamfleto OVNI", "Un señor con sombrero de papel te da un pamfleto sobre OVNIs.", "👽",
            listOf(
                WorldEventChoice("Aceptar leerlo", happinessDelta = +3, xpDelta = 10, resultMessage = "La verdad está ahí fuera."),
                WorldEventChoice("Tirar al pasar", resultMessage = "Se ofende sutilmente.")
            )),
        WorldEvent("we_lost_kid", "Niña perdida", "Una niña llora porque ha perdido a su madre.", "👧",
            listOf(
                WorldEventChoice("Ayudarla a buscar", energyDelta = -15, karmaDelta = +6, happinessDelta = +6, resultMessage = "Encuentras a la madre. Mucho llanto y abrazos."),
                WorldEventChoice("Llamar a un policía", karmaDelta = +3, resultMessage = "Esperas hasta que llega la policía.")
            )),
        WorldEvent("we_charity_telemarketer", "Telemarketing benéfico", "Te llaman para una donación recurrente de 50 €/mes.", "📞",
            listOf(
                WorldEventChoice("Aceptar 50€/mes", cashDelta = -50.0, karmaDelta = +4, reputationDelta = +1, resultMessage = "Donación recurrente activada."),
                WorldEventChoice("Donación única 100€", cashDelta = -100.0, karmaDelta = +3, resultMessage = "Pago único."),
                WorldEventChoice("Colgar", resultMessage = "Vuelven a llamar.")
            )),
        WorldEvent("we_secret_admirer", "Admirador secreto", "En la calle, alguien te deja una nota: 'Eres mi inspiración.'", "💌",
            listOf(
                WorldEventChoice("Sonreír", happinessDelta = +8, resultMessage = "Te ilumina el día."),
                WorldEventChoice("Buscarle", energyDelta = -10, happinessDelta = +12, xpDelta = 30, resultMessage = "Le encuentras. Tomarán un café.")
            )),
        WorldEvent("we_old_classmate", "Compañero del cole", "Te encuentras a un compañero que ahora vive en la calle.", "😢",
            listOf(
                WorldEventChoice("Ofrecerle un trabajo", cashDelta = -2_000.0, karmaDelta = +10, reputationDelta = +5, happinessDelta = +10, resultMessage = "Le das una oportunidad."),
                WorldEventChoice("Darle 200 €", cashDelta = -200.0, karmaDelta = +3, resultMessage = "Una ayuda momentánea."),
                WorldEventChoice("Fingir no reconocerle", karmaDelta = -8, happinessDelta = -10, resultMessage = "Te sientes una basura.")
            ))
    )

    /** Combinado total con los originales. */
    val merged: List<WorldEvent>
        get() = WorldEventCatalog.all + extra
}
