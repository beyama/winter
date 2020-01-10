package io.jentz.winter

import io.jentz.winter.Component.Builder.SubcomponentIncludeMode.*
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ComponentBuilderTest {

    @Test
    fun `empty builder should result in empty dependency map`() {
        component { }.isEmpty().shouldBeTrue()
    }

    @Test
    fun `#prototype should register UnboundPrototypeService`() {
        component {
            prototype("a") { Heater() }
        }.shouldContainServiceOfType<UnboundPrototypeService<*>>(typeKey<Heater>("a"))
    }

    @Test
    fun `#prototype should return the type key`() {
        component {
            prototype("a") { Heater() }
                .shouldBe(typeKey<Heater>("a"))
        }
    }

    @Test
    fun `#singleton should register UnboundSingletonService`() {
        component {
            singleton("b") { Heater() }
        }.shouldContainServiceOfType<UnboundSingletonService<*>>(typeKey<Heater>("b"))
    }

    @Test
    fun `#singleton should return the type key`() {
        component {
            singleton("b") { Heater() }
                .shouldBe(typeKey<Heater>("b"))
        }
    }

    @Test
    fun `#eagerSingleton should register UnboundSingletonService`() {
        val c = component {
            eagerSingleton("c") { Heater() }
        }
        c.shouldContainServiceOfType<UnboundSingletonService<*>>(typeKey<Heater>("c"))
        // eager dependencies add a set of type keys of eager dependencies to the dependency
        // registry
        c.size.shouldBe(2)
        c.shouldContainService(eagerDependenciesKey)
    }

    @Test
    fun `#eagerSingleton should return the type key`() {
        component {
            eagerSingleton("c") { Heater() }
                .shouldBe(typeKey<Heater>("c"))
        }
    }

    @Test
    fun `#weakSingleton should register UnboundWeakSingletonService`() {
        component {
            weakSingleton("d") { Heater() }
        }.shouldContainServiceOfType<UnboundWeakSingletonService<*>>(typeKey<Heater>("d"))
    }

    @Test
    fun `#weakSingleton should return the type key`() {
        component {
            weakSingleton("d") { Heater() }
                .shouldBe(typeKey<Heater>("d"))
        }
    }

    @Test
    fun `#softSingleton should register UnboundSoftSingletonService`() {
        component {
            softSingleton("e") { Heater() }
        }.shouldContainServiceOfType<UnboundSoftSingletonService<*>>(typeKey<Heater>("e"))
    }

    @Test
    fun `#softSingleton should return the type key`() {
        component {
            softSingleton("e") { Heater() }
                .shouldBe(typeKey<Heater>("e"))
        }
    }

    @Test
    fun `#constant should register ConstantService`() {
        component {
            constant(42, "h")
        }.shouldContainServiceOfType<ConstantService<*>>(typeKey<Int>("h"))
    }

    @Test
    fun `#constant should return the type key`() {
        component {
            constant(42, "h")
                .shouldBe(typeKey<Int>("h"))
        }
    }

    @Test
    fun `#alias should register alias service`() {
        component {
            prototype { Thermosiphon(instance()) }
            alias(typeKey<Thermosiphon>(), typeKey<Pump>())
        }.shouldContainServiceOfType<AliasService<*>>(typeKey<Pump>())
    }

    @Test
    fun `#alias should return the target type key`() {
        component {
            prototype { Thermosiphon(instance()) }
            alias(typeKey<Thermosiphon>(), typeKey<Pump>())
                .shouldBe(typeKey<Thermosiphon>())
        }
    }

    @Test
    fun `#alias should throw an exception if from key doesn't exist`() {
        shouldThrow<EntryNotFoundException> {
            component { alias(typeKey<Thermosiphon>(), typeKey<Pump>()) }
        }
    }

    @Test
    fun `#alias should override existing entry if override is true`() {
        component {
            prototype { Thermosiphon(instance()) }
            singleton<Pump> { Thermosiphon(instance()) }
            alias(typeKey<Thermosiphon>(), typeKey<Pump>(), override = true)
        }.shouldContainServiceOfType<AliasService<*>>(typeKey<Pump>())
    }

    @Test
    fun `#alias should throw an exception if to key already exists and override is false (default)`() {
        shouldThrow<WinterException> {
            component {
                prototype { Thermosiphon(instance()) }
                prototype<Pump> { Thermosiphon(instance()) }
                alias(typeKey<Thermosiphon>(), typeKey<Pump>())
            }
        }
    }

    @Test
    fun `TypeKey#alias extension should register alias`() {
        component {
            prototype {
                Thermosiphon(instance())
            }.alias<Pump>()
        }.shouldContainServiceOfType<AliasService<*>>(typeKey<Pump>())
    }

    @Test
    fun `#generated should register generated factory for class`() {
        component {
            generated<Service>()
        }.shouldContainService(typeKey<Service>())
    }

    @Test
    fun `#generatedFactory should load generated factory for class`() {
        component {
            generatedFactory<Service>().shouldBeInstanceOf<Service_WinterFactory>()
        }
    }

    @Test
    fun `#register should throw an exception if the same key is registered twice`() {
        shouldThrow<WinterException> {
            component {
                register(ConstantService(typeKey(), ""), false)
                register(ConstantService(typeKey(), ""), false)
            }
        }
    }

    @Test
    fun `#register should override key if override is true`() {
        component {
            register(ConstantService(typeKey(), ""), false)
            register(ConstantService(typeKey(), ""), true)
        }.size.shouldBe(1)
    }

    @Test
    fun `#include with subcomponent include mode 'DoNotInclude' should not include subcomponents`() {
        val c1 = component { subcomponent("sub") { constant("a", qualifier = "a") } }
        component {
            include(c1, subcomponentIncludeMode = DoNotInclude)
        }.isEmpty().shouldBeTrue()
    }

    @Test
    fun `#include with subcomponent include mode 'DoNotIncludeIfAlreadyPresent' should not touch existing subcomponents`() {
        val c1 = component { subcomponent("sub") { constant("a", qualifier = "a") } }
        val c2 = component { subcomponent("sub") { constant("b", qualifier = "b") } }
        val c3 = c1.derive { include(c2, false, DoNotIncludeIfAlreadyPresent) }

        c3.subcomponent("sub").shouldNotContainService(typeKey<String>("b"))
        c3.subcomponent("sub").size.shouldBe(1)
    }

    @Test
    fun `#include with subcomponent include mode 'Replace' should replace existing subcomponents`() {
        val c1 = component { subcomponent("sub") { constant("a", qualifier = "a") } }
        val c2 = component { subcomponent("sub") { constant("b", qualifier = "b") } }
        val c3 = c1.derive { include(c2, false, Replace) }

        c3.subcomponent("sub").shouldNotContainService(typeKey<String>("a"))
        c3.subcomponent("sub").size.shouldBe(1)
    }

    @Test
    fun `#include with subcomponent include mode 'Merge' should merge existing subcomponents`() {
        val c1 = component { subcomponent("sub") { constant("a", qualifier = "a") } }
        val c2 = component { subcomponent("sub") { constant("b", qualifier = "b") } }
        val c3 = c1.derive { include(c2, false, Merge) }

        c3.subcomponent("sub").shouldContainService(typeKey<String>("a"))
        c3.subcomponent("sub").shouldContainService(typeKey<String>("b"))
        c3.subcomponent("sub").size.shouldBe(2)
    }

    @Test
    fun `#include with subcomponent include mode 'Merge' should override existing provider`() {
        val c1 = component { subcomponent("sub") { constant("a", qualifier = "a") } }
        val c2 = component { subcomponent("sub") { constant("b", qualifier = "a") } }
        val c3 = c1.derive { include(c2, false, Merge) }

        c3.subcomponent("sub").size.shouldBe(1)
        (c3.subcomponent("sub")[typeKey<String>("a")] as ConstantService).value.shouldBe("b")
    }

    @Test
    fun `#include should throw an exception if a subcomponent has the same qualifier as its parent`() {
        val c1 = component { subcomponent("sub") {} }
        shouldThrow<WinterException> {
            component("sub") {
                allowComponentQualifier(APPLICATION_COMPONENT_QUALIFIER) {
                    include(c1)
                }
            }
        }.message.shouldBe("Subcomponent must have unique qualifier (qualifier `sub` is roots component qualifier).")
    }

    @Test
    fun `#include should throw an exception if included component has different qualifier`() {
        val other = component("other") {}
        shouldThrow<WinterException> {
            component { include(other) }
        }.message.shouldBe("Component qualifier `other` does not match required qualifier `$APPLICATION_COMPONENT_QUALIFIER`.")
    }

    @Test
    fun `#checkComponentQualifier should throw an exception if given qualifier doesn't match the component qualifier`() {
        shouldThrow<WinterException> {
            component { checkComponentQualifier("foo") }
        }.message.shouldBe("Component qualifier `foo` does not match required qualifier `application`.")
    }

    @Test
    fun `#checkComponentQualifier should not throw an exception if a different qualifier was set with allowComponentQualifier`() {
        component { allowComponentQualifier("foo") { checkComponentQualifier("foo") } }
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
    fun `#subcomponent should throw an exception when deriveExisting is true but subcomponent doesn't exist`() {
        shouldThrow<WinterException> {
            component { subcomponent("sub", deriveExisting = true) {} }
        }
    }

    @Test
    fun `#subcomponent should throw an exception when override is true but subcomponent doesn't exist`() {
        shouldThrow<WinterException> {
            component { subcomponent("sub", override = true) {} }
        }
    }

    @Test
    fun `#subcomponent should throw an exception when deriveExisting and override is true`() {
        val base = component { subcomponent("sub") {} }
        shouldThrow<WinterException> {
            base.derive { subcomponent("sub", deriveExisting = true, override = true) {} }
        }
    }

    @Test
    fun `#subcomponent should throw an exception when subcomponent qualifier is not unique`() {
        shouldThrow<WinterException> {
            component { subcomponent(APPLICATION_COMPONENT_QUALIFIER) {} }
        }.message.shouldBe("Subcomponent must have unique qualifier (qualifier `application` is roots component qualifier).")
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
        c1.derive { remove(typeKey<Heater>()) }.size.shouldBe(0)
    }

    @Test
    fun `#remove should unregister eager singleton`() {
        val c = component { eagerSingleton { Heater() } }
        // eager dependencies add a set of type keys to the dependency map; so one more dependency
        c.size.shouldBe(2)
        c.derive { remove(typeKey<Heater>()) }.size.shouldBe(0)
    }

    @Test
    fun `#allowComponentQualifier should allow different component qualifier inside the block`() {
        val other = component("other") {}
        component {
            allowComponentQualifier("other") {
                include(other)
            }
        }
    }

    @Nested
    inner class Validation {

        @Test
        fun `should validate that component qualifiers are unique in component tree`() {
            shouldThrow<WinterException> {
                component {
                    subcomponent("sub") {
                        subcomponent(APPLICATION_COMPONENT_QUALIFIER) {}
                    }
                }
            }.message.shouldBe("Subcomponent must have unique qualifier (qualifier `application` is roots component qualifier).")

            val c = component {
                subcomponent("sub") {
                    subcomponent("sub sub") {}
                }
            }

            val c2 = component {
                subcomponent("sub sub") {}
            }

            shouldThrow<WinterException> {
                c.derive { include(c2) }
            }.message.shouldBe("Subcomponent with qualifier `sub sub` already exists.")
        }

        @Test
        fun `should not fail with non-unique qualifier exception when subcomponent was removed`() {
            val c = component {
                subcomponent("sub") {
                    subcomponent("sub sub") {}
                }
            }

            val c2 = component {
                subcomponent("sub sub") {}
                remove(typeKey<Component>("sub sub"))
            }

            c2.derive { include(c) }
        }

    }


}
