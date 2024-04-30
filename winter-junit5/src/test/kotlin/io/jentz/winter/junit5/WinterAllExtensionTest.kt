package io.jentz.winter.junit5

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import io.jentz.winter.WinterApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(WinterAllExtensionTest.TestExtension::class)
class WinterAllExtensionTest {

    private object TestApp : WinterApplication()

    class TestExtension : AfterAllCallback {
        override fun afterAll(context: ExtensionContext?) {
            assertThat(TestApp.plugins).isEmpty()
        }
    }

    @JvmField
    @RegisterExtension
    val extension = WinterAllExtension(TestApp) {}

    @Test
    fun `session plugin should be registered`() {
        assertThat(TestApp.plugins.size).isEqualTo(1)
    }

}
