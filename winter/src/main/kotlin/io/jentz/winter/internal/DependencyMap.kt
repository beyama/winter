package io.jentz.winter.internal

import kotlin.reflect.KClass

internal class DependencyMap<T>(capacity: Int) {

    internal data class Entry<T>(var key: DependencyKey, var value: T, var next: Entry<T>? = null)

    constructor(map: Map<DependencyKey, T>) : this(map.size) {
        map.forEach { (key, value) -> put(key, value) }
    }

    private val tableSize = Integer.highestOneBit(if (capacity <= 0) 2 else capacity) shl 1
    private val tableMask = tableSize - 1
    private val table = arrayOfNulls<Entry<T>?>(tableSize)

    var size: Int = 0
        private set

    fun put(key: DependencyKey, value: T): T? {
        val index = key.hashCode() and tableMask
        val entry = table[index]

        if (entry == null) {
            table[index] = Entry(key, value)
            size += 1
        } else {
            var e = entry
            while (e != null) {
                if (e.key == key) {
                    val oldValue = e.value
                    e.key = key
                    e.value = value
                    return oldValue
                }

                if (e.next == null) {
                    e.next = Entry(key, value)
                    size += 1
                } else {
                    e = e.next
                }
            }
        }
        return null
    }

    fun getEntry(key: DependencyKey) = getEntry(key.hashCode()) { it == key }

    fun getEntry(kClass: KClass<*>, qualifier: Any?): Entry<T>? {
        val javaClass = kClass.javaObjectType

        return getEntry(Types.hashCode(javaClass, qualifier)) {
            when (it) {
                is TypeKey -> it.qualifier == qualifier && it.type == javaClass
                is GenericTypeKey<*> -> it.qualifier == qualifier && Types.equals(it.type, javaClass)
                else -> false
            }
        }
    }

    fun getEntry(argClass: KClass<*>, retClass: KClass<*>, qualifier: Any? = null): Entry<T>? {
        val argJavaClass = argClass.javaObjectType
        val retJavaClass = retClass.javaObjectType

        return getEntry(Types.hashCode(argJavaClass, retJavaClass, qualifier)) {
            when (it) {
                is CompoundTypeKey -> it.qualifier == qualifier && it.firstType == argJavaClass && it.secondType == retJavaClass
                is GenericCompoundTypeKey<*, *> -> it.qualifier == qualifier && Types.equals(it.firstType, argJavaClass) && Types.equals(it.secondType, retJavaClass)
                else -> false
            }
        }
    }

    private inline fun getEntry(hash: Int, equals: (DependencyKey) -> Boolean): Entry<T>? {
        val index = hash and tableMask
        val entry = table[index] ?: return null
        if (equals(entry.key)) {
            return entry
        }
        var e = entry.next
        while (e != null) {
            if (equals(e.key)) {
                return e
            }
            e = e.next
        }
        return null
    }

    operator fun get(key: DependencyKey): T? = get(key.hashCode()) { it == key }

    fun get(kClass: KClass<*>, qualifier: Any? = null) = getEntry(kClass, qualifier)?.value

    fun get(argClass: KClass<*>, retClass: KClass<*>, qualifier: Any? = null): T? = getEntry(argClass, retClass, qualifier)?.value

    private inline fun get(hash: Int, equals: (DependencyKey) -> Boolean): T? = getEntry(hash, equals)?.value

    operator fun set(key: DependencyKey, value: T) {
        put(key, value)
    }

    fun forEach(action: (DependencyKey, T) -> Unit) {
        table.forEach { entry ->
            entry?.let {
                action(entry.key, entry.value)

                var e = entry.next
                while (e != null) {
                    action(e.key, e.value)
                    e = e.next
                }
            }
        }
    }

    fun containsKey(key: DependencyKey) = getEntry(key) != null

    fun isEmpty() = size == 0

    override fun hashCode(): Int {
        var h = 0
        forEach { _, v -> h += (v?.hashCode() ?: 0) }
        return h
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is DependencyMap<*>) return false
        if (other.size != size) return false

        var result = true
        forEach { key, value ->
            if (other[key] != value) {
                result = false
                return@forEach
            }
        }
        return result
    }

}