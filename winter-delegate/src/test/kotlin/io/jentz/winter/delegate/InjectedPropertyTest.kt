package io.jentz.winter.delegate

import io.jentz.winter.EntryNotFoundException
import io.jentz.winter.WinterApplication
import io.jentz.winter.WinterException
import io.jentz.winter.component
import io.jentz.winter.emptyGraph
import io.jentz.winter.graph
import io.jentz.winter.typeKey
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
    }

    @Nested
    inner class DelegateMethods {

        private val injector = Injector(this, app)

        @Test
        fun `should return ProviderProperty for #provider`() {
            injector.provider<String>().shouldBeInstanceOf<ProviderProperty<*>>()
        }

        @Test
        fun `should return InstanceProperty for #instance`() {
            injector.instance<String>().shouldBeInstanceOf<InstanceProperty<*>>()
        }

        @Test
        fun `should return LazyInstanceProperty for #lazyInstance`() {
            injector.lazyInstance<String>()
                .shouldBeInstanceOf<LazyInstanceProperty<*>>()
        }

    }

    @Nested
    @DisplayName("Injector")
    inner class InjectorTest {

        @BeforeEach
        fun beforeEach() {
            app.component { constant("a value") }
            app.openGraph()
        }

        @Test
        fun `should inject on init`() {
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
        fun `#value should be null if dependency is optional and not found`() {
            InstanceProperty<String?>(typeKey(), null).apply {
                inject(emptyGraph)
                value.shouldBeNull()
            }
        }

        @Test
        fun `should eagerly resolve dependency`() {
            InstanceProperty<Int>(typeKey(), null).inject(testComponent.createGraph())
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
                atomicInteger.get().shouldBe(0)
                value.shouldBe(1)
                atomicInteger.get().shouldBe(1)
            }
        }

        @Test
        fun `should resolve existing optional dependency`() {
            LazyInstanceProperty<String?>(typeKey(), null).apply {
                inject(graph { prototype { "test string" } })
                value.shouldBe("test string")
            }
        }

        @Test
        fun `should resolve to null for non-existing optional dependency`() {
            LazyInstanceProperty<String?>(typeKey(), null).apply {
                inject(emptyGraph)
                value.shouldBeNull()
            }
        }

    }

    @Nested
    @DisplayName("eager property mapper")
    inner class EagerPropertyMapperTest {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            val property = InstanceProperty<Int>(typeKey(), null)

            shouldThrow<UninitializedPropertyAccessException> {
                property.map { it * 2 }.value
            }
        }

        @Test
        fun `should be applied after inject is called`() {
            InstanceProperty<Int>(typeKey(), null)
                .map { it * 3 }
                .apply { inject(testComponent.createGraph()) }
            atomicInteger.get().shouldBe(1)
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

    @Nested
    @DisplayName("lazy property mapper")
    inner class LayzPropertyMapperTest {

        @Test
        fun `should throw an exception if #value is called before injecting`() {
            val property = LazyInstanceProperty<Int>(typeKey(), null)

            shouldThrow<UninitializedPropertyAccessException> {
                property.map { it * 2 }.value
            }
        }

        @Test
        fun `should not be applied before accessing value`() {
            LazyInstanceProperty<Int>(typeKey(), null)
                .map { it * 3 }
                .apply { inject(testComponent.createGraph()) }
            atomicInteger.get().shouldBe(0)
        }

        @Test
        fun `#value should apply mapping function to given property value`() {
            LazyInstanceProperty<Int>(typeKey(), null)
                .map { it * 3 }
                .apply {
                    inject(testComponent.createGraph())
                    value.shouldBe(3)
                }
        }

    }


    private class InjectedPropertiesClass(app: WinterApplication) {

        private val injector by app
        val property: String by injector()

        init {
            injector.inject()
        }
    }

}