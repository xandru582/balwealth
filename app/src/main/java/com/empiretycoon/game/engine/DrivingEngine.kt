package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.random.Random

/**
 * Motor del subsistema vehicular: comprar coches, gestionar el garaje,
 * subirse/bajarse, y ajustar la velocidad de movimiento del avatar.
 *
 * Cuando el jugador `currentlyDrivingId != null`, la velocidad de movimiento
 * en `MovementEngine` se multiplica por el `topSpeed` del modelo. El
 * WorldScreen renderiza el coche en lugar del avatar.
 */
object DrivingEngine {

    /**
     * Compra un coche del catálogo. Falla si no hay caja, no hay slot libre,
     * o el modelo no existe.
     */
    fun buyCar(state: GameState, modelId: String, customColor: Long? = null): GameState {
        val model = CarCatalog.byId(modelId) ?: return notify(state, NotificationKind.ERROR, "Modelo no encontrado", modelId)
        if (state.company.cash < model.price)
            return notify(state, NotificationKind.ERROR, "Sin fondos",
                "Necesitas ${"%,.0f".format(model.price - state.company.cash)} € más para el ${model.displayName}")
        if (!state.garage.canFitMore)
            return notify(state, NotificationKind.WARNING, "Garaje lleno",
                "Vende un coche o amplía tu garaje (${state.garage.cars.size}/${state.garage.maxSlots}).")

        val instanceId = "car_${System.nanoTime()}"
        val plate = randomPlate(Random(state.tick xor System.nanoTime()))
        val owned = OwnedCar(
            instanceId = instanceId,
            modelId = modelId,
            purchasedAtTick = state.tick,
            customColor = customColor,
            plateNumber = plate
        )
        val company = state.company.copy(cash = state.company.cash - model.price)
        // Bono de prestigio = +reputation por tier
        val newRep = (company.reputation + model.prestige / 10).coerceIn(0, 100)
        val player = state.player.withHappiness(model.happinessBoost)
        val notif = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.SUCCESS,
            title = "${model.emoji} Nuevo coche",
            message = "${model.brand.displayName} ${model.displayName} en tu garaje. +${model.happinessBoost} 😊"
        )
        return state.copy(
            company = company.copy(reputation = newRep),
            player = player,
            garage = state.garage.copy(cars = state.garage.cars + owned),
            notifications = (state.notifications + notif).takeLast(40)
        )
    }

    /** Vende un coche (devuelve el 70% del precio del modelo). */
    fun sellCar(state: GameState, instanceId: String): GameState {
        val owned = state.garage.cars.find { it.instanceId == instanceId } ?: return state
        val refund = owned.model().price * 0.7
        val newCars = state.garage.cars.filterNot { it.instanceId == instanceId }
        val newDriving = if (state.garage.currentlyDrivingId == instanceId) null else state.garage.currentlyDrivingId
        return state.copy(
            company = state.company.copy(cash = state.company.cash + refund),
            garage = state.garage.copy(cars = newCars, currentlyDrivingId = newDriving)
        )
    }

    /** Subir/bajar del coche. Bajarse pone driving=null. */
    fun toggleDriving(state: GameState, instanceId: String? = null): GameState {
        val newDrivingId = if (state.garage.isDriving) null
            else (instanceId ?: state.garage.cars.firstOrNull()?.instanceId)
        if (newDrivingId == null && !state.garage.isDriving) {
            return notify(state, NotificationKind.WARNING, "Sin coches",
                "Compra un coche en el concesionario primero.")
        }
        val notif = if (newDrivingId != null) {
            val car = state.garage.cars.find { it.instanceId == newDrivingId }
            GameNotification(
                id = System.nanoTime(),
                timestamp = System.currentTimeMillis(),
                kind = NotificationKind.INFO,
                title = "🚗 Subido al coche",
                message = "Conduces ${car?.model()?.displayName ?: "tu coche"}."
            )
        } else {
            GameNotification(
                id = System.nanoTime(),
                timestamp = System.currentTimeMillis(),
                kind = NotificationKind.INFO,
                title = "🚶 Bajado del coche",
                message = "Vuelves a caminar."
            )
        }
        return state.copy(
            garage = state.garage.copy(currentlyDrivingId = newDrivingId),
            notifications = (state.notifications + notif).takeLast(40)
        )
    }

    /** Ampliar slots de garaje (10k * slot actual). */
    fun expandGarage(state: GameState): GameState {
        val cost = state.garage.maxSlots * 10_000.0
        if (state.company.cash < cost)
            return notify(state, NotificationKind.ERROR, "Sin fondos",
                "Necesitas ${"%,.0f".format(cost)} € para ampliar el garaje.")
        return state.copy(
            company = state.company.copy(cash = state.company.cash - cost),
            garage = state.garage.copy(maxSlots = state.garage.maxSlots + 1)
        )
    }

    /** Pintar coche custom — 500 €. */
    fun repaintCar(state: GameState, instanceId: String, color: Long): GameState {
        if (state.company.cash < 500.0) return state
        val updated = state.garage.cars.map {
            if (it.instanceId == instanceId) it.copy(customColor = color) else it
        }
        return state.copy(
            company = state.company.copy(cash = state.company.cash - 500.0),
            garage = state.garage.copy(cars = updated)
        )
    }

    /** Devuelve el multiplicador de velocidad si está conduciendo. */
    fun speedMultiplier(state: GameState): Float {
        if (!state.garage.isDriving) return 1f
        val car = state.garage.current() ?: return 1f
        return car.model().topSpeed
    }

    private fun randomPlate(rng: Random): String {
        val letters = ('A'..'Z').toList()
        val l = (1..3).map { letters.random(rng) }.joinToString("")
        val n = (1000 + rng.nextInt(9000)).toString()
        return "$l-$n"
    }

    private fun notify(state: GameState, kind: NotificationKind, title: String, msg: String): GameState {
        val n = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = kind,
            title = title,
            message = msg
        )
        return state.copy(notifications = (state.notifications + n).takeLast(40))
    }
}
