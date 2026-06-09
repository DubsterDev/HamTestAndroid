package com.hazelhope.dubster.hamtest

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class HamTestViewModel(application: Application) : AndroidViewModel(application) {
    private val _db = Room.databaseBuilder(
        application,
        HamTestDatabase::class.java,
        "ham-test-db"
    ).build()

    private val _placeholderQuestion = HamQuestion(
        id = "BOB",
        correct = 0,
        refs = "no_refs",
        question = "Does this take a while to load?",
        answers = listOf("No", "Absolutely not", "Yes", "Of course"),
        figure = "",
        correct_letter = "A"
    )

    private var quizType: String = "unknown"

    private val _quiz = MutableStateFlow(listOf(
        _placeholderQuestion
    ))
    val quiz: StateFlow<List<HamQuestion>> = _quiz.asStateFlow()

    private val _currentQuestionNumber = MutableStateFlow(0)

    private val _currentQuestion = MutableStateFlow(_placeholderQuestion)
    val currentQuestion: StateFlow<HamQuestion> = _currentQuestion.asStateFlow()

    fun loadQuiz(quizType: String) {
        this.quizType = quizType
        _currentQuestionNumber.update {
            0
        }
       _quiz.update {
           val rawQuiz = this.application.assets.open("$quizType.json").bufferedReader().use { it.readText() }
           Json.decodeFromString<List<HamQuestion>>(rawQuiz)
       }
    }

    fun nextQuestion(wasQuestionWrong: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            val oldUserQuestion = _db.userQuestionDao().loadAllByIds(listOf(_currentQuestion.value.id))
            var score = -2
            var didExist = false
            if (!oldUserQuestion.isEmpty()) {
                score = oldUserQuestion[0].score
                didExist = true
            }
            if (wasQuestionWrong) score--
            else score++

            Log.d("nextQuestion", "score: $score")

            if (didExist) {
                _db.userQuestionDao().updateScore(_currentQuestion.value.id, score)
            } else {
                _db.userQuestionDao().insertAll(UserQuestionInfo(
                    id = _currentQuestion.value.id,
                    pool = quizType,
                    score = score
                ))
            }
            _currentQuestion.update {
                shuffleHamQuestion(_quiz.value[_currentQuestionNumber.value])

            }
            _currentQuestionNumber.update { currentNumber ->
                currentNumber + 1
            }
        }
    }

    fun shuffleHamQuestion(hamQuestion: HamQuestion): HamQuestion {
        val correctAnswer = hamQuestion.answers[hamQuestion.correct]
        val shuffledAnswers = hamQuestion.answers.shuffled()
        val newCorrectAnswer = shuffledAnswers.indexOf(correctAnswer)
        val newCorrectAnswerLetter = listOf("A", "B", "C", "D")[newCorrectAnswer]
        return hamQuestion.copy(
            answers = shuffledAnswers,
            correct = newCorrectAnswer,
            correct_letter = newCorrectAnswerLetter
        )
    }

}