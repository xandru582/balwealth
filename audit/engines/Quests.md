# Auditoría: QuestEngine + SideQuestEngine + StorylineEngine

## Bugs críticos
- [ ] **SideQuestEngine.kt L106-143** — `claimReward()` no valida si la recompensa ya fue cobrada. Doble click puede duplicar cash/XP/karma. Falta flag `claimed: Boolean` en SideQuest.
- [ ] **SideQuestEngine.kt L192** — `CompleteContracts` siempre retorna `0L to o.n.toLong()` → progreso siempre 0. La quest **nunca avanza**.
- [ ] **SideQuestEngine.kt L196-200** — `DonateToCharity` usa `company.reputation - 30` como proxy: si reputación baja, progreso retrocede → quest inalcanzable.

## Bugs de balanceo / lógica
- [ ] **QuestEngine.kt L19-45** — `evaluate()` solo marca como completadas, no revisa si la condición sigue cumpliéndose en ticks posteriores.
- [ ] **StorylineEngine.kt L88** — `completedChapters + currentChapterId` puede crear bucle: si `nextChap == null`, currentChapterId se queda igual indefinidamente.
- [ ] **StorylineEngine.kt L42** — `state.day - 1 >= req.daysSinceStart` off-by-one sin justificación.
- [ ] **StorylineEngine.kt L184-195** — `computeKarma()` recalcula manualmente pero nunca se llama automáticamente. Si karma se corrompe, no se repara.

## UX / feedback
- [ ] Sin progreso visible para algunas quests largas.

## Performance
- [ ] (sin hallazgos)

## Code smell / dead code
- [ ] **SideQuestEngine.kt L125** — Karma aplicado sin validar bounds antes de coerce.

## Localización / texto
- [ ] (español-only)

## Recomendación 1-line
**P0** — Flag `claimed` en SideQuest (L106). Implementar progreso real para `CompleteContracts` (L192). Bucle storyline (L88).
