package io.jentz.winter.junit4

import io.jentz.winter.WinterException
import io.jentz.winter.typeKey
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.Test

class PropertyServiceTest {

    private class PublicNonFinalField {
        lateinit var field: String
    }

    private class PrivateFinalField {
        private val field = 42
    }

    private class NullProperty {
        var property: String? = null
    }

    @Test
    fun `#instance should get value from field on every invocation`() {
        val instance = PublicNonFinalField()
        val service = PropertyService(typeKey<String>(), instance, PublicNonFinalField::class.getDeclaredMemberProperty("field"))

        (1 until 5).map(Int::toString).forEach { int ->
            instance.field = int
            service.instance(Unit).shouldBe(int)
        }
    }

    @Test
    fun `#instance should get value from private final field`() {
        val instance = PrivateFinalField()
        val service = PropertyService(typeKey<Int>(), instance, PrivateFinalField::class.getDeclaredMemberProperty("field"))
        service.instance(Unit).shouldBe(42)
    }

    @Test
    fun `#instance should throw exception when field returns null`() {
        val instance = NullProperty()
        val service = PropertyService(typeKey<Int>(), instance, NullProperty::class.getDeclaredMemberProperty("property"))

        shouldThrow<WinterException> {
            service.instance(Unit)
        }.message.shouldBe("Property `io.jentz.winter.junit4.PropertyServiceTest\$NullProperty::property returned null`.")
    }

}