package io.jentz.winter

interface ServiceDependency {
    val aValue: String?
}

interface Service {
    val dependency: ServiceDependency
}

interface GenericDependency<out T> {
    val aValue: T?
}

interface GenericService<out T> {
    val dependency: GenericDependency<T>
}

class ServiceDependencyImpl(override val aValue: String? = null) : ServiceDependency

class ServiceImpl(override val dependency: ServiceDependency) : Service

class GenericDependencyImpl<out T>(override val aValue: T?) : GenericDependency<T>

class GenericServiceImpl<out T>(override val dependency: GenericDependency<T>) : GenericService<T>

abstract class InjectorAwareBase : InjectorAware {
    override val injector = Injector()
}