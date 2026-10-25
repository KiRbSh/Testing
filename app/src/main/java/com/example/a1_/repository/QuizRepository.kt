package com.example.a1_.repository

import android.content.Context
import com.example.a1_.data.ModeProgress
import com.example.a1_.data.ProgressDao
import com.example.a1_.data.Question
import com.example.a1_.data.QuestionFlags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class QuizRepository(private val context: Context, private val progressDao: ProgressDao) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadQuestions(file: File?): List<Question> = withContext(Dispatchers.IO) {
        if (file == null || !file.exists()) return@withContext emptyList()
        try {
            val jsonString = file.readText()
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getAllModeProgress(packId: String): Flow<List<ModeProgress>> = progressDao.getAllModeProgress(packId)
    fun getAllFlags(packId: String): Flow<List<QuestionFlags>> = progressDao.getAllFlags(packId)

    suspend fun saveModeProgress(progress: ModeProgress) {
        progressDao.saveModeProgress(progress)
    }

    suspend fun clearModeProgress(packId: String, mode: String, topicId: String = "") {
        if (topicId.isEmpty()) {
            progressDao.clearAllModeProgress(packId, mode)
        } else {
            progressDao.clearModeProgress(packId, mode, topicId)
        }
    }

    suspend fun updateFavorite(packId: String, id: String, isFavorite: Boolean) {
        val current = progressDao.getFlagsForQuestion(packId, id)
        if (current == null) {
            progressDao.saveFlags(QuestionFlags(packId, id, isFavorite = isFavorite))
        } else {
            progressDao.updateFavorite(packId, id, isFavorite)
        }
    }

    suspend fun updateError(packId: String, id: String, isError: Boolean) {
        val current = progressDao.getFlagsForQuestion(packId, id)
        if (current == null) {
            progressDao.saveFlags(QuestionFlags(packId, id, isError = isError))
        } else {
            progressDao.updateError(packId, id, isError)
        }
    }

    suspend fun clearAllErrors(packId: String) {
        progressDao.clearAllErrors(packId)
    }

    suspend fun clearAllFavorites(packId: String) {
        progressDao.clearAllFavorites(packId)
    }

    suspend fun deletePackData(packId: String) {
        progressDao.deleteAllProgressForPack(packId)
        progressDao.deleteAllFlagsForPack(packId)
    }
}
