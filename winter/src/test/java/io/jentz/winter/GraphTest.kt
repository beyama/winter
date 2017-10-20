package io.jentz.winter

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class GraphTest {

    private val emptyGraph = component {}.init()

    @Test
    fun `#get should return instance returned by the provider`() {
        val instance = Any()
        val graph = component { provider { instance } }.init()
        assertSame(instance, graph.get())
    }

    @Test
    fun `#get should return instance with generics returned by the provider`() {
        val withGenerics = mapOf(1 to "1")
        val graph = component {
            provider(generics = true) { withGenerics }
            provider { mapOf<Int, String>() }
        }.init()
        assertSame(withGenerics, graph.get<Map<Int, String>>(generics = true))
    }

    @Test(expected = EntryNotFoundException::class)
    fun `#get should throw an EntryNotFoundException when the requested provider does not exist`() {
        emptyGraph.get<Service>()
    }

    @Test
    fun `#get should be able to retrieve dependencies from parent graph`() {
        val graph = component {
            constant("foo")
            provider<ServiceDependency> { ServiceDependencyImpl(get()) }
            subComponent("sub") {
                provider<Service> { ServiceImpl(get()) }
            }
        }.init().initSubComponent("sub")
        assertEquals("foo", graph.get<Service>().dependency.aValue)
    }

    @Test
    fun `#get should be able to retrieve dependencies with generics from parent graph`() {
        val graph = component {
            constant("foo")
            provider<GenericDependency<String>>(generics = true) { GenericDependencyImpl(get()) }
            subComponent("sub") {
                provider<GenericService<String>>(generics = true) {
                    GenericServiceImpl(get(generics = true))
                }
            }
        }.init().initSubComponent("sub")
        assertEquals("foo", graph.get<GenericService<String>>(generics = true).dependency.aValue)
    }

    @Test
    fun `#getOrNull should return instance returned by the provider`() {
        val instance = Any()
        val graph = component { provider { instance } }.init()
        assertSame(instance, graph.getOrNull())
    }

    @Test
    fun `#getOrNull should return instance with generics returned by the provider`() {
        val instance = mapOf(1 to "1")
        val graph = component { provider(generics = true) { instance } }.init()
        assertSame(instance, graph.getOrNull<Map<Int, String>>(generics = true))
    }

    @Test
    fun `#getOrNull should return null when the requested provider does not exist`() {
        assertNull(emptyGraph.getOrNull<Service>())
    }

    @Test(expected = EntryNotFoundException::class)
    fun `#getProvider should throw a EntryNotFoundException if provider does not exist`() {
        emptyGraph.getProvider<Service>()
    }

    @Test
    fun `#getProvider should return a provider function that calls the register provider block when invoked`() {
        val instance = Any()
        val graph = component { provider { instance } }.init()
        val provider = graph.getProvider<Any>()
        assertSame(instance, provider())
    }

    @Test
    fun `provider block should get called every time a value is requested`() {
        val atomicInteger = AtomicInteger(0)
        val graph = component { provider { atomicInteger.getAndIncrement() } }.init()
        (0 until 5).forEach { graph.get<Int>() }
        assertEquals(5, atomicInteger.get())
    }

    @Test
    fun `provider block should have access to graph`() {
        val graph = component {
            constant("foo")
            provider<ServiceDependency> { ServiceDependencyImpl(get()) }
            provider<Service> { ServiceImpl(get()) }
        }.init()

        assertEquals("foo", graph.get<Service>().dependency.aValue)
    }

    @Test
    fun `singleton block should only get called once`() {
        val atomicInteger = AtomicInteger(0)
        val graph = component { provider(scope = singleton) { atomicInteger.getAndIncrement() } }.init()
        (0 until 5).forEach { graph.get<Int>() }
        assertEquals(1, atomicInteger.get())
    }

    @Test
    fun `singleton block should have access to graph`() {
        val graph = component {
            constant("foo")
            provider<ServiceDependency> { ServiceDependencyImpl(get()) }
            provider<Service>(scope = singleton) { ServiceImpl(get()) }
        }.init()

        assertEquals("foo", graph.get<Service>().dependency.aValue)
    }

    @Test
    fun `#factory should return a factory function that calls the registered factory block when invoked`() {
        val graph = component { factory { int: Int -> 4 + int } }.init()
        val factory = graph.getFactory<Int, Int>()
        assertEquals(10, factory(6))
    }

    @Test
    fun `multiton block should only get called once per argument`() {
        val atomicInteger = AtomicInteger(0)
        val graph = component { factory(scope = multiton) { add: Int -> atomicInteger.getAndAdd(add) } }.init()

        (0 until 5).forEach { graph.getFactory<Int, Int>().invoke(4) }
        (0 until 5).forEach { graph.getFactory<Int, Int>().invoke(6) }

        assertEquals(10, atomicInteger.get())
    }

    @Test
    fun `multiton block should have access to graph`() {
        val graph = component {
            constant("Hello %s!")
            factory<String, ServiceDependency> { arg: String -> ServiceDependencyImpl(get<String>().format(arg)) }
            factory<String, Service>(scope = multiton) { name: String ->
                ServiceImpl(getFactory<String, ServiceDependency>().invoke(name))
            }
        }.init()

        val f: (String) -> Service = graph.getFactory()
        assertEquals("Hello Joe!", f("Joe").dependency.aValue)
    }

    @Test(expected = CyclicDependencyException::class)
    fun `graph should detect cyclic dependencies`() {
        component {
            provider {
                get<Any>()
                Any()
            }
        }.init().get<Any>()
    }

}