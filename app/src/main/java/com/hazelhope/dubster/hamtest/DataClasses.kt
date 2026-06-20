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
    val userQuestionInfo: UserQuestionInfo? = null
)

@Serializable
data class PracticeTestQuestion(
    val hamQuestion: HamQuestion,
    val selectedAnswer: Int
)

@Serializable
data class QuestionPoolLiveData(
    val totalPoolSize: Int,
    val inUsePoolSize: Int,
    val weakQuestions: Int,
    val currentQuestionScore: Int
)

@Entity(tableName = "question_info", primaryKeys = ["id", "pool"])
@Serializable
data class UserQuestionInfo(
    val id: String,
    val pool: String,
    val score: Int,
    val firstTime: Boolean = false
)

@Entity(tableName = "settings")
@Serializable
data class SettingsItem(
    @PrimaryKey val id: String,
    val value: String,
)