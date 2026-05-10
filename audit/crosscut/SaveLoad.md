# Auditoría cross-cutting: Save / Load

Archivos analizados:
- `app/src/main/java/com/empiretycoon/game/data/SaveRepository.kt`
- `app/src/main/java/com/empiretycoon/game/data/GameViewModel.kt`

## Hallazgos

### 1. Atomic write — AUSENTE (severidad: alta)
`SaveRepository.kt:28-31` realiza `mainFile.copyTo(backupFile, overwrite=true)` seguido de `mainFile.writeText(payload)`. No hay escritura atómica (tmp + rename). Si el proceso muere a mitad de `writeText`, `save.json` queda truncado. El backup mitiga, pero la rotación ocurre **antes** de validar el nuevo payload, así que un kill durante `copyTo` puede dejar `save.bak.json` también corrupto. Recomendado: escribir a `save.json.tmp`, `flush+fsync`, luego `renameTo(mainFile)` (atómico en mismo FS) y solo después actualizar backup.

### 2. JSON unknown keys — OK
`SaveRepository.kt:16` configura `Json { ignoreUnknownKeys = true; encodeDefaults = true }`. Forward/backward compatible para campos eliminados o desconocidos.

### 3. Version migration — DÉBIL (severidad: media)
No existe campo `schemaVersion` en `GameState`. La migración de `WorldState` (`SaveRepository.kt:46-48`) depende solo de defaults del serializador; la migración de mineros legacy vive fuera del repo (`GameViewModel.kt:139-149`) y se aplica tras `load()` (`GameViewModel.kt:54`). No hay forma sistemática de versionar/migrar saves; futuros breaking changes obligarán a borrar saves vía catch (línea 50-54).

### 4. Mutex protection — PARCIAL (severidad: media)
`GameViewModel.kt:38-46` (`saveMutex`) serializa **escrituras** del `saveLoop` (línea 109-114) y `onCleared` (línea 123-125). PERO: `mutate()` en `GameViewModel.kt:153-158` no toma el mutex y modifica `_state.value` desde `gameLoop` y desde callbacks de UI sin sincronización entre sí. Carrera real: `gameLoop` (línea 100-107) avanza tick mientras la UI llama `mutate()`; ambos hacen read-modify-write sobre `_state` sin lock → última escritura gana. El mutex solo protege I/O, no consistencia de estado.

### 5. Exception handling — FRÁGIL (severidad: media)
`SaveRepository.kt:50-54` captura `Throwable` y borra el archivo silenciosamente. No hay logging ni notificación al usuario; un error transitorio (p.ej. I/O lleno, OOM en parser) destruye el save. `GameViewModel.kt:53` no envuelve `repo.load()` en try/catch — un fallo no manejado en `load` rompe `init`. `safeSave` (línea 40-46) tampoco captura excepciones de `repo.save`.

### 6. Otros
- `runBlocking` en `onCleared` (`GameViewModel.kt:123`) puede ANR si el FS está lento.
- `clear()` (`SaveRepository.kt:34-37`) ignora valor de retorno de `delete()`.

## Prioridad de fixes
1. Atomic write (tmp+rename+fsync).
2. Añadir `schemaVersion` y pipeline de migración explícita.
3. Mutex/Channel para serializar `mutate()` vs `gameLoop`.
4. Logging estructurado en parse errors antes de borrar.
