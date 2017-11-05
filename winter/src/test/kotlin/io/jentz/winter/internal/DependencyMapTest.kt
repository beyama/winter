package io.jentz.winter.internal

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DependencyMapTest {
    val value1 = Any()
    val value2 = Any()

    class FixedHash(private val hashCode: Int) {
        override fun hashCode() = hashCode
    }

    private lateinit var map: DependencyMap<Any?>

    @Before
    fun before() {
        map = DependencyMap(16)
    }

    @Test
    fun `empty dependency map initialized with a capacity of 0 should return null on #get`() {
        map = DependencyMap(0)
        assertNull(map.get(String::class.java))
    }

    @Test
    fun `#get with class and qualifier should return value associated with corresponding type key`() {
        map.put(typeKey<String>(), value1)
        map.put(typeKey<String>("a"), value2)

        assertSame(value1, map.get(String::class.java))
        assertSame(value2, map.get(String::class.java, "a"))
    }

    @Test
    fun `#get with classes and qualifier should return value associated with corresponding compound type key`() {
        map.put(compoundTypeKey<List<*>, String>(), value1)
        map.put(compoundTypeKey<List<*>, String>("a"), value2)

        assertSame(value1, map.get(List::class.java, String::class.java))
        assertSame(value2, map.get(List::class.java, String::class.java, "a"))
    }

    @Test
    fun `#forEach should call action for every key value pair`() {
        val expected = mapOf<DependencyKey, Any?>(
                typeKey<FixedHash>(0) to FixedHash(1),
                typeKey<FixedHash>(1) to FixedHash(1),
                typeKey<FixedHash>(2) to FixedHash(3),
                typeKey<FixedHash>(3) to FixedHash(4)
        )

        val actual = mutableMapOf<DependencyKey, Any?>()
        map = DependencyMap(expected)
        map.forEach { k, v -> actual[k] = v }
        assertEquals(expected, actual)
    }

    @Test
    fun `#size should return the count of entries in the map`() {
        assertEquals(0, map.size)

        map.put(typeKey<String>(), "")
        map.put(typeKey<String>(), "")
        assertEquals(1, map.size)

        map.put(typeKey<String>("a"), "")
        assertEquals(2, map.size)
    }

}