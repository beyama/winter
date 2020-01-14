package io.jentz.winter.compiler.kotlinbuilder

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import io.jentz.winter.compiler.*
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType

private fun simpleName(typeName: TypeName): KotlinCode = buildKotlinCode {
    when (typeName) {
        is ClassName -> {
            import(typeName)
            append(typeName.simpleName)
        }
        is ParameterizedTypeName -> {
            import(typeName.rawType)
            append(typeName.rawType.simpleName)

            val arguments = typeName.typeArguments
                    .map { simpleName(it) }
                    .onEach { import(it.imports) }
                    .joinToString(prefix = "<", postfix = ">") { it.code }
            append(arguments)
        }
    }
}

private fun getInstanceCode(
        target: String,
        typeName: TypeName,
        isNullable: Boolean,
        qualifier: String?
): KotlinCode = buildKotlinCode {
    val args = mutableListOf<String>()
    if (qualifier != null) args += "\"$qualifier\""
    if (typeName is ParameterizedTypeName) args += "generics = true"

    val simpleName = simpleName(typeName)

    if (isNullable) {
        append("${target}instanceOrNull")
    } else {
        append("${target}instance")
    }
    append("<")
    appendCode(simpleName)
    append(">")
    append("(${args.joinToString()})")
}

fun generateGetInstanceCode(
    target: String,
    variableElement: VariableElement,
    isNullable: Boolean,
    qualifier: String?
): KotlinCode = buildKotlinCode {
    when {
        variableElement.isProvider -> {
            val genericTypeMirror = (variableElement.asType() as DeclaredType).typeArguments.first()
            val typeName = mapTypeName(genericTypeMirror.asTypeName())

            import(PROVIDER_INTERFACE_NAME)

            append("Provider { ")
            appendCode(getInstanceCode(target, typeName, isNullable, qualifier))
            append(" }")
        }
        variableElement.isLazy -> {
            val genericTypeMirror = (variableElement.asType() as DeclaredType).typeArguments.first()
            val typeName = mapTypeName(genericTypeMirror.asTypeName())
            append("lazy { ")
            appendCode(getInstanceCode(target, typeName, isNullable, qualifier))
            append(" }")
        }
        else -> {
            val typeName = mapTypeName(variableElement.asType().asTypeName())
            appendCode(getInstanceCode(target, typeName, isNullable, qualifier))
        }
    }
}
