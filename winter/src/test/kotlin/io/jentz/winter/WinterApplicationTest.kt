package io.jentz.winter

import com.nhaarman.mockitokotlin2.*
import io.jentz.winter.WinterApplication.InjectionAdapter
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
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
                app.injectionAdapter = mock(); null
            }.message.shouldBe("Cannot set injection adapter because application graph is already open")
        }

        @Test
        fun `#getGraph should pass instance to Adapter#get and return the result`() {
            whenever(adapter.get(instance)).thenReturn(rootGraph)
            app.getGraph(instance)
            verify(adapter, times(1)).get(instance)
        }

        @Test
        fun `#getGraph should throw an exception when adapter returns null`() {
            shouldThrow<WinterException> {
                app.getGraph(instance)
            }.message.shouldBe("No graph found for instance `$instance`.")
        }

        @Test
        fun `#openGraph should pass instance to Adapter#open and return the result`() {
            val block: ComponentBuilderBlock = {}
            whenever(adapter.open(instance, block)).thenReturn(rootGraph)
            app.openGraph(instance, block).shouldBeSameInstanceAs(rootGraph)
            verify(adapter, times(1)).open(instance, block)
        }

        @Test
        fun `#getOrOpenGraph should pass instance to Adapter#open and return the result if graph is not open`() {
            val block: ComponentBuilderBlock = {}
            whenever(adapter.open(instance, block)).thenReturn(rootGraph)
            app.getOrOpenGraph(instance, block).shouldBeSameInstanceAs(rootGraph)
            verify(adapter, times(1)).open(instance, block)
        }

        @Test
        fun `#getOrOpenGraph should call Adapter#get and return the result if graph is open`() {
            val block: ComponentBuilderBlock = {}

            whenever(adapter.get(instance)).thenReturn(rootGraph)
            app.getOrOpenGraph(instance, block).shouldBeSameInstanceAs(rootGraph)
            verify(adapter, times(1)).get(instance)
        }

        @Test
        fun `#isGraphOpen should return true if Adapter#get returns a graph`() {
            whenever(adapter.get(instance)).thenReturn(rootGraph)
            app.isGraphOpen(instance).shouldBeTrue()
        }

        @Test
        fun `#isGraphOpen should return false if Adapter#get returns null`() {
            app.isGraphOpen(instance).shouldBeFalse()
        }

        @Test
        fun `#closeGraph should pass instance to Adapter#close`() {
            app.closeGraph(instance)
            verify(adapter, times(1)).close(instance)
        }

        @Test
        fun `#closeGraphIfOpen should call Adapter#close when Adapter#get returned graph`() {
            whenever(adapter.get(instance)).thenReturn(rootGraph)
            app.closeGraphIfOpen(instance)
            verify(adapter, times(1)).get(instance)
            verify(adapter, times(1)).close(instance)
        }

        @Test
        fun `#closeGraphIfOpen should not call Adapter#close when Adapter#get returned null`() {
            app.closeGraphIfOpen(instance)
            verify(adapter, times(1)).get(instance)
            verify(adapter, never()).close(instance)
        }

        @Test
        fun `#inject with injection target should call graph#inject`() {
            val graph = mock<Graph>()
            whenever(adapter.get(instance)).thenReturn(graph)

            app.inject(instance)
            verify(graph, times(1)).inject(instance)
        }

        @Test
        fun `#openGraphAndInject with injection target should open graph and call graph#inject on it`() {
            val block: ComponentBuilderBlock = {}
            val graph = mock<Graph>()

            whenever(adapter.open(instance, block)).thenReturn(graph)

            app.openGraphAndInject(instance, block)

            verify(adapter, times(1)).open(instance, block)
            verify(graph, times(1)).inject(instance)
        }

    }

}
