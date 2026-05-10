# Auditoría modelo: World States

Alcance: data class `@Serializable` en `app/src/main/java/com/empiretycoon/game/world/`.

## Resumen ejecutivo

Esquemas razonables, defaults coherentes en ramas hijas, pero la raíz `WorldState` y el grid empaquetado son frágiles ante migraciones, y varias listas pueden crecer sin trim.

## Hallazgos por archivo

### WorldState.kt (raíz)
- L18-24 `WorldState`: campos `grid` y `avatar` SIN default. Cualquier save antiguo al que se le añada un campo nuevo arriba romperá la deserialización (kotlinx.serialization exige valores no-default). Riesgo alto: añadir un futuro `weather`/`traffic` directamente aquí explota saves. Recomendado: default `WorldGrid.blank()` / `Avatar(0f,0f)` o usar `@OptIn` con defaults en todos los nuevos.
- L23 `npcsPath: List<String>`: sin tope. Si se taggean NPCs sin limpieza, crece el save.

### WorldGrid.kt
- L24-29 `WorldGrid`: `width/height/tilesPacked` sin defaults; un save corrupto o de versión previa con dim distinto fallará en `init` (L30-34) lanzando `IllegalArgumentException` durante deserialización => crash al cargar. Debería degradar a `blank()` en lugar de require.
- L27 `tilesPacked: List<Int>` para 64×64 = 4096 ints serializados como JSON: bloat considerable. Considerar `ByteArray` packed o RLE; al menos formato binario (Cbor/ProtoBuf).
- L106-110 `unpackType`: ya hace fallback `GRASS` si el ordinal está fuera de rango => OK forwards-compat para nuevos `TileType`.

### Tiles.kt
- L16-37 `TileType`: enum con campos NO serializables (Long, Boolean) — kotlinx serializa solo `.name`/`.ordinal`. Reordenar o eliminar entradas rompe el packing en L103-104 (usa `ordinal`). Forward-compat: añadir SIEMPRE al final.
- L43-52 `TileInteractKind`: mismo riesgo, sin tag estable.
- L65-73 `Tile`: bien con defaults; OK.

### Avatar.kt
- L13-21 `AvatarLook`: defaults completos, OK.
- L31-39 `Avatar`: `x` e `y` sin default => save sin esos campos crashea. Añadir `= 0f`.

### NpcWorld.kt
- L15-27 `NpcWalker`: `id`, `homeX`, `homeY` sin default; el resto OK.
- L29-34 `NpcWorldState.walkers`: lista crece hasta `target=12` (L39) pero no hay trim si target baja entre versiones; tampoco se limpian walkers fuera del grid tras un resize.

### LifeSystems.kt
- L26-37 `Pet.species`/`L408 PlacedFurniture.kind`/`L67 Vehicle.kind`/`L33 WorldProp.kind`: patrón consistente — enum serializado como `String` con `runCatching { .valueOf }` y fallback (L38, L412, L76, L36). Esto es la implementación correcta para forwards-compat: nuevas entradas en otra build no rompen saves antiguos.
- L137-157 `FollowerNpc`: `id, name, portraitSeed, x, y, question, choiceA, choiceB, spawnedAtTick` sin default — si cambias el modelo, los followers en saves antiguos crashean. Sugerencia: defaults vacíos.
- L302-310 `UfoSighting`: `id, x, y, spawnedAtTick` sin default.
- L361-376 `Spouse`/`Child`: faltan defaults en `name`, `portraitSeed`.
- L405-413 `PlacedFurniture`: `id, kind, x, y` sin default.

### LiveWorld.kt
- L66-77 `Vehicle`: `id, kind, x, y, dx, dy` sin default.
- L80-82 `TrafficState.vehicles`: bien limitado por `target=18` en `ensurePopulated`; sin trim si target baja.
- L185-191 `WorldEvent`/L206-217 `WorldEventState.seenIds: Set<String>`: crece monotónicamente y nunca se purga; tras cientos de horas inflará el save.

### CityProps.kt
- L30-37 `WorldProp`: `id, kind, x, y` sin default.
- L40-42 `CityPropsState.props`: regenerado vía `generate()`; en grids 64×64 puede superar 1000 props. No hay cap; si se persiste el save crece linealmente con el área.

### KarmaEcho.kt
- L10-16 `KarmaEchoState`: defaults completos, `tone` como String del enum — forwards-compat OK.

## Recomendaciones prioritarias
1. Añadir defaults en TODOS los `id/x/y/spawnedAtTick` (WorldGrid, Avatar, NpcWalker, Vehicle, FollowerNpc, UfoSighting, WorldProp, PlacedFurniture, Spouse, Child).
2. Reemplazar `require` en `WorldGrid.init` por fallback a `blank()`.
3. Trim de `npcsPath`, `WorldEventState.seenIds`, walkers/vehículos huérfanos.
4. Documentar contrato "enums append-only" para `TileType` (usa ordinal en pack).
