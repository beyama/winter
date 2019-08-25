package io.jentz.winter.testing

import io.jentz.winter.graph
import io.jentz.winter.typeKey
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Spy
import javax.inject.Named

class ComponentBuilderExtTest {

    private val testField = 12

    @Mock
    @Named("mock")
    private val testMockField = "test mock"

    @field:Spy
    @field:Named("spy")
    private val testSpyField = "test spy"

    @Test
    fun `#property should register property by name`() {
        graph {
            property(this@ComponentBuilderExtTest, "testField")
        }.instance<Int>().shouldBe(12)

    }

    @Test
    fun `#property should register property by name and key`() {
        graph {
            property(typeKey<Number>(),this@ComponentBuilderExtTest, "testField")
        }.instance<Number>().shouldBe(12)

    }

    @Test
    fun `#property should register property by KProperty1 instance`() {
        graph {
            val source = this@ComponentBuilderExtTest
            property(source, source::class.getDeclaredMemberProperty("testField"))
        }.instance<Int>().shouldBe(12)
    }

    @Test
    fun `#bindAllMocks should provide all Mock or Spy annotated fields`() {
        val graph = graph { bindAllMocks(this@ComponentBuilderExtTest) }
        graph.instance<String>("mock").shouldBe("test mock")
        graph.instance<String>("spy").shouldBe("test spy")
    }

}
