package dev.appsmithery

import dev.appsmithery.models.ClassMapping
import dev.appsmithery.models.Mapping
import dev.appsmithery.models.ProR8Rules
import dev.appsmithery.models.R8Metadata
import java.io.File

class Parser {

    fun parse(file: File, packageName: String): List<ClassMapping> {
        val classMappings = mutableListOf<ClassMapping>()
        val metadata = mutableListOf<String>()
        val currentMembers = mutableListOf<Mapping>()
        var currentClass: Mapping? = null

        file.readLines().forEach { line ->
            when {
                line.startsWith("#") -> {
                    metadata.add(line)
                }
                line.startsWith(packageName) && line.contains("->") -> {
                    // commit previous class before starting new one
                    currentClass?.let {
                        classMappings.add(ClassMapping(it, currentMembers.toMutableList()))
                        currentMembers.clear()
                    }
                    val parts = line.split("->")
                    val original = parts[0].trim()
                    val obfuscated = parts[1].trimEnd(':').trim()
                    currentClass = Mapping(original != obfuscated, original, obfuscated)
                }
                line.startsWith(" ") && line.contains("->") -> {
                    val parts = line.trim().split("->")
                    val original = parts[0].trim()
                    val obfuscated = parts[1].trim()
                    currentMembers.add(Mapping(original != obfuscated, original, obfuscated))
                }
            }
        }

        // commit the last class
        currentClass?.let {
            classMappings.add(ClassMapping(it, currentMembers.toMutableList()))
        }

        return classMappings
    }

    fun parseProguardFile(file: File): ProR8Rules {
        val keepRules = mutableListOf<String>()
        val configRules = mutableListOf<String>()
        val lines = file.readLines()
        lines.forEach { line ->
            when {
                line.startsWith("#") -> {//no-op
                    // no-op
                    }
                line.startsWith("-keep") -> {
                    keepRules.add(line)
                }
                else -> {
                    if (!line.trim().isEmpty()) {
                        configRules.add(line)
                    }

                }
            }
        }
        val proR8Rules = ProR8Rules(keepRules, configRules)
        return proR8Rules
    }

}

