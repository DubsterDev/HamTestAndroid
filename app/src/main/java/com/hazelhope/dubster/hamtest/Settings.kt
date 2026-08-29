package com.hazelhope.dubster.hamtest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.hazelhope.dubster.hamtest.ui.theme.HamTestTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(
    exportData: () -> Unit,
    importData: () -> Unit,
    modifier: Modifier = Modifier,
    settingsDao: SettingsDao? = null
) {
    val isAutoSelectCorrectAnswerEnabledFlow by (
            settingsDao?.getValueAsFlow("autoSelectCorrectAnswer")
                ?: flowOf(emptyList())
            ).collectAsState(initial = emptyList())

    val isBreakTheFlowEnabledFlow by (
            settingsDao?.getValueAsFlow("breakTheFlow")
                ?: flowOf(emptyList())
            ).collectAsState(initial = emptyList())

    val isDailyNotificationsEnabledFlow by (
            settingsDao?.getValueAsFlow("dailyNotifications")
                ?: flowOf(emptyList())
            ).collectAsState(initial = emptyList())

    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (!granted) {
            Toast.makeText(context, "Disabling daily notifications since permission was not granted", Toast.LENGTH_LONG).show()
            CoroutineScope(Dispatchers.IO).launch {
                settingsDao?.upsertSetting(SettingsItem("dailyNotifications", false.toString()))
            }
        } else {
            WorkManager
                .getInstance(context)
                .cancelAllWorkByTag("notifications")

            val notificationWorkRequest: WorkRequest =
                PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
                    .addTag("notifications")
                    // Run 23 hours and 45 minutes from now
                    .setInitialDelay(23 * 60 + 45, TimeUnit.MINUTES)
                    .build()

            WorkManager
                .getInstance(context)
                .enqueue(notificationWorkRequest)
        }
    }

    val scrollState = rememberScrollState()

    var isImportConfirmationDialogOpen by remember { mutableStateOf(false) }

    if (isImportConfirmationDialogOpen) {
        AlertDialog(
            icon = {
                Icon(
                    painterResource(R.drawable.outline_upload),
                    contentDescription = null)
            },
            title = {
                Text(
                    text = "Are you sure?"
                )
            },
            text = {
                Text(
                    text = "Importing progress from a file will overwrite all of your progress on this device with the progress included in the file. Settings will also be modified to match the settings in the file."
                )
            },
            onDismissRequest = { isImportConfirmationDialogOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        isImportConfirmationDialogOpen = false
                        importData()
                    }
                ) {
                    Text("Proceed")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isImportConfirmationDialogOpen = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        SettingsSeparator("Study Mode")
        BooleanSettingsCard(
            title = "Auto select correct answers",
            description = "Makes the correct answer A on new questions",
            isEnabled = isAutoSelectCorrectAnswerEnabledFlow.firstOrNull()?.value == "true",
            position = "top",
            onToggle = {
                CoroutineScope(Dispatchers.IO).launch {
                    settingsDao?.upsertSetting(SettingsItem("autoSelectCorrectAnswer", it.toString()))
                }
            }
        )
        BooleanSettingsCard(
            title = "Break the flow",
            description = "Reminders every fifteen minutes (just in case you should be doing something else)",
            position = "bottom",
            isEnabled = isBreakTheFlowEnabledFlow.firstOrNull()?.value == "true",
            onToggle = {
                CoroutineScope(Dispatchers.IO).launch {
                    settingsDao?.upsertSetting(SettingsItem("breakTheFlow", it.toString()))
                }
            }
        )
        SettingsSeparator("Reminders")
        BooleanSettingsCard(
            title = "Daily notifications",
            description = "Sends a notification every day reminding you to study, unless you've already studied",
            position = "only_card",
            isEnabled = isDailyNotificationsEnabledFlow.firstOrNull()?.value == "true",
            onToggle = {
                CoroutineScope(Dispatchers.IO).launch {
                    settingsDao?.upsertSetting(SettingsItem("dailyNotifications", it.toString()))
                }
                if (it && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                if (!it) {
                    WorkManager
                        .getInstance(context)
                        .cancelAllWorkByTag("notifications")
                }
            }
        )
        SettingsSeparator("Data management")
        IconSettingsCard(
            title = "Export progress",
            description = "Export your progress to a file",
            position = "top",
            icon = R.drawable.outline_download,
            onClick = {
                exportData()
            }
        )
        IconSettingsCard(
            title = "Import progress",
            description = "Import your progress from a file",
            position = "bottom",
            icon = R.drawable.outline_upload,
            onClick = {
                isImportConfirmationDialogOpen = true
            }
        )
        SettingsSeparator("About Ham Test")
        IconSettingsCard(
            title = "Open source",
            description = "Ham Test is open source on GitHub under the GPLv3 license",
            position = "top",
            icon = R.drawable.outline_open_in_new,
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, "https://github.com/DubsterDev/HamTestAndroid".toUri())
                context.startActivity(intent)
            }
        )
        SettingsCard(
            title = "Version",
            description = BuildConfig.VERSION_NAME,
            position = "bottom",
            content = {}
        )
    }
}

@Composable
fun SettingsSeparator(
    label: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier.padding(6.dp, 18.dp, 6.dp, 6.dp)
    )
}

@Composable
fun IconSettingsCard(
    title: String,
    description: String,
    position: String,
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(
        title,
        description,
        position,
        content = {
            Icon(
                painterResource(icon),
                null,
                modifier = Modifier.size(48.dp)
            )
        },
        modifier = modifier.clickable {
            onClick()
        }
    )
}

@Composable
fun BooleanSettingsCard(
    title: String,
    description: String,
    position: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(
        title,
        description,
        position,
        content = {
            Switch(
                isEnabled,
                null,
                modifier = it
            )
        },
        modifier = modifier
            .toggleable(
                value = isEnabled,
                onValueChange = onToggle,
                role = Role.Switch
            )
    )
}

@Composable
fun SettingsCard(
    title: String,
    description: String,
    position: String,
    content: @Composable (Modifier) -> Unit,
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
        "only_card" -> RoundedCornerShape(12.dp)
        else -> RoundedCornerShape(0.dp)
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
            content(modifier.weight(0.2f))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    HamTestTheme {
        Settings({}, {})
    }
}