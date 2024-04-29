package io.jentz.winter

import io.jentz.winter.Component.Builder.SubcomponentIncludeMode.DoNotInclude
import io.jentz.winter.Component.Builder.SubcomponentIncludeMode.DoNotIncludeIfAlreadyPresent
import io.jentz.winter.Component.Builder.SubcomponentIncludeMode.Merge
import io.jentz.winter.Component.Builder.SubcomponentIncludeMode.Replace
import io.jentz.winter.dsl.prototypeOf
import io.jentz.winter.dsl.singletonOf
import io.jentz.winter.services.AliasService
import io.jentz.winter.services.ConstantService
import io.jentz.winter.services.MapOfProvidersForTypeService
import io.jentz.winter.services.MapOfTypeService
import io.jentz.winter.services.PrototypeService
import io.jentz.winter.services.SetOfProvidersForTypeService
import io.jentz.winter.services.SetOfTypeService
import io.jentz.winter.services.SingletonService
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
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
            prototype { Heater() }
        }.shouldContainServiceOfType<PrototypeService<*>>(typeKey<Heater>())
    }

    @Test
    fun `#prototypeOf should register UnboundPrototypeService`() {
        component {
            prototypeOf(::Heater)
        }.shouldContainServiceOfType<PrototypeService<*>>(typeKey<Heater>())
    }

    @Test
    fun `#singleton should register UnboundSingletonService`() {
        component {
            singleton { Heater() }
        }.shouldContainServiceOfType<SingletonService<*>>(typeKey<Heater>())
    }

    @Test
    fun `#singletonOf should register UnboundSingletonService`() {
        component {
            singletonOf(::Heater)
        }.shouldContainServiceOfType<SingletonService<*>>(typeKey<Heater>())
    }

    @Test
    fun `#constant should register ConstantService`() {
        component {
            constant(42)
        }.shouldContainServiceOfType<ConstantService<*>>(typeKey<Int>())
    }

    @Test
    fun `#setOfType should register SetOfTypeService`() {
        component {
            setOfType<String>()
        }.shouldContainServiceOfType<SetOfTypeService<*>>(typeKey<Set<String>>(generics = true))
    }

    @Test
    fun `#setOfProvidersForType should register SetOfProvidersForTypeService`() {
        component {
            setOfProvidersForType<String>()
        }.shouldContainServiceOfType<SetOfProvidersForTypeService<*>>(
            typeKey<Set<Provider<String>>>(generics = true)
        )
    }

    @Test
    fun `#mapOfType should register MapOfTypeService`() {
        component {
            mapOfType<String>()
        }.shouldContainServiceOfType<MapOfTypeService<*>>(
            typeKey<Map<Qualifier, String>>(generics = true)
        )
    }

    @Test
    fun `#mapOfProvidersForType should register MapOfProvidersForTypeService`() {
        component {
            mapOfProvidersForType<String>()
        }.shouldContainServiceOfType<MapOfProvidersForTypeService<*>>(
            typeKey<Map<Qualifier, Provider<String>>>(generics = true)
        )
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
    fun `#alias should override existing entry if override is true`() {
        component {
            prototype { Thermosiphon(instance()) }
            singleton<Pump> { Thermosiphon(instance()) }
            override {
                alias(typeKey<Thermosiphon>(), typeKey<Pump>())
            }
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
    fun `TypeKey#alias extension should override existing entry if override is true`() {
        component {
            singleton<Pump> { Thermosiphon(instance()) }
            override {
                prototype {
                    Thermosiphon(instance())
                }.alias<Pump>()
            }
        }.shouldContainServiceOfType<AliasService<*>>(typeKey<Pump>())
    }

    @Test
    fun `#containsKey with should return true if builder contains key otherwise false`() {
        component { containsKey(typeKey<Any>()).shouldBeFalse() }
        component {
            constant(Any())
            containsKey(typeKey<Any>()).shouldBeTrue()
        }
    }

    @Test
    fun `#containsKey should also check parent by default`() {
        component {
            constant(Any())
            subcomponent(Qualifier.sub) {
                containsKey(typeKey<Any>()).shouldBeTrue()
            }
        }
    }

    @Test
    fun `#containsKey should ignore parent if checkParent is false`() {
        component {
            constant(Any())
            subcomponent(Qualifier.sub) {
                containsKey(typeKey<Any>(), checkParent = false).shouldBeFalse()
            }
        }
    }

    @Test
    fun `#register should throw an exception if the same key is registered twice`() {
        shouldThrow<WinterException> {
            component {
                register(ConstantService(typeKey(), ""))
                register(ConstantService(typeKey(), ""))
            }
        }
    }

    @Test
    fun `#register should override key if override is true`() {
        component {
            register(ConstantService(typeKey(), ""))
            override { register(ConstantService(typeKey(), "")) }
        }.size.shouldBe(1)
    }

    @Test
    fun `#include with subcomponent include mode 'DoNotInclude' should not include subcomponents`() {
        val c1 = component {
            subcomponent(Qualifier.sub) {
                constant("a")
            }
        }

        component {
            include(c1, subcomponentIncludeMode = DoNotInclude)
        }.isEmpty().shouldBeTrue()
    }

    @Test
    fun `#include with subcomponent include mode 'DoNotIncludeIfAlreadyPresent' should not touch existing subcomponents`() {
        val c1 = component { subcomponent(Qualifier.sub) { constant("a", typeKey(Qualifier.a)) } }
        val c2 = component { subcomponent(Qualifier.sub) { constant("b", typeKey(Qualifier.b)) } }
        val c3 = c1.derive { include(c2, DoNotIncludeIfAlreadyPresent) }

        c3.subcomponent(Qualifier.sub).shouldNotContainService(typeKey<String>(Qualifier.b))
        c3.subcomponent(Qualifier.sub).size.shouldBe(1)
    }

    @Test
    fun `#include with subcomponent include mode 'Replace' should replace existing subcomponents`() {
        val c1 = component { subcomponent(Qualifier.sub) { constant("a", typeKey(Qualifier.a)) } }
        val c2 = component { subcomponent(Qualifier.sub) { constant("b", typeKey(Qualifier.b)) } }
        val c3 = c1.derive { include(c2, Replace) }

        c3.subcomponent(Qualifier.sub).shouldNotContainService(typeKey<String>(Qualifier.a))
        c3.subcomponent(Qualifier.sub).size.shouldBe(1)
    }

    @Test
    fun `#include with subcomponent include mode 'Merge' should merge existing subcomponents`() {
        val c1 = component { subcomponent(Qualifier.sub) { constant("a", typeKey(Qualifier.a)) } }
        val c2 = component { subcomponent(Qualifier.sub) { constant("b", typeKey(Qualifier.b)) } }
        val c3 = c1.derive { include(c2, Merge) }

        c3.subcomponent(Qualifier.sub).shouldContainService(typeKey<String>(Qualifier.a))
        c3.subcomponent(Qualifier.sub).shouldContainService(typeKey<String>(Qualifier.b))
        c3.subcomponent(Qualifier.sub).size.shouldBe(2)
    }

    @Test
    fun `#include with subcomponent include mode 'Merge' should override existing provider`() {
        val c1 = component { subcomponent(Qualifier.sub) { constant("a", typeKey(Qualifier.a)) } }
        val c2 = component { subcomponent(Qualifier.sub) { constant("b", typeKey(Qualifier.a)) } }
        val c3 = c1.derive { override { include(c2, Merge) }}

        c3.subcomponent(Qualifier.sub).size.shouldBe(1)
        (c3.subcomponent(Qualifier.sub)[typeKey<String>(Qualifier.a)] as ConstantService).value.shouldBe("b")
    }

    @Test
    fun `#subcomponent should register a subcomponent`() {
        component {
            subcomponent(Qualifier.sub) { }
        }.shouldContainService(typeKey<Component>(Qualifier.sub))
    }

    @Test
    fun `#subcomponent should extend existing subcomponent when deriveExisting is true`() {
        val base = component { subcomponent(Qualifier.sub) { constant("a", typeKey(Qualifier.a)) } }
        val derived = base.derive { subcomponent(Qualifier.sub, deriveExisting = true) { constant("b", typeKey(Qualifier.b)) } }
        val sub = derived.subcomponent(Qualifier.sub)

        sub.shouldContainService(typeKey<String>(Qualifier.a))
        sub.shouldContainService(typeKey<String>(Qualifier.b))
    }

    @Test
    fun `#subcomponent should replace existing subcomponent when override is true`() {
        val base = component {
            subcomponent(Qualifier.sub) {
                constant("a", typeKey(Qualifier.a))
            }
        }
        val derived = base.derive {
            override {
                subcomponent(Qualifier.sub) {
                    constant("b", typeKey(Qualifier.b))
                }
            }
        }
        val sub = derived.subcomponent(Qualifier.sub)

        sub.shouldNotContainService(typeKey<String>(Qualifier.a))
        sub.shouldContainService(typeKey<String>(Qualifier.b))
    }

    @Test
    fun `#subcomponent should throw an exception when deriveExisting and override is true`() {
        val base = component { subcomponent(Qualifier.sub) {} }
        shouldThrow<WinterException> {
            base.derive { override { subcomponent(Qualifier.sub, true) {} } }
        }
    }

    @Test
    fun `#subcomponent should set qualifier to resulting subcomponent`() {
        component {
            subcomponent(Qualifier.sub) {}
        }.subcomponent(Qualifier.sub).qualifier.shouldBe(Qualifier.sub)
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
        val c = component { singleton { Heater() }.eager() }
        // eager dependencies add a set of type keys to the dependency map; so one more dependency
        c.size.shouldBe(2)
        c.derive { remove(typeKey<Heater>()) }.size.shouldBe(0)
    }

}
