# Concurrencia — GameViewModel.kt

**Archivo:** `app/src/main/java/com/empiretycoon/game/data/GameViewModel.kt`
**Fecha:** 2026-05-10
**Alcance:** Race conditions entre `gameLoop`, `saveLoop`, `mutate()`, dispatchers y acceso a `_state.value`.

## Hallazgos

### H1 — `mutate()` no es thread-safe (CRITICO) — L153-158
```
val prev = _state.value
val next = block(prev)
_state.value = TutorialEngine.checkAdvance(prev, next)
```
Lee-modifica-escribe NO atomico sobre `MutableStateFlow`. Si `gameLoop` (L101-107) corre en paralelo con cualquier `fun` del bridge (build/buy/hire/...), puede ocurrir lost-update: ambos leen `prev`, ambos calculan `next`, el ultimo `_state.value = …` pisa el otro. `MutableStateFlow.value` solo garantiza visibilidad atomica, no compare-and-set. **Bug latente:** L105 `_state.value = GameEngine.advanceSeconds(s, …)` puede sobrescribir un `mutate {}` reciente del usuario (ej. `cryptoBuy` justo antes del tick).

### H2 — `gameLoop` no usa `mutate()` — L100-108
El loop de tick salta el wrapper de tutorial (`TutorialEngine.checkAdvance`) y ademas no comparte ninguna primitiva de exclusion con las acciones del usuario. No hay `Mutex` que serialice escrituras a `_state`.

### H3 — `applyWorldMove` tampoco es atomico — L354-362
Mismo patron read-modify-write fuera de `mutate()`. En 60fps + tick 1Hz la probabilidad de colision es baja pero no nula; el resultado seria un movimiento "rebobinado".

### H4 — `saveLoop` lee snapshot fuera de lock — L109-114
`safeSave(_state.value.copy(...))` toma `.value` SIN `saveMutex`. El mutex solo protege la I/O (L41-46), no la captura del snapshot. Si `gameLoop`/`mutate` escriben justo entre `_state.value` y el `withLock`, el save puede capturar un estado a medio modificar (en la practica sano por inmutabilidad de `GameState`, pero el comentario L35-37 promete mas de lo que entrega: NO previene "estado intermedio", solo previene writes concurrentes al disco).

### H5 — `runBlocking` en `onCleared` — L123-125
`runBlocking(NonCancellable)` en main thread. Si I/O tarda >5s -> ANR al cerrar Activity. Aceptable como trade-off de no perder save, pero no es concurrencia segura: si `saveLoop` ya esta dentro del `withLock` cuando se llama `cancel()` (L122) + `runBlocking` espera al mutex en main -> bloqueo del UI thread.

### H6 — Dispatchers — L49, L99, L552
`viewModelScope.launch` sin dispatcher = `Dispatchers.Main.immediate`. Todos los `mutate()` y el `gameLoop` ejecutan en Main. Esto **acidentalmente serializa** la mayoria de race conditions de H1/H2 (Main es single-threaded) — pero `withContext(Dispatchers.IO/Default)` en L53,64,42 suspende y permite reentrancia desde Main, asi que la garantia no es solida.

## Recomendacion
Envolver `mutate` y `gameLoop` con un `Mutex` compartido (`stateMutex.withLock { _state.update { … } }`) o usar `MutableStateFlow.update { }` (CAS atomico). Capturar snapshot del save dentro del lock.
