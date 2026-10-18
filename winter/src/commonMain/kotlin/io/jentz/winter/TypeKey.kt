package io.jentz.winter

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

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
): TypeKey<R> = ClassTypeKey(R::class, null is R, qualifier)

/**
 * Returns [TypeKey] for type [R] with generics information preserved.
 *
 * A generic key of a type without type arguments is equal to the [erased] key of the same class.
 * Nullability is ignored, `generic<List<String?>?>()` is equal to `generic<List<String>>()`.
 *
 * @param qualifier An optional qualifier for this key.
 */
inline fun <reified R: Any?> generic(
    qualifier: Qualifier? = null
): TypeKey<R> = GenericClassTypeKey(typeOf<R>(), null is R, qualifier)

inline fun <reified T: Any> KClass<T>.typeKey(qualifier: Qualifier? = null): ClassTypeKey<T> =
    ClassTypeKey(this, false, qualifier)

/**
 * [TypeKey] of a class, see [erased].
 */
class ClassTypeKey<R>(
    val type: KClass<*>,
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
        "TypeKey(${type.displayName}, qualifier = $qualifier, isOptional = $isOptional)"

}

/**
 * [TypeKey] of a type with type arguments, see [generic].
 */
class GenericClassTypeKey<R>(
    val type: KType,
    override val isOptional: Boolean = false,
    override val qualifier: Qualifier? = null
) : TypeKey<R> {

    private var _hashCode = 0

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
            _hashCode = Types.hashCode(type, qualifier)
        }
        return _hashCode
    }

    override fun toString(): String = "GenericClassTypeKey($type, $qualifier)"

}
