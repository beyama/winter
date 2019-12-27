package io.jentz.winter

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TypeKeyTest {

    interface TestInterface
    class TestClass

    @Test
    fun `TypeKey should not be equal to null`() {
        @Suppress("SENSELESS_COMPARISON")
        assertFalse(typeKey<TestInterface>() == null)
    }

    @Test
    fun `TypeKey should be equal to TypeKey from same class`() {
        assertSameHashAndEquals(typeKey<TestInterface>(), typeKey<TestInterface>())
    }

    @Test
    fun `TypeKey should be equal to TypeKey from same class with same qualifier`() {
        assertSameHashAndEquals(typeKey<TestInterface>("test"), typeKey<TestInterface>("test"))
    }

    @Test
    fun `TypeKey should not be equal to TypeKey from different class`() {
        assertNotSameHashAndEquals(typeKey<TestInterface>(), typeKey<TestClass>())
    }

    @Test
    fun `TypeKey should not be equal to TypeKey from same class but with different qualifier`() {
        assertNotSameHashAndEquals(typeKey<TestInterface>(), typeKey<TestInterface>("test"))
    }

    @Test
    fun `TypeKey should be type equal to TypeKey from same class`() {
        assertTrue(typeKey<TestInterface>().typeEquals(typeKey<TestInterface>(qualifier = Any())))
    }

    @Test
    fun `TypeKey should not be type equal to TypeKey from different class`() {
        assertFalse(typeKey<TestInterface>().typeEquals(typeKey<TestClass>()))
    }

    @Test
    fun `GenericTypeKey should be equal to GenericTypeKey from same class`() {
        assertSameHashAndEquals(typeKey<Map<String, List<Int>>>(generics = true), typeKey<Map<String, List<Int>>>(generics = true))
    }

    @Test
    fun `GenericTypeKey should be equal to GenericTypeKey from same class with same qualifier`() {
        assertSameHashAndEquals(
                typeKey<Map<String, List<Int>>>("test", true),
                typeKey<Map<String, List<Int>>>("test", true))
    }

    @Test
    fun `GenericTypeKey should not be equal to GenericTypeKey from different class`() {
        assertNotSameHashAndEquals(
                typeKey<Map<String, List<Int>>>(generics = true),
                typeKey<Set<String>>(generics = true))
    }

    @Test
    fun `GenericTypeKey should not be equal to GenericTypeKey from same class but different qualifier`() {
        assertNotSameHashAndEquals(
                typeKey<Map<String, List<Int>>>(generics = true),
                typeKey<Map<String, List<Int>>>("test", true))
    }

    @Test
    fun `TypeKey and GenericTypeKey should be equal when created from the same class`() {
        assertSameHashAndEquals(typeKey<TestInterface>(), typeKey<TestInterface>(generics = true))
        assertSameHashAndEquals(typeKey<TestInterface>(generics = true), typeKey<TestInterface>())
    }

    @Test
    fun `TypeKey and GenericTypeKey should not be equal when created from the same class but different qualifier`() {
        assertNotSameHashAndEquals(typeKey<TestInterface>(), typeKey<TestInterface>("test", true))
        assertNotSameHashAndEquals(typeKey<TestInterface>(generics = true), typeKey<TestInterface>("test"))
    }

    @Test
    fun `TypeKey and GenericTypeKey should be type equal when created from the same class`() {
        assertTrue(typeKey<TestInterface>(qualifier = Any()).typeEquals(typeKey<TestInterface>(generics = true)))
        assertTrue(typeKey<TestInterface>(qualifier = Any(), generics = true).typeEquals(typeKey<TestInterface>()))
    }

    private fun assertSameHashAndEquals(left: Any, right: Any) {
        assertEquals(left.hashCode(), right.hashCode(), "Should have same hash code")
        assertEquals(left, right, "Should be equal")
    }

    private fun assertNotSameHashAndEquals(left: Any, right: Any) {
        assertNotEquals(left.hashCode(), right.hashCode(), "Should not have same hash code")
        assertNotEquals(left, right, "Should not be equal")
    }

}