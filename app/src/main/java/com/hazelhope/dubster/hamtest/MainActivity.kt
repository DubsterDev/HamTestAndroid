package com.hazelhope.dubster.hamtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hazelhope.dubster.hamtest.ui.theme.HamTestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navigateTo = when (intent.action) {
            "com.hazelhope.dubster.hamtest.OPEN_TECHNICIAN" -> "quiz/technician"
            "com.hazelhope.dubster.hamtest.OPEN_GENERAL" -> "quiz/general"
            "com.hazelhope.dubster.hamtest.OPEN_EXTRA" -> "quiz/extra"
            else -> null
        }

        enableEdgeToEdge()
        setContent {
            HamTestTheme {
                App(navigateTo = navigateTo)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    modifier: Modifier = Modifier,
    navigateTo: String? = null
) {
    val navController = rememberNavController()

    LaunchedEffect(navigateTo) {
        if (navigateTo != null) {
            navController.navigate(navigateTo)
        }
    }
    Scaffold(
        topBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: "home"

            val hamClass = navBackStackEntry?.arguments?.getString("class") ?: "oops"

            val topLevel = listOf("home")

            val normalTitles = mapOf(
                "home" to "Ham Test"
            )
            val classTitles = mapOf(
                "technician" to "Technician Quiz",
                "general" to "General Quiz",
                "extra" to "Extra Quiz"
            )

            val title = if (currentRoute.startsWith("quiz/"))
                classTitles[hamClass] ?: hamClass
            else normalTitles[currentRoute] ?: currentRoute

            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title
                    )
                },
                navigationIcon = {
                    if (
                        navController.previousBackStackEntry != null
                        && !topLevel.contains(currentRoute)
                        ) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                painterResource(R.drawable.outline_arrow_back),
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        val newModifier = Modifier.padding(innerPadding)
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") {
                Home(goToQuiz = {
                    navController.navigate("quiz/$it")
                }, modifier = newModifier)
            }
            composable("quiz/{class}", arguments = listOf(navArgument("class") { type = NavType.StringType })) {
                val quizType = it.arguments?.getString("class") ?: "unknown"
                Quiz(quizType, modifier = newModifier)
            }
        }
    }
}

@Composable
fun Home(goToQuiz: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            text = "Pick a test to begin"
        )
        Button(
            onClick = {
                goToQuiz("technician")
            },
            modifier = Modifier.fillMaxWidth(.75f)
        ) {
            Text("Technician")
        }
        Button(
            onClick = {
                goToQuiz("general")
            },
            modifier = Modifier.fillMaxWidth(.75f)
        ) {
            Text("General")
        }
        Button(
            onClick = {
                goToQuiz("extra")
            },
            modifier = Modifier.fillMaxWidth(.75f)
        ) {
            Text("Extra")
        }
    }
}

@Composable
fun Quiz(
    quizType: String,
    modifier: Modifier = Modifier,
    viewModel: HamTestViewModel = viewModel()
) {
    LaunchedEffect(quizType) {
        viewModel.loadQuiz(quizType)
    }

    val questionPoolData by viewModel.questionPoolData.collectAsStateWithLifecycle()

    val currentQuestion by viewModel.currentQuestion.collectAsStateWithLifecycle()

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        QuestionPoolDiagnostics(questionPoolData)
        QuestionPoolQuestion(currentQuestion, {
            viewModel.nextQuestion(it)
        })
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
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
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

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    HamTestTheme {
        App()
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

val figurePathsToIds = mapOf(
    "e5-1.png" to R.drawable.e5_1,
    "e6-1.png" to R.drawable.e6_1,
    "e6-2.png" to R.drawable.e6_2,
    "e6-3.png" to R.drawable.e6_3,
    "e7-1.png" to R.drawable.e7_1,
    "e7-2.png" to R.drawable.e7_2,
    "e7-3.png" to R.drawable.e7_3,
    "e9-1.png" to R.drawable.e9_1,
    "e9-2.png" to R.drawable.e9_2,
    "e9-3.png" to R.drawable.e9_3,
    "g7-1.png" to R.drawable.g7_1,
    "t-1.png" to R.drawable.t_1,
    "t-2.png" to R.drawable.t_2,
    "t-3.png" to R.drawable.t_3,
)