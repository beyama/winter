package io.jentz.winter.junit5

import io.jentz.winter.WinterApplication
import io.kotlintest.matchers.boolean.shouldBeTrue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class GraphLifecycleExtensionTest {

    object TestApp : WinterApplication()

    companion object {

        @AfterAll
        @JvmStatic
        fun afterAll() {
            TestApp.plugins.isEmpty().shouldBeTrue()
        }

    }

    @JvmField
    @RegisterExtension
    val extension = object : GraphLifecycleExtension(TestApp) {}

    @Test
    fun `should register itself as plugin during test`() {
        TestApp.plugins.contains(extension).shouldBeTrue()
    }

}
