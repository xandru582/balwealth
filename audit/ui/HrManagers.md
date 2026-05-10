# Auditoría UX — RRHH y Gerentes

Archivos auditados:
- `HrScreen.kt`
- `ManagersScreen.kt`

## Solicitantes (`HrScreen.kt`)
- Info clara: edad, experiencia, educación, traits, salario y prima (L500–L531). Bien estructurado.
- **Sin filtros** por rol/educación/salario sobre `applicants` (L463). Con pool grande, navegación tediosa.
- Refrescar disponible (L479). Falta indicar coste o cooldown del refresh.
- **Sin confirmación de fichaje ni preview**: `Fichar` ejecuta directo `vm.hireApplicant` (L536). Riesgo de fichaje accidental sin ver impacto en caja/plantilla.

## Plantilla (`HrScreen.kt`)
- `LazyColumn` con `key = { it.id }` correcto para 1000+ (L109). Performance OK.
- **Resumen** calcula `avgSat`/`avgBurn` recorriendo todos los profiles cada recomposición (L91–L94); con 1000+ empleados puede ser costoso. Recomendar `remember(profiles)` o derivar en VM.
- Acciones `Despedir`/`Ascender` sin confirmación (L128, L133). Despedir es destructivo: añadir AlertDialog.

## Formación (`HrScreen.kt`)
- Catálogo y activos en pestaña (L322–L348). Muestra duración, coste, boost (L386, L400).
- **Elegibilidad** filtrada por rol, educación y nivel (L411–L416). Correcto, pero sin explicar por qué un empleado NO aparece.
- Selección por checkbox (L433). Sin indicador de coste total acumulado al iniciar.

## Ejecutivos (`HrScreen.kt`)
- `ExecPicker` (L260) muestra candidatos con rol/nivel. Bien.
- Liberar exec sin confirmación (L225).

## Gerentes (`ManagersScreen.kt`)
- Refresh pool (L176), hire con validación cash/slots/duplicados (L213–L215). Correcto.
- **Fire sin confirmación** (L146): destructivo silencioso.
- Mejora muestra coste (L143) — bien.
- `ManagerConfigDialog` (L280) con campos texto: validación solo `toDoubleOrNull` (L301–L308). Sin rangos, sin hints, sin teclado numérico (`keyboardOptions`).
- `WhitelistEditor` anidado dentro de scroll vertical + LazyColumn (L319, L395) — riesgo de conflicto de scroll.

## Recomendaciones prioritarias
1. AlertDialog confirm: fichar, despedir, fire manager, liberar exec.
2. Filtros y búsqueda en Solicitantes y Plantilla.
3. Memoizar agregados de plantilla.
4. `KeyboardType.Number` en `ConfigField`.
