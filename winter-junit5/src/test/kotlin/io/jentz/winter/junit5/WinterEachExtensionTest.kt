package io.jentz.winter.junit5

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import io.jentz.winter.WinterApplication
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.RegisterExtension


class WinterEachExtensionTest {

    companion object {

        val app = WinterApplication {
            constant(42)
        }

        @JvmField
        @RegisterExtension
        // static extensions are registered before non static so after each is called the last.
        val testExtension: Extension = AfterEachCallback {
            assertThat(app.plugins).isEmpty()
        }

    }

    @JvmField
    @RegisterExtension
    val winterExtension = WinterEachExtension(app) {}

    @BeforeEach
    fun beforeEach() {
        app.createGraph()
    }

    @Test
    fun `session plugin should be registered`() {
        assertThat(app.plugins.size).isEqualTo(1)
    }

    @Test
    fun `should resolve parameters`(@WInject theAnswer: Int) {
        assertThat(theAnswer).isEqualTo(42)
    }

}
