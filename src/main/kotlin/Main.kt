package dev.appsmithery

import dev.appsmithery.models.LensConfig
import java.io.File




fun main() {

    val file = File("/Users/vivek.shindhe/Projects/appsmithery/lenssampleapp/sample-library/build/outputs/mapping/release/mapping.txt")
    val packageName = "com.appsmithery.sample_library"
//    val checkForClassObfuscation = $$"com.appsmithery.sample_library.PublicApiClass$PaymentResult"
    val parser = Parser()
    val classMappings = parser.parse(file, packageName)
    val config = LensConfig(
        publicApis = listOf(
            "com.appsmithery.sample_library.PublicApiClass",
            "com.appsmithery.sample_library.annotations.PublicApi"
        )
    )
    val suspiciousClasses = classMappings.filter { classMapping ->
        !classMapping.classMapping.obfuscated &&
                config.publicApis.none { it == classMapping.classMapping.name }
    }
    suspiciousClasses.forEach {
        println("Suspicious unobfuscated class found")
        println("Class Name : ${it.classMapping.name}")
        it.functionMapping.forEach {
            println("Functions ${it.name}")
        }
    }

    val proguardRuleFile = File("/Users/vivek.shindhe/Projects/appsmithery/lenssampleapp/sample-library/proguard-rules.pro")
    val proR8Rules = parser.parseProguardFile(proguardRuleFile)
    println("Proguard keep Rules found: ${proR8Rules.keepRules}")
    println("Proguard config Rules found: ${proR8Rules.configRules}")
}