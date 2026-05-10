# Auditoría modelos de economía

Alcance solicitado: `Stocks.kt`, `Market.kt`, `Resources.kt`, `RealEstate.kt`, `Banking.kt`. **`Stocks.kt` y `Banking.kt` no existen** en `app/src/main/java/com/empiretycoon/game/model/`. Los tipos de bolsa (`Stock`, `StockHoldings`, `StockCatalog`) viven dentro de `Market.kt`. El equivalente bancario más cercano es `Loans.kt` (no auditado aquí).

## Serializable

- OK: `Market` (`Market.kt:11-12`), `Stock` (`Market.kt:37-38`), `StockHoldings` (`Market.kt:50-51`), `Resource` (`Resources.kt:15-16`), `Property` (`RealEstate.kt:19-20`), `RealEstatePortfolio` (`RealEstate.kt:30-31`).
- `ResourceCategory` (`Resources.kt:9`) y `PropertyType` (`RealEstate.kt:5-17`) son `enum class` sin `@Serializable`; kotlinx-serialization los soporta por nombre, pero conviene anotar explícitamente para estabilidad si se reordenan.

## Defaults

- `Market(priceFactors = emptyMap(), priceTrend = emptyMap())` (`Market.kt:14-16`): un `Market()` deserializado vacío hace `priceOf` devolver `base * 1.0` silenciosamente; `Market.fresh()` (`Market.kt:28`) sí inicializa todos los IDs. Riesgo si la migración de save omite el campo.
- `Stock.trend = 0.0`, `priceHistory = emptyList()`, `annualDividendYield = 0.0` (`Market.kt:43-46`): razonables.
- `Property.occupied = true`, `maintenancePerDay = 0.0` (`RealEstate.kt:26-27`): default `occupied=true` puede inflar `dailyNet` tras carga parcial.
- `Resource` (`Resources.kt:16-22`) sin defaults: bien, todos los campos son obligatorios.

## Ranges / clamps

- `clampFactor` 0.55–1.8 (`Market.kt:73`) coincide con el comentario de `priceFactors` (`Market.kt:13`). OK, pero **no se aplica dentro del modelo**: depende de que el motor lo invoque. `priceOf` (`Market.kt:18-22`) no clamp-ea defensivamente.
- `buyPriceOf` *1.15 / `sellPriceOf` *0.90 (`Market.kt:24-25`): spread fijo 25%, no parametrizable.
- `Stock.volatility` y `annualDividendYield` (`Market.kt:42,46`) sin validación de rango (>=0, <1).
- `Property.rentPerDay`, `purchasePrice`, `maintenancePerDay` sin clamp (`RealEstate.kt:24-27`): permite valores negativos.

## History bloat

- **`Stock.priceHistory: List<Double>` sin cota** (`Market.kt:44`). Si el motor agrega un punto por tick y la partida dura miles de ticks * 8 acciones, el save crece sin límite y la (de)serialización JSON paga O(n) por stock. Recomendado: `ArrayDeque` con tamaño máx (p.ej. 256) recortado al insertar, o serializar sólo ventana reciente.
- `RealEstatePortfolio.owned/available` (`RealEstate.kt:32-33`) son `List<Property>` sin tope; aceptable para escala humana, pero `dailyNet`/`totalValue` (`RealEstate.kt:35-39`) recalculan cada acceso (`get()`), no cachean.

## Otros

- `ResourceCatalog.byId` lanza `error()` (`Resources.kt:72`); IDs huérfanos en saves antiguos rompen la carga. `tryById` existe pero `Market.priceOf` usa el lanzador.
- Faltan archivos solicitados: cubrir `Loans.kt` y crear `Stocks.kt`/`Banking.kt` si la separación está planificada.
