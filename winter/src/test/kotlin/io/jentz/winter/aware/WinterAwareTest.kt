package io.jentz.winter.aware

import com.nhaarman.mockitokotlin2.*
import io.jentz.winter.*
import io.jentz.winter.delegate.*
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

class WinterAwareTest {

    private val aware = TestAware()
    private val app = aware.winterApplication

    @BeforeEach
    fun beforeEach() {
        reset(app)
    }

    @Nested
    inner class DefaultInterfaceMethods {

        @Test
        fun `#graph should call WinterApplication#getGraph with this`() {
            val graph = emptyGraph()
            whenever(app.getGraph(aware)).thenReturn(graph)
            aware.graph.shouldBeSameInstanceAs(graph)
            verify(app, times(1)).getGraph(aware)
        }

        @Test
        fun `#winterApplication should return default Winter object`() {
            val aware = object : WinterAware {}
            aware.winterApplication.shouldBeSameInstanceAs(Winter)
        }

    }

    @Nested
    inner class GraphMethods {

        private val aware = object : WinterAware {
            override val graph: Graph = graph {
                singleton { 42 }
            }
        }

        @Test
        fun `#instance should resolve instance`() {
            aware.instance<Int>().shouldBe(42)
        }

        @Test
        fun `#instanceOrNull should resolve instance`() {
            aware.instanceOrNull<Int>().shouldBe(42)
        }

        @Test
        fun `#instanceOrNull should return null if type doesn't exist`() {
            aware.instanceOrNull<Date>().shouldBe(null)
        }

        @Test
        fun `#lazyInstance should return lazy`() {
            aware.lazyInstance<Int>().apply {
                isInitialized().shouldBeFalse()
                value.shouldBe(42)
            }
        }

        @Test
        fun `#lazyInstanceOrNull should return lazy`() {
            aware.lazyInstanceOrNull<Int>().apply {
                isInitialized().shouldBeFalse()
                value.shouldBe(42)
            }
        }

        @Test
        fun `#instanceOrNull should return lazy which returns null if type doesn't exist`() {
            aware.lazyInstanceOrNull<Date>().apply {
                isInitialized().shouldBeFalse()
                value.shouldBe(null)
            }
        }

        @Test
        fun `#provider should return function which resolves instance`() {
            aware.provider<Int>().invoke().shouldBe(42)
        }

        @Test
        fun `#providerOrNull should return function which resolves instance`() {
            aware.providerOrNull<Int>()?.invoke().shouldBe(42)
        }

        @Test
        fun `#providerOrNull should return null if type doesn't exist`() {
            aware.providerOrNull<Date>().shouldBe(null)
        }

        @Test
        fun `#providersOfType should return a set of providers`() {
            aware.providersOfType<Int>().apply {
                shouldBeInstanceOf<Set<*>>()
                size.shouldBe(1)
            }
        }

        @Test
        fun `#instancesOfType should return a set of instances`() {
            aware.instancesOfType<Int>().apply {
                shouldBeInstanceOf<Set<*>>()
                size.shouldBe(1)
                first().shouldBe(42)
            }
        }

    }

    @Nested
    inner class ApplicationMethods {

        @Test
        fun `#openGraph should call WinterApplication#openGraph with this`() {
            val block: ComponentBuilderBlock = {}
            whenever(app.openGraph(aware, block)).thenReturn(emptyGraph())

            aware.openGraph(block)

            verify(app, times(1)).openGraph(aware, block)
        }

        @Test
        fun `#getOrOpenGraph should call WinterApplication#getOrOpenGraph with this`() {
            val block: ComponentBuilderBlock = {}
            whenever(app.getOrOpenGraph(aware, block)).thenReturn(emptyGraph())

            aware.getOrOpenGraph(block)

            verify(app, times(1)).getOrOpenGraph(aware, block)
        }

        @Test
        fun `#openGraphAndInject should call WinterApplication#openGraphAndInject with this`() {
            val block: ComponentBuilderBlock = {}
            whenever(app.openGraphAndInject(aware, block)).thenReturn(emptyGraph())

            aware.openGraphAndInject(block)

            verify(app, times(1)).openGraphAndInject(aware, block)
        }

        @Test
        fun `#isGraphOpen should call WinterApplication#isGraphOpen with this`() {
            whenever(app.isGraphOpen(aware)).thenReturn(true)
            aware.isGraphOpen()
            verify(app, times(1)).isGraphOpen(aware)
        }

        @Test
        fun `#closeGraph should call WinterApplication#closeGraph with this`() {
            aware.closeGraph()
            verify(app, times(1)).closeGraph(aware)
        }

    }

    private open class TestAware : WinterAware {
        override val winterApplication: WinterApplication = mock()
    }

}