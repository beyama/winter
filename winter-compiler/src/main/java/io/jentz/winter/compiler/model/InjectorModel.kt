package io.jentz.winter.compiler.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

@KotlinPoetMetadataPreview
class InjectorModel(
    val originatingElement: TypeElement,
    superClassWithInjector: TypeElement?,
    private val kmClass: ImmutableKmClass?
) {

    val typeName = originatingElement.asClassName()

    val generatedClassName = generatedClassNameForClassName(typeName)

    val superclassInjectorClassName = superClassWithInjector
        ?.let { generatedClassNameForClassName(it.asClassName()) }

    private val _targets: MutableList<InjectTargetModel> = mutableListOf()

    val targets: List<InjectTargetModel> get() = _targets

    fun addTarget(fieldOrSetter: Element) {
        _targets += InjectTargetModel.forElement(fieldOrSetter, kmClass)
    }

    private fun generatedClassNameForClassName(name: ClassName) = ClassName(
        name.packageName,
        "${name.simpleNames.joinToString("_")}_WinterMembersInjector"
    )

}
