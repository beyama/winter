package io.jentz.winter

import org.junit.Assert
import org.junit.Test
import javax.inject.Inject
import javax.inject.Named

class Blub @Inject constructor() {

    @field:[Inject]
    lateinit var bar: String

}

class Foo @Inject constructor(val message: String) {
    @field:[Inject Named("Bar")]
    internal lateinit var bar: String

    @Inject internal lateinit var baz: Any
}

class CompilerTest {

    inline fun <reified T : Any> injectorForClass(instance: T): MembersInjector<T> {
        return Class.forName("${T::class.java.name}$\$MembersInjector").newInstance() as MembersInjector<T>
    }

    inline fun <reified T : Any> inject(graph: Graph, instance: T) {
        injectorForClass(instance).injectMembers(graph, instance)
    }

    @Test
    fun foo() {
        val graph = component { constant("Hello World!") }.init()
        val blub = Blub()
        inject(graph, blub)
        Assert.assertEquals("Hello World!", blub.bar)
    }

}