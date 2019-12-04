package io.jentz.winter.testing

import io.jentz.winter.Graph
import io.jentz.winter.WinterApplication
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.collections.shouldHaveSize
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

    private object TestApp : WinterApplication(block = {
        constant("application")

        subcomponent("sub") {
            constant("sub")

            subcomponent("sub") {
                constant("sub sub")
                prototype { Service(instance(), instance()) }
            }
        }
    })

    @Mock private val dependency1 = Dependency1()

    @Test
    fun `#resolve should resolve dependency from test graph`() {
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

    @Nested
    inner class AutoDispose {

        @Test
        fun `#autoDisposeTestGraph should dispose test graph but not parents on #after`() {
            session {
                testGraph("sub")
                autoDisposeTestGraph()
            }.apply {
                start()
                val graph = createAll("sub")
                val parent = graph.parent!!
                stop()
                graph.isDisposed.shouldBeTrue()
                parent.isDisposed.shouldBeFalse()
            }
        }

        @Test
        fun `#autoDisposeTestGraphAndAncestors should dispose test graph and its ancestors on #after`() {
            session {
                testGraph("sub")
                autoDisposeTestGraphAndAncestors()
            }.apply {
                start()
                val graph = createAll("sub")
                val parent = graph.parent!!
                stop()
                graph.isDisposed.shouldBeTrue()
                parent.isDisposed.shouldBeTrue()
            }
        }

        @Test
        fun `#autoDisposeAllGraphs should dispose all graphs created during test on #after`() {
            session {
                autoDisposeAllGraphs()
            }.apply {
                start()
                repeat(3) { createAll() }
                allGraphs.shouldHaveSize(3)
                allGraphs.none { it.isDisposed }.shouldBeTrue()
                stop()
                allGraphs.all { it.isDisposed }.shouldBeTrue()
            }
        }

        @Test
        fun `should not dispose test graph on #after if not auto dispose is configured`() {
            session {
                testGraph("sub")
            }.apply {
                start()
                val graph = createAll("sub")
                stop()
                graph.isDisposed.shouldBeFalse()
            }
        }

    }

    @Nested
    inner class Builder {

        @Test
        fun `#testGraph should configure the graph to use`() {
            session {
                testGraph("sub")
            }.test {
                createAll("sub", "sub")
                requireGraph.instance<String>().shouldBe("sub sub")
            }
        }

        @Test
        fun `#testGraph with parent matcher should configure the graph to use`() {
            session {
                testGraph("sub", "sub")
            }.test {
                createAll("sub", "sub")
                requireGraph.instance<String>().shouldBe("sub sub")
            }
        }

        @Test
        fun `#extend without arguments should extend application graph`() {
            session {
                extend { prototype(override = true) { "new string" } }
            }.test {
                createAll()
                    .instance<String>().shouldBe("new string")
            }
        }

        @Test
        fun `#extend should extend graph with component qualifier`() {
            session {
                extend("sub") { prototype(override = true) { "new string" } }
                testGraph("sub")
            }.test {
                createAll("sub")
                    .instance<String>().shouldBe("new string")
            }
        }

        @Test
        fun `#extend should extend graph with parent matcher`() {
            session {
                extend("sub", "sub") { prototype(override = true) { "new string" } }
                testGraph("sub", "sub")
            }.test {
                createAll("sub", "sub")
                    .instance<String>().shouldBe("new string")
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

    }

    private fun session(
        vararg instances: Any = arrayOf(this),
        block: WinterTestSession.Builder.() -> Unit
    ): WinterTestSession = WinterTestSession.session(*instances) {
        block()
        application = TestApp
    }

    private fun WinterTestSession.test(block: WinterTestSession.() -> Unit) {
        start()
        block()
        stop()
    }

    private fun createAll(vararg qualifiers: Any): Graph =
        qualifiers.fold(TestApp.createGraph()) { parent, qualifier -> parent.createSubgraph(qualifier) }

}
