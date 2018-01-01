package io.jentz.winter

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GraphRegistryTest {

    private val mvpComponent = component {
        constant("root")
        subcomponent("presentation") {
            constant("presentation")
            subcomponent("view") {
                constant("view")
            }
        }
    }

    private val viewPath = arrayOf("presentation", "view")

    @Before
    fun beforeEach() {
        if (GraphRegistry.has()) GraphRegistry.close()
        WinterPlugins.resetGraphDisposePlugins()
        GraphRegistry.applicationComponent = mvpComponent
    }

    @Test(expected = WinterException::class)
    fun `#get without arguments should throw an exception if root dependency graph isn't initialized`() {
        GraphRegistry.get()
    }

    @Test(expected = EntryNotFoundException::class)
    fun `#get should throw an exception if dependency graph in path doesn't exist`() {
        openAll("presentation")
        GraphRegistry.get(*viewPath)
    }

    @Test
    fun `#has without arguments should return false if root dependency graph isn't initialized otherwise true`() {
        assertFalse(GraphRegistry.has())
        GraphRegistry.open()
        assertTrue(GraphRegistry.has())
    }

    @Test
    fun `#has with path should return false if dependency graph in path isn't present otherwise true`() {
        assertFalse(GraphRegistry.has(*viewPath))
        openAll(*viewPath)
        assertTrue(GraphRegistry.has(*viewPath))
    }

    @Test(expected = WinterException::class)
    fun `#open without arguments should throw an exception if root component is not set`() {
        GraphRegistry.applicationComponent = null
        GraphRegistry.open()
    }

    @Test(expected = WinterException::class)
    fun `#open without arguments should throw an exception if root dependency dependency graph is already initialized`() {
        GraphRegistry.open()
        GraphRegistry.open()
    }

    @Test(expected = WinterException::class)
    fun `#open without path but identifier should throw an exception`() {
        GraphRegistry.open(identifier = "root2")
    }

    @Test(expected = WinterException::class)
    fun `#open should throw an exception if dependency graph in path already exists`() {
        openAll(*viewPath)
        GraphRegistry.open(*viewPath)
    }

    @Test
    fun `#open without arguments should initialize and return root dependency graph`() {
        assertEquals("root", GraphRegistry.open().instance<String>())
    }

    @Test
    fun `#open should initialize and return subcomponent by path`() {
        openAll("presentation")
        assertEquals("view", GraphRegistry.open(*viewPath).instance<String>())
    }

    @Test
    fun `#open without arguments should pass the builder block to the root component init method`() {
        assertEquals(42, GraphRegistry.open { constant(42) }.instance<Int>())
    }

    @Test
    fun `#open should should pass the builder block to the subcomponent init method`() {
        openAll("presentation")
        assertEquals(42, GraphRegistry.open(*viewPath) { constant(42) }.instance<Int>())
    }

    @Test
    fun `#open with identifier should initialize subcomponent by path and register it under the given identifier`() {
        openAll("presentation")
        val graph0 = GraphRegistry.open("presentation", "view")
        val graph1 = GraphRegistry.open("presentation", "view", identifier = "view0") {
            constant("view0", override = true)
        }
        assertEquals("view", graph0.instance<String>())
        assertEquals("view0", graph1.instance<String>())
    }

    @Test(expected = WinterException::class)
    fun `#close without path should throw an exception if root dependency graph isn't initialized`() {
        GraphRegistry.close()
    }

    @Test(expected = WinterException::class)
    fun `#close with path should throw an exception if dependency graph in path doesn't exist`() {
        GraphRegistry.open()
        GraphRegistry.close("presentation")
    }

    @Test
    fun `#close without path should close root dependency graph`() {
        GraphRegistry.open()
        GraphRegistry.close()
        assertFalse(GraphRegistry.has())
    }

    @Test
    fun `#close with path should close sub dependency graphs by path`() {
        openAll(*viewPath)
        GraphRegistry.close(*viewPath)
        assertFalse(GraphRegistry.has(*viewPath))
    }

    @Test
    fun `#close without path should dispose child dependency graphs and the root dependency graph itself`() {
        val disposed = mutableListOf<Graph>()
        WinterPlugins.addGraphDisposePlugin { graph -> disposed += graph }

        val root = GraphRegistry.open()
        val presentation = GraphRegistry.open("presentation")
        val view = GraphRegistry.open(*viewPath)

        GraphRegistry.close()
        assertEquals(listOf(view, presentation, root), disposed)
    }

    @Test
    fun `#close with path should dispose child dependency graphs and the dependency graph itself`() {
        val disposed = mutableListOf<Graph>()
        WinterPlugins.addGraphDisposePlugin { graph -> disposed += graph }

        GraphRegistry.open()
        val presentation = GraphRegistry.open("presentation")
        val view = GraphRegistry.open(*viewPath)

        GraphRegistry.close("presentation")
        assertEquals(listOf(view, presentation), disposed)
    }

    private fun openAll(vararg pathTokens: Any): Graph {
        (-1..pathTokens.lastIndex)
                .map { pathTokens.slice(0..it).toTypedArray() }
                .filterNot { GraphRegistry.has(*it) }
                .forEach { GraphRegistry.open(*it) }
        return GraphRegistry.get(*pathTokens)
    }

}