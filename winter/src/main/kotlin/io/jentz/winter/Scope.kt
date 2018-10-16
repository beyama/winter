package io.jentz.winter

data class Scope(val name: String) {
    companion object {
        val Prototype = Scope("prototype")
        val Singleton = Scope("singleton")
        val SoftSingleton = Scope("softSingleton")
        val WeakSingleton = Scope("weakSingleton")
        val PrototypeFactory = Scope("prototypeFactory")
        val MultitonFactory = Scope("multitonFactory")
    }
}
