# Auditoría QA UX — Skill & Trait Trees

Fecha: 2026-05-10
Archivos:
- `app/src/main/java/com/empiretycoon/game/ui/screens/SkillTreeScreen.kt`
- `app/src/main/java/com/empiretycoon/game/ui/screens/TraitTreeScreen.kt`

## Resumen

Ambas pantallas resuelven correctamente el flujo (ramas en tabs, nodos por tier, estado visual por color de borde, coste y prereqs visibles). Hay deficiencias de claridad, accesibilidad y consistencia entre las dos pantallas.

## Hallazgos

### Visualización del árbol
- SkillTree pinta filas de 2 nodos sin líneas que conecten prereqs → la topología real del árbol no se ve (`SkillTreeScreen.kt:119-138`). Solo hay un divisor vertical genérico entre tiers (`SkillTreeScreen.kt:139-153`).
- TraitTree es lineal (cadena 1→12) pero se muestra como lista vertical sin indicador visual de cadena (`TraitTreeScreen.kt:111-120`).
- Inconsistencia: SkillTree usa `ScrollableTabRow` Material (`SkillTreeScreen.kt:68`); TraitTree usa Row+Box manual (`TraitTreeScreen.kt:67-92`). Unificar.

### Prerrequisitos
- SkillTree solo lista nombres en texto plano sin diferenciar cumplidos/pendientes (`SkillTreeScreen.kt:242-253`). Falta color/check por prereq.
- TraitTree muestra "Falta tier N-1" pero no nombra el trait previo (`TraitTreeScreen.kt:163`).

### Coste
- SkillTree no indica si el jugador puede pagar el `cost`; solo lo lista (`SkillTreeScreen.kt:228-232`). Texto en Dim aunque sea desbloqueable.
- TraitTree sí distingue "💸 Necesitas X XP" vs "Disponible" (`TraitTreeScreen.kt:160-165`) — buen patrón a portar.

### Info de skill/trait
- SkillTree carece de botón explícito; el nodo entero es clickable (`SkillTreeScreen.kt:215`) — riesgo de toques accidentales y poca affordance. TraitTree tiene botón "Comprar" claro (`TraitTreeScreen.kt:176-184`).
- Sin tooltip ni modal de detalle: descripción truncable a 1-2 líneas no permite ver efectos largos.
- Falta indicar efecto numérico aplicado tras desbloquear (sin "+X%/+Y").

### Accesibilidad
- Tamaños 10-11sp en estado/prereqs (`SkillTreeScreen.kt:250`, `TraitTreeScreen.kt:166`) por debajo del mínimo recomendado.
- Estado se comunica solo por color (Gold/Emerald/InkBorder) — sin `contentDescription` para TalkBack.
- Emojis como única señal (`🔒`, `★`, `✔`) en `StatusBadge` (`SkillTreeScreen.kt:259-263`).

## Recomendaciones prioritarias
1. Añadir botón "Comprar/Desbloquear" explícito en `SkillNode`.
2. Pintar conectores entre prereqs y nodo (tier-a-tier).
3. Coste con color rojo si no alcanza, en ambas pantallas.
4. `contentDescription` y subir tipografías a 12sp mínimo.
