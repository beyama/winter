package io.jentz.winter.compiler.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import javax.lang.model.element.TypeElement

class InjectorModel(
    val originatingElement: TypeElement,
    superClassWithInjector: TypeElement?
) {

    val typeName = originatingElement.asClassName()

    val generatedClassName = generatedClassNameForClassName(typeName)

    val superclassInjectorClassName = superClassWithInjector
        ?.let { generatedClassNameForClassName(it.asClassName()) }

    val targets: MutableSet<InjectTargetModel> = mutableSetOf()

    private fun generatedClassNameForClassName(name: ClassName) = ClassName(
        name.packageName,
        "${name.simpleNames.joinToString("_")}_WinterMembersInjector"
    )

}
