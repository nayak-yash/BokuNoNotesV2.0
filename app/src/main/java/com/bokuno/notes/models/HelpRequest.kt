package com.bokuno.notes.models

data class HelpRequest(
    val max_tokens: Int = 250,
    val model: String = "text-davinci-003",
    val prompt: String,
    val temperature: Double = 0.7
)