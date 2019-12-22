package io.jentz.winter.junit5

import io.jentz.winter.WinterApplication
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class WinterAllExtensionStaticTest {

    companion object {

        val app = WinterApplication()

        @JvmField
        @RegisterExtension
        val extension = WinterAllExtension {
            application = app
        }
    }

    @Test
    fun `session plugin should be registered`() {
        app.plugins.size.shouldBe(1)
    }

}
