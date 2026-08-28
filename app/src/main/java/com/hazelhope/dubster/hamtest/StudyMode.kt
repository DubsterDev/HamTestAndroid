package com.hazelhope.dubster.hamtest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hazelhope.dubster.hamtest.ui.theme.HamTestTheme
import com.hazelhope.dubster.hamtest.ui.theme.extendedColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun Study(goToQuiz: (String) -> Unit, modifier: Modifier = Modifier) {
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
            title = "Technician Questions",
            description = "Study for the 2026-2030 question pool",
            onClick = {
                goToQuiz("technician")
            }
        )
        PickAQuizCard(
            title = "General Questions",
            description = "Study for the 2023-2027 question pool",
            onClick = {
                goToQuiz("general")
            }
        )
        PickAQuizCard(
            title = "Extra Questions",
            description = "Study for the 2024-2028 question pool",
            onClick = {
                goToQuiz("extra")
            }
        )
    }
}

@Composable
fun Quiz(
    quizType: String,
    db: HamTestDatabase,
    modifier: Modifier = Modifier,
    viewModel: StudyModeViewModel = viewModel()
) {
    var shouldAutoSelectCorrectAnswer by remember { mutableStateOf(false) }

    LaunchedEffect(db) {
        CoroutineScope(Dispatchers.IO).launch {
            val settingsDao = db.settingsDao()
            shouldAutoSelectCorrectAnswer = settingsDao.getValue("autoSelectCorrectAnswer").getOrNull(0)?.value == "true"
        }
    }

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
            QuestionPoolQuestion(currentQuestion, shouldAutoSelectCorrectAnswer, {
                viewModel.nextQuestion(it)
            },
                modifier = Modifier.weight(1f))
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
fun QuestionPoolQuestion(currentQuestion: HamQuestion, autoSelectCorrectAnswer: Boolean, nextQuestion: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    var checked by remember { mutableStateOf(false) }

    var selectedAnswer by remember { mutableIntStateOf(-1) }

    LaunchedEffect(autoSelectCorrectAnswer) {
        selectedAnswer = if (autoSelectCorrectAnswer && currentQuestion.userQuestionInfo?.firstTime != false) 0 else -1
    }

    LaunchedEffect(currentQuestion) {
        selectedAnswer = if (autoSelectCorrectAnswer && currentQuestion.userQuestionInfo?.firstTime != false) 0 else -1
    }

    var revealedIsCheckedCorrect by remember { mutableStateOf(false) }
    var revealedCorrectLetter by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .verticalScroll(scrollState)
            .fillMaxSize()
    ) {
        QuestionPoolJustQuestion(
            currentQuestion = currentQuestion,
            selectedAnswer = selectedAnswer,
            onSelect = { selectedAnswer = it },
            enabled = !checked,
            showInfoButton = currentQuestion.explanation != null
        )

        AnimatedVisibility(checked) {
            QuestionPoolSuccessCard(revealedIsCheckedCorrect, revealedCorrectLetter)
        }

        Button({
            if (!checked) {
                revealedIsCheckedCorrect = selectedAnswer == currentQuestion.correct
                revealedCorrectLetter = currentQuestion.correctLetter
                checked = true
            } else {
                nextQuestion(!revealedIsCheckedCorrect)
                checked = false
            }
        },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            enabled = selectedAnswer != -1
        ) {
            Text(
                text = if (checked) "Next Question" else "Submit"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionPoolJustQuestion(
    currentQuestion: HamQuestion,
    selectedAnswer: Int,
    onSelect: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    hideIdentifier: Boolean = false,
    showInfoButton: Boolean = false
) {
    // Info dialog
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    var showBottomSheet by remember { mutableStateOf(false) }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
            },
            sheetState = sheetState
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Text(
                    text = "Explanation for ${currentQuestion.id}",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = currentQuestion.explanation ?: "There is no explanation for this question."
                )
                Button(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Got it")
                }
            }
        }
    }


    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        if (currentQuestion.figure != "") {
            Image(
                painterResource(
                    figurePathsToIds[currentQuestion.figure] ?: R.drawable.emoji_shocked
                ),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                modifier = Modifier.fillMaxHeight()
            ) {
                if (!hideIdentifier) {
                    Text(
                        text = currentQuestion.id,
                        textAlign = TextAlign.Left,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (showInfoButton) {
                    IconButton(
                        onClick = {
                            showBottomSheet = true
                        },
                        modifier = Modifier.size(24.dp)) {
                        Icon(
                            painterResource(R.drawable.outline_info),
                            contentDescription = "Explanation"
                        )
                    }
                }
            }

            val firstTime = currentQuestion.userQuestionInfo?.firstTime == true
            val weakQuestion =
                if (currentQuestion.userQuestionInfo != null) currentQuestion.userQuestionInfo.score < 0
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
            val letters = listOf("A", "B", "C", "D")
            currentQuestion.answers.forEachIndexed { index, answer ->
                QuestionPoolAnswer(
                    answer = "${letters[index]}. $answer",
                    selected = index == selectedAnswer,
                    enabled = enabled,
                    correct = !enabled && index == currentQuestion.correct,
                    onSelect = {
                        onSelect(index)
                    }
                )
            }
        }
    }
}

@Composable
fun QuestionPoolAnswer(
    answer: String,
    selected: Boolean,
    enabled: Boolean,
    correct: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val green = MaterialTheme.extendedColors.success
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
            onClick = null,
            colors = if (correct) RadioButtonColors(
                selectedColor = green,
                unselectedColor = green,
                disabledSelectedColor = green,
                disabledUnselectedColor = green
            ) else RadioButtonDefaults.colors()
        )
        Text(
            text = answer,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp),
            color = if (correct) green else Color.Unspecified
        )
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
                explanation = "FCC rules explicitly state the software used by hams must never look cool. If a ham uses such software, their license may be immediately and permanently revoked or suspended.",
                userQuestionInfo = UserQuestionInfo(
                    id = "PREVIEW",
                    pool = "preview",
                    score = -2,
                    lastSeenAt = 0,
                    firstTime = false
                )
            ),
            false,
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
fun BreakTheFlowPreview() {
    HamTestTheme {
        BreakTheFlow(90, {})
    }
}