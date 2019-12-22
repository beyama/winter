package io.jentz.winter.junit5

import io.jentz.winter.WinterApplication
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.RegisterExtension


class WinterEachExtensionTest {

    companion object {

        val app = WinterApplication()

        @JvmField
        @RegisterExtension
        // static extensions are registered before non static so after each is called the last.
        val testExtension: Extension = AfterEachCallback {
            app.plugins.isEmpty().shouldBeTrue()
        }

    }

    @JvmField
    @RegisterExtension
    val winterExtension = WinterEachExtension {
        application = app
    }

    @Test
    fun `session plugin should be registered`() {
        app.plugins.size.shouldBe(1)
    }

}
