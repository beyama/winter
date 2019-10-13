package io.jentz.winter

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Interface for all type keys.
 */
interface TypeKey<in A, out R : Any> {

    val qualifier: Any?

    /**
     * Test if [other] has the same type.
     * Like [equals] without looking onto the [qualifier].
     */
    fun typeEquals(other: TypeKey<*, *>): Boolean

}

class ClassTypeKey<R : Any> @JvmOverloads constructor(
    val type: Class<R>,
    override val qualifier: Any? = null
) : TypeKey<Unit, R> {

    private var _hashCode = 0

    override fun typeEquals(other: TypeKey<*, *>): Boolean {
        if (other === this) return true
        if (other is GenericClassTypeKey<*>) return Types.equals(type, other.type)
        if (other !is ClassTypeKey) return false
        return other.type == type
    }

    override fun equals(other: Any?): Boolean {
        return other is TypeKey<*, *> && other.qualifier == qualifier && typeEquals(other)
    }

    override fun hashCode(): Int {
        if (_hashCode == 0) {
            _hashCode = Types.hashCode(type, qualifier)
        }
        return _hashCode
    }

    override fun toString(): String = "ClassTypeKey($type qualifier = $qualifier)"

}

abstract class GenericClassTypeKey<R : Any> @JvmOverloads constructor(
    override val qualifier: Any? = null
) : TypeKey<Unit, R> {

    private var _hashCode = 0
    val type: Type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]

    override fun typeEquals(other: TypeKey<*, *>): Boolean {
        if (other === this) return true
        if (other is ClassTypeKey) return Types.equals(other.type, type)
        if (other is GenericClassTypeKey<*>) return Types.equals(type, other.type)
        return false
    }

    override fun equals(other: Any?): Boolean {
        return other is TypeKey<*, *> && other.qualifier == qualifier && typeEquals(other)
    }

    override fun hashCode(): Int {
        if (_hashCode == 0) {
            _hashCode = Types.hashCode(type)
            _hashCode = 31 * _hashCode + (qualifier?.hashCode() ?: 0)
        }
        return _hashCode
    }

    override fun toString(): String = "GenericClassTypeKey($type qualifier = $qualifier)"

}

class CompoundClassTypeKey<A, R : Any> @JvmOverloads constructor(
    val firstType: Class<A>,
    val secondType: Class<R>,
    override val qualifier: Any? = null
) : TypeKey<A, R> {

    private var _hashCode = 0

    override fun typeEquals(other: TypeKey<*, *>): Boolean {
        if (other === this) return true
        if (other is GenericCompoundClassTypeKey<*, *>) {
            return Types.equals(other.argumentType, firstType)
                    && Types.equals(other.returnType, secondType)
        }
        if (other !is CompoundClassTypeKey) return false
        return other.firstType == firstType && other.secondType == secondType
    }

    override fun equals(other: Any?): Boolean {
        return other is TypeKey<*, *> && other.qualifier == qualifier && typeEquals(other)
    }

    override fun hashCode(): Int {
        if (_hashCode == 0) {
            _hashCode = Types.hashCode(firstType, secondType, qualifier)
        }
        return _hashCode
    }

    override fun toString(): String =
        "CompoundClassTypeKey($firstType $secondType qualifier = $qualifier)"

}

abstract class GenericCompoundClassTypeKey<A, R : Any> @JvmOverloads constructor(
    override val qualifier: Any? = null
) : TypeKey<A, R> {

    private var _hashCode = 0

    val argumentType: Type = (javaClass.genericSuperclass as ParameterizedType)
        .actualTypeArguments[0]

    val returnType: Type = (javaClass.genericSuperclass as ParameterizedType)
        .actualTypeArguments[1]

    override fun typeEquals(other: TypeKey<*, *>): Boolean {
        if (other === this) return true
        if (other is CompoundClassTypeKey) {
            return Types.equals(other.firstType, argumentType)
                    && Types.equals(other.secondType, returnType)
        }
        if (other !is GenericCompoundClassTypeKey<*, *>) return false
        return Types.equals(argumentType, other.argumentType)
                && Types.equals(returnType, other.returnType)
    }

    override fun equals(other: Any?): Boolean {
        return other is TypeKey<*, *> && other.qualifier == qualifier && typeEquals(other)
    }

    override fun hashCode(): Int {
        if (_hashCode == 0) {
            _hashCode = Types.hashCode(argumentType)
            _hashCode = 31 * _hashCode + Types.hashCode(returnType)
            _hashCode = 31 * _hashCode + (qualifier?.hashCode() ?: 0)
        }
        return _hashCode
    }

    override fun toString(): String =
        "GenericCompoundClassTypeKey($argumentType $returnType qualifier = $qualifier)"

}
