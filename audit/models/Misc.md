# Auditoría — Modelos Misc

Alcance: `MultiCity.kt`, `Arcade.kt`, `Seasons.kt`, `TutorialState.kt`, `News.kt`.
Foco: `@Serializable`, defaults, forwards-compat de enums.

## Resumen

| Archivo | @Serializable | Defaults | Enum forwards-compat |
|---|---|---|---|
| MultiCity.kt | OK | OK | Riesgo |
| Arcade.kt | OK | OK | Riesgo (mitigado) |
| Seasons.kt | OK | OK | OK (string + valueOf) |
| TutorialState.kt | Parcial | OK | Riesgo |
| News.kt | Parcial | OK | Riesgo |

## Hallazgos por archivo

### MultiCity.kt
- L34, L45, L74, L90, L108, L121: `@Serializable` correcto en enum `CityId` y todas las data classes.
- L46–L63, L75–L87, L91–L102, L122–L133: todos los campos con default — saves antiguos cargan. Correcto.
- L34 `enum class CityId`: si se elimina o renombra una ciudad, los saves con `CityMarket.id` apuntando al valor caído fallan al deserializar. Sin estrategia `@SerialName` ni mapeo de unknown.

### Arcade.kt
- L19, L38, L51, L62: `@Serializable` correcto.
- L39–L47, L52–L58, L63–L73: defaults completos. Correcto.
- L65 `stats: Map<String, ArcadeGameStats>`: usa `String` (game.name) como clave — buena decisión, sobrevive a remoción de un enum value (sólo se ignora la entrada huérfana).
- L19 enum: si se borra `SNAKE` u otro, partidas históricas en `recentPlays` (L67) referencian `ArcadeGameId` directo — ahí sí rompería.

### Seasons.kt
- L25, L53, L79, L89: `@Serializable` correcto.
- L92 `activeSeasonName: String` + L107 `runCatching { SeasonId.valueOf(...) }.getOrDefault(OFFSEASON)`: patrón ejemplar de forwards-compat — si el enum cambia, fallback seguro.
- L100 `completedCount: Map<String, Int>`: clave string, robusto.

### TutorialState.kt
- L12 `enum class TutorialStep`, L72 `enum class AdvanceCondition`: NO llevan `@Serializable`.
- L57 `TutorialState` sí es `@Serializable` y contiene `currentStep: TutorialStep` (L58) y `Set<TutorialStep>` (L59). Funciona vía serializador automático, pero al añadir/quitar un step un save antiguo puede romper. Sin `@SerialName` por valor.
- L101 `TutorialSpec` y L116 `TutorialScript` no son persistidos (solo runtime) — OK.

### News.kt
- L7 `NewsCategory`, L19 `NewsSeverity`: enums SIN `@Serializable`.
- L31 `NewsItem` `@Serializable` referencia ambos enums (L35, L39): mismo riesgo que TutorialState.
- L70 `NewsTemplate`, L106 `NewsTemplates`: catálogo runtime, no se persiste — OK.
- Defaults: L45–L47, L56–L57 correctos.

## Recomendaciones
1. Añadir `@Serializable` explícito a `TutorialStep`, `AdvanceCondition`, `NewsCategory`, `NewsSeverity`.
2. Considerar `@SerialName("...")` por valor enum para permitir renombrado sin romper saves.
3. Replicar el patrón `String + valueOf+getOrDefault` (Seasons L107) en `MultiCityState.cities[].id` y en `ArcadePlayResult.game` para tolerar enums caídos.