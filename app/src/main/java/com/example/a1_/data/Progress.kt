package com.example.a1_.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mode_progress", primaryKeys = ["packId", "mode", "topicId", "questionId"])
data class ModeProgress(
    val packId: String,
    val mode: String,
    val topicId: String = "",
    val questionId: String,
    val selectedOption: Int,
    val isCorrect: Boolean
)

@Entity(tableName = "question_flags", primaryKeys = ["packId", "questionId"])
data class QuestionFlags(
    val packId: String,
    val questionId: String,
    val isFavorite: Boolean = false,
    val isError: Boolean = false
)
