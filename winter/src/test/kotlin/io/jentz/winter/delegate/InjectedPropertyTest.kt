package io.jentz.winter.delegate

import io.jentz.winter.*
import io.jentz.winter.aware.WinterAware
import io.jentz.winter.aware.inject
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.atomic.AtomicInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InjectedPropertyTest {

    private val app = WinterApplication()

    private val atomicInteger = AtomicInteger(0)

    private val emptyGraph = emptyGraph()

    private val testComponent = component {
        prototype { atomicInteger.incrementAndGet() }
    }

    private val ofTypeTestComponent = component {
        (0..4).forEach { prototype(qualifier = it) { atomicInteger.incrementAndGet() } }
    }

    @BeforeEach
    fun beforeEach() {
        atomicInteger.set(0)
        app.tree.closeIfOpen()
    }

    @Nested
    inner class InjectedProperty {

        @Test
        fun `should register itself on the application notifier`() {
            app.component { constant("a value") }
            app.tree.open()
            AwareTest().property.shouldBe("a value")
        }

    }

    @Nested
    inner class ProviderProperty {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                ProviderProperty<String>(app, typeKey()).value
            }
        }

        @Test
        fun `throws an exception if dependency can't be found`() {
            shouldThrow<EntryNotFoundException> {
                ProviderProperty<String>(app, typeKey()).inject(emptyGraph)
            }
        }

        @Test
        fun `returns a provider block`() {
            val property = ProviderProperty<Int>(app, typeKey())
            property.inject(testComponent.createGraph())
            val provider = property.value
            atomicInteger.get().shouldBe(0)
            provider().shouldBe(1)
        }

    }

    @Nested
    inner class ProviderOrNullProperty {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                ProviderOrNullProperty<String>(app, typeKey()).value
            }
        }

        @Test
        fun `#value should be null if dependency is not found`() {
            ProviderOrNullProperty<String>(app, typeKey()).apply {
                inject(emptyGraph)
                value.shouldBeNull()
            }
        }

        @Test
        fun `returns a provider block that resolves dependency when called`() {
            val property = ProviderOrNullProperty<Int>(app, typeKey())
            property.inject(testComponent.createGraph())
            val provider = property.value
            atomicInteger.get().shouldBe(0)
            provider?.invoke().shouldBe(1)
        }

    }

    @Nested
    inner class InstanceProperty {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                InstanceProperty<String>(app, typeKey()).value
            }
        }

        @Test
        fun `throws an exception if dependency can't be found`() {
            shouldThrow<EntryNotFoundException> {
                InstanceProperty<String>(app, typeKey()).inject(emptyGraph)
            }
        }

        @Test
        fun `should eagerly resolve dependency`() {
            InstanceProperty<Int>(app, typeKey()).inject(testComponent.createGraph())
            atomicInteger.get().shouldBe(1)
        }

    }

    @Nested
    inner class InstanceOrNullProperty {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                InstanceOrNullProperty<String>(app, typeKey()).value
            }
        }

        @Test
        fun `#value should be null if dependency is not found`() {
            InstanceOrNullProperty<String>(app, typeKey()).apply {
                inject(emptyGraph)
                value.shouldBeNull()
            }
        }

        @Test
        fun `should eagerly resolve dependency`() {
            InstanceOrNullProperty<Int>(app, typeKey())
                .inject(testComponent.createGraph())
            atomicInteger.get().shouldBe(1)
        }

    }

    @Nested
    inner class LazyInstanceProperty {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                LazyInstanceProperty<String>(app, typeKey()).value
            }
        }

        @Test
        fun `throws an exception if dependency can't be found`() {
            shouldThrow<EntryNotFoundException> {
                LazyInstanceProperty<String>(app, typeKey()).inject(emptyGraph)
            }
        }

        @Test
        fun `should lazy resolve dependency`() {
            LazyInstanceProperty<Int>(app, typeKey()).apply {
                inject(testComponent.createGraph())
                expectValueToChange(0, 1, atomicInteger::get) {
                    value.shouldBe(1)
                }
            }
        }

    }

    @Nested
    inner class LazyInstanceOrNullProperty {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                LazyInstanceOrNullProperty<String>(app, typeKey()).value
            }
        }

        @Test
        fun `should resolve to null if dependency isn't found`() {
            LazyInstanceOrNullProperty<String>(app, typeKey()).apply {
                inject(emptyGraph)
                value.shouldBeNull()
            }
        }

        @Test
        fun `should lazy resolve dependency`() {
            LazyInstanceOrNullProperty<Int>(app, typeKey()).apply {
                inject(testComponent.createGraph())
                expectValueToChange(0, 1, atomicInteger::get) {
                    value.shouldBe(1)
                }
            }
        }

    }

    @Nested
    inner class ProvidersOfTypeProperty {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                ProvidersOfTypeProperty<String>(app, typeKeyOfType()).value
            }
        }

        @Test
        fun `should return an empty set if no dependency was found`() {
            ProvidersOfTypeProperty<String>(app, typeKeyOfType()).apply {
                inject(emptyGraph)
                value.shouldBeEmpty()
            }
        }

        @Test
        fun `returns a set of provider blocks`() {
            ProvidersOfTypeProperty<Int>(app, typeKeyOfType()).apply {
                inject(ofTypeTestComponent.createGraph())
                value.shouldHaveSize(5)
                expectValueToChange(0, 5, atomicInteger::get) {
                    value.map { it.invoke() }.toSet().shouldBe((1..5).toSet())
                }
            }
        }

    }

    @Nested
    inner class InstancesOfTypeProperty {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                InstancesOfTypeProperty<String>(app, typeKeyOfType()).value
            }
        }

        @Test
        fun `should return an empty set if no dependency was found`() {
            InstancesOfTypeProperty<String>(app, typeKeyOfType()).apply {
                inject(emptyGraph)
                value.shouldBeEmpty()
            }
        }

        @Test
        fun `should eagerly resolve dependencies`() {
            expectValueToChange(0, 5, atomicInteger::get) {
                InstancesOfTypeProperty<Int>(app, typeKeyOfType()).apply {
                    inject(ofTypeTestComponent.createGraph())
                    value.shouldHaveSize(5)
                }
            }
        }

    }

    @Nested
    inner class LazyInstancesOfTypeProperty {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                LazyInstancesOfTypeProperty<String>(app, typeKeyOfType()).value
            }
        }

        @Test
        fun `should return an empty set if no dependency was found`() {
            LazyInstancesOfTypeProperty<String>(app, typeKeyOfType()).apply {
                inject(emptyGraph)
                value.shouldBeEmpty()
            }
        }

        @Test
        fun `should lazy resolve dependencies`() {
            LazyInstancesOfTypeProperty<Int>(app, typeKeyOfType()).apply {
                inject(ofTypeTestComponent.createGraph())
                atomicInteger.get().shouldBe(0)
                value.shouldHaveSize(5)
                atomicInteger.get().shouldBe(5)
            }
        }

    }

    @Nested
    inner class PropertyMapper {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            val property = InstanceProperty<Int>(app, typeKey())

            shouldThrow<UninitializedPropertyAccessException> {
                property.map { it * 2 }.value
            }
        }

        @Test
        fun `#value should apply mapping function to given property value`() {
            InstanceProperty<Int>(app, typeKey())
                .map { it * 3 }
                .apply {
                    inject(testComponent.createGraph())
                    value.shouldBe(3)
                }
        }

    }

    private inner class AwareTest : WinterAware {
        override val winterApplication = app

        val property: String by inject()

        init {
            inject(this)
        }
    }

}