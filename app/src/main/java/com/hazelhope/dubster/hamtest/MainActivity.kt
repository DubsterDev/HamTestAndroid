package com.hazelhope.dubster.hamtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.hazelhope.dubster.hamtest.ui.theme.HamTestTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navigateTo = when (intent.action) {
            "com.hazelhope.dubster.hamtest.OPEN_TECHNICIAN" -> "study/technician"
            "com.hazelhope.dubster.hamtest.OPEN_GENERAL" -> "study/general"
            "com.hazelhope.dubster.hamtest.OPEN_EXTRA" -> "study/extra"
            else -> null
        }

        val db = Room.databaseBuilder(
            application,
            HamTestDatabase::class.java,
            "ham-test-db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

        enableEdgeToEdge()
        setContent {
            HamTestTheme {
                App(db, navigateTo = navigateTo)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    db: HamTestDatabase,
    modifier: Modifier = Modifier,
    navigateTo: String? = null
) {
    val settingsDao = remember { db.settingsDao() }
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "study"

    val topLevelRoutes = listOf("study", "settings")

    LaunchedEffect(navigateTo) {
        if (navigateTo != null) {
            navController.navigate(navigateTo)
        }
    }
    Scaffold(
        topBar = {
            TopBar(topLevelRoutes, navController)
        },
        bottomBar = {
            if (topLevelRoutes.contains(currentRoute)) {
                BottomBar(
                    currentRoute,
                    { to ->
                        navController.navigate(to)
                    }
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        val newModifier = Modifier.padding(innerPadding)
        NavHost(
            navController = navController,
            startDestination = "study"
        ) {
            composable("study") {
                Study(goToQuiz = {
                    navController.navigate("study/$it")
                }, modifier = newModifier)
            }
            composable("study/{class}", arguments = listOf(navArgument("class") { type = NavType.StringType })) {
                val quizType = it.arguments?.getString("class") ?: "unknown"
                Quiz(quizType, db, modifier = newModifier)
            }
            composable("settings") {
                Settings(modifier = newModifier, settingsDao = settingsDao)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(topLevel: List<String>, navController: NavController, modifier: Modifier = Modifier) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "study"
    val hamClass = navBackStackEntry?.arguments?.getString("class") ?: "oops"

    val normalTitles = mapOf(
        "study" to "Study",
        "settings" to "Settings"
    )
    val classTitles = mapOf(
        "removedtech2022" to "Technician (removed in 2026)",
        "technician" to "Technician Quiz",
        "general" to "General Quiz",
        "extra" to "Extra Quiz"
    )

    val title = if (currentRoute.startsWith("study/"))
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
        },
        actions = {},
        modifier = modifier
    )
}

@Composable
fun BottomBar(currentRoute: String, navigate: (String) -> Unit, modifier: Modifier = Modifier) {
    NavigationBar(
        modifier = modifier
    ) {
        NavigationBarItem(
            selected = currentRoute == "study",
            onClick = {
                navigate("study")
            },
            icon = {
                Icon(
                    painterResource(R.drawable.outline_book_2),
                    contentDescription = "Study"
                )
            },
            label = { Text("Study") }
        )
        NavigationBarItem(
            selected = currentRoute == "practice",
            onClick = {
                navigate("practice")
            },
            icon = {
                Icon(
                    painterResource(R.drawable.outline_format_list_numbered),
                    contentDescription = "Practice"
                )
            },
            label = { Text("Practice") }
        )
        NavigationBarItem(
            selected = currentRoute == "settings",
            onClick = {
                navigate("settings")
            },
            icon = {
                Icon(
                    painterResource(R.drawable.outline_settings),
                    contentDescription = "Settings"
                )
            },
            label = { Text("Settings") }
        )
    }
}

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
fun Settings(
    modifier: Modifier = Modifier,
    settingsDao: SettingsDao? = null
) {
    var isAutoSelectCorrectAnswerEnabled by remember {
        mutableStateOf(
            false
        )
    }

    var isBreakTheFlowEnabled by remember {
        mutableStateOf(
            false
        )
    }

    LaunchedEffect(settingsDao) {
        CoroutineScope(Dispatchers.IO).launch {
            isAutoSelectCorrectAnswerEnabled = settingsDao?.getValue("autoSelectCorrectAnswer")?.getOrNull(0)?.value == "true"
            isBreakTheFlowEnabled = settingsDao?.getValue("breakTheFlow")?.getOrNull(0)?.value == "true"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = "Study Mode",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(6.dp)
        )
        SettingsCard(
            title = "Auto select correct answers",
            description = "Makes the correct answer A on new questions",
            isEnabled = isAutoSelectCorrectAnswerEnabled,
            position = "top",
            onToggle = {
                isAutoSelectCorrectAnswerEnabled = it
                CoroutineScope(Dispatchers.IO).launch {
                    settingsDao?.upsertSetting(SettingsItem("autoSelectCorrectAnswer", it.toString()))
                }
            }
        )
        SettingsCard(
            title = "Break the flow",
            description = "Reminders every fifteen minutes (just in case you should be doing something else)",
            position = "bottom",
            isEnabled = isBreakTheFlowEnabled,
            onToggle = {
                isBreakTheFlowEnabled = it
                CoroutineScope(Dispatchers.IO).launch {
                    settingsDao?.upsertSetting(SettingsItem("breakTheFlow", it.toString()))
                }
            }
        )
    }
}

@Composable
fun SettingsCard(
    title: String,
    description: String,
    position: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = when (position) {
        "top" -> RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 12.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        )
        "bottom" -> RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = 12.dp,
            bottomEnd = 12.dp
        )
        else -> RoundedCornerShape(12.dp)
    }

    Card(
        shape = shape,
        colors = CardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.background,
            disabledContentColor = MaterialTheme.colorScheme.onBackground,
        ),
        modifier = modifier
            .toggleable(
                value = isEnabled,
                onValueChange = onToggle,
                role = Role.Switch
            )
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(0.8f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = description
                )
            }
            Switch(
                isEnabled,
                null,
                modifier = Modifier.weight(0.2f)
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
fun SettingsPreview() {
    HamTestTheme {
        Settings()
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