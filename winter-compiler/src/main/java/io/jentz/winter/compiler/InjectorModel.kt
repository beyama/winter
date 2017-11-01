package io.jentz.winter.compiler

import com.squareup.kotlinpoet.*
import javax.lang.model.element.QualifiedNameable
import javax.lang.model.element.TypeElement

class InjectorModel(val typeElement: TypeElement) {
    val targets: MutableSet<InjectTargetModel> = mutableSetOf()

    fun generate(): FileSpec {
        val packageName = (typeElement.enclosingElement as QualifiedNameable).qualifiedName.toString()
        val graphClass = ClassName("io.jentz.winter", "Graph")
        val injectorInterface = ClassName("io.jentz.winter", "MembersInjector")
        val targetClass = ClassName(packageName, typeElement.simpleName.toString())
        val className = "${typeElement.simpleName}$\$MembersInjector"

        return FileSpec.builder(packageName, className)
                .addStaticImport(graphClass.packageName(), graphClass.simpleName())
                .addType(
                        TypeSpec
                                .classBuilder("`$className`")
                                .addSuperinterface(ParameterizedTypeName.get(injectorInterface, targetClass))
                                .addFunction(
                                        FunSpec.builder("injectMembers")
                                                .addModifiers(KModifier.OVERRIDE)
                                                .addParameter("graph", graphClass)
                                                .addParameter("target", targetClass)
                                                .also {
                                                    targets.forEach { target ->
                                                        it.addCode(target.codeBlock())
                                                    }
                                                }
                                                .build()
                                )
                                .build()
                ).build()
    }

}