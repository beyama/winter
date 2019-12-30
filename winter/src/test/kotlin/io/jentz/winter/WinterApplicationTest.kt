package io.jentz.winter

import com.nhaarman.mockitokotlin2.*
import io.jentz.winter.WinterApplication.InjectionAdapter
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class WinterApplicationTest {

    private val app = WinterApplication {}

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

    @Nested
    inner class TreeMethods {
        
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
        fun `#get without arguments should return root graph`() {
            app.open().shouldBe(app.get())
        }

        @Test
        fun `#getOrNull without arguments should null if root graph is not open`() {
            app.getOrNull().shouldBeNull()
        }

        @Test
        fun `#getOrNull without arguments should root graph if open`() {
            app.open().shouldBe(app.getOrNull())
        }

        @Test
        fun `#has without arguments should return false if root dependency graph isn't initialized otherwise true`() {
            expectValueToChange(false, true, { app.isOpen() }) {
                app.open()
            }
        }

        @Test
        fun `#has with path should return false if dependency graph in path isn't present otherwise true`() {
            expectValueToChange(false, true, { app.isOpen(*viewPath) }) {
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
        fun `#open without arguments should throw an exception if application dependency graph is already initialized`() {
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
            app.isOpen("presentation", "view0").shouldBeTrue()
        }

        @Test
        fun `#getOrOpen should throw an exception when application graph isn't open but path is given`() {
            shouldThrow<WinterException> {
                app.getOrOpen(*viewPath)
            }.message.shouldBe("Cannot open path `presentation.view` because application graph is not opened.")
        }

        @Test
        fun `#getOrOpen without arguments should return application graph if it is already open`() {
            app.open().shouldBeSameInstanceAs(app.getOrOpen())
        }

        @Test
        fun `#getOrOpen without path but identifier should throw an exception if application graph is not open`() {
            shouldThrow<IllegalArgumentException> {
                app.getOrOpen(identifier = "root")
            }.message.shouldBe("Argument `identifier` for application graph is not supported.")
        }

        @Test
        fun `#getOrOpen should return graph in path when it is already open`() {
            openAll(*viewPath).shouldBeSameInstanceAs(app.getOrOpen(*viewPath))
        }

        @Test
        fun `#getOrOpen with identifier should return graph in path when it is already open`() {
            openAll("presentation")
            app.open(*viewPath, identifier = "foo").shouldBeSameInstanceAs(app.getOrOpen(*viewPath, identifier = "foo"))
        }

        @Test
        fun `#getOrOpen should throw an exception when parent graph isn't open`() {
            app.open()
            shouldThrow<WinterException> {
                app.getOrOpen("presentation", "view")
            }.message.shouldBe("Can't open `presentation.view` because `presentation` is not open.")
        }

        @Test
        fun `#getOrOpen without arguments should open application dependency graph`() {
            app.getOrOpen().component.qualifier.shouldBe("root")
        }

        @Test
        fun `#getOrOpen should open subgraph by path`() {
            openAll("presentation")
            app.getOrOpen(*viewPath).component.qualifier.shouldBe("view")
        }

        @Test
        fun `#getOrOpen without path should extend application component with given block`() {
            app.getOrOpen { constant(42) }.instance<Int>().shouldBe(42)
        }

        @Test
        fun `#getOrOpen with path should extend subcomponent with given block`() {
            openAll("presentation")
            app.getOrOpen(*viewPath) { constant(42) }.instance<Int>().shouldBe(42)
        }

        @Test
        fun `#getOrOpen with identifier should open subgraph by path and register it under the given identifier`() {
            openAll("presentation")
            val graph1 = app.getOrOpen("presentation", "view", identifier = "view0")
            graph1.component.qualifier.shouldBe("view")
            app.isOpen("presentation", "view0").shouldBeTrue()
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
        fun `#close without path should close the root dependency graph`() {
            val root = app.open()
            app.close()
            root.isClosed.shouldBeTrue()
            app.isOpen().shouldBeFalse()
        }

        @Test
        fun `#close with path should close sub dependency graphs by path`() {
            val graph = openAll(*viewPath)
            app.close(*viewPath)
            graph.isClosed.shouldBeTrue()
            app.isOpen(*viewPath).shouldBeFalse()
        }

        @Test
        fun `#close without path should close subgraphs and the root dependency graph itself`() {
            val root = app.open()
            val presentation = app.open("presentation")
            val view = app.open(*viewPath)

            app.close()
            listOf(root, presentation, view).all { it.isClosed }.shouldBeTrue()
        }

        @Test
        fun `#close with path should close subgraphs and the object graph itself`() {
            val root = app.open()
            val presentation = app.open("presentation")
            val view = app.open(*viewPath)

            app.close("presentation")
            root.isClosed.shouldBeFalse()
            listOf(presentation, view).all { it.isClosed }.shouldBeTrue()
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
            view.isClosed.shouldBeTrue()

            val root = app.get()
            app.closeIfOpen().shouldBeTrue()
            root.isClosed.shouldBeTrue()
        }

        @Test
        fun `disposing the application graph should have the same effect as calling #close without path`() {
            val graph = app.open()
            graph.close()
            app.isOpen().shouldBeFalse()
        }

        private fun openAll(vararg pathTokens: Any): Graph {
            (-1..pathTokens.lastIndex)
                .map { pathTokens.slice(0..it).toTypedArray() }
                .filterNot { app.isOpen(*it) }
                .forEach { app.open(*it) }
            return app.get(*pathTokens)
        }
    }

    @Nested
    inner class AdapterBasedMethods {

        private val adapter: InjectionAdapter = mock()

        private val instance = Any()

        @BeforeEach
        fun beforeEach() {
            reset(adapter)
            app.injectionAdapter = adapter
        }

        @Test
        fun `#injectionAdapter should throw an exception if tree is already open`() {
            app.open()
            shouldThrow<WinterException> {
                app.injectionAdapter = mock(); null
            }.message.shouldBe("Cannot set injection adapter because application graph is already open")
        }

        @Test
        fun `#inject with injection target should call graph#inject`() {
            val graph = mock<Graph>()
            whenever(adapter.get(instance)).thenReturn(graph)

            app.inject(instance)
            verify(graph, times(1)).inject(instance)
        }

    }

}
