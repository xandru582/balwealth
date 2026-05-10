# Audit UX — Concesionario, Garaje y Casa

Fecha: 2026-05-10. Archivos: `DealershipScreen.kt`, `GarageScreen.kt`, `HouseScreen.kt`.

## Dealership (`DealershipScreen.kt`)

- **Lista + preview real**: `LazyColumn` con `CarRow` (L86-87) renderiza mini-canvas via `drawPlayerCar` (L114-122) — coherente con el render del mundo. OK.
- **Filtros marca**: `FilterChip` horizontalScroll (L52-75). Falta chip "Eléctricos/Clásicos" como atajo.
- **Precios**: color condicional Gold/Ruby según `canAfford` (L135-136). Botón "Comprar" deshabilita si owned o garaje lleno (L140). Buen feedback.
- **Comparación**: ausente. Solo `CarPreviewDialog` (L155-200) muestra stats absolutos via `StatLine` (L191-194). No hay diff vs coche actual del jugador. **Gap UX**.
- **Bug menor**: L127 concatena emojis con `if … else ""` mal anidado — `isClassic` solo se aplica si `isElectric` es false.

## Garage (`GarageScreen.kt`)

- **Highlight actual**: borde Emerald + badge "CONDUCIENDO" (L83, L97-105). Claro.
- **Switch active**: botón `toggleDriving` cambia label "Bajar/Conducir" y color (L119-129). OK.
- **Sell**: `TextButton` "Vender" en Ruby (L136-138) — **sin confirmación**, riesgo de tap accidental. Añadir diálogo.
- **Repintado**: paleta inline `CarColors` (L29-33), toggle `showColors` (L140-160). Precio "🎨 500€" hardcoded (L134) — debería leer del modelo.
- **Capacidad**: contador `cars.size/maxSlots` (L46) y CTA `expandGarage` solo aparece si lleno (L49-56) — debería estar siempre visible.

## House (`HouseScreen.kt`)

- **Decoración (grid)**: `HouseGrid` 28dp celdas (L130, L132-165), tap coloca/quita. Funcional pero sin indicador de celdas válidas para `pickedKind` con `sizeX/sizeY > 1`.
- **Catálogo**: `Row` horizontalScroll con tiles 86dp (L77-103). Precio Gold/Ruby según cash (L98-99). OK, pero falta categorización y descripción.
- **Mejoras/muebles**: solo colocar/vender 50% (L48-49). Sin niveles ni stats de mueble visibles.
- **Familia**: tabs Pareja/Hijos (L110-113) embebidas — fuera de scope "decoración".

**Prioridad**: confirmación venta coche; comparativa stats en dealership; feedback de footprint mueble.
