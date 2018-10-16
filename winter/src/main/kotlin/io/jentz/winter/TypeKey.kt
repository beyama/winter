package io.jentz.winter

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Interface for all type keys.
 */
interface TypeKey {

    val qualifier: Any?

    /**
     * Test if [other] has the same type.
     * Like [equals] without looking onto the [qualifier].
     */
    fun typeEquals(other: TypeKey): Boolean
}

class ClassTypeKey(val type: Class<*>, override val qualifier: Any?) : TypeKey {
    private var _hashCode = 0

    override fun typeEquals(other: TypeKey): Boolean {
        if (other === this) return true
        if (other is GenericClassTypeKey<*>) return Types.equals(type, other.type)
        if (other !is ClassTypeKey) return false
        return other.type == type
    }

    override fun equals(other: Any?): Boolean {
        return other is TypeKey && other.qualifier == qualifier && typeEquals(other)
    }

    override fun hashCode(): Int {
        if (_hashCode == 0) {
            _hashCode = Types.hashCode(type, qualifier)
        }
        return _hashCode
    }

    override fun toString(): String = "ClassTypeKey($type qualifier = $qualifier)"
}

@Suppress("unused")
abstract class GenericClassTypeKey<T>(override val qualifier: Any?) : TypeKey {
    private var _hashCode = 0
    val type: Type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]

    override fun typeEquals(other: TypeKey): Boolean {
        if (other === this) return true
        if (other is ClassTypeKey) return Types.equals(other.type, type)
        if (other is GenericClassTypeKey<*>) return Types.equals(type, other.type)
        return false
    }

    override fun equals(other: Any?): Boolean {
        return other is TypeKey && other.qualifier == qualifier && typeEquals(other)
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

class CompoundClassTypeKey(
    val firstType: Class<*>,
    val secondType: Class<*>,
    override val qualifier: Any?
) : TypeKey {
    private var _hashCode = 0

    override fun typeEquals(other: TypeKey): Boolean {
        if (other === this) return true
        if (other is GenericCompoundClassTypeKey<*, *>) {
            return Types.equals(other.firstType, firstType)
                    && Types.equals(other.secondType, secondType)
        }
        if (other !is CompoundClassTypeKey) return false
        return other.firstType == firstType && other.secondType == secondType
    }

    override fun equals(other: Any?): Boolean {
        return other is TypeKey && other.qualifier == qualifier && typeEquals(other)
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

@Suppress("unused")
abstract class GenericCompoundClassTypeKey<T0, T1>(override val qualifier: Any?) : TypeKey {
    private var _hashCode = 0
    val firstType: Type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]
    val secondType: Type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[1]

    override fun typeEquals(other: TypeKey): Boolean {
        if (other === this) return true
        if (other is CompoundClassTypeKey) {
            return Types.equals(other.firstType, firstType)
                    && Types.equals(other.secondType, secondType)
        }
        if (other !is GenericCompoundClassTypeKey<*, *>) return false
        return Types.equals(firstType, other.firstType)
                && Types.equals(secondType, other.secondType)
    }

    override fun equals(other: Any?): Boolean {
        return other is TypeKey && other.qualifier == qualifier && typeEquals(other)
    }

    override fun hashCode(): Int {
        if (_hashCode == 0) {
            _hashCode = Types.hashCode(firstType)
            _hashCode = 31 * _hashCode + Types.hashCode(secondType)
            _hashCode = 31 * _hashCode + (qualifier?.hashCode() ?: 0)
        }
        return _hashCode
    }

    override fun toString(): String =
        "GenericCompoundClassTypeKey($firstType $secondType qualifier = $qualifier)"
}
