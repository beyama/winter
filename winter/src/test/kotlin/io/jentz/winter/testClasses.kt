package io.jentz.winter

interface Pump

class Heater

class Thermosiphon(val heater: Heater) : Pump

class RotaryPump : Pump

class CoffeeMaker(val heater: Heater, val pump: Pump)

class Parent(val child: Child)

class Child {
    var parent: Parent? = null
}

enum class Color { RED, GREEN, BLUE }

class Widget(val color: Color)

open class Service {
    var property = 0
}

@Suppress("unused", "ClassName")
class Service_WinterMembersInjector : MembersInjector<Service> {
    override fun invoke(graph: Graph, target: Service) {
        target.property = 42
    }
}

@Suppress("unused", "ClassName")
class Service_WinterFactory : Factory<Graph, Service> {
    override fun invoke(graph: Graph): Service = Service()
}

class ExtendedService : Service()

/**
 * We can't really test Soft- and WeakReferences so this is a version that is baked by a field
 * instead of a reference so we have control and not the GC.
 */

internal class UnboundReferenceService<T : Any>(
    override val key: TypeKey<Unit, T>,
    val block: Graph.() -> T
) : UnboundService<Unit, T> {

    override val requiresLifecycleCallbacks: Boolean get() = false

    override fun bind(graph: Graph): BoundService<Unit, T> {
        return BoundReferenceService(graph, this)
    }
}

internal class BoundReferenceService<R : Any>(
    graph: Graph,
    override val unboundService: UnboundReferenceService<R>
) : AbstractBoundSingletonService<R>(graph) {

    var postConstructCalledCount = 0
    var postConstructLastArguments: Pair<Any, Any>? = null
    var disposeCalled = 0

    override val scope: Scope get() = Scope("referenceTest")

    override fun postConstruct(argument: Unit, instance: R) {
        postConstructCalledCount += 1
        postConstructLastArguments = argument to instance
    }

    override fun dispose() {
        disposeCalled += 1
    }

    public override var instance: Any = UNINITIALIZED_VALUE

    override fun newInstance(argument: Unit): R {
        return unboundService.block(graph).also { instance = it }
    }

}

internal inline fun <reified R : Any> ComponentBuilder.reference(noinline block: GFactory0<R>) {
    register(UnboundReferenceService(typeKey<R>(), block), false)
}
