package io.jentz.winter

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class InjectorTest {

    private val emptyGraph = component {  }.init()

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

}