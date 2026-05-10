package com.empiretycoon.game.data

import android.content.Context
import com.empiretycoon.game.model.GameState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persistencia simple a archivo JSON en filesDir. Un slot principal + un
 * backup rotativo. Todas las operaciones en IO dispatcher.
 */
class SaveRepository(private val appContext: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false; encodeDefaults = true }

    private val mainFile get() = File(appContext.filesDir, "save.json")
    private val backupFile get() = File(appContext.filesDir, "save.bak.json")
    private val tmpFile get() = File(appContext.filesDir, "save.tmp.json")
    private val brokenFile get() = File(appContext.filesDir, "save.broken.json")

    suspend fun load(): GameState? = withContext(Dispatchers.IO) {
        tryParse(mainFile) ?: tryParse(backupFile)
    }

    /**
     * FIX P0: atomic write con tmp+rename para evitar saves a medias si la
     * app se mata durante el writeText. Ahora la rotación es:
     *   1. escribe a save.tmp.json (commit completo)
     *   2. si éxito: rota main → backup, luego tmp → main (rename atómico)
     *   3. si fallo en cualquier paso, save.json original queda intacto.
     */
    suspend fun save(state: GameState) = withContext(Dispatchers.IO) {
        val payload = json.encodeToString(GameState.serializer(), state)
        // 1) Escribe a tmp (commit completo antes de tocar main)
        tmpFile.writeText(payload)
        // 2) Rota main → backup (preserva versión anterior)
        if (mainFile.exists()) {
            try { mainFile.copyTo(backupFile, overwrite = true) } catch (_: Throwable) {}
        }
        // 3) Reemplaza main por tmp (rename atómico en mismo filesystem)
        if (!tmpFile.renameTo(mainFile)) {
            // Fallback: copia y borra tmp
            tmpFile.copyTo(mainFile, overwrite = true)
            try { tmpFile.delete() } catch (_: Throwable) {}
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        mainFile.delete()
        backupFile.delete()
        tmpFile.delete()
    }

    private fun tryParse(f: File): GameState? {
        if (!f.exists()) return null
        return try {
            val txt = f.readText()
            if (txt.isBlank()) return null
            json.decodeFromString(GameState.serializer(), txt)
        } catch (t: Throwable) {
            // FIX P0: NO borramos el save. Lo movemos a save.broken.json
            // para que el jugador (o un futuro debug) pueda recuperar el
            // estado anterior si una migración futura sabe leerlo. Si ya
            // existía un broken, sobrescribir.
            try { f.copyTo(brokenFile, overwrite = true) } catch (_: Throwable) {}
            try { f.delete() } catch (_: Throwable) {}
            null
        }
    }
}
