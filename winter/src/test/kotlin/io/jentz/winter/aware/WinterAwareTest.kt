package io.jentz.winter.aware

import io.jentz.winter.Graph
import io.jentz.winter.graph
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import java.util.*

class WinterAwareTest : WinterAware {

    override val graph: Graph = graph {
        singleton { 42 }
    }
    
    @Test
    fun `#instance should resolve instance`() {
        instance<Int>().shouldBe(42)
    }

    @Test
    fun `#instanceOrNull should resolve instance`() {
        instanceOrNull<Int>().shouldBe(42)
    }

    @Test
    fun `#instanceOrNull should return null if type doesn't exist`() {
        instanceOrNull<Date>().shouldBe(null)
    }

    @Test
    fun `#lazyInstance should return lazy`() {
        lazyInstance<Int>().apply {
            isInitialized().shouldBeFalse()
            value.shouldBe(42)
        }
    }

    @Test
    fun `#lazyInstanceOrNull should return lazy`() {
        lazyInstanceOrNull<Int>().apply {
            isInitialized().shouldBeFalse()
            value.shouldBe(42)
        }
    }

    @Test
    fun `#instanceOrNull should return lazy which returns null if type doesn't exist`() {
        lazyInstanceOrNull<Date>().apply {
            isInitialized().shouldBeFalse()
            value.shouldBe(null)
        }
    }

    @Test
    fun `#provider should return function which resolves instance`() {
        provider<Int>().invoke().shouldBe(42)
    }

    @Test
    fun `#providerOrNull should return function which resolves instance`() {
        providerOrNull<Int>()?.invoke().shouldBe(42)
    }

    @Test
    fun `#providerOrNull should return null if type doesn't exist`() {
        providerOrNull<Date>().shouldBe(null)
    }

    @Test
    fun `#providersOfType should return a set of providers`() {
        providersOfType<Int>().apply {
            shouldBeInstanceOf<Set<*>>()
            size.shouldBe(1)
        }
    }

    @Test
    fun `#instancesOfType should return a set of instances`() {
        instancesOfType<Int>().apply {
            shouldBeInstanceOf<Set<*>>()
            size.shouldBe(1)
            first().shouldBe(42)
        }
    }

}