package io.jentz.winter.adapter

import io.jentz.winter.*
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ApplicationGraphOnlyAdapterTest {

    private val app = WinterApplication {}

    private val adapter = ApplicationGraphOnlyAdapter(app)

    private val injection = WinterInjection()

    @BeforeEach
    fun beforeEach() {
        app.closeIfOpen()
    }

    @Test
    fun `#getGraph should throw an exception if no root graph is open`() {
        shouldThrow<WinterException> { adapter.getGraph(Any()) }
    }

    @Test
    fun `#getGraph should return root graph for any argument`() {
        val graph = app.open()
        repeat(2) { adapter.getGraph(Any()).shouldBeSameInstanceAs(graph) }
    }

    @Test
    fun `#createGraph should open root graph`() {
        adapter.createGraph(Any(), null)
        app.has().shouldBeTrue()
    }

    @Test
    fun `#createGraph should apply builder block`() {
        adapter.createGraph(Any()) { constant("") }
        app.get().component.shouldContainService(typeKey<String>())
    }

    @Test
    fun `#createGraph should throw an exception if root graph is already open`() {
        app.open()
        shouldThrow<WinterException> { adapter.createGraph(Any(), null) }
    }

    @Test
    fun `#disposeGraph should dispose root graph`() {
        app.open()
        expectValueToChange(true, false, { app.has() }) {
            adapter.disposeGraph(Any())
        }
    }

    @Test
    fun `#disposeGraph should throw an excpetion if root graph is not open`() {
        shouldThrow<WinterException> { adapter.disposeGraph(Any()) }
    }

    @Test
    fun `#useApplicationGraphOnlyAdapter with tree should register adapter with given tree`() {
        injection.useApplicationGraphOnlyAdapter(app)
        injection.adapter.shouldBeInstanceOf<ApplicationGraphOnlyAdapter>()
        app.open()
        injection.adapter.getGraph(Any()).shouldBeSameInstanceAs(app.get())
    }

    @Test
    fun `#useApplicationGraphOnlyAdapter with application should register adapter with application`() {
        injection.useApplicationGraphOnlyAdapter(app)
        injection.adapter.shouldBeInstanceOf<ApplicationGraphOnlyAdapter>()
        injection.adapter.createGraph(Any(), null)
            .component.shouldBeSameInstanceAs(app.component)
    }

}