package com.example.a1_.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.dataStore by preferencesDataStore(name = "settings")

data class PackInfo(val id: String, val name: String, val isExternal: Boolean)

data class ExamSettings(
    val questionCount: Int = 14,
    val maxErrors: Int = 2,
    val penaltyEnabled: Boolean = false
)

class PackManager(private val context: Context) {
    private val LAST_PACK_KEY = stringPreferencesKey("last_pack_id")
    private val EXAM_QUEST_COUNT = intPreferencesKey("exam_quest_count")
    private val EXAM_MAX_ERRORS = intPreferencesKey("exam_max_errors")
    private val EXAM_PENALTY = booleanPreferencesKey("exam_penalty")
    
    private val packsDir = File(context.filesDir, "packs")

    init {
        if (!packsDir.exists()) packsDir.mkdirs()
    }

    val activePackId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LAST_PACK_KEY]
    }

    val settings: Flow<ExamSettings> = context.dataStore.data.map { prefs ->
        ExamSettings(
            questionCount = prefs[EXAM_QUEST_COUNT] ?: 14,
            maxErrors = prefs[EXAM_MAX_ERRORS] ?: 2,
            penaltyEnabled = prefs[EXAM_PENALTY] ?: false
        )
    }

    suspend fun updateSettings(settings: ExamSettings) {
        context.dataStore.edit { prefs ->
            prefs[EXAM_QUEST_COUNT] = settings.questionCount
            prefs[EXAM_MAX_ERRORS] = settings.maxErrors
            prefs[EXAM_PENALTY] = settings.penaltyEnabled
        }
    }

    suspend fun setActivePack(packId: String?) {
        context.dataStore.edit { preferences ->
            if (packId == null) preferences.remove(LAST_PACK_KEY)
            else preferences[LAST_PACK_KEY] = packId
        }
    }

    fun getAvailablePacks(): List<PackInfo> {
        return packsDir.listFiles { file -> file.extension == "json" }
            ?.map { PackInfo(it.name, it.nameWithoutExtension, true) } ?: emptyList()
    }

    fun importPack(uri: Uri, fileName: String): String? {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (content != null) {
                val destFile = File(packsDir, fileName)
                destFile.writeBytes(content)
                fileName
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getPackFile(packId: String): File? {
        val file = File(packsDir, packId)
        return if (file.exists()) file else null
    }

    fun deletePack(packId: String): Boolean {
        val file = File(packsDir, packId)
        return if (file.exists()) file.delete() else false
    }
}
