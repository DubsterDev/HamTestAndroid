package com.hazelhope.dubster.hamtest

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hazelhope.dubster.hamtest.ui.theme.HamTestTheme
import com.hazelhope.dubster.hamtest.ui.theme.extendedColors

@Composable
fun PickAQuizCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardColors(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        ),
        modifier = modifier.fillMaxWidth().clickable(
            enabled = true,
            onClick = onClick
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = description
                )
            }
            Icon(
                painterResource(R.drawable.outline_chevron_right),
                contentDescription = null
            )
        }
    }
}

@Composable
fun SuccessCard(isCorrect: Boolean, title: String, description: String, modifier: Modifier = Modifier) {
    val container = if (isCorrect) MaterialTheme.extendedColors.successContainer
    else MaterialTheme.colorScheme.errorContainer
    val onContainer = if (isCorrect) MaterialTheme.extendedColors.onSuccessContainer
    else MaterialTheme.colorScheme.onErrorContainer
    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardColors(
            containerColor = container,
            contentColor = onContainer,
            disabledContainerColor = container,
            disabledContentColor = onContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Icon(
                if (isCorrect) painterResource(R.drawable.outline_check_circle)
                else painterResource(R.drawable.outline_cancel),
                contentDescription = null,
                tint = if (isCorrect) MaterialTheme.extendedColors.success
                else MaterialTheme.colorScheme.error
            )
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = description
                )
            }
        }
    }
}

@Composable
fun QuestionPoolSuccessCard(isCorrect: Boolean, correctLetter: String, modifier: Modifier = Modifier) {
    SuccessCard(
        isCorrect = isCorrect,
        title = if (isCorrect) "Correct!" else "Wrong.",
        description = if (isCorrect) "That was the right answer."
                        else "The right answer was $correctLetter",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun PickAQuizCardPreview() {
    HamTestTheme {
        PickAQuizCard(
            "Technician",
            "Study for the 2026-2030 question pool",
            {}
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