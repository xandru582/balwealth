# Auditoría — Modelos de Progresión

Archivos auditados:
- `Achievements.kt`
- `Quests.kt`
- `SideQuests.kt`
- `Storyline.kt`

## 1. Serialización

- `Achievement` (L26) y `AchievementsState` (L43) anotados `@Serializable`. OK.
- `Quest` (L9 `Quests.kt`) `@Serializable`. Falta un `QuestsState` agregado equivalente a `AchievementsState`/`SideQuestsState` — los flags `completed`/`claimed` viven embebidos en cada `Quest` (L18-19), forzando duplicar el catálogo en runtime para mutar progreso.
- `SideQuest` (L50), `SideQuestsState` (L72), `QuestObjective` sealed (L25), `QuestReward` (L40) — todos `@Serializable`. OK.
- `StoryChapter` (L19), `ChapterReq` (L30), `StoryChoice` (L38), `StorylineState` (L73) — `@Serializable`. OK.
- `StoryEffect` sealed (L50 `Storyline.kt`) marcada `@Serializable` con subclases anotadas; el `SerializersModule` (L618-630) registra 9 subclases pero la sealed declara 9 — coherente. NOTA: `import kotlinx.serialization.Polymorphic` (L3) está sin uso.

## 2. Sealed / representación de condiciones

- `QuestObjective` (`SideQuests.kt` L25-37): sealed bien usada; 11 variantes data class. Adecuado para `when` exhaustivo.
- `StoryEffect` (`Storyline.kt` L50-60): sealed correcta y polimorfismo registrado.
- `Quest` (`Quests.kt`) NO usa sealed para condiciones — la condición vive solo en `goalDescription: String` (L13). El motor debe acoplarse a `id` por convención. Inconsistente frente a `SideQuest`/`StoryEffect`.
- `ChapterReq` (`Storyline.kt` L30): data class plana en lugar de sealed. Aceptable porque combina requisitos AND, pero limita expresividad (sin OR).

## 3. Flags `claimed` / `completed`

- `AchievementsState` (L44-48): separación correcta `unlocked` vs `claimedAchievements` vs `progressMap`. Bien.
- `Quest` (L18-19): `completed` y `claimed` como campos embebidos sobre el catálogo `object QuestCatalog` (L22) — el catálogo es `val` inmutable; cualquier mutación obliga a reemplazar la `Quest` en otra colección. Falta `QuestsState`.
- `SideQuestsState` (L72-79): tiene `completed` y `failed` pero NO `claimedSideQuests`. Si las `QuestReward` se cobran, no hay flag persistente que distinga "completada" de "reclamada"; riesgo de doble cobro.

## 4. Consistencia de ramas

- `StoryChoice.leadsToChapterId` (L42) declarado pero nunca usado: `StoryArc.nextOf` (L605-609) avanza linealmente ignorando ramificación. Las elecciones no ramifican capítulos a pesar del campo.
- `StorylineState.achievedEndingType` (L79) declarado pero ningún capítulo lo asigna; `ch_legacy` (L559) no produce un `endingType`.
- `UnlockAchievement` referencia `ach_5_tech` (L358) y `ach_rep_max` (L533) — IDs existen en catálogo (L298, L324). `ach_cash_1m` (L146) coherente con L76.
- `ChapterReq.requiredQuestId` (L33) nunca usado en ningún capítulo.

## 5. Otros

- `Achievement.threshold = TechCatalog.all.size.toLong()` (L310) crea acoplamiento estático en init order.
- `${"\${empresa}"}` literal en `ch_pr_crisis` (L262) — placeholder sin interpolación.
