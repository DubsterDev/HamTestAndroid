package com.hazelhope.dubster.hamtest

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlin.math.floor

class PracticeTestViewModel(application: Application) : AndroidViewModel(application) {

    private val _placeholderQuestion = PracticeTestQuestion(
        selectedAnswer = -1,
        hamQuestion = HamQuestion(
            id = "BOB",
            correct = 0,
            refs = "no_refs",
            question = "Does this take a while to load?",
            answers = listOf("No", "Absolutely not", "Yes", "Of course"),
            figure = "",
            correctLetter = "A"
        )
    )

    private var quizType: String = "unknown"

    private var _quiz: List<PracticeTestQuestion> = listOf(
        _placeholderQuestion
    )

    private var _generatedQuiz = mutableListOf<PracticeTestQuestion>()

    private val _currentQuestion = MutableStateFlow(_placeholderQuestion)
    val currentQuestion: StateFlow<PracticeTestQuestion> = _currentQuestion.asStateFlow()

    private val _liveData = MutableStateFlow(PracticeTestLiveData(
        currentQuestionNumber = 0,
        questionsToGo = 300,
        testComplete = false,
        finalScore = 900
    ))
    val liveData: StateFlow<PracticeTestLiveData> = _liveData.asStateFlow()

    private var _currentQuestionNumber = 0

    fun loadQuiz(quizType: String) {
        if (this.quizType != quizType) {
            this.quizType = quizType

            val rawQuiz = this.application.assets.open("$quizType.json").bufferedReader().use { it.readText() }
            _quiz = Json.decodeFromString<List<HamQuestion>>(rawQuiz).map { hamQuestion ->
                PracticeTestQuestion(
                    selectedAnswer = 0,
                    hamQuestion = hamQuestion
                )
            }

            generateQuiz()
        }
    }

    fun generateQuiz() {
        val dividedQuestions = mutableMapOf<String, List<PracticeTestQuestion>>()
        _quiz.forEach { question ->
            val element = question.hamQuestion.id.take(3)
            var oldValue = listOf<PracticeTestQuestion>()
            if (dividedQuestions.containsKey(element)) {
                oldValue = dividedQuestions.getValue(element)
            }
            dividedQuestions[element] = oldValue + question
        }

        dividedQuestions.forEach { (_, questions) ->
            val randomQuestion = questions.random()
            val randomizedQuestion = shuffleHamQuestion(randomQuestion.hamQuestion, false)
            _generatedQuiz.add(
                PracticeTestQuestion(
                    hamQuestion = randomizedQuestion,
                    selectedAnswer = -1
                )
            )
        }

        _generatedQuiz.shuffle()

        _currentQuestion.update {
            _generatedQuiz[_currentQuestionNumber]
        }

        _liveData.update {
            it.copy(
                currentQuestionNumber = _currentQuestionNumber,
                questionsToGo = (_generatedQuiz.size - 1) - _currentQuestionNumber
            )
        }
    }

    fun setAnswer(answer: Int) {
        _generatedQuiz[_currentQuestionNumber] = _generatedQuiz[_currentQuestionNumber].copy(
            selectedAnswer = answer
        )

        _currentQuestion.update {
            _generatedQuiz[_currentQuestionNumber]
        }
    }

    fun changeQuestion(direction: String) {
        if (direction == "next") {
            _currentQuestionNumber++
        } else if (direction == "previous") {
            _currentQuestionNumber--
        }

        if (_currentQuestionNumber < 0) _currentQuestionNumber = 0
        else if (_currentQuestionNumber == _generatedQuiz.size) _currentQuestionNumber = _generatedQuiz.size - 1

        _currentQuestion.update {
            _generatedQuiz[_currentQuestionNumber]
        }

        _liveData.update {
            it.copy(
                currentQuestionNumber = _currentQuestionNumber,
                questionsToGo = (_generatedQuiz.size - 1) - _currentQuestionNumber
            )
        }
    }

    fun finishAndGrade() {
        var correctQuestions = 0

        _generatedQuiz.forEach { question ->
            if (question.selectedAnswer == question.hamQuestion.correct) correctQuestions++
        }

        val score = ((correctQuestions * 1f) / (_generatedQuiz.size)) * 100f

        Log.d("TAG", "finishAndGrade: User got $score")

        val finalScore = floor(score).toInt()
        _liveData.update {
            it.copy(
                testComplete = true,
                finalScore = finalScore
            )
        }
    }
}