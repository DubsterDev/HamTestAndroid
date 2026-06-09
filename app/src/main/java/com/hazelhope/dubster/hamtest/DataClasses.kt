package com.hazelhope.dubster.hamtest

import kotlinx.serialization.Serializable

@Serializable
data class HamQuestion(
    val id: String,
    val correct: Int,
    val refs: String,
    val question: String,
    val answers: List<String>,
    val figure: String,
    val correct_letter: String,
)