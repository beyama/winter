package io.jentz.winter.delegate

import io.jentz.winter.EntryNotFoundException
import io.jentz.winter.WinterApplication
import io.jentz.winter.WinterException
import io.jentz.winter.component
import io.jentz.winter.emptyGraph
import io.jentz.winter.graph
import io.jentz.winter.erased
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class InjectedPropertyTest {

    private val app = WinterApplication()

    private var counter = 0

    private val emptyGraph = emptyGraph()

    private val testComponent = component {
        prototype { ++counter }
    }

    private val injector = Injector(this, app)

    @Test
    fun `Injector should return ProviderProperty for provider`() {
        injector.provider<String>().shouldBeInstanceOf<ProviderProperty<*>>()
    }

    @Test
    fun `Injector should return InstanceProperty for instance`() {
        injector.instance<String>().shouldBeInstanceOf<InstanceProperty<*>>()
    }

    @Test
    fun `Injector should return LazyInstanceProperty for lazyInstance`() {
        injector.lazyInstance<String>()
            .shouldBeInstanceOf<LazyInstanceProperty<*>>()
    }


    @Test
    fun `Injector should inject on init`() {
        app.component { constant("a value") }
        app.openGraph()
        InjectedPropertiesClass(app).property.shouldBe("a value")
    }

    @Test
    fun `Injector should throw an exception if inject is called multiple times`() {
        app.component { constant("a value") }
        app.openGraph()
        val property = InjectedPropertiesClass(app).delegate
        shouldThrow<WinterException> {
            property.inject(app.graph)
        }.message.shouldBe("Inject was called multiple times.")
    }

    @Test
    fun `ProviderProperty should throw an exception if value is called before injecting`() {
        shouldThrow<NotInjectedException> {
            ProviderProperty<String>(erased(), null).value
        }
    }

    @Test
    fun `ProviderProperty throws an exception if dependency can't be found`() {
        shouldThrow<EntryNotFoundException> {
            ProviderProperty<String>(erased(), null).inject(emptyGraph)
        }
    }

    @Test
    fun `ProviderProperty returns a provider block`() {
        val property = ProviderProperty<Int>(erased(), null)
        property.inject(testComponent.createGraph())
        val provider = property.value
        counter.shouldBe(0)
        provider().shouldBe(1)
    }


    @Test
    fun `InstanceProperty should throw an exception if value is called before injecting`() {
        shouldThrow<NotInjectedException> {
            InstanceProperty<String>(erased(), null).value
        }
    }

    @Test
    fun `InstanceProperty throws an exception if dependency can't be found`() {
        shouldThrow<EntryNotFoundException> {
            InstanceProperty<String>(erased(), null).inject(emptyGraph)
        }
    }

    @Test
    fun `InstanceProperty value should be null if dependency is optional and not found`() {
        InstanceProperty<String?>(erased(), null).apply {
            inject(emptyGraph)
            value.shouldBeNull()
        }
    }

    @Test
    fun `InstanceProperty should eagerly resolve dependency`() {
        InstanceProperty<Int>(erased(), null).inject(testComponent.createGraph())
        counter.shouldBe(1)
    }


    @Test
    fun `LazyInstanceProperty should throw an exception if value is called before injecting`() {
        shouldThrow<NotInjectedException> {
            LazyInstanceProperty<String>(erased(), null).value
        }
    }

    @Test
    fun `LazyInstanceProperty throws an exception if dependency can't be found`() {
        shouldThrow<EntryNotFoundException> {
            LazyInstanceProperty<String>(erased(), null).inject(emptyGraph)
        }
    }

    @Test
    fun `LazyInstanceProperty should lazy resolve dependency`() {
        LazyInstanceProperty<Int>(erased(), null).apply {
            inject(testComponent.createGraph())
            counter.shouldBe(0)
            value.shouldBe(1)
            counter.shouldBe(1)
        }
    }

    @Test
    fun `LazyInstanceProperty should resolve existing optional dependency`() {
        LazyInstanceProperty<String?>(erased(), null).apply {
            inject(graph { prototype { "test string" } })
            value.shouldBe("test string")
        }
    }

    @Test
    fun `LazyInstanceProperty should resolve to null for non-existing optional dependency`() {
        LazyInstanceProperty<String?>(erased(), null).apply {
            inject(emptyGraph)
            value.shouldBeNull()
        }
    }


    @Test
    fun `eager property mapper should throw an exception if value is called before injecting`() {
        val property = InstanceProperty<Int>(erased(), null)

        shouldThrow<NotInjectedException> {
            property.map { it * 2 }.value
        }
    }

    @Test
    fun `eager property mapper should be applied after inject is called`() {
        InstanceProperty<Int>(erased(), null)
            .map { it * 3 }
            .apply { inject(testComponent.createGraph()) }
        counter.shouldBe(1)
    }

    @Test
    fun `eager property mapper value should apply mapping function to given property value`() {
        InstanceProperty<Int>(erased(), null)
            .map { it * 3 }
            .apply {
                inject(testComponent.createGraph())
                value.shouldBe(3)
            }
    }


    @Test
    fun `lazy property mapper should throw an exception if value is called before injecting`() {
        val property = LazyInstanceProperty<Int>(erased(), null)

        shouldThrow<NotInjectedException> {
            property.map { it * 2 }.value
        }
    }

    @Test
    fun `lazy property mapper should not be applied before accessing value`() {
        LazyInstanceProperty<Int>(erased(), null)
            .map { it * 3 }
            .apply { inject(testComponent.createGraph()) }
        counter.shouldBe(0)
    }

    @Test
    fun `lazy property mapper value should apply mapping function to given property value`() {
        LazyInstanceProperty<Int>(erased(), null)
            .map { it * 3 }
            .apply {
                inject(testComponent.createGraph())
                value.shouldBe(3)
            }
    }



    @Test
    fun `mapped properties of an injector should be injected`() {
        val app = WinterApplication { constant(21) }
        app.openGraph()

        val target = MappedPropertiesClass(app)
        target.injector.inject()

        target.eager.shouldBe(42)
        target.lazy.shouldBe("21!")
        app.closeGraph()
    }

    private class MappedPropertiesClass(app: WinterApplication) {
        val injector by app
        val eager: Int by injector<Int>().map { it * 2 }
        val lazy: String by injector.lazyInstance<Int>().map { it.toString() }.map { "$it!" }
    }

    private class InjectedPropertiesClass(app: WinterApplication) {

        private val injector by app
        val delegate = injector<String>()
        val property: String by delegate

        init {
            injector.inject()
        }
    }

}