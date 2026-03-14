package dev.appsmithery.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class LensConfig(
    val publicApis: List<String> = emptyList(),
    val alwaysKeep: List<String> = emptyList(),
    val androidComponents: List<String> = emptyList(),

)

fun loadConfig(path: String):LensConfig{
    val json = File(path).readText(Charsets.UTF_8)
    return Json.decodeFromString(LensConfig.serializer(), json)
}