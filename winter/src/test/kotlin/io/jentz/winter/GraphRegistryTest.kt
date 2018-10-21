package io.jentz.winter

import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GraphRegistryTest {

    private val mvpComponent = component("root") {
        subcomponent("presentation") {
            subcomponent("view") {}
        }
    }

    private val viewPath = arrayOf("presentation", "view")

    @BeforeEach
    fun beforeEach() {
        if (GraphRegistry.has()) GraphRegistry.close()
        GraphRegistry.applicationComponent = mvpComponent
    }

    @Test
    fun `#applicationComponent should dispose existing root graph if new component is set`() {
        openAll(*viewPath)
        val root = GraphRegistry.get()
        expectValueToChange(false, true, root::isDisposed) {
            GraphRegistry.applicationComponent = mvpComponent
        }
    }

    @Test
    fun `#get without arguments should throw an exception if root dependency graph isn't initialized`() {
        shouldThrow<WinterException> {
            GraphRegistry.get()
        }.message.shouldBe("GraphRegistry.get called but there is no open graph.")
    }

    @Test
    fun `#get should throw an exception if dependency graph in path doesn't exist`() {
        openAll("presentation")
        shouldThrow<WinterException> {
            GraphRegistry.get(*viewPath)
        }.message.shouldBe("No graph in path `presentation.view` found.")
    }

    @Test
    fun `#has without arguments should return false if root dependency graph isn't initialized otherwise true`() {
        expectValueToChange(false, true, { GraphRegistry.has() }) {
            GraphRegistry.open()
        }
    }

    @Test
    fun `#has with path should return false if dependency graph in path isn't present otherwise true`() {
        expectValueToChange(false, true, { GraphRegistry.has(*viewPath) }) {
            openAll(*viewPath)
        }
    }

    @Test
    fun `#create without arguments should throw an exception if root component is not set`() {
        GraphRegistry.applicationComponent = null
        shouldThrow<WinterException> {
            GraphRegistry.create()
        }.message.shouldBe("GraphRegistry.create called but there is no application component set.")
    }

    @Test
    fun `#create without path should initialize and return root dependency graph`() {
        GraphRegistry.create().component.qualifier.shouldBe("root")
    }

    @Test
    fun `#create should initialize and return subcomponent by path`() {
        openAll("presentation")
        GraphRegistry.create(*viewPath).component.qualifier.shouldBe("view")
    }

    @Test
    fun `#create without path should pass the builder block to the root component init method`() {
        GraphRegistry.create { constant(42) }.instance<Int>().shouldBe(42)
    }

    @Test
    fun `#create should should pass the builder block to the subcomponent init method`() {
        openAll("presentation")
        GraphRegistry.create(*viewPath) { constant(42) }.instance<Int>().shouldBe(42)
    }

    @Test
    fun `#create should throw an exception when root graph isn't open but a non-empty path is given`() {
        shouldThrow<WinterException> {
            GraphRegistry.create(*viewPath)
        }.message.shouldBe("GraphRegistry.create with path `presentation.view` called but root graph isn't open.")
    }

    @Test
    fun `#create should throw an exception when parent graph isn't open`() {
        GraphRegistry.open()
        shouldThrow<WinterException> {
            GraphRegistry.create("presentation", "view")
        }.message.shouldBe("GraphRegistry.create can't open `presentation.view` because `presentation` is not open.")
    }

    @Test
    fun `#open without arguments should throw an exception if root component is not set`() {
        GraphRegistry.applicationComponent = null
        shouldThrow<WinterException> {
            GraphRegistry.open()
        }.message.shouldBe("GraphRegistry.open called but there is no application component set.")
    }

    @Test
    fun `#open should throw an exception when root graph isn't open but path is given`() {
        shouldThrow<WinterException> {
            GraphRegistry.open(*viewPath)
        }.message.shouldBe("GraphRegistry.open with path `presentation.view` called but root graph isn't initialized.")
    }

    @Test
    fun `#open without arguments should throw an exception if root dependency dependency graph is already initialized`() {
        GraphRegistry.open()
        shouldThrow<WinterException> {
            GraphRegistry.open()
        }.message.shouldBe("GraphRegistry.open can't open root graph because it is already open.")
    }

    @Test
    fun `#open without path but identifier should throw an exception`() {
        shouldThrow<IllegalArgumentException> {
            GraphRegistry.open(identifier = "root2")
        }.message.shouldBe("GraphRegistry.open must not be called with identifier for root graph.")
    }

    @Test
    fun `#open should throw an exception if dependency graph in path already exists`() {
        openAll(*viewPath)
        shouldThrow<WinterException> {
            GraphRegistry.open(*viewPath)
        }.message.shouldBe("GraphRegistry.open can't open 'presentation.view' because it already exists.")
    }

    @Test
    fun `#open with identifier should throw an exception if dependency graph in path already exists`() {
        openAll("presentation")
        GraphRegistry.open(*viewPath, identifier = "foo")
        shouldThrow<WinterException> {
            GraphRegistry.open(*viewPath, identifier = "foo")
        }.message.shouldBe("GraphRegistry.open can't open 'presentation.foo' because it already exists.")
    }

    @Test
    fun `#open should throw an exception when parent graph isn't open`() {
        GraphRegistry.open()
        shouldThrow<WinterException> {
            GraphRegistry.open("presentation", "view")
        }.message.shouldBe("GraphRegistry.open can't open `presentation.view` because `presentation` is not open.")
    }

    @Test
    fun `#open without arguments should initialize and return root dependency graph`() {
        GraphRegistry.open().component.qualifier.shouldBe("root")
    }

    @Test
    fun `#open should initialize and return subcomponent by path`() {
        openAll("presentation")
        GraphRegistry.open(*viewPath).component.qualifier.shouldBe("view")
    }

    @Test
    fun `#open without path should pass the builder block to the root component init method`() {
        GraphRegistry.open { constant(42) }.instance<Int>().shouldBe(42)
    }

    @Test
    fun `#open should should pass the builder block to the subcomponent init method`() {
        openAll("presentation")
        GraphRegistry.open(*viewPath) { constant(42) }.instance<Int>().shouldBe(42)
    }

    @Test
    fun `#open with identifier should initialize subcomponent by path and register it under the given identifier`() {
        openAll("presentation")
        val graph1 = GraphRegistry.open("presentation", "view", identifier = "view0")
        graph1.component.qualifier.shouldBe("view")
        GraphRegistry.has("presentation", "view0")
    }

    @Test
    fun `#close without path should throw an exception if root dependency graph isn't initialized`() {
        shouldThrow<WinterException> {
            GraphRegistry.close()
        }.message.shouldBe("GraphRegistry.close called but no graph is open.")
    }

    @Test
    fun `#close with path should throw an exception if dependency graph in path doesn't exist`() {
        GraphRegistry.open()
        shouldThrow<WinterException> {
            GraphRegistry.close("presentation")
        }.message.shouldBe("GraphRegistry.close can't close `presentation` because it doesn't exist.")
    }

    @Test
    fun `#close without path should close and dispose root dependency graph`() {
        val root = GraphRegistry.open()
        GraphRegistry.close()
        root.isDisposed.shouldBeTrue()
        GraphRegistry.has().shouldBeFalse()
    }

    @Test
    fun `#close with path should close and dispose sub dependency graphs by path`() {
        val graph = openAll(*viewPath)
        GraphRegistry.close(*viewPath)
        graph.isDisposed.shouldBeTrue()
        GraphRegistry.has(*viewPath).shouldBeFalse()
    }

    @Test
    fun `#close without path should dispose child dependency graphs and the root dependency graph itself`() {
        val root = GraphRegistry.open()
        val presentation = GraphRegistry.open("presentation")
        val view = GraphRegistry.open(*viewPath)

        GraphRegistry.close()
        listOf(root, presentation, view).all { it.isDisposed }.shouldBeTrue()
    }

    @Test
    fun `#close with path should dispose child dependency graphs and the dependency graph itself`() {
        val root = GraphRegistry.open()
        val presentation = GraphRegistry.open("presentation")
        val view = GraphRegistry.open(*viewPath)

        GraphRegistry.close("presentation")
        root.isDisposed.shouldBeFalse()
        listOf(presentation, view).all { it.isDisposed }.shouldBeTrue()
    }

    private fun openAll(vararg pathTokens: Any): Graph {
        (-1..pathTokens.lastIndex)
                .map { pathTokens.slice(0..it).toTypedArray() }
                .filterNot { GraphRegistry.has(*it) }
                .forEach { GraphRegistry.open(*it) }
        return GraphRegistry.get(*pathTokens)
    }

}