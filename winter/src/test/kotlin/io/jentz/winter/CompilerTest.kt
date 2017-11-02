package io.jentz.winter

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

class CompilerTest {

    class Blub @Inject constructor() {

        @field:[Inject]
        lateinit var bar: String

    }

    @Singleton
    class Foo @Inject constructor(val message: String) {
        @field:[Inject Named("Bar")]
        internal lateinit var bar: String

        @Inject internal lateinit var baz: Any
    }

    @Test
    fun foo() {
        val any = Any()
        val graph = component {
            include(generatedComponent)
            constant("Hello World!")
            constant("Hello Foo!", qualifier = "Bar")
            constant(any)
        }.init()
        val blub: Blub = graph.instance()
        val foo: Foo = graph.instance()
        assertEquals("Hello World!", blub.bar)
        assertEquals("Hello Foo!", foo.bar)
        assertSame(any, foo.baz)
        assertSame(foo, graph.instance<Foo>())
    }

}