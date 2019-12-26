package io.jentz.winter.aware

import com.nhaarman.mockitokotlin2.*
import io.jentz.winter.*
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
    private val injection = aware.winterApplication

    @BeforeEach
    fun beforeEach() {
        reset(injection.injectionAdapter)
    }

    @Nested
    inner class DefaultInterfaceMethods {

        @Test
        fun `#graph should call #injection#getGraph with instance`() {
            val graph = emptyGraph()
            whenever(injection.injectionAdapter.getGraph(aware)).thenReturn(graph)
            aware.graph.shouldBeSameInstanceAs(graph)
            verify(injection.injectionAdapter, times(1)).getGraph(aware)
        }

        @Test
        fun `#winterApplication should return default Winter object`() {
            val aware = object : WinterAware {}
            aware.winterApplication.shouldBeSameInstanceAs(Winter)
        }

    }

    @Nested
    inner class RetrievalMethods {

        private val aware = object : WinterAware {
            override val graph: Graph = graph {
                singleton { 42 }
                factory { i: Int -> i.toString() }
            }
        }

        @Test
        fun `#instance should resolve instance`() {
            aware.instance<Int>().shouldBe(42)
        }

        @Test
        fun `#instance with argument should resolve and call factory`() {
            aware.instance<Int, String>(42).shouldBe("42")
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
        fun `#instanceOrNull with argument should resolve and call factory`() {
            aware.instanceOrNull<Int, String>(42).shouldBe("42")
        }

        @Test
        fun `#instanceOrNull with argument should return null if factory doesn't exist`() {
            aware.instanceOrNull<Int, Date>(0).shouldBe(null)
        }

        @Test
        fun `#lazyInstance should return lazy`() {
            aware.lazyInstance<Int>().apply {
                isInitialized().shouldBeFalse()
                value.shouldBe(42)
            }
        }

        @Test
        fun `#lazyInstance with argument should return lazy`() {
            aware.lazyInstance<Int, String>(42).apply {
                isInitialized().shouldBeFalse()
                value.shouldBe("42")
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
        fun `#lazyInstanceOrNull with argument return lazy`() {
            aware.lazyInstanceOrNull<Int, String>(42).apply {
                isInitialized().shouldBeFalse()
                value.shouldBe("42")
            }
        }

        @Test
        fun `#lazyInstanceOrNull with argument should return lazy which returns null if factory doesn't exist`() {
            aware.lazyInstanceOrNull<Int, Date>(0).apply {
                isInitialized().shouldBeFalse()
                value.shouldBe(null)
            }
        }

        @Test
        fun `#provider should return function which resolves instance`() {
            aware.provider<Int>().invoke().shouldBe(42)
        }

        @Test
        fun `#provider with argument should return functions which calls factory`() {
            aware.provider<Int, String>(42).invoke().shouldBe("42")
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
        fun `#providerOrNull with argument should return function which calls factory`() {
            aware.providerOrNull<Int, String>(42)?.invoke().shouldBe("42")
        }

        @Test
        fun `#providerOrNull with argument should return null if factory doesn't exist`() {
            aware.providerOrNull<Int, Date>(0).shouldBe(null)
        }

        @Test
        fun `#factory should return factory function`() {
            aware.factory<Int, String>().invoke(42).shouldBe("42")
        }

        @Test
        fun `#factoryOrNull should return factory function`() {
            aware.factoryOrNull<Int, String>()?.invoke(42).shouldBe("42")
        }

        @Test
        fun `#factoryOrNull should return null if factory doesn't exist`() {
            aware.factoryOrNull<Int, Date>().shouldBe(null)
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
    inner class Jsr330Methods {

        @Test
        fun `#createGraphAndInject should use aware instance to create graph and inject into target`() {
            val graph: Graph = mock()
            val target = Any()
            whenever(injection.injectionAdapter.createGraph(aware, null)).thenReturn(graph)

            aware.createGraphAndInject(target)
            verify(injection.injectionAdapter, times(1)).createGraph(aware, null)
            verify(graph, times(1)).inject(target)
        }

        @Test
        fun `#inject should use aware instance to get graph and inject into target`() {
            val graph: Graph = mock()
            val target = Any()
            whenever(injection.injectionAdapter.getGraph(aware)).thenReturn(graph)

            aware.inject(target)
            verify(injection.injectionAdapter, times(1)).getGraph(aware)
            verify(graph, times(1)).inject(target)
        }

    }

    private open class TestAware : WinterAware {
        override val winterApplication = WinterApplication().also { it.injectionAdapter = mock() }
    }

}