package io.jentz.winter

import io.jentz.winter.ComponentBuilder.SubcomponentIncludeMode.*
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.Test

class ComponentBuilderTest {

    @Test
    fun `empty builder should result in empty dependency map`() {
        component { }.dependencies.isEmpty().shouldBeTrue()
    }

    @Test
    fun `#prototype should register UnboundPrototypeService`() {
        component {
            prototype("a") { Heater() }
        }.shouldContainServiceOfType<UnboundPrototypeService<*>>(typeKey<Heater>("a"))
    }

    @Test
    fun `#singleton should register UnboundSingletonService`() {
        component {
            singleton("b") { Heater() }
        }.shouldContainServiceOfType<UnboundSingletonService<*>>(typeKey<Heater>("b"))
    }

    @Test
    fun `#eagerSingleton should register UnboundSingletonService`() {
        val c = component {
            eagerSingleton("c") { Heater() }
        }
        c.shouldContainServiceOfType<UnboundSingletonService<*>>(typeKey<Heater>("c"))
        // eager dependencies add a set of type keys of eager dependencies to the dependency
        // registry, so one more here than registered
        c.dependencies.size.shouldBe(2)
    }

    @Test
    fun `#weakSingleton should register UnboundWeakSingletonService`() {
        component {
            weakSingleton("d") { Heater() }
        }.shouldContainServiceOfType<UnboundWeakSingletonService<*>>(typeKey<Heater>("d"))
    }

    @Test
    fun `#softSingleton should register UnboundSoftSingletonService`() {
        component {
            softSingleton("e") { Heater() }
        }.shouldContainServiceOfType<UnboundSoftSingletonService<*>>(typeKey<Heater>("e"))
    }

    @Test
    fun `#factory should register UnboundFactoryService`() {
        component {
            factory("f") { c: Color -> Widget(c) }
        }.shouldContainServiceOfType<UnboundFactoryService<*, *>>(compoundTypeKey<Color, Widget>("f"))
    }

    @Test
    fun `#multiton should register UnboundMultitonFactoryService`() {
        component {
            multiton("g") { c: Color -> Widget(c) }
        }.shouldContainServiceOfType<UnboundMultitonFactoryService<*, *>>(compoundTypeKey<Color, Widget>("g"))
    }

    @Test
    fun `#constant should register ConstantService`() {
        component {
            constant(42, "h")
        }.shouldContainServiceOfType<ConstantService<*>>(typeKey<Int>("h"))
    }

    @Test
    fun `#register should throw an exception if the same key is register twice`() {
        shouldThrow<WinterException> {
            component {
                register(ConstantService(typeKey<String>(), ""), false)
                register(ConstantService(typeKey<String>(), ""), false)
            }
        }
    }

    @Test
    fun `#register should override key is if override is true`() {
        component {
            register(ConstantService(typeKey<String>(), ""), false)
            register(ConstantService(typeKey<String>(), ""), true)
        }.dependencies.size.shouldBe(1)
    }

    @Test
    fun `#include with subcomponent include mode 'DoNotInclude' should not include subcomponents`() {
        val c1 = component { subcomponent("sub") { constant("a", qualifier = "a") } }
        component {
            include(c1, subcomponentIncludeMode = DoNotInclude)
        }.dependencies.isEmpty().shouldBeTrue()
    }

    @Test
    fun `#include with subcomponent include mode 'DoNotIncludeIfAlreadyPresent' should not touch existing subcomponents`() {
        val c1 = component { subcomponent("sub") { constant("a", qualifier = "a") } }
        val c2 = component { subcomponent("sub") { constant("b", qualifier = "b") } }
        val c3 = c1.derive { include(c2, false, DoNotIncludeIfAlreadyPresent) }

        c3.subcomponent("sub").shouldNotContainService(typeKey<String>("b"))
        c3.subcomponent("sub").dependencies.size.shouldBe(1)
    }

    @Test
    fun `#include with subcomponent include mode 'Replace' should replace existing subcomponents`() {
        val c1 = component { subcomponent("sub") { constant("a", qualifier = "a") } }
        val c2 = component { subcomponent("sub") { constant("b", qualifier = "b") } }
        val c3 = c1.derive { include(c2, false, Replace) }

        c3.subcomponent("sub").shouldNotContainService(typeKey<String>("a"))
        c3.subcomponent("sub").dependencies.size.shouldBe(1)
    }

    @Test
    fun `#include with subcomponent include mode 'Merge' should merge existing subcomponents`() {
        val c1 = component { subcomponent("sub") { constant("a", qualifier = "a") } }
        val c2 = component { subcomponent("sub") { constant("b", qualifier = "b") } }
        val c3 = c1.derive { include(c2, false, Merge) }

        c3.subcomponent("sub").shouldContainService(typeKey<String>("a"))
        c3.subcomponent("sub").shouldContainService(typeKey<String>("b"))
        c3.subcomponent("sub").dependencies.size.shouldBe(2)
    }

    @Test
    fun `#include with subcomponent include mode 'Merge' should override existing provider`() {
        val c1 = component { subcomponent("sub") { constant("a", qualifier = "a") } }
        val c2 = component { subcomponent("sub") { constant("b", qualifier = "a") } }
        val c3 = c1.derive { include(c2, false, Merge) }

        c3.subcomponent("sub").dependencies.size.shouldBe(1)
        (c3.subcomponent("sub").dependencies[typeKey<String>("a")] as ConstantService).value.shouldBe("b")
    }

    @Test
    fun `#subcomponent should register a subcomponent`() {
        component {
            subcomponent("sub") { }
        }.shouldContainService(typeKey<Component>("sub"))
    }

    @Test
    fun `#subcomponent should extend existing subcomponent when deriveExisting is true`() {
        val base = component { subcomponent("sub") { constant("a", "a") } }
        val derived = base.derive { subcomponent("sub", deriveExisting = true) { constant("b", "b") } }
        val sub = derived.subcomponent("sub")

        sub.shouldContainService(typeKey<String>("a"))
        sub.shouldContainService(typeKey<String>("b"))
    }

    @Test
    fun `#subcomponent should throw exception when deriveExisting is true but subcomponent doesn't exist`() {
        shouldThrow<WinterException> {
            component { subcomponent("sub", deriveExisting = true) {} }
        }
    }

    @Test
    fun `#subcomponent should throw exception when override is true but subcomponent doesn't exist`() {
        shouldThrow<WinterException> {
            component { subcomponent("sub", override = true) {} }
        }
    }

    @Test
    fun `#subcomponent should throw exception when deriveExisting and override is true`() {
        val base = component { subcomponent("sub") {} }
        shouldThrow<WinterException> {
            base.derive { subcomponent("sub", deriveExisting = true, override = true) {} }
        }
    }

    @Test
    fun `#subcomponent should set qualifier to resulting subcomponent`() {
        component {
            subcomponent("sub") {}
        }.subcomponent("sub").qualifier.shouldBe("sub")
    }

    @Test
    fun `#remove should throw an exception when service doesn't exist`() {
        shouldThrow<WinterException> {
            component { remove(typeKey<Heater>()) }
        }
    }

    @Test
    fun `#remove should not throw an exception when service doesn't exist but silent is true`() {
        component { remove(typeKey<Heater>(), silent = true) }
    }

    @Test
    fun `#remove should remove service`() {
        val c1 = component { prototype { Heater() } }
        c1.derive { remove(typeKey<Heater>()) }.dependencies.size.shouldBe(0)
    }

    @Test
    fun `#remove should unregister eager singleton`() {
        val c1 = component { eagerSingleton { Heater() } }
        // eager dependencies add a set of type keys to the dependency map; so one more dependency
        c1.dependencies.size.shouldBe(2)
        c1.derive { remove(typeKey<Heater>()) }.dependencies.size.shouldBe(0)
    }

}