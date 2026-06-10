package com.hazelhope.dubster.hamtest

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
data class HamQuestion(
    val id: String,
    val correct: Int,
    val refs: String,
    val question: String,
    val answers: List<String>,
    val figure: String,
    val correct_letter: String,
)

@Serializable
data class QuestionPoolLiveData(
    val totalPoolSize: Int,
    val inUsePoolSize: Int,
    val weakQuestions: Int
)

@Entity(tableName = "question_info")
data class UserQuestionInfo(
    @PrimaryKey val id: String,
    val pool: String,
    val score: Int
)