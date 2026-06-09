package com.hazelhope.dubster.hamtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
    var view by remember { mutableStateOf("home") }
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
        if (view == "home") {
            Home(goToQuiz = {
                view = it
            }, modifier = newModifier)
        } else {
            Quiz(view, modifier = newModifier)
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
fun Quiz(quizType: String, modifier: Modifier = Modifier) {
    Text(
        text = quizType,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    HamTestTheme {
        App()
    }
}