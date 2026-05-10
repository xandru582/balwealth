# Auditoría QA UX — PrestigeScreen.kt

## Resumen
Pantalla funcional con preview de puntos, dialog de confirmación detallado, lista filtrable de perks y cabecera de estado. Faltan elementos clave: multiplicador global y feedback de no-elegibilidad.

## Hallazgos

### Preview de puntos a ganar — OK
- L33: cálculo `PrestigeEngine.computePoints(state)` reactivo a `state`.
- L100-104: muestra `+$pointsIfRebirth puntos de prestigio` destacado en Gold/Black/18sp.
- L106-108: contexto auxiliar (caja, edificios, nivel). Bien.

### Confirmation dialog "pierdes/conservas" — OK
- L150-201: AlertDialog con secciones "Vas a perder" (L176, Ruby L180-184) y "Conservas" (L187, Emerald L191-196).
- L192-193: muestra transición de nivel `N → N+1`. Excelente claridad.
- L158: botón "RENACER" sin confirmación destructiva tipo hold/typing — riesgo bajo pero presente.

### Lista de perks con costes — OK
- L140-146: `items(perks)` con `key`. L264-336 `PerkCard` muestra emoji, nombre, descripción, categoría, coste (L314-319) en Gold/Ruby según affordability.
- L322-332: botón "Comprar"/"Activo" con estado `enabled` correcto.
- L133-138: chips de categoría filtrables.

### Prestige actual + multiplicador — PARCIAL
- L52-82: StatPills con nivel, puntos disp., total ganados, cash vital. Falta **multiplicador global** explícito (p.ej. `x1.25`) derivado del nivel/perks. UX pierde el "por qué renacer".

## Sugerencias
1. Añadir StatPill de multiplicador activo.
2. Mostrar delta vs. estado actual (puntos antes→después).
3. Indicar requisito mínimo cuando `pointsIfRebirth == 0` (L115 deshabilita sin explicar).
4. Tooltip/expandible en perks sobre efecto numérico exacto.
