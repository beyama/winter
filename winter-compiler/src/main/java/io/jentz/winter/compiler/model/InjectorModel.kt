package io.jentz.winter.compiler.model

import com.squareup.javapoet.ClassName
import io.jentz.winter.compiler.hasAccessibleSetter
import kotlinx.metadata.KmClass
import kotlinx.metadata.jvm.fieldSignature
import kotlinx.metadata.jvm.setterSignature
import javax.lang.model.element.*

class InjectorModel(
    val originatingElement: TypeElement,
    superClassWithInjector: TypeElement?,
    private val kmClass: KmClass?
) {

    val typeName: ClassName = ClassName.get(originatingElement)

    val generatedClassName: ClassName = generatedClassNameForClassName(typeName)

    val superclassInjectorClassName = superClassWithInjector
        ?.let { generatedClassNameForClassName(ClassName.get(it)) }

    private val _targets: MutableSet<InjectTargetModel> = mutableSetOf()

    val targets: Set<InjectTargetModel> get() = _targets

    fun addTarget(fieldOrSetter: Element) {
        when (fieldOrSetter.kind) {
            ElementKind.FIELD -> {
                val field = fieldOrSetter as VariableElement
                val kmProperty = kmClass
                    ?.properties
                    ?.find { it.fieldSignature?.name == field.simpleName.toString() }
                    ?.takeIf { it.hasAccessibleSetter }

                _targets += FieldInjectTarget(field, kmProperty)
            }
            ElementKind.METHOD -> {
                val setter = fieldOrSetter as ExecutableElement
                val kmProperty = kmClass
                    ?.properties
                    ?.find { it.setterSignature?.name == setter.simpleName.toString() }
                    ?.takeIf { it.hasAccessibleSetter }

                _targets += SetterInjectTarget(setter, kmProperty)
            }
            else -> {
                throw IllegalArgumentException("fieldOrSetter must be VariableElement or ExecutableElement")
            }
        }
    }

    private fun generatedClassNameForClassName(name: ClassName) = ClassName.get(
        name.packageName(),
        "${name.simpleNames().joinToString("_")}_WinterMembersInjector"
    )

}
