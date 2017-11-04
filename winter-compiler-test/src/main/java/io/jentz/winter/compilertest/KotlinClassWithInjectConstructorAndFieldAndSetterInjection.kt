package io.jentz.winter.compilertest

import javax.inject.Inject

class KotlinClassWithInjectConstructorAndFieldAndSetterInjection @Inject constructor(val dep0: NoArgumentInjectConstructor) {

    lateinit var _dep3: NoArgumentInjectConstructor

    @Inject lateinit var dep1: OneArgumentInjectConstructor

    @field:[Inject]
    lateinit var dep2: SingletonWithInjectConstructorAndInjectedFields

    @Inject
    fun dep3(dep: NoArgumentInjectConstructor) {
        _dep3 = dep
    }
}