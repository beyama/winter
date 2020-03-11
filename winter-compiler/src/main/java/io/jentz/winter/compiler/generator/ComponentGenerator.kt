package io.jentz.winter.compiler.generator

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import io.jentz.winter.Component
import io.jentz.winter.compiler.*
import io.jentz.winter.compiler.model.FactoryModel
import javax.lang.model.element.Modifier.*

class ComponentGenerator(
    private val configuration: ProcessorConfiguration,
    private val factories: List<FactoryModel>
) {

    private val packageName = checkNotNull(configuration.generatedComponentPackage) {
        "BUG: ComponentGenerator instantiated but package is null."
    }

    private val generatedClassName = ClassName.get(packageName, "GeneratedComponent")

    fun generate(): JavaFile {
        val componentClassName = Component::class.java.toClassName()
        val commonKt = componentClassName.peerClass("CommonKt")

        val generatedComponent = CodeBlock.builder()
            .add("generatedComponent = \$T.component (\$S, builder -> {\n", commonKt, "generated")
            .indent()
            .apply {
                val groupedFactories = factories.groupBy {
                    it.scopeAnnotationName ?: APPLICATION_SCOPE_CLASS_NAME
                }

                groupedFactories.forEach { (scopeName, factories) ->
                    add("builder.subcomponent(\$L, false, false, subBuilder -> {\n", scopeName.toGetKotlinKClassCodeBlock())

                    indent()

                    factories.forEach { factory ->
                        addStatement("new \$T().register(subBuilder, false)", factory.generatedClassName)
                    }

                    addStatement("return \$T.INSTANCE", Unit.javaClass.toClassName())
                    unindent()
                    add("});\n")
                }
            }
            .addStatement("return \$T.INSTANCE", Unit.javaClass.toClassName())
            .unindent()
            .add("});\n")
            .build()

        val componentClass = TypeSpec
            .classBuilder(generatedClassName)
            .addModifiers(PUBLIC, FINAL)
            .generatedAnnotation(configuration.generatedAnnotation)
            .addField(componentClassName, "generatedComponent", PUBLIC, FINAL, STATIC)
            .addStaticBlock(generatedComponent)
            .build()

        return JavaFile
            .builder(generatedClassName.packageName(), componentClass)
            .build()

    }

}
