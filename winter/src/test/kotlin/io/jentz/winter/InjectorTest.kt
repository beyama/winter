package io.jentz.winter

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class InjectorTest {

    private val emptyGraph = component { }.init()

    private lateinit var injector: Injector

    @Before
    fun beforeEach() {
        injector = Injector()
    }

    @Test(expected = UninitializedPropertyAccessException::class)
    fun `Instance#value should throw an error if accessed before injected`() {
        Injector.Instance<String>(typeKey<String>()).value
    }

    @Test(expected = EntryNotFoundException::class)
    fun `Instance#inject should throw an error if dependency couldn't be found`() {
        Injector.Instance<String>(typeKey<String>()).inject(emptyGraph)
    }

    @Test
    fun `Instance#inject should eagerly resolve dependency`() {
        val integer = AtomicInteger(0)
        val graph = component { provider { integer.getAndIncrement() } }.init()
        Injector.Instance<Int>(typeKey<Int>()).inject(graph)
        assertEquals(1, integer.get())
    }

    @Test(expected = UninitializedPropertyAccessException::class)
    fun `InstanceOrNull should throw an error if accessed before injected`() {
        Injector.InstanceOrNull<String>(typeKey<String>()).value
    }

    @Test
    fun `InstanceOrNull#inject should eagerly resolve dependency`() {
        val integer = AtomicInteger(0)
        val graph = component { provider { integer.getAndIncrement() } }.init()
        Injector.InstanceOrNull<Int>(typeKey<Int>()).inject(graph)
        assertEquals(1, integer.get())
    }

    @Test
    fun `InstanceOrNull#value should return null if provider doesn't exist`() {
        val property = Injector.InstanceOrNull<Any>(typeKey<Any>())
        property.inject(emptyGraph)
        assertNull(property.value)
    }

    @Test(expected = UninitializedPropertyAccessException::class)
    fun `LazyInstance should throw an error if accessed before injected`() {
        Injector.LazyInstance<String>(typeKey<String>()).value
    }

    @Test
    fun `LazyInstance should resolve dependency on first access`() {
        val integer = AtomicInteger(0)
        val graph = component { provider { integer.getAndIncrement() } }.init()
        val property = Injector.LazyInstance<Int>(typeKey<Int>())
        property.inject(graph)
        assertEquals(0, integer.get())
        property.value
        assertEquals(1, integer.get())
    }

    @Test(expected = UninitializedPropertyAccessException::class)
    fun `LazyInstanceOrNull should throw an error if accessed before injected`() {
        Injector.LazyInstanceOrNull<String>(typeKey<String>()).value
    }

    @Test
    fun `LazyInstanceOrNull#value should return null if provider doesn't exist`() {
        val property = Injector.LazyInstanceOrNull<Any>(typeKey<Any>())
        property.inject(emptyGraph)
        assertNull(property.value)
    }

    @Test
    fun `MapProperty#value should apply mapping function to value of base property and memorize and return the result`() {
        val graph = component { constant(21) }.init()
        val property = Injector.Instance<Int>(typeKey<Int>())
        val mapped = property.map { it * 2 }
        property.inject(graph)
        assertEquals(42, mapped.value)
    }

    @Test
    fun `Injector#instance should return an instance of Instance`() {
        assertTrue(injector.instance<String>() is Injector.Instance)
    }

    @Test
    fun `Injector#instanceOrNull should return an instance of InstanceOrNull`() {
        assertTrue(injector.instanceOrNull<String>() is Injector.InstanceOrNull)
    }

    @Test
    fun `Injector#lazyInstance should return an instance of LazyInstance`() {
        assertTrue(injector.lazyInstance<String>() is Injector.LazyInstance)
    }

    @Test
    fun `Injector#lazyInstanceOrNull should return an instance of LazyInstanceOrNull`() {
        assertTrue(injector.lazyInstanceOrNull<String>() is Injector.LazyInstanceOrNull)
    }

    @Test
    fun `Injector#factory should return an instance of Instance`() {
        assertTrue(injector.factory<Int, String>() is Injector.Instance)
    }

    @Test
    fun `Injector#factoryOrNull should return an instance of InstanceOrNull`() {
        assertTrue(injector.factoryOrNull<Int, String>() is Injector.InstanceOrNull)
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