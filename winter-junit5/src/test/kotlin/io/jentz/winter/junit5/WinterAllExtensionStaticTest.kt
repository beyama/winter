package io.jentz.winter.junit5

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.jentz.winter.WinterApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class WinterAllExtensionStaticTest {

    companion object {

        val app = WinterApplication()

        @JvmField
        @RegisterExtension
        val extension = WinterAllExtension(app) {}
    }

    @Test
    fun `session plugin should be registered`() {
        assertThat(app.plugins.size).isEqualTo(1)
    }

}
