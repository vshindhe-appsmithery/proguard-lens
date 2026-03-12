package dev.appsmithery.models

data class ProR8Rules(
    val keepRules: List<String>, //anything with keep. It can be any flavor of keep. keep, keepclassmembers, and all of that.
    val configRules: List<String>, // anything from dontobfuscate, dontwarn, assumevalues, assumenosideeffects all of that.
)
