package io.jentz.winter.junit5

import io.jentz.winter.*
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.shouldBe
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.extension.RegisterExtension

@TestInstance(PER_CLASS)
class GraphLifecycleExtensionTest {

    private var graphInitializingCalled = 0
    private var graphInitializedCalled = 0
    private var graphDisposeCalled = 0
    private var postConstructCalled = 0

    private val component = component {
        singleton { "" }
    }

    @JvmField
    @RegisterExtension
    val extension = object : GraphLifecycleExtension() {
        override fun graphInitializing(parentGraph: Graph?, builder: ComponentBuilder) {
            graphInitializingCalled += 1
        }

        override fun graphInitialized(graph: Graph) {
            graphInitializedCalled += 1
        }

        override fun graphDispose(graph: Graph) {
            graphDisposeCalled += 1
        }

        override fun postConstruct(graph: Graph, scope: Scope, argument: Any, instance: Any) {
            postConstructCalled += 1
        }
    }

    @BeforeAll
    fun beforeAll() {
        Winter.plugins.unregisterAll()
    }

    @AfterAll
    fun afterAll() {
        Winter.plugins.isEmpty().shouldBeTrue()
    }

    @Test
    fun `should call all lifecycle methods during test`() {
        graphInitializingCalled = 0
        graphInitializedCalled = 0
        postConstructCalled = 0
        graphDisposeCalled = 0

        val graph = component.createGraph()

        graphInitializingCalled.shouldBe(1)
        graphInitializedCalled.shouldBe(1)
        postConstructCalled.shouldBe(0)
        graphDisposeCalled.shouldBe(0)

        graph.instance<String>()

        graphInitializingCalled.shouldBe(1)
        graphInitializedCalled.shouldBe(1)
        postConstructCalled.shouldBe(1)
        graphDisposeCalled.shouldBe(0)

        graph.dispose()

        graphInitializingCalled.shouldBe(1)
        graphInitializedCalled.shouldBe(1)
        postConstructCalled.shouldBe(1)
        graphDisposeCalled.shouldBe(1)
    }

}
