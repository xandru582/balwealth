# Auditoría — Modelos Core

Alcance: `Player.kt`, `Company.kt`, `Buildings.kt`, `Employees.kt`.

## Serialización
- `PlayerStats` y `Player` marcados `@Serializable` con defaults coherentes (Player.kt:12, 28).
- `Company` `@Serializable`; colecciones con defaults vacíos (Company.kt:9, 18-21). OK para evolución de saves.
- `Building` `@Serializable` (Buildings.kt:40), pero falta default para `id` y `type` (Buildings.kt:42-43): añadir nuevos campos exigirá migración manual.
- `BuildingType` enum NO marcado `@Serializable` (Buildings.kt:6). kotlinx-serialization soporta enums por nombre, pero **renombrar un valor rompe saves silenciosamente**. Sugerido: `@SerialName` por entrada.
- `Employee` `@Serializable`; `id` sin default (Employees.kt:13).

## Defaults / Saves
- `Company.founded: Long = 0` (Company.kt:17): valor 0 ambiguo entre "sin iniciar" y tick real 0.
- `Player.cash` (Double) y `Company.cash` (Double): pérdida de precisión monetaria; recomendado `Long` (céntimos) o `BigDecimal`.
- `inventory: Map<String,Int>` (Company.kt:20): claves string sin enum. Refactor de IDs de recurso rompe stocks guardados.

## Overflow / Numérico
- `Player.xpForNextLevel` y `addXp` (Player.kt:39-49): `1.35.pow(level-1)` desborda `Long` ~lvl 134 y produce `Infinity` antes. Bucle `while` puede colgarse si XP y umbral colapsan.
- En Player.kt:44 hay bug: `100 * 1.35.pow(newLevel-1).toLong()` — el `.toLong()` se aplica al `pow` (truncando a 1) antes de multiplicar; inconsistente con la línea 45 que sí parentiza. Resultado: condición de salida del while errónea, sube de nivel demasiado rápido.
- `Company.addXp` (Company.kt:38-46): mismo riesgo de overflow `Double→Long` en niveles altos (Math.pow 1.5^N).
- `BuildingType.costAtLevel` (Buildings.kt:26): `1.8^level` desborda Double razonable >lvl ~600, pero `Double` evita crash. Sin clamp.
- `Building.progressSeconds: Double` (Buildings.kt:47) sin tope: deltas largos sin consumir generan acumulado infinito.

## Accessors / lazy
- `val total`, `totalWorkers`, `totalSalaries`, `inventoryCount`, `effectiveCapacity`, `name`, `workerCapacity`, `productivity`: todos `get()` recomputados por llamada (Player.kt:20, Company.kt:23-34, Buildings.kt:50-52). Correctos como `data class` inmutable; no usar `by lazy` sobre `val` derivados ya que `copy()` no los invalidaría. Aceptable.
- `Employee.effectiveOutput()` función pura (Employees.kt:20). OK.

## Otros
- `EmployeeFactory.id = "emp_${System.nanoTime()}_$idx"` (Employees.kt:44): colisiones posibles en bucles rápidos; preferir UUID.
- `withLoyalty/withEnergy/withHappiness` clamp correcto (Employees.kt:22, Player.kt:51-55).
