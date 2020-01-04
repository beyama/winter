package io.jentz.winter

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Interface for all type keys.
 */
interface TypeKey<out R : Any> {

    val qualifier: Any?

    /**
     * Test if [other] has the same type.
     * Like [equals] without looking onto the [qualifier].
     */
    fun typeEquals(other: TypeKey<*>): Boolean

}

class ClassTypeKey<R : Any> @JvmOverloads constructor(
    val type: Class<R>,
    override val qualifier: Any? = null
) : TypeKey<R> {

    private var _hashCode = 0

    override fun typeEquals(other: TypeKey<*>): Boolean {
        if (other === this) return true
        if (other is GenericClassTypeKey<*>) return Types.equals(type, other.type)
        if (other !is ClassTypeKey) return false
        return other.type == type
    }

    override fun equals(other: Any?): Boolean {
        return other is TypeKey<*> && other.qualifier == qualifier && typeEquals(other)
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
) : TypeKey<R> {

    private var _hashCode = 0
    val type: Type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]

    override fun typeEquals(other: TypeKey<*>): Boolean {
        if (other === this) return true
        if (other is ClassTypeKey) return Types.equals(other.type, type)
        if (other is GenericClassTypeKey<*>) return Types.equals(type, other.type)
        return false
    }

    override fun equals(other: Any?): Boolean {
        return other is TypeKey<*> && other.qualifier == qualifier && typeEquals(other)
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
