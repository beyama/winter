package io.jentz.winter

import io.jentz.winter.Component.Builder.SubcomponentIncludeMode.DoNotInclude
import io.jentz.winter.Component.Builder.SubcomponentIncludeMode.DoNotIncludeIfAlreadyPresent
import io.jentz.winter.Component.Builder.SubcomponentIncludeMode.Merge
import io.jentz.winter.Component.Builder.SubcomponentIncludeMode.Replace
import io.jentz.winter.dsl.prototypeOf
import io.jentz.winter.dsl.singletonOf
import io.jentz.winter.services.AliasService
import io.jentz.winter.services.ConstantService
import io.jentz.winter.services.PrototypeService
import io.jentz.winter.services.SingletonService
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
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
        }.shouldContainServiceOfType<PrototypeService<*>>(erased<Heater>())
    }

    @Test
    fun `#prototypeOf should register UnboundPrototypeService`() {
        component {
            prototypeOf(::Heater)
        }.shouldContainServiceOfType<PrototypeService<*>>(erased<Heater>())
    }

    @Test
    fun `#singleton should register UnboundSingletonService`() {
        component {
            singleton { Heater() }
        }.shouldContainServiceOfType<SingletonService<*>>(erased<Heater>())
    }

    @Test
    fun `#singletonOf should register UnboundSingletonService`() {
        component {
            singletonOf(::Heater)
        }.shouldContainServiceOfType<SingletonService<*>>(erased<Heater>())
    }

    @Test
    fun `#constant should register ConstantService`() {
        component {
            constant(42)
        }.shouldContainServiceOfType<ConstantService<*>>(erased<Int>())
    }

    @Test
    fun `#alias should register alias service`() {
        component {
            prototype { Thermosiphon(instance()) }
            alias(erased<Thermosiphon>(), erased<Pump>())
        }.shouldContainServiceOfType<AliasService<*>>(erased<Pump>())
    }

    @Test
    fun `#alias should override existing entry if override is true`() {
        component {
            prototype { Thermosiphon(instance()) }
            singleton<Pump> { Thermosiphon(instance()) }
            override {
                alias(erased<Thermosiphon>(), erased<Pump>())
            }
        }.shouldContainServiceOfType<AliasService<*>>(erased<Pump>())
    }

    @Test
    fun `#alias should throw an exception if to key already exists and override is false (default)`() {
        shouldThrow<WinterException> {
            component {
                prototype { Thermosiphon(instance()) }
                prototype<Pump> { Thermosiphon(instance()) }
                alias(erased<Thermosiphon>(), erased<Pump>())
            }
        }
    }

    @Test
    fun `UnboundService#alias extension should register alias`() {
        component {
            prototype {
                Thermosiphon(instance())
            }.alias(erased<Pump>())
        }.shouldContainServiceOfType<AliasService<*>>(erased<Pump>())
    }

    @Test
    fun `UnboundService#alias extension should override existing entry if override is true`() {
        component {
            singleton<Pump> { Thermosiphon(instance()) }
            override {
                prototype {
                    Thermosiphon(instance())
                }.alias(erased<Pump>())
            }
        }.shouldContainServiceOfType<AliasService<*>>(erased<Pump>())
    }

    @Test
    fun `UnboundService#alias extension with class argument should register alias`() {
        component {
            prototype {
                Thermosiphon(instance())
            }.alias(Pump::class, QualifierTest)
        }.shouldContainServiceOfType<AliasService<*>>(erased<Pump>(QualifierTest))
    }

    @Test
    fun `#containsKey with should return true if builder contains key otherwise false`() {
        component { containsKey(erased<Any>()).shouldBeFalse() }
        component {
            constant(Any())
            containsKey(erased<Any>()).shouldBeTrue()
        }
    }

    @Test
    fun `#containsKey should also check parent by default`() {
        component {
            constant(Any())
            subcomponent(QualifierSub) {
                containsKey(erased<Any>()).shouldBeTrue()
            }
        }
    }

    @Test
    fun `#containsKey should ignore parent if checkParent is false`() {
        component {
            constant(Any())
            subcomponent(QualifierSub) {
                containsKey(erased<Any>(), checkParent = false).shouldBeFalse()
            }
        }
    }

    @Test
    fun `#register should throw an exception if the same key is registered twice`() {
        shouldThrow<WinterException> {
            component {
                register(ConstantService(erased(), ""))
                register(ConstantService(erased(), ""))
            }
        }
    }

    @Test
    fun `#register should override key if override is true`() {
        component {
            register(ConstantService(erased(), ""))
            override { register(ConstantService(erased(), "")) }
        }.size.shouldBe(1)
    }

    @Test
    fun `#include with subcomponent include mode 'DoNotInclude' should not include subcomponents`() {
        val c1 = component {
            subcomponent(QualifierSub) {
                constant("a")
            }
        }

        component {
            include(c1, subcomponentIncludeMode = DoNotInclude)
        }.isEmpty().shouldBeTrue()
    }

    @Test
    fun `#include with subcomponent include mode 'DoNotIncludeIfAlreadyPresent' should not touch existing subcomponents`() {
        val c1 = component { subcomponent(QualifierSub) { constant("a", erased(QualifierA)) } }
        val c2 = component { subcomponent(QualifierSub) { constant("b", erased(QualifierB)) } }
        val c3 = c1.derive { include(c2, DoNotIncludeIfAlreadyPresent) }

        c3.subcomponent(QualifierSub).shouldNotContainService(erased<String>(QualifierB))
        c3.subcomponent(QualifierSub).size.shouldBe(1)
    }

    @Test
    fun `#include with subcomponent include mode 'Replace' should replace existing subcomponents`() {
        val c1 = component { subcomponent(QualifierSub) { constant("a", erased(QualifierA)) } }
        val c2 = component { subcomponent(QualifierSub) { constant("b", erased(QualifierB)) } }
        val c3 = c1.derive { include(c2, Replace) }

        c3.subcomponent(QualifierSub).shouldNotContainService(erased<String>(QualifierA))
        c3.subcomponent(QualifierSub).size.shouldBe(1)
    }

    @Test
    fun `#include with subcomponent include mode 'Merge' should merge existing subcomponents`() {
        val c1 = component { subcomponent(QualifierSub) { constant("a", erased(QualifierA)) } }
        val c2 = component { subcomponent(QualifierSub) { constant("b", erased(QualifierB)) } }
        val c3 = c1.derive { include(c2, Merge) }

        c3.subcomponent(QualifierSub).shouldContainService(erased<String>(QualifierA))
        c3.subcomponent(QualifierSub).shouldContainService(erased<String>(QualifierB))
        c3.subcomponent(QualifierSub).size.shouldBe(2)
    }

    @Test
    fun `#include with subcomponent include mode 'Merge' should override existing provider`() {
        val c1 = component { subcomponent(QualifierSub) { constant("a", erased(QualifierA)) } }
        val c2 = component { subcomponent(QualifierSub) { constant("b", erased(QualifierA)) } }
        val c3 = c1.derive { override { include(c2, Merge) }}

        c3.subcomponent(QualifierSub).size.shouldBe(1)
        (c3.subcomponent(QualifierSub)[erased<String>(QualifierA)] as ConstantService).value.shouldBe("b")
    }

    @Test
    fun `#subcomponent should register a subcomponent`() {
        component {
            subcomponent(QualifierSub) { }
        }.shouldContainService(erased<Component>(QualifierSub))
    }

    @Test
    fun `#subcomponent should extend existing subcomponent when deriveExisting is true`() {
        val base = component { subcomponent(QualifierSub) { constant("a", erased(QualifierA)) } }
        val derived = base.derive { subcomponent(QualifierSub, deriveExisting = true) { constant("b", erased(QualifierB)) } }
        val sub = derived.subcomponent(QualifierSub)

        sub.shouldContainService(erased<String>(QualifierA))
        sub.shouldContainService(erased<String>(QualifierB))
    }

    @Test
    fun `#subcomponent should replace existing subcomponent when override is true`() {
        val base = component {
            subcomponent(QualifierSub) {
                constant("a", erased(QualifierA))
            }
        }
        val derived = base.derive {
            override {
                subcomponent(QualifierSub) {
                    constant("b", erased(QualifierB))
                }
            }
        }
        val sub = derived.subcomponent(QualifierSub)

        sub.shouldNotContainService(erased<String>(QualifierA))
        sub.shouldContainService(erased<String>(QualifierB))
    }

    @Test
    fun `#subcomponent should throw an exception when deriveExisting and override is true`() {
        val base = component { subcomponent(QualifierSub) {} }
        shouldThrow<WinterException> {
            base.derive { override { subcomponent(QualifierSub, true) {} } }
        }
    }

    @Test
    fun `#subcomponent should set qualifier to resulting subcomponent`() {
        component {
            subcomponent(QualifierSub) {}
        }.subcomponent(QualifierSub).qualifier.shouldBe(QualifierSub)
    }

    @Test
    fun `#remove should throw an exception when service doesn't exist`() {
        shouldThrow<WinterException> {
            component { remove(erased<Heater>()) }
        }
    }

    @Test
    fun `#remove should not throw an exception when service doesn't exist but silent is true`() {
        component { remove(erased<Heater>(), silent = true) }
    }

    @Test
    fun `#remove should remove service`() {
        val c1 = component { prototype { Heater() } }
        c1.derive { remove(erased<Heater>()) }.size.shouldBe(0)
    }

    @Test
    fun `#remove should unregister eager singleton`() {
        val c = component { singleton { Heater() }.eager() }
        // eager dependencies add a set of type keys to the dependency map; so one more dependency
        c.size.shouldBe(2)
        c.derive { remove(erased<Heater>()) }.size.shouldBe(0)
    }

}
