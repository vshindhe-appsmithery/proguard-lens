package dev.appsmithery.reachability

import dev.appsmithery.models.LensConfig
import dev.appsmithery.models.MyClassReference
import dev.appsmithery.models.MyMethodReference
import dev.appsmithery.utils.Constants
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.File
import java.util.zip.ZipFile
import kotlin.collections.set
import kotlin.sequences.forEach

class Reachability {


    private val allClasses = mutableMapOf<String, MyClassReference>()
    private val reachedClasses = mutableListOf<String>()

    fun evaluateReachability(pathToAar : String, lensConfig: LensConfig): MutableMap<String, MyClassReference>{
        //asm reachability graph testing
        val aarFile = ZipFile(File(pathToAar))
        val classesJarEntry = aarFile.getEntry("classes.jar")
        val classesJarBytes = aarFile.getInputStream(classesJarEntry).readBytes()

        val tempJar = File.createTempFile("classes", ".jar")
        tempJar.writeBytes(classesJarBytes)

        val classesJar = ZipFile(tempJar)
        classesJar.entries().asSequence()
            .filter { it.name.endsWith(".class") }
            .forEach { classFile ->
                val classBytes = classesJar.getInputStream(classFile).readBytes()
                val classRef = analyzeClass(classBytes)
                allClasses[classRef.className] = classRef
            }

        classesJar.close()
        println("====================")
        allClasses.forEach { singleClass ->
            println("class name : ${singleClass.key}")
            println("extends class : ${singleClass.value.extends}")
            println("implements class : ${singleClass.value.implements}")
            singleClass.value.methods?.forEach { data ->
//            println("method: ${data.methodName}")


                println("method : ${data.key}")
                data.value.calls.forEach {call ->
                    println("calls -> $call")
                }
                data.value.accesses.forEach {access ->
                    println("accesses -> $access")
                }
            }
        }

        println("====================")
        allClasses.forEach { singleClass ->
            println("class name : ${singleClass.key}")
            println("extends class : ${singleClass.value.extends}")
            println("implements class : ${singleClass.value.implements}")
            singleClass.value.methods?.forEach { data ->
//            println("method: ${data.methodName}")


                println("method : ${data.key}")
                data.value.calls.forEach {call ->
                    println("calls -> $call")
                }
                data.value.accesses.forEach {access ->
                    println("accesses -> $access")
                }
            }
        }

        println("====================")
        println("==================== Traversing now ====================")
        val publicClasses = lensConfig.publicApis
        val alwaysKeep = lensConfig.alwaysKeep
        val androidComponents = lensConfig.androidComponents
        reachedClasses.addAll(alwaysKeep)
        reachedClasses.addAll(androidComponents)

        publicClasses.forEach { className ->
            isReachableTraversal(className, true)
        }
        allClasses.filter { it.key in reachedClasses ||
                it.value.extends?.any { extends -> extends in Constants.defaultAndroidComponents } == true ||
                it.value.implements?.any { implementations -> implementations in Constants.defaultAndroidComponents } == true }
            .forEach { foundClasses ->
            foundClasses.value.isReachable = true
        }
//        allClasses.values.filter { classRef ->
//            classRef.extends?.any { it in Constants.defaultAndroidComponents } == true
//        }

        return allClasses

//        val unreachable = allClasses.keys.filter { it !in reachedClasses }
//        println("\n==== UNREACHABLE CLASSES ====")
//        unreachable.forEach { println("❌ $it") }
    }

    private fun isReachableTraversal(className: String, isPublic: Boolean) {
        println("**********************")
        println("Traversing $className.")
        println("isPublic $isPublic")
        if(!reachedClasses.contains(className)) {
            reachedClasses.add(className)
        }
        val allCallClassList = mutableSetOf<String>()
        allClasses[className]?.let { myClassReference ->
            myClassReference.extends?.let { extends ->
                allCallClassList.addAll(extends)
            }
            myClassReference.implements?.let { implements ->
                allCallClassList.addAll(implements)
            }
            myClassReference.methods?.forEach { method ->
                method.value.calls.forEach { call ->

                    allCallClassList.add(call.key)

                }
            }
        }
        println("**********************")

        allCallClassList.forEach { className ->
            if (!reachedClasses.contains(className)){
                println("Now traversing reached class $className")
                isReachableTraversal(className, false)
            }

        }


    }

    private fun analyzeClass(classBytes: ByteArray): MyClassReference {
        val myClassReference = MyClassReference()
        val extendsList = mutableListOf<String>()
        val implementsList = mutableListOf<String>()
        val methodList = mutableMapOf<String, MyMethodReference>()
        val reader = ClassReader(classBytes)
        reader.accept(object : ClassVisitor(Opcodes.ASM9) {

            private var currentClass = ""

            override fun visit(
                version: Int,
                access: Int,
                name: String,
                signature: String?,
                superName: String?,
                interfaces: Array<String>?
            ) {
                currentClass = name.replace("/", ".")
                myClassReference.className = currentClass
                println("\nClass: $currentClass")
                superName?.let {

                    extendsList.add(it)
                    println("  extends: ${it.replace("/", ".")}")
                }
                interfaces?.forEach {
                    implementsList.add(it)
                    println("  implements: ${it.replace("/", ".")}")
                }
            }

            override fun visitMethod(
                access: Int,
                name: String,
                descriptor: String,
                signature: String?,
                exceptions: Array<String>?
            ): MethodVisitor {
                val methodReference = MyMethodReference()
                methodList["$name$descriptor"] = methodReference
                methodReference.methodName = "$name$descriptor"
                println("  Method: $name$descriptor")
                return object : MethodVisitor(Opcodes.ASM9) {
                    override fun visitMethodInsn(
                        opcode: Int,
                        owner: String,
                        name: String,
                        descriptor: String,
                        isInterface: Boolean
                    ) {
                        val ownerClass = owner.replace("/", ".")
                        if (ownerClass.startsWith("com.razorpay")) {
                            methodReference.calls[ownerClass] = "$ownerClass.$name"
                            println("    → calls $ownerClass.$name")
                        }
                    }

                    override fun visitFieldInsn(
                        opcode: Int,
                        owner: String,
                        name: String,
                        descriptor: String
                    ) {
                        val ownerClass = owner.replace("/", ".")
                        methodReference.accesses[ownerClass] = "$ownerClass.$name"
                        println("    → accesses field ${owner.replace("/", ".")}.$name")
                    }
                }
            }
        }, 0)
        myClassReference.extends = extendsList
        myClassReference.implements = implementsList
        myClassReference.methods = methodList
        return myClassReference
    }

}