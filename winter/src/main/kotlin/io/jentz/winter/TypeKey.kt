package io.jentz.winter

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass

/**
 * Interface for all type keys.
 */
interface TypeKey<out R> {

    val isOptional: Boolean

    val qualifier: Qualifier?

    /**
     * Test if [other] has the same type.
     * Like [equals] without looking onto the [qualifier].
     */
    fun typeEquals(other: TypeKey<*>): Boolean

}

@Suppress("UNCHECKED_CAST")
inline fun <reified R: Any?> Set<TypeKey<*>>.ofType(key: TypeKey<R> = erased()): Set<TypeKey<R>> =
    filterTo(mutableSetOf()) { it.typeEquals(key) } as Set<TypeKey<R>>

/**
 * Returns [TypeKey] for type [R] with erased generics.
 *
 * @param qualifier An optional qualifier for this key.
 */
inline fun <reified R : Any?> erased(
    qualifier: Qualifier? = null
): TypeKey<R> = ClassTypeKey(R::class.java, null is R, qualifier)

/**
 * Returns [TypeKey] for type [R] with generics information preserved.
 *
 * @param qualifier An optional qualifier for this key.
 */
inline fun <reified R: Any?> generic(
    qualifier: Qualifier? = null
): TypeKey<R> = object : GenericClassTypeKey<R>(null is R, qualifier) {}

inline fun <reified T: Any> KClass<T>.typeKey(qualifier: Qualifier? = null) =
    ClassTypeKey(java, false, qualifier)


class ClassTypeKey<R>(
    val type: Class<R>,
    override val isOptional: Boolean = false,
    override val qualifier: Qualifier? = null
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

    override fun toString(): String =
        "TypeKey(${type.name}, qualifier = $qualifier, isOptional = $isOptional)"

}

abstract class GenericClassTypeKey<R>(
    override val isOptional: Boolean = false,
    override val qualifier: Qualifier? = null
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

    override fun toString(): String = "GenericClassTypeKey($type, $qualifier)"

}
