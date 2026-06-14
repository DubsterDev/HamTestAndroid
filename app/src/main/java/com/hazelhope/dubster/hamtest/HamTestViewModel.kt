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
    )
        .addMigrations(MIGRATION_1_2)
        .build()

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

    private val _questionPoolData = MutableStateFlow(QuestionPoolLiveData(
        totalPoolSize = 1,
        inUsePoolSize = 1,
        weakQuestions = 1,
        currentQuestionScore = 0
    ))
    val questionPoolData: StateFlow<QuestionPoolLiveData> = _questionPoolData.asStateFlow()

    fun loadQuiz(quizType: String) {
        if (this.quizType != quizType) {
            this.quizType = quizType
            val rawQuiz = this.application.assets.open("$quizType.json").bufferedReader().use { it.readText() }
            _quiz = Json.decodeFromString<List<HamQuestion>>(rawQuiz)

            _questionPoolData.update {
                QuestionPoolLiveData(
                    totalPoolSize = _quiz.size,
                    inUsePoolSize = 0,
                    weakQuestions = 0,
                    currentQuestionScore = 0
                )
            }
            nextQuestion(false, specialFirstQuestion = true)
        }
    }

    fun nextQuestion(wasQuestionWrong: Boolean, specialFirstQuestion: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            val dao = _db.userQuestionDao()

            if (!specialFirstQuestion) {
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

            var scoresLessThanZero = dao.getAmountOfScoresLessThanZero(quizType)

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
                        score = -2,
                        firstTime = true
                    )

                    dao.insertAll(newQuestionInfo)

                    allQuestions = listOf(newQuestionInfo) + allQuestions
                    scoresLessThanZero = listOf(newQuestionInfo) + scoresLessThanZero
                }
            }

            var allHamQuestions = allQuestions.mapNotNull { questionInfo ->
                _quiz.find { it.id == questionInfo.id }?.copy(
                    userQuestionInfo = questionInfo
                )
            }

            if (allHamQuestions.isEmpty()) {
                allHamQuestions = listOf(_placeholderQuestion)
            }

            val shouldUseRandomQuestion = Random.nextDouble() >= .6

            var randomQuestion = allHamQuestions[0]

            if (shouldUseRandomQuestion) {
                val onlyCorrectHamQuestions = allHamQuestions.filter {
                    (it.userQuestionInfo?.score ?: -10) >= 0
                }

                randomQuestion = if (onlyCorrectHamQuestions.isNotEmpty()) {
                    onlyCorrectHamQuestions.random()
                } else {
                    allHamQuestions.random()
                }
            }

            _questionPoolData.update { oldData ->
                oldData.copy(
                    inUsePoolSize = allQuestions.size,
                    weakQuestions = scoresLessThanZero.size,
                    currentQuestionScore = randomQuestion.userQuestionInfo?.score ?: -2
                )
            }

            _currentQuestion.update {
                shuffleHamQuestion(randomQuestion)
            }
        }
    }

    fun shuffleHamQuestion(hamQuestion: HamQuestion): HamQuestion {
        val correctAnswer = hamQuestion.answers[hamQuestion.correct]
        val originalShuffledAnswers = hamQuestion.answers.shuffled()

        val shuffledAnswers = if (hamQuestion.userQuestionInfo?.firstTime == true) {
            listOf(correctAnswer) + originalShuffledAnswers.filter { answer -> answer != correctAnswer }
        } else originalShuffledAnswers

        val newCorrectAnswer = shuffledAnswers.indexOf(correctAnswer)
        val newCorrectAnswerLetter = listOf("A", "B", "C", "D")[newCorrectAnswer]
        return hamQuestion.copy(
            answers = shuffledAnswers,
            correct = newCorrectAnswer,
            correct_letter = newCorrectAnswerLetter
        )
    }

}