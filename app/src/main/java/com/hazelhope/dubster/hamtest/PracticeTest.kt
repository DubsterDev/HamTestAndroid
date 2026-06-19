package com.hazelhope.dubster.hamtest

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.hazelhope.dubster.hamtest.ui.theme.HamTestTheme

@Composable
fun Practice(
    goToTest: (String) -> Unit,
    modifier: Modifier = Modifier
) {

}

@Preview(showBackground = true)
@Composable
fun PracticePreview() {
    HamTestTheme {
        Practice({})
    }
}