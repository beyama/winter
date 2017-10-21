package io.jentz.winter

interface InjectorAware {
    val injector: Injector
}

inline fun <reified T : Any> InjectorAware.instance(qualifier: String? = null, generics: Boolean = false)
        = injector.instance<T>(qualifier, generics)

inline fun <reified T : Any?> InjectorAware.instanceOrNull(qualifier: String? = null, generics: Boolean = false)
        = injector.instanceOrNull<T>(qualifier, generics)

inline fun <reified A, reified R> InjectorAware.factory(qualifier: String? = null, generics: Boolean = false)
        = injector.factory<A, R>(qualifier, generics)

inline fun <reified A, reified R> InjectorAware.curriedFactory(argument: A, qualifier: String? = null, generics: Boolean = false)
        = injector.curriedFactory<A, R>(argument, qualifier, generics)

inline fun <reified T : Any> InjectorAware.lazyInstance(qualifier: String? = null, generics: Boolean = false)
        = injector.lazyInstance<T>(qualifier, generics)

inline fun <reified T : Any?> InjectorAware.lazyInstanceOrNull(qualifier: String? = null, generics: Boolean = false)
        = injector.lazyInstanceOrNull<T>(qualifier, generics)

fun InjectorAware.inject(graph: Graph) = injector.inject(graph)