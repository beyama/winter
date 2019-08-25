package io.jentz.winter.junit5

import io.jentz.winter.component
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import javax.inject.Inject

class ExtendGraphExtensionTest {

    @JvmField
    @RegisterExtension
    val applicationGraphExtension = WinterJUnit5.extension {
        constant("application")
    }

    @JvmField
    @RegisterExtension
    val subGraphExtension = WinterJUnit5.extension("sub") {
        constant("sub")
    }

    private val component = component {
        subcomponent("sub") {}
    }

    @Inject
    private lateinit var injectedValue: String

    @Test
    fun `should extend graph`() {
        val applicationGraph = component.createGraph()
        applicationGraphExtension.inject(this)
        injectedValue.shouldBe("application")

        applicationGraph.createSubgraph("sub")
        subGraphExtension.inject(this)
        injectedValue.shouldBe("sub")
    }

}
