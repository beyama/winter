package io.jentz.winter.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import javax.inject.Named
import javax.lang.model.element.VariableElement

val optionGeneratedComponentPackage = "winterGeneratedComponentPackage"

val graphClassName = ClassName("io.jentz.winter", "Graph")
val componentClassName = ClassName("io.jentz.winter", "Component")
val factoryInterfaceName = ClassName("io.jentz.winter.internal", "Factory")
val injectorInterfaceName = ClassName("io.jentz.winter.internal", "MembersInjector")

val generatedFactoryPostfix = "\$\$Factory"
val generatedInjectorPostfix = "\$\$MembersInjector"

fun generateGetInstanceCodeBlock(e: VariableElement): CodeBlock {
    val qualifier = e.getAnnotation(Named::class.java)?.value
    return if (qualifier != null) {
        CodeBlock.of("graph.instance(%S)", qualifier)
    } else {
        CodeBlock.of("graph.instance()")
    }
}