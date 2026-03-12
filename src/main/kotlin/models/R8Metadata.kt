package dev.appsmithery.models

data class R8Metadata(
    val compiler: String,
    val compilerVersion: String,
    val options: String,
    val pgMapId: String,
    val pgMapHash: String
)
