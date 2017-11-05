package io.jentz.winter.internal

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DependencyMapTest {

    interface TestInterface

    private lateinit var locator: DependencyMap<Any?>

    @Before
    fun before() {
        locator = DependencyMap(16)
    }

    @Test
    fun `empty dependency map initialized with a capacity of 0 should return null on #get`() {
        locator = DependencyMap(0)
        assertNull(locator.get(TestInterface::class))
    }

    @Test
    fun `should be able to retrieve entry by class`() {
        val v1 = object : TestInterface {}
        val v2 = "A string"

        locator.put(typeKey<TestInterface>(), v1)
        locator.put(typeKey<String>(), v2)

        assertTrue(v1 === locator.get(TestInterface::class))
        assertTrue(v2 === locator.get(String::class))
    }

    @Test
    fun `should be able to retrieve entry by class and qualifier`() {
        val v1 = object : TestInterface {}
        val v2 = object : TestInterface {}

        locator.put(typeKey<TestInterface>("a"), v1)
        locator.put(typeKey<TestInterface>("b"), v2)

        assertTrue(v1 === locator.get(TestInterface::class, "a"))
        assertTrue(v2 === locator.get(TestInterface::class, "b"))
    }

    @Test
    fun `should be able to retrieve factory entry by class and qualifier`() {
        val v1 = object : TestInterface {}
        val v2 = { arg: Int -> arg.toString() }

        locator.put(typeKey<TestInterface>(), v1)
        locator.put(compoundTypeKey<Int, String>(), v2)

        assertEquals(v1, locator.get(TestInterface::class))
        assertEquals(v2, locator.get(Int::class, String::class))
    }

}