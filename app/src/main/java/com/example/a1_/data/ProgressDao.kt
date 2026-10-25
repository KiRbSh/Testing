package com.example.a1_.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    // Mode Progress
    @Query("SELECT * FROM mode_progress WHERE packId = :packId")
    fun getAllModeProgress(packId: String): Flow<List<ModeProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveModeProgress(progress: ModeProgress)

    @Query("DELETE FROM mode_progress WHERE packId = :packId AND mode = :mode AND topicId = :topicId")
    suspend fun clearModeProgress(packId: String, mode: String, topicId: String)

    @Query("DELETE FROM mode_progress WHERE packId = :packId AND mode = :mode")
    suspend fun clearAllModeProgress(packId: String, mode: String)

    // Question Flags (Global per pack)
    @Query("SELECT * FROM question_flags WHERE packId = :packId")
    fun getAllFlags(packId: String): Flow<List<QuestionFlags>>

    @Query("SELECT * FROM question_flags WHERE packId = :packId AND questionId = :id")
    suspend fun getFlagsForQuestion(packId: String, id: String): QuestionFlags?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFlags(flags: QuestionFlags)

    @Query("UPDATE question_flags SET isFavorite = :isFav WHERE packId = :packId AND questionId = :id")
    suspend fun updateFavorite(packId: String, id: String, isFav: Boolean)

    @Query("UPDATE question_flags SET isError = :isErr WHERE packId = :packId AND questionId = :id")
    suspend fun updateError(packId: String, id: String, isErr: Boolean)

    @Query("UPDATE question_flags SET isError = 0 WHERE packId = :packId")
    suspend fun clearAllErrors(packId: String)

    @Query("UPDATE question_flags SET isFavorite = 0 WHERE packId = :packId")
    suspend fun clearAllFavorites(packId: String)
    @Query("DELETE FROM mode_progress WHERE packId = :packId")
    suspend fun deleteAllProgressForPack(packId: String)

    @Query("DELETE FROM question_flags WHERE packId = :packId")
    suspend fun deleteAllFlagsForPack(packId: String)
}
