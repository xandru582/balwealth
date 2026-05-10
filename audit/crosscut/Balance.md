# Auditoría de Balance Económico — Crosscut

Fecha: 2026-05-10
Alcance: CryptoEngine.kt, Buildings.kt, Jobs.kt, Payroll.kt

## 1. Constantes clave

- `MINER_HIRE_COST = 800.0` — CryptoEngine.kt L454
- `MINER_MONTHLY_SALARY = 350.0` — CryptoEngine.kt L456 -> diario `350/30 ≈ 11,67 €` (L255, L523)
- `Payroll.dailyCost = totalSalaries / 30.0` — Payroll.kt L19
- `baseHourlyWage` Jobs.kt: rango 14-100 (LIBRARIAN 18 L134, BAKER 16 L148, FARMER 14 L270, FOOTBALL_PLAYER 100 L304)
- `baseCost` Buildings.kt L13-L23: FARM 1.200 / SAWMILL 1.800 / MINE 4.500 / BAKERY 3.200 / WAREHOUSE 6.000 / SMELTER 12.000 / OFFICE 18.000 / REFINERY 28.000 / JEWELRY 48.000 / FACTORY 55.000 / SHIPYARD 220.000

## 2. Incoherencias detectadas

### 2.1 Minero como "empleado fantasma" — CryptoEngine.kt L454-L456, L518-L523 vs Payroll.kt L16-L19
Los mineros NO son `Employee`, así que no entran en `company.totalSalaries` (Payroll.kt L19) pero sí cobran su propia nómina aparte (CryptoEngine L255). Resultado: dos sistemas de nómina paralelos sin auditoría unificada. Si `employees.isEmpty()` Payroll retorna sin tocar cash (L17), pero los mineros pueden ser miles y no aparecen en ningún panel agregado.

### 2.2 Hourly wage (jugador) >> salario empleado (escala 24h)
Jobs LIBRARIAN cobra `18 €/h` (L134) -> 432 €/día si trabajas 24 turnos. Un minero cuesta `11,67 €/día` (L255). Un empleado de oficina con salario mensual ~350 € cobra lo mismo que un minero. **El jugador trabajando turnos manuales rinde 37x más por hora que un minero**, lo que rompe el incentivo de delegar.

### 2.3 ROI minero absurdamente rápido
Hire 800 € + 11,67 €/día (L454, L523). Si `produced = miners/difficulty` (L141) y autoSell vende al precio del día (L150-L154), basta con que un minero genere >11,67 €/día neto para ser rentable. Con tokens > 12 € y `miningDifficulty=1`, **payback < 70 días sin riesgo**. No hay coste de electricidad, hardware ni decay.

### 2.4 SHIPYARD vs FOOTBALL_PLAYER
SHIPYARD `baseCost = 220.000` (L22) requiere ~510 turnos como FOOTBALL_PLAYER a 100 €/h sin bonos (L304) — 21 días reales. Comparado con FACTORY (55.000 L19) la curva es 4x sin justificación de output documentada.

### 2.5 Despido sin coste — exploit ciclable
`assignMiners` con delta<0 (L429-L433) y `hireMiners` (L476) sin retención: contratas N por 800·N, mineas 1 día (autoSell L145), despides 0 € coste, neto = producción. El loop es infinito si producción > MINER_HIRE_COST/(días retenidos).

### 2.6 Cascada de despido por impago — sesgo
L267-L274: ordena por `minersAssigned DESC` y reparte despidos. Si un token tiene 99% de los mineros, absorbe casi todos los firings; tokens minoritarios quedan intactos pese a producir menos. Falta proporcionalidad por rentabilidad, no por volumen.

### 2.7 Penalty unstake = quemado, no a la casa
L391-L396: `burned` desaparece sin acreditarse a nadie. El sistema pierde tokens sin sink económico documentado.

## 3. Recomendaciones

1. Unificar `Payroll.applyDaily` con mineros (sumar `dailyMinerPayroll` a `totalSalaries`).
2. Subir `MINER_HIRE_COST` a ≥3.000 € o añadir coste energético diario.
3. Añadir penalización de despido (severance) en `assignMiners` delta<0.
4. Reescalar SHIPYARD baseCost o subir output multiplier.
5. Cap horas/día en Jobs para evitar grindeo manual >> delegación.
