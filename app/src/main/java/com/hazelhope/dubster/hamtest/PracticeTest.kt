package com.hazelhope.dubster.hamtest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hazelhope.dubster.hamtest.ui.theme.HamTestTheme

@Composable
fun Practice(
    goToTest: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(scrollState)
    ) {
        PickAQuizCard(
            title = "Technician Test",
            description = "Take a practice test for the 2026-2030 question pool",
            onClick = {
                goToTest("technician")
            }
        )
        PickAQuizCard(
            title = "General Test",
            description = "Take a practice test for the 2023-2027 question pool",
            onClick = {
                goToTest("general")
            }
        )
        PickAQuizCard(
            title = "Extra Test",
            description = "Take a practice test for the 2024-2028 question pool",
            onClick = {
                goToTest("extra")
            }
        )
    }
}

@Composable
fun PracticeTest(
    quizType: String,
    modifier: Modifier = Modifier,
    viewModel: PracticeTestViewModel = viewModel()
) {
    LaunchedEffect(quizType) {
        viewModel.loadQuiz(quizType)
    }

    val currentQuestion by viewModel.currentQuestion.collectAsStateWithLifecycle()
    val liveData by viewModel.liveData.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom)
    ) {
        if (liveData.testComplete) {
            val passed = liveData.finalScore >= 74
            SuccessCard(
                isCorrect = passed,
                title = if (passed) "You passed!" else "You failed.",
                description = "You got ${liveData.finalScore}%"
            )
        } else {
            Text(
                text = "${liveData.questionsToGo} remaining",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
        QuestionPoolJustQuestion(
            currentQuestion = currentQuestion.hamQuestion,
            selectedAnswer = currentQuestion.selectedAnswer,
            onSelect = {
                viewModel.setAnswer(it)
            },
            enabled = !liveData.testComplete,
            modifier = Modifier.verticalScroll(scrollState).weight(1f),
            hideIdentifier = !liveData.testComplete,
            showInfoButton = currentQuestion.hamQuestion.explanation != null && liveData.testComplete
        )
        if (liveData.testComplete) {
            QuestionPoolSuccessCard(
                currentQuestion.hamQuestion.correct == currentQuestion.selectedAnswer,
                currentQuestion.hamQuestion.correctLetter,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(
                onClick = {
                    viewModel.changeQuestion("previous")
                },
                modifier = Modifier.weight(1f),
                enabled = liveData.currentQuestionNumber != 0
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                ) {
                    Icon(
                        painterResource(R.drawable.outline_arrow_back),
                        contentDescription = "Previous question"
                    )
                    Text(
                        text = "Previous"
                    )
                }
            }
            TextButton(
                onClick = {
                    if (liveData.questionsToGo > 0) {
                        viewModel.changeQuestion("next")
                    } else {
                        viewModel.finishAndGrade()
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = liveData.questionsToGo > 0 || !liveData.testComplete
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                ) {
                    if (liveData.questionsToGo > 0) {
                        Text(
                            text = "Next"
                        )
                        Icon(
                            painterResource(R.drawable.outline_arrow_forward),
                            contentDescription = "Next question"
                        )
                    } else {
                        Icon(
                            painterResource(R.drawable.outline_check),
                            contentDescription = "Finish test"
                        )
                        Text(
                            text = "Finish"
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PracticePreview() {
    HamTestTheme {
        Practice({})
    }
}