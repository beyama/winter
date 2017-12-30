package io.jentz.winter.compiler

import com.squareup.kotlinpoet.*
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import javax.inject.Scope
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

class FactoryModel(val constructor: ExecutableElement) {
    val typeElement = (constructor.enclosingElement as TypeElement)
    val typeName = typeElement.asClassName()
    val generatedClassName = ClassName(
            packageName = typeName.packageName(),
            simpleName = "${typeName.simpleNames().joinToString("_")}$generatedFactoryPostfix")
    val scope: String?

    init {
        val isInnerClass = typeElement.enclosingElement is TypeElement
        if (isInnerClass && !typeElement.modifiers.contains(Modifier.STATIC)) {
            throw IllegalArgumentException("Can't inject a non-static inner class: $typeElement")
        }
        if (typeElement.modifiers.contains(Modifier.PRIVATE)) {
            throw IllegalArgumentException("Can't inject a private class: $typeElement")
        }
        if (typeElement.modifiers.contains(Modifier.ABSTRACT)) {
            throw IllegalArgumentException("Can't inject a abstract class: $typeElement")
        }

        val scopes = typeElement.annotationMirrors.map {
            it.annotationType.asElement() as TypeElement
        }.filter {
            it.getAnnotation(Scope::class.java) != null
        }

        if (scopes.size > 1) {
            throw IllegalArgumentException(
                    "Multiple @Scope qualified annotations found on $typeElement but only one is allowed. " +
                            "(${scopes.joinToString(", ") { it.qualifiedName.toString() }})")
        }
        val scopeAnnotation = scopes.firstOrNull()
        scope = if (scopeAnnotation != null) {
            val scopeAnnotationName = scopeAnnotation.qualifiedName.toString()
            val retention = scopeAnnotation.getAnnotation(Retention::class.java)

            if (retention?.value != RetentionPolicy.RUNTIME) {
                throw IllegalArgumentException("Scope annotation `$scopeAnnotationName` doesn't have RUNTIME retention.")
            }

            scopeAnnotationName
        } else {
            null
        }
    }

    fun generate(injectorModel: InjectorModel?, generatedAnnotationAvailable: Boolean): FileSpec {
        val params = constructor.parameters.map { generateGetInstanceCodeBlock(it) }.joinToString(", ")

        val createInstance = CodeBlock.of("%T(%L)", typeName, params)

        val body = if (injectorModel == null) {
            CodeBlock.of("return %L\n", createInstance)
        } else {
            CodeBlock.builder()
                    .add("val instance = %L\n", createInstance)
                    .add("`%T`().injectMembers(graph, instance)\n", injectorModel.generatedClassName)
                    .add("return instance\n")
                    .build()
        }

        return FileSpec.builder(generatedClassName.packageName(), generatedClassName.simpleName())
                .addType(
                        TypeSpec.classBuilder("`${generatedClassName.simpleName()}`")
                                .also {
                                    if (generatedAnnotationAvailable) {
                                        it.addAnnotation(generatedAnnotation())
                                    } else {
                                        it.addKdoc(generatedComment())
                                    }
                                }
                                .addSuperinterface(ParameterizedTypeName.get(factoryInterfaceName, typeName))
                                .addFunction(
                                        FunSpec.builder("createInstance")
                                                .addModifiers(KModifier.OVERRIDE)
                                                .addParameter("graph", graphClassName)
                                                .returns(typeName)
                                                .addCode(body)
                                                .build()
                                )
                                .build()
                )
                .build()
    }

}