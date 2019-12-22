package io.jentz.winter

import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WinterApplicationTest {

    private val app = WinterApplication("root") {
        subcomponent("presentation") {
            subcomponent("view") {}
        }
    }

    private val viewPath = arrayOf("presentation", "view")

    @BeforeEach
    fun beforeEach() {
        app.closeIfOpen()
    }

    @Test
    fun `#component should configure new component`() {
        app.component("test") { constant("") }
        app.component.qualifier.shouldBe("test")
        app.component.size.shouldBe(1)
    }

    @Test
    fun `#component should throw exception if application graph is already open`() {
        app.open()
        shouldThrow<WinterException> {
            app.component {}
        }.message.shouldBe("Cannot set component because application graph is already open")
    }

    @Test
    fun `#plugins should be empty by default`() {
        app.plugins.isEmpty().shouldBeTrue()
    }

    @Test
    fun `#get without arguments should throw an exception if root dependency graph isn't initialized`() {
        shouldThrow<WinterException> {
            app.get()
        }.message.shouldBe("No object graph opened.")
    }

    @Test
    fun `#get should throw an exception if dependency graph in path doesn't exist`() {
        openAll("presentation")
        shouldThrow<WinterException> {
            app.get(*viewPath)
        }.message.shouldBe("No object graph in path `presentation.view` found.")
    }

    @Test
    fun `#has without arguments should return false if root dependency graph isn't initialized otherwise true`() {
        expectValueToChange(false, true, { app.has() }) {
            app.open()
        }
    }

    @Test
    fun `#has with path should return false if dependency graph in path isn't present otherwise true`() {
        expectValueToChange(false, true, { app.has(*viewPath) }) {
            openAll(*viewPath)
        }
    }

    @Test
    fun `#create without path should initialize and return root dependency graph`() {
        app.create().component.qualifier.shouldBe("root")
    }

    @Test
    fun `#create should initialize and return subcomponent by path`() {
        openAll("presentation")
        app.create(*viewPath).component.qualifier.shouldBe("view")
    }

    @Test
    fun `#create without path should pass the builder block to the root component init method`() {
        app.create { constant(42) }.instance<Int>().shouldBe(42)
    }

    @Test
    fun `#create should should pass the builder block to the subcomponent init method`() {
        openAll("presentation")
        app.create(*viewPath) { constant(42) }.instance<Int>().shouldBe(42)
    }

    @Test
    fun `#create should throw an exception when root graph isn't open but a non-empty path is given`() {
        shouldThrow<WinterException> {
            app.create(*viewPath)
        }.message.shouldBe("Cannot create `presentation.view` because application graph is not open.")
    }

    @Test
    fun `#create should throw an exception when parent graph isn't open`() {
        app.open()
        shouldThrow<WinterException> {
            app.create("presentation", "view")
        }.message.shouldBe("Cannot create `presentation.view` because `presentation` is not open.")
    }

    @Test
    fun `#open should throw an exception when root graph isn't open but path is given`() {
        shouldThrow<WinterException> {
            app.open(*viewPath)
        }.message.shouldBe("Cannot open path `presentation.view` because application graph is not opened.")
    }

    @Test
    fun `#open without arguments should throw an exception if root dependency dependency graph is already initialized`() {
        app.open()
        shouldThrow<WinterException> {
            app.open()
        }.message.shouldBe("Cannot open application graph because it is already open.")
    }

    @Test
    fun `#open without path but identifier should throw an exception`() {
        shouldThrow<IllegalArgumentException> {
            app.open(identifier = "root")
        }.message.shouldBe("Argument `identifier` for application graph is not supported.")
    }

    @Test
    fun `#open should throw an exception if dependency graph in path already exists`() {
        openAll(*viewPath)
        shouldThrow<WinterException> {
            app.open(*viewPath)
        }.run {
            message.shouldBe("Cannot open `presentation.view`.")
            cause.shouldBeInstanceOf<WinterException>()
            cause!!.message.shouldBe("Cannot open subgraph with identifier `view` because it is already open.")
        }
    }

    @Test
    fun `#open with identifier should throw an exception if dependency graph in path already exists`() {
        openAll("presentation")
        app.open(*viewPath, identifier = "foo")
        shouldThrow<WinterException> {
            app.open(*viewPath, identifier = "foo")
        }.run {
            message.shouldBe("Cannot open `presentation.foo`.")
            cause.shouldBeInstanceOf<WinterException>()
            cause!!.message.shouldBe("Cannot open subgraph with identifier `foo` because it is already open.")
        }
    }

    @Test
    fun `#open should throw an exception when parent graph isn't open`() {
        app.open()
        shouldThrow<WinterException> {
            app.open("presentation", "view")
        }.message.shouldBe("Can't open `presentation.view` because `presentation` is not open.")
    }

    @Test
    fun `#open without arguments should initialize and return root dependency graph`() {
        app.open().component.qualifier.shouldBe("root")
    }

    @Test
    fun `#open should initialize and return subcomponent by path`() {
        openAll("presentation")
        app.open(*viewPath).component.qualifier.shouldBe("view")
    }

    @Test
    fun `#open without path should pass the builder block to the root component init method`() {
        app.open { constant(42) }.instance<Int>().shouldBe(42)
    }

    @Test
    fun `#open should should pass the builder block to the subcomponent init method`() {
        openAll("presentation")
        app.open(*viewPath) { constant(42) }.instance<Int>().shouldBe(42)
    }

    @Test
    fun `#open with identifier should initialize subcomponent by path and register it under the given identifier`() {
        openAll("presentation")
        val graph1 = app.open("presentation", "view", identifier = "view0")
        graph1.component.qualifier.shouldBe("view")
        app.has("presentation", "view0")
    }

    @Test
    fun `#close without path should throw an exception if root dependency graph isn't initialized`() {
        shouldThrow<WinterException> {
            app.close()
        }.message.shouldBe("Cannot close because noting is open.")
    }

    @Test
    fun `#close with path should throw an exception if dependency graph in path doesn't exist`() {
        app.open()
        shouldThrow<WinterException> {
            app.close("presentation")
        }.message.shouldBe("Cannot close `presentation` because it doesn't exist.")
    }

    @Test
    fun `#close without path should close and dispose root dependency graph`() {
        val root = app.open()
        app.close()
        root.isDisposed.shouldBeTrue()
        app.has().shouldBeFalse()
    }

    @Test
    fun `#close with path should close and dispose sub dependency graphs by path`() {
        val graph = openAll(*viewPath)
        app.close(*viewPath)
        graph.isDisposed.shouldBeTrue()
        app.has(*viewPath).shouldBeFalse()
    }

    @Test
    fun `#close without path should dispose subgraphs and the root dependency graph itself`() {
        val root = app.open()
        val presentation = app.open("presentation")
        val view = app.open(*viewPath)

        app.close()
        listOf(root, presentation, view).all { it.isDisposed }.shouldBeTrue()
    }

    @Test
    fun `#close with path should dispose subgraphs and the object graph itself`() {
        val root = app.open()
        val presentation = app.open("presentation")
        val view = app.open(*viewPath)

        app.close("presentation")
        root.isDisposed.shouldBeFalse()
        listOf(presentation, view).all { it.isDisposed }.shouldBeTrue()
    }

    @Test
    fun `#closeIfOpen should do nothing if nothing is open in path`() {
        app.closeIfOpen().shouldBeFalse()
        app.closeIfOpen("presentation").shouldBeFalse()
    }

    @Test
    fun `#closeIfOpen should close existing path`() {
        val view = openAll(*viewPath)
        app.closeIfOpen(*viewPath).shouldBeTrue()
        view.isDisposed.shouldBeTrue()

        val root = app.get()
        app.closeIfOpen().shouldBeTrue()
        root.isDisposed.shouldBeTrue()
    }

    private fun openAll(vararg pathTokens: Any): Graph {
        (-1..pathTokens.lastIndex)
            .map { pathTokens.slice(0..it).toTypedArray() }
            .filterNot { app.has(*it) }
            .forEach { app.open(*it) }
        return app.get(*pathTokens)
    }

}
