package io.jentz.winter.testing

import io.jentz.winter.graph
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import org.mockito.Spy
import javax.inject.Named

@Suppress("unused")
class ComponentBuilderExtTest {

    private val testField = 12

    @Mock
    @Named("mock field")
    private val mockField = "mock field"

    @field:Spy
    @field:Named("spy field")
    private val spyField = "spy field"

    @Mock
    @Named("mock property")
    val mockProperty = "mock property"

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
        graph.instance<String>("mock field").shouldBe("mock field")
        graph.instance<String>("spy field").shouldBe("spy field")
        graph.instance<String>("mock property").shouldBe("mock property")
    }

}
