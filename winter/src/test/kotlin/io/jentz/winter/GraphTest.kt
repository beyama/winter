package io.jentz.winter

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class GraphTest {

    private val emptyGraph = component {}.init()

    @Test
    fun `#instance should return instance returned by the provider`() {
        val instance = Any()
        val graph = component { provider { instance } }.init()
        assertSame(instance, graph.instance())
    }

    @Test
    fun `#instance should return instance with generics returned by the provider`() {
        val withGenerics = mapOf(1 to "1")
        val graph = component {
            provider(generics = true) { withGenerics }
            provider { mapOf<Int, String>() }
        }.init()
        assertSame(withGenerics, graph.instance<Map<Int, String>>(generics = true))
    }

    @Test(expected = EntryNotFoundException::class)
    fun `#instance should throw an EntryNotFoundException when the requested provider does not exist`() {
        emptyGraph.instance<Service>()
    }

    @Test
    fun `#instance should be able to retrieve dependencies from parent graph`() {
        val graph = component {
            constant("foo")
            provider<ServiceDependency> { ServiceDependencyImpl(instance()) }
            subcomponent("sub") {
                provider<Service> { ServiceImpl(instance()) }
            }
        }.init().initSubcomponent("sub")
        assertEquals("foo", graph.instance<Service>().dependency.aValue)
    }

    @Test
    fun `#instance should be able to retrieve dependencies with generics from parent graph`() {
        val graph = component {
            constant("foo")
            provider<GenericDependency<String>>(generics = true) { GenericDependencyImpl(instance()) }
            subcomponent("sub") {
                provider<GenericService<String>>(generics = true) {
                    GenericServiceImpl(instance(generics = true))
                }
            }
        }.init().initSubcomponent("sub")
        assertEquals("foo", graph.instance<GenericService<String>>(generics = true).dependency.aValue)
    }

    @Test
    fun `#instanceOrNull should return instance returned by the provider`() {
        val instance = Any()
        val graph = component { provider { instance } }.init()
        assertSame(instance, graph.instanceOrNull())
    }

    @Test
    fun `#instanceOrNull should return instance with generics returned by the provider`() {
        val instance = mapOf(1 to "1")
        val graph = component { provider(generics = true) { instance } }.init()
        assertSame(instance, graph.instanceOrNull<Map<Int, String>>(generics = true))
    }

    @Test
    fun `#instanceOrNull should return null when the requested provider does not exist`() {
        assertNull(emptyGraph.instanceOrNull<Service>())
    }

    @Test(expected = EntryNotFoundException::class)
    fun `#provider should throw a EntryNotFoundException when provider does not exist`() {
        emptyGraph.provider<Any>()
    }

    @Test
    fun `#provider should return a provider function that calls the register provider block when invoked`() {
        val instance = Any()
        val graph = component { provider { instance } }.init()
        val provider = graph.provider<Any>()
        assertSame(instance, provider())
    }

    @Test
    fun `#providerOrNull should return null when provider does not exist`() {
        assertNull(emptyGraph.providerOrNull<Any>())
    }

    @Test
    fun `#providerOrNull should return a provider function that calls the register provider block when invoked`() {
        val instance = Any()
        val graph = component { provider { instance } }.init()
        val provider = graph.provider<Any>()
        assertSame(instance, provider())
    }

    @Test
    fun `provider block should get called every time a value is requested`() {
        val atomicInteger = AtomicInteger(0)
        val graph = component { provider { atomicInteger.getAndIncrement() } }.init()
        (0 until 5).forEach { graph.instance<Int>() }
        assertEquals(5, atomicInteger.get())
    }

    @Test
    fun `provider block should have access to graph`() {
        val graph = component {
            constant("foo")
            provider<ServiceDependency> { ServiceDependencyImpl(instance()) }
            provider<Service> { ServiceImpl(instance()) }
        }.init()

        assertEquals("foo", graph.instance<Service>().dependency.aValue)
    }

    @Test
    fun `singleton block should only get called once`() {
        val atomicInteger = AtomicInteger(0)
        val graph = component { provider(scope = singleton) { atomicInteger.getAndIncrement() } }.init()
        (0 until 5).forEach { graph.instance<Int>() }
        assertEquals(1, atomicInteger.get())
    }

    @Test
    fun `singleton block should have access to graph`() {
        val graph = component {
            constant("foo")
            provider<ServiceDependency> { ServiceDependencyImpl(instance()) }
            provider<Service>(scope = singleton) { ServiceImpl(instance()) }
        }.init()

        assertEquals("foo", graph.instance<Service>().dependency.aValue)
    }

    @Test
    fun `eager singleton should get initialized when component gets initialized`() {
        var initialized = false
        component { eagerSingleton { initialized = true } }.init()
        assertTrue(initialized)
    }

    @Test(expected = EntryNotFoundException::class)
    fun `graph initialisation should throw an exception if a eager dependency doesn't exist`() {
        component { eagerDependencies += providerKey<Service>() }.init()
    }

    @Test
    fun `#factory should return a factory function that calls the registered factory block when invoked`() {
        val graph = component { factory { int: Int -> 4 + int } }.init()
        val factory = graph.factory<Int, Int>()
        assertEquals(10, factory(6))
    }

    @Test(expected = CyclicDependencyException::class)
    fun `factory should detect cyclic dependency on invocation`() {
        val graph = component {
            factory { arg: Int -> factory<Int, Int>().invoke(arg) }
        }.init()
        graph.factory<Int, Int>().invoke(42)
    }

    @Test
    fun `multiton block should only get called once per argument`() {
        val atomicInteger = AtomicInteger(0)
        val graph = component { factory(scope = multiton) { add: Int -> atomicInteger.getAndAdd(add) } }.init()

        (0 until 5).forEach { graph.factory<Int, Int>().invoke(4) }
        (0 until 5).forEach { graph.factory<Int, Int>().invoke(6) }

        assertEquals(10, atomicInteger.get())
    }

    @Test
    fun `multiton block should have access to graph`() {
        val graph = component {
            constant("Hello %s!")
            factory<String, ServiceDependency> { arg: String -> ServiceDependencyImpl(instance<String>().format(arg)) }
            factory<String, Service>(scope = multiton) { name: String ->
                ServiceImpl(factory<String, ServiceDependency>().invoke(name))
            }
        }.init()

        val f: (String) -> Service = graph.factory()
        assertEquals("Hello Joe!", f("Joe").dependency.aValue)
    }

    @Test(expected = CyclicDependencyException::class)
    fun `graph should detect cyclic dependencies`() {
        component {
            provider {
                instance<Any>()
                Any()
            }
        }.init().instance<Any>()
    }

    @Test
    fun `provider registered via init block should be retrievable from graph`() {
        val component = component { provider<ServiceDependency> { ServiceDependencyImpl(instance()) } }
        val graph = component.init { constant("foo") }
        assertEquals("foo", graph.instance<ServiceDependency>().aValue)
    }

}