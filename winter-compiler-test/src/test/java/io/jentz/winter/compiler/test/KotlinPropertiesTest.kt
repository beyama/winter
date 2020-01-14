package io.jentz.winter.compiler.test

import io.jentz.winter.component
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test

class KotlinPropertiesTest {

    @Test
    fun `should inject Kotlin properties`() {
        val instance = KotlinProperties()
        component {
            constant(21)
            constant(42, qualifier = "someInt")
            constant(listOf("a", "b", "c"), generics = true)
        }.createGraph().inject(instance)

        instance.primitiveProperty.shouldBe(21)
        instance.primitiveSetter.shouldBe(21)

        instance.namedPrimitiveProperty.shouldBe(42)
        instance.namedPrimitiveSetter.shouldBe(42)
    }

}