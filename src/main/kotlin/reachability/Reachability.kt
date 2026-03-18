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
    private val reachedClasses = mutableMapOf<String, List<String>>() // the classes, and the classes that accessed this class.
    private lateinit var lensConfig: LensConfig



    fun evaluateReachability(pathToAar : String, lensConfig: LensConfig): MutableMap<String, MyClassReference>{
        //store lensconfig in a global variable for usage
        this.lensConfig = lensConfig
        //ASM reachability graph testing

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

        val publicClasses = lensConfig.publicApis
        val alwaysKeep = lensConfig.alwaysKeep
        val androidComponents = lensConfig.androidComponents

        publicClasses.forEach { publicClass ->
            traverseThroughClass(withClassName = publicClass, isPublic = true, beingCalledFrom = null)
        }
        alwaysKeep.forEach { publicClass ->
            traverseThroughClass(withClassName = publicClass, isPublic = true, beingCalledFrom = null)
        }
        androidComponents.forEach { androidComponent ->
            traverseThroughClass(withClassName = androidComponent, isPublic = true, beingCalledFrom = null)
        }

        return allClasses
    }

    private fun traverseThroughClass(withClassName: String, isPublic: Boolean, beingCalledFrom: String?){
        allClasses[withClassName]?.let { myClassReference ->
            myClassReference.isPublic = isPublic
            if(beingCalledFrom != null){
                myClassReference.getsCalledFrom.add(beingCalledFrom)
            }
            //doing it this way without the else because it's possible that this was called from somewhere but also was equipped in it's own way
            when {
                lensConfig.publicApis.contains(withClassName) -> {
                    myClassReference.getsCalledFrom.add(Constants.KeepRules.public_api.name)
                }
                lensConfig.alwaysKeep.contains(withClassName) -> {
                    myClassReference.getsCalledFrom.add(Constants.KeepRules.keep_rule.name)
                }
                lensConfig.androidComponents.contains(withClassName)  -> {
                    myClassReference.getsCalledFrom.add(Constants.KeepRules.android_component.name)
                }
                myClassReference.extends?.any { extends -> extends in Constants.defaultAndroidComponents } == true -> {
                    myClassReference.getsCalledFrom.add(Constants.KeepRules.default_android_component.name)
                }
                myClassReference.implements?.any { implements -> implements in Constants.defaultAndroidComponents } == true -> {
                    myClassReference.getsCalledFrom.add(Constants.KeepRules.default_android_component.name)
                }
            }
            myClassReference.isReachable = true
            //all methods myClassReference calls, accesses, extends or implements
            val allReferencesList = mutableSetOf<String>()
            myClassReference.extends?.let { extends ->
                allReferencesList.addAll(extends)
            }
            myClassReference.implements?.let { implements ->
                allReferencesList.addAll(implements)
            }
            myClassReference.methods?.let { methods ->
                methods.forEach { method ->
                    method.value.isReachable = true
                    method.value.calls.forEach { call ->
                        allReferencesList.add(call.key)
                    }
                    method.value.accesses.forEach { access ->
                        allReferencesList.add(access.key)
                    }
                }
            }

            allReferencesList.forEach { nextClassName ->
                allClasses[nextClassName]?.let { myClassReference ->
                    if(!myClassReference.isReachable){
                        traverseThroughClass(nextClassName, isPublic = false, beingCalledFrom = withClassName)
                    }
                }

            }

        }


    }

//    private fun isReachableTraversal(className: String, isPublic: Boolean, usedIn: String) {
//        if(!reachedClasses.contains(className)) {
//            reachedClasses.add(className)
//        }
//        // a list to store all the classes that were
//        // extended, implemented, variable accessed, method triggered
//        // that were found
//        val allCallClassList = mutableSetOf<String>()
//
//        allClasses[className]?.let { myClassReference ->
//            myClassReference.isPublic = isPublic
//            myClassReference.extends?.let { extends ->
//                allCallClassList.addAll(extends)
//            }
//            myClassReference.implements?.let { implements ->
//                allCallClassList.addAll(implements)
//            }
//            myClassReference.methods?.forEach { method ->
//                method.value.calls.forEach { call ->
//
//                    allCallClassList.add(call.key)
//
//                }
//            }
//        }
//        allCallClassList.forEach { classNameToTraverse ->
//            if (!reachedClasses.contains(classNameToTraverse)){
//                isReachableTraversal(classNameToTraverse, false, className)
//            }
//
//        }
//
//
//    }

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
                superName?.let {
                    extendsList.add(it)
                }
                interfaces?.forEach {
                    implementsList.add(it)
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