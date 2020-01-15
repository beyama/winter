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

/**
 * We can't really test Soft- and WeakReferences so this is a version that is baked by a field
 * instead of a reference so we have control and not the GC.
 */

internal class UnboundReferenceService<T : Any>(
    override val key: TypeKey<T>,
    val block: Graph.() -> T
) : UnboundService<T> {

    override val requiresLifecycleCallbacks: Boolean get() = false

    override fun bind(graph: Graph): BoundService<T> {
        return BoundReferenceService(graph, this)
    }
}

internal class BoundReferenceService<R : Any>(
    graph: Graph,
    override val unboundService: UnboundReferenceService<R>
) : AbstractBoundSingletonService<R>(graph) {

    var postConstructCalledCount = 0
    var postConstructLastArgument: Any? = null
    var closeCalled = 0

    override val scope: Scope get() = Scope("referenceTest")

    override fun onPostConstruct(instance: R) {
        postConstructCalledCount += 1
        postConstructLastArgument = instance
    }

    override fun onClose() {
        closeCalled += 1
    }

    public override var instance: Any = UNINITIALIZED_VALUE

    override fun newInstance(): R {
        return unboundService.block(graph).also { instance = it }
    }

}

internal inline fun <reified R : Any> Component.Builder.reference(noinline block: GFactory<R>) {
    register(UnboundReferenceService(typeKey(), block), false)
}
