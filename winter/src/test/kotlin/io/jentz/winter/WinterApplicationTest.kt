package io.jentz.winter

import com.nhaarman.mockitokotlin2.*
import io.jentz.winter.WinterApplication.InjectionAdapter
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WinterApplicationTest {

    private val app = WinterApplication {}

    private val adapter: InjectionAdapter = mock()

    @BeforeEach
    fun beforeEach() {
        reset(adapter)
        app.closeGraphIfOpen()
        app.injectionAdapter = adapter
    }

    @Test
    fun `#component should configure new component`() {
        app.component("test") { constant("") }
        app.component.qualifier.shouldBe("test")
        app.component.size.shouldBe(1)
    }

    @Test
    fun `#component should throw exception if application graph is already open`() {
        app.openGraph()
        shouldThrow<WinterException> {
            app.component {}
        }.message.shouldBe("Cannot set component because application graph is already open.")
    }

    @Test
    fun `#graph should throw an exception if application graph is not open`() {
        shouldThrow<WinterException> {
            app.graph
        }.message.shouldBe("Application graph is not open.")
    }

    @Test
    fun `#plugins should be empty by default`() {
        app.plugins.isEmpty().shouldBeTrue()
    }

    @Test
    fun `#injectionAdapter should throw an exception if tree is already open`() {
        app.openGraph()
        shouldThrow<WinterException> {
            app.injectionAdapter = mock(); null
        }.message.shouldBe("Cannot set injection adapter because application graph is already open.")
    }

    @Test
    fun `#openGraph should throw an exception if graph is already open`() {
        app.openGraph()
        shouldThrow<WinterException> {
            app.openGraph()
        }.message.shouldBe("Cannot open application graph because it is already open.")
    }

    @Test
    fun `#openGraph should register close callback so that graph gets reset to null`() {
        app.openGraph()
        app.graph.close()
        app.graphOrNull.shouldBeNull()
    }

    @Test
    fun `#getOrOpenGraph should open application graph if not open`() {
        app.graphOrNull.shouldBeNull()
        app.getOrOpenGraph().shouldBeSameInstanceAs(app.graphOrNull)
        app.graphOrNull.shouldNotBeNull()
    }

    @Test
    fun `#getOrOpenGraph should return application graph if already open`() {
        app.openGraph()
        app.graph.shouldBeSameInstanceAs(app.getOrOpenGraph())
    }

    @Test
    fun `#getOrOpenGraph should register close callback so that graph gets reset to null`() {
        app.getOrOpenGraph()
        app.graph.close()
        app.graphOrNull.shouldBeNull()
    }

    @Test
    fun `#createGraph should create graph from component`() {
        val graph = app.createGraph()
        graph.component.shouldBeSameInstanceAs(app.component)
        graph.application.shouldBeSameInstanceAs(app)
        app.graphOrNull.shouldBeNull()
    }

    @Test
    fun `#closeGraph should throw an exception if graph is not open`() {
        shouldThrow<WinterException> {
            app.closeGraph()
        }.message.shouldBe("Cannot close because noting is open.")
    }

    @Test
    fun `#closeGraph should close application graph`() {
        val graph = app.openGraph()
        app.closeGraph()
        graph.isClosed.shouldBeTrue()
        app.graphOrNull.shouldBeNull()
    }

    @Test
    fun `#closeIfOpen should do nothing if application graph is not open`() {
        app.closeGraphIfOpen()
    }

    @Test
    fun `#closeIfOpen should close application graph if open`() {
        val graph = app.openGraph()
        app.closeGraphIfOpen()
        graph.isClosed.shouldBeTrue()
        app.graphOrNull.shouldBeNull()
    }

    @Test
    fun `#inject with injection target should call graph#inject`() {
        val instance = Any()
        val graph = mock<Graph>()
        whenever(adapter.get(instance)).thenReturn(graph)

        app.inject(instance)
        verify(graph, times(1)).inject(instance)
    }

}
