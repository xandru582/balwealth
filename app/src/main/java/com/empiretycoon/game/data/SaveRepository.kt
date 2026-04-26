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

    suspend fun load(): GameState? = withContext(Dispatchers.IO) {
        tryParse(mainFile) ?: tryParse(backupFile)
    }

    suspend fun save(state: GameState) = withContext(Dispatchers.IO) {
        val payload = json.encodeToString(GameState.serializer(), state)
        // rota backup
        if (mainFile.exists()) {
            mainFile.copyTo(backupFile, overwrite = true)
        }
        mainFile.writeText(payload)
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        mainFile.delete()
        backupFile.delete()
    }

    private fun tryParse(f: File): GameState? {
        if (!f.exists()) return null
        return try {
            val txt = f.readText()
            if (txt.isBlank()) return@tryParse null
            val parsed = json.decodeFromString(GameState.serializer(), txt)
            // Migración: si el save es de una versión que no incluía
            // `world` (v < 2), regenera el WorldState fresh en runtime.
            // Como el campo tiene default = WorldState.fresh(), el parser
            // ya lo rellena. Nada extra que hacer aquí.
            parsed
        } catch (t: Throwable) {
            // Save corrupto o de versión incompatible — descartado, se crea uno nuevo
            try { f.delete() } catch (_: Throwable) {}
            null
        }
    }
}
