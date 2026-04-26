# 🧪 BalWealth — QA, Bugfix & Senior Review Team (90 roles)

> Estructura organizativa del equipo de calidad asignado a la auditoría
> de regresión de la versión v9. Tres líneas de trabajo en cascada:
> **30 testers** → **30 bugfixers** → **30 senior reviewers**.

---

## 🔍 Línea 1: 30 QA Testers (encuentran bugs, no tocan código)

| # | Rol | Área asignada |
|---|---|---|
| 01 | Render Tester | WorldScreen, RenderEngine, depth sort, sky |
| 02 | Vehicle Tester | Cars catalog, dealership, garage, driving |
| 03 | Life Systems Tester | Pets, follower NPC, UFOs |
| 04 | Market Tester | Stocks, dividends, market factors |
| 05 | Banking Tester | Loans, IPO, options, dividends de empresa |
| 06 | HR Tester | Employees, managers, payroll |
| 07 | Production Tester | Recipes, buildings, lines, contracts, quality |
| 08 | Audio/Tutorial Tester | Sound engine, music, tutorial flow |
| 09 | Persistence/Balance Tester | Save/load, offline progress, balance económico |
| 10 | Integration Tester | Root, navigation, MainTab vs subScreen |
| 11 | Sprite Tester | Avatar, NPC, prop sprites — visual fidelity |
| 12 | Camera Tester | Follow, lerp, screen edges |
| 13 | Joystick/Input Tester | Touch handling, dead zone, multitouch |
| 14 | Pixel Art Tester | TileRenderer textures, BuildingSpriteRenderer |
| 15 | Lighting Tester | Night mode, lamps, car headlights |
| 16 | Shadow Tester | Direction, length, ambient gating |
| 17 | Sky Tester | Hour transitions, stars, moon, sun |
| 18 | Weather Tester | Rain, snow, storm, fog particles |
| 19 | Traffic Tester | Vehicle pathing, collision, wrap-around |
| 20 | NPC Walker Tester | Routes, schedules, collision |
| 21 | Family Tester | Spouse aging, children growth |
| 22 | House Tester | Furniture placement, multi-cell items |
| 23 | Casino Tester | Roulette spin, payout math |
| 24 | Dream Tester | Lucid dream tone selection, fade transitions |
| 25 | Achievement Tester | Detection, claim, deduplication |
| 26 | Skill Tree Tester | Unlock, prereqs, point spend |
| 27 | Story/Quest Tester | Chapter triggers, side quests, NPCs |
| 28 | World Event Tester | 50 random events, choices, cooldown |
| 29 | Karma Echo Tester | Visual saturation, beggars, kids |
| 30 | Performance Tester | FPS, memory, GC pressure |

## 🛠 Línea 2: 30 Bugfixers (aplican parches a partir de los reportes)

| # | Rol | Especialidad |
|---|---|---|
| 31 | Render Bugfixer | Pipeline, depth sort, layer ordering |
| 32 | Sprite Bugfixer | Pixel-art primitives, color regressions |
| 33 | Camera Bugfixer | Smoothing, edge clamps |
| 34 | Input Bugfixer | Joystick, action button responsiveness |
| 35 | Lighting Bugfixer | Halo math, blend modes |
| 36 | Shadow Bugfixer | Direction, fade |
| 37 | Sky Bugfixer | Gradient transitions |
| 38 | Weather Bugfixer | Particle perf, blendmodes |
| 39 | Vehicle Bugfixer | Movement clamp, sell-while-driving |
| 40 | Pet Bugfixer | Following, pathing, hunger |
| 41 | Follower Bugfixer | Spawn safety, dialog dismiss |
| 42 | UFO Bugfixer | Lifecycle, off-screen cleanup |
| 43 | Family Bugfixer | DaysWith ticking, age increments |
| 44 | House Bugfixer | Multi-cell furniture, refund math |
| 45 | Market Bugfixer | Dividend pay, MAX overflow |
| 46 | Banking Bugfixer | Loan default, IPO valuation |
| 47 | Options Bugfixer | Pricer edge cases, ITM/OTM |
| 48 | HR Bugfixer | Worker assignment, churn |
| 49 | Manager Bugfixer | Whitelist, cooldowns, conflicts |
| 50 | Production Bugfixer | Cycle stability, output tier |
| 51 | Quality Bugfixer | Tier rolls, sell pricing |
| 52 | Lines Bugfixer | Orchestration, dead refs |
| 53 | Contracts Bugfixer | Auto-deliver, expiry penalties |
| 54 | Audio Bugfixer | MediaPlayer race, volume, mute |
| 55 | Tutorial Bugfixer | Coachmark anchor, completion |
| 56 | Dialog Bugfixer | Compact dialog, dismiss flow |
| 57 | Persistence Bugfixer | Save migration, defaults |
| 58 | Balance Bugfixer | Economic numbers, costs |
| 59 | Navigation Bugfixer | Tab/subScreen state, back button |
| 60 | Integration Bugfixer | Cross-system race conditions |

## ✅ Línea 3: 30 Senior Reviewers (aprueban o rechazan los fixes)

| # | Rol | Validación |
|---|---|---|
| 61 | Render Architect | Que el render pipeline siga lineal y O(n log n) |
| 62 | Visual QA Lead | Cambios visuales sin regresiones |
| 63 | Performance Lead | Sin memory leaks, FPS estable |
| 64 | Save System Lead | Compatibilidad backwards-compat |
| 65 | UX Lead | Flujos no rotos, onboarding intacto |
| 66 | Audio Lead | Toggle mute confiable, no clicks |
| 67 | Game Design Lead | Balance económico mantenido |
| 68 | Narrative Lead | Misiones e historia coherentes |
| 69 | Combat/Vehicle Lead | Driving feel justo |
| 70 | Life Systems Lead | Pets/family sin estados fantasma |
| 71 | Build Lead | Compila clean, sin warnings críticos |
| 72 | Test Coverage Lead | Casos extremos cubiertos |
| 73 | Security Lead | Sin entradas de usuario sin sanitizar |
| 74 | API Stability Lead | Bridge methods consistentes |
| 75 | i18n Lead | Strings en español sin hardcodes |
| 76 | Accessibility Lead | Contraste, tamaños mínimos |
| 77 | Code Quality Lead | Imports, naming, dead code |
| 78 | Compose Lead | Estado Compose correcto, sin recomposiciones excesivas |
| 79 | Coroutines Lead | Scope correcto, no leaks |
| 80 | Threading Lead | UI vs background coherente |
| 81 | Memory Lead | Asignaciones por frame |
| 82 | Crash-Free Lead | Try/catch en boundaries |
| 83 | Onboarding Lead | Tutorial completable |
| 84 | Save Migration Lead | v6/v7/v8 → v9 OK |
| 85 | Visual Polish Lead | HUD pulido, vignette correcto |
| 86 | Sprite Consistency Lead | Estilo coherente |
| 87 | Sound Design Lead | Música no saturante |
| 88 | World Population Lead | Densidad NPCs/coches/props equilibrada |
| 89 | Weather Lead | Transiciones suaves |
| 90 | Final Sign-off | Aprobador único de release |

---

## 🌊 Workflow

```
[Tester #N] → reporte de bugs → [Bugfixer #N+30] → parche → [Senior #N+60] → ✅ / 🔁
```

Cada fix sigue: **encontrado → arreglado → validado → mergeable**.
