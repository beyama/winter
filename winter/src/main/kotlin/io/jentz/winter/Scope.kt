package io.jentz.winter

@JvmInline
value class Scope(val name: String) {
    init {
        require(name.isNotBlank()) { "Scope name must not be blank." }
    }

    companion object {
        val Prototype = Scope("prototype")
        val Singleton = Scope("singleton")
    }
}