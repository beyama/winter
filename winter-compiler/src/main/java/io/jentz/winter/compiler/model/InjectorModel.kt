package io.jentz.winter.compiler.model

import com.squareup.javapoet.ClassName
import io.jentz.winter.compiler.KotlinMetadata
import javax.lang.model.element.*

class InjectorModel(
    val originatingElement: TypeElement,
    superClassWithInjector: TypeElement?,
    private val kotlinMetadata: KotlinMetadata?
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
                val kotlinProperty = kotlinMetadata
                    ?.getKotlinPropertyForField(field)
                    ?.takeIf { it.hasAccessibleSetter }

                _targets += FieldInjectTarget(field, kotlinProperty)
            }
            ElementKind.METHOD -> {
                val setter = fieldOrSetter as ExecutableElement
                val kotlinProperty = kotlinMetadata
                    ?.getKotlinPropertyForSetter(setter)
                    ?.takeIf { it.hasAccessibleSetter }

                _targets += SetterInjectTarget(setter, kotlinProperty)
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
