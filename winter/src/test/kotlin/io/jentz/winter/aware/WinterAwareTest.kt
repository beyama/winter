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

    @Nested
    inner class DelegateMethods {

        @Test
        fun `should return ProviderProperty for #injectProvider`() {
            aware.injectProvider<String>().shouldBeInstanceOf<ProviderProperty<*>>()
        }

        @Test
        fun `should return ProviderOrNullProperty for #injectProviderOrNull`() {
            aware.injectProviderOrNull<String>()
                .shouldBeInstanceOf<ProviderOrNullProperty<*>>()
        }

        @Test
        fun `should return InstanceProperty for #inject`() {
            aware.inject<String>().shouldBeInstanceOf<InstanceProperty<*>>()
        }

        @Test
        fun `should return InstanceOrNullProperty for #injectOrNull`() {
            aware.injectOrNull<String>()
                .shouldBeInstanceOf<InstanceOrNullProperty<*>>()
        }

        @Test
        fun `should return LazyInstanceProperty for #injectLazy`() {
            aware.injectLazy<String>()
                .shouldBeInstanceOf<LazyInstanceProperty<*>>()
        }

        @Test
        fun `should return LazyInstanceOrNullProperty for #injectLazyOrNull`() {
            aware.injectLazyOrNull<String>()
                .shouldBeInstanceOf<LazyInstanceOrNullProperty<*>>()
        }

        @Test
        fun `should return ProvidersOfTypeProperty for #injectProvidersOfType`() {
            aware.injectProvidersOfType<String>()
                .shouldBeInstanceOf<ProvidersOfTypeProperty<*>>()
        }

        @Test
        fun `should return InstancesOfTypeProperty for #injectInstancesOfType`() {
            aware.injectInstancesOfType<String>()
                .shouldBeInstanceOf<InstancesOfTypeProperty<*>>()
        }

        @Test
        fun `should return LazyInstancesOfTypeProperty for #injectLazyInstancesOfType`() {
            aware.injectLazyInstancesOfType<String>()
                .shouldBeInstanceOf<LazyInstancesOfTypeProperty<*>>()
        }

    }

    private open class TestAware : WinterAware {
        override val winterApplication = WinterApplication().also { it.injectionAdapter = mock() }
    }

}