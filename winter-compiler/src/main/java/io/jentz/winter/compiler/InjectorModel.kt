package io.jentz.winter.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.asClassName
import io.jentz.winter.compiler.kotlinbuilder.KotlinFile
import io.jentz.winter.compiler.kotlinbuilder.buildKotlinFile
import io.jentz.winter.compiler.kotlinbuilder.generatedAnnotation
import javax.lang.model.element.TypeElement

class InjectorModel(
        private val configuration: ProcessorConfiguration,
        typeElement: TypeElement
) {

    val typeName = typeElement.asClassName()

    val generatedClassName = ClassName(
            typeName.packageName,
            "WinterMembersInjector_${typeName.simpleNames.joinToString("_")}"
    )

    val targets: MutableSet<InjectTargetModel> = mutableSetOf()

}
