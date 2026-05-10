# Auditoría QA UX — ResearchScreen.kt

## Bugs críticos
- L40-41: division `inProgressSecondsLeft / researchSeconds` puede dar `frac` negativo si overflow o >1 si seconds=0 (sin guard). Posible crash/visual roto.
- L43: `.toInt()` sobre `Double` segundos pierde precisión y muestra "0s restantes" mientras sigue activa.

## Bugs balanceo
- L89: condición `canStart && cash >= cost` duplica chequeo (canStart ya debería incluirlo); inconsistencia si difieren.
- Sin tree visualization: prerequisitos solo listados como texto (L75-80), no hay grafo/árbol pedido por diseño.

## UX
- L62: emojis hardcoded como icono; no respeta theming/accesibilidad (TalkBack lee "lupa").
- L75-80: prerequisites coloreados solo binario Dim/Ruby — falta estado "completado" (Emerald) por prereq individual.
- L83-85: coste y duración apilados sin label; usuario nuevo no distingue "$" de "s".
- Sin filtro/búsqueda ni agrupación por tier; lista plana puede crecer ilegible.
- L95-99: botón "Activa" sigue habilitado visualmente (disabled solo por `canStart`), confunde.

## Performance
- L52: `TechCatalog.all` iterado completo sin remember; OK en LazyColumn pero `byId(it)` (L36, L76) recalcula cada recomposición.

## Dead code
- Ninguno detectado.

## Localización
- Strings hardcoded ES (L34-35, 39, 43, 47, 78, 96-98); no usa `stringResource`.

## Recomendación
- P0: Guard division-by-zero (L40-41); fix `.toInt()` (L43).
- P1: Añadir tree/grafo prereq con colores por nodo; i18n strings.
- P2: Iconos vectoriales + contentDescription; labels "Coste"/"Tiempo".
- P3: Filtros, agrupación por tier, remember catalog lookups.
