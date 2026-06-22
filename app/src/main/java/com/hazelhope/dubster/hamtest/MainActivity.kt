package com.hazelhope.dubster.hamtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.hazelhope.dubster.hamtest.ui.theme.HamTestTheme

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

    val topLevelRoutes = listOf("study", "practice", "settings")

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
            composable("practice") {
                Practice(
                    goToTest = {
                        navController.navigate("practice/$it")
                    },
                    modifier = newModifier
                )
            }
            composable("practice/{class}", arguments = listOf(navArgument("class") { type = NavType.StringType })) {
                val quizType = it.arguments?.getString("class") ?: "unknown"
                PracticeTest(quizType, modifier = newModifier)
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
        "practice" to "Practice",
        "settings" to "Settings"
    )
    val studyTitles = mapOf(
        "removedtech2022" to "Technician Quiz (removed in 2026)",
        "technician" to "Technician Quiz",
        "general" to "General Quiz",
        "extra" to "Extra Quiz"
    )

    val testTitles = mapOf(
        "technician" to "Technician Test",
        "general" to "General Test",
        "extra" to "Extra Test"
    )

    val title = when {
        currentRoute.startsWith("study/") -> studyTitles[hamClass] ?: hamClass
        currentRoute.startsWith("practice/") -> testTitles[hamClass] ?: hamClass
        else -> normalTitles[currentRoute] ?: currentRoute
    }

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