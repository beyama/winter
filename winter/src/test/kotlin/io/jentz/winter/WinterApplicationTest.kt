package io.jentz.winter

import com.nhaarman.mockitokotlin2.mock
import io.jentz.winter.plugin.Plugin
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class WinterApplicationTest {

    private lateinit var app: WinterApplication

    @BeforeEach
    fun beforeEach() {
        app = WinterApplication()
    }

    @Test
    fun `component should return empty component by default`() {
        app.component.isEmpty().shouldBeTrue()
    }

    @Test
    fun `#component should configure new component`() {
        app.component("test") { constant("") }
        app.component.qualifier.shouldBe("test")
        app.component.size.shouldBe(1)
    }

    @Test
    fun `#init should create graph from component`() {
        val graph = app.createGraph()
        graph.application.shouldBeSameInstanceAs(app)
        graph.component.shouldBeSameInstanceAs(app.component)
    }

    @Test
    fun `#init with builder block should derive component`() {
        val graph = app.createGraph { constant("") }
        graph.component.size.shouldBe(1)
    }

    @Nested
    inner class PluginMethods {

        private lateinit var plugin: Plugin
        private lateinit var plugin2: Plugin

        @BeforeEach
        fun beforeEach() {
            plugin = mock()
            plugin2 = mock()
        }

        @Test
        fun `plugins should be empty by default`() {
            app.plugins.isEmpty().shouldBeTrue()
        }

        @Test
        fun `#registerPlugin should register plugin`() {
            app.registerPlugin(plugin)
            app.plugins.contains(plugin).shouldBeTrue()
            app.plugins.size.shouldBe(1)
        }

        @Test
        fun `#registerPlugin should only register an instance once`() {
            expectValueToChange(0, 1, { app.plugins.size }) {
                app.registerPlugin(plugin).shouldBeTrue()
                app.registerPlugin(plugin).shouldBeFalse()
            }
        }

        @Test
        fun `#unregisterPlugin should unregister plugin`() {
            app.registerPlugin(plugin)
            app.registerPlugin(plugin2)

            app.unregisterPlugin(plugin)
            app.plugins.contains(plugin).shouldBeFalse()
            app.plugins.contains(plugin2).shouldBeTrue()
        }

    }

}
