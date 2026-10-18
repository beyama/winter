package io.jentz.winter

import io.jentz.winter.dsl.new
import io.jentz.winter.dsl.singletonOf
import io.jentz.winter.plugin.Plugin
import io.jentz.winter.plugin.Plugins
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class GraphPrototypeScopeTest {

    private val instance = Any()

    private val plugin = RecordingPlugin()

    @BeforeTest
    fun beforeEach() {
        Winter.plugins = Plugins(plugin)
    }

    @AfterTest
    fun afterEach() {
        Winter.plugins = Plugins.EMPTY
    }

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
    fun `should return null for optional type if service returns null`() {
        val graph = graph { prototype<String?> { null } }
        graph.instance<String?>().shouldBeNull()
    }

    @Test
    fun `should invoke post construct callback with instance`() {
        var called = false
        graph {
            prototype { instance }
                .onPostConstruct {
                    it.shouldBeSameInstanceAs(instance)
                    called = true
                }
        }.instance<Any>()
        called.shouldBeTrue()
    }

    @Test
    fun `should invoke post construct callback with extended graph`() {
        var called = false
        val graph = graph {
            prototype { instance }
                .onPostConstruct {
                    called = true
                    component.qualifier.shouldBe(Qualifier("_DERIVED_"))
                }
        }
        graph.instance<Any> {
            prototype { constant("foo") }
        }
        called.shouldBeTrue()
    }

    @Test
    fun `should run post construct plugins`() {
        val graph = graph { prototype { instance } }
        graph.instance<Any>()
        plugin.postConstruct.shouldContainExactly(PostConstruct(graph, Prototype, instance))
    }

    @Test
    fun `should allow resolution of nested dependencies`() {
        graph {
            prototype { Heater() }
            prototype<Pump> { Thermosiphon(instance()) }
            prototype { CoffeeMaker(instance(), instance()) }
        }.instance<CoffeeMaker>()
    }

}

class GraphSingletonScopeTest {

    private val instance = Any()

    private val plugin = RecordingPlugin()

    @BeforeTest
    fun beforeEach() {
        Winter.plugins = Plugins(plugin)
    }

    @AfterTest
    fun afterEach() {
        Winter.plugins = Plugins.EMPTY
    }

    private val testComponent = component {
        singleton { instance }
        singleton { Parent(instance()) }
        singleton { Child() }
            .onPostConstruct { it.parent = instance() }
            .onClose { it.parent = null }
    }

    @Test
    fun `should return instance returned by factory function`() {
        testComponent.createGraph().instance<Any>().shouldBeSameInstanceAs(instance)
    }

