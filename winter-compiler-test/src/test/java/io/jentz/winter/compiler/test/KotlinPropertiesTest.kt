package io.jentz.winter.compiler.test

import io.jentz.winter.component
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test

class KotlinPropertiesTest {

    @Test
    fun `should inject Kotlin properties`() {
        val graph = component {
            constant("foo")
            constant(21)
            constant(42, qualifier = "someInt")
            constant(listOf("a", "b", "c"), generics = true)
            generated<KotlinProperties>()
        }.createGraph()

        val instance: KotlinProperties = graph.instance()

        instance.constructorInjectedString.shouldBe("foo")
        instance.constructorInjectedPrimitive.shouldBe(42)

        instance.primitiveProperty.shouldBe(21)
        instance.primitiveSetter.shouldBe(21)

        instance.namedPrimitiveProperty.shouldBe(42)
        instance.namedPrimitiveSetter.shouldBe(42)

        instance.someList.shouldBe(listOf("a", "b", "c"))

        instance.stringProvider().shouldBe("foo")
    }

}