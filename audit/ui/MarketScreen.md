# Audit UX — MarketScreen.kt

## Resumen
Pantalla funcional con filtros por categoría y filas expandibles para compra/venta. Falta input de cantidad libre, validación visible de fondos y feedback de transacción.

## Hallazgos

### Compra/venta
- Botones fijos `Comprar 1 / x10 / x50` (L125–129) y `Vender 1 / x10 / Todo` (L133–137). No hay input numérico para cantidad arbitraria.
- Validación de inventario en venta vía `enabled = have >= n` (L133–137). **Falta validación de fondos en compra**: `QtyButton` (L145–151) no recibe `enabled`, permite tap aunque no haya cash.

### Precio + tendencia
- Precio coloreado por `factor` (L98–103) y flecha up/down según `trend` (L104–109). **No hay sparkline**, solo flecha binaria; pierde magnitud histórica.

### Bulk buy/sell
- Venta tiene "Todo" (L137). **Compra carece de "Max"** según fondos/capacidad.

### Filtro/sort
- `FilterChip` por `ResourceCategory` (L47–57) + "Todo" (L38–46). **No hay sort** (precio, tendencia, stock).

### Feedback transacción
- Ausente. `vm.buy/sell` se invocan sin Snackbar, toast ni animación de confirmación/error.

### Inventory overflow
- Solo muestra `Stock: $have` (L111). **Sin warning de capacidad** ni indicador de límite máximo.

## Prioridad
Alta: validar fondos en compra, feedback transaccional, input cantidad. Media: sort, max-buy, sparkline. Baja: overflow warning.
