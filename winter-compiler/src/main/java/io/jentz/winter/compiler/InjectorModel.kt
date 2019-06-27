package io.jentz.winter.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import javax.lang.model.element.TypeElement

class InjectorModel(typeElement: TypeElement) {

    val typeName = typeElement.asClassName()

    val generatedClassName = ClassName(
            typeName.packageName,
            "WinterMembersInjector_${typeName.simpleNames.joinToString("_")}"
    )

    val targets: MutableSet<InjectTargetModel> = mutableSetOf()

}
