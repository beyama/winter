package io.jentz.winter.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test

class TypeMappingTest {

    @Test
    fun `should map Java classes to Kotlin classes`() {
        ClassName("java.lang", "String")
            .kotlinTypeName
            .shouldBe(String::class.asClassName())
    }

    @Test
    fun `should map generic Java classes to Kotlin classes`() {
        val jvmString = ClassName("java.lang", "String")
        val jvmMap = ClassName("java.util", "Map")
        val jvmList = ClassName("java.util", "List")
        val kMap = Map::class.asClassName()
        val kList = List::class.asClassName()
        val kString = String::class.asClassName()

        jvmMap.parameterizedBy(jvmString, jvmString)
            .kotlinTypeName
            .shouldBe(kMap.parameterizedBy(kString, kString))

        jvmList.parameterizedBy(jvmString)
            .kotlinTypeName
            .shouldBe(kList.parameterizedBy(kString))
    }

}