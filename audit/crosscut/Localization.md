# Auditoría de Localización — balwealth

## Estado actual

- `res/values/strings.xml` contiene **1 sola entrada** (`app_name`).
- No existe `values-en/strings.xml` ni ningún otro `values-<locale>`.
- Búsqueda en `ui/screens/` (80 archivos `.kt`):
  - `Text("...")` con literal hardcodeado: **1163 ocurrencias** en 80 archivos.
  - `stringResource(R.string....)`: **0 ocurrencias**.
- Cobertura de localización efectiva: **~0%**. El juego está 100% en español embebido en código Kotlin/Compose.

## Top pantallas por carga de literales

| Pantalla | Literales `Text("...")` |
|---|---|
| RacingScreen.kt | 111 |
| EmpireScreen.kt | 37 |
| WealthScreen.kt | 34 |
| OptionsScreen.kt | 33 |
| CryptoScreen.kt | 26 |
| BankingScreen.kt | 23 |
| HrScreen.kt | 23 |
| SideQuestsScreen.kt | 22 |
| MoreScreen.kt | 19 |
| PilotJobScreen.kt | 18 |
| MultiCityScreen.kt | 18 |
| HouseScreen.kt / IpoScreen.kt / JobBusinessScreen.kt | 17 c/u |
| HeistsScreen.kt / NpcsScreen.kt / ArcadeScreen.kt / FirefighterJobScreen.kt / PrestigeScreen.kt | 15-16 |
| Resto (~60 pantallas, mayoría jobs/) | 3-15 c/u |

Total estimado de claves a extraer (descontando duplicados como "Comprar", "Cobrar", "Cancelar", emojis sueltos): **~850-950 strings únicos**.

## Ejemplos representativos

- `CasinoScreen.kt:61` — `Text("🎰 Casino — Ruleta", ...)`
- `CasinoScreen.kt:62` — `Text("Caja empresa: ${state.company.cash.fmtMoney()}", ...)` (interpolación, requiere `getString(R.string.x, arg)`)
- `CasinoScreen.kt:115` — `label = { Text("Apuesta (€)") }` (símbolo monetario hardcodeado)
- `HomeScreen.kt:49` — `Text("$salute, ${state.player.name}", ...)` (saludo dependiente de hora del día)
- `HomeScreen.kt:52` — `Text("Día ${state.day} · Hora ${"%02d".format(hour)}:00", ...)`
- `IpoScreen.kt:30-31` — `Tab(... text = { Text("Comprar") })` / `Text("Mis posiciones")` (string repetido en muchas pantallas).

## Plan de extracción recomendado

**Fase 1 — Infraestructura (1 día)**
1. Crear `values-en/strings.xml` vacío y `locales_config.xml` (Android 13+).
2. Añadir `android:localeConfig` en `AndroidManifest.xml`.
3. Definir convención de naming: `<screen>_<element>_<purpose>` (ej. `casino_title`, `casino_bet_label`, `home_greeting_morning`).

**Fase 2 — Tokens compartidos (1 día)**
Extraer primero strings repetidos de alta frecuencia (`Comprar`, `Vender`, `Cancelar`, `Cobrar`, `Apuesta`, `Caja empresa`, días/horas, sufijos `xp`, `rep`). Estimado: 40-60 claves cubren ~30% de ocurrencias.

**Fase 3 — Por pantalla, ordenado por ROI (3-5 días)**
Atacar primero pantallas con >20 literales (Racing, Empire, Wealth, Options, Crypto, Banking, Hr, SideQuests). Para interpolaciones usar `stringResource(R.string.x, arg1, arg2)` con placeholders `%1$s`/`%2$d`.

**Fase 4 — Jobs (2 días)**
~50 `*JobScreen.kt` con 11-18 literales c/u; muchos siguen plantilla similar — considerar un `JobScreenStrings` data class poblado vía `stringResource` para reducir boilerplate.

**Fase 5 — Traducción y QA (paralelo)**
Pasar `values/strings.xml` (es) a traductor → `values-en/strings.xml`. Verificar pluralización (`<plurals>`) para "X días", "X monedas". Cuidar el símbolo `€` (no traducible automáticamente — usar `NumberFormat.getCurrencyInstance(locale)` en `fmtMoney()`).

**Riesgos**: emojis embebidos en strings (válidos pero revisar render); strings con lógica condicional dentro del compose (`if(...) "A" else "B"`); formateo monetario actualmente locale-blind.
