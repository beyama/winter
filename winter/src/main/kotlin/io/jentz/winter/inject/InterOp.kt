package io.jentz.winter.inject

import io.jentz.winter.*

/**
 * Helper for Winter compiler.
 */
object InterOp {

    @JvmStatic
    fun <R: Any> prototype(
        builder: Component.Builder,
        key: TypeKey<R>,
        override: Boolean,
        factory: GFactory<R>
    ) {
        builder.register(UnboundPrototypeService(key, factory, null), override)
    }

    @JvmStatic
    fun <R : Any> singleton(
        builder: Component.Builder,
        key: TypeKey<R>,
        override: Boolean,
        factory: GFactory<R>
    ) {
        builder.register(UnboundSingletonService(key, factory, null, null), override)
    }

    @JvmStatic
    fun <R : Any> eagerSingleton(
        builder: Component.Builder,
        key: TypeKey<R>,
        override: Boolean,
        factory: GFactory<R>
    ) {
        singleton(builder, key, override, factory)
        builder.addEagerDependency(key)
    }

}
