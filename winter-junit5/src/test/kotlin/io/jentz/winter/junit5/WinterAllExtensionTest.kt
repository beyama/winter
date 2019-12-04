package io.jentz.winter.junit5

import io.jentz.winter.WinterApplication
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(WinterAllExtensionTest.TestExtension::class)
class WinterAllExtensionTest {

    object TestApp : WinterApplication()

    class TestExtension : AfterAllCallback {
        override fun afterAll(context: ExtensionContext?) {
            TestApp.plugins.isEmpty().shouldBeTrue()
        }
    }

    @JvmField
    @RegisterExtension
    val extension = WinterAllExtension {
        application = TestApp
    }

    @Test
    fun `session plugin should be registered`() {
        TestApp.plugins.size.shouldBe(1)
    }

}
