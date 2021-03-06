package io.jentz.winter.plugin

import com.nhaarman.mockitokotlin2.mock
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.matchers.types.shouldNotBeSameInstanceAs
import io.kotlintest.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class PluginsTest {

    private lateinit var plugin: Plugin
    private lateinit var plugin2: Plugin

    @BeforeEach
    fun beforeEach() {
        plugin = mock()
        plugin2 = mock()
    }

    @Test
    fun `#isNotEmpty should return false if no plugin is registered`() {
        Plugins().isNotEmpty().shouldBeFalse()
    }

    @Test
    fun `#isNotEmpty should return true if a plugin is registered`() {
        Plugins(plugin).isNotEmpty().shouldBeTrue()
    }

    @Test
    fun `#isEmpty should return true if no plugin is register`() {
        Plugins().isEmpty().shouldBeTrue()
    }

    @Test
    fun `#isEmpty should return false if a plugin is register`() {
        Plugins(plugin).isEmpty().shouldBeFalse()
    }

    @Test
    fun `#size should return the number of registered plugins`() {
        Plugins().size.shouldBe(0)
        Plugins(plugin, plugin2).size.shouldBe(2)
    }

    @Test
    fun `#plus operator should return new instance with added plugin`() {
        val empty = Plugins()
        val new = empty + plugin
        new.contains(plugin).shouldBeTrue()
        new.shouldNotBeSameInstanceAs(empty)
        new.size.shouldBe(1)
    }

    @Test
    fun `#plus operator should only create a new instance if given plugin is not contained`() {
        val old = Plugins(plugin)
        val new = old + plugin
        old.shouldBeSameInstanceAs(new)
        old.size.shouldBe(1)
    }

    @Test
    fun `#minus operator should return new instance with given plugin removed`() {
        val old = Plugins(plugin, plugin2)

        old.contains(plugin).shouldBeTrue()
        old.contains(plugin2).shouldBeTrue()

        val new = old - plugin
        new.contains(plugin).shouldBeFalse()
        new.contains(plugin2).shouldBeTrue()
    }

    @Test
    fun `#minus operator should only create new instance if given plugin is contained`() {
        val old = Plugins(plugin)
        val new = old - plugin2
        old.shouldBeSameInstanceAs(new)
        old.size.shouldBe(1)
    }

    @Test
    fun `#contains should return true if plugin is registered otherwise false`() {
        val plugins = Plugins(plugin)
        plugins.contains(plugin).shouldBeTrue()
        plugins.contains(plugin2).shouldBeFalse()
    }

}
