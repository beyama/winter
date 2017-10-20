package io.jentz.winter

import io.jentz.winter.internal.*
import org.junit.Assert.*
import org.junit.Test

class ComponentTest {

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
        assertSame(instanceB, derived.init().get())
    }

}