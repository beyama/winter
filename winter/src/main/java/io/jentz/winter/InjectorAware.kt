package io.jentz.winter

interface InjectorAware {
    val injector: Injector
}

inline fun <reified T> InjectorAware.instance(qualifier: String? = null, generics: Boolean = false)
        = injector.instance<T>(qualifier, generics)

inline fun <reified A, reified R> InjectorAware.factory(qualifier: String? = null, generics: Boolean = false)
        = injector.factory<A, R>(qualifier, generics)

fun InjectorAware.inject(graph: Graph) = injector.inject(graph)