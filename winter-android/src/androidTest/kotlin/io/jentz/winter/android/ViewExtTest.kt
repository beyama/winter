package io.jentz.winter.android

import android.support.test.InstrumentationRegistry
import android.view.View
import io.jentz.winter.*
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.collections.shouldContainAll
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.After
import org.junit.Before
import org.junit.Test

class ViewExtTest {

    private val context = InstrumentationRegistry.getContext()

    private val applicationComponent = component {
        constant(42)
        constant(5, qualifier = "other")
        factory { i: Int -> i.toString() }
        factory("other") { i: Int -> (i * 2).toString() }
    }

    @Before
    fun beforeEach() {
        GraphRegistry.closeIfOpen()
        Injection.useAndroidPresentationScopeAdapter()
        Winter.component { include(applicationComponent) }
        GraphRegistry.open()
    }

    @After
    fun afterEach() {
        GraphRegistry.close()
    }

    @Test
    fun dependencyGraph_should_return_graph_associated_with_context() {
        val view = View(context)
        view.dependencyGraph.shouldBe(GraphRegistry.get())
    }

    @Test
    fun instance_methods() {
        val view = View(context)
        view.instance<Int>().shouldBe(42)
        view.instance<Int>("other").shouldBe(5)
        view.instance<Int, String>(42).shouldBe("42")
        view.instance<Int, String>(5, "other").shouldBe("10")

        shouldThrow<EntryNotFoundException> { view.instance() }

        view.instanceOrNull<Int>().shouldBe(42)
        view.instanceOrNull<Int>("other").shouldBe(5)
        view.instanceOrNull<Int, String>(42).shouldBe("42")
        view.instanceOrNull<Int, String>(5, "other").shouldBe("10")

        view.instanceOrNull<Any, Any>("").shouldBe(null)
        view.instanceOrNull<Any, Any>("").shouldBe(null)

        view.instancesOfType<Int>().shouldContainAll(42, 5)

    }

    @Test
    fun lazy_methods() {
        val view = View(context)
        view.lazyInstance<Int>().apply {
            shouldBeInstanceOf<Lazy<Int>>()
            isInitialized().shouldBeFalse()
            value.shouldBe(42)
        }

        view.lazyInstance<Int, String>(7).apply {
            shouldBeInstanceOf<Lazy<String>>()
            isInitialized().shouldBeFalse()
            value.shouldBe("7")
        }

        shouldThrow<EntryNotFoundException> {
            view.lazyInstance<Any>().value
        }

        view.lazyInstanceOrNull<Int>().apply {
            shouldBeInstanceOf<Lazy<Int>>()
            isInitialized().shouldBeFalse()
            value.shouldBe(42)
        }

        view.lazyInstanceOrNull<Int, String>(7).apply {
            shouldBeInstanceOf<Lazy<String>>()
            isInitialized().shouldBeFalse()
            value.shouldBe("7")
        }

        view.lazyInstanceOrNull<Any>().apply {
            shouldBeInstanceOf<Lazy<Any>>()
            isInitialized().shouldBeFalse()
            value.shouldBe(null)
        }

        view.lazyInstanceOrNull<Any, Any>("").apply {
            shouldBeInstanceOf<Lazy<Any>>()
            isInitialized().shouldBeFalse()
            value.shouldBe(null)
        }
    }

    @Test
    fun provider_methods() {
        val view = View(context)

        view.provider<Int>().apply {
            shouldBeInstanceOf<Provider<Int>>()
            invoke().shouldBe(42)
        }

        view.provider<Int>("other").apply {
            shouldBeInstanceOf<Provider<Int>>()
            invoke().shouldBe(5)
        }

        view.provider<Int, String>(7).apply {
            shouldBeInstanceOf<Provider<Int>>()
            invoke().shouldBe("7")
        }

        view.provider<Int, String>(7, "other").apply {
            shouldBeInstanceOf<Provider<Int>>()
            invoke().shouldBe("14")
        }

        shouldThrow<EntryNotFoundException> {
            view.provider<Any>()
        }

        shouldThrow<EntryNotFoundException> {
            view.provider<Any, Any>("")
        }

        view.providerOrNull<Int>().apply {
            shouldBeInstanceOf<Provider<Int>>()
            this?.invoke().shouldBe(42)
        }

        view.providerOrNull<Int>("other").apply {
            shouldBeInstanceOf<Provider<Int>>()
            this?.invoke().shouldBe(5)
        }

        view.providerOrNull<Int, String>(7).apply {
            shouldBeInstanceOf<Provider<Int>>()
            this?.invoke().shouldBe("7")
        }

        view.providerOrNull<Int, String>(7, "other").apply {
            shouldBeInstanceOf<Provider<Int>>()
            this?.invoke().shouldBe("14")
        }

        view.providerOrNull<Any>().shouldBe(null)
        view.providerOrNull<Any, Any>("").shouldBe(null)

        view.providersOfType<Int>().map { it.invoke() }.shouldContainAll(42, 5)
    }

    @Test
    fun factory_methods() {
        val view = View(context)

        view.factory<Int, String>().apply {
            shouldBeInstanceOf<Factory<Int, String>>()
            invoke(42).shouldBe("42")
        }

        view.factory<Int, String>("other").apply {
            shouldBeInstanceOf<Factory<Int, String>>()
            invoke(7).shouldBe("14")
        }

        shouldThrow<EntryNotFoundException> {
            view.factory<Any, Any>()
        }

        view.factoryOrNull<Int, String>().apply {
            shouldBeInstanceOf<Factory<Int, String>>()
            this?.invoke(42).shouldBe("42")
        }

        view.factoryOrNull<Int, String>("other").apply {
            shouldBeInstanceOf<Factory<Int, String>>()
            this?.invoke(7).shouldBe("14")
        }

        view.factoryOrNull<Any, Any>().shouldBe(null)
    }

}
