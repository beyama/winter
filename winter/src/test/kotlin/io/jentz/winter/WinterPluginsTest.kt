package io.jentz.winter

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WinterPluginsTest {

    interface InitializingBean {
        fun afterInitialize()
    }

    class Bean : InitializingBean {
        var didCallAfterInitialize = false
        val list = mutableListOf<Int>()

        override fun afterInitialize() {
            didCallAfterInitialize = true
        }
    }

    val testComponent = component { provider { Bean() } }

    @Before
    fun beforeEach() {
        WinterPlugins.resetPostConstructPlugins()
        WinterPlugins.resetInitializingComponentPlugins()
        WinterPlugins.resetGraphDisposePlugins()
    }

    @Test
    fun `should run post construct plugins`() {
        WinterPlugins.addPostConstructPlugin { _, _, instance -> (instance as? InitializingBean)?.afterInitialize() }
        val bean = testComponent.init().instance<Bean>()
        assertTrue(bean.didCallAfterInitialize)
    }

    @Test
    fun `should run post construct plugins in order of registration`() {
        (1 until 4).forEach { i ->
            WinterPlugins.addPostConstructPlugin { _, _, instance -> (instance as? Bean)?.let { it.list += i } }
        }
        val bean = testComponent.init().instance<Bean>()
        assertEquals(listOf(1, 2, 3), bean.list)
    }

    @Test
    fun `#removePostConstructPlugin should remove a post construct plugin`() {
        val plugin: PostConstructPlugin = { _, _, instance -> (instance as? InitializingBean)?.afterInitialize() }
        val graph = testComponent.init()
        WinterPlugins.addPostConstructPlugin(plugin)
        assertTrue(graph.instance<Bean>().didCallAfterInitialize)
        WinterPlugins.removePostConstructPlugin(plugin)
        assertFalse(graph.instance<Bean>().didCallAfterInitialize)
    }

    @Test
    fun `should run initializing component plugins while initializing a component`() {
        WinterPlugins.addInitializingComponentPlugin { _, builder ->
            builder.provider { "foo" }
        }
        val graph = component {}.init()
        assertEquals("foo", graph.instance<String>())
    }

    @Test
    fun `should run initializing component plugins with parent graph while initializing a subcomponent`() {
        WinterPlugins.addInitializingComponentPlugin { parentGraph, builder ->
            parentGraph?.let { builder.provider { it.instance<String>().toUpperCase() } }
        }
        val graph = component {
            provider { "foo" }
            subcomponent("sub") {}
        }.init().initSubcomponent("sub")
        assertEquals("FOO", graph.instance<String>())
    }

    @Test
    fun `#removeInitializingComponentPlugin should remove the given initializing component plugin`() {
        val plugin: InitializingComponentPlugin = { _, builder -> builder.provider { "foo" } }
        WinterPlugins.addInitializingComponentPlugin(plugin)
        assertEquals("foo", testComponent.init().instance<String>())
        WinterPlugins.removeInitializingComponentPlugin(plugin)
        assertNull(testComponent.init().instanceOrNull<String>())
    }

    @Test
    fun `should run dispose plugins when graph is disposed`() {
        var called = false
        WinterPlugins.addGraphDisposePlugin { called = true }
        testComponent.init().dispose()
        assertTrue(called)
    }

    @Test
    fun `#removeGraphDisposePlugin should remove the given graph dispose plugin`() {
        var called = false
        val plugin: GraphDisposePlugin = { called = true }
        WinterPlugins.addGraphDisposePlugin(plugin)
        WinterPlugins.removeGraphDisposePlugin(plugin)
        testComponent.init().dispose()
        assertFalse(called)
    }

}