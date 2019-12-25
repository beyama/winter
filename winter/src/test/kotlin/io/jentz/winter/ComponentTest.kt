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
        factory { c: Color -> Widget(c) }
    }

    @Test
    fun `#component should create component with default qualifier`() {
        testComponent.qualifier.shouldBe(APPLICATION_COMPONENT_QUALIFIER)
    }

    @Test
    fun `#component should create component with given qualifier`() {
        component("foo") {  }.qualifier.shouldBe("foo")
    }

    @Test
    fun `#derive with empty block should create a copy of the component`() {
        val component = component { prototype { Heater() } }
        val derived = component.derive { }
        component.shouldNotBeSameInstanceAs(derived)
        component.forEach { (k, s) -> derived[k].shouldBeSameInstanceAs(s) }
        derived.forEach { (k, s) -> component[k].shouldBeSameInstanceAs(s) }
    }

    @Test
    fun `#derive with block should copy all dependencies to new component`() {
        val new = testComponent.derive { prototype("qualifier") { Heater() } }
        new.size.shouldBe(testComponent.size + 1)
        testComponent.forEach { (k, s) -> new[k].shouldBeSameInstanceAs(s) }
        new.containsKey(typeKey<Heater>("qualifier")).shouldBeTrue()
    }

    @Test
    fun `#derive should copy the qualifier of the component it is derived from when no qualifier is given`() {
        testComponent.derive { }.qualifier.shouldBe(APPLICATION_COMPONENT_QUALIFIER)
    }

    @Test
    fun `#derive should set new qualifier if one is supplied`() {
        testComponent.derive("derived") { }.qualifier.shouldBe("derived")
    }

    @Test
    fun `#subcomponent should throw an exception if entry doesn't exist`() {
        shouldThrow<EntryNotFoundException> {
            component {}.subcomponent("a")
        }
    }

    @Test
    fun `#subcomponent with one qualifier should return the corresponding subcomponent`() {
        component {
            subcomponent("s1") {}
            subcomponent("s2") {}
        }.subcomponent("s2").qualifier.shouldBe("s2")
    }

    @Test
    fun `#subcomponent with multiple qualifiers should return the corresponding nested subcomponent`() {
        component {
            subcomponent("1") {
                subcomponent("1.1") {
                    subcomponent("1.1.1") {}
                }
            }
        }.subcomponent("1", "1.1", "1.1.1").qualifier.shouldBe("1.1.1")
    }

    @Test
    fun `#init without builder block should return graph with component`() {
        val c = component("root") { }
        c.createGraph().component.shouldBeSameInstanceAs(c)
    }

    @Test
    fun `#init with builder block should return graph with derived component`() {
        val c = component("root") { }
        val graph = c.createGraph { }
        graph.component.qualifier.shouldBe("root")
        graph.component.shouldNotBeSameInstanceAs(c)
    }

}