package io.jentz.winter

import com.nhaarman.mockitokotlin2.*
import io.jentz.winter.WinterApplication.InjectionAdapter
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class WinterApplicationTest {

    private val app = WinterApplication()

    @BeforeEach
    fun beforeEach() {
        app.tree.closeIfOpen()
    }

    @Test
    fun `#component should configure new component`() {
        app.component("test") { constant("") }
        app.component.qualifier.shouldBe("test")
        app.component.size.shouldBe(1)
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

        private val rootGraph = graph {}

        @BeforeEach
        fun beforeEach() {
            reset(adapter)
            app.injectionAdapter = adapter
        }

        @Test
        fun `#injectionAdapter should throw an exception if tree is already open`() {
            app.tree.open()
            shouldThrow<WinterException> {
                app.injectionAdapter = adapter; null
            }.message.shouldBe("Cannot set injection adapter because application graph is already open")
        }

        @Test
        fun `#getGraph should pass instance to Adapter#getGraph and return the result`() {
            whenever(adapter.getGraph(instance)).thenReturn(rootGraph)
            app.getGraph(instance).shouldBeSameInstanceAs(rootGraph)
        }

        @Test
        fun `#createGraph should pass instance to Adapter#createGraph and return the result`() {
            whenever(adapter.createGraph(instance, null)).thenReturn(rootGraph)
            app.createGraph(instance).shouldBeSameInstanceAs(rootGraph)
        }

        @Test
        fun `#createGraph should pass builder block to Adapter#createGraph`() {
            val block: ComponentBuilderBlock = {}
            whenever(adapter.createGraph(instance, block)).thenReturn(rootGraph)
            app.createGraph(instance, block).shouldBeSameInstanceAs(rootGraph)
        }

        @Test
        fun `#disposeGraph should pass instance to Adapter#disposeGraph`() {
            app.disposeGraph(instance)
            verify(adapter, times(1)).disposeGraph(instance)
        }

        @Nested
        @DisplayName("JSR330 methods")
        inner class JSR330MemberInjector {

            @Test
            fun `#inject with injection target should call graph#inject`() {
                val graph = mock<Graph>()
                whenever(adapter.getGraph(instance)).thenReturn(graph)

                app.inject(instance)
                verify(graph, times(1)).inject(instance)
            }

            @Test
            fun `#createGraphAndInject with injection target should create graph and call graph#inject on it`() {
                val graph = mock<Graph>()
                whenever(adapter.createGraph(instance, null)).thenReturn(graph)

                app.createGraphAndInject(instance)
                verify(graph, times(1)).inject(instance)
            }
        }

    }

}
