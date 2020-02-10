package io.jentz.winter

import com.nhaarman.mockitokotlin2.*
import io.jentz.winter.plugin.Plugin
import io.jentz.winter.plugin.Plugins
import io.jentz.winter.plugin.SimplePlugin
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.collections.shouldContainAll
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
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
        Winter.plugins = Plugins(plugin)
    }

    @AfterEach
    fun afterEach() {
        Winter.plugins = Plugins.EMPTY
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
                prototype(onPostConstruct = {
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
            verify(plugin, times(1)).postConstruct(graph, Scope.Prototype, instance)
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
                onPostConstruct = { it.parent = instance() },
                onClose = { it.parent = null }
            ) { Child() }
        }

        @Test
        fun `should return instance returned by factory function`() {
            testComponent.createGraph().instance<Any>().shouldBeSameInstanceAs(instance)
        }

        @Test
        fun `should invoke factory function only on the first lookup`() {
            var count = 0
            val graph = graph { singleton { count += 1; count } }
            (1..5).forEach { graph.instance<Int>().shouldBe(1) }
        }

        @Test
        fun `should invoke post construct callback with instance`() {
            val parent = testComponent.createGraph().instance<Parent>()
            parent.child.parent.shouldBeSameInstanceAs(parent)
        }

        @Test
        fun `should run post construct plugins`() {
            val graph = graph { singleton { instance } }
            graph.instance<Any>()
            verify(plugin, times(1)).postConstruct(graph, Scope.Singleton, instance)
        }

        @Test
        fun `should invoke close callback with instance`() {
            val graph = testComponent.createGraph()
            val parent: Parent = graph.instance()
            val child: Child = graph.instance()
            expectValueToChange(parent, null, child::parent) {
                graph.close()
            }
        }

        @Test
        fun `#eagerSingleton should be a singleton but created as soon as the graph gets initialized`() {
            var initialized = false
            val graph = graph { eagerSingleton { initialized = true; instance } }
            initialized.shouldBeTrue()
            graph.service(typeKey<Any>()).shouldBeInstanceOf<BoundSingletonService<*>>()
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
            val service = graph.service<Int>(typeKey()) as BoundReferenceService
            service.instance.shouldBe(UNINITIALIZED_VALUE)
            repeat(5) { graph.instance<Int>() }
            service.instance.shouldBe(1)
            service.instance = UNINITIALIZED_VALUE
            repeat(5) { graph.instance<Int>() }
            service.instance.shouldBe(2)
        }

        @Test
        fun `should invoke post construct callback with instance`() {
            val graph = graph { reference { "test" } }
            val service = graph.service<String>(typeKey()) as BoundReferenceService
            graph.instance<String>()
            service.postConstructCalledCount.shouldBe(1)
            service.postConstructLastArgument.shouldBe("test")
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
                softSingleton(onPostConstruct = {
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
            verify(plugin, times(1)).postConstruct(graph, Scope.SoftSingleton, instance)
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
                weakSingleton(onPostConstruct = {
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
            verify(plugin, times(1)).postConstruct(graph, Scope.WeakSingleton, instance)
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
    @DisplayName("Alias service")
    inner class AliasService {

        @Test
        fun `should bind aliased service only once`() {
            val graph = graph {
                prototype { Heater() }
                prototype { Thermosiphon(instance()) }
                alias(typeKey<Thermosiphon>(), typeKey<Pump>())
            }
            graph.service<Pump>(typeKey())
                .shouldBeSameInstanceAs(graph.service<Thermosiphon>(typeKey()))
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
                }.service<Pump>(typeKey())
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
        fun `should throw an exception if dependency doesn't exist`() {
            shouldThrow<EntryNotFoundException> { emptyGraph.instance() }
        }

        @Test
        fun `should throw an exception when graph is closed`() {
            graph { prototype { "string" } }.apply {
                close()
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
        fun `should return null if dependency doesn't exist`() {
            emptyGraph.instanceOrNull<Any>().shouldBe(null)
        }

        @Test
        fun `should throw an exception when graph is closed`() {
            graph { prototype { "string" } }.apply {
                close()
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
        fun `should throw an exception if dependency doesn't exist`() {
            shouldThrow<EntryNotFoundException> { emptyGraph.provider<Any>() }
        }

        @Test
        fun `should throw an exception when graph is closed`() {
            graph { prototype { "string" } }.apply {
                close()
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
        fun `should return null if dependency doesn't exist`() {
            emptyGraph.providerOrNull<Any>().shouldBe(null)
        }

        @Test
        fun `should throw an exception when graph is closed`() {
            graph { prototype { "string" } }.apply {
                close()
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
    @DisplayName("#*OfType")
    inner class OfTypeMethods {

        private val testComponent = component {
            prototype("something else") { Any() }
            prototype("a") { "a" }
            prototype("b") { "b" }
            prototype("c") { "c" }
            prototype { "bar" }

            setOfType<String>("a")
            setOfProvidersForType<String>("b")
            mapOfType<String>("c", defaultKey = "foo")
            mapOfProvidersForType<String>("d", defaultKey = "foo")
        }

        @Test
        fun `should provide a set of instances of type`() {
            val set: Set<String> = testComponent.createGraph().instance("a", true)
            set.shouldBe(setOf("a", "b", "c", "bar"))
        }

        @Test
        fun `should provide a set of providers of type`() {
            val set: Set<Provider<String>> = testComponent.createGraph().instance("b", true)
            set.map { it() }.shouldContainAll("a", "b", "c", "bar")
        }

        @Test
        fun `should provide a map of instances of type`() {
            val map: Map<Any, String> = testComponent.createGraph().instance("c", true)
            map.shouldBe(mapOf(
                "a" to "a",
                "b" to "b",
                "c" to "c",
                "foo" to "bar"
            ))
        }

        @Test
        fun `should provide a map of providers of type`() {
            val map: Map<Any, Provider<String>> = testComponent.createGraph().instance("d", true)
            map.map { (k, v) -> k to v() }.toMap()
                .shouldBe(mapOf(
                    "a" to "a",
                    "b" to "b",
                    "c" to "c",
                    "foo" to "bar"
                ))
        }

        @Test
        fun `#providersOfTypeByKey should fail if type key is not correct`() {
            shouldThrow<IllegalArgumentException> {
                testComponent.createGraph().providersOfTypeByKey(typeKey<String>())
            }
        }

        @Test
        fun `#instancesOfTypeByKey should fail if type key is not correct`() {
            shouldThrow<IllegalArgumentException> {
                testComponent.createGraph().instancesOfTypeByKey(typeKey<String>())
            }
        }

        @Test
        fun `#providersOfType should return a set of providers of a given type`() {
            val providers = testComponent.createGraph().providersOfType<String>()
            providers.shouldHaveSize(4)
            providers.map { it() }.shouldContainAll("a", "b", "c", "bar")
        }

        @Test
        fun `#instancesOfType should return a set of instances of given type`() {
            val instances = testComponent.createGraph().instancesOfType<String>()
            instances.shouldHaveSize(4)
            instances.shouldContainAll("a", "b", "c", "bar")
        }

    }

    @Nested
    @DisplayName("#inject method")
    inner class InjectMethod {

        @Test
        fun `#inject should call members injector for type`() {
            emptyGraph.inject(Service()).property.shouldBe(42)
        }

        @Test
        fun `#inject should call members injector for superclass`() {
            emptyGraph.inject(ExtendedService()).property.shouldBe(42)
        }

    }

    @Nested
    @DisplayName("Properties")
    inner class Properties {

        @Test
        fun `#parent should return null when no parent graph exists`() {
            val graph = graph { subcomponent("sub") {} }
            graph.parent.shouldBeNull()
        }

        @Test
        fun `#parent should return parent graph`() {
            val parent = graph { subcomponent("sub") {} }
            val sub = parent.createSubgraph("sub")
            sub.parent.shouldBeSameInstanceAs(parent)
        }

        @Test
        fun `#parent should throw an exception when graph is closed`() {
            val sub = graph { subcomponent("sub") {} }.createSubgraph("sub")
            shouldThrow<WinterException> {
                sub.close()
                sub.parent
            }.message.shouldBe("Graph is already closed.")
        }

        @Test
        fun `#component should return backing component`() {
            val parent = graph { subcomponent("sub") {} }
            val sub = parent.createSubgraph("sub")
            sub.component.shouldBeSameInstanceAs(parent.component.subcomponent("sub"))
        }

        @Test
        fun `#component should throw an exception when graph is closed`() {
            val sub = graph { subcomponent("sub") {} }.createSubgraph("sub")
            shouldThrow<WinterException> {
                sub.close()
                sub.component
            }.message.shouldBe("Graph is already closed.")
        }

    }

    @Nested
    @DisplayName("initialization")
    inner class Initialisation {

        val component = component { subcomponent("test") {} }

        @Test
        fun `should initialize graph with given component`() {
            Graph(WinterApplication(), null, emptyComponent, null, null)
                .component.shouldBe(emptyComponent)
        }

        @Test
        fun `should run plugins`() {
            val parent = graph { }
            verify(plugin, times(1)).graphInitializing(isNull(), any())
            verify(plugin, times(1)).graphInitialized(any())
            reset(plugin)
            val graph = Graph(Winter, parent, emptyComponent, null, null)
            verify(plugin, times(1)).graphInitializing(same(parent), any())
            verify(plugin, times(1)).graphInitialized(graph)
            verify(plugin, never()).graphClose(any())
            graph.close()
            verify(plugin, times(1)).graphClose(graph)
        }

        @Test
        fun `should derive component when builder block is given`() {
            val graph = Graph(Winter, null, emptyComponent, null) { constant(42) }
            graph.instance<Int>().shouldBe(42)
        }

        @Test
        fun `should add itself to the registry for ability to inject the graph itself`() {
            emptyGraph.instance<Graph>().shouldBeSameInstanceAs(emptyGraph)
        }

        @Test
        fun `#createSubgraph should derive component when builder block is given`() {
            val graph = component.createGraph().createSubgraph("test") { constant(42) }
            graph.instance<Int>().shouldBe(42)
        }

        @Test
        fun `#createSubgraph should pass WinterApplication to new graph`() {
            val testApp = WinterApplication()
            component.createGraph(testApp)
                .createSubgraph("test")
                .application.shouldBeSameInstanceAs(testApp)
        }

    }

    @Nested
    @DisplayName("#close and #isClosed")
    inner class CloseMethod {

        @Test
        fun `#close should mark the graph as closed`() {
            val graph = graph {}
            expectValueToChange(from = false, to = true, valueProvider = graph::isClosed) {
                graph.close()
            }
        }

        @Test
        fun `subsequent calls to #close should be ignored`() {
            val graph = graph {}
            repeat(3) { graph.close() }
            verify(plugin, times(1)).graphClose(graph)
        }

        @Test
        fun `#close should run graph close plugins before marking graph as closed`() {
            var called = false
            Winter.plugins += object : SimplePlugin() {
                override fun graphClose(graph: Graph) {
                    called = true
                    graph.isClosed.shouldBeFalse()
                }
            }
            val graph = graph {}
            graph.close()
            called.shouldBeTrue()
        }

        @Test
        fun `#close should ignore calls to close from plugin`() {
            Winter.plugins + object : SimplePlugin() {
                override fun graphClose(graph: Graph) {
                    graph.close()
                }
            }
            graph {}.close()
            // no StackOverflowError here
        }

    }

    @Nested
    inner class SubgraphManagement {

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
            root = component.createGraph()
        }

        @Test
        fun `#openSubgraph should throw an exception if graph with identifier already exists`() {
            root.openSubgraph("presentation")

            shouldThrow<WinterException> {
                root.openSubgraph("presentation")
            }.message.shouldBe("Cannot open subgraph with identifier `presentation` because it is already open.")
        }

        @Test
        fun `#openSubgraph with identifier should throw an exception if graph with identifier already exists`() {
            root.openSubgraph("presentation", "foo")

            shouldThrow<WinterException> {
                root.openSubgraph("presentation", "foo")
            }.message.shouldBe("Cannot open subgraph with identifier `foo` because it is already open.")
        }

        @Test
        fun `#openSubgraph should initialize and return subcomponent by qualifier`() {
            root.openSubgraph("presentation").component
                .shouldBeSameInstanceAs(component.subcomponent("presentation"))
        }

        @Test
        fun `#openSubgraph should should pass the builder block to the subcomponent init method`() {
            root.openSubgraph("presentation") {
                constant(42)
            }.instance<Int>().shouldBe(42)
        }

        @Test
        fun `#openSubgraph with identifier should initialize subcomponent and register it under the given identifier`() {
            val graph = root.openSubgraph("presentation", identifier = "foo")
            graph.component.shouldBeSameInstanceAs(component.subcomponent("presentation"))
            root.instance<Graph>("foo").shouldBeSameInstanceAs(graph)
        }

        @Test
        fun `#getSubgraph should return subgraph by identifier`() {
            root.openSubgraph("presentation")
                .shouldBeSameInstanceAs(root.getSubgraph("presentation"))
        }

        @Test
        fun `#getSubgraph should throw an exception if subgraph is not open`() {
            shouldThrow<WinterException> { root.getSubgraph("presentation") }
        }

        @Test
        fun `#getSubgraphOrNull should return null if subgraph is not open`() {
            root.getSubgraphOrNull("presentation").shouldBeNull()
        }

        @Test
        fun `#getSubgraphOrNull should return subgraph by identifier`() {
            root.openSubgraph("presentation")
                .shouldBeSameInstanceAs(root.getSubgraphOrNull("presentation"))
        }

        @Test
        fun `#getOrOpenSubgraph should return subgraph if already open`() {
            root.openSubgraph("presentation")
                .shouldBeSameInstanceAs(root.getOrOpenSubgraph("presentation"))
        }

        @Test
        fun `#getOrOpenSubgraph should open subgraph if not present`() {
            root.getOrOpenSubgraph("presentation")
                .shouldBeSameInstanceAs(root.getSubgraph("presentation"))
        }

        @Test
        fun `#getOrOpenSubgraph should open subgraph with identifier`() {
            root.getOrOpenSubgraph("presentation", "foo")
                .shouldBeSameInstanceAs(root.getSubgraph("foo"))
        }

        @Test
        fun `#getOrOpenSubgraph should get subgraph with identifier`() {
            root.openSubgraph("presentation", "foo")
                .shouldBeSameInstanceAs(root.getOrOpenSubgraph("presentation", "foo"))
        }

        @Test
        fun `#closeSubgraph should close and remove subgraph with identifier`() {
            val graph = root.openSubgraph("presentation")
            root.closeSubgraph("presentation")

            graph.isClosed.shouldBeTrue()
            root.instanceOrNull<Graph>("presentation").shouldBeNull()
        }

        @Test
        fun `#closeSubgraph should throw an exception when graph doesn't exist`() {
            shouldThrow<WinterException> {
                root.closeSubgraph("foo")
            }.message.shouldBe("Subgraph with identifier `foo` doesn't exist.")
        }

        @Test
        fun `#closeSubgraphIfOpen should close and remove subgraph with identifier`() {
            val graph = root.openSubgraph("presentation")
            root.closeSubgraphIfOpen("presentation")

            graph.isClosed.shouldBeTrue()
            root.instanceOrNull<Graph>("presentation").shouldBeNull()
        }

        @Test
        fun `#closeSubgraphIfOpen should do noting if graph doesn't exist`() {
            root.closeSubgraphIfOpen("foo")
        }

        @Test
        fun `#close should close all managed subgraphs`() {
            val presentation = root.openSubgraph("presentation")
            val view = presentation.openSubgraph("view")

            presentation.close()

            presentation.isClosed.shouldBeTrue()
            view.isClosed.shouldBeTrue()
        }

        @Test
        fun `#close of subgraphs should not lead to concurrent modification exception`() {
            val presentation = root.openSubgraph("presentation")
            val view = presentation.openSubgraph("view")

            // we need more than one service in our registry to get the exception when unregistering
            // of subgraphs is not prevented during close
            presentation.instance<List<*>>()
            view.instance<Map<*, *>>()

            root.close()
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
                        onPostConstruct = { called = true }
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