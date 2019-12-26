package io.jentz.winter

import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TreeTest {

    private val app = WinterApplication("root") {
        subcomponent("presentation") {
            subcomponent("view") {}
        }
    }

    private val tree = app.tree

    private val viewPath = arrayOf("presentation", "view")

    @BeforeEach
    fun beforeEach() {
        tree.closeIfOpen()
    }


    @Test
    fun `#get without arguments should throw an exception if root dependency graph isn't initialized`() {
        shouldThrow<WinterException> {
            tree.get()
        }.message.shouldBe("No object graph opened.")
    }

    @Test
    fun `#get should throw an exception if dependency graph in path doesn't exist`() {
        openAll("presentation")
        shouldThrow<WinterException> {
            tree.get(*viewPath)
        }.message.shouldBe("No object graph in path `presentation.view` found.")
    }

    @Test
    fun `#has without arguments should return false if root dependency graph isn't initialized otherwise true`() {
        expectValueToChange(false, true, { tree.isOpen() }) {
            tree.open()
        }
    }

    @Test
    fun `#has with path should return false if dependency graph in path isn't present otherwise true`() {
        expectValueToChange(false, true, { tree.isOpen(*viewPath) }) {
            openAll(*viewPath)
        }
    }

    @Test
    fun `#create without path should initialize and return root dependency graph`() {
        tree.create().component.qualifier.shouldBe("root")
    }

    @Test
    fun `#create should initialize and return subcomponent by path`() {
        openAll("presentation")
        tree.create(*viewPath).component.qualifier.shouldBe("view")
    }

    @Test
    fun `#create without path should pass the builder block to the root component init method`() {
        tree.create { constant(42) }.instance<Int>().shouldBe(42)
    }

    @Test
    fun `#create should should pass the builder block to the subcomponent init method`() {
        openAll("presentation")
        tree.create(*viewPath) { constant(42) }.instance<Int>().shouldBe(42)
    }

    @Test
    fun `#create should throw an exception when root graph isn't open but a non-empty path is given`() {
        shouldThrow<WinterException> {
            tree.create(*viewPath)
        }.message.shouldBe("Cannot create `presentation.view` because application graph is not open.")
    }

    @Test
    fun `#create should throw an exception when parent graph isn't open`() {
        tree.open()
        shouldThrow<WinterException> {
            tree.create("presentation", "view")
        }.message.shouldBe("Cannot create `presentation.view` because `presentation` is not open.")
    }

    @Test
    fun `#open should throw an exception when root graph isn't open but path is given`() {
        shouldThrow<WinterException> {
            tree.open(*viewPath)
        }.message.shouldBe("Cannot open path `presentation.view` because application graph is not opened.")
    }

    @Test
    fun `#open without arguments should throw an exception if application dependency graph is already initialized`() {
        tree.open()
        shouldThrow<WinterException> {
            tree.open()
        }.message.shouldBe("Cannot open application graph because it is already open.")
    }

    @Test
    fun `#open without path but identifier should throw an exception`() {
        shouldThrow<IllegalArgumentException> {
            tree.open(identifier = "root")
        }.message.shouldBe("Argument `identifier` for application graph is not supported.")
    }

    @Test
    fun `#open should throw an exception if dependency graph in path already exists`() {
        openAll(*viewPath)
        shouldThrow<WinterException> {
            tree.open(*viewPath)
        }.run {
            message.shouldBe("Cannot open `presentation.view`.")
            cause.shouldBeInstanceOf<WinterException>()
            cause!!.message.shouldBe("Cannot open subgraph with identifier `view` because it is already open.")
        }
    }

    @Test
    fun `#open with identifier should throw an exception if dependency graph in path already exists`() {
        openAll("presentation")
        tree.open(*viewPath, identifier = "foo")
        shouldThrow<WinterException> {
            tree.open(*viewPath, identifier = "foo")
        }.run {
            message.shouldBe("Cannot open `presentation.foo`.")
            cause.shouldBeInstanceOf<WinterException>()
            cause!!.message.shouldBe("Cannot open subgraph with identifier `foo` because it is already open.")
        }
    }

    @Test
    fun `#open should throw an exception when parent graph isn't open`() {
        tree.open()
        shouldThrow<WinterException> {
            tree.open("presentation", "view")
        }.message.shouldBe("Can't open `presentation.view` because `presentation` is not open.")
    }

    @Test
    fun `#open without arguments should initialize and return root dependency graph`() {
        tree.open().component.qualifier.shouldBe("root")
    }

    @Test
    fun `#open should initialize and return subcomponent by path`() {
        openAll("presentation")
        tree.open(*viewPath).component.qualifier.shouldBe("view")
    }

    @Test
    fun `#open without path should pass the builder block to the root component init method`() {
        tree.open { constant(42) }.instance<Int>().shouldBe(42)
    }

    @Test
    fun `#open should should pass the builder block to the subcomponent init method`() {
        openAll("presentation")
        tree.open(*viewPath) { constant(42) }.instance<Int>().shouldBe(42)
    }

    @Test
    fun `#open with identifier should initialize subcomponent by path and register it under the given identifier`() {
        openAll("presentation")
        val graph1 = tree.open("presentation", "view", identifier = "view0")
        graph1.component.qualifier.shouldBe("view")
        tree.isOpen("presentation", "view0").shouldBeTrue()
    }

    @Test
    fun `#getOrOpen should throw an exception when application graph isn't open but path is given`() {
        shouldThrow<WinterException> {
            tree.getOrOpen(*viewPath)
        }.message.shouldBe("Cannot open path `presentation.view` because application graph is not opened.")
    }

    @Test
    fun `#getOrOpen without arguments should return application graph if it is already open`() {
        tree.open().shouldBeSameInstanceAs(tree.getOrOpen())
    }

    @Test
    fun `#getOrOpen without path but identifier should throw an exception if application graph is not open`() {
        shouldThrow<IllegalArgumentException> {
            tree.getOrOpen(identifier = "root")
        }.message.shouldBe("Argument `identifier` for application graph is not supported.")
    }

    @Test
    fun `#getOrOpen should return graph in path when it is already open`() {
        openAll(*viewPath).shouldBeSameInstanceAs(tree.getOrOpen(*viewPath))
    }

    @Test
    fun `#getOrOpen with identifier should return graph in path when it is already open`() {
        openAll("presentation")
        tree.open(*viewPath, identifier = "foo").shouldBeSameInstanceAs(tree.getOrOpen(*viewPath, identifier = "foo"))
    }

    @Test
    fun `#getOrOpen should throw an exception when parent graph isn't open`() {
        tree.open()
        shouldThrow<WinterException> {
            tree.getOrOpen("presentation", "view")
        }.message.shouldBe("Can't open `presentation.view` because `presentation` is not open.")
    }

    @Test
    fun `#getOrOpen without arguments should open application dependency graph`() {
        tree.getOrOpen().component.qualifier.shouldBe("root")
    }

    @Test
    fun `#getOrOpen should open subgraph by path`() {
        openAll("presentation")
        tree.getOrOpen(*viewPath).component.qualifier.shouldBe("view")
    }

    @Test
    fun `#getOrOpen without path should extend application component with given block`() {
        tree.getOrOpen { constant(42) }.instance<Int>().shouldBe(42)
    }

    @Test
    fun `#getOrOpen with path should extend subcomponent with given block`() {
        openAll("presentation")
        tree.getOrOpen(*viewPath) { constant(42) }.instance<Int>().shouldBe(42)
    }

    @Test
    fun `#getOrOpen with identifier should open subgraph by path and register it under the given identifier`() {
        openAll("presentation")
        val graph1 = tree.getOrOpen("presentation", "view", identifier = "view0")
        graph1.component.qualifier.shouldBe("view")
        tree.isOpen("presentation", "view0").shouldBeTrue()
    }

    @Test
    fun `#close without path should throw an exception if root dependency graph isn't initialized`() {
        shouldThrow<WinterException> {
            tree.close()
        }.message.shouldBe("Cannot close because noting is open.")
    }

    @Test
    fun `#close with path should throw an exception if dependency graph in path doesn't exist`() {
        tree.open()
        shouldThrow<WinterException> {
            tree.close("presentation")
        }.message.shouldBe("Cannot close `presentation` because it doesn't exist.")
    }

    @Test
    fun `#close without path should close and dispose root dependency graph`() {
        val root = tree.open()
        tree.close()
        root.isDisposed.shouldBeTrue()
        tree.isOpen().shouldBeFalse()
    }

    @Test
    fun `#close with path should close and dispose sub dependency graphs by path`() {
        val graph = openAll(*viewPath)
        tree.close(*viewPath)
        graph.isDisposed.shouldBeTrue()
        tree.isOpen(*viewPath).shouldBeFalse()
    }

    @Test
    fun `#close without path should dispose subgraphs and the root dependency graph itself`() {
        val root = tree.open()
        val presentation = tree.open("presentation")
        val view = tree.open(*viewPath)

        tree.close()
        listOf(root, presentation, view).all { it.isDisposed }.shouldBeTrue()
    }

    @Test
    fun `#close with path should dispose subgraphs and the object graph itself`() {
        val root = tree.open()
        val presentation = tree.open("presentation")
        val view = tree.open(*viewPath)

        tree.close("presentation")
        root.isDisposed.shouldBeFalse()
        listOf(presentation, view).all { it.isDisposed }.shouldBeTrue()
    }

    @Test
    fun `#closeIfOpen should do nothing if nothing is open in path`() {
        tree.closeIfOpen().shouldBeFalse()
        tree.closeIfOpen("presentation").shouldBeFalse()
    }

    @Test
    fun `#closeIfOpen should close existing path`() {
        val view = openAll(*viewPath)
        tree.closeIfOpen(*viewPath).shouldBeTrue()
        view.isDisposed.shouldBeTrue()

        val root = tree.get()
        tree.closeIfOpen().shouldBeTrue()
        root.isDisposed.shouldBeTrue()
    }

    @Test
    fun `disposing the application graph should have the same effect as calling #close without path`() {
        val graph = tree.open()
        graph.dispose()
        tree.isOpen().shouldBeFalse()
    }

    private fun openAll(vararg pathTokens: Any): Graph {
        (-1..pathTokens.lastIndex)
            .map { pathTokens.slice(0..it).toTypedArray() }
            .filterNot { tree.isOpen(*it) }
            .forEach { tree.open(*it) }
        return tree.get(*pathTokens)
    }

}
