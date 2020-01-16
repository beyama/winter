package io.jentz.winter.compiler

import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import kotlin.reflect.jvm.internal.impl.metadata.ProtoBuf
import kotlin.reflect.jvm.internal.impl.metadata.ProtoBuf.Visibility.INTERNAL
import kotlin.reflect.jvm.internal.impl.metadata.ProtoBuf.Visibility.PUBLIC
import kotlin.reflect.jvm.internal.impl.metadata.deserialization.Flags
import kotlin.reflect.jvm.internal.impl.metadata.jvm.deserialization.JvmNameResolver
import kotlin.reflect.jvm.internal.impl.metadata.jvm.deserialization.JvmProtoBufUtil

class KotlinMetadata(
    private val nameResolver: JvmNameResolver,
    private val clazz: ProtoBuf.Class
) {

    companion object {

        fun fromTypeElement(typeElement: TypeElement): KotlinMetadata? {
            val annotation = typeElement.getAnnotation(Metadata::class.java) ?: return null
            val (nameResolver, clazz) = JvmProtoBufUtil.readClassDataFrom(annotation.data1, annotation.data2)
            return KotlinMetadata(nameResolver, clazz)
        }

    }

    fun getKotlinPropertyForField(field: VariableElement): Property? =
        findPropertyByName(field.simpleName.toString())?.let {
            Property(nameResolver, it)
        }

    fun getKotlinPropertyForSetter(setter: ExecutableElement): Property? {
        val name = setter.simpleName.toString()
        if (!name.startsWith("set") || name.length <= 3 || setter.parameters.size != 1) {
            return null
        }

        val propertyName = name.removePrefix("set").decapitalize()
        val property = findPropertyByName(propertyName) ?: return null
        return Property(nameResolver, property)
    }

    private fun findPropertyByName(name: String): ProtoBuf.Property? = clazz.propertyList.find {
        nameResolver.getString(it.name) == name
    }

    class Property(
        nameResolver: JvmNameResolver,
        private val property: ProtoBuf.Property
    ) {

        val name: String = nameResolver.getString(property.name)

        val isNullable: Boolean = property.returnType.nullable

        val hasAccessibleSetter: Boolean
            get() {
                if (!Flags.HAS_SETTER.get(property.flags)) return false

                val propertyVisibility = Flags.VISIBILITY.get(property.flags)

                if (!property.hasSetterFlags()
                    && (propertyVisibility == PUBLIC
                            || propertyVisibility == INTERNAL)) {
                    return true
                }

                val setterVisibility = Flags.VISIBILITY.get(property.setterFlags)

                return setterVisibility == PUBLIC || setterVisibility == INTERNAL
            }

    }

}
