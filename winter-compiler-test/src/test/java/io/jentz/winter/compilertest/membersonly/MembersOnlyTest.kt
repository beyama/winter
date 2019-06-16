package io.jentz.winter.compilertest.membersonly

import io.jentz.winter.Graph
import io.jentz.winter.compilertest.generatedComponent
import org.junit.Assert
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class MembersOnlyTest {

    private lateinit var graph: Graph

    @Before
    fun setUp() {
        graph = generatedComponent.init()
    }

    @Test
    fun `Test members injector only`() {
        val i = graph.inject(OnlyInjectedMembers())
        assertNotNull(i.field0)
        assertNotNull(i.field1)
    }

    @Test
    fun `Test extended members injector only`() {
        val i = graph.inject(OnlyInjectedMembersExtended(), injectSuperClasses = true)
        assertNotNull(i.field0)
        assertNotNull(i.field1)
        assertNotNull(i.field2)
    }

    @Test
    fun `Test injected provider`() {
        graph = generatedComponent.init {
            constant("constructor", qualifier = "constructor")
            constant("setter", qualifier = "setter")
            constant("field", qualifier = "field")
        }
        val instance: ProviderInjection = graph.instance()
        Assert.assertEquals("constructor", instance.namedConstructorInjected.get())
        Assert.assertEquals("setter", instance.namedSetterInjected.get())
        Assert.assertEquals("field", instance.namedFieldInjected.get())
    }

    @Test
    fun `Test lazy injection`() {
        val atomic = AtomicInteger(0)
        val graph = generatedComponent.init { prototype { atomic.incrementAndGet() } }
        val instance: LazyInjection = graph.instance()
        Assert.assertEquals(1, instance.field.value)
        Assert.assertEquals(1, instance.field.value)
    }

}