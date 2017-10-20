package io.jentz.winter

import io.jentz.winter.internal.*
import org.junit.Assert.*
import org.junit.Test

class ComponentTest {

    private val testComponent = component {
        provider { ServiceDependencyImpl("") }
        provider(qualifier = "name") { ServiceDependencyImpl("") }
        provider(generics = true) { GenericDependencyImpl(1) }
        provider(generics = true, qualifier = "name") { GenericDependencyImpl(1) }
        factory { string: String -> ServiceDependencyImpl(string) }
        factory(qualifier = "name") { string: String -> ServiceDependencyImpl(string) }
        factory(generics = true) { int: Int -> GenericDependencyImpl(int) }
        factory(qualifier = "name", generics = true) { int: Int -> GenericDependencyImpl(int) }
    }

    @Test
    fun `Component created with empty block should contain an empty dependency map`() {
        assertTrue(component { }.dependencyMap.isEmpty())
    }

    @Test
    fun `Component configured with provider should contain provider in its dependency map`() {
        val component = component { provider { ServiceDependencyImpl("") } }
        assertTrue(component.dependencyMap[providerId<ServiceDependencyImpl>()] is Provider<*>)
    }

    @Test
    fun `Component configured with provider and qualifier should contain provider in its dependency map`() {
        val component = component { provider("name") { ServiceDependencyImpl("") } }
        assertTrue(component.dependencyMap[providerId<ServiceDependencyImpl>("name")] is Provider<*>)
    }

    @Test
    fun `Component configured with factory should contain factory in its dependency map`() {
        val component = component { factory { arg: String -> ServiceDependencyImpl(arg) } }
        assertTrue(component.dependencyMap[factoryId<String, ServiceDependencyImpl>()] is Factory<*, *>)
    }

    @Test
    fun `Component configured with factory and qualifier should contain factory in its dependency map`() {
        val component = component { factory("name") { arg: String -> ServiceDependencyImpl(arg) } }
        assertTrue(component.dependencyMap[factoryId<String, ServiceDependencyImpl>("name")] is Factory<*, *>)
    }

    @Test
    fun `Component configured with constant should contain constant in its dependency map`() {
        val component = component { constant(ServiceDependencyImpl("")) }
        assertTrue(component.dependencyMap[providerId<ServiceDependencyImpl>()] is Constant<*>)
    }

    @Test
    fun `Component configured with constant and qualifier should contain constant in its dependency map`() {
        val component = component { constant(ServiceDependencyImpl(""), qualifier = "name") }
        assertTrue(component.dependencyMap[providerId<ServiceDependencyImpl>("name")] is Constant<*>)
    }

    @Test(expected = WinterException::class)
    fun `Component configured with same entry should throw an exception`() {
        component {
            constant(ServiceDependencyImpl(""))
            constant(ServiceDependencyImpl(""))
        }
    }

    @Test(expected = WinterException::class)
    fun `ComponentBuilder#removeProvider should throw an exception when provider doesn't exist`() {
        component { removeProvider<ServiceDependency>() }
    }

    @Test
    fun `ComponentBuilder#removeProvider should not throw an exception when provider doesn't exist but silent is true`() {
        component { removeProvider<ServiceDependency>(silent = true) }
    }

    @Test
    fun `ComponentBuilder#removeProvider should remove non-generic provider`() {
        val graph = testComponent.derive { removeProvider<ServiceDependencyImpl>() }.init()
        assertNull(graph.instanceOrNull<ServiceDependencyImpl>())
    }

    @Test
    fun `ComponentBuilder#removeProvider should remove generic provider`() {
        val graph = testComponent.derive { removeProvider<GenericDependencyImpl<Int>>(generics = true) }.init()
        assertNull(graph.instanceOrNull<GenericDependencyImpl<Int>>(generics = true))
    }

    //

    @Test(expected = WinterException::class)
    fun `ComponentBuilder#removeFactory should throw an exception when factory doesn't exist`() {
        component { removeFactory<String, ServiceDependency>() }
    }

    @Test
    fun `ComponentBuilder#removeFactory should not throw an exception when factory doesn't exist but silent is true`() {
        component { removeFactory<String, ServiceDependency>(silent = true) }
    }

    @Test
    fun `ComponentBuilder#removeFactory should remove non-generic factory`() {
        val graph = testComponent.derive { removeFactory<String, ServiceDependencyImpl>() }.init()
        assertNull(graph.factoryOrNull<String, ServiceDependencyImpl>())
    }

    @Test
    fun `ComponentBuilder#removeFactory should remove generic factory`() {
        val graph = testComponent.derive { removeFactory<Int, GenericDependencyImpl<Int>>(generics = true) }.init()
        assertNull(graph.factoryOrNull<Int, GenericDependencyImpl<Int>>(generics = true))
    }

    @Test
    fun `#derive with empty block should create a copy of the component`() {
        val component = component { provider { ServiceDependencyImpl("") } }
        val derived = component.derive { }
        assertNotSame(component, derived)
        assertNotSame(component.dependencyMap, derived.dependencyMap)
        assertEquals(component.dependencyMap, derived.dependencyMap)
    }

    @Test
    fun `#derive with block should add provider to newly created component`() {
        val component = component { provider { ServiceDependencyImpl("") } }
        val derived = component.derive { provider("name") { ServiceDependencyImpl("") } }
        assertEquals(1, component.dependencyMap.size)
        assertEquals(2, derived.dependencyMap.size)
        assertTrue(derived.dependencyMap.containsKey(providerId<ServiceDependencyImpl>("name")))
    }

    @Test(expected = WinterException::class)
    fun `#derive should throw an exception when configured with an entry that already exists in the component derived from`() {
        val component = component { provider { ServiceDependencyImpl("") } }
        component.derive { provider { ServiceDependencyImpl("") } }
    }

    @Test
    fun `#derive should be able to override existing provider entries`() {
        val instanceA = Any()
        val instanceB = Any()
        val component = component { provider { instanceA } }
        val derived = component.derive { provider(override = true) { instanceB } }
        assertEquals(1, derived.dependencyMap.size)
        assertSame(instanceB, derived.init().instance())
    }

}