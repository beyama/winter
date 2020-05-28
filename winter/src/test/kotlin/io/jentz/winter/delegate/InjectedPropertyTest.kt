package io.jentz.winter.delegate

import io.jentz.winter.*
import io.jentz.winter.adapter.useApplicationGraphOnlyAdapter
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.jvm.isAccessible

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InjectedPropertyTest {

    private val app = WinterApplication()

    private val atomicInteger = AtomicInteger(0)

    private val emptyGraph = emptyGraph()

    private val testComponent = component {
        prototype { atomicInteger.incrementAndGet() }
    }

    @BeforeEach
    fun beforeEach() {
        atomicInteger.set(0)
        app.closeGraphIfOpen()
        app.useApplicationGraphOnlyAdapter()
    }

    @Nested
    inner class DelegateMethods {

        @Test
        fun `should return ProviderProperty for #injectProvider`() {
            injectProvider<String>().shouldBeInstanceOf<ProviderProperty<*>>()
        }

        @Test
        fun `should return ProviderOrNullProperty for #injectProviderOrNull`() {
            injectProviderOrNull<String>()
                .shouldBeInstanceOf<ProviderOrNullProperty<*>>()
        }

        @Test
        fun `should return InstanceProperty for #inject`() {
            inject<String>().shouldBeInstanceOf<InstanceProperty<*>>()
        }

        @Test
        fun `should return InstanceOrNullProperty for #injectOrNull`() {
            injectOrNull<String>()
                .shouldBeInstanceOf<InstanceOrNullProperty<*>>()
        }

        @Test
        fun `should return LazyInstanceProperty for #injectLazy`() {
            injectLazy<String>()
                .shouldBeInstanceOf<LazyInstanceProperty<*>>()
        }

        @Test
        fun `should return LazyInstanceOrNullProperty for #injectLazyOrNull`() {
            injectLazyOrNull<String>()
                .shouldBeInstanceOf<LazyInstanceOrNullProperty<*>>()
        }

    }

    @Nested
    @DisplayName("InjectedProperty")
    inner class InjectedPropertyTest {

        @BeforeEach
        fun beforeEach() {
            app.component { constant("a value") }
            app.openGraph()
        }

        @Test
        fun `should register itself on the delegate notifier`() {
            InjectedPropertiesClass(app).property.shouldBe("a value")
        }

        @Test
        @Suppress("UNCHECKED_CAST")
        fun `should throw an exception if inject is called multiple times`() {
            val instance = InjectedPropertiesClass(app)
            instance::property.isAccessible = true
            val property = instance::property.let { it ->
                it.isAccessible = true
                it.getDelegate() as InjectedProperty<String>
            }
            shouldThrow<WinterException> {
                property.inject(app.graph)
            }.message.shouldBe("Inject was called multiple times.")
        }

    }

    @Nested
    @DisplayName("ProviderProperty")
    inner class ProviderPropertyTest {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                ProviderProperty<String>(typeKey(), null).value
            }
        }

        @Test
        fun `throws an exception if dependency can't be found`() {
            shouldThrow<EntryNotFoundException> {
                ProviderProperty<String>(typeKey(), null).inject(emptyGraph)
            }
        }

        @Test
        fun `returns a provider block`() {
            val property = ProviderProperty<Int>(typeKey(), null)
            property.inject(testComponent.createGraph())
            val provider = property.value
            atomicInteger.get().shouldBe(0)
            provider().shouldBe(1)
        }

    }

    @Nested
    @DisplayName("ProviderOrNullProperty")
    inner class ProviderOrNullPropertyTest {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                ProviderOrNullProperty<String>(typeKey(), null).value
            }
        }

        @Test
        fun `#value should be null if dependency is not found`() {
            ProviderOrNullProperty<String>(typeKey(), null).apply {
                inject(emptyGraph)
                value.shouldBeNull()
            }
        }

        @Test
        fun `returns a provider block that resolves dependency when called`() {
            val property = ProviderOrNullProperty<Int>(typeKey(), null)
            property.inject(testComponent.createGraph())
            val provider = property.value
            atomicInteger.get().shouldBe(0)
            provider?.invoke().shouldBe(1)
        }

    }

    @Nested
    @DisplayName("InstanceProperty")
    inner class InstancePropertyTest {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                InstanceProperty<String>(typeKey(), null).value
            }
        }

        @Test
        fun `throws an exception if dependency can't be found`() {
            shouldThrow<EntryNotFoundException> {
                InstanceProperty<String>(typeKey(), null).inject(emptyGraph)
            }
        }

        @Test
        fun `should eagerly resolve dependency`() {
            InstanceProperty<Int>(typeKey(), null).inject(testComponent.createGraph())
            atomicInteger.get().shouldBe(1)
        }

    }

    @Nested
    @DisplayName("InstanceOrNullProperty")
    inner class InstanceOrNullPropertyTest {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                InstanceOrNullProperty<String>(typeKey(), null).value
            }
        }

        @Test
        fun `#value should be null if dependency is not found`() {
            InstanceOrNullProperty<String>(typeKey(), null).apply {
                inject(emptyGraph)
                value.shouldBeNull()
            }
        }

        @Test
        fun `should eagerly resolve dependency`() {
            InstanceOrNullProperty<Int>(typeKey(), null)
                .inject(testComponent.createGraph())
            atomicInteger.get().shouldBe(1)
        }

    }

    @Nested
    @DisplayName("LazyInstanceProperty")
    inner class LazyInstancePropertyTest {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                LazyInstanceProperty<String>(typeKey(), null).value
            }
        }

        @Test
        fun `throws an exception if dependency can't be found`() {
            shouldThrow<EntryNotFoundException> {
                LazyInstanceProperty<String>(typeKey(), null).inject(emptyGraph)
            }
        }

        @Test
        fun `should lazy resolve dependency`() {
            LazyInstanceProperty<Int>(typeKey(), null).apply {
                inject(testComponent.createGraph())
                expectValueToChange(0, 1, atomicInteger::get) {
                    value.shouldBe(1)
                }
            }
        }

    }

    @Nested
    @DisplayName("LazyInstanceOrNullProperty")
    inner class LazyInstanceOrNullPropertyTest {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            shouldThrow<UninitializedPropertyAccessException> {
                LazyInstanceOrNullProperty<String>(typeKey(), null).value
            }.message.shouldBe("Property not initialized.")
        }

        @Test
        fun `should resolve to null if dependency isn't found`() {
            LazyInstanceOrNullProperty<String>(typeKey(), null).apply {
                inject(emptyGraph)
                value.shouldBeNull()
            }
        }

        @Test
        fun `should lazy resolve dependency`() {
            LazyInstanceOrNullProperty<Int>(typeKey(), null).apply {
                inject(testComponent.createGraph())
                expectValueToChange(0, 1, atomicInteger::get) {
                    value.shouldBe(1)
                }
            }
        }

    }

    @Nested
    @DisplayName("PropertyMapper")
    inner class PropertyMapperTest {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            val property = InstanceProperty<Int>(typeKey(), null)

            shouldThrow<UninitializedPropertyAccessException> {
                property.map { it * 2 }.value
            }
        }

        @Test
        fun `#value should apply mapping function to given property value`() {
            InstanceProperty<Int>(typeKey(), null)
                .map { it * 3 }
                .apply {
                    inject(testComponent.createGraph())
                    value.shouldBe(3)
                }
        }

    }

    private class InjectedPropertiesClass(val app: WinterApplication) {
        val property: String by inject()

        init {
            app.inject(this)
        }
    }

}