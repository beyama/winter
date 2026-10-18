package io.jentz.winter.junit5.docs

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.jentz.winter.ApplicationScope
import io.jentz.winter.Winter
import io.jentz.winter.junit5.WInject
import io.jentz.winter.junit5.WinterEachExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/*
 * The JUnit 5 sample of the guide (doc/index.adoc).
 *
 * The guide includes the regions between the `tag::name[]` and `end::name[]` comments.
 */

interface Api {
    fun greeting(): String
}

class RealApi : Api {
    override fun greeting() = "Hello"
}

class FakeApi : Api {
    override fun greeting() = "Hello from the test"
}

class Greeter(private val api: Api) {
    fun greet() = api.greeting()
}

// tag::junit5[]
class GreeterTest {

    @JvmField
    @RegisterExtension
    val winterExtension = WinterEachExtension {
        // replace the Api in the application graph
        extend(ApplicationScope) {
            override {
                singleton<Api> { FakeApi() }
            }
        }
    }

    @BeforeEach
    fun setUp() {
        Winter.component {
            singleton<Api> { RealApi() }
            prototype { Greeter(instance()) }
        }
        Winter.openGraph()
    }

    @AfterEach
    fun tearDown() {
        Winter.closeGraph()
    }

    // parameters annotated with @WInject are resolved from the latest opened graph
    @Test
    fun greets(@WInject greeter: Greeter) {
        assertThat(greeter.greet()).isEqualTo("Hello from the test")
    }

}
// end::junit5[]
