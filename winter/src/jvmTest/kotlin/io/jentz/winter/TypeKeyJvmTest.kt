package io.jentz.winter

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TypeKeyJvmTest {

    class TestClass

    @Test
    fun `toString should use the binary class name`() {
        erased<TestClass>(QualifierTest).toString()
            .shouldBe("TypeKey(io.jentz.winter.TypeKeyJvmTest\$TestClass, qualifier = qualifier(test), isOptional = false)")
        erased<String?>().toString()
            .shouldBe("TypeKey(java.lang.String, qualifier = null, isOptional = true)")
    }

    @Test
    fun `erased TypeKeys of primitive and boxed classes should be equal`() {
        assertSameHashAndEquals(erased<Int>(), ClassTypeKey<Int>(Int::class.javaPrimitiveType!!.kotlin))
        assertSameHashAndEquals(erased<Int>(), ClassTypeKey<Int>(Int::class.javaObjectType.kotlin))
        assertSameHashAndEquals(erased<Int>(), generic<Int>())
    }

    @Test
    fun `erased TypeKeys of mutable and read only collections should be equal`() {
        assertSameHashAndEquals(erased<List<String>>(), erased<MutableList<String>>())
        assertSameHashAndEquals(generic<List<String>>(), generic<MutableList<String>>())
    }

    @Test
    fun `class qualifier should be the qualified class name`() {
        qualifier<TestClass>().value.shouldBe("io.jentz.winter.TypeKeyJvmTest.TestClass")
    }

    private fun assertSameHashAndEquals(left: Any, right: Any) {
        left.hashCode().shouldBe(right.hashCode())
        left.shouldBe(right)
    }

}
