package io.jentz.winter.compiler.test

import io.jentz.winter.inject.ApplicationScope
import io.jentz.winter.inject.InjectConstructor
import javax.inject.Inject
import javax.inject.Named

@ApplicationScope
@InjectConstructor
class KotlinProperties(
    val constructorInjectedString: String,
    @Named("someInt") val constructorInjectedPrimitive: Int
) {

    @Inject var primitiveProperty: Int = 0

    @set:Inject var primitiveSetter: Int = 0

    @Inject @Named("someInt") var namedPrimitiveProperty: Int = 0

    @set:[Inject Named("someInt")] var namedPrimitiveSetter: Int = 0

    @Inject lateinit var someList: List<String>

    @Inject lateinit var stringProvider: () -> String
}
