# Auditoría: Sistemas grandes (Crypto, Racing, Jobs, JobBusiness)

## 1. Cobertura @Serializable
- Crypto.kt L20, L24, L46, L61, L95, L117 — todas las data classes/enums marcados.
- Racing.kt L22, L64, L188, L197, L254, L268, L277, L330, L346, L360, L371, L460, L467, L486 — cobertura completa.
- Jobs.kt L27, L31, L59, L358, L376, L394 — completo.
- JobBusiness.kt L30, L47, L83 — completo.

## 2. Defaults (forwards-compat de saves)
- Crypto.kt L53-L57, L65-L91, L97-L109 — todos los campos nuevos llevan default; `wasEverBought` (L82) y `autoSellMining` (L91) explícitamente justificados.
- Racing.kt L76-L98, L259-L261, L286, L334-L339, L488-L503 — defaults consistentes; `RaceCar` L365-L366 ok.
- Jobs.kt L362-L372, L379-L387, L400 — ok.
- JobBusiness.kt L36, L50-L65, L86-L88 — ok.
- Riesgo: Racing.kt L23-L35 (`RaceCircuit`) y L65 (`RaceDriver` campos id..salaryPerDay), L199-L212 (`Sponsor`), L279-L286 (`TechStaff`) NO tienen defaults en campos obligatorios. Mitiga: estas instancias salen de catálogos estáticos, no se serializan partidas viejas con shape distinta — pero un cambio de catálogo rompe saves.

## 3. Enums forwards-compat
- Crypto.kt L21 `CryptoProfile`, Racing.kt L189 `SponsorTier`, L269 `StaffRole`, Jobs.kt L28 `JobStat`, L32 `JobCategory` — sin valor `UNKNOWN`/fallback. kotlinx.serialization lanzará `SerializationException` si un save futuro contiene un valor desconocido. Recomendación: añadir `@JsonClassDiscriminator` o un default estable, o evitar persistir el enum directamente (guardar `String` y mapear).
- Jobs.kt L60 `JobId` se persiste vía `JobProgress.jobName: String` (L360) y `JobsState.progress: Map<String, ...>` (L381) — buena defensa: borrar un JobId no rompe el save, solo deja entradas huérfanas.

## 4. Rangos numéricos
- Sin `require(...)` ni validación en init. Comentarios indican rangos (Racing.kt L31 `difficulty 1..10`, L72-L74 `skill 50..99`, L284 `rating 50..99`, L362-L364 car stats `1..100`, Jobs.kt L362 `level 1..50`) pero no se imponen. Crypto.kt L32 `volatility 0..1`, L36 `rugChancePerDay`, L57 `sentiment -1..+1` — sin clamp.
- JobBusiness.kt L52 `upgradeLevel 1..5` documentado pero `maxEmployees` (L68) usa fórmula sin cap superior.
- Riesgo: un save corrupto o migración futura puede meter valores fuera de rango y no fallará al deserializar — fallará en gameplay.

## 5. Resumen
Serialización sólida en cuanto a `@Serializable` y defaults para campos opcionales. Dos huecos: enums sin fallback y ausencia de validación de rangos al deserializar.
