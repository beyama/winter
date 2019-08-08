package io.jentz.winter.compiler

import javax.lang.model.element.TypeElement

class ComponentModel {
    val factories = mutableListOf<ServiceModel>()
    val injectors = mutableMapOf<TypeElement, InjectorModel>()

    fun isEmpty() = factories.isEmpty() && injectors.isEmpty()
}
