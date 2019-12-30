package io.jentz.winter

import com.nhaarman.mockitokotlin2.*
import io.jentz.winter.WinterApplication.InjectionAdapter
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class WinterApplicationTest {

    private val app = WinterApplication {}

    @BeforeEach
    fun beforeEach() {
        app.tree.closeIfOpen()
    }

    @Test
    fun `#component should configure new component`() {
        app.component("test") { constant("") }
        app.component!!.qualifier.shouldBe("test")
        app.component!!.size.shouldBe(1)
    }

    @Test
    fun `#component should throw exception if application graph is already open`() {
        app.tree.open()
        shouldThrow<WinterException> {
            app.component {}
        }.message.shouldBe("Cannot set component because application graph is already open")
    }

    @Test
    fun `#plugins should be empty by default`() {
        app.plugins.isEmpty().shouldBeTrue()
    }

    @Nested
    inner class AdapterBasedMethods {

        private val adapter: InjectionAdapter = mock()

        private val instance = Any()

        @BeforeEach
        fun beforeEach() {
            reset(adapter)
            app.injectionAdapter = adapter
        }

        @Test
        fun `#injectionAdapter should throw an exception if tree is already open`() {
            app.tree.open()
            shouldThrow<WinterException> {
                app.injectionAdapter = mock(); null
            }.message.shouldBe("Cannot set injection adapter because application graph is already open")
        }

        @Test
        fun `#inject with injection target should call graph#inject`() {
            val graph = mock<Graph>()
            whenever(adapter.get(instance)).thenReturn(graph)

            app.inject(instance)
            verify(graph, times(1)).inject(instance)
        }

    }

}
