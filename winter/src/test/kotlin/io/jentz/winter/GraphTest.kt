package io.jentz.winter

import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.collections.shouldContainAll
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.matchers.types.shouldNotBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.*
import java.util.concurrent.atomic.AtomicInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GraphTest {

    private val emptyGraph = graph {}

    private val instance = Any()

    @BeforeEach
    fun beforeEach() {
        WinterPlugins.resetPostConstructPlugins()
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
            var called = false
            val testGraph = graph { prototype { instance } }
            WinterPlugins.addPostConstructPlugin { graph, scope, argument, i ->
                graph.shouldBeSameInstanceAs(testGraph)
                scope.shouldBeSameInstanceAs(Scope.Prototype)
                argument.shouldBe(Unit)
                i.shouldBeSameInstanceAs(instance)
                called = true
            }
            testGraph.instance<Any>()
            called.shouldBeTrue()
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
            var called = false
            val testGraph = graph { singleton { instance } }
            WinterPlugins.addPostConstructPlugin { graph, scope, argument, i ->
                graph.shouldBeSameInstanceAs(testGraph)
                scope.shouldBeSameInstanceAs(Scope.Singleton)
                argument.shouldBe(Unit)
                i.shouldBeSameInstanceAs(instance)
                called = true
            }
            testGraph.instance<Any>()
            called.shouldBeTrue()
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

    }

    @Nested
    @DisplayName("Reference scope (WeakSingleton and SoftSingleton")
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
            var called = false
            val testGraph = graph { softSingleton { instance } }
            WinterPlugins.addPostConstructPlugin { graph, scope, argument, i ->
                graph.shouldBeSameInstanceAs(testGraph)
                scope.shouldBeSameInstanceAs(Scope.SoftSingleton)
                argument.shouldBe(Unit)
                i.shouldBeSameInstanceAs(instance)
                called = true
            }
            testGraph.instance<Any>()
            called.shouldBeTrue()
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
            var called = false
            val testGraph = graph { weakSingleton { instance } }
            WinterPlugins.addPostConstructPlugin { graph, scope, argument, i ->
                graph.shouldBeSameInstanceAs(testGraph)
                scope.shouldBeSameInstanceAs(Scope.WeakSingleton)
                argument.shouldBe(Unit)
                i.shouldBeSameInstanceAs(instance)
                called = true
            }
            testGraph.instance<Any>()
            called.shouldBeTrue()
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
            var called = false
            val testGraph = graph { factory { i: Int -> i.toString() } }
            WinterPlugins.addPostConstructPlugin { graph, scope, argument, i ->
                graph.shouldBeSameInstanceAs(testGraph)
                scope.shouldBeSameInstanceAs(Scope.PrototypeFactory)
                argument.shouldBe(42)
                i.shouldBe("42")
                called = true
            }
            testGraph.instance<Int, String>(42)
            called.shouldBeTrue()
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
            var called = false
            val testGraph = graph { multiton { i: Int -> i.toString() } }
            WinterPlugins.addPostConstructPlugin { graph, scope, argument, i ->
                graph.shouldBeSameInstanceAs(testGraph)
                scope.shouldBeSameInstanceAs(Scope.MultitonFactory)
                argument.shouldBe(42)
                i.shouldBe("42")
                called = true
            }
            testGraph.instance<Int, String>(42)
            called.shouldBeTrue()
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
            }.providerOrNull<Int, Map<Int, String>>(1, generics = true)?.invoke().shouldBe(mapOf(1 to "1"))
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
            }.factoryOrNull<Int, Map<Int, String>>(generics = true)?.invoke(5).shouldBe(mapOf(5 to "5"))
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
    @DisplayName("#dispose and #isDisposed")
    inner class DisposeMethod {

        @AfterEach
        fun afterEach() {
            WinterPlugins.resetGraphDisposePlugins()
        }

        @Test
        fun `#dispose should mark the graph as disposed`() {
            val graph = graph {}
            expectValueToChange(false, true, graph::isDisposed) {
                graph.dispose()
            }
        }

        @Test
        fun `subsequent calls to #dispose should be ignored`() {
            val count = AtomicInteger(0)
            val graph = graph {}
            WinterPlugins.addGraphDisposePlugin { count.incrementAndGet() }
            expectValueToChange(0, 1, count::get) {
                (0..3).forEach { graph.dispose() }
            }
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

}