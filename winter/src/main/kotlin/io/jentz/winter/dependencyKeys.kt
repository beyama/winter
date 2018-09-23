package io.jentz.winter

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class TypeKey(val type: Class<*>, override val qualifier: Any?) : DependencyKey {
    private var _hashCode = 0

    override fun typeEquals(other: DependencyKey): Boolean {
        if (other === this) return true
        if (other is GenericTypeKey<*>) return Types.equals(type, other.type)
        if (other !is TypeKey) return false
        return other.type == type
    }

    override fun equals(other: Any?): Boolean {
        return other is DependencyKey && other.qualifier == qualifier && typeEquals(other)
    }

    override fun hashCode(): Int {
        if (_hashCode == 0) {
            _hashCode = Types.hashCode(type, qualifier)
        }
        return _hashCode
    }

    override fun toString(): String = "TypeKey($type qualifier = $qualifier)"
}

@Suppress("unused")
abstract class GenericTypeKey<T>(override val qualifier: Any?) : DependencyKey {
    private var _hashCode = 0
    val type: Type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]

    override fun typeEquals(other: DependencyKey): Boolean {
        if (other === this) return true
        if (other is TypeKey) return Types.equals(other.type, type)
        if (other is GenericTypeKey<*>) return Types.equals(type, other.type)
        return false
    }

    override fun equals(other: Any?): Boolean {
        return other is DependencyKey && other.qualifier == qualifier && typeEquals(other)
    }

    override fun hashCode(): Int {
        if (_hashCode == 0) {
            _hashCode = Types.hashCode(type)
            _hashCode = 31 * _hashCode + (qualifier?.hashCode() ?: 0)
        }
        return _hashCode
    }

    override fun toString(): String = "GenericTypeKey($type qualifier = $qualifier)"
}

class CompoundTypeKey(val firstType: Class<*>, val secondType: Class<*>, override val qualifier: Any?) : DependencyKey {
    private var _hashCode = 0

    override fun typeEquals(other: DependencyKey): Boolean {
        if (other === this) return true
        if (other is GenericCompoundTypeKey<*, *>)
            return Types.equals(other.firstType, firstType) && Types.equals(other.secondType, secondType)
        if (other !is CompoundTypeKey) return false
        return other.firstType == firstType && other.secondType == secondType
    }

    override fun equals(other: Any?): Boolean {
        return other is DependencyKey && other.qualifier == qualifier && typeEquals(other)
    }

    override fun hashCode(): Int {
        if (_hashCode == 0) {
            _hashCode = Types.hashCode(firstType, secondType, qualifier)
        }
        return _hashCode
    }

    override fun toString(): String = "CompoundTypeKey($firstType $secondType qualifier = $qualifier)"
}

@Suppress("unused")
abstract class GenericCompoundTypeKey<T0, T1>(override val qualifier: Any?) : DependencyKey {
    private var _hashCode = 0
    val firstType: Type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]
    val secondType: Type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[1]

    override fun typeEquals(other: DependencyKey): Boolean {
        if (other === this) return true
        if (other is CompoundTypeKey)
            return Types.equals(other.firstType, firstType) && Types.equals(other.secondType, secondType)
        if (other !is GenericCompoundTypeKey<*, *>) return false
        return Types.equals(firstType, other.firstType) && Types.equals(secondType, other.secondType)
    }

    override fun equals(other: Any?): Boolean {
        return other is DependencyKey && other.qualifier == qualifier && typeEquals(other)
    }

    override fun hashCode(): Int {
        if (_hashCode == 0) {
            _hashCode = Types.hashCode(firstType)
            _hashCode = 31 * _hashCode + Types.hashCode(secondType)
            _hashCode = 31 * _hashCode + (qualifier?.hashCode() ?: 0)
        }
        return _hashCode
    }

    override fun toString(): String = "GenericCompoundTypeKey($firstType $secondType qualifier = $qualifier)"
}