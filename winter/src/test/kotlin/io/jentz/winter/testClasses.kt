package io.jentz.winter

import io.jentz.winter.inject.Factory
import io.jentz.winter.inject.MembersInjector

interface Pump

class Heater

class Thermosiphon(val heater: Heater) : Pump

class CoffeeMaker(val heater: Heater, val pump: Pump)

class Parent(val child: Child)

class Child {
    var parent: Parent? = null
}

open class Service {
    var property = 0
}

@Suppress("unused", "ClassName")
class Service_WinterMembersInjector : MembersInjector<Service> {
    override fun inject(graph: Graph, target: Service) {
        target.property = 42
    }
}

@Suppress("unused", "ClassName")
class Service_WinterFactory : Factory<Service> {
    override fun register(builder: Component.Builder, override: Boolean): TypeKey<Service> {
        return builder.prototype(factory = this)
    }

    override fun invoke(graph: Graph): Service = Service()
}

class ExtendedService : Service()
