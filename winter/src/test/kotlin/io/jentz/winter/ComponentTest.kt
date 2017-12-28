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
        assertTrue(component.dependencyMap[typeKey<ServiceDependencyImpl>()] is ProviderEntry<*>)
    }

    @Test
    fun `Component configured with provider and qualifier should contain provider in its dependency map`() {
        val component = component { provider("name") { ServiceDependencyImpl("") } }
        assertTrue(component.dependencyMap[typeKey<ServiceDependencyImpl>("name")] is ProviderEntry<*>)
    }

    @Test
    fun `Component configured with factory should contain factory in its dependency map`() {
        val component = component { factory { arg: String -> ServiceDependencyImpl(arg) } }
        assertTrue(component.dependencyMap[compoundTypeKey<String, ServiceDependencyImpl>()] is FactoryEntry<*, *>)
    }

    @Test
    fun `Component configured with factory and qualifier should contain factory in its dependency map`() {
        val component = component { factory("name") { arg: String -> ServiceDependencyImpl(arg) } }
        assertTrue(component.dependencyMap[compoundTypeKey<String, ServiceDependencyImpl>("name")] is FactoryEntry<*, *>)
    }

    @Test
    fun `Component configured with constant should contain constant in its dependency map`() {
        val component = component { constant(ServiceDependencyImpl("")) }
        assertTrue(component.dependencyMap[typeKey<ServiceDependencyImpl>()] is ConstantEntry<*>)
    }

    @Test
    fun `Component configured with constant and qualifier should contain constant in its dependency map`() {
        val component = component { constant(ServiceDependencyImpl(""), qualifier = "name") }
        assertTrue(component.dependencyMap[typeKey<ServiceDependencyImpl>("name")] is ConstantEntry<*>)
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
        assertTrue(derived.dependencyMap.containsKey(typeKey<ServiceDependencyImpl>("name")))
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

    @Test(expected = EntryNotFoundException::class)
    fun `#subcomponent should throw an exception if entry doesn't exist`() {
        component {}.subcomponent("a")
    }

    @Test
    fun `#subcomponent with one qualifier should return the corresponding subcomponent`() {
        val c = component {
            subcomponent("s1") {}
            subcomponent("s2") { constant(42) }
        }
        assertEquals(42, c.subcomponent("s2").constantValue(typeKey<Int>()))
    }

    @Test
    fun `#subcomponent with multiple qualifiers should return the corresponding nested subcomponent`() {
        val c = component {
            subcomponent("1") {
                subcomponent("1.1") {
                    subcomponent("1.1.1") {
                        constant(42)
                    }
                }
            }
        }
        assertEquals(42, c.subcomponent("1", "1.1", "1.1.1").constantValue(typeKey<Int>()))
    }

    @Test
    fun `ComponentBuilder#include with subcomponent include mode 'DoNotInclude' should not include subcomponents`() {
        val c1 = component { subcomponent("sub") { constant("a", qualifier = "a") } }
        val c2 = component { include(c1, subcomponentIncludeMode = ComponentBuilder.SubcomponentIncludeMode.DoNotInclude) }

        assertTrue(c2.dependencyMap.isEmpty())
    }

    @Test
    fun `ComponentBuilder#include with subcomponent include mode 'DoNotIncludeIfAlreadyPresent' should not touch existing subcomponents`() {
        val c1 = component { subcomponent("sub") { constant("a", qualifier = "a") } }
        val c2 = component { subcomponent("sub") { constant("b", qualifier = "b") } }
        val c3 = c1.derive { include(c2, false, ComponentBuilder.SubcomponentIncludeMode.DoNotIncludeIfAlreadyPresent) }

        assertFalse(c3.subcomponent("sub").has(typeKey<String>("b")))
        assertEquals(1, c3.subcomponent("sub").size)
    }

    @Test
    fun `ComponentBuilder#include with subcomponent include mode 'Replace' should replace existing subcomponents`() {
        val c1 = component { subcomponent("sub") { constant("a", qualifier = "a") } }
        val c2 = component { subcomponent("sub") { constant("b", qualifier = "b") } }
        val c3 = c1.derive { include(c2, false, ComponentBuilder.SubcomponentIncludeMode.Replace) }

        assertFalse(c3.subcomponent("sub").has(typeKey<String>("a")))
        assertEquals(1, c3.subcomponent("sub").size)
    }

    @Test
    fun `ComponentBuilder#include with subcomponent include mode 'Merge' should merge existing subcomponents`() {
        val c1 = component { subcomponent("sub") { constant("a", qualifier = "a") } }
        val c2 = component { subcomponent("sub") { constant("b", qualifier = "b") } }
        val c3 = c1.derive { include(c2, false, ComponentBuilder.SubcomponentIncludeMode.Merge) }

        assertTrue(c3.subcomponent("sub").has(typeKey<String>("a")))
        assertTrue(c3.subcomponent("sub").has(typeKey<String>("b")))
        assertEquals(2, c3.subcomponent("sub").size)
    }

    @Test
    fun `ComponentBuilder#include with subcomponent include mode 'Merge' should override existing provider`() {
        val c1 = component { subcomponent("sub") { constant("a", qualifier = "a") } }
        val c2 = component { subcomponent("sub") { constant("b", qualifier = "a") } }
        val c3 = c1.derive { include(c2, false, ComponentBuilder.SubcomponentIncludeMode.Merge) }

        assertEquals(1, c3.subcomponent("sub").size)
        assertEquals("b", c3.subcomponent("sub").constantValue(typeKey<String>("a")))
    }

    @Test
    fun `ComponentBuilder#subcomponent should register a subcomponent`() {
        val component = component { subcomponent("sub") { } }
        assertNotNull(component.dependencyMap.get(Component::class.java, "sub"))
    }

    @Test
    fun `ComponentBuilder#subcomponent should extend existing subcomponent when deriveExisting is true`() {
        val base = component { subcomponent("sub") { constant("a", "a") } }
        val derived = base.derive { subcomponent("sub", deriveExisting = true) { constant("b", "b") } }
        val sub = derived.subcomponent("sub")

        assertNotNull(sub.dependencyMap.get(String::class.java, "a"))
        assertNotNull(sub.dependencyMap.get(String::class.java, "b"))
    }

    @Test(expected = WinterException::class)
    fun `ComponentBuilder#subcomponent should throw exception when deriveExisting is true but subcomponent doesn't exist`() {
        component { subcomponent("sub", deriveExisting = true) {} }
    }

    @Test(expected = WinterException::class)
    fun `ComponentBuilder#subcomponent should throw exception when override is true but subcomponent doesn't exist`() {
        component { subcomponent("sub", override = true) {} }
    }

    @Test(expected = WinterException::class)
    fun `ComponentBuilder#subcomponent should throw exception when deriveExisting and override is true`() {
        val base = component { subcomponent("sub") {} }
        base.derive { subcomponent("sub", deriveExisting = true, override = true) {} }
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
        val key = typeKey<ServiceDependencyImpl>()
        assertTrue(testComponent.has(key))
        val component = testComponent.derive { removeProvider<ServiceDependencyImpl>() }
        assertFalse(component.has(key))
    }

    @Test
    fun `ComponentBuilder#removeProvider should remove generic provider`() {
        val key = genericTypeKey<GenericDependencyImpl<Int>>()
        assertTrue(testComponent.has(key))
        val component = testComponent.derive { removeProvider<GenericDependencyImpl<Int>>(generics = true) }
        assertFalse(component.has(key))
    }

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
        val key = compoundTypeKey<String, ServiceDependencyImpl>()
        assertTrue(testComponent.has(key))
        val component = testComponent.derive { removeFactory<String, ServiceDependencyImpl>() }
        assertFalse(component.has(key))
    }

    @Test
    fun `ComponentBuilder#removeFactory should remove generic factory`() {
        val key = genericCompoundTypeKey<Int, GenericDependencyImpl<Int>>()
        assertTrue(testComponent.has(key))
        val component = testComponent.derive { removeFactory<Int, GenericDependencyImpl<Int>>(generics = true) }
        assertFalse(component.has(key))
    }

    private fun Component.has(key: DependencyKey) = dependencyMap.containsKey(key)

    private val Component.size get() = dependencyMap.size

    private fun Component.constant(key: DependencyKey) = dependencyMap[key] as ConstantEntry<*>

    private fun Component.constantValue(key: DependencyKey) = constant(key).value

}