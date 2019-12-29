package io.jentz.winter.delegate

import io.jentz.winter.*
import io.jentz.winter.adapter.useApplicationGraphOnlyAdapter
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.shouldBeInstanceOf
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
        app.useApplicationGraphOnlyAdapter()
    }

    @Nested
    inner class DelegateMethods {

        @Test
        fun `should return ProviderProperty for #injectProvider`() {
            injectProvider<String>().shouldBeInstanceOf<io.jentz.winter.delegate.ProviderProperty<*>>()
        }

        @Test
        fun `should return ProviderOrNullProperty for #injectProviderOrNull`() {
            injectProviderOrNull<String>()
                .shouldBeInstanceOf<io.jentz.winter.delegate.ProviderOrNullProperty<*>>()
        }

        @Test
        fun `should return InstanceProperty for #inject`() {
            inject<String>().shouldBeInstanceOf<io.jentz.winter.delegate.InstanceProperty<*>>()
        }

        @Test
        fun `should return InstanceOrNullProperty for #injectOrNull`() {
            injectOrNull<String>()
                .shouldBeInstanceOf<io.jentz.winter.delegate.InstanceOrNullProperty<*>>()
        }

        @Test
        fun `should return LazyInstanceProperty for #injectLazy`() {
            injectLazy<String>()
                .shouldBeInstanceOf<io.jentz.winter.delegate.LazyInstanceProperty<*>>()
        }

        @Test
        fun `should return LazyInstanceOrNullProperty for #injectLazyOrNull`() {
            injectLazyOrNull<String>()
                .shouldBeInstanceOf<io.jentz.winter.delegate.LazyInstanceOrNullProperty<*>>()
        }

        @Test
        fun `should return ProvidersOfTypeProperty for #injectProvidersOfType`() {
            injectProvidersOfType<String>()
                .shouldBeInstanceOf<io.jentz.winter.delegate.ProvidersOfTypeProperty<*>>()
        }

        @Test
        fun `should return InstancesOfTypeProperty for #injectInstancesOfType`() {
            injectInstancesOfType<String>()
                .shouldBeInstanceOf<io.jentz.winter.delegate.InstancesOfTypeProperty<*>>()
        }

        @Test
        fun `should return LazyInstancesOfTypeProperty for #injectLazyInstancesOfType`() {
            injectLazyInstancesOfType<String>()
                .shouldBeInstanceOf<io.jentz.winter.delegate.LazyInstancesOfTypeProperty<*>>()
        }

    }

    @Nested
    inner class InjectedProperty {

        @Test
        fun `should register itself on the delegate notifier`() {
            app.component { constant("a value") }
            app.tree.open()
            InjectedPropertiesClass().property.shouldBe("a value")
        }

    }

    @Nested
    inner class ProviderProperty {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                ProviderProperty<String>(typeKey()).value
            }
        }

        @Test
        fun `throws an exception if dependency can't be found`() {
            shouldThrow<EntryNotFoundException> {
                ProviderProperty<String>(typeKey()).inject(emptyGraph)
            }
        }

        @Test
        fun `returns a provider block`() {
            val property = ProviderProperty<Int>(typeKey())
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
                ProviderOrNullProperty<String>(typeKey()).value
            }
        }

        @Test
        fun `#value should be null if dependency is not found`() {
            ProviderOrNullProperty<String>(typeKey()).apply {
                inject(emptyGraph)
                value.shouldBeNull()
            }
        }

        @Test
        fun `returns a provider block that resolves dependency when called`() {
            val property = ProviderOrNullProperty<Int>(typeKey())
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
                InstanceProperty<String>(typeKey()).value
            }
        }

        @Test
        fun `throws an exception if dependency can't be found`() {
            shouldThrow<EntryNotFoundException> {
                InstanceProperty<String>(typeKey()).inject(emptyGraph)
            }
        }

        @Test
        fun `should eagerly resolve dependency`() {
            InstanceProperty<Int>(typeKey()).inject(testComponent.createGraph())
            atomicInteger.get().shouldBe(1)
        }

    }

    @Nested
    inner class InstanceOrNullProperty {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                InstanceOrNullProperty<String>(typeKey()).value
            }
        }

        @Test
        fun `#value should be null if dependency is not found`() {
            InstanceOrNullProperty<String>(typeKey()).apply {
                inject(emptyGraph)
                value.shouldBeNull()
            }
        }

        @Test
        fun `should eagerly resolve dependency`() {
            InstanceOrNullProperty<Int>(typeKey())
                .inject(testComponent.createGraph())
            atomicInteger.get().shouldBe(1)
        }

    }

    @Nested
    inner class LazyInstanceProperty {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                LazyInstanceProperty<String>(typeKey()).value
            }
        }

        @Test
        fun `throws an exception if dependency can't be found`() {
            shouldThrow<EntryNotFoundException> {
                LazyInstanceProperty<String>(typeKey()).inject(emptyGraph)
            }
        }

        @Test
        fun `should lazy resolve dependency`() {
            LazyInstanceProperty<Int>(typeKey()).apply {
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
                LazyInstanceOrNullProperty<String>(typeKey()).value
            }
        }

        @Test
        fun `should resolve to null if dependency isn't found`() {
            LazyInstanceOrNullProperty<String>(typeKey()).apply {
                inject(emptyGraph)
                value.shouldBeNull()
            }
        }

        @Test
        fun `should lazy resolve dependency`() {
            LazyInstanceOrNullProperty<Int>(typeKey()).apply {
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
                ProvidersOfTypeProperty<String>(typeKeyOfType()).value
            }
        }

        @Test
        fun `should return an empty set if no dependency was found`() {
            ProvidersOfTypeProperty<String>(typeKeyOfType()).apply {
                inject(emptyGraph)
                value.shouldBeEmpty()
            }
        }

        @Test
        fun `returns a set of provider blocks`() {
            ProvidersOfTypeProperty<Int>(typeKeyOfType()).apply {
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
                InstancesOfTypeProperty<String>(typeKeyOfType()).value
            }
        }

        @Test
        fun `should return an empty set if no dependency was found`() {
            InstancesOfTypeProperty<String>(typeKeyOfType()).apply {
                inject(emptyGraph)
                value.shouldBeEmpty()
            }
        }

        @Test
        fun `should eagerly resolve dependencies`() {
            expectValueToChange(0, 5, atomicInteger::get) {
                InstancesOfTypeProperty<Int>(typeKeyOfType()).apply {
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
                LazyInstancesOfTypeProperty<String>(typeKeyOfType()).value
            }
        }

        @Test
        fun `should return an empty set if no dependency was found`() {
            LazyInstancesOfTypeProperty<String>(typeKeyOfType()).apply {
                inject(emptyGraph)
                value.shouldBeEmpty()
            }
        }

        @Test
        fun `should lazy resolve dependencies`() {
            LazyInstancesOfTypeProperty<Int>(typeKeyOfType()).apply {
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
            val property = InstanceProperty<Int>(typeKey())

            shouldThrow<UninitializedPropertyAccessException> {
                property.map { it * 2 }.value
            }
        }

        @Test
        fun `#value should apply mapping function to given property value`() {
            InstanceProperty<Int>(typeKey())
                .map { it * 3 }
                .apply {
                    inject(testComponent.createGraph())
                    value.shouldBe(3)
                }
        }

    }

    private inner class InjectedPropertiesClass {
        val property: String by inject()

        init {
            app.inject(this)
        }
    }

}