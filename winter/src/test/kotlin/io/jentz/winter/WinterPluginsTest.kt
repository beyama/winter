package io.jentz.winter

import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


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

    private val testComponent = component { prototype { Bean() } }

    @BeforeEach
    fun beforeEach() {
        WinterPlugins.resetPostConstructPlugins()
        WinterPlugins.resetInitializingComponentPlugins()
        WinterPlugins.resetGraphDisposePlugins()
    }

    @Test
    fun `should run post construct plugins`() {
        WinterPlugins.addPostConstructPlugin { _, _, _, instance -> (instance as? InitializingBean)?.afterInitialize() }
        testComponent.init().instance<Bean>().didCallAfterInitialize.shouldBeTrue()
    }

    @Test
    fun `should run post construct plugins in order of registration`() {
        (1..3).forEach { i ->
            WinterPlugins.addPostConstructPlugin { _, _, _, instance -> (instance as? Bean)?.let { it.list += i } }
        }
        testComponent.init().instance<Bean>().list.shouldContainExactly(1, 2, 3)
    }

    @Test
    fun `#removePostConstructPlugin should remove a post construct plugin`() {
        val plugin: PostConstructPlugin = { _, _, _, instance -> (instance as? InitializingBean)?.afterInitialize() }
        val graph = testComponent.init()
        WinterPlugins.addPostConstructPlugin(plugin)
        graph.instance<Bean>().didCallAfterInitialize.shouldBeTrue()
        WinterPlugins.removePostConstructPlugin(plugin)
        graph.instance<Bean>().didCallAfterInitialize.shouldBeFalse()
    }

    @Test
    fun `should run initializing component plugins while initializing a component`() {
        WinterPlugins.addInitializingComponentPlugin { _, builder ->
            builder.prototype { "" }
        }
        graph {}.shouldContainService(typeKey<String>())
    }

    @Test
    fun `should run initializing component plugins with parent graph while initializing a subcomponent`() {
        WinterPlugins.addInitializingComponentPlugin { parentGraph, builder ->
            parentGraph?.let { builder.prototype { it.instance<String>().toUpperCase() } }
        }
        graph("parent") {
            prototype { "foo" }
            subcomponent("sub") {}
        }.initSubcomponent("sub").instance<String>().shouldBe("FOO")
    }

    @Test
    fun `#removeInitializingComponentPlugin should remove the given initializing component plugin`() {
        val plugin: InitializingComponentPlugin = { _, builder -> builder.prototype { "" } }
        WinterPlugins.addInitializingComponentPlugin(plugin)
        testComponent.init().shouldContainService(typeKey<String>())
        WinterPlugins.removeInitializingComponentPlugin(plugin)
        testComponent.init().shouldNotContainService(typeKey<String>())
    }

    @Test
    fun `should run dispose plugins when graph is disposed`() {
        var called = false
        WinterPlugins.addGraphDisposePlugin { called = true }
        testComponent.init().dispose()
        called.shouldBeTrue()
    }

    @Test
    fun `#removeGraphDisposePlugin should remove the given graph dispose plugin`() {
        var called = false
        val plugin: GraphDisposePlugin = { called = true }
        WinterPlugins.addGraphDisposePlugin(plugin)
        WinterPlugins.removeGraphDisposePlugin(plugin)
        testComponent.init().dispose()
        called.shouldBeFalse()
    }

    @Test
    fun `#resetAll should remove all plugins`() {
        WinterPlugins.addInitializingComponentPlugin { _, _ -> throw Error() }
        WinterPlugins.addPostConstructPlugin { _, _, _, _ -> throw Error() }
        WinterPlugins.addGraphDisposePlugin { _ -> throw Error() }
        WinterPlugins.resetAll()
        graph { prototype { Any() } }.apply {
            instance<Any>()
            dispose()
        }
    }

}