package io.jentz.winter.docs

import io.jentz.winter.CyclicDependencyException
import io.jentz.winter.DependencyResolutionException
import io.jentz.winter.Graph
import io.jentz.winter.WinterApplication
import io.jentz.winter.component
import io.jentz.winter.dsl.lazyInstance
import io.jentz.winter.dsl.new
import io.jentz.winter.dsl.provider
import io.jentz.winter.dsl.prototypeOf
import io.jentz.winter.dsl.singletonOf
import io.jentz.winter.erased
import io.jentz.winter.generic
import io.jentz.winter.graph
import io.jentz.winter.plugin.plugin
import io.jentz.winter.qualifier
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test

/*
 * The code samples of the guide (doc/index.adoc).
 *
 * The guide includes the regions between the `tag::name[]` and `end::name[]` comments, so the
 * samples are compiled and run with the tests and can't get out of date.
 */

// tag::classes[]
interface Pump
class RotaryPump : Pump
class Heater
class CoffeeMaker(val heater: Heater, val pump: Pump)
// end::classes[]

class Grinder
class Order(val drink: String, val coffeeMaker: CoffeeMaker)

class Connection {
    var isOpen = true
    fun close() {
        isOpen = false
    }
}

class HttpClient(val baseUrl: String = "https://api.example.com")
class User(val name: String)
class LoginController(val client: HttpClient, val user: User)
class ProfileController(val user: User)

class Parent(val child: Child)
class Child {
    lateinit var parent: Parent
}

enum class Roast { Light, Dark }

// tag::libraryObject[]
object CoffeeShop : WinterApplication()
// end::libraryObject[]

// tag::sessionQualifier[]
val Session = qualifier("session")
// end::sessionQualifier[]

class CoreGuideTest {

    @Test
    fun quickstart() {
        // tag::quickstart[]
        val coffeeComponent = component {
            singleton { Heater() }
            singleton<Pump> { RotaryPump() }
            prototype { CoffeeMaker(instance(), instance()) }
        }

        val graph = coffeeComponent.createGraph()
        val coffeeMaker: CoffeeMaker = graph.instance()
        // end::quickstart[]

        coffeeMaker.pump.shouldBeInstanceOf<RotaryPump>()
        graph.close()
    }

    @Test
    fun scopes() {
        var pumpCreated = false
        val component =
            // tag::scopes[]
            component {
                // a new instance for every request
                prototype { CoffeeMaker(instance(), instance()) }
                // created on the first request, then the same instance for the lifetime of the graph
                singleton { Heater() }
                // a singleton that is created as soon as the graph is opened
                singleton<Pump> {
                    // end::scopes[]
                    pumpCreated = true
                    // tag::scopes[]
                    RotaryPump()
                }.eager()
                // an existing value
                constant("Espresso")
            }
            // end::scopes[]

        val graph = component.createGraph()
        pumpCreated shouldBe true
        graph.instance<Heater>() shouldBeSameInstanceAs graph.instance<Heater>()
        (graph.instance<CoffeeMaker>() === graph.instance<CoffeeMaker>()).shouldBeFalse()
        graph.instance<String>() shouldBe "Espresso"
    }

    @Test
    fun constructorReferences() {
        val component =
            // tag::constructorReferences[]
            component {
                singletonOf(::Heater)
                singletonOf(::RotaryPump).alias(Pump::class)
                prototypeOf(::CoffeeMaker)
            }
            // end::constructorReferences[]

        val graph = component.createGraph()
        graph.instance<CoffeeMaker>().pump shouldBeSameInstanceAs graph.instance<RotaryPump>()
    }

    @Test
    fun qualifiers() {
        // tag::qualifiers[]
        val graph = graph {
            constant("Arabica", erased(qualifier("bean")))
            constant("Robusta", erased(qualifier(Roast.Dark)))
            constant("Ethiopia", erased(qualifier<Grinder>()))
        }

        val bean: String = graph.instance(erased(qualifier("bean")))
        // end::qualifiers[]

        bean shouldBe "Arabica"
        graph.instance<String>(erased(Roast.Dark.qualifier)) shouldBe "Robusta"
        graph.instance<String>(erased(qualifier<Grinder>())) shouldBe "Ethiopia"
    }

    @Test
    fun callbacks() {
        var connected = false
        // tag::callbacks[]
        val graph = graph {
            singleton { Connection() }
                .onPostConstruct {
                    // end::callbacks[]
                    connected = true
                    // tag::callbacks[]
                    // called after the instance and all its dependencies were created
                }
                .onClose { it.close() }
        }

        val connection: Connection = graph.instance()
        graph.close() // calls Connection.close()
        // end::callbacks[]

        connected shouldBe true
        connection.isOpen.shouldBeFalse()
    }

    @Test
    fun subgraphs() {
        // tag::subgraphs[]
        val appComponent = component {
            singleton { HttpClient() }

            subcomponent(Session) {
                singleton { LoginController(instance(), instance()) }
            }
        }

        val appGraph = appComponent.createGraph()

        // A subgraph can be extended with a builder block, e.g. to add runtime values.
        val sessionGraph = appGraph.createSubgraph(Session) {
            constant(User("ada"))
        }

        val controller: LoginController = sessionGraph.instance()

        sessionGraph.close()
        appGraph.close()
        // end::subgraphs[]

        controller.user.name shouldBe "ada"
        shouldThrow<Exception> { appGraph.instance<LoginController>() }
    }

    @Test
    fun retrieval() {
        val graph = graph {
            singleton { Heater() }
            singleton<Pump> { RotaryPump() }
        }
        // tag::retrieval[]
        // an instance, the type is inferred from the declaration or given explicitly
        val heater: Heater = graph.instance()
        val pump = graph.instance<Pump>()

        // an optional instance, null if Grinder isn't registered
        val grinder: Grinder? = graph.instance()

        // a function that resolves the instance on every call
        val heaterProvider: () -> Heater = graph.provider()

        // resolved on first access
        val lazyHeater: Lazy<Heater> = graph.lazyInstance()

        // call a constructor with arguments from the graph
        val coffeeMaker = graph.new(::CoffeeMaker)
        // end::retrieval[]

        grinder.shouldBeNull()
        heaterProvider() shouldBeSameInstanceAs heater
        lazyHeater.value shouldBeSameInstanceAs heater
        coffeeMaker.pump shouldBeSameInstanceAs pump
    }

    @Test
    fun runtimeArguments() {
        // tag::runtimeArguments[]
        val graph = graph {
            singleton { Heater() }
            singleton<Pump> { RotaryPump() }
            prototype { CoffeeMaker(instance(), instance()) }
            // the drink (String) is provided by the caller
            prototype { Order(instance(), instance()) }
        }

        val order: Order = graph.instance { constant("Flat White") }
        // end::runtimeArguments[]

        order.drink shouldBe "Flat White"
    }

    @Test
    fun runtimeArgumentsAreOnlyVisibleToTheRequestedFactory() {
        class Barista(val order: Order)
        val graph = graph {
            singleton { Heater() }
            singleton<Pump> { RotaryPump() }
            prototype { CoffeeMaker(instance(), instance()) }
            prototype { Order(instance(), instance()) }
            prototype { Barista(instance()) }
        }
        shouldThrow<DependencyResolutionException> {
            graph.instance<Barista> { constant("Flat White") }
        }
    }

    @Test
    fun generics() {
        // tag::generics[]
        val graph = graph {
            singleton(generic<List<Pump>>()) { listOf(RotaryPump(), RotaryPump()) }
            singleton(generic<List<Heater>>()) { listOf(Heater()) }
        }

        val pumps: List<Pump> = graph.instance(generic())
        // end::generics[]

        pumps shouldHaveSize 2
        graph.instance<List<Heater>>(generic()) shouldHaveSize 1
    }

    @Test
    fun include() {
        // tag::include[]
        val networkComponent = component {
            singleton { HttpClient() }
        }

        val loginFeature = component {
            subcomponent(Session) {
                singleton { LoginController(instance(), instance()) }
            }
        }

        val profileFeature = component {
            subcomponent(Session) {
                singleton { ProfileController(instance()) }
            }
        }

        val appComponent = component {
            include(networkComponent)
            // Both features contribute to the session subcomponent, it gets merged.
            include(loginFeature)
            include(profileFeature)
        }
        // end::include[]

        val sessionGraph = appComponent.createGraph().createSubgraph(Session) {
            constant(User("ada"))
        }
        sessionGraph.instance<LoginController>().user.name shouldBe "ada"
        sessionGraph.instance<ProfileController>().user.name shouldBe "ada"

        // tag::derive[]
        val testComponent = appComponent.derive {
            // replace existing entries
            override {
                singleton { HttpClient(baseUrl = "http://localhost:8080") }
            }
            // extend an existing subcomponent
            subcomponent(Session, deriveExisting = true) {
                constant(User("test user"))
            }
        }
        // end::derive[]

        val testSession = testComponent.createGraph().createSubgraph(Session)
        testSession.instance<LoginController>().client.baseUrl shouldBe "http://localhost:8080"
        testSession.instance<ProfileController>().user.name shouldBe "test user"
    }

    @Test
    fun circular() {
        // tag::circular[]
        val graph = graph {
            singleton { Parent(instance()) }
            singleton { Child() }
                .onPostConstruct { it.parent = instance() }
        }
        // end::circular[]

        val parent: Parent = graph.instance()
        parent.child.parent shouldBeSameInstanceAs parent
    }

    @Test
    fun cyclicDependency() {
        class A(val b: Any)
        class B(val a: A)
        val graph = graph {
            singleton { A(instance<B>()) }
            singleton { B(instance()) }
        }
        shouldThrow<CyclicDependencyException> { graph.instance<A>() }
    }

    @Test
    fun application() {
        // tag::application[]
        CoffeeShop.component {
            singleton { Heater() }
            singleton<Pump> { RotaryPump() }
            prototype { CoffeeMaker(instance(), instance()) }

            subcomponent(Session) {
                singleton { ProfileController(instance()) }
            }
        }

        val appGraph: Graph = CoffeeShop.openGraph()
        // anywhere else in the application
        val coffeeMaker: CoffeeMaker = CoffeeShop.graph.instance()
        // end::application[]

        coffeeMaker.heater shouldBeSameInstanceAs appGraph.instance<Heater>()

        // tag::application[]

        // e.g. when the application terminates
        CoffeeShop.closeGraph()
        // end::application[]
    }

    @Test
    fun plugins() {
        CoffeeShop.component {
            singleton { Heater() }
            subcomponent(Session) {
                singleton { ProfileController(instance()) }
            }
        }
        val heaters = mutableListOf<Heater>()
        // tag::plugins[]
        val uninstall = CoffeeShop.plugin {
            // derive the component of every new graph with the qualifier Session
            extend(Session) {
                constant(User("guest"))
            }
            // called for every new graph
            withNewGraph { }
            // called for every graph that gets closed
            withClosingGraph { }
            // called for every new instance of Heater
            withNewInstance<Heater> { scope, heater ->
                // end::plugins[]
                heaters += heater
                // tag::plugins[]
            }
        }

        // later
        uninstall()
        // end::plugins[]

        val appGraph = CoffeeShop.openGraph()
        appGraph.instance<Heater>()
        heaters.shouldHaveSize(0)

        val uninstallAgain = CoffeeShop.plugin {
            extend(Session) { constant(User("guest")) }
            withNewInstance<Heater> { _, heater -> heaters += heater }
        }
        val sessionGraph = appGraph.createSubgraph(Session)
        sessionGraph.instance<ProfileController>().user.name shouldBe "guest"
        CoffeeShop.closeGraph()
        CoffeeShop.openGraph().instance<Heater>()
        heaters shouldHaveSize 1
        uninstallAgain()
        CoffeeShop.closeGraph()
    }

}
