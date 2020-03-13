package io.jentz.winter.compiler.model

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.ImmutableKmProperty
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.isNullable
import io.jentz.winter.compiler.*
import javax.lang.model.element.*
import javax.lang.model.util.ElementFilter

data class InjectTargetModel(
    val originatingElement: Element,
    val targetName: String,
    val targetTypeName: TypeName,
    val isNullable: Boolean,
    val qualifier: String?,
    val targetType: TargetType,
    val fqdn: String
) {
    enum class TargetType { Field, Method, Property }

    companion object {

        @KotlinPoetMetadataPreview
        fun forElement(originatingElement: Element, kmClass: ImmutableKmClass?): InjectTargetModel {

            val typeElement: TypeElement = originatingElement.enclosingElement as TypeElement
            val fqdn = "${typeElement.qualifiedName}.${originatingElement.simpleName}"

            val variableElement: VariableElement
            val targetName: String
            val targetType: TargetType
            val kmProperty: ImmutableKmProperty?
            val qualifier: String?

            when (originatingElement.kind) {
                ElementKind.FIELD -> {
                    variableElement = originatingElement as VariableElement

                    kmProperty = kmClass
                        ?.properties
                        ?.find { it.fieldSignature?.name == variableElement.simpleName.toString() }
                        ?.takeIf { it.hasAccessibleSetter }

                    targetName = kmProperty?.name ?: variableElement.simpleName.toString()

                    val propertyQualifier = kmProperty?.let { getPropertyQualifier(it, typeElement) }

                    qualifier = propertyQualifier ?: variableElement.qualifier

                    targetType = if (kmProperty == null) TargetType.Field else TargetType.Property
                }
                ElementKind.METHOD -> {
                    val setter = originatingElement as ExecutableElement

                    require(setter.parameters.size == 1) {
                        "Setter for setter injection must have exactly one parameter not " +
                                "${originatingElement.parameters.size} ($fqdn)."
                    }

                    variableElement = setter.parameters.first()

                    kmProperty = kmClass
                        ?.properties
                        ?.find { it.setterSignature?.name == setter.simpleName.toString() }
                        ?.takeIf { it.hasAccessibleSetter }

                    targetName = kmProperty?.name ?: setter.simpleName.toString()

                    val argumentQualifier = setter.parameters.first().qualifier
                    val propertyQualifier = kmProperty?.let { getPropertyQualifier(it, typeElement) }

                    qualifier = propertyQualifier ?: argumentQualifier ?: setter.qualifier

                    targetType = if (kmProperty == null) TargetType.Method else TargetType.Property
                }
                else -> error("BUG: Unexpected element kind `${originatingElement.kind}`.")
            }

            require(!originatingElement.isPrivate || kmProperty?.hasAccessibleSetter == true) {
                "Cannot inject into private fields or properties ($fqdn)."
            }

            val targetTypeName = kmProperty?.returnType?.asTypeName()
                ?: variableElement.asType().asTypeName().kotlinTypeName

            return InjectTargetModel(
                originatingElement = originatingElement,
                targetName = targetName,
                targetTypeName = targetTypeName,
                isNullable = kmProperty?.returnType?.isNullable ?: variableElement.isNullable,
                qualifier = qualifier,
                targetType = targetType,
                fqdn = fqdn
            )

        }

        @KotlinPoetMetadataPreview
        private fun getPropertyQualifier(
            kmProperty: ImmutableKmProperty,
            typeElement: TypeElement
        ): String? {
            return kmProperty
                .syntheticMethodForAnnotations
                ?.let { signature ->
                    ElementFilter
                        .methodsIn(typeElement.enclosedElements)
                        .find { it.simpleName.contentEquals(signature.name) }
                }?.qualifier
        }
    }

}
