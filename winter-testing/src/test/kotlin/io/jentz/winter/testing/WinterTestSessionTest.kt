package io.jentz.winter.testing

import io.jentz.winter.Graph
import io.jentz.winter.Qualifier
import io.jentz.winter.WinterApplication
import io.jentz.winter.delegate.provideDelegate
import io.jentz.winter.qualifier
import io.jentz.winter.typeKey
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

    private val app =  WinterApplication {
        constant("application")

        subcomponent(Sub) {
            constant("sub")

            subcomponent(SubSub) {
                constant("sub sub")
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
                    prototype(typeKey(qualifier("test"))) { "test" }
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
                testGraph(Sub)
                autoCloseTestGraph()
            }.apply {
                start()
                val graph = createAll(Sub)
                val parent = graph.parent!!
                stop()
                graph.isClosed.shouldBeTrue()
                parent.isClosed.shouldBeFalse()
            }
        }

        @Test
        fun `#autoCloseTestGraphAndAncestors should close test graph and its ancestors on #stop`() {
            session {
                testGraph(Sub)
                autoCloseTestGraphAndAncestors()
            }.apply {
                start()
                val graph = createAll(Sub)
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
                testGraph(Sub)
            }.apply {
                start()
                val graph = createAll(Sub)
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
                extend {
                    override {
                        prototype { "new string" }
                    }
                }
            }.test {
                createAll()
                    .instance<String>().shouldBe("new string")
            }
        }

        @Test
        fun `should extend graph with component qualifier`() {
            session {
                extend(Sub) {
                    override {
                        prototype { "new string" }
                    }
                }
                testGraph(Sub)
            }.test {
                createAll(Sub)
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

        private val injector by app
        private val injectedProperty: String by injector.instance()

        @Test
        fun `should configure the graph to use`() {
            session {
                testGraph(Sub)
            }.test {
                createAll(listOf(Sub, SubSub))
                requireTestGraph.component.qualifier.shouldBe(Sub)
            }
        }

        @Test
        fun `should call #inject with test instances on test graph`() {
            session(this) {
                testInjector = injector
                testGraph(Sub)
            }.test {
                createAll(Sub)
                injectedProperty.shouldBe("sub")
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
                bindAllMocks(Sub)
            }.test {
                createAll(Sub).apply {
                    instance<Dependency1>().shouldBeSameInstanceAs(dependency1)
                    instance<Dependency2>().shouldBeSameInstanceAs(dependency2)
                    parent!!.instance<Dependency1?>().shouldBeNull()
                }
            }
        }

    }

    @Nested
    @DisplayName("#onGraphInitialized")
    inner class OnGraphInitialized {

        @Test
        fun `with qualifier should get invoked with graph`() {
            var called = false
            session {
                onGraphInitialized(Sub) { graph ->
                    graph.instance<String>().shouldBe("sub")
                    called = true
                }
            }.test {
                createAll(Sub)
                called.shouldBeTrue()
            }
        }


    }

    @Nested
    @DisplayName("#onGraphClose")
    inner class OnGraphClose {

        @Test
        fun `with qualifier should get invoked with graph`() {
            var called = false
            session {
                onGraphClose(Sub) { graph ->
                    graph.instance<String>().shouldBe("sub")
                    called = true
                }
            }.test {
                createAll(Sub).close()
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

    private fun createAll(): Graph = createAll(emptyList())

    private fun createAll(qualifier: Qualifier): Graph = createAll(listOf(qualifier))

    private fun createAll(qualifiers: List<Qualifier>): Graph =
        qualifiers.fold(app.createGraph()) { parent, qualifier -> parent.createSubgraph(qualifier) }

}
