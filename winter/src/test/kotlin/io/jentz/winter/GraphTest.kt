package io.jentz.winter

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class GraphTest {

    private val emptyGraph = graph {}

    @Test
    fun `#instance should return instance returned by the provider`() {
        val instance = Any()
        val graph = graph { provider { instance } }
        assertSame(instance, graph.instance())
    }

    @Test
    fun `#instance should return instance with generics returned by the provider`() {
        val withGenerics = mapOf(1 to "1")
        val graph = graph {
            provider(generics = true) { withGenerics }
            provider { mapOf<Int, String>() }
        }
        assertSame(withGenerics, graph.instance<Map<Int, String>>(generics = true))
    }

    @Test(expected = EntryNotFoundException::class)
    fun `#instance should throw an EntryNotFoundException when the requested provider does not exist`() {
        emptyGraph.instance<Service>()
    }

    @Test
    fun `#instance should be able to retrieve dependencies from parent graph`() {
        val graph = graph {
            constant("foo")
            provider<ServiceDependency> { ServiceDependencyImpl(instance()) }
            subcomponent("sub") {
                provider<Service> { ServiceImpl(instance()) }
            }
        }.initSubcomponent("sub")
        assertEquals("foo", graph.instance<Service>().dependency.aValue)
    }

    @Test
    fun `#instance should be able to retrieve dependencies with generics from parent graph`() {
        val graph = graph {
            constant("foo")
            provider<GenericDependency<String>>(generics = true) { GenericDependencyImpl(instance()) }
            subcomponent("sub") {
                provider<GenericService<String>>(generics = true) {
                    GenericServiceImpl(instance(generics = true))
                }
            }
        }.initSubcomponent("sub")
        assertEquals("foo", graph.instance<GenericService<String>>(generics = true).dependency.aValue)
    }

    @Test(expected = WinterException::class)
    fun `#instance should throw an exception if graph is disposed`() {
        val graph = graph { constant("foo") }
        graph.dispose()
        graph.instance<String>()
    }

    @Test
    fun `#instanceOrNull should return instance returned by the provider`() {
        val instance = Any()
        val graph = graph { provider { instance } }
        assertSame(instance, graph.instanceOrNull())
    }

    @Test
    fun `#instanceOrNull should return instance with generics returned by the provider`() {
        val instance = mapOf(1 to "1")
        val graph = graph { provider(generics = true) { instance } }
        assertSame(instance, graph.instanceOrNull<Map<Int, String>>(generics = true))
    }

    @Test
    fun `#instanceOrNull should return null when the requested provider does not exist`() {
        assertNull(emptyGraph.instanceOrNull<Service>())
    }

    @Test(expected = WinterException::class)
    fun `#instanceOrNull should throw an exception if graph is disposed`() {
        val graph = graph {}
        graph.dispose()
        graph.instanceOrNull<String>()
    }

    @Test(expected = EntryNotFoundException::class)
    fun `#provider should throw a EntryNotFoundException when provider does not exist`() {
        emptyGraph.provider<Any>()
    }

    @Test
    fun `#provider should return a provider function that calls the register provider block when invoked`() {
        val instance = Any()
        val graph = graph { provider { instance } }
        val provider = graph.provider<Any>()
        assertSame(instance, provider())
    }

    @Test(expected = WinterException::class)
    fun `#provider should throw an exception if graph is disposed`() {
        val graph = graph { constant("foo") }
        graph.dispose()
        graph.provider<String>()
    }

    @Test
    fun `#providerOrNull should return null when provider does not exist`() {
        assertNull(emptyGraph.providerOrNull<Any>())
    }

    @Test
    fun `#providerOrNull should return a provider function that calls the register provider block when invoked`() {
        val instance = Any()
        val graph = graph { provider { instance } }
        val provider = graph.provider<Any>()
        assertSame(instance, provider())
    }

    @Test(expected = WinterException::class)
    fun `#providerOrNull should throw an exception if graph is disposed`() {
        val graph = graph {}
        graph.dispose()
        graph.providerOrNull<String>()
    }

    @Test
    fun `provider block should get called every time a value is requested`() {
        val atomicInteger = AtomicInteger(0)
        val graph = graph { provider { atomicInteger.getAndIncrement() } }
        (0 until 5).forEach { graph.instance<Int>() }
        assertEquals(5, atomicInteger.get())
    }

    @Test
    fun `provider block should have access to graph`() {
        val graph = graph {
            constant("foo")
            provider<ServiceDependency> { ServiceDependencyImpl(instance()) }
            provider<Service> { ServiceImpl(instance()) }
        }

        assertEquals("foo", graph.instance<Service>().dependency.aValue)
    }

    @Test
    fun `singleton block should only get called once`() {
        val atomicInteger = AtomicInteger(0)
        val graph = graph { provider(scope = singleton) { atomicInteger.getAndIncrement() } }
        (0 until 5).forEach { graph.instance<Int>() }
        assertEquals(1, atomicInteger.get())
    }

    @Test
    fun `singleton block should have access to graph`() {
        val graph = graph {
            constant("foo")
            provider<ServiceDependency> { ServiceDependencyImpl(instance()) }
            provider<Service>(scope = singleton) { ServiceImpl(instance()) }
        }

        assertEquals("foo", graph.instance<Service>().dependency.aValue)
    }

    @Test
    fun `eager singleton should get initialized when component gets initialized`() {
        var initialized = false
        graph { eagerSingleton { initialized = true } }
        assertTrue(initialized)
    }

    @Test
    fun `#factory should return a factory function that calls the registered factory block when invoked`() {
        val graph = graph { factory { int: Int -> 4 + int } }
        val factory = graph.factory<Int, Int>()
        assertEquals(10, factory(6))
    }

    @Test(expected = CyclicDependencyException::class)
    fun `factory should detect cyclic dependency on invocation`() {
        val graph = graph {
            factory { arg: Int -> factory<Int, Int>().invoke(arg) }
        }
        graph.factory<Int, Int>().invoke(42)
    }

    @Test
    fun `multiton block should only get called once per argument`() {
        val atomicInteger = AtomicInteger(0)
        val graph = graph { factory(scope = multiton) { add: Int -> atomicInteger.getAndAdd(add) } }

        (0 until 5).forEach { graph.factory<Int, Int>().invoke(4) }
        (0 until 5).forEach { graph.factory<Int, Int>().invoke(6) }

        assertEquals(10, atomicInteger.get())
    }

    @Test
    fun `multiton block should have access to graph`() {
        val graph = graph {
            constant("Hello %s!")
            factory<String, ServiceDependency> { arg: String -> ServiceDependencyImpl(instance<String>().format(arg)) }
            factory<String, Service>(scope = multiton) { name: String ->
                ServiceImpl(factory<String, ServiceDependency>().invoke(name))
            }
        }

        val f: (String) -> Service = graph.factory()
        assertEquals("Hello Joe!", f("Joe").dependency.aValue)
    }

    @Test(expected = CyclicDependencyException::class)
    fun `graph should detect cyclic dependencies`() {
        graph {
            provider {
                instance<Any>()
                Any()
            }
        }.instance<Any>()
    }

    @Test
    fun `provider registered via init block should be retrievable from graph`() {
        val component = component { provider<ServiceDependency> { ServiceDependencyImpl(instance()) } }
        val graph = component.init { constant("foo") }
        assertEquals("foo", graph.instance<ServiceDependency>().aValue)
    }

    @Test
    fun `#providersOfType should return a set of providers of given type`() {
        val graph = graph {
            provider("something else") { Any() }
            provider("a") { "a" }
            provider("b") { "b" }
            provider("c") { "c" }
        }

        val providers = graph.providersOfType<String>()
        val instances = providers.map { it() }

        assertEquals(3, providers.size)

        listOf("a", "b", "c").forEach { v ->
            assertTrue(instances.contains(v))
        }
    }

    @Test
    fun `#instancesOfType should return a set of instances of given type`() {
        val graph = graph {
            provider("something else") { Any() }
            provider("a") { "a" }
            provider("b") { "b" }
            provider("c") { "c" }
        }

        val instances = graph.instancesOfType<String>()

        assertEquals(3, instances.size)

        listOf("a", "b", "c").forEach { v ->
            assertTrue(instances.contains(v))
        }
    }

    @Test
    fun `#dispose should mark the graph as disposed`() {
        val graph = graph {}
        assertFalse(graph.isDisposed)
        graph.dispose()
        assertTrue(graph.isDisposed)
    }

    @Test
    fun `subsequent calls to #dispose should be ignored`() {
        var count = 0
        val graph = graph {}
        WinterPlugins.addGraphDisposePlugin { count += 1 }
        (0..3).forEach { graph.dispose() }
        WinterPlugins.resetGraphDisposePlugins()
        assertEquals(1, count)
    }

}