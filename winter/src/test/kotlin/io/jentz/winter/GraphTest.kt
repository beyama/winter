package io.jentz.winter

import com.nhaarman.mockito_kotlin.*
import io.jentz.winter.plugin.Plugin
import io.jentz.winter.plugin.SimplePlugin
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.collections.shouldContainAll
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.matchers.types.shouldNotBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GraphTest {

    private val emptyComponent = emptyComponent()

    private val emptyGraph = emptyGraph()

    private val instance = Any()

    private lateinit var executor: ExecutorService

    private val plugin: Plugin = mock()

    @BeforeAll
    fun beforeAll() {
        executor = Executors.newFixedThreadPool(8)
    }

    @AfterAll
    fun afterAll() {
        executor.shutdown()
    }

    @BeforeEach
    fun beforeEach() {
        reset(plugin)
        Winter.plugins.unregisterAll()
        Winter.plugins.register(plugin)
    }

    @AfterEach
    fun afterEach() {
        Winter.plugins.unregisterAll()
    }

    @Nested
    @DisplayName("Prototype scope")
    inner class PrototypeScope {

        @Test
        fun `should return instance returned by factory function`() {
            graph {
                prototype { instance }
            }.instance<Any>().shouldBeSameInstanceAs(instance)
        }

        @Test
        fun `should invoke factory function for every lookup`() {
            var count = 0
            val graph = graph { prototype { count += 1; count } }
            (1..5).forEach {
                graph.instance<Int>()
                count.shouldBe(it)
            }
        }

        @Test
        fun `should invoke post construct callback with instance`() {
            var called = false
            graph {
                prototype(postConstruct = {
                    it.shouldBeSameInstanceAs(instance)
                    called = true
                }) { instance }
            }.instance<Any>()
            called.shouldBeTrue()
        }

        @Test
        fun `should run post construct plugins`() {
            val graph = graph { prototype { instance } }
            graph.instance<Any>()
            verify(plugin, times(1)).postConstruct(graph, Scope.Prototype, Unit, instance)
        }

        @Test
        fun `should allow resolution of nested dependencies`() {
            graph {
                prototype { Heater() }
                prototype<Pump> { Thermosiphon(instance()) }
                prototype { CoffeeMaker(instance(), instance()) }
            }.instance<CoffeeMaker>()
        }

        @Test
        fun `should be thread safe`() {
            val graph = graph {
                (0..100).forEach { value ->
                    prototype(qualifier = value) { value }
                }
            }

            (0..100).map {
                executor.submit { graph.instance<Int>(qualifier = it).shouldBe(it) }
            }.forEach { it.get() }
        }

    }

    @Nested
    @DisplayName("Singleton scope")
    inner class SingletonScope {

        private val testComponent = component {
            singleton { instance }
            singleton { Parent(instance()) }
            singleton(
                postConstruct = { it.parent = instance() },
                dispose = { it.parent = null }
            ) { Child() }
        }

        @Test
        fun `should return instance returned by factory function`() {
            testComponent.init().instance<Any>().shouldBeSameInstanceAs(instance)
        }

        @Test
        fun `should invoke factory function only on the first lookup`() {
            var count = 0
            val graph = graph { singleton { count += 1; count } }
            (1..5).forEach { graph.instance<Int>().shouldBe(1) }
        }

        @Test
        fun `should invoke post construct callback with instance`() {
            val parent = testComponent.init().instance<Parent>()
            parent.child.parent.shouldBeSameInstanceAs(parent)
        }

        @Test
        fun `should run post construct plugins`() {
            val graph = graph { singleton { instance } }
            graph.instance<Any>()
            verify(plugin, times(1)).postConstruct(graph, Scope.Singleton, Unit, instance)
        }

        @Test
        fun `should invoke dispose callback with instance`() {
            val graph = testComponent.init()
            val parent: Parent = graph.instance()
            val child: Child = graph.instance()
            expectValueToChange(parent, null, child::parent) {
                graph.dispose()
            }
        }

        @Test
        fun `#eagerSingleton should be a singleton but created as soon as the graph gets initialized`() {
            var initialized = false
            val graph = graph { eagerSingleton { initialized = true; instance } }
            initialized.shouldBeTrue()
            graph.service<Unit, Any>(typeKey<Any>()).shouldBeInstanceOf<BoundSingletonService<*>>()
        }

        @Test
        fun `should allow resolution of nested dependencies`() {
            graph {
                singleton { Heater() }
                singleton<Pump> { Thermosiphon(instance()) }
                singleton { CoffeeMaker(instance(), instance()) }
            }.let {
                it.instance<CoffeeMaker>().heater.shouldBeSameInstanceAs(it.instance<Heater>())
            }
        }

        @Test
        fun `should be thread safe`() {
            val graph = graph {
                (0..100).forEach { value ->
                    singleton(qualifier = value) { value }
                }
            }

            (0..100).map {
                executor.submit { graph.instance<Int>(qualifier = it).shouldBe(it) }
            }.forEach { it.get() }
        }

    }

    @Nested
    @DisplayName("Reference scope (WeakSingleton and SoftSingleton)")
    inner class ReferenceScope {

        @Test
        fun `should return instance returned by factory function`() {
            graph {
                reference { instance }
            }.instance<Any>().shouldBeSameInstanceAs(instance)
        }

        @Test
        fun `should invoke factory function only when reference is null`() {
            var count = 0
            val graph = graph { reference { count += 1; count } }
            val service = graph.service<Unit, Int>(typeKey<Int>()) as BoundReferenceService
            service.instance.shouldBe(UNINITIALIZED_VALUE)
            (1..5).forEach { graph.instance<Int>() }
            service.instance.shouldBe(1)
            service.instance = UNINITIALIZED_VALUE
            (1..5).forEach { graph.instance<Int>() }
            service.instance.shouldBe(2)
        }

        @Test
        fun `should invoke post construct callback with instance`() {
            val graph = graph { reference { "test" } }
            val service = graph.service<Unit, String>(typeKey<String>()) as BoundReferenceService
            graph.instance<String>()
            service.postConstructCalledCount.shouldBe(1)
            service.postConstructLastArguments.shouldBe(Unit to "test")
        }

        @Test
        fun `should allow resolution of nested dependencies`() {
            graph {
                reference { Heater() }
                reference<Pump> { Thermosiphon(instance()) }
                reference { CoffeeMaker(instance(), instance()) }
            }.let {
                it.instance<CoffeeMaker>().heater.shouldBeSameInstanceAs(it.instance<Heater>())
            }
        }

        @Test
        fun `#softSingleton should return instance returned by factory function`() {
            graph {
                softSingleton { instance }
            }.instance<Any>().shouldBeSameInstanceAs(instance)
        }

        @Test
        fun `#softSingleton should invoke post construct callback with instance`() {
            var postConstructCalledCount = 0
            val graph = graph {
                softSingleton(postConstruct = {
                    it.shouldBeSameInstanceAs(instance)
                    postConstructCalledCount += 1
                }) { instance }
            }
            graph.instance<Any>()
            postConstructCalledCount.shouldBe(1)
        }

        @Test
        fun `#softSingleton should run post construct plugins`() {
            val graph = graph { softSingleton { instance } }
            graph.instance<Any>()
            verify(plugin, times(1)).postConstruct(graph, Scope.SoftSingleton, Unit, instance)
        }

        @Test
        fun `#softSingleton should be thread safe`() {
            val graph = graph {
                (0..100).forEach { value ->
                    softSingleton(qualifier = value) { value }
                }
            }

            (0..100).map {
                executor.submit { graph.instance<Int>(qualifier = it).shouldBe(it) }
            }.forEach { it.get() }
        }

        @Test
        fun `#weakSingleton should return instance returned by factory`() {
            graph {
                weakSingleton { instance }
            }.instance<Any>().shouldBeSameInstanceAs(instance)
        }

        @Test
        fun `#weakSingleton should invoke post construct callback with instance`() {
            var postConstructCalledCount = 0
            val graph = graph {
                weakSingleton(postConstruct = {
                    it.shouldBeSameInstanceAs(instance)
                    postConstructCalledCount += 1
                }) { instance }
            }
            graph.instance<Any>()
            postConstructCalledCount.shouldBe(1)
        }

        @Test
        fun `#weakSingleton should run post construct plugins`() {
            val graph = graph { weakSingleton { instance } }
            graph.instance<Any>()
            verify(plugin, times(1)).postConstruct(graph, Scope.WeakSingleton, Unit, instance)
        }

        @Test
        fun `#weakSingleton should be thread safe`() {
            val graph = graph {
                (0..100).forEach { value ->
                    weakSingleton(qualifier = value) { value }
                }
            }

            (0..100).map {
                executor.submit { graph.instance<Int>(qualifier = it).shouldBe(it) }
            }.forEach { it.get() }
        }

    }

    @Nested
    @DisplayName("Factory scope")
    inner class FactoryScope {

        @Test
        fun `should return instance returned by the factory`() {
            graph {
                factory { i: Int -> i.toString() }
            }.instance<Int, String>(42).shouldBe("42")
        }

        @Test
        fun `should invoke factory for every lookup`() {
            var count = 0
            val graph = graph {
                factory { i: Int ->
                    count += 1
                    i.toString()
                }
            }
            (1..5).forEach {
                graph.instance<Int, String>(it).shouldBe(it.toString())
            }

        }

        @Test
        fun `should invoke postConstruct callback with argument and instance`() {
            var called = false
            graph {
                factory(
                    postConstruct = { a, r ->
                        called = true
                        a.shouldBe(42)
                        r.shouldBe("42")
                    }
                ) { i: Int -> i.toString() }
            }.instance<Int, String>(42)
            called.shouldBeTrue()
        }

        @Test
        fun `should run post construct plugins`() {
            val graph = graph { factory { i: Int -> i.toString() } }
            graph.instance<Int, String>(42)
            verify(plugin, times(1)).postConstruct(graph, Scope.PrototypeFactory, 42, "42")
        }

        @Test
        fun `should allow resolution of nested dependencies`() {
            graph {
                prototype { Heater() }
                factory { type: String ->
                    when (type) {
                        "rotary" -> RotaryPump()
                        "thermo" -> Thermosiphon(instance())
                        else -> throw IllegalArgumentException()
                    }
                }
                factory { pumpType: String ->
                    CoffeeMaker(instance(), instance(argument = pumpType))
                }
            }.instance<String, CoffeeMaker>("thermo").pump.shouldBeInstanceOf<Thermosiphon>()
        }

        @Test
        fun `should be thread safe`() {
            val graph = graph {
                (0..100).forEach { value ->
                    factory<Int, String>(qualifier = value) { i -> i.toString() }
                }
            }

            (0..100).map {
                executor.submit {
                    graph.instance<Int, String>(it, qualifier = it).shouldBe(it.toString())
                }
            }.forEach { it.get() }
        }

    }

    @Nested
    @DisplayName("Multiton scope")
    inner class MultitonScope {

        @Test
        fun `should return instance returned by factory`() {
            graph {
                multiton { c: Color -> Widget(c) }
            }.instance<Color, Widget>(Color.BLUE).color.shouldBe(Color.BLUE)
        }

        @Test
        fun `should invoke factory only on the first lookup with new argument`() {
            val graph = graph {
                multiton { c: Color -> Widget(c) }
            }
            val a: Widget = graph.instance(argument = Color.BLUE)
            val b: Widget = graph.instance(argument = Color.BLUE)
            val c: Widget = graph.instance(argument = Color.RED)
            a.shouldBeSameInstanceAs(b)
            c.shouldNotBeSameInstanceAs(b)
            c.color.shouldBe(Color.RED)
        }

        @Test
        fun `should invoke post construct callback with instance and argument`() {
            var called = false
            graph {
                multiton(
                    postConstruct = { a, r ->
                        called = true
                        a.shouldBe(Color.GREEN)
                        r.shouldBeInstanceOf<Widget>()
                    }
                ) { c: Color -> Widget(c) }
            }.instance<Color, Widget>(Color.GREEN)
            called.shouldBeTrue()
        }

        @Test
        fun `should run post construct plugins`() {
            val graph = graph { multiton { i: Int -> i.toString() } }
            graph.instance<Int, String>(42)
            verify(plugin, times(1)).postConstruct(graph, Scope.MultitonFactory, 42, "42")
        }

        @Test
        fun `should invoke dispose callback with instance and argument for each constructed instance`() {
            var count = 0
            val graph = graph {
                multiton(
                    dispose = { a, r ->
                        count += 1
                        a.shouldBeInstanceOf<Color>()
                        r.shouldBeInstanceOf<Widget>()
                    }
                ) { c: Color -> Widget(c) }
            }
            graph.instance<Color, Widget>(Color.RED)
            graph.instance<Color, Widget>(Color.GREEN)
            graph.instance<Color, Widget>(Color.BLUE)
            expectValueToChange(0, 3, { count }) { graph.dispose() }
        }

        @Test
        fun `should allow resolution of nested dependencies`() {
            graph {
                prototype { Heater() }
                multiton { type: String ->
                    when (type) {
                        "rotary" -> RotaryPump()
                        "thermo" -> Thermosiphon(instance())
                        else -> throw IllegalArgumentException()
                    }
                }
                factory { pumpType: String ->
                    CoffeeMaker(instance(), instance(argument = pumpType))
                }
            }.let {
                it.instance<String, CoffeeMaker>("thermo").pump
                    .shouldBeSameInstanceAs(it.instance<String, Pump>("thermo"))
            }
        }

        @Test
        fun `should be thread safe`() {
            val graph = graph {
                (0..100).forEach { value ->
                    multiton<Int, String>(qualifier = value) { i -> i.toString() }
                }
            }

            (0..100).map {
                executor.submit {
                    graph.instance<Int, String>(it, qualifier = it).shouldBe(it.toString())
                }
            }.forEach { it.get() }
        }

    }

    @Nested
    @DisplayName("Alias service")
    inner class AliasService {

        @Test
        fun `should bind aliased service only once`() {
            val graph = graph {
                prototype { Heater() }
                prototype { Thermosiphon(instance()) }
                alias(typeKey<Thermosiphon>(), typeKey<Pump>())
            }
            graph.service<Unit, Pump>(typeKey<Pump>())
                .shouldBeSameInstanceAs(graph.service<Unit, Thermosiphon>(typeKey<Thermosiphon>()))
        }

        @Test
        fun `should allow aliases to aliases`() {
            graph {
                prototype { Heater() }
                prototype { Thermosiphon(instance()) }
                alias(typeKey<Thermosiphon>(), typeKey<Pump>())
                alias(typeKey<Pump>(), typeKey<Any>())
            }.instance<Any>().shouldBeInstanceOf<Thermosiphon>()
        }

        @Test
        fun `#bind should throw proper exception when aliased service doesn't exist anymore`() {
            shouldThrow<WinterException> {
                graph {
                    prototype { Heater() }
                    prototype { Thermosiphon(instance()) }
                    alias(typeKey<Thermosiphon>(), typeKey<Pump>())
                    remove(typeKey<Thermosiphon>())
                }.service<Unit, Pump>(typeKey<Pump>())
            }.message.shouldBe("Error resolving alias `${typeKey<Pump>()}` pointing to `${typeKey<Thermosiphon>()}`.")
        }

    }

    @Nested
    @DisplayName("#instance")
    inner class InstanceMethod {

        @Test
        fun `should resolve instance by class`() {
            graph {
                prototype { "string" }
            }.instance<String>().shouldBe("string")
        }

        @Test
        fun `should resolve instance by generic class`() {
            graph {
                prototype(generics = true) { mapOf(1 to "1") }
            }.instance<Map<Int, String>>(generics = true).shouldBe(mapOf(1 to "1"))
        }

        @Test
        fun `should resolve instance with qualifier`() {
            graph {
                prototype("a") { "a" }
                prototype("b") { "b" }
            }.instance<String>(qualifier = "b").shouldBe("b")
        }

        @Test
        fun `should resolve instance from factory`() {
            graph {
                factory { i: Int -> i.toString() }
            }.instance<Int, String>(42).shouldBe("42")
        }

        @Test
        fun `should resolve instance from factory with qualifier`() {
            graph {
                factory("a") { i: Int -> i.toString() }
                factory("b") { i: Int -> (i * 2).toString() }
            }.instance<Int, String>(21, "b").shouldBe("42")
        }

        @Test
        fun `should resolve instance from factory with generics`() {
            graph {
                factory(generics = true) { i: Int -> mapOf(i to i.toString()) }
            }.instance<Int, Map<Int, String>>(1, generics = true).shouldBe(mapOf(1 to "1"))
        }

        @Test
        fun `should throw an exception if dependency doesn't exist`() {
            shouldThrow<EntryNotFoundException> { emptyGraph.instance() }
        }

        @Test
        fun `should throw an exception when graph is disposed`() {
            graph { prototype { "string" } }.apply {
                dispose()
                shouldThrow<WinterException> { instance() }
            }
        }

    }

    @Nested
    @DisplayName("#instancOrNull")
    inner class InstanceOrNullMethod {

        @Test
        fun `should resolve instance by class`() {
            graph {
                prototype { "string" }
            }.instanceOrNull<String>().shouldBe("string")
        }

        @Test
        fun `should resolve instance by generic class`() {
            graph {
                prototype(generics = true) { mapOf(1 to "1") }
            }.instanceOrNull<Map<Int, String>>(generics = true).shouldBe(mapOf(1 to "1"))
        }

        @Test
        fun `should resolve instance with qualifier`() {
            graph {
                prototype("a") { "a" }
                prototype("b") { "b" }
            }.instanceOrNull<String>(qualifier = "b").shouldBe("b")
        }

        @Test
        fun `should resolve instance from factory`() {
            graph {
                factory { i: Int -> i.toString() }
            }.instanceOrNull<Int, String>(42).shouldBe("42")
        }

        @Test
        fun `should resolve instance from factory with qualifier`() {
            graph {
                factory("a") { i: Int -> i.toString() }
                factory("b") { i: Int -> (i * 2).toString() }
            }.instanceOrNull<Int, String>(21, "b").shouldBe("42")
        }

        @Test
        fun `should resolve instance from factory with generics`() {
            graph {
                factory(generics = true) { i: Int -> mapOf(i to i.toString()) }
            }.instanceOrNull<Int, Map<Int, String>>(1, generics = true).shouldBe(mapOf(1 to "1"))
        }

        @Test
        fun `should return null if dependency doesn't exist`() {
            emptyGraph.instanceOrNull<Any>().shouldBe(null)
        }

        @Test
        fun `should return null if factory doesn't exist`() {
            emptyGraph.instanceOrNull<Int, String>(1).shouldBe(null)
        }

        @Test
        fun `should throw an exception when graph is disposed`() {
            graph { prototype { "string" } }.apply {
                dispose()
                shouldThrow<WinterException> { instanceOrNull() }
            }
        }

    }

    @Nested
    @DisplayName("#provider")
    inner class ProviderMethod {

        @Test
        fun `should resolve provider by class`() {
            graph {
                prototype { "string" }
            }.provider<String>().invoke().shouldBe("string")
        }

        @Test
        fun `should resolve provider by generic class`() {
            graph {
                prototype(generics = true) { mapOf(1 to "1") }
            }.provider<Map<Int, String>>(generics = true).invoke().shouldBe(mapOf(1 to "1"))
        }

        @Test
        fun `should resolve provider with qualifier`() {
            graph {
                prototype("a") { "a" }
                prototype("b") { "b" }
            }.provider<String>(qualifier = "b").invoke().shouldBe("b")
        }

        @Test
        fun `should resolve provider from factory`() {
            graph {
                factory { i: Int -> i.toString() }
            }.provider<Int, String>(42).invoke().shouldBe("42")
        }

        @Test
        fun `should resolve provider from factory with qualifier`() {
            graph {
                factory("a") { i: Int -> i.toString() }
                factory("b") { i: Int -> (i * 2).toString() }
            }.provider<Int, String>(21, "b").invoke().shouldBe("42")
        }

        @Test
        fun `should resolve instance from factory with generics`() {
            graph {
                factory(generics = true) { i: Int -> mapOf(i to i.toString()) }
            }.provider<Int, Map<Int, String>>(1, generics = true).invoke().shouldBe(mapOf(1 to "1"))
        }

        @Test
        fun `should throw an exception if dependency doesn't exist`() {
            shouldThrow<EntryNotFoundException> { emptyGraph.provider<Any>() }
        }

        @Test
        fun `should throw an exception when graph is disposed`() {
            graph { prototype { "string" } }.apply {
                dispose()
                shouldThrow<WinterException> { provider<Any>() }
            }
        }

        @Test
        fun `should postpone evaluation until provider is called`() {
            var counter = 0
            val provider = graph { prototype { counter += 1; counter } }.provider<Int>()
            counter.shouldBe(0)
            provider.invoke().shouldBe(1)
            provider.invoke().shouldBe(2)
        }

    }

    @Nested
    @DisplayName("#providerOrNull")
    inner class ProviderOrNullMethod {

        @Test
        fun `should resolve provider by class`() {
            graph {
                prototype { "string" }
            }.providerOrNull<String>()?.invoke().shouldBe("string")
        }

        @Test
        fun `should resolve provider by generic class`() {
            graph {
                prototype(generics = true) { mapOf(1 to "1") }
            }.providerOrNull<Map<Int, String>>(generics = true)?.invoke().shouldBe(mapOf(1 to "1"))
        }

        @Test
        fun `should resolve provider with qualifier`() {
            graph {
                prototype("a") { "a" }
                prototype("b") { "b" }
            }.providerOrNull<String>(qualifier = "b")?.invoke().shouldBe("b")
        }

        @Test
        fun `should resolve provider from factory`() {
            graph {
                factory { i: Int -> i.toString() }
            }.providerOrNull<Int, String>(42)?.invoke().shouldBe("42")
        }

        @Test
        fun `should resolve provider from factory with qualifier`() {
            graph {
                factory("a") { i: Int -> i.toString() }
                factory("b") { i: Int -> (i * 2).toString() }
            }.providerOrNull<Int, String>(21, "b")?.invoke().shouldBe("42")
        }

        @Test
        fun `should resolve instance from factory with generics`() {
            graph {
                factory(generics = true) { i: Int -> mapOf(i to i.toString()) }
            }.providerOrNull<Int, Map<Int, String>>(1, generics = true)?.invoke()
                .shouldBe(mapOf(1 to "1"))
        }

        @Test
        fun `should return null if dependency doesn't exist`() {
            emptyGraph.providerOrNull<Any>().shouldBe(null)
        }

        @Test
        fun `should throw an exception when graph is disposed`() {
            graph { prototype { "string" } }.apply {
                dispose()
                shouldThrow<WinterException> { providerOrNull<Any>() }
            }
        }

        @Test
        fun `should postpone evaluation until provider is called`() {
            var counter = 0
            val provider = graph { prototype { counter += 1; counter } }.providerOrNull<Int>()!!
            counter.shouldBe(0)
            provider.invoke().shouldBe(1)
            provider.invoke().shouldBe(2)
        }

    }

    @Nested
    @DisplayName("#factory")
    inner class FactoryMethod {

        @Test
        fun `should resolve factory by class`() {
            graph {
                factory { i: Int -> i.toString() }
            }.factory<Int, String>().invoke(5).shouldBe("5")
        }

        @Test
        fun `should resolve factory by generic class`() {
            graph {
                factory(generics = true) { i: Int -> mapOf(i to i.toString()) }
            }.factory<Int, Map<Int, String>>(generics = true).invoke(5).shouldBe(mapOf(5 to "5"))
        }

        @Test
        fun `should resolve factory with qualifier`() {
            graph {
                factory("a") { i: Int -> i.toString() }
                factory("b") { i: Int -> (i * 2).toString() }
            }.factory<Int, String>(qualifier = "b").invoke(5).shouldBe("10")
        }

        @Test
        fun `should throw an exception if factory doesn't exist`() {
            shouldThrow<EntryNotFoundException> { emptyGraph.factory<Int, String>() }
        }

        @Test
        fun `should throw an exception when graph is disposed`() {
            graph { factory { i: Int -> i.toString() } }.apply {
                dispose()
                shouldThrow<WinterException> { factory<Int, String>() }
            }
        }

    }

    @Nested
    @DisplayName("#factoryOrNull")
    inner class FactoryOrNullMethod {

        @Test
        fun `should resolve factory by class`() {
            graph {
                factory { i: Int -> i.toString() }
            }.factoryOrNull<Int, String>()?.invoke(5).shouldBe("5")
        }

        @Test
        fun `should resolve factory by generic class`() {
            graph {
                factory(generics = true) { i: Int -> mapOf(i to i.toString()) }
            }.factoryOrNull<Int, Map<Int, String>>(generics = true)?.invoke(5)
                .shouldBe(mapOf(5 to "5"))
        }

        @Test
        fun `should resolve factory with qualifier`() {
            graph {
                factory("a") { i: Int -> i.toString() }
                factory("b") { i: Int -> (i * 2).toString() }
            }.factoryOrNull<Int, String>(qualifier = "b")?.invoke(5).shouldBe("10")
        }

        @Test
        fun `should return null if factory doesn't exist`() {
            emptyGraph.factoryOrNull<Int, String>().shouldBe(null)
        }

        @Test
        fun `should throw an exception when graph is disposed`() {
            graph { factory { i: Int -> i.toString() } }.apply {
                dispose()
                shouldThrow<WinterException> { factoryOrNull<Int, String>() }
            }
        }

    }

    @Nested
    @DisplayName("#*OfType methods")
    inner class OfTypeMethods {

        @Test
        fun `#providersOfType should return a set of providers of a given type`() {
            val graph = graph {
                prototype("something else") { Any() }
                prototype("a") { "a" }
                prototype("b") { "b" }
                prototype("c") { "c" }
            }

            val providers = graph.providersOfType<String>()
            providers.shouldHaveSize(3)
            providers.map { it() }.shouldContainAll("a", "b", "c")
        }

        @Test
        fun `#instancesOfType should return a set of instances of given type`() {
            val graph = graph {
                prototype("something else") { Any() }
                prototype("a") { "a" }
                prototype("b") { "b" }
                prototype("c") { "c" }
            }

            val instances = graph.instancesOfType<String>()
            instances.shouldHaveSize(3)
            instances.shouldContainAll("a", "b", "c")
        }

    }

    @Nested
    @DisplayName("#inject method")
    inner class InjectMethod {

        var calledForService = false
        var calledForExtendedService = false

        val graph = graph {
            membersInjector {
                object : MembersInjector<Service> {
                    override fun injectMembers(graph: Graph, target: Service) {
                        calledForService = true
                    }
                }
            }
            membersInjector {
                object : MembersInjector<ExtendedService> {
                    override fun injectMembers(graph: Graph, target: ExtendedService) {
                        calledForExtendedService = true
                    }
                }
            }
        }

        @BeforeEach
        fun beforeEach() {
            calledForService = false
            calledForExtendedService = false
        }

        @Test
        fun `#inject should resolve and call members injector for type`() {
            val service = Service()
            graph.inject(service)
            calledForService.shouldBeTrue()
        }

        @Test
        fun `#inject with injectSuperClasses set to false should only resolve and call members injector for type`() {
            val service = ExtendedService()
            graph.inject(service)
            calledForService.shouldBeFalse()
            calledForExtendedService.shouldBeTrue()
        }

        @Test
        fun `#inject with injectSuperClasses set to true should resolve and call members injector for type and supertype`() {
            val service = ExtendedService()
            graph.inject(service, true)
            calledForService.shouldBeTrue()
            calledForExtendedService.shouldBeTrue()
        }

    }

    @Nested
    @DisplayName("Properties")
    inner class Properties {

        @Test
        fun `#parent should return null when no parent graph exists`() {
            val graph = graph { subcomponent("child") {} }
            graph.parent.shouldBeNull()
        }

        @Test
        fun `#parent should return parent graph`() {
            val parent = graph { subcomponent("child") {} }
            val child = parent.createChildGraph("child")
            child.parent.shouldBeSameInstanceAs(parent)
        }

        @Test
        fun `#parent should throw an exception when graph is disposed`() {
            val child = graph { subcomponent("child") {} }.createChildGraph("child")
            shouldThrow<WinterException> {
                child.dispose()
                child.parent
            }.message.shouldBe("Graph is already disposed.")
        }

        @Test
        fun `#component should return backing component`() {
            Winter.plugins.unregisterAll() // otherwise it will derive the component
            val parent = graph { subcomponent("child") {} }
            val child = parent.createChildGraph("child")
            child.component.shouldBeSameInstanceAs(parent.component.subcomponent("child"))
        }

        @Test
        fun `#component should throw an exception when graph is disposed`() {
            val child = graph { subcomponent("child") {} }.createChildGraph("child")
            shouldThrow<WinterException> {
                child.dispose()
                child.component
            }.message.shouldBe("Graph is already disposed.")
        }

    }

    @Nested
    @DisplayName("initialization")
    inner class Initialisation {

        val component = component { subcomponent("test") {} }

        @Test
        fun `should initialize graph with given component`() {
            Graph(WinterApplication(),null, emptyComponent, null, null)
                .component.shouldBe(emptyComponent)
        }

        @Test
        fun `should run plugins`() {
            val parent = graph { }
            verify(plugin, only()).initializingComponent(isNull(), any())
            reset(plugin)
            Graph(Winter, parent, emptyComponent, null, null)
            verify(plugin, only()).initializingComponent(same(parent), any())
        }

        @Test
        fun `should derive component when builder block is given`() {
            val graph = Graph(Winter,null, emptyComponent, null) { constant(42) }
            graph.instance<Int>().shouldBe(42)
        }

        @Test
        fun `#initSubcomponent should dervie component when builder block is given`() {
            val graph = component.init().createChildGraph("test") { constant(42) }
            graph.instance<Int>().shouldBe(42)
        }

        @Test
        fun `#initSubcomponent should pass WinterApplication to new graph`() {
            val testApp = WinterApplication()
            component.init(testApp)
                .createChildGraph("test")
                .application.shouldBeSameInstanceAs(testApp)
        }

    }

    @Nested
    @DisplayName("#dispose and #isDisposed")
    inner class DisposeMethod {

        @Test
        fun `#dispose should mark the graph as disposed`() {
            val graph = graph {}
            expectValueToChange(false, true, graph::isDisposed) {
                graph.dispose()
            }
        }

        @Test
        fun `subsequent calls to #dispose should be ignored`() {
            val graph = graph {}
            repeat(3) { graph.dispose() }
            verify(plugin, times(1)).graphDispose(graph)
        }

        @Test
        fun `#dispose should run graph dispose plugins before marking graph as disposed`() {
            var called = false
            Winter.plugins.register(object : SimplePlugin() {
                override fun graphDispose(graph: Graph) {
                    called = true
                    graph.isDisposed.shouldBeFalse()
                }
            })
            val graph = graph {}
            graph.dispose()
            called.shouldBeTrue()
        }

        @Test
        fun `#dispose should ignore a call to dispose from plugin`() {
            Winter.plugins.register(object : SimplePlugin() {
                override fun graphDispose(graph: Graph) {
                    graph.dispose()
                }
            })
            graph {}.dispose()
            // no StackOverflowError here
        }

    }

    @Nested
    @DisplayName("Cyclic dependencies")
    inner class CyclicDependencies {

        @Test
        fun `should detect cyclic dependencies`() {
            shouldThrow<CyclicDependencyException> {
                graph {
                    singleton { Parent(instance()) }
                    singleton { Child().apply { parent = instance() } }
                }.instance<Parent>()
            }
        }

        @Test
        fun `should detect cyclic dependencies for self referencing factory`() {
            shouldThrow<CyclicDependencyException> {
                graph {
                    prototype {
                        instance<Any>()
                        Any()
                    }
                }.instance()
            }
        }

        @Test
        fun `should detect cyclic dependencies for factory`() {
            shouldThrow<CyclicDependencyException> {
                graph {
                    factory { arg: Int -> factory<Int, Int>().invoke(arg) }
                }.factory<Int, Int>().invoke(42)
            }
        }

    }

    @Nested
    inner class ChildManagement {

        private val component = component {
            subcomponent("presentation") {
                singleton { listOf<String>() }

                subcomponent("view") {
                    singleton { mapOf<String, String>() }
                }
            }
        }

        private lateinit var root: Graph

        @BeforeEach
        fun beforeEach() {
            root = component.init()
        }

        @Test
        fun `#openChildGraph should throw an exception if graph with identifier already exists`() {
            root.openChildGraph("presentation")

            shouldThrow<WinterException> {
                root.openChildGraph("presentation")
            }.message.shouldBe("Cannot open graph with identifier `presentation` because it is already open.")
        }

        @Test
        fun `#openChildGraph with identifier should throw an exception if graph with identifier already exists`() {
            root.openChildGraph("presentation", "foo")

            shouldThrow<WinterException> {
                root.openChildGraph("presentation", "foo")
            }.message.shouldBe("Cannot open graph with identifier `foo` because it is already open.")
        }

        @Test
        fun `#openChildGraph should initialize and return subcomponent by qualifier`() {
            root.openChildGraph("presentation").component.qualifier.shouldBe("presentation")
        }

        @Test
        fun `#openChildGraph should should pass the builder block to the subcomponent init method`() {
            root.openChildGraph("presentation") {
                constant(42)
            }.instance<Int>().shouldBe(42)
        }

        @Test
        fun `#openChildGraph with identifier should initialize subcomponent and register it under the given identifier`() {
            val graph = root.openChildGraph("presentation", identifier = "foo")
            graph.component.qualifier.shouldBe("presentation")
            root.instance<Graph>("foo").shouldBeSameInstanceAs(graph)
        }

        @Test
        fun `#closeChildGraph should dispose and remove child graph`() {
            val graph = root.openChildGraph("presentation")
            root.closeChildGraph("presentation")

            graph.isDisposed.shouldBeTrue()
            root.instanceOrNull<Graph>("presentation").shouldBeNull()
        }

        @Test
        fun `#closeChildGraph should throw an exception when graph doesn't exist`() {
            shouldThrow<WinterException> {
                root.closeChildGraph("foo")
            }.message.shouldBe("Child graph with identifier `foo` doesn't exist.")
        }

        @Test
        fun `#dispose should dispose managed children`() {
            val presentation = root.openChildGraph("presentation")
            val view = presentation.openChildGraph("view")

            presentation.dispose()

            presentation.isDisposed.shouldBeTrue()
            view.isDisposed.shouldBeTrue()
        }

        @Test
        fun `#dispose of child graphs should not lead to concurrent modification exception`() {
            val presentation = root.openChildGraph("presentation")
            val view = presentation.openChildGraph("view")

            // we need more than one service in our registry to get the exception when unregistering
            // of children is not prevented during dispose
            presentation.instance<List<*>>()
            view.instance<Map<*, *>>()

            root.dispose()
        }

    }

    @Nested
    @DisplayName("Exception while resolving")
    inner class Exceptions {

        @Test
        fun `should have the right message when dependency wasn't found with one level of nesting`() {
            shouldThrow<DependencyResolutionException> {
                graph {
                    prototype { CoffeeMaker(instance(), instance()) }
                }.instance<CoffeeMaker>()
            }.message.shouldBe(
                "Error while resolving dependency with key: " +
                        "ClassTypeKey(class io.jentz.winter.CoffeeMaker qualifier = null) " +
                        "reason: could not find dependency with key " +
                        "ClassTypeKey(class io.jentz.winter.Heater qualifier = null)"
            )
        }

        @Test
        fun `should have the right message when dependency wasn't found with two levels of nesting`() {
            shouldThrow<DependencyResolutionException> {
                graph {
                    prototype { Heater() }
                    prototype<Pump> { Thermosiphon(instance("doesn't exist")) }
                    prototype { CoffeeMaker(instance(), instance()) }
                }.instance<CoffeeMaker>()
            }.message.shouldBe(
                "Error while resolving dependency with key: " +
                        "ClassTypeKey(interface io.jentz.winter.Pump qualifier = null) " +
                        "reason: could not find dependency with key " +
                        "ClassTypeKey(class io.jentz.winter.Heater qualifier = doesn't exist)"
            )
        }

        @Test
        fun `should have the right message when factory of dependency throws an exception`() {
            shouldThrow<DependencyResolutionException> {
                graph {
                    prototype { Heater() }
                    prototype<Pump> { throw Error("Boom!") }
                    prototype { CoffeeMaker(instance(), instance()) }
                }.instance<CoffeeMaker>()
            }.let {
                it.message.shouldBe(
                    "Factory of dependency with key " +
                            "ClassTypeKey(interface io.jentz.winter.Pump qualifier = null) " +
                            "threw an exception on invocation."
                )
                it.cause?.message.shouldBe("Boom!")
            }
        }

        @Test
        fun `should trigger post callbacks for dependencies that are resolved without error`() {
            var called = false
            shouldThrow<DependencyResolutionException> {
                graph {
                    prototype(
                        postConstruct = { called = true }
                    ) { Heater() }
                    prototype<Pump> { throw Error("Boom!") }
                    prototype { CoffeeMaker(instance(), instance()) }
                }.instance<CoffeeMaker>()
            }
            called.shouldBeTrue()
        }

        @Test
        fun `should not result in a invalid state`() {
            val graph = graph {
                prototype { Heater() }
                prototype<Pump> { throw Error("Boom!") }
                prototype { CoffeeMaker(instance(), instance()) }
            }
            shouldThrow<DependencyResolutionException> { graph.instance<CoffeeMaker>() }
            graph.instance<Heater>().shouldBeInstanceOf<Heater>()
        }

    }
}