package dev.appsmithery.models

 class MyClassReference {
     var isReachable: Boolean = false
     var isPublic: Boolean = false
      var className: String = ""
      var extends: List<String>? = null
     var implements: List<String>? = null
     var methods: MutableMap<String,MyMethodReference>? = null
     var getsCalledFrom = arrayListOf<String>()

 }

