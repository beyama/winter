package io.jentz.winter.testing

import io.jentz.winter.Graph
import io.jentz.winter.WinterApplication
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class WinterTestSessionTest {

    class Dependency1
    class Dependency2
    class Service(val dependency1: Dependency1, val dependency2: Dependency2)

    private val app =  WinterApplication {
        constant("application")

        subcomponent("sub") {
            constant("sub")

            subcomponent("sub") {
                constant("sub sub")
                prototype { Service(instance(), instance()) }
            }
        }
    }

    @Mock private val dependency1 = Dependency1()

    @Nested
    @DisplayName("#resolve")
    inner class Resolve {

        @Test
        fun `should resolve dependency from test graph`() {
            session {
                extend {
                    prototype("test") { "test" }
                }
            }.test {
                createAll()
                resolve(String::class.java, null).shouldBe("application")
                resolve(String::class.java, "test").shouldBe("test")
            }
        }

    }

    @Nested
    inner class AutoClose {

        @Test
        fun `#autoCloseTestGraph should close test graph but not parents on #stop`() {
            session {
                testGraph("sub")
                autoCloseTestGraph()
            }.apply {
                start()
                val graph = createAll("sub")
                val parent = graph.parent!!
                stop()
                graph.isClosed.shouldBeTrue()
                parent.isClosed.shouldBeFalse()
            }
        }

        @Test
        fun `#autoCloseTestGraphAndAncestors should close test graph and its ancestors on #stop`() {
            session {
                testGraph("sub")
                autoCloseTestGraphAndAncestors()
            }.apply {
                start()
                val graph = createAll("sub")
                val parent = graph.parent!!
                stop()
                graph.isClosed.shouldBeTrue()
                parent.isClosed.shouldBeTrue()
            }
        }

        @Test
        fun `#autoCloseAllGraphs should close all graphs created during test on #stop`() {
            session {
                autoCloseAllGraphs()
            }.apply {
                start()
                val graphs = (0 until 3).map { createAll() }
                graphs.none { it.isClosed }.shouldBeTrue()
                stop()
                graphs.all { it.isClosed }.shouldBeTrue()
            }
        }

        @Test
        fun `should not close test graph on #stop if no auto close is configured`() {
            session {
                testGraph("sub")
            }.apply {
                start()
                val graph = createAll("sub")
                stop()
                graph.isClosed.shouldBeFalse()
            }
        }

    }

    @Nested
    @DisplayName("#extend")
    inner class Extend {

        @Test
        fun `without arguments should extend application graph`() {
            session {
                extend { prototype(override = true) { "new string" } }
            }.test {
                createAll()
                    .instance<String>().shouldBe("new string")
            }
        }

        @Test
        fun `should extend graph with component qualifier`() {
            session {
                extend("sub") { prototype(override = true) { "new string" } }
                testGraph("sub")
            }.test {
                createAll("sub")
                    .instance<String>().shouldBe("new string")
            }
        }

        @Test
        fun `should extend graph with parent matcher`() {
            session {
                extend("sub", "sub") { prototype(override = true) { "new string" } }
                testGraph("sub", "sub")
            }.test {
                createAll("sub", "sub")
                    .instance<String>().shouldBe("new string")
            }
        }

    }

    @Nested
    @DisplayName("#allGraphs")
    inner class AllGraphs {

        @Test
        fun `should contain all graphs created during test`() {
            session {}.test {
                val graphs = (0 until 3).map { createAll() }
                allGraphs.shouldBe(graphs)
            }
        }

        @Test
        fun `should not contain graphs that got closed during test`() {
            session {}.test {
                val graphs = (0 until 3).map { createAll() }
                graphs.first().close()
                allGraphs.shouldBe(graphs.subList(1, graphs.size))
            }
        }

    }

    @Nested
    @DisplayName("#testGraph")
    inner class TestGraph {

        @Test
        fun `should configure the graph to use`() {
            session {
                testGraph("sub")
            }.test {
                createAll("sub", "sub")
                requireTestGraph.instance<String>().shouldBe("sub sub")
            }
        }

        @Test
        fun `with parent matcher should configure the graph to use`() {
            session {
                testGraph("sub", "sub")
            }.test {
                createAll("sub", "sub")
                requireTestGraph.instance<String>().shouldBe("sub sub")
            }
        }

    }

    @Nested
    @DisplayName("#bindAllMocks")
    inner class BindAllMocks {

        @Mock val dependency2 = Dependency2()

        @Test
        fun `without arguments should bind all mocks from all test classes to application graph`() {
            session(this@WinterTestSessionTest, this) {
                bindAllMocks()
            }.test {
                createAll().apply {
                    instance<Dependency1>().shouldBeSameInstanceAs(dependency1)
                    instance<Dependency2>().shouldBeSameInstanceAs(dependency2)
                }
            }
        }

        @Test
        fun `with qualifier should bind all mocks on graph with component qualifier`() {
            session(this@WinterTestSessionTest, this) {
                bindAllMocks("sub")
            }.test {
                createAll("sub").apply {
                    instance<Dependency1>().shouldBeSameInstanceAs(dependency1)
                    instance<Dependency2>().shouldBeSameInstanceAs(dependency2)
                    parent!!.instanceOrNull<Dependency1>().shouldBeNull()
                }
            }
        }

        @Test
        fun `with parent matcher should bind all mocks on graph that matches`() {
            session(this@WinterTestSessionTest, this) {
                bindAllMocks("sub", "sub")
            }.test {
                createAll("sub", "sub").apply {
                    instance<Dependency1>().shouldBeSameInstanceAs(dependency1)
                    instance<Dependency2>().shouldBeSameInstanceAs(dependency2)
                    parent!!.instanceOrNull<Dependency1>().shouldBeNull()
                }
            }
        }

    }

    @Nested
    @DisplayName("#onGraphInitialized")
    inner class OnGraphInitialized {

        @Test
        fun `with parent matcher should get invoked with graph`() {
            var called = false
            session {
                onGraphInitialized("sub", "sub") { graph ->
                    graph.instance<String>().shouldBe("sub sub")
                    called = true
                }
            }.test {
                createAll("sub", "sub")
                called.shouldBeTrue()
            }
        }

        @Test
        fun `with qualifier should get invoked with graph`() {
            var called = false
            session {
                onGraphInitialized("sub") { graph ->
                    graph.instance<String>().shouldBe("sub")
                    called = true
                }
            }.test {
                createAll("sub")
                called.shouldBeTrue()
            }
        }


    }

    @Nested
    @DisplayName("#onGraphClose")
    inner class OnGraphClose {

        @Test
        fun `with parent matcher should get invoked with graph`() {
            var called = false
            session {
                onGraphClose("sub", "sub") { graph ->
                    graph.instance<String>().shouldBe("sub sub")
                    called = true
                }
            }.test {
                createAll("sub", "sub").close()
                called.shouldBeTrue()
            }
        }

        @Test
        fun `with qualifier should get invoked with graph`() {
            var called = false
            session {
                onGraphClose("sub") { graph ->
                    graph.instance<String>().shouldBe("sub")
                    called = true
                }
            }.test {
                createAll("sub").close()
                called.shouldBeTrue()
            }
        }


    }

    private fun session(
        vararg instances: Any = arrayOf(this),
        block: WinterTestSession.Builder.() -> Unit
    ): WinterTestSession = WinterTestSession.session(*instances) {
        block()
        application = app
    }

    private fun WinterTestSession.test(block: WinterTestSession.() -> Unit) {
        start()
        block()
        stop()
    }

    private fun createAll(vararg qualifiers: Any): Graph =
        qualifiers.fold(app.createGraph()) { parent, qualifier -> parent.createSubgraph(qualifier) }

}
