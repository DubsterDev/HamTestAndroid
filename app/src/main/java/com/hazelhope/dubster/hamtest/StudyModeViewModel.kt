package com.hazelhope.dubster.hamtest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class StudyModeViewModel(application: Application) : AndroidViewModel(application) {
    private var _db: HamTestDatabase? = null

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

    private var startTime = Clock.System.now()
    private var lastReminder = Clock.System.now()

    private val _shouldBreakTheFlow = MutableStateFlow(0)
    val shouldBreakTheFlow: StateFlow<Int> = _shouldBreakTheFlow.asStateFlow()

    fun setDatabase(db: HamTestDatabase) {
        _db = db
    }

    fun loadQuiz(quizType: String) {
        if (this.quizType != quizType) {
            this.quizType = quizType
            this.startTime = Clock.System.now()
            this.lastReminder = Clock.System.now()

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
        if (_shouldBreakTheFlow.value != 0) {
            _shouldBreakTheFlow.update { 0 }
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val dao = _db?.userQuestionDao() ?: return@launch
            val settingsDao = _db?.settingsDao() ?: return@launch
            val shouldAutoSelectCorrectAnswer = settingsDao.getValue("autoSelectCorrectAnswer").getOrNull(0)?.value == "true"
            val breakTheFlowEnabled = settingsDao.getValue("breakTheFlow").getOrNull(0)?.value == "true"

            if (!specialFirstQuestion) {
                val oldUserQuestion = dao.loadAllByIds(listOf(_currentQuestion.value.id))
                var score = if (shouldAutoSelectCorrectAnswer) -3 else -2
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
                        score = if (shouldAutoSelectCorrectAnswer) -3 else -2,
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
                shuffleHamQuestion(randomQuestion, shouldAutoSelectCorrectAnswer)
            }

            val now = Clock.System.now()
            val diff = now - lastReminder
            val totalDiff = now - startTime

            if (diff >= 15.minutes && breakTheFlowEnabled) {
                _shouldBreakTheFlow.update {
                    totalDiff.inWholeMinutes.toInt()
                }
                lastReminder = now
            }
        }
    }

    fun shuffleHamQuestion(hamQuestion: HamQuestion, autoSelectCorrectAnswer: Boolean): HamQuestion {
        val correctAnswer = hamQuestion.answers[hamQuestion.correct]
        val originalShuffledAnswers = hamQuestion.answers.shuffled()

        val shuffledAnswers = if (hamQuestion.userQuestionInfo?.firstTime == true && autoSelectCorrectAnswer) {
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