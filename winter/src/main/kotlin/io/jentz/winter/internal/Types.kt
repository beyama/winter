package io.jentz.winter.internal

import java.lang.reflect.*

internal object Types {

    fun equals(left: Type, right: Type): Boolean {
        if (left.javaClass != right.javaClass) return false

        return when (left) {
            is Class<*> -> left == right
            is ParameterizedType -> {
                right as ParameterizedType
                equals(left.rawType, right.rawType) && equals(left.actualTypeArguments, right.actualTypeArguments)
            }
            is WildcardType -> {
                right as WildcardType
                equals(left.lowerBounds, right.lowerBounds) && equals(left.upperBounds, right.upperBounds)
            }
            is GenericArrayType -> {
                right as GenericArrayType
                equals(left.genericComponentType, right.genericComponentType)
            }
            is TypeVariable<*> -> {
                right as TypeVariable<*>
                equals(left.bounds, right.bounds)
            }
            else -> left == right
        }
    }

    private fun equals(left: Array<Type>, right: Array<Type>): Boolean {
        if (left.size != right.size) return false
        return left.indices.all { equals(left[it], right[it]) }
    }

    fun hashCode(type: Type): Int = when (type) {
        is Class<*> -> type.hashCode()
        is ParameterizedType -> {
            var hashCode = hashCode(type.rawType)
            for (arg in type.actualTypeArguments)
                hashCode = hashCode * 31 + hashCode(arg)
            hashCode
        }
        is WildcardType -> {
            var hashCode = 0
            for (arg in type.upperBounds)
                hashCode = hashCode * 19 + hashCode(arg)
            for (arg in type.lowerBounds)
                hashCode = hashCode * 17 + hashCode(arg)
            hashCode
        }
        is GenericArrayType -> 53 + hashCode(type.genericComponentType)
        is TypeVariable<*> -> {
            var hashCode = 0
            for (arg in type.bounds)
                hashCode = hashCode * 29 + hashCode(arg)
            hashCode
        }
        else -> type.hashCode()
    }

    fun hashCode(cls: Class<*>, qualifier: Any? = null): Int = 31 * cls.hashCode() + (qualifier?.hashCode() ?: 0)

    fun hashCode(class0: Class<*>, class1: Class<*>, qualifier: Any?): Int {
        var hashCode = class0.hashCode()
        hashCode = 31 * hashCode + class1.hashCode()
        return 31 * hashCode + (qualifier?.hashCode() ?: 0)
    }

}