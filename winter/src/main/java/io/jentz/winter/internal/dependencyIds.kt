package io.jentz.winter.internal

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

inline fun <reified T> providerId(qualifier: Any? = null) = ProviderId(T::class.java, qualifier)
inline fun <reified T> genericProviderId(qualifier: Any? = null) = object : GenericProviderId<T>(qualifier) {}
inline fun <reified A, reified R> factoryId(qualifier: Any? = null) = FactoryId(A::class.java, R::class.java, qualifier)
inline fun <reified A, reified R> genericFactoryId(qualifier: Any? = null) = object : GenericFactoryId<A, R>(qualifier) {}

sealed class DependencyId

class ProviderId(val cls: Class<*>, val qualifier: Any?) : DependencyId() {
    private var _hashCode = 0

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other is GenericProviderId<*>) return other.qualifier == qualifier && Types.equals(cls, other.type)
        if (other !is ProviderId) return false
        if (other.cls != cls) return false
        if (other.qualifier != qualifier) return false
        return true
    }

    override fun hashCode(): Int {
        if (_hashCode == 0) {
            _hashCode = Types.hashCode(cls, qualifier)
        }
        return _hashCode
    }

    override fun toString(): String = "ProviderId($cls qualifier = $qualifier)"
}

@Suppress("unused")
abstract class GenericProviderId<T>(val qualifier: Any?) : DependencyId() {
    private var _hashCode = 0
    val type: Type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other is ProviderId) return other.qualifier == qualifier && Types.equals(other.cls, type)
        if (other !is GenericProviderId<*>) return false
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

    override fun toString(): String = "GenericProviderId($type qualifier = $qualifier)"
}

class FactoryId(val argClass: Class<*>, val retClass: Class<*>, val qualifier: Any?) : DependencyId() {
    private var _hashCode = 0

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other is GenericFactoryId<*, *>) return other.qualifier == qualifier
                && Types.equals(other.argType, argClass) && Types.equals(other.retType, retClass)
        if (other !is FactoryId) return false
        if (other.qualifier != qualifier) return false
        if (other.argClass != argClass) return false
        return other.retClass == retClass
    }

    override fun hashCode(): Int {
        if (_hashCode == 0) {
            _hashCode = Types.hashCode(argClass, retClass, qualifier)
        }
        return _hashCode
    }

    override fun toString(): String = "FactoryId(($argClass) -> $retClass qualifier = $qualifier)"
}

@Suppress("unused")
abstract class GenericFactoryId<A, R>(val qualifier: Any?) : DependencyId() {
    private var _hashCode = 0
    val argType: Type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]
    val retType: Type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[1]

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other is FactoryId) return other.qualifier == qualifier
                && Types.equals(other.argClass, argType) && Types.equals(other.retClass, retType)
        if (other !is GenericFactoryId<*, *>) return false
        if (other.qualifier != qualifier) return false
        return Types.equals(argType, other.argType) && Types.equals(retType, other.retType)
    }

    override fun hashCode(): Int {
        if (_hashCode == 0) {
            _hashCode = Types.hashCode(argType)
            _hashCode = 31 * _hashCode + Types.hashCode(retType)
            _hashCode = 31 * _hashCode + (qualifier?.hashCode() ?: 0)
        }
        return _hashCode
    }

    override fun toString(): String = "GenericFactoryId(($argType) -> $retType qualifier = $qualifier)"
}