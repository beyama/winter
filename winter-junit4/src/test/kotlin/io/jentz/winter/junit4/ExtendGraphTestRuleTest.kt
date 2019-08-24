package io.jentz.winter.junit4

import io.jentz.winter.component
import io.kotlintest.shouldBe
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import javax.inject.Inject
import org.mockito.Mockito.`when` as whenever

class ExtendGraphTestRuleTest {

    interface Dependency {
        fun getName(): String
    }

    class DependencyImpl : Dependency {
        override fun getName(): String = "Moon"
    }

    class Service(private val dependency: Dependency) {
        fun greeting() = "Hello ${dependency.getName()}!"
    }

    @get:Rule
    val mockitorRule: MockitoRule = MockitoJUnit.rule()

    @get:Rule
    val rule = WinterJunit4.rule {
        bindAllMocks(this@ExtendGraphTestRuleTest)
    }

    @Mock private lateinit var dependency: Dependency

    @Inject private lateinit var service: Service

    private val component = component {
        singleton<Dependency> { DependencyImpl() }
        singleton { Service(instance()) }
    }

    @Test
    fun `should extend test graph`() {
        component.createGraph()

        rule.inject(this)

        whenever(dependency.getName()).thenReturn("World")

        service.greeting().shouldBe("Hello World!")
    }

}
