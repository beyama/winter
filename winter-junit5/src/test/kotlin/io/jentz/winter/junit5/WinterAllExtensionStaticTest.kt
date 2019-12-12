package io.jentz.winter.junit5

import io.jentz.winter.WinterApplication
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class WinterAllExtensionStaticTest {

    object TestApp : WinterApplication()

    companion object {
        @JvmField
        @RegisterExtension
        val extension = WinterAllExtension {
            application = TestApp
        }
    }

    @Test
    fun `session plugin should be registered`() {
        TestApp.plugins.size.shouldBe(1)
    }

}
