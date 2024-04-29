package io.jentz.winter.services

import io.jentz.winter.Prototype
import io.jentz.winter.Qualifier
import io.jentz.winter.TypeKey

internal abstract class OfTypeService<T : Any, R : Any>(
    override val key: TypeKey<R>,
    val typeOfKey: TypeKey<T>
) : UnboundService<R> {

    override val scope: Qualifier
        get() = Prototype

    override val requiresPostConstructCallback: Boolean
        get() = false

}

