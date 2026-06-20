package com.hazelhope.dubster.hamtest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    val liveData by viewModel.liveData.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom)
    ) {
        Text(
            text = if (liveData.testComplete && liveData.finalScore >= 74)
                "You passed, with a score of ${liveData.finalScore}%"
            else if (liveData.testComplete) "You failed, with a score of ${liveData.finalScore}%"
            else "${liveData.questionsToGo} remaining",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        QuestionPoolJustQuestion(
            currentQuestion = currentQuestion.hamQuestion,
            selectedAnswer = currentQuestion.selectedAnswer,
            onSelect = {
                viewModel.setAnswer(it)
            },
            enabled = !liveData.testComplete,
            modifier = Modifier.verticalScroll(scrollState).weight(1f),
            hideIdentifier = true
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = {
                    viewModel.changeQuestion("previous")
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painterResource(R.drawable.outline_arrow_back),
                    contentDescription = "Previous question"
                )
            }
            IconButton(
                onClick = {
                    if (liveData.questionsToGo > 0) {
                        viewModel.changeQuestion("next")
                    } else {
                        viewModel.finishAndGrade()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                if (liveData.questionsToGo > 0) {
                    Icon(
                        painterResource(R.drawable.outline_arrow_forward),
                        contentDescription = "Next question"
                    )
                } else {
                    Icon(
                        painterResource(R.drawable.outline_check),
                        contentDescription = "Finish test"
                    )
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