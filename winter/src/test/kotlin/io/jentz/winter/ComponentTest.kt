package io.jentz.winter

import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.matchers.types.shouldNotBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.Test

class ComponentTest {

    private val testComponent = component {
        prototype { Heater() }
        singleton<Pump> { Thermosiphon(instance()) }
        singleton { CoffeeMaker(instance(), instance()) }
    }

    @Test
    fun `#component should create component with default qualifier`() {
        testComponent.qualifier.shouldBe(Qualifier.App)
    }

    @Test
    fun `#component should create component with given qualifier`() {
        component(qualifier("foo")) {  }.qualifier.shouldBe(qualifier("foo"))
    }

    @Test
    fun `#derive with empty block and same qualifier should return same component`() {
        testComponent.shouldBeSameInstanceAs(testComponent.derive { })
    }

    @Test
    fun `#derive with block should copy all dependencies to new component`() {
        val new = testComponent.derive { prototype(typeKey(qualifier("qualifier"))) { Heater() } }
        new.size.shouldBe(testComponent.size + 1)
        new.containsKey(typeKey<Heater>(qualifier("qualifier"))).shouldBeTrue()
        testComponent.keys().forEach { key -> new[key].shouldBeSameInstanceAs(testComponent[key]) }
    }

    @Test
    fun `#derive should copy the qualifier of the component it is derived from when no qualifier is given`() {
        component(qualifier("some qualifier")) {}.derive {
            constant(42)
        }.qualifier.shouldBe(qualifier("some qualifier"))
    }

    @Test
    fun `#derive should set new qualifier if one is supplied`() {
        testComponent.derive(qualifier("derived")) { }.qualifier.shouldBe(qualifier("derived"))
    }

    @Test
    fun `#subcomponent should throw an exception if entry doesn't exist`() {
        shouldThrow<EntryNotFoundException> {
            component {}.subcomponent(Qualifier.a)
        }
    }

    @Test
    fun `#subcomponent with one qualifier should return the corresponding subcomponent`() {
        component {
            subcomponent(Qualifier.a) {}
            subcomponent(Qualifier.b) {}
        }.subcomponent(Qualifier.b).qualifier.shouldBe(Qualifier.b)
    }

    @Test
    fun `#subcomponent with multiple qualifiers should return the corresponding nested subcomponent`() {
        component {
            subcomponent(Qualifier.a) {
                subcomponent(Qualifier.b) {
                    subcomponent(Qualifier.c) {}
                }
            }
        }.subcomponent(Qualifier.a, Qualifier.b, Qualifier.c).qualifier.shouldBe(Qualifier.c)
    }

    @Test
    fun `#createGraph without builder block should return graph with same component`() {
        val c = component { }
        c.createGraph().component.shouldBeSameInstanceAs(c)
    }

    @Test
    fun `#createGraph with empty builder block should return graph with same component`() {
        val c = component { }
        c.createGraph {}.component.shouldBeSameInstanceAs(c)
    }

    @Test
    fun `#createGraph with builder block should return graph with derived component`() {
        val c = component { }
        val graph = c.createGraph { constant(42) }
        graph.component.qualifier.shouldBe(Qualifier.App)
        graph.component.shouldNotBeSameInstanceAs(c)
    }

}