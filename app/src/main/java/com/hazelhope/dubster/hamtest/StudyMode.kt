package com.hazelhope.dubster.hamtest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hazelhope.dubster.hamtest.ui.theme.HamTestTheme

@Composable
fun Study(goToQuiz: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = modifier.fillMaxSize().padding(12.dp)
    ) {
        Button(
            onClick = {
                goToQuiz("removedtech2022")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Technician (questions removed in 2026)")
        }
        Button(
            onClick = {
                goToQuiz("technician")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Technician")
        }
        Button(
            onClick = {
                goToQuiz("general")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("General")
        }
        Button(
            onClick = {
                goToQuiz("extra")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Extra")
        }
    }
}

@Composable
fun Quiz(
    quizType: String,
    db: HamTestDatabase,
    modifier: Modifier = Modifier,
    viewModel: StudyModeViewModel = viewModel()
) {
    LaunchedEffect(db) {
        viewModel.setDatabase(db)
    }

    LaunchedEffect(quizType) {
        viewModel.loadQuiz(quizType)
    }

    val questionPoolData by viewModel.questionPoolData.collectAsStateWithLifecycle()

    val currentQuestion by viewModel.currentQuestion.collectAsStateWithLifecycle()

    val shouldBreakTheFlow by viewModel.shouldBreakTheFlow.collectAsStateWithLifecycle()

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        if (shouldBreakTheFlow != 0) {
            BreakTheFlow(shouldBreakTheFlow, {
                viewModel.nextQuestion(false)
            })
        } else {
            LinearProgressIndicator(
                progress = { questionPoolData.inUsePoolSize / (questionPoolData.totalPoolSize * 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            QuestionPoolDiagnostics(questionPoolData)
            QuestionPoolQuestion(currentQuestion, {
                viewModel.nextQuestion(it)
            })
        }
    }
}

@Composable
fun QuestionPoolDiagnostics(questionPoolData: QuestionPoolLiveData, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text(
            text = "Pool Size: ${questionPoolData.inUsePoolSize}/${questionPoolData.totalPoolSize}\n" +
                    "Weak Questions: ${questionPoolData.weakQuestions}\n" +
                    "Current Question Score: ${questionPoolData.currentQuestionScore}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun QuestionPoolQuestion(currentQuestion: HamQuestion, nextQuestion: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    var checked by remember { mutableStateOf(false) }

    var selectedAnswer by remember { mutableIntStateOf(0) }

    var revealedIsCheckedCorrect by remember { mutableStateOf(false) }
    var revealedCorrectLetter by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        if (currentQuestion.figure != "") {
            Image(
                painterResource(figurePathsToIds[currentQuestion.figure] ?: R.drawable.outline_cancel),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = currentQuestion.id,
                textAlign = TextAlign.Left,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val firstTime = currentQuestion.userQuestionInfo?.firstTime == true
            val weakQuestion = if (currentQuestion.userQuestionInfo != null) currentQuestion.userQuestionInfo.score < 0
            else false

            if (firstTime || weakQuestion) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (firstTime) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.secondaryContainer
                        )
                ) {
                    Text(
                        text = if (firstTime) "New"
                        else "Weak",
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                    )
                }
            }
        }
        Text(
            text = currentQuestion.question,
            style = MaterialTheme.typography.headlineSmall
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.selectableGroup()
        ) {
            val letters = listOf("A", "B" , "C", "D")
            currentQuestion.answers.forEachIndexed { index, answer ->
                QuestionPoolAnswer(
                    answer = "${letters[index]}. $answer",
                    selected = index == selectedAnswer,
                    enabled = !checked,
                    onSelect = {
                        selectedAnswer = index
                    }
                )
            }
        }

        AnimatedVisibility(checked) {
            QuestionPoolSuccessCard(revealedIsCheckedCorrect, revealedCorrectLetter)
        }

        Button({
            if (!checked) {
                revealedIsCheckedCorrect = selectedAnswer == currentQuestion.correct
                revealedCorrectLetter = currentQuestion.correct_letter
                checked = true
            } else {
                nextQuestion(!revealedIsCheckedCorrect)
                checked = false
                selectedAnswer = 0
            }
        },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        ) {
            Text(
                text = if (checked) "Next Question" else "Submit"
            )
        }
    }
}

@Composable
fun QuestionPoolAnswer(
    answer: String,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton,
                enabled = enabled
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = answer,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
fun QuestionPoolSuccessCard(isCorrect: Boolean, correctLetter: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Image(
                if (isCorrect) painterResource(R.drawable.outline_check_circle)
                else painterResource(R.drawable.outline_cancel),
                contentDescription = null
            )
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = if (isCorrect) "Correct!" else "Wrong.",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = if (isCorrect) "That was the right answer."
                    else "The right answer was $correctLetter"
                )
            }
        }
    }
}

@Composable
fun BreakTheFlow(minutes: Int, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        val icon = when {
            minutes >= 90 -> R.drawable.emoji_shocked
            minutes >= 75 -> R.drawable.emoji_astonished
            minutes >= 60 -> R.drawable.emoji_wow
            minutes >= 45 -> R.drawable.emoji_grin_with_teeth
            minutes >= 30 -> R.drawable.emoji_grin_with_big_eyes
            else -> R.drawable.emoji_grin
        }

        Image(
            painterResource(icon),
            contentDescription = "Emoji representing how long you've done this for",
            modifier = Modifier.fillMaxWidth(0.5f)
        )
        Text(
            text = "You've been studying for $minutes minutes!",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Do you want to continue?",
            textAlign = TextAlign.Center
        )
        Button(
            onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        ) {
            Text(
                text = "Yes, see the next question"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StudyPreview() {
    HamTestTheme {
        Study(
            {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun QuestionPoolQuestionPreview() {
    HamTestTheme {
        QuestionPoolQuestion(
            HamQuestion(
                "PREVIEW",
                2,
                "(wow)",
                "Does this look cool?",
                listOf("Yes", "Of course", "No, the FCC rules prohibit it from looking cool", "The ARRL bylaws prevent ham software from looking new"),
                "",
                "C",
                UserQuestionInfo(
                    "PREVIEW",
                    "preview",
                    -2,
                    false
                )
            ),
            nextQuestion = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun QuestionPoolDiagnosticsPreview() {
    HamTestTheme {
        QuestionPoolDiagnostics(
            QuestionPoolLiveData(
                400,
                35,
                2,
                0
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun QuestionPoolSuccessCardCorrectPreview() {
    HamTestTheme {
        QuestionPoolSuccessCard(
            true,
            "C"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun QuestionPoolSuccessCardWrongPreview() {
    HamTestTheme {
        QuestionPoolSuccessCard(
            false,
            "C"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BreakTheFlowPreview() {
    HamTestTheme {
        BreakTheFlow(90, {})
    }
}