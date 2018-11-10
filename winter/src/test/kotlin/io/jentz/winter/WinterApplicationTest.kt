package io.jentz.winter

import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WinterApplicationTest {

    private lateinit var app: WinterApplication

    @BeforeEach
    fun beforeEach() {
        app = WinterApplication()
    }

    @Test
    fun `component should return empty component by default`() {
        app.component.isEmpty().shouldBeTrue()
    }

    @Test
    fun `plugins should be empty by default`() {
        app.plugins.isEmpty().shouldBeTrue()
    }

    @Test
    fun `#component should configure new component`() {
        app.component("test") { constant("") }
        app.component.qualifier.shouldBe("test")
        app.component.size.shouldBe(1)
    }

    @Test
    fun `#init should create graph from component`() {
        val graph = app.init()
        graph.application.shouldBeSameInstanceAs(app)
        graph.component.shouldBeSameInstanceAs(app.component)
    }

    @Test
    fun `#init with builder block should derive component`() {
        val graph = app.init { constant("") }
        graph.component.size.shouldBe(1)
    }

}
