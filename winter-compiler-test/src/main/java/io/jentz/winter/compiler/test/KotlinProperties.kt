package io.jentz.winter.compiler.test

import javax.inject.Inject
import javax.inject.Named

class KotlinProperties {

    @Inject var primitiveProperty: Int = 0
    @set:Inject var primitiveSetter: Int = 0

    @Inject @Named("someInt") var namedPrimitiveProperty: Int = 0
    @set:[Inject Named("someInt")] var namedPrimitiveSetter: Int = 0
}
