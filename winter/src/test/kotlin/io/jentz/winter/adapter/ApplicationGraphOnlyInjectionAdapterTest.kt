package io.jentz.winter.adapter

import io.jentz.winter.WinterApplication
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ApplicationGraphOnlyInjectionAdapterTest {

    private val app = WinterApplication {}

    private val adapter = ApplicationGraphOnlyInjectionAdapter(app)

    @BeforeEach
    fun beforeEach() {
        app.closeIfOpen()
    }

    @Test
    fun `#get should open root graph if not open`() {
        adapter.get(Any()).shouldBe(app.get())
    }

    @Test
    fun `#get should return root graph for any argument`() {
        val graph = app.open()
        repeat(2) { adapter.get(Any()).shouldBeSameInstanceAs(graph) }
    }

    @Test
    fun `#useApplicationGraphOnlyAdapter should register adapter`() {
        app.useApplicationGraphOnlyAdapter()
        app.injectionAdapter.shouldBeInstanceOf<ApplicationGraphOnlyInjectionAdapter>()
    }

}