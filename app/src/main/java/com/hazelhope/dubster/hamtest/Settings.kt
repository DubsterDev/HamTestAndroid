package com.hazelhope.dubster.hamtest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.hazelhope.dubster.hamtest.ui.theme.HamTestTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

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

    var isDailyNotificationsEnabled by remember {
        mutableStateOf(
            false
        )
    }

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
            isDailyNotificationsEnabled = false
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

    LaunchedEffect(settingsDao) {
        CoroutineScope(Dispatchers.IO).launch {
            isAutoSelectCorrectAnswerEnabled = settingsDao?.getValue("autoSelectCorrectAnswer")?.getOrNull(0)?.value == "true"
            isBreakTheFlowEnabled = settingsDao?.getValue("breakTheFlow")?.getOrNull(0)?.value == "true"
            isDailyNotificationsEnabled = settingsDao?.getValue("dailyNotifications")?.getOrNull(0)?.value == "true"
        }
    }

    val scrollState = rememberScrollState()

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
            isEnabled = isAutoSelectCorrectAnswerEnabled,
            position = "top",
            onToggle = {
                isAutoSelectCorrectAnswerEnabled = it
                CoroutineScope(Dispatchers.IO).launch {
                    settingsDao?.upsertSetting(SettingsItem("autoSelectCorrectAnswer", it.toString()))
                }
            }
        )
        BooleanSettingsCard(
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
        SettingsSeparator("Reminders")
        BooleanSettingsCard(
            title = "Daily notifications",
            description = "Sends a notification every day reminding you to study, unless you've already studied",
            position = "only_card",
            isEnabled = isDailyNotificationsEnabled,
            onToggle = {
                isDailyNotificationsEnabled = it
                CoroutineScope(Dispatchers.IO).launch {
                    settingsDao?.upsertSetting(SettingsItem("dailyNotifications", it.toString()))
                }
                if (isDailyNotificationsEnabled && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                if (!isDailyNotificationsEnabled) {
                    WorkManager
                        .getInstance(context)
                        .cancelAllWorkByTag("notifications")
                }
            }
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
        modifier = modifier.padding(6.dp)
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
        Settings()
    }
}