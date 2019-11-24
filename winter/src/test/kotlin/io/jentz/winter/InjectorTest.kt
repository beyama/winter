package io.jentz.winter

import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.shouldBeInstanceOf
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
        factory<Int, String> { arg -> arg.toString() }
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
            injector.provider<String>().shouldBeInstanceOf<Injector.ProviderProperty<*, *>>()
        }

        @Test
        fun `should return ProviderProperty for #provider with argument`() {
            injector.provider<Unit, String>(argument = Unit)
                .shouldBeInstanceOf<Injector.ProviderProperty<*, *>>()
        }

        @Test
        fun `should return ProviderOrNullProperty for #providerOrNull`() {
            injector.providerOrNull<String>()
                .shouldBeInstanceOf<Injector.ProviderOrNullProperty<*, *>>()
        }

        @Test
        fun `should return ProviderOrNullProperty for #providerOrNull with argument`() {
            injector.providerOrNull<Unit, String>(argument = Unit)
                .shouldBeInstanceOf<Injector.ProviderOrNullProperty<*, *>>()
        }

        @Test
        fun `should return InstanceProperty for #instance`() {
            injector.instance<String>().shouldBeInstanceOf<Injector.InstanceProperty<*, *>>()
        }

        @Test
        fun `should return InstanceProperty for #instance with argument`() {
            injector.instance<Unit, String>(argument = Unit)
                .shouldBeInstanceOf<Injector.InstanceProperty<*, *>>()
        }

        @Test
        fun `should return InstanceOrNullProperty for #instanceOrNull`() {
            injector.instanceOrNull<String>()
                .shouldBeInstanceOf<Injector.InstanceOrNullProperty<*, *>>()
        }

        @Test
        fun `should return InstanceOrNullProperty for #instanceOrNull with argument`() {
            injector.instanceOrNull<Unit, String>(argument = Unit)
                .shouldBeInstanceOf<Injector.InstanceOrNullProperty<*, *>>()
        }

        @Test
        fun `should return LazyInstanceProperty for #lazyInstance`() {
            injector.lazyInstance<String>()
                .shouldBeInstanceOf<Injector.LazyInstanceProperty<*, *>>()
        }

        @Test
        fun `should return LazyInstanceProperty for #lazyInstance with argument`() {
            injector.lazyInstance<Unit, String>(argument = Unit)
                .shouldBeInstanceOf<Injector.LazyInstanceProperty<*, *>>()
        }

        @Test
        fun `should return LazyInstanceOrNullProperty for #lazyInstanceOrNull`() {
            injector.lazyInstanceOrNull<String>()
                .shouldBeInstanceOf<Injector.LazyInstanceOrNullProperty<*, *>>()
        }

        @Test
        fun `should return LazyInstanceOrNullProperty for #lazyInstanceOrNull with argument`() {
            injector.lazyInstanceOrNull<Unit, String>(argument = Unit)
                .shouldBeInstanceOf<Injector.LazyInstanceOrNullProperty<*, *>>()
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

        @Test
        fun `should return FactoryProperty for #factory`() {
            injector.factory<Int, String>().shouldBeInstanceOf<Injector.FactoryProperty<*, *>>()
        }

        @Test
        fun `should return FactoryOrNullProperty for #factoryOrNull`() {
            injector.factoryOrNull<Int, String>()
                .shouldBeInstanceOf<Injector.FactoryOrNullProperty<*, *>>()
        }

    }

    @Nested
    inner class ProviderProperty {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                Injector.ProviderProperty<Unit, String>(typeKey(), Unit).value
            }
        }

        @Test
        fun `throws an exception if dependency can't be found`() {
            shouldThrow<EntryNotFoundException> {
                Injector.ProviderProperty<Unit, String>(typeKey(), Unit).inject(emptyGraph)
            }
        }

        @Test
        fun `returns a provider block`() {
            val property = Injector.ProviderProperty<Unit, Int>(typeKey(), Unit)
            property.inject(testComponent.createGraph())
            val provider = property.value
            atomicInteger.get().shouldBe(0)
            provider().shouldBe(1)
        }

        @Test
        fun `returns a provider block that resolves dependency with argument when called`() {
            val property =
                Injector.ProviderProperty<Int, String>(compoundTypeKey(), 42)
            property.inject(testComponent.createGraph())
            val provider = property.value
            provider().shouldBe("42")
        }

    }

    @Nested
    inner class ProviderOrNullProperty {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                Injector.ProviderOrNullProperty<Unit, String>(typeKey(), Unit).value
            }
        }

        @Test
        fun `#value should be null if dependency is not found`() {
            Injector.ProviderOrNullProperty<Unit, String>(typeKey(), Unit).apply {
                inject(emptyGraph)
                value.shouldBeNull()
            }
        }

        @Test
        fun `returns a provider block that resolves dependency when called`() {
            val property = Injector.ProviderOrNullProperty<Unit, Int>(typeKey(), Unit)
            property.inject(testComponent.createGraph())
            val provider = property.value
            atomicInteger.get().shouldBe(0)
            provider?.invoke().shouldBe(1)
        }

        @Test
        fun `returns a provider block that resolves dependency with argument when called`() {
            val property =
                Injector.ProviderOrNullProperty<Int, String>(compoundTypeKey(), 42)
            property.inject(testComponent.createGraph())
            val provider = property.value
            provider?.invoke().shouldBe("42")
        }

    }

    @Nested
    inner class InstanceProperty {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                Injector.InstanceProperty<Unit, String>(typeKey(), Unit).value
            }
        }

        @Test
        fun `throws an exception if dependency can't be found`() {
            shouldThrow<EntryNotFoundException> {
                Injector.InstanceProperty<Unit, String>(typeKey(), Unit).inject(emptyGraph)
            }
        }

        @Test
        fun `should eagerly resolve dependency`() {
            Injector.InstanceProperty<Unit, Int>(typeKey(), Unit).inject(testComponent.createGraph())
            atomicInteger.get().shouldBe(1)
        }

        @Test
        fun `should resolve dependency with argument`() {
            Injector.InstanceProperty<Int, String>(compoundTypeKey(), 42).apply {
                inject(testComponent.createGraph())
                value.shouldBe("42")
            }
        }

    }

    @Nested
    inner class InstanceOrNullProperty {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                Injector.InstanceOrNullProperty<Unit, String>(typeKey(), Unit).value
            }
        }

        @Test
        fun `#value should be null if dependency is not found`() {
            Injector.InstanceOrNullProperty<Unit, String>(typeKey(), Unit).apply {
                inject(emptyGraph)
                value.shouldBeNull()
            }
        }

        @Test
        fun `should eagerly resolve dependency`() {
            Injector.InstanceOrNullProperty<Unit, Int>(typeKey(), Unit)
                .inject(testComponent.createGraph())
            atomicInteger.get().shouldBe(1)
        }

        @Test
        fun `should resolve dependency with argument`() {
            Injector.InstanceOrNullProperty<Int, String>(compoundTypeKey(), 42).apply {
                inject(testComponent.createGraph())
                value.shouldBe("42")
            }
        }

    }

    @Nested
    inner class LazyInstanceProperty {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                Injector.LazyInstanceProperty<Unit, String>(typeKey(), Unit).value
            }
        }

        @Test
        fun `throws an exception if dependency can't be found`() {
            shouldThrow<EntryNotFoundException> {
                Injector.LazyInstanceProperty<Unit, String>(typeKey(), Unit)
                    .inject(emptyGraph)
            }
        }

        @Test
        fun `should lazy resolve dependency`() {
            Injector.LazyInstanceProperty<Unit, Int>(typeKey(), Unit).apply {
                inject(testComponent.createGraph())
                expectValueToChange(0, 1, atomicInteger::get) {
                    value.shouldBe(1)
                }
            }
        }

        @Test
        fun `should resolve dependency with argument`() {
            Injector.LazyInstanceProperty<Int, String>(compoundTypeKey(), 42).apply {
                inject(testComponent.createGraph())
                value.shouldBe("42")
            }
        }

    }

    @Nested
    inner class LazyInstanceOrNullProperty {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                Injector.LazyInstanceOrNullProperty<Unit, String>(typeKey(), Unit).value
            }
        }

        @Test
        fun `should resolve to null if dependency isn't found`() {
            Injector.LazyInstanceOrNullProperty<Unit, String>(typeKey(), Unit).apply {
                inject(emptyGraph)
                value.shouldBeNull()
            }
        }

        @Test
        fun `should lazy resolve dependency`() {
            Injector.LazyInstanceOrNullProperty<Unit, Int>(typeKey(), Unit).apply {
                inject(testComponent.createGraph())
                expectValueToChange(0, 1, atomicInteger::get) {
                    value.shouldBe(1)
                }
            }
        }

        @Test
        fun `should resolve dependency with argument`() {
            Injector.LazyInstanceOrNullProperty<Int, String>(compoundTypeKey(), 42)
                .apply {
                    inject(testComponent.createGraph())
                    value.shouldBe("42")
                }
        }

    }

    @Nested
    inner class FactoryProperty {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                Injector.FactoryProperty<Int, String>(compoundTypeKey()).value
            }
        }

        @Test
        fun `throws an exception if dependency can't be found`() {
            shouldThrow<EntryNotFoundException> {
                Injector.FactoryProperty<String, String>(compoundTypeKey()).inject(emptyGraph)
            }
        }

        @Test
        fun `should resolve dependency`() {
            val factory: Factory<Int, String> = Injector.FactoryProperty<Int, String>(compoundTypeKey()).run {
                    inject(testComponent.createGraph())
                    value
                }
            factory(42).shouldBe("42")
        }

    }

    @Nested
    inner class FactoryOrNullProperty {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                Injector.FactoryOrNullProperty<Int, String>(compoundTypeKey()).value
            }
        }

        @Test
        fun `should resolve dependency`() {
            val factory: Factory<Int, String>? = Injector.FactoryOrNullProperty<Int, String>(compoundTypeKey()).run {
                inject(testComponent.createGraph())
                value
            }
            factory?.invoke(42).shouldBe("42")
        }

        @Test
        fun `should resolve to null if dependency can't be found`() {
            val factory: Factory<String, String>? = Injector.FactoryOrNullProperty<String, String>(compoundTypeKey()).run {
                inject(testComponent.createGraph())
                value
            }
            factory.shouldBeNull()
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
            val property = Injector.InstanceProperty<Unit, Int>(typeKey(), Unit)

            shouldThrow<UninitializedPropertyAccessException> {
                property.map { it * 2 }.value
            }
        }

        @Test
        fun `#value should apply mapping function to given property value`() {
            Injector.InstanceProperty<Unit, Int>(typeKey(), Unit)
                .map { it * 3 }
                .apply {
                    inject(testComponent.createGraph())
                    value.shouldBe(3)
                }
        }

    }

}