# Auditoría modelos RPG

Foco: `@Serializable`, defaults, lookup tables.

## Serializable

- Todas las `data class`/`enum`/`sealed` exponen `@Serializable` correctamente:
  - `TraitTree.kt:27,42,57,70` (enums + state).
  - `SkillTree.kt:18,33,70,86` (sealed `SkillEffect` + variantes 35-49).
  - `Perks.kt:13,22,32`.
  - `Disasters.kt:11,27,30,54,57,69`.
  - `Heists.kt:15,27,35,42,50,53,70,79,93,104`.
- `private data class Quad` en `Disasters.kt:157` no es `@Serializable`: OK, es local al builder y nunca se persiste.

## Defaults

- Defaults sólidos para evitar romper save migrations:
  - `TraitTreeState(unlockedIds = emptySet())` `TraitTree.kt:72`.
  - `SkillTreeState` con tres campos default `SkillTree.kt:88-90`.
  - `PerksState` con `pendingChoice: List<String>? = null` `Perks.kt:34`.
  - `DisasterState` con cooldown/seguro defaults `Disasters.kt:71-83`.
  - `HeistState` con `lastCrewRefreshTick = -1L` `Heists.kt:106-120`.
- Riesgo: `ActiveDisaster.mitigationDeadlineTick` `Disasters.kt:51` no tiene default; cualquier save antiguo sin ese campo fallará. Añadir `= 0L`.

## Lookup tables

- Buenas prácticas (`associateBy` precomputado):
  - `TraitCatalog.byIdMap` `TraitTree.kt:179` con accessor `byId()`.
  - `SkillTreeCatalog.byId` `SkillTree.kt:509`.
  - `PerkCatalog.byId` + `byRarity` `Perks.kt:185-186`.
- Ineficiencias O(n):
  - `TraitCatalog.byBranch` filtra+sort en cada llamada `TraitTree.kt:181-182`. Cachear como `Map<TraitBranch,List<TraitDefinition>>`.
  - `SkillTreeCatalog.byBranch` y `byTier` recorren `all` cada vez `SkillTree.kt:511-515`. Idem.
  - `HeistCatalog.byType` usa `find` lineal `Heists.kt:215`. Sustituir por `associateBy { it.type }`.
  - `TraitTreeState.totalEffectOf` itera unlocked y consulta map `TraitTree.kt:75-82`: aceptable, pero podría cachear `Map<TraitEffectType, Double>` al modificar set.

## Otros

- `PerkCatalog.rollChoices` `Perks.kt:189-205` recalcula `sumOf` por iteración: O(n*count); aceptable con n=42, count=3.
- `CrewGenerator.generate` `Heists.kt:230` correcto, pero `firstNames`/`nicknames` deberían ser `private`.
