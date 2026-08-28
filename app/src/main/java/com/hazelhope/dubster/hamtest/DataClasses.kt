package com.hazelhope.dubster.hamtest

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HamQuestion(
    val id: String,
    val correct: Int,
    val refs: String,
    val question: String,
    val answers: List<String>,
    val figure: String,
    @SerialName("correct_letter") val correctLetter: String,
    val explanation: String? = null,
    val userQuestionInfo: UserQuestionInfo? = null
)

@Serializable
data class PracticeTestQuestion(
    val hamQuestion: HamQuestion,
    val selectedAnswer: Int
)

@Serializable
data class PracticeTestLiveData(
    val currentQuestionNumber: Int,
    val questionsToGo: Int,
    val testComplete: Boolean,
    val finalScore: Int
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
    val lastSeenAt: Int,
    val firstTime: Boolean = false
)

@Entity(tableName = "settings")
@Serializable
data class SettingsItem(
    @PrimaryKey val id: String,
    val value: String,
)