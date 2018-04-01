package io.jentz.winter

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class InjectorTest {

    private val emptyGraph = graph {}

    private lateinit var injector: Injector

    @Before
    fun beforeEach() {
        injector = Injector()
    }

    @Test(expected = UninitializedPropertyAccessException::class)
    fun `InstanceProperty#value should throw an error if accessed before injected`() {
        Injector.InstanceProperty<String>(typeKey<String>()).value
    }

    @Test(expected = EntryNotFoundException::class)
    fun `InstanceProperty#inject should throw an error if dependency couldn't be found`() {
        Injector.InstanceProperty<String>(typeKey<String>()).inject(emptyGraph)
    }

    @Test
    fun `InstanceProperty#inject should eagerly resolve dependency`() {
        val integer = AtomicInteger(0)
        val graph = graph { provider { integer.getAndIncrement() } }
        Injector.InstanceProperty<Int>(typeKey<Int>()).inject(graph)
        assertEquals(1, integer.get())
    }

    @Test(expected = UninitializedPropertyAccessException::class)
    fun `InstanceOrNullProperty should throw an error if accessed before injected`() {
        Injector.InstanceOrNullProperty<String>(typeKey<String>()).value
    }

    @Test
    fun `InstanceOrNullProperty#inject should eagerly resolve dependency`() {
        val integer = AtomicInteger(0)
        val graph = graph { provider { integer.getAndIncrement() } }
        Injector.InstanceOrNullProperty<Int>(typeKey<Int>()).inject(graph)
        assertEquals(1, integer.get())
    }

    @Test
    fun `InstanceOrNullProperty#value should return null if provider doesn't exist`() {
        val property = Injector.InstanceOrNullProperty<Any>(typeKey<Any>())
        property.inject(emptyGraph)
        assertNull(property.value)
    }

    @Test(expected = UninitializedPropertyAccessException::class)
    fun `LazyInstanceProperty should throw an error if accessed before injected`() {
        Injector.LazyInstanceProperty<String>(typeKey<String>()).value
    }

    @Test
    fun `LazyInstanceProperty should resolve dependency on first access`() {
        val integer = AtomicInteger(0)
        val graph = graph { provider { integer.getAndIncrement() } }
        val property = Injector.LazyInstanceProperty<Int>(typeKey<Int>())
        property.inject(graph)
        assertEquals(0, integer.get())
        property.value
        assertEquals(1, integer.get())
    }

    @Test(expected = UninitializedPropertyAccessException::class)
    fun `LazyInstanceOrNullProperty should throw an error if accessed before injected`() {
        Injector.LazyInstanceOrNullProperty<String>(typeKey<String>()).value
    }

    @Test
    fun `LazyInstanceOrNullProperty#value should return null if provider doesn't exist`() {
        val property = Injector.LazyInstanceOrNullProperty<Any>(typeKey<Any>())
        property.inject(emptyGraph)
        assertNull(property.value)
    }

    @Test
    fun `ProvidersOfTypeProperty#value should get all providers of type`() {
        val property = Injector.ProvidersOfTypeProperty<Int>(typeKeyOfType<Int>(false))
        val values = (1 until 5).toSet()
        val graph = graph { values.forEach { provider(it) { it } } }
        property.inject(graph)
        assertEquals(values, property.value.map { it() }.toSet())
    }

    @Test
    fun `InstancesOfTypeProperty#value should get all instances of type`() {
        val property = Injector.InstancesOfTypeProperty<Int>(typeKeyOfType<Int>(false))
        val values = (1 until 5).toSet()
        val graph = graph { values.forEach { provider(it) { it } } }
        property.inject(graph)
        assertEquals(values, property.value)
    }

    @Test
    fun `LazyInstancesOfTypeProperty should resolve dependencies of first access`() {
        val property = Injector.LazyInstancesOfTypeProperty<Int>(typeKeyOfType<Int>(false))
        val values = (1 until 5).toSet()
        val atomics = values.map { AtomicInteger(it) }
        val graph = graph { atomics.forEach { provider(it) { it.incrementAndGet() } } }
        property.inject(graph)

        assertEquals(values, atomics.map { it.get() }.toSet())
        assertEquals(values.map { it + 1 }.toSet(), property.value)
    }

    @Test
    fun `MapPropertyProperty#value should apply mapping function to value of base property and memorize and return the result`() {
        val graph = graph { constant(21) }
        val property = Injector.InstanceProperty<Int>(typeKey<Int>())
        val mapped = property.map { it * 2 }
        property.inject(graph)
        assertEquals(42, mapped.value)
    }

    @Test
    fun `Injector#provider should return an instance of ProviderProperty`() {
        assertTrue(injector.provider<String>() is Injector.ProviderProperty)
    }

    @Test
    fun `Injector#providerOrNull should return an instance of ProviderOrNullProperty`() {
        assertTrue(injector.providerOrNull<String>() is Injector.ProviderOrNullProperty)
    }

    @Test
    fun `Injector#instance should return an instance of InstanceProperty`() {
        assertTrue(injector.instance<String>() is Injector.InstanceProperty)
    }

    @Test
    fun `Injector#instanceOrNull should return an instance of InstanceOrNullProperty`() {
        assertTrue(injector.instanceOrNull<String>() is Injector.InstanceOrNullProperty)
    }

    @Test
    fun `Injector#lazyInstance should return an instance of LazyInstanceProperty`() {
        assertTrue(injector.lazyInstance<String>() is Injector.LazyInstanceProperty)
    }

    @Test
    fun `Injector#lazyInstanceOrNull should return an instance of LazyInstanceOrNullProperty`() {
        assertTrue(injector.lazyInstanceOrNull<String>() is Injector.LazyInstanceOrNullProperty)
    }

    @Test
    fun `Injector#factory should return an instance of InstanceProperty`() {
        assertTrue(injector.factory<Int, String>() is Injector.InstanceProperty)
    }

    @Test
    fun `Injector#factoryOrNull should return an instance of InstanceOrNullProperty`() {
        assertTrue(injector.factoryOrNull<Int, String>() is Injector.InstanceOrNullProperty)
    }

    @Test
    fun `Injector#providersOfType should return an instance of ProvidersOfTypeProperty`() {
        assertTrue(injector.providersOfType<String>() is Injector.ProvidersOfTypeProperty)
    }

    @Test
    fun `Injector#instancesOfType should return an instance of InstancesOfTypeProperty`() {
        assertTrue(injector.instancesOfType<String>() is Injector.InstancesOfTypeProperty)
    }

    @Test
    fun `Injector#lazyInstancesOfType should return an instance of LazyInstancesOfTypeProperty`() {
        assertTrue(injector.lazyInstancesOfType<String>() is Injector.LazyInstancesOfTypeProperty)
    }

    @Test
    fun `Injector#inject should inject graph on registered properties`() {
        val property = TestProperty()
        injector.register(property)
        injector.inject(emptyGraph)
        assertEquals(1, property.injectCalled)
    }

    @Test
    fun `Injector#injected should return true after injecting`() {
        assertFalse(injector.injected)
        injector.inject(emptyGraph)
        assertTrue(injector.injected)
    }

    @Test(expected = IllegalStateException::class)
    fun `Injector#register should thorow an exception if already injected`() {
        injector.inject(emptyGraph)
        injector.register(TestProperty())
    }

    @Test
    fun `Subsequent calls to Injector#inject should be ignored`() {
        val property = TestProperty()
        injector.register(property)
        (0 until 5).forEach { injector.inject(emptyGraph) }
        assertEquals(1, property.injectCalled)
    }

    private class TestProperty : Injector.AbstractEagerProperty<Any>() {
        var injectCalled = 0
        override fun getValue(graph: Graph): Any {
            injectCalled += 1
            return Any()
        }
    }

}