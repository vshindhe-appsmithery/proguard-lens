package dev.appsmithery

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import dev.appsmithery.models.LensConfig
import dev.appsmithery.models.MyClassReference
import dev.appsmithery.models.MyMethodReference
import dev.appsmithery.models.loadConfig
import dev.appsmithery.reachability.Reachability
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.File
import java.util.zip.ZipFile


class Lens : CliktCommand() {
    override fun run() = Unit


}

class ReachabilityTraversal: CliktCommand(name = "reachability") {
    val lensConfigPath by option(
        "--config",
        help = "Path to lens.config.json"
    ).required()

    val aarPath by option(
        "--aar",
        help = "Path to the AAR file"
    ).required()

    override fun run() {


        val reachability = Reachability()
        val lensConfig = loadConfig(lensConfigPath)
        val traversedGraph = reachability.evaluateReachability(aarPath, lensConfig)

//        echo("Starting Lens : Reachability Graph with rules \n Public APIs: ${lensConfig.publicApis}\n Always Keep: ${lensConfig.alwaysKeep}")
        val reachable = traversedGraph.filter { it.value.isReachable }
        val unreachable = traversedGraph.filter { !it.value.isReachable }

        echo("\n🔍 LENS — Reachability Report")
        echo("─".repeat(50))
        echo("AAR:    $aarPath")
        echo("Config: $lensConfigPath")
        echo("─".repeat(50))

        echo("\n    ✅ Reachable (${reachable.size} classes)")

        reachable.forEach {

            echo("   Name:${it.value.className}                               :  Is Public:${it.value.isPublic}  |  Called from:${it.value.getsCalledFrom}  |")
            echo("methods: ${it.value.methods}")
            echo("extends: ${it.value.extends}")
            echo("implements: ${it.value.implements}")

        }

        echo("\n    ❌ Unreachable (${unreachable.size} classes)")
        unreachable.forEach { echo("   ${it.value.className}") }

        echo("─".repeat(50))
        echo("Total classes analyzed : ${traversedGraph.size}")
        echo("Reachable              : ${reachable.size}")
        echo("Unreachable            : ${unreachable.size}")
//        echo("─".repeat(50))

    }

}

fun main(args: Array<String>) = Lens()
    .subcommands(ReachabilityTraversal())
    .main(args)





/*
* //1. check if the class exists in allClasses
//    allClasses[className]?.let {
//        println("Class $className is reachable")
//        it.isReachable = true
//        it.isPublic = isPublic
//        it.methods?.let { methods ->
//            val callClassList = mutableMapOf<String, MutableList<String>>()
//            println("$className has methods")
//            methods.forEach { method ->
//                println("method $method calls the following classes")
//                method.value.calls.forEach { call ->
//                    val callClassName = call.key
//                    if(callClassList.containsKey(callClassName)) {
//                        if(callClassList[callClassName]!=null){
//                            callClassList[callClassName]?.add(call.value)
//                        }else{
//                            callClassList[callClassName] = mutableListOf(call.value)
//                        }
//
//                    }else{
//                        callClassList[callClassName] = mutableListOf(call.value)
//                    }
//                }
//            }
//            callClassList.forEach { calledClass ->
//                println("${calledClass.key} is reachable")
//            }
//        }
//    }
*
* */



fun lensAuditFunctions(){
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