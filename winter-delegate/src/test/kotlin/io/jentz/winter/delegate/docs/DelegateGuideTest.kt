package io.jentz.winter.delegate.docs

import io.jentz.winter.Graph
import io.jentz.winter.WinterApplication
import io.jentz.winter.delegate.graphResolver
import io.jentz.winter.delegate.provideDelegate
import io.jentz.winter.qualifier
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/*
 * The property delegate samples of the guide (doc/index.adoc).
 *
 * The guide includes the regions between the `tag::name[]` and `end::name[]` comments.
 */

interface Pump
class RotaryPump : Pump
class Heater
class CoffeeMaker(val heater: Heater, val pump: Pump)
class OrderList(val coffeeMaker: CoffeeMaker)

object CoffeeShop : WinterApplication()

val ScreenScope = qualifier("screen")

// tag::delegates[]
class CoffeeScreen {
    private val injector by CoffeeShop

    // retrieved when inject() is called
    private val coffeeMaker: CoffeeMaker by injector()
    // retrieved on first access
    private val heater: Heater by injector.lazyInstance()
    // resolves a new instance on every call
    private val pumpProvider: () -> Pump by injector.provider()
    // maps the instance
    private val pumpName: String by injector<Pump>().map { it.javaClass.simpleName }

    fun onCreate() {
        injector.inject()
    }
    // end::delegates[]

    fun check() {
        coffeeMaker.heater shouldBeSameInstanceAs heater
        pumpProvider() shouldBeSameInstanceAs coffeeMaker.pump
        pumpName shouldBe "RotaryPump"
    }
    // tag::delegates[]
}
// end::delegates[]

class OrderScreen {
    private val injector by CoffeeShop
    val orders: OrderList by injector()
    val graph: Graph by injector()

    fun onCreate() {
        injector.inject()
    }
}

class DelegateGuideTest {

    @AfterEach
    fun afterEach() {
        CoffeeShop.closeGraphIfOpen()
    }

    @Test
    fun delegates() {
        CoffeeShop.component {
            singleton { Heater() }
            singleton<Pump> { RotaryPump() }
            singleton { CoffeeMaker(instance(), instance()) }
        }
        CoffeeShop.openGraph()

        CoffeeScreen().apply { onCreate() }.check()
    }

    @Test
    fun graphResolver() {
        // tag::graphResolver[]
        CoffeeShop.component {
            singleton { Heater() }
            singleton<Pump> { RotaryPump() }
            singleton { CoffeeMaker(instance(), instance()) }

            // OrderScreen gets its dependencies from a new screen subgraph
            graphResolver<OrderScreen> { appGraph, screen ->
                appGraph.createSubgraph(ScreenScope)
            }

            subcomponent(ScreenScope) {
                singleton { OrderList(instance()) }
            }
        }
        // end::graphResolver[]

        val appGraph = CoffeeShop.openGraph()
        val screen = OrderScreen().apply { onCreate() }
        screen.graph.parent shouldBeSameInstanceAs appGraph
        screen.orders.coffeeMaker shouldBeSameInstanceAs appGraph.instance<CoffeeMaker>()
        screen.graph.close()
    }

}
