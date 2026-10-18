package io.jentz.winter

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

/**
 * Structural equality and hash codes of [KType]s for [TypeKey]s.
 *
 * Nullability is ignored and a type without arguments is equal to its class, so that
 * [generic] and [erased] keys of the same class are equal.
 */
@Suppress("MagicNumber")
internal object Types {

    fun equals(left: KType, right: KType): Boolean {
        if (left === right) return true
        if (left.classifier != right.classifier) return false
        val leftArguments = left.arguments
        val rightArguments = right.arguments
        if (leftArguments.size != rightArguments.size) return false
        return leftArguments.indices.all { equals(leftArguments[it], rightArguments[it]) }
    }

    fun equals(left: KClass<*>, right: KType): Boolean =
        right.arguments.isEmpty() && right.classifier == left

    private fun equals(left: KTypeProjection, right: KTypeProjection): Boolean {
        if (left.variance != right.variance) return false
        val leftType = left.type
        val rightType = right.type
        if (leftType == null || rightType == null) return leftType == null && rightType == null
        return equals(leftType, rightType)
    }

    private fun hashCode(type: KType): Int {
        var hashCode = type.classifier.hashCode()
        for (argument in type.arguments) {
            hashCode = hashCode * 31 + hashCode(argument)
        }
        return hashCode
    }

    private fun hashCode(projection: KTypeProjection): Int {
        val type = projection.type ?: return 0
        return 17 * (projection.variance?.ordinal ?: 0) + hashCode(type)
    }

    fun hashCode(type: KType, qualifier: Any?): Int =
        31 * hashCode(type) + (qualifier?.hashCode() ?: 0)

    fun hashCode(cls: KClass<*>, qualifier: Any?): Int =
        31 * cls.hashCode() + (qualifier?.hashCode() ?: 0)

}
