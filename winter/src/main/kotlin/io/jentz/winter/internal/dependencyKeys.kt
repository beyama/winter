package io.jentz.winter.internal

import io.jentz.winter.DependencyKey
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class TypeKey(val type: Class<*>, val qualifier: Any?) : DependencyKey {
    private var _hashCode = 0

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other is GenericTypeKey<*>) return other.qualifier == qualifier && Types.equals(type, other.type)
        if (other !is TypeKey) return false
        if (other.type != type) return false
        if (other.qualifier != qualifier) return false
        return true
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
abstract class GenericTypeKey<T>(val qualifier: Any?) : DependencyKey {
    private var _hashCode = 0
    val type: Type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other is TypeKey) return other.qualifier == qualifier && Types.equals(other.type, type)
        if (other !is GenericTypeKey<*>) return false
        if (other.qualifier != qualifier) return false
        return Types.equals(type, other.type)
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

class CompoundTypeKey(val firstType: Class<*>, val secondType: Class<*>, val qualifier: Any?) : DependencyKey {
    private var _hashCode = 0

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other is GenericCompoundTypeKey<*, *>) return other.qualifier == qualifier
                && Types.equals(other.firstType, firstType) && Types.equals(other.secondType, secondType)
        if (other !is CompoundTypeKey) return false
        if (other.qualifier != qualifier) return false
        if (other.firstType != firstType) return false
        return other.secondType == secondType
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
abstract class GenericCompoundTypeKey<T0, T1>(val qualifier: Any?) : DependencyKey {
    private var _hashCode = 0
    val firstType: Type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]
    val secondType: Type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[1]

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other is CompoundTypeKey) return other.qualifier == qualifier
                && Types.equals(other.firstType, firstType) && Types.equals(other.secondType, secondType)
        if (other !is GenericCompoundTypeKey<*, *>) return false
        if (other.qualifier != qualifier) return false
        return Types.equals(firstType, other.firstType) && Types.equals(secondType, other.secondType)
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