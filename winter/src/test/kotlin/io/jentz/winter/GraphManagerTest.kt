package io.jentz.winter

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GraphManagerTest {

    val mvpComponent = component {
        subcomponent("presentation") {
            subcomponent("view") { provider { true } }
        }
    }

    val viewPath = arrayOf("presentation", "view")

    @Before
    fun beforeEach() {
        GraphManager.reset()
        WinterPlugins.resetGraphDisposePlugins()
    }

    @Test(expected = WinterException::class)
    fun `#open should throw an exception if root component isn't set`() {
        GraphManager.open()
    }

    @Test
    fun `#open should initialize and return root-component the first time it is called without arguments`() {
        GraphManager.rootComponent = component { singleton { true } }
        assertTrue(GraphManager.open().instance())
    }

    @Test
    fun `#open should return the same instance of the root-component when called multiple times`() {
        GraphManager.rootComponent = component {}
        assertSame(GraphManager.open(), GraphManager.open())
    }

    @Test(expected = WinterException::class)
    fun `#open should throw an exception if a subcomponent doesn't exist`() {
        GraphManager.rootComponent = component {}
        GraphManager.open("presentation")
    }

    @Test(expected = WinterException::class)
    fun `#open should throw an exception if a sub-subcomponent doesn't exist`() {
        GraphManager.rootComponent = component {}
        GraphManager.open(*viewPath)
    }

    @Test
    fun `#open should initialize and return subcomponent by path`() {
        GraphManager.rootComponent = mvpComponent
        assertTrue(GraphManager.open(*viewPath).instance())
    }

    @Test
    fun `#open should return the same instance of a subcomponent when called multiple times`() {
        GraphManager.rootComponent = mvpComponent
        assertSame(
                GraphManager.open(*viewPath),
                GraphManager.open(*viewPath))
    }

    @Test(expected = WinterException::class)
    fun `#close without arguments should throw exception if root-component isn't initialized`() {
        GraphManager.close()
    }

    @Test(expected = WinterException::class)
    fun `#close with path should throw exception if subcomponent in path isn't initialized`() {
        GraphManager.rootComponent = mvpComponent
        GraphManager.close("presentation")
    }

    @Test
    fun `#close with path should close subcomponent by path`() {
        GraphManager.rootComponent = mvpComponent
        val graph = GraphManager.open(*viewPath)
        GraphManager.close(*viewPath)
        assertNotSame(graph, GraphManager.open(*viewPath))
    }

    @Test
    fun `#close should dispose child-nodes and the node itself`() {
        val disposed = mutableListOf<Graph>()
        WinterPlugins.addGraphDisposePlugin { graph -> disposed += graph }
        GraphManager.rootComponent = mvpComponent
        val root = GraphManager.open()
        val presentation = GraphManager.open("presentation")
        val view = GraphManager.open(*viewPath)
        GraphManager.close()
        assertEquals(listOf(view, presentation, root), disposed)
    }

    @Test
    fun `#root setter should allow to set the root-component instance`() {
        val graph = mvpComponent.init()
        GraphManager.root = graph
        assertSame(graph, GraphManager.open())
    }

    @Test
    fun `#root getter should return the instance of the root-component`() {
        GraphManager.rootComponent = mvpComponent
        assertSame(GraphManager.open(), GraphManager.root)
    }

}