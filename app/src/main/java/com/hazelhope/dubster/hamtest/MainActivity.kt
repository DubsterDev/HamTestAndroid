package com.hazelhope.dubster.hamtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hazelhope.dubster.hamtest.ui.theme.HamTestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HamTestTheme {
                App()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Ham Test"
                    )
                },
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
    var checked by remember { mutableStateOf(false) }

    var selectedAnswer by remember { mutableIntStateOf(0) }

    var revealedIsCheckedCorrect by remember { mutableStateOf(false) }
    var revealedCorrectLetter by remember { mutableStateOf("") }

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize().padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "Pool Size: ${questionPoolData.inUsePoolSize}/${questionPoolData.totalPoolSize}; " +
                        "Weak Questions: ${questionPoolData.weakQuestions}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier.fillMaxSize()
        ) {
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
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (index == selectedAnswer),
                                onClick = { selectedAnswer = index },
                                role = Role.RadioButton,
                                enabled = !checked
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (index == selectedAnswer),
                            onClick = null
                        )
                        Text(
                            text = "${letters[index]}. $answer",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
            AnimatedVisibility(checked) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Image(
                            if (revealedIsCheckedCorrect) painterResource(R.drawable.outline_check_circle)
                            else painterResource(R.drawable.outline_cancel),
                            contentDescription = null
                        )
                        Column(
                            modifier = Modifier
                                .padding(start = 12.dp)
                        ) {
                            Text(
                                text = if (revealedIsCheckedCorrect) "Correct!" else "Wrong.",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = if (revealedIsCheckedCorrect) "That was the right answer."
                                else "The right answer was $revealedCorrectLetter"
                            )
                        }
                    }
                }
            }

            Button({
                if (!checked) {
                    revealedIsCheckedCorrect = selectedAnswer == currentQuestion.correct
                    revealedCorrectLetter = currentQuestion.correct_letter
                    checked = true
                } else {
                    viewModel.nextQuestion(!revealedIsCheckedCorrect)
                    checked = false
                    selectedAnswer = 0
                }
            },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (checked) "Next Question" else "Submit"
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

@Preview(showBackground = true, widthDp = 400, heightDp = 600)
@Composable
fun GeneralQuizPreview() {
    HamTestTheme {
        Quiz("general")
    }
}