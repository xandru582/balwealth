# RESUMEN auditoría BalWealth — agregado de 64 informes

> Sistema de auditoría con 100 agentes ejecutados en 4 oleadas:
> - **Wave 1**: 32 engines (`audit/engines/`)
> - **Wave 2**: 23 UI screens (`audit/ui/`)
> - **Wave 3**: 9 model files (`audit/models/`)
> - **Wave 4**: 11 cross-cutting (`audit/crosscut/`)
>
> Total: **64 informes**, ~600 hallazgos individuales. Este resumen agrupa
> los más críticos por prioridad. Para detalle por área, consulta el
> archivo correspondiente.

---

## P0 — BLOQUEAN RELEASE (arregla antes de subir a Play Store)

### Crashes y bugs funcionales que rompen progresión

| # | Área | Hallazgo | Fix |
|---|---|---|---|
| 1 | **Theme.kt L61** | `if (dark) Dark else Dark` — light mode nunca se aplica. | Cambiar `else Dark` por `Light`. |
| 2 | **Player.kt L44** | `1.35.pow(...).toLong()` con precedencia incorrecta diverge del check L45 → niveles concedidos en exceso a XP altos. | Asignar a variable: `val cost = (100 * 1.35.pow(newLevel-1)).toLong()`. |
| 3 | **GameViewModel L153-158** | `mutate()` hace read-modify-write NO atómico. `gameLoop` (L100-108) escribe directo sin pasar por mutate → lost updates con acciones de usuario. | Usar `_state.update { }` (CAS) o Mutex global. |
| 4 | **SideQuestEngine L106-143** | `claimReward()` no valida si la recompensa ya fue cobrada → doble click duplica cash/XP/karma. | Flag `claimed: Boolean` en SideQuest. |
| 5 | **SideQuestEngine L192** | `CompleteContracts` siempre retorna progreso 0 → quest **nunca avanza**. | Implementar conteo real. |
| 6 | **ProductionLinesEngine L71-72** | `toggle()` solo cambia flag, NO libera empleados → quedan asignados como "fantasmas" bloqueando otras recetas. | Liberar workers en toggle off. |
| 7 | **JobBusinessEngine L254** | Si pasan múltiples días offline, payroll se ejecuta UNA vez en vez de N → exploit "cierro app para no pagar". | Loop sobre `daysToPay` actualizando `lastPayrollDay`. |
| 8 | **RacingEngine L799** | Championship bonus de constructor usa `company.cash` en vez de `team.budget` (incoherente con refactor). | Cambiar a `team.budget`. |
| 9 | **RacingEngine L723-735** | Bonus de patrocinadores se aplica DOS VECES al budget del equipo del jugador. | Eliminar duplicación. |
| 10 | **WorldStates** | Múltiples data classes (`WorldGrid`, `Avatar`, `Vehicle`, `Spouse`, `Child`, etc.) sin defaults → saves antiguos crashean al cargar. | Añadir defaults compatibles. |
| 11 | **WorldGrid.init L30-34** | `IllegalArgumentException` si tamaño no coincide → save antiguo crashea. | Degradar a `blank()`. |
| 12 | **HomeScreen** | NO tiene Scaffold/BottomBar/Drawer → tras splash el jugador no puede navegar. | Verificar Root, posiblemente regresión. |
| 13 | **EmpireScreen L262/L149** | `vm.build` y `vm.demolish` ejecutan SIN confirmación → mis-tap arruina partida. | AlertDialog con confirmación. |
| 14 | **GarageScreen L136-138** | "Vender coche" sin confirmación. | AlertDialog. |
| 15 | **JobBusinessScreen L223-228** | "Cerrar empresa" (irreversible) sin confirm dialog. | AlertDialog. |
| 16 | **ResearchScreen L40-41** | División sin guard cuando `researchSeconds=0` → crash/NaN. | Guard con `coerceAtLeast(1)`. |
| 17 | **StoryScreen L208** | Concatenación rota: siempre muestra `"???Capítulo bloqueado"`. | Reescribir expresión. |
| 18 | **RivalEngine L46-52** | Doble cobro de defeat rewards si `HostileTakeoverEngine` también marca al rival como derrotado. | Verificar `defeated` antes de aplicar rewards. |
| 19 | **PrivacyPolicy** | Sin política declarada — requisito Play Console. | Hostear en GitHub Pages, declarar en Play Console. |
| 20 | **applicationId** vs **app_name** | `com.empiretycoon.game` vs `BalWealth` — inconsistencia de marca. | Decidir y unificar. |

---

## P1 — ROMPEN EXPERIENCIA (arregla antes de la beta)

### Performance

- **GameEngine `advanceOneSecond`**: cascada de 30+ `state.copy`, `notifications + takeLast(40)` repetido 8 veces, `research.completed.sumOf{}` no cacheado, `EventPool.find` O(N). Cada tick aloca ~10MB de objetos.
- **Production L37**: `HashMap(company.inventory)` por tick. **L100**: `inv.values.sum()` dentro del bucle de edificios → O(B×M).
- **WorldRender**: doble pase literal de luces (`WorldScreen.kt:261-263 / 271-273`), 3 `mutableListOf` por frame, `Brush.radialGradient` recreado por luz/frame, Paint() recreado en MapLabels (L54/67/80), `Random(seed)` por frame en partículas lluvia/nieve.
- **GameViewModel L123-125**: `runBlocking(NonCancellable)` en Main puede ANR al cerrar.
- **TileRenderer**: cientos de `Brush.verticalGradient(listOf(...))` por frame.

### UX que aplasta reviews

- **MarketScreen L145-151**: compra sin validar fondos (TextButton sin `enabled`).
- **CryptoScreen**: Buy/Sell con `TextButton` pequeño, `qtyText` se comparte entre Buy/Sell/Stake.
- **RacingScreen L99-100**: tab activo se pierde en rotación (no usa `rememberSaveable`).
- **RacingScreen L336**: botón "+10" valida solo el coste del 1º upgrade, no los 10.
- **Banking**: Repay no tiene input de cantidad libre (solo "1 cuota" y "Liquidar").
- **JobsScreen L192**: aceptar bolsa de empleo "irreversible" sin dialog de confirmación.
- **Navigation Root**: `when` sin `else` en `SubScreenHost` → pantalla en blanco silenciosa con id desconocido.
- **Tutorial**: cobertura de solo 5 pilares de los 30+ sistemas; nuevo jugador se pierde.
- **Notifications Toast**: sin dismiss manual, duración fija 2400ms, pierde mensajes al desbordar.

### Balance económico

- **Doble payroll paralelo**: `Payroll.applyDaily` ignora mineros agregados; `CryptoEngine.dailyTick` los paga aparte. No hay vista unificada.
- **LIBRARIAN 18€/h**: jugador grindeando rinde ~37× más que un minero (11.67€/día).
- **ROI minero <70 días** sin coste de hardware/energía.
- **Exploit hire-and-fire**: contratar minero por 1 día y despedir es positivo (sin severance).
- **SHIPYARD 220k€** vs **FACTORY 55k€**: salto 4× sin progresión justificada.
- **JobsEngine wage**: `statMul = 1 + statValue * 0.005` con stat 1000+ (vía traits/prestige) → wage descontrolado.
- **HeistEngine L197**: prob. éxito sin cap → score >0.85 trivial con crew skill 99 + gear 250k.
- **HeistEngine L215**: si `sum(cutPct) > 1.0`, paga > botín → cash negativo.
- **MultiCityEngine**: arbitraje cíclico sin comisión ni límite de envíos por ruta.
- **OptionsEngine L42/L80**: naked puts/calls infinitos sin colateral.
- **Banking**: sin tope de endeudamiento simultáneo.

### Save/Load

- **Atomic write AUSENTE**: `SaveRepository.kt:28-31` rota backup antes de escribir nuevo payload sin tmp+rename+fsync.
- **Sin `schemaVersion` real**: migraciones dispersas, frágil para futuros cambios.
- **`repo.load()` no envuelto en try/catch** en `GameViewModel.kt:53` → crash al cargar save corrupto.
- **rngSeed default dinámico** (`System.currentTimeMillis()`): rompe determinismo si falta en JSON antiguo.
- **Stock.priceHistory** sin cota → infla saves indefinidamente (8 stocks × N ticks).
- **WorldEventState.seenIds** crece sin purga.

### Localización

- **0 ocurrencias de `stringResource(...)`** en TODA la carpeta de pantallas.
- **1163 `Text("...")` con literales** repartidos en 80 archivos.
- Para vender en Play Store global, traducir al inglés es obligatorio. Estimado: 850-950 claves únicas tras dedup, ~7-9 días de trabajo.

---

## P2 — PULIDO (semanas 2-3 antes de release)

### Comportamientos sutiles que afectan calidad percibida

- **Production L57-58**: `happinessFactor` solo penaliza (<50), no bonifica (>50) — asimetría.
- **DisasterEngine L165**: `productionMul` cap `<=1.0` (asimétrico vs los demás 0.1-2.0).
- **CryptoEngine L340/L349**: threshold "polvo" 1e-6 demasiado permisivo. Subir a 1e-8.
- **CryptoEngine L518**: `totalMiners()` no cacheado — recorre holdings cada llamada.
- **HrEngine L421-433**: refresh applicants reemplaza pool COMPLETA — el jugador pierde candidatos buenos.
- **ManagerEngine L292-298**: auto-assign solo 1 trabajador por tick → tarda 50s reales en llenar 50 plazas.
- **Achievements**: `ach_cash_1b` (1 trillón) demasiado alto, `ach_specialist_1000` puede ser inalcanzable según capacidad.
- **PrestigeEngine**: sin cap de level/points → multiplicador infinito en runs largos.
- **Mineros legacy** ya migrados, pero `Concurrency` H1-H6 siguen.
- **Render**: SkyEngine repinta gradient+sol+24 nubes+80 estrellas+luna+god rays cada frame aunque solo cambia animPhase.
- **IPO L34**: `estimateValuation()` sin cap → con cash 1e15 los números visualmente se rompen.
- **Achievements UI**: estado "Reclamado" sigue siendo un Button activo visual (L258-272).
- **WealthScreen**: solo cubre Bolsa + Inmuebles pese al nombre genérico "Wealth".
- **Concurrency H1-H6**: `gameLoop` y `mutate()` compiten sin Mutex compartido. Mayoría enmascarado por `Dispatchers.Main` pero suspension points (`withContext IO/Default`) abren ventanas de race.

### Modelos

- **Stock.priceHistory** sin window → bloat save.
- **Enums sin `@SerialName`** (`BuildingType`, `CityId`, `TutorialStep`, etc.): renombrar enum rompe saves.
- **TileType serializado por ordinal** en `WorldGrid.pack` → reordenar entradas rompe saves; debería ser append-only.
- **cash: Double** en Player/Company → debería ser `Long` (céntimos) o `BigDecimal` para precisión monetaria.
- **CompanyStock.sharesOwnedByPlayer** sin invariante vs `sharesOutstanding`.
- **Falta `init { require(...) }`** en data classes finance — saves manipulados pasan validación.

### Accesibilidad

- **Tap targets <48dp**: `HelpButton` (32dp), `PolishedTopBar` (36dp), `Toast` (28dp).
- **Iconos sin `contentDescription`**: solo 2 de ~80 archivos UI lo usan.
- **Texto <11sp**: nav bar (10sp), AchievementsScreen (10sp), AnimatedBadge (9sp), JobMinigames varios.
- **Contraste**: `Dim #8899AA` sobre `Ink #0F1724` queda al límite a 10-11sp.
- **Emoji-only buttons** sin contentDescription: `PolishedTopBar`, `HelpButton`, varios.

---

## P3 — NICE TO HAVE (post-launch)

- AICompanion: extraer constantes hardcoded.
- DailyChallenge: animación de "completaste reto, claim aquí".
- Quality of life: undo en demolish/sell, history de transacciones.
- Más temporadas/eventos.
- Modos de dificultad.
- Cloud save (Google Play Games).
- Achievements Play Games integration.
- Adaptive icon `monochrome` para themed icons Android 13+.
- Habilitar R8 minify+shrinkResources en release build.

---

## Resumen ejecutivo

| Prioridad | Total | Estado |
|---|---|---|
| **P0** | ~25 issues | Bloquean release, requieren fix antes de Play Store |
| **P1** | ~40 issues | Rompen experiencia, fix antes de beta |
| **P2** | ~80 issues | Pulido, fix en sprint dedicado |
| **P3** | ~50+ issues | Nice-to-have, post-launch |

### Roadmap a release sugerido

1. **Sprint 1 (1 semana)** — todos los P0:
   - Theme.kt fix (5 min).
   - Player.addXp fix (15 min).
   - GameViewModel mutate atómico (2-3h).
   - Confirmation dialogs en demolish/sell/closeBusiness (1 día).
   - SideQuest claimed flag + CompleteContracts progress (1 día).
   - ProductionLines toggle libera workers (2h).
   - JobBusiness payroll loop offline (2h).
   - Racing duplicaciones (1h).
   - WorldStates defaults (medio día).
   - WorldGrid.init resilient load (30min).
   - Privacy policy + applicationId/app_name (medio día).

2. **Sprint 2 (2 semanas)** — P1:
   - Performance pass (advanceOneSecond, WorldRender, runBlocking) (3-4 días).
   - UX confirmations + tap targets + RacingScreen rotation (2 días).
   - Balance pass (doble payroll, exploits) (2 días).
   - Save/Load atomic + try-catch (1 día).
   - Strings.xml extraction + traducción inglés (4-5 días).
   - Tutorial expansion (2-3 días).

3. **Sprint 3 (1 semana)** — P2 críticos + assets Play Store:
   - Modelos: SerialName, append-only enums, Long para cash.
   - Accesibilidad: contentDescription, tap targets, contraste.
   - Icono pro, capturas, descripción, feature graphic.
   - Beta cerrada con 10-20 testers.

4. **Sprint 4 (1 semana)** — bugs reportados por testers + soft launch.

**Total**: ~5 semanas a un developer trabajando sostenido. Lanzable en **6 semanas** con 1 semana de margen para imprevistos.

Después del lanzamiento, los P3 + features nuevas pueden venir en patches.

---

## Cómo se generó este informe

1. Plan en `audit/PLAN.md`.
2. Wave 1: 32 agentes auditaron `engine/*.kt` con prompts específicos por archivo.
3. Wave 2: 25 agentes auditaron `ui/screens/**/*.kt`.
4. Wave 3: 9 agentes auditaron `model/*.kt`.
5. Wave 4: 11 agentes auditaron temas transversales (save/load, balance, performance, tutorial, localización, navigation, notifications, world render, components, concurrency, accessibility).
6. Cada agente escribió su informe en `audit/<categoría>/<área>.md` con formato uniforme (Bugs críticos, Bugs balanceo, UX, Performance, Code smell, Localización, Recomendación P0-P3).
7. Este `RESUMEN.md` agrega los hallazgos más críticos por prioridad.

Para revisar un área concreta, ver el archivo individual.
