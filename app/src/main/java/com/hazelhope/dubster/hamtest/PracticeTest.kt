package com.hazelhope.dubster.hamtest

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hazelhope.dubster.hamtest.ui.theme.HamTestTheme
import com.hazelhope.dubster.hamtest.ui.theme.extendedColors
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeTest(
    quizType: String,
    db: HamTestDatabase,
    modifier: Modifier = Modifier,
    viewModel: PracticeTestViewModel = viewModel()
) {
    LaunchedEffect(db) {
        viewModel.setDatabase(db)
    }

    LaunchedEffect(quizType) {
        viewModel.loadQuiz(quizType)
    }

    val currentQuestion by viewModel.currentQuestion.collectAsStateWithLifecycle()
    val liveData by viewModel.liveData.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    var showTestFinished by remember { mutableStateOf(false) }

    LaunchedEffect(liveData.testComplete) {
        if (liveData.testComplete) {
            showTestFinished = true
        }
    }

    if (showTestFinished) {
        ModalBottomSheet(
            onDismissRequest = {
                showTestFinished = false
            },
            sheetState = sheetState,
            containerColor = if (liveData.finalScore >= 74) MaterialTheme.extendedColors.successContainer else MaterialTheme.colorScheme.errorContainer
        ) {
            TestFinished(
                liveData.finalScore,
                {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showTestFinished = false
                        }
                    }
                }
            )
        }
    }

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

@Composable
fun TestFinished(
    score: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val passed = score >= 74
    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(if (passed) MaterialTheme.extendedColors.successContainer else MaterialTheme.colorScheme.errorContainer)
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Icon(
            painterResource(if (passed) R.drawable.outline_check_circle else R.drawable.outline_cancel),
            contentDescription = null,
            tint = if (passed) MaterialTheme.extendedColors.success else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(128.dp)
        )
        Text(
            text = if (passed) "You passed!" else "You failed.",
            style = MaterialTheme.typography.headlineLarge,
            color = if (passed) MaterialTheme.extendedColors.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer
        )
        Text(
            text = "You got $score%"
        )
        if (passed) {
            Text(
                text = "Are you ready to take the actual test?"
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val buttonRadius = (ButtonDefaults.shape as RoundedCornerShape)
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://hamstudy.org/sessions/remote".toUri())
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = buttonRadius.copy(
                        bottomEnd = CornerSize(0.dp),
                        topEnd = CornerSize(0.dp)
                    )
                ) {
                    Text("Online tests")
                }
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://hamstudy.org/sessions/inperson".toUri())
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = buttonRadius.copy(
                        bottomStart = CornerSize(0.dp),
                        topStart = CornerSize(0.dp)
                    )
                ) {
                    Text("In person tests")
                }
            }
        } else {
            Text(
                text = "You might want to keep practicing."
            )
        }
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Review questions"
            )
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

@Preview(showBackground = true)
@Composable
fun TestPassedPreview() {
    HamTestTheme {
        TestFinished(
            95,
            {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TestFailedPreview() {
    HamTestTheme {
        TestFinished(
            68,
            {}
        )
    }
}