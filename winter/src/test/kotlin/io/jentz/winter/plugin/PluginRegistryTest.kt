package io.jentz.winter.plugin

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.only
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import io.jentz.winter.ComponentBuilder
import io.jentz.winter.Scope
import io.jentz.winter.expectValueToChange
import io.jentz.winter.graph
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test


class PluginRegistryTest {

    private val registry = PluginRegistry()

    private lateinit var plugin: Plugin
    private lateinit var plugin2: Plugin

    @BeforeEach
    fun beforeEach() {
        registry.unregisterAll()
        plugin = mock()
        plugin2 = mock()
    }

    @Test
    fun `#isNotEmpty should return false if no plugin is registered otherwise true`() {
        expectValueToChange(false, true, registry::isNotEmpty) {
            registry.register(plugin)
        }
    }

    @Test
    fun `#isEmpty should return true if no plugin is register otherwise false`() {
        expectValueToChange(true, false, registry::isEmpty) {
            registry.register(plugin)
        }
    }

    @Test
    fun `#size should return the number of registered plugins`() {
        registry.size.shouldBe(0)
        registry.register(plugin)
        registry.size.shouldBe(1)
        registry.register(plugin2)
        registry.size.shouldBe(2)
    }

    @Test
    fun `#register should register plugin`() {
        registry.register(plugin)
        registry.contains(plugin).shouldBeTrue()
        registry.size.shouldBe(1)
    }

    @Test
    fun `#register should only register an instance once`() {
        expectValueToChange(0, 1, registry::size) {
            registry.register(plugin).shouldBeTrue()
            registry.register(plugin).shouldBeFalse()
        }
    }

    @Test
    fun `#unregister should unregister plugin`() {
        registry.register(plugin)
        registry.register(plugin2)

        registry.contains(plugin).shouldBeTrue()
        registry.contains(plugin2).shouldBeTrue()

        registry.unregister(plugin)
        registry.contains(plugin).shouldBeFalse()
        registry.contains(plugin2).shouldBeTrue()
    }

    @Test
    fun `#contains should return true if plugin is registered otherwise false`() {
        registry.register(plugin)
        registry.contains(plugin).shouldBeTrue()
        registry.contains(plugin2).shouldBeFalse()
    }

    @Nested
    @DisplayName("run methods")
    inner class RunMethods {

        private val graph = graph { }
        private val argument = Any()
        private val instance = Any()

        @BeforeEach
        fun beforeEach() {
            registry.register(plugin)
            registry.register(plugin2)
        }

        @Test
        fun `#runGraphInitializing should call #graphInitializing on all plugins`() {
            val builder = ComponentBuilder(null)
            registry.runGraphInitializing(graph, builder)
            verify(plugin, only()).graphInitializing(graph, builder)
            verify(plugin2, only()).graphInitializing(graph, builder)
        }

        @Test
        fun `#runGraphInitialized should call #graphInitialized on all plugins`() {
            registry.runGraphInitialized(graph)
            verify(plugin, only()).graphInitialized(graph)
            verify(plugin2, only()).graphInitialized(graph)
        }

        @Test
        fun `#runGraphDispose should call #graphDispose on all plugins`() {
            registry.runGraphDispose(graph)
            verify(plugin, times(1)).graphDispose(graph)
            verify(plugin2, times(1)).graphDispose(graph)
        }

        @Test
        fun `#runPostConstruct should call #postConstruct on all plugins`() {
            registry.runPostConstruct(graph, Scope.Singleton, argument, instance)
            verify(plugin, only()).postConstruct(graph, Scope.Singleton, argument, instance)
            verify(plugin2, only()).postConstruct(graph, Scope.Singleton, argument, instance)
        }

    }

}
