package com.hazelhope.dubster.hamtest

import android.app.Application
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
import kotlin.random.Random

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

    private var _quiz = listOf(
        _placeholderQuestion
    )

    private val _currentQuestion = MutableStateFlow(_placeholderQuestion)
    val currentQuestion: StateFlow<HamQuestion> = _currentQuestion.asStateFlow()

    fun loadQuiz(quizType: String) {
        if (this.quizType != quizType) {
            this.quizType = quizType
            val rawQuiz = this.application.assets.open("$quizType.json").bufferedReader().use { it.readText() }
            _quiz = Json.decodeFromString<List<HamQuestion>>(rawQuiz)
            nextQuestion(false, _specialFirstQuestion = true)
        }
    }

    fun nextQuestion(wasQuestionWrong: Boolean, _specialFirstQuestion: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            val dao = _db.userQuestionDao()

            if (!_specialFirstQuestion) {
                val oldUserQuestion = dao.loadAllByIds(listOf(_currentQuestion.value.id))
                var score = -2
                var didExist = false
                if (!oldUserQuestion.isEmpty()) {
                    score = oldUserQuestion[0].score
                    didExist = true
                }
                if (wasQuestionWrong) score--
                else score++

                if (didExist) {
                    dao.updateScore(_currentQuestion.value.id, score)
                } else {
                    dao.insertAll(UserQuestionInfo(
                        id = _currentQuestion.value.id,
                        pool = quizType,
                        score = score
                    ))
                }
            }

            var allQuestions = dao.getAll(quizType)

            val scoresLessThanZero = dao.getAmountOfScoresLessThanZero(quizType)

            if (scoresLessThanZero.isEmpty()) {
                val allQuestionIds = allQuestions.map { it.id }
                val filteredQuestions = _quiz.filter {
                    !allQuestionIds.contains(it.id)
                }

                if (!filteredQuestions.isEmpty()) {
                    val newQuestion = filteredQuestions.random()
                    val newQuestionInfo = UserQuestionInfo(
                        id = newQuestion.id,
                        pool = quizType,
                        score = -2
                    )

                    dao.insertAll(newQuestionInfo)

                    allQuestions = listOf(newQuestionInfo) + allQuestions
                }
            }

            var allHamQuestions = allQuestions.mapNotNull { questionInfo ->
                _quiz.find { it.id == questionInfo.id }
            }

            if (allHamQuestions.isEmpty()) {
                allHamQuestions = listOf(_placeholderQuestion)
            }

            val shouldUseRandomQuestion = Random.nextDouble()

            var randomQuestion = allHamQuestions[0]

            if (shouldUseRandomQuestion >= .6) {
                val onlyCorrectQuestions = allQuestions.filter { it.score >= 0 }
                val onlyCorrectHamQuestions = onlyCorrectQuestions.mapNotNull { questionInfo ->
                    _quiz.find { it.id == questionInfo.id }
                }

                randomQuestion = if (onlyCorrectHamQuestions.isNotEmpty()) {
                    onlyCorrectHamQuestions.random()
                } else {
                    allHamQuestions.random()
                }
            }

            _currentQuestion.update {
                shuffleHamQuestion(randomQuestion)
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