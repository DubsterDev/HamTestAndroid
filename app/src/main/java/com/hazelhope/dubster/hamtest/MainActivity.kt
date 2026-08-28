package com.hazelhope.dubster.hamtest

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.hazelhope.dubster.hamtest.ui.theme.HamTestTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private var exportJson: String = ""
    private val createFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(exportJson.toByteArray())
                }
            }
        }
    }

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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            val isDailyNotificationsEnabled = db.settingsDao().getValue("dailyNotifications").getOrNull(0)?.value == "true"
            if (isDailyNotificationsEnabled) {
                WorkManager
                    .getInstance(applicationContext)
                    .cancelAllWorkByTag("notifications")

                val notificationWorkRequest: WorkRequest =
                    PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
                        .addTag("notifications")
                        // Run 23 hours and 45 minutes from now
                        .setInitialDelay(23 * 60 + 45, TimeUnit.MINUTES)
                        .build()

                WorkManager
                    .getInstance(applicationContext)
                    .enqueue(notificationWorkRequest)
            }
        }

        enableEdgeToEdge()
        setContent {
            HamTestTheme {
                App(db, { exportData(db) }, navigateTo = navigateTo)
            }
        }
    }

    fun exportData(
        db: HamTestDatabase
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val userQuestionDao = db.userQuestionDao()
            val settingsDao = db.settingsDao()
            val databaseVersion = db.openHelper.readableDatabase.version
            val technicianData = userQuestionDao.getAll("technician").associate {
                it.id to ExportQuestionPoolData(
                    score = it.score,
                    lastSeenAt = it.lastSeenAt,
                    firstTime = it.firstTime
                )
            }
            val generalData = userQuestionDao.getAll("general").associate {
                it.id to ExportQuestionPoolData(
                    score = it.score,
                    lastSeenAt = it.lastSeenAt,
                    firstTime = it.firstTime
                )
            }
            val extraData = userQuestionDao.getAll("extra").associate {
                it.id to ExportQuestionPoolData(
                    score = it.score,
                    lastSeenAt = it.lastSeenAt,
                    firstTime = it.firstTime
                )
            }
            val settings = settingsDao.getAll().associate {
                // All settings are booleans right now
                it.id to JsonPrimitive(it.value.toBoolean())
            }

            val exportData = ExportData(
                dbVersion = databaseVersion,
                settings = settings,
                technician = technicianData,
                general = generalData,
                extra = extraData
            )

            exportJson = Json.encodeToString(exportData)

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_TITLE, "progress.hamtest")
            }

            createFileLauncher.launch(intent)
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    db: HamTestDatabase,
    exportData: () -> Unit,
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
                PracticeTest(quizType, db, modifier = newModifier)
            }
            composable("settings") {
                Settings(
                    modifier = newModifier,
                    settingsDao = settingsDao,
                    exportData = exportData
                )
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