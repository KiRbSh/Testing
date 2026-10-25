package com.example.a1_.data

import kotlinx.serialization.Serializable

@Serializable
data class Question(
    val id: String,
    val section: String,
    val question: String,
    val options: List<String>,
    val answer: Int,
    val explanation: String? = null
)
