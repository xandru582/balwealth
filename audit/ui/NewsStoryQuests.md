# Auditoría UX — News / Story / SideQuests / DailyChallenges

Fecha: 2026-05-10 · Auditor: QA UX

## Resumen
Cuatro pantallas con patrones inconsistentes de progreso, claim, dismiss y navegación. Hallazgos priorizados abajo.

## Hallazgos

### Progreso visible
- OK `SideQuestsScreen.kt:100-104` — `ProgressBarWithLabel` muestra `cur/target` + días restantes + color según urgencia. Patrón ejemplar.
- OK `DailyChallengesScreen.kt:86-91` — `LinearProgressIndicator` + texto `progress/target`.
- WARN `StoryScreen.kt:45` — Subtítulo "Capítulo X/Y" sin barra; `requirementHint` (`StoryScreen.kt:229-237`) lista requisitos pero sin progreso visual hacia desbloqueo.
- FAIL `NewsScreen.kt` — No hay progreso por noticia; expiración solo texto (`NewsScreen.kt:192-198`), sin countdown visual.

### Claim (cobrar recompensa)
- OK `SideQuestsScreen.kt:109-115` — Botón "Cobrar" Emerald, prominente cuando `isComplete`.
- WARN `DailyChallengesScreen.kt:98-101` — `TextButton` "Reclamar recompensa" es plano, baja jerarquía vs. el "Cobrar" de quests. Inconsistencia visual.
- N/A `NewsScreen.kt`, `StoryScreen.kt` — sin mecánica claim.

### Dismiss / abandonar
- OK `SideQuestsScreen.kt:117-121` — `OutlinedButton` "Abandonar" en Ruby, sin confirmación (RIESGO: pérdida accidental de progreso, falta `AlertDialog`).
- OK `StoryScreen.kt:81` — `EndingDialog` con `onDismiss = acknowledgeEnding`, correcto.
- FAIL `NewsScreen.kt` — Las noticias no se pueden marcar leídas/descartar; el feed crece indefinidamente (`NewsScreen.kt:101-116`).
- FAIL `DailyChallengesScreen.kt` — Sin opción de saltar reto.

### Navegación
- OK `SideQuestsScreen.kt:31-38` — `TabRow` con contadores por estado; clara.
- WARN `NewsScreen.kt:46-79` — `FilterChip` en `horizontalScroll` sin indicador de overflow; chips pueden quedar ocultos en pantallas estrechas.
- BUG `StoryScreen.kt:208` — `if (locked) "??? ${ch.title.take(0)}".trim() + "Capítulo bloqueado"` produce texto siempre `"???Capítulo bloqueado"` (concatenación basura, `take(0)` siempre vacía). Corregir a `"??? Capítulo bloqueado"`.
- WARN `SideQuestsScreen.kt:142-146` — "Refrescar listado" sin coste/cooldown visible; usuario puede spamear.
- WARN `DailyChallengesScreen.kt:54-57` — Empty state `"Esperando al amanecer in-game…"` sin CTA ni tiempo estimado.

## Prioridad
1. P1 BUG `StoryScreen.kt:208` (texto roto).
2. P1 confirmación dismiss `SideQuestsScreen.kt:117`.
3. P2 unificar claim a `Button` en `DailyChallengesScreen.kt:99`.
4. P2 dismiss/marcar-leído en `NewsScreen.kt`.
5. P3 progreso visual desbloqueo capítulo `StoryScreen.kt:57`.
