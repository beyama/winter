package io.jentz.winter

import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.*
import java.util.concurrent.atomic.AtomicInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InjectorTest {

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
    }

    @Nested
    @DisplayName("Injector methods")
    inner class InjectorMethods {

        private var injector = Injector()

        @BeforeEach
        fun beforeEach() {
            injector = Injector()
        }

        @Test
        fun `#injected should return true if graph was injected otherwise false`() {
            expectValueToChange(false, true, injector::injected) {
                injector.inject(emptyGraph)
            }
        }

        @Test
        fun `#register should throw an exception if graph was already injected`() {
            injector.inject(emptyGraph)
            shouldThrow<IllegalStateException> {
                injector.provider<String>()
            }
        }

        @Test
        fun `#inject should throw an exception when called more than once`() {
            injector.inject(emptyGraph)
            shouldThrow<IllegalStateException> { injector.inject(emptyGraph) }
        }

        @Test
        fun `#inject should call #inject on each registered property delegate`() {
            val properties = (0..3).map { TestProperty() }
            properties.forEach { injector.register(it) }
            properties.any { it.graph != null }.shouldBeFalse()
            injector.inject(emptyGraph)
            properties.all { it.graph == emptyGraph }.shouldBeTrue()
        }

        private inner class TestProperty : Injector.InjectedProperty<Unit>() {
            var graph: Graph? = null
            override val value = Unit
            override fun inject(graph: Graph) {
                this.graph = graph
            }
        }

    }

    @Nested
    @DisplayName("Injector property delegate methods")
    inner class PropertyDelegateMethods {

        private val injector = Injector()

        @Test
        fun `should return ProviderProperty for #provider`() {
            injector.provider<String>().shouldBeInstanceOf<Injector.ProviderProperty<*>>()
        }

        @Test
        fun `should return ProviderOrNullProperty for #providerOrNull`() {
            injector.providerOrNull<String>()
                .shouldBeInstanceOf<Injector.ProviderOrNullProperty<*>>()
        }

        @Test
        fun `should return InstanceProperty for #instance`() {
            injector.instance<String>().shouldBeInstanceOf<Injector.InstanceProperty<*>>()
        }

        @Test
        fun `should return InstanceOrNullProperty for #instanceOrNull`() {
            injector.instanceOrNull<String>()
                .shouldBeInstanceOf<Injector.InstanceOrNullProperty<*>>()
        }

        @Test
        fun `should return LazyInstanceProperty for #lazyInstance`() {
            injector.lazyInstance<String>()
                .shouldBeInstanceOf<Injector.LazyInstanceProperty<*>>()
        }

        @Test
        fun `should return LazyInstanceOrNullProperty for #lazyInstanceOrNull`() {
            injector.lazyInstanceOrNull<String>()
                .shouldBeInstanceOf<Injector.LazyInstanceOrNullProperty<*>>()
        }

        @Test
        fun `should return ProvidersOfTypeProperty for #providersOfType`() {
            injector.providersOfType<String>()
                .shouldBeInstanceOf<Injector.ProvidersOfTypeProperty<*>>()
        }

        @Test
        fun `should return InstancesOfTypeProperty for #instancesOfType`() {
            injector.instancesOfType<String>()
                .shouldBeInstanceOf<Injector.InstancesOfTypeProperty<*>>()
        }

        @Test
        fun `should return LazyInstancesOfTypeProperty for #lazyInstancesOfType`() {
            injector.lazyInstancesOfType<String>()
                .shouldBeInstanceOf<Injector.LazyInstancesOfTypeProperty<*>>()
        }

    }

    @Nested
    inner class ProviderProperty {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                Injector.ProviderProperty<String>(typeKey()).value
            }
        }

        @Test
        fun `throws an exception if dependency can't be found`() {
            shouldThrow<EntryNotFoundException> {
                Injector.ProviderProperty<String>(typeKey()).inject(emptyGraph)
            }
        }

        @Test
        fun `returns a provider block`() {
            val property = Injector.ProviderProperty<Int>(typeKey())
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
                Injector.ProviderOrNullProperty<String>(typeKey()).value
            }
        }

        @Test
        fun `#value should be null if dependency is not found`() {
            Injector.ProviderOrNullProperty<String>(typeKey()).apply {
                inject(emptyGraph)
                value.shouldBeNull()
            }
        }

        @Test
        fun `returns a provider block that resolves dependency when called`() {
            val property = Injector.ProviderOrNullProperty<Int>(typeKey())
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
                Injector.InstanceProperty<String>(typeKey()).value
            }
        }

        @Test
        fun `throws an exception if dependency can't be found`() {
            shouldThrow<EntryNotFoundException> {
                Injector.InstanceProperty<String>(typeKey()).inject(emptyGraph)
            }
        }

        @Test
        fun `should eagerly resolve dependency`() {
            Injector.InstanceProperty<Int>(typeKey()).inject(testComponent.createGraph())
            atomicInteger.get().shouldBe(1)
        }

    }

    @Nested
    inner class InstanceOrNullProperty {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                Injector.InstanceOrNullProperty<String>(typeKey()).value
            }
        }

        @Test
        fun `#value should be null if dependency is not found`() {
            Injector.InstanceOrNullProperty<String>(typeKey()).apply {
                inject(emptyGraph)
                value.shouldBeNull()
            }
        }

        @Test
        fun `should eagerly resolve dependency`() {
            Injector.InstanceOrNullProperty<Int>(typeKey())
                .inject(testComponent.createGraph())
            atomicInteger.get().shouldBe(1)
        }

    }

    @Nested
    inner class LazyInstanceProperty {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                Injector.LazyInstanceProperty<String>(typeKey()).value
            }
        }

        @Test
        fun `throws an exception if dependency can't be found`() {
            shouldThrow<EntryNotFoundException> {
                Injector.LazyInstanceProperty<String>(typeKey()).inject(emptyGraph)
            }
        }

        @Test
        fun `should lazy resolve dependency`() {
            Injector.LazyInstanceProperty<Int>(typeKey()).apply {
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
                Injector.LazyInstanceOrNullProperty<String>(typeKey()).value
            }
        }

        @Test
        fun `should resolve to null if dependency isn't found`() {
            Injector.LazyInstanceOrNullProperty<String>(typeKey()).apply {
                inject(emptyGraph)
                value.shouldBeNull()
            }
        }

        @Test
        fun `should lazy resolve dependency`() {
            Injector.LazyInstanceOrNullProperty<Int>(typeKey()).apply {
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
                Injector.ProvidersOfTypeProperty<String>(typeKeyOfType()).value
            }
        }

        @Test
        fun `should return an empty set if no dependency was found`() {
            Injector.ProvidersOfTypeProperty<String>(typeKeyOfType()).apply {
                inject(emptyGraph)
                value.shouldBeEmpty()
            }
        }

        @Test
        fun `returns a set of provider blocks`() {
            Injector.ProvidersOfTypeProperty<Int>(typeKeyOfType()).apply {
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
                Injector.InstancesOfTypeProperty<String>(typeKeyOfType()).value
            }
        }

        @Test
        fun `should return an empty set if no dependency was found`() {
            Injector.InstancesOfTypeProperty<String>(typeKeyOfType()).apply {
                inject(emptyGraph)
                value.shouldBeEmpty()
            }
        }

        @Test
        fun `should eagerly resolve dependencies`() {
            expectValueToChange(0, 5, atomicInteger::get) {
                Injector.InstancesOfTypeProperty<Int>(typeKeyOfType()).apply {
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
                Injector.LazyInstancesOfTypeProperty<String>(typeKeyOfType()).value
            }
        }

        @Test
        fun `should return an empty set if no dependency was found`() {
            Injector.LazyInstancesOfTypeProperty<String>(typeKeyOfType()).apply {
                inject(emptyGraph)
                value.shouldBeEmpty()
            }
        }

        @Test
        fun `should lazy resolve dependencies`() {
            Injector.LazyInstancesOfTypeProperty<Int>(typeKeyOfType()).apply {
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
            val property = Injector.InstanceProperty<Int>(typeKey())

            shouldThrow<UninitializedPropertyAccessException> {
                property.map { it * 2 }.value
            }
        }

        @Test
        fun `#value should apply mapping function to given property value`() {
            Injector.InstanceProperty<Int>(typeKey())
                .map { it * 3 }
                .apply {
                    inject(testComponent.createGraph())
                    value.shouldBe(3)
                }
        }

    }

}