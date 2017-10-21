package io.jentz.winter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class InjectorTest {

    private val emptyGraph = component { }.init()

    @Test(expected = WinterException::class)
    fun `#instance delegate should throw an error if accessed before the graph is attached`() {
        val o = object : InjectorAwareBase() {
            val property: String by instance()
        }
        o.property
    }

    @Test(expected = EntryNotFoundException::class)
    fun `#instance delegate should throw an error if dependency couldn't be found`() {
        val o = object : InjectorAwareBase() {
            val property: String by instance()
        }
        o.inject(emptyGraph)
    }

    @Test
    fun `#instance delegate should eagerly resolve dependency when graph is attached`() {
        val integer = AtomicInteger(0)
        val graph = component { provider { integer.getAndIncrement() } }.init()
        val o = object : InjectorAwareBase() {
            val property: Int by instance()
        }
        o.inject(graph)
        assertEquals(1, integer.get())
    }

    @Test(expected = WinterException::class)
    fun `#instanceOrNull delegate should throw an error if accessed before the graph is attached`() {
        val o = object : InjectorAwareBase() {
            val property: String? by instanceOrNull()
        }
        o.property
    }

    @Test
    fun `#instanceOrNull delegate should eagerly resolve dependency when graph is attached`() {
        val integer = AtomicInteger(0)
        val graph = component { provider { integer.getAndIncrement() } }.init()
        val o = object : InjectorAwareBase() {
            val property: Int? by instanceOrNull()
        }
        o.inject(graph)
        assertEquals(1, integer.get())
    }

    @Test
    fun `#instanceOrNull should resolve to null if provider doesn't exist`() {
        val o = object : InjectorAwareBase() {
            val property: Any? by instanceOrNull()
        }
        o.inject(emptyGraph)
        assertNull(o.property)
    }

    @Test(expected = WinterException::class)
    fun `#factory delegate should throw an error if accessed before the graph is attached`() {
        val o = object : InjectorAwareBase() {
            val property: (Int) -> String by factory()
        }
        o.property
    }

    @Test(expected = EntryNotFoundException::class)
    fun `#factory delegate should throw an error if dependency couldn't be found`() {
        val o = object : InjectorAwareBase() {
            val property: (Int) -> String by factory()
        }
        o.inject(emptyGraph)
    }

    @Test
    fun `#lazyInstance should resolve dependency on first access`() {
        val integer = AtomicInteger(0)
        val graph = component { provider { integer.getAndIncrement() } }.init()
        val o = object : InjectorAwareBase() {
            val property: Int by lazyInstance()
        }
        o.inject(graph)
        assertEquals(0, integer.get())
        o.property
        assertEquals(1, integer.get())
    }

    @Test
    fun `#lazyInstanceOrNull should resolve to null if provider doesn't exist`() {
        val o = object : InjectorAwareBase() {
            val property: Any? by lazyInstanceOrNull()
        }
        o.inject(emptyGraph)
        assertNull(o.property)
    }

}