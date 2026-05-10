# Auditoría: GameState.kt

**Archivo:** `app/src/main/java/com/empiretycoon/game/model/GameState.kt`
**Anotación:** `@Serializable` (kotlinx.serialization) L13

## Version field
- L15: `val version: Int = 2` — campo presente, default `2`. No se observa lógica de migración en este archivo (auditar `SaveRepository`/migraciones aparte).

## Defaults para backwards-compat
Todos los campos de la `data class` tienen valor por defecto, lo que permite que kotlinx.serialization rellene campos ausentes en saves antiguos siempre que el formato JSON tenga `encodeDefaults=false` o `ignoreUnknownKeys=true`. Defaults sólidos:
- L19-23 sub-states con constructor vacío.
- L25 `stocks = StockCatalog.starter()` — default no determinista si catálogo cambia entre versiones.
- L33 `quests = QuestCatalog.all` — idem, riesgo si IDs cambian.
- L36 `rngSeed = System.currentTimeMillis()` — default impuro: cada deserialización ausente regenera seed (rompe reproducibilidad).
- L59 `rivals = RivalRoster.freshState()`, L137 `crypto = CryptoCatalog.freshState()`, L152 `multiCity = MultiCityCatalog.freshState()` — fábricas; revisar idempotencia.

## Fields nuevos sin default (rompen save antiguo)
**Ninguno detectado.** Todos los 60+ campos tienen `=` con default. Saves v1 deberían poder cargarse si `version<2` se maneja en el cargador.

## Riesgos de serialization
- L89-131, L119, L122, L125, L128, L131: tipos en sub-paquete `com.empiretycoon.game.world.*` referenciados con FQN; deben ser `@Serializable` (no verificable aquí).
- L36 `rngSeed` con default dinámico `System.currentTimeMillis()` — al faltar en JSON antiguo, el determinismo del RNG se pierde silenciosamente.
- L172-174: getters computados (`day`, `hourOfDay`, `inventoryOf`) — OK, no serializados.
- Bloque "v17" L137-170: gran superficie añadida; confirmar bump de `version` y migración correspondiente.

## Recomendaciones
1. Bump `version` cuando se agreguen campos v17 y añadir migrador.
2. Sustituir `rngSeed` default por `0L` o sentinel y derivar seed en una sola entrada.
3. Verificar `@Serializable` en todos los `WorldState`, `WeatherState`, etc.
