package io.jentz.winter.internal

/**
 * A map to store values by [DependencyKey].
 *
 * This was created to retrieve values by the components of the [keys][DependencyKey] without
 * building an instance of the key.
 */
internal class DependencyMap<T>(capacity: Int) {

    internal data class Entry<T>(var key: DependencyKey, var value: T, var next: Entry<T>? = null)

    /**
     * Create a instance with all entries from the given map.
     *
     * @param map The map to copy values from.
     */
    constructor(map: Map<DependencyKey, T>) : this(map.size) {
        map.forEach { (key, value) -> put(key, value) }
    }

    private val tableSize = Integer.highestOneBit(if (capacity <= 0) 2 else capacity) shl 1
    private val tableMask = tableSize - 1
    private val table = arrayOfNulls<Entry<T>?>(tableSize)

    var size: Int = 0
        private set

    /**
     * Associates the specified value with the specified key in the map.
     *
     * @return Return the previous value associated with the key, or null if the key was not present in the map.
     */
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

    /**
     * An alias for [put].
     */
    operator fun set(key: DependencyKey, value: T) {
        put(key, value)
    }

    /**
     * Returns the value corresponding to the given [key], or null if such a key is not present in the map.
     */
    operator fun get(key: DependencyKey): T? = getEntry(key.hashCode()) { it == key }?.value

    /**
     * Returns the value corresponding to a [TypeKey] that was created from the given class and qualifier,
     * or null if such a key is not present in the map.
     */
    fun get(cls: Class<*>, qualifier: Any? = null): T? = getEntry(cls, qualifier)?.value

    /**
     * Returns the value corresponding to a [CompoundTypeKey] that was created from the given classes and qualifier,
     * or null if such a key is not present in the map.
     */
    fun get(firstClass: Class<*>, secondClass: Class<*>, qualifier: Any? = null): T? =
            getEntry(firstClass, secondClass, qualifier)?.value

    /**
     * Returns the entry corresponding to the given [key], or null if such a key is not present in the map.
     */
    fun getEntry(key: DependencyKey): Entry<T>? = getEntry(key.hashCode()) { it == key }

    /**
     * Returns the entry corresponding to a [TypeKey] that was created from the given class and qualifier,
     * or null if such a key is not present in the map.
     */
    fun getEntry(cls: Class<*>, qualifier: Any?): Entry<T>? =
            getEntry(Types.hashCode(cls, qualifier)) {
                when (it) {
                    is TypeKey -> it.qualifier == qualifier && it.type == cls
                    is GenericTypeKey<*> -> it.qualifier == qualifier && Types.equals(it.type, cls)
                    else -> false
                }
            }

    /**
     * Returns the entry corresponding to a [CompoundTypeKey] that was created from the given classes and qualifier,
     * or null if such a key is not present in the map.
     */
    fun getEntry(firstClass: Class<*>, secondClass: Class<*>, qualifier: Any? = null): Entry<T>? =
            getEntry(Types.hashCode(firstClass, secondClass, qualifier)) {
                when (it) {
                    is CompoundTypeKey -> it.qualifier == qualifier && it.firstType == firstClass && it.secondType == secondClass
                    is GenericCompoundTypeKey<*, *> -> it.qualifier == qualifier && Types.equals(it.firstType, firstClass) && Types.equals(it.secondType, secondClass)
                    else -> false
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

    /**
     * Performs the given action on each key/value pair.
     */
    fun forEach(action: (DependencyKey, T) -> Unit) {
        table.forEach { entry ->
            entry?.let {
                action(it.key, it.value)

                var e = it.next
                while (e != null) {
                    action(e.key, e.value)
                    e = e.next
                }
            }
        }
    }

    /**
     * Returns true if the map contains the specified [key].
     */
    fun containsKey(key: DependencyKey) = getEntry(key) != null

    /**
     * Returns true if the map is empty (contains no entries), false otherwise.
     */
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