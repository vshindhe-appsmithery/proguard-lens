package dev.appsmithery.models

class MyMethodReference(){
    var isReachable: Boolean = false
    var methodName: String = ""
    var calls: MutableMap<String, String> = mutableMapOf()
    var accesses: MutableMap<String, String> = mutableMapOf()

    override fun toString(): String {
        val returnData = """
           Method : $methodName
           Calls : $calls
           Accesses : $accesses
           Reachable : $isReachable
        """

        return returnData;
    }
}


