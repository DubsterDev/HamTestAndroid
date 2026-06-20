package com.hazelhope.dubster.hamtest

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