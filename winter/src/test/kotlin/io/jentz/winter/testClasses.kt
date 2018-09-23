package io.jentz.winter

interface Pump

class Heater

class Thermosiphon(val heater: Heater) : Pump

class CoffeeMaker(val heater: Heater, val pump: Pump)

class Parent(val child: Child)

class Child {
    var parent: Parent? = null
}

enum class Color { RED, GREEN, BLUE }

class Widget(val color: Color)

interface ServiceDependency {
    val aValue: String?
}

interface GenericDependency<out T> {
    val aValue: T?
}

class ServiceDependencyImpl(override val aValue: String? = null) : ServiceDependency

class GenericDependencyImpl<out T>(override val aValue: T?) : GenericDependency<T>

/**
 * We can't really test Soft- and WeakReferences so this is a version that is baked by a field
 * instead of a reference so we have control and not the GC.
 */

internal class UnboundReferenceService<T : Any>(
        override val key: DependencyKey,
        val block: Graph.() -> T
) : UnboundService<Unit, T> {

    override fun bind(graph: Graph): BoundService<Unit, T> {
        return BoundReferenceService(graph, this)
    }
}

internal class BoundReferenceService<T : Any>(
        graph: Graph,
        override val unboundService: UnboundReferenceService<T>
) : AbstractBoundSingletonService<T>(graph) {

    var postConstructCalledCount = 0
    var postConstructLastArguments: Pair<Any, Any>? = null
    var disposeCalled = 0


    override fun postConstruct(arg: Any, instance: Any) {
        postConstructCalledCount += 1
        postConstructLastArguments = arg to instance
    }

    override fun dispose() {
        disposeCalled += 1
    }

    public override var instance: Any = UNINITIALIZED_VALUE

    override fun initialize(): T {
        val instance = graph.evaluate(this, Unit) { unboundService.block(graph) }
        this.instance = instance
        graph.postConstruct()
        return instance
    }
}

internal inline fun <reified T : Any> ComponentBuilder.reference(noinline block: ProviderBlock<T>) {
    register(UnboundReferenceService(typeKey<T>(), block), false)
}