    @Test
    fun `should return null for optional type if service returns null`() {
        val graph = graph { singleton<String?> { null } }
        graph.instance<String?>().shouldBeNull()
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
    fun `should invoke post construct callback with extended graph`() {
        var called = false
        val graph = graph {
            singleton { instance }
                .onPostConstruct {
                    called = true
                    component.qualifier.shouldBe(qualifier("_DERIVED_"))
                }
        }
        graph.instance<Any> {
            prototype { constant("foo") }
        }
        called.shouldBeTrue()
    }

    @Test
    fun `should run post construct plugins`() {
        val graph = graph { singleton { instance } }
        graph.instance<Any>()
        plugin.postConstruct.shouldContainExactly(PostConstruct(graph, Singleton, instance))
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
    fun `eager singleton should be initialized as soon as the graph gets initialized`() {
        var initialized = false
        graph { singleton { initialized = true; instance }.eager() }
        initialized.shouldBeTrue()
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
    fun `should not resolve classes from child graphs`() {
        /**
         * class Heater
         *
         * class Thermosiphon(val heater: Heater) : Pump
         *
         * class CoffeeMaker(val heater: Heater, val pump: Pump)
         */

        val component = component {
            singletonOf(::Heater)
                .onClose { println("onClose($it)") }
            singletonOf(::Thermosiphon)
                .alias(Pump::class)
                .onClose { println("onClose($it)") }

            subcomponent(QualifierSub) {
                singletonOf(::CoffeeMaker)
                    .onClose { println("onClose($it)") }
            }
        }
        val parent = component.createGraph()
        val child = parent.createSubgraph(QualifierSub)
        val coffeeMaker: CoffeeMaker = child.instance()
        child.close()
        parent.close()
//            println(coffeeMaker)
    }

}

class GraphAliasTest {

    @Test
    fun `should allow aliases to aliases`() {
        graph {
            prototype { Heater() }
            prototype { Thermosiphon(instance()) }
            alias(erased<Thermosiphon>(), erased<Pump>())
            alias(erased<Pump>(), erased<Any>())
        }.instance<Any>().shouldBeInstanceOf<Thermosiphon>()
    }

    @Test
    fun `bind should throw proper exception when aliased service doesn't exist anymore`() {
        shouldThrow<WinterException> {
            graph {
                prototype { Heater() }
                prototype { Thermosiphon(instance()) }
                alias(erased<Thermosiphon>(), erased<Pump>())
                remove(erased<Thermosiphon>())
            }.service<Pump>(erased())
        }.message.shouldBe("Error resolving alias `${erased<Pump>()}` pointing to `${erased<Thermosiphon>()}`.")
    }

    @Test
    fun `should call close only once for the target`() {
        var closed = 0
        val graph = graph {
            singleton { "foo" }
                .onClose { closed += 1 }
                .alias(erased<CharSequence>())
        }
        graph.instance<CharSequence>()
        graph.close()
        closed.shouldBe(1)
    }

}

class GraphInstanceTest {

    private val emptyGraph = emptyGraph()

    private val instance = Any()

    @Test
    fun `should resolve instance`() {
        graph {
            prototype { "string" }
        }.instance<String>().shouldBe("string")
    }

    @Test
    fun `should resolve optional instance`() {
        graph {
            prototype { "string" }
        }.instance<String?>().shouldBe("string")
    }

    @Test
    fun `should resolve null if optional service does not exit`() {
        emptyGraph().instance<String?>().shouldBeNull()
    }

    @Test
    fun `should resolve null if optional service returns null`() {
        graph {
            prototype<String?> { null }
        }.instance<String?>().shouldBeNull()
    }

    @Test
    fun `should fail if service returns null for non-optional key`() {
        shouldThrow<DependencyResolutionException> {
            graph {
                prototype<String?> { null }
            }.instance<String>().uppercase()
        }
    }

    @Test
    fun `should resolve instance by generic class`() {
        graph {
            prototype(generic()) { mapOf(1 to "1") }
        }.instance(generic<Map<Int, String>>()).shouldBe(mapOf(1 to "1"))
    }

    @Test
    fun `should resolve instance with qualifier`() {
        graph {
            prototype(erased(QualifierA)) { "a" }
            prototype(erased(QualifierB)) { "b" }
        }.instance(erased<String>(QualifierB)).shouldBe("b")
    }

    @Test
    fun `should extend the evaluation graph with given builder block`() {
        var extended: Graph? = null
        var extendedParent: Graph? = null
        val heater = Heater()
        val graph = graph {
            prototype {
                extended = this
                extendedParent = parent
                Thermosiphon(instance())
            }
        }
        val instance = graph.instance<Thermosiphon> {
            constant(heater)
        }
        extended!!.isClosed.shouldBeTrue()
        extendedParent.shouldBeSameInstanceAs(graph)
        instance.heater.shouldBeSameInstanceAs(heater)
    }

    @Test
    fun `should throw an exception if a non-optional dependency doesn't exist`() {
        shouldThrow<EntryNotFoundException> { emptyGraph.instance<Any>() }
    }

    @Test
    fun `should return null if an optional dependency doesn't exist`() {
        emptyGraph.instance<Any?>().shouldBeNull()
    }

    @Test
    fun `should throw an exception when graph is closed`() {
        graph { prototype { "string" } }.apply {
            close()
            shouldThrow<WinterException> { instance() }
        }
    }

    @Test
    fun `should throw an exception when graph is closed and service was already bound`() {
        graph { singleton { "string" } }.apply {
            instance<String>()
            close()
            shouldThrow<WinterException> {
                instance<String>()
            }.message.shouldBe("Graph is already closed.")
        }
    }

    @Test
    fun `should pass builder block to factory`() {
        val heater = Heater()
        graph {
            prototype { Thermosiphon(instance()) }
        }.instance<Thermosiphon> {
            constant(heater)
        }.heater.shouldBeSameInstanceAs(heater)
    }

}

class GraphPropertiesTest {

    @Test
    fun `parent should return null when no parent graph exists`() {
        graph {}.parent.shouldBeNull()
    }

    @Test
    fun `parent should return parent graph`() {
        val parent = graph { subcomponent(QualifierA) {} }
        val sub = parent.createSubgraph(QualifierA)
        sub.parent.shouldBeSameInstanceAs(parent)
    }

    @Test
    fun `parent should throw an exception when graph is closed`() {
        val sub = graph { subcomponent(QualifierA) {} }.createSubgraph(QualifierA)
        shouldThrow<WinterException> {
            sub.close()
            sub.parent
        }.message.shouldBe("Graph is already closed.")
    }

    @Test
    fun `component should return backing component`() {
        val parent = graph { subcomponent(QualifierA) {} }
        val sub = parent.createSubgraph(QualifierA)
        sub.component.shouldBeSameInstanceAs(parent.component.subcomponent(QualifierA))
    }

    @Test
    fun `component should throw an exception when graph is closed`() {
        val sub = graph { subcomponent(QualifierA) {} }.createSubgraph(QualifierA)
        shouldThrow<WinterException> {
            sub.close()
            sub.component
        }.message.shouldBe("Graph is already closed.")
    }

}

class GraphInitializationTest {

    private val emptyComponent = emptyComponent()

    private val emptyGraph = emptyGraph()

    private val plugin = RecordingPlugin()

    @BeforeTest
    fun beforeEach() {
        Winter.plugins = Plugins(plugin)
    }

    @AfterTest
    fun afterEach() {
        Winter.plugins = Plugins.EMPTY
    }

    val component = component {
        subcomponent(QualifierA) {
            subcomponent(QualifierB) {
                subcomponent(QualifierC) {}
            }
        }
    }

    @Test
    fun `should initialize graph with given component`() {
        Graph(WinterApplication(), null, emptyComponent, null, null)
            .component.shouldBe(emptyComponent)
    }

    @Test
    fun `should run plugins`() {
        val parent = graph { }
        plugin.graphInitializing.shouldContainExactly(null)
        plugin.graphInitialized.shouldContainExactly(parent)
        plugin.reset()
        val graph = Graph(Winter, parent, emptyComponent, null, null)
        plugin.graphInitializing.shouldContainExactly(parent)
        plugin.graphInitialized.shouldContainExactly(graph)
        plugin.graphClose.shouldBeEmpty()
        graph.close()
        plugin.graphClose.shouldContainExactly(graph)
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
    fun `createSubgraph should derive component when builder block is given`() {
        val graph = component.createGraph().createSubgraph(QualifierA) { constant(42) }
        graph.instance<Int>().shouldBe(42)
    }

    @Test
    fun `createSubgraph should create subgraph from a derived component`() {
        component.createGraph()
            .createSubgraph(QualifierA) { constant(42) }
            .createSubgraph(QualifierB)
            .instance<Int>()
            .shouldBe(42)
    }

    @Test
    fun `createSubgraph should not create subgraph from ancestor components`() {
        shouldThrow<EntryNotFoundException> {
            component.createGraph()
                .createSubgraph(QualifierA)
                .createSubgraph(QualifierB)
                .createSubgraph(QualifierA)
        }.message.shouldBe("Subcomponent `qualifier(a)` doesn't exist.")
    }

    @Test
    fun `createSubgraph should pass WinterApplication to new graph`() {
        val testApp = WinterApplication()
        component.createGraph(testApp)
            .createSubgraph(QualifierA)
            .application.shouldBeSameInstanceAs(testApp)
    }

}

class GraphCloseTest {

    private val plugin = RecordingPlugin()

    @BeforeTest
    fun beforeEach() {
        Winter.plugins = Plugins(plugin)
    }

    @AfterTest
    fun afterEach() {
        Winter.plugins = Plugins.EMPTY
    }

    @Test
    fun `close should mark the graph as closed`() {
        val graph = graph {}
        expectValueToChange(from = false, to = true, valueProvider = graph::isClosed) {
            graph.close()
        }
    }

    @Test
    fun `subsequent calls to close should be ignored`() {
        val graph = graph {}
        repeat(3) { graph.close() }
        plugin.graphClose.shouldContainExactly(graph)
    }

    @Test
    fun `close should run graph close plugins before marking graph as closed`() {
        var called = false
        Winter.plugins += object : Plugin {
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
    fun `close should ignore calls to close from plugin`() {
        Winter.plugins + object : Plugin {
            override fun graphClose(graph: Graph) {
                graph.close()
            }
        }
        graph {}.close()
        // no StackOverflowError here
    }

}

class GraphCyclicDependenciesTest {

    private val testComponent = component {
        singleton { Parent(instance()) }
        singleton { Child() }
            .onPostConstruct { it.parent = instance() }
            .onClose { it.parent = null }
    }

    @Test
    fun `should allow to resolve a cyclic dependency in onPostConstruct`() {
        val child = testComponent.createGraph().instance<Child>()
        child.parent?.child.shouldBeSameInstanceAs(child)
    }

}

class GraphExceptionTest {

    @Test
    fun `should have the right message when dependency wasn't found with one level of nesting`() {
        shouldThrow<DependencyResolutionException> {
            graph {
                prototype { CoffeeMaker(instance(), instance()) }
            }.instance<CoffeeMaker>()
        }.message.shouldBe(
            "Error while resolving dependency with key: " +
                    "TypeKey(${CoffeeMaker::class.displayName}, qualifier = null, isOptional = false) " +
                    "reason: could not find dependency with key " +
                    "TypeKey(${Heater::class.displayName}, qualifier = null, isOptional = false)"
        )
    }

    @Test
    fun `should have the right message when dependency wasn't found with two levels of nesting`() {
        shouldThrow<DependencyResolutionException> {
            graph {
                prototype { Heater() }
                prototype<Pump> { Thermosiphon(instance(erased(qualifier("doesn't exist")))) }
                prototype { CoffeeMaker(instance(), instance()) }
            }.instance<CoffeeMaker>()
        }.message.shouldBe(
            "Error while resolving dependency with key: " +
                    "TypeKey(${Pump::class.displayName}, qualifier = null, isOptional = false) " +
                    "reason: could not find dependency with key " +
                    "TypeKey(${Heater::class.displayName}, qualifier = qualifier(doesn't exist), isOptional = false)"
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
                        "TypeKey(${Pump::class.displayName}, qualifier = null, isOptional = false) " +
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
                prototype { Heater() }
                    .onPostConstruct { called = true }
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

class GraphNewTest {

    @Test
    fun `should resolve all constructor parameters`() {
        val graph = graph {
            singletonOf(::Heater)
            singletonOf(::Thermosiphon)
                .alias(Pump::class)
        }
        val coffeeMaker = graph.new(::CoffeeMaker)
        coffeeMaker.heater.shouldBeSameInstanceAs(graph.instance<Heater>())
        coffeeMaker.pump.shouldBeSameInstanceAs(graph.instance<Pump>())
    }

}
