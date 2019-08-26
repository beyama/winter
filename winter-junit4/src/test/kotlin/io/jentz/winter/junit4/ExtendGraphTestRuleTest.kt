package io.jentz.winter.junit4

import io.jentz.winter.component
import io.kotlintest.shouldBe
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

class ExtendGraphTestRuleTest {

    @get:Rule
    val applicationGraphRule = WinterJUnit4.rule {
        constant("application")
    }

    @get:Rule
    val subGraphRule = WinterJUnit4.rule("sub") {
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
        applicationGraphRule.inject(this)
        injectedValue.shouldBe("application")

        applicationGraph.createSubgraph("sub")
        subGraphRule.inject(this)
        injectedValue.shouldBe("sub")
    }

}
