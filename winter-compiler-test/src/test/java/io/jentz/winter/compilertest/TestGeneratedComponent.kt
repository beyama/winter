package io.jentz.winter.compilertest

import io.jentz.winter.Graph
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TestGeneratedComponent {

    private lateinit var graph: Graph

    @Before
    fun setUp() {
        graph = generatedComponent.init()
    }

    @Test
    fun `Test inject constructor with no arguments`() {
        val instance: NoArgumentInjectConstructor = graph.instance()
        assertNotNull(instance)
    }

    @Test
    fun `Test inject constructor with one argument`() {
        val instance: OneArgumentInjectConstructor = graph.instance()
        assertNotNull(instance)
        assertNotNull(instance.arg)
    }

    @Test
    fun `Test inject constructor with named argument`() {
        val message = "Hey Joe!"
        graph = generatedComponent.init {
            constant("a string")
            constant(message, qualifier = "message")
        }
        val instance: NamedArgumentInjectConstructor = graph.instance()
        assertNotNull(instance)
        assertEquals(message, instance.message)
    }

    @Test
    fun `Test inject constructor with five arguments`() {
        val instance: FiveArgumentsInjectConstructor = graph.instance()
        assertNotNull(instance)
        assertNotNull(instance.arg0)
        assertNotNull(instance.arg1)
        assertNotNull(instance.arg2)
        assertNotNull(instance.arg3)
        assertNotNull(instance.arg4)
    }

    @Test
    fun `Test no argument inject constructor with injected fields`() {
        val instance: NoArgumentInjectConstructorWithInjectedFields = graph.instance()
        assertNotNull(instance)
        assertNotNull(instance.field0)
        assertNotNull(instance.field1)
    }

    @Test
    fun `Test no argument inject constructor with setter injection`() {
        val instance: NoArgumentInjectConstructorWithSetterInjection = graph.instance()
        assertNotNull(instance)
        assertNotNull(instance.field0)
        assertNotNull(instance.field1)
    }

    @Test
    fun `Test singleton with no argument inject constructor and field injection`() {
        assertSame(
                graph.instance<SingletonWithInjectConstructorAndInjectedFields>(),
                graph.instance<SingletonWithInjectConstructorAndInjectedFields>())
    }

    @Test
    fun `Test inner class with inject constructor and field injection`() {
        val instance = graph.instance<OuterClass.InnerClassWithInjectConstructorAndInjectedFields>()
        assertNotNull(instance)
        assertNotNull(instance.field0)
        assertNotNull(instance.field1)
    }

    @Test
    fun `Test kotlin class with inject constructor and field, method and setter injection`() {
        val instance = graph.instance<KotlinClassWithInjectConstructorAndFieldAndSetterInjection>()
        assertNotNull(instance)
        assertNotNull(instance.dep0)
        assertNotNull(instance.dep1)
        assertNotNull(instance.dep2)
        assertNotNull(instance._dep3    )
    }

    @Test
    fun `Test custom scoped class with no argument inject constructor`() {
        val subgraph = graph.initSubcomponent(CustomScope::class)
        assertSame(
                subgraph.instance<CustomScopedWithNoArgumentInjectConstructor>(),
                subgraph.instance<CustomScopedWithNoArgumentInjectConstructor>()
        )
    }

}