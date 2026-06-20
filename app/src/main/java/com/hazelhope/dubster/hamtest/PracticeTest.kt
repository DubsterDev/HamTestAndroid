package com.hazelhope.dubster.hamtest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hazelhope.dubster.hamtest.ui.theme.HamTestTheme

@Composable
fun Practice(
    goToTest: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Button({
        goToTest("general")
    },
        modifier = modifier) {
        Text(
            text = "General"
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

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        QuestionPoolJustQuestion(
            currentQuestion = currentQuestion.hamQuestion,
            selectedAnswer = currentQuestion.selectedAnswer,
            onSelect = {
                viewModel.setAnswer(it)
            },
            enabled = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PracticePreview() {
    HamTestTheme {
        Practice({})
    }
}