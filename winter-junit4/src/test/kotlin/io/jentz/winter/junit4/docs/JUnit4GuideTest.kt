package io.jentz.winter.junit4.docs

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.jentz.winter.ApplicationScope
import io.jentz.winter.Winter
import io.jentz.winter.junit4.WinterRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/*
 * The JUnit 4 sample of the guide (doc/index.adoc).
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

// tag::junit4[]
class GreeterTest {

    @get:Rule
    val winterRule = WinterRule {
        // replace the Api in the application graph
        extend(ApplicationScope) {
            override {
                singleton<Api> { FakeApi() }
            }
        }
    }

    @Before
    fun setUp() {
        Winter.component {
            singleton<Api> { RealApi() }
            prototype { Greeter(instance()) }
        }
        Winter.openGraph()
    }

    @After
    fun tearDown() {
        Winter.closeGraph()
    }

    @Test
    fun greets() {
        val greeter: Greeter = Winter.graph.instance()
        assertThat(greeter.greet()).isEqualTo("Hello from the test")
    }

}
// end::junit4[]
