package dev.appsmithery.generator

import dev.appsmithery.models.LensConfig
import dev.appsmithery.models.MyClassReference
import dev.appsmithery.utils.Constants

class RuleGenerator {

    fun generateRules(traversedGraph : MutableMap<String, MyClassReference>, lensConfig: LensConfig) {
        val keepRules = arrayListOf("")
        traversedGraph.forEach { (_, value) ->
//            generateIndividualRule(value.className, value.getsCalledFrom)
        }
    }

    private fun generateIndividualRule(className: String, rules: Constants.KeepRules): String{
        return when (rules){
            Constants.KeepRules.public_api -> {
                "-keep $className{*;}"
            }
            Constants.KeepRules.keep_rule -> {
                "-keep $className{*;}"
            }
            Constants.KeepRules.android_component -> {
                "-keep $className{*;}"
            }
            Constants.KeepRules.default_android_component -> {
                "-keep $className{*;}"
            }
        }
    }

}