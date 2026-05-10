# Auditoría EmpireScreen.kt

## Bugs críticos
- L262 `vm.build(type)` se ejecuta sin diálogo de confirmación: un toque accidental en "Construir" debita el coste sin segunda oportunidad.
- L149-151 `vm.demolish(b.id)` ejecuta venta sin confirmación ni mostrar refund: pérdida irreversible con un solo toque.
- L142-144 `addWorker`/`removeWorker` no muestran cap real ni bloquean al alcanzar `workerCapacity` (L95): el jugador no entiende por qué falla.

## Bugs balanceo
- L154 muestra coste de mejora solo si `recipe != null`: edificios sin receta ocultan precio de upgrade.
- L342 `c.monthlySalary * 0.5` hardcodea prima; debe venir del modelo.

## UX
- L113 estado "auto ON/OFF" en texto pequeño; no hay Switch real (L146-148 es TextButton). Poco visible.
- L130 chips de inputs/outputs en `Row` simple sin wrap: recetas con muchos insumos se cortan en pantallas estrechas.
- L66-69 empty state de Edificios no enlaza a la pestaña Construir (no es clickable).
- L197 `LazyColumn` dentro de `AlertDialog` puede no scrollear correctamente en dialogs cortos.
- No se distingue visualmente edificio "parado" (sin receta) vs "produciendo": falta badge de estado.

## Performance
- L77 `items(buildings, key = { it.id })` correcto. L242 `items(BuildingType.values())` sin key: re-key en cada recomposición.

## Dead code
- L11-12 imports `Icons.filled.*` solo usa Lock (L223).

## Localización
- "Auto ON/OFF" (L113,147), "-1 👤"/"+1 👤" (L142,144) sin i18n.

## Recomendación
- P0: confirm dialog en build/demolish con coste/refund explícito.
- P0: deshabilitar +1 worker al cap; tooltip motivo.
- P1: Switch real para auto-restart; badge estado producción.
- P2: FlowRow para chips; key en ConstructTab.
- P3: extraer strings a resources.
