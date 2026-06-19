package com.hazelhope.dubster.hamtest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hazelhope.dubster.hamtest.ui.theme.HamTestTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
fun SettingsPreview() {
    HamTestTheme {
        Settings()
    }
}