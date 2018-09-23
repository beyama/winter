package io.jentz.winter

import org.junit.Assert.*
import org.junit.Test

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

    @Test
    fun `CompoundTypeKey should be equal to CompoundTypeKey from same classes`() {
        assertSameHashAndEquals(compoundTypeKey<Int, String>(), compoundTypeKey<Int, String>())
    }

    @Test
    fun `CompoundTypeKey should be equal to CompoundTypeKey from same classes with same qualifier`() {
        assertSameHashAndEquals(compoundTypeKey<Int, String>("test"), compoundTypeKey<Int, String>("test"))
    }

    @Test
    fun `CompoundTypeKey should not be equal to CompoundTypeKey from different classes`() {
        assertNotSameHashAndEquals(compoundTypeKey<Int, String>(), compoundTypeKey<String, Int>())
    }

    @Test
    fun `CompoundTypeKey should not be equal to CompoundTypeKey from same classes but different qualifier`() {
        assertNotSameHashAndEquals(compoundTypeKey<Int, String>(), compoundTypeKey<String, Int>("test"))
    }

    @Test
    fun `CompoundTypeKey should be type equal to CompoundTypeKey from same classes`() {
        assertTrue(compoundTypeKey<Int, String>(qualifier = Any()).typeEquals(compoundTypeKey<Int, String>()))
    }

    @Test
    fun `CompoundTypeKey should not be type equal to CompoundTypeKey from different classes`() {
        assertFalse(compoundTypeKey<Int, String>().typeEquals(compoundTypeKey<String, Int>()))
    }

    @Test
    fun `GenericCompoundTypeKey should be equal to GenericCompoundTypeKey from same classes`() {
        assertSameHashAndEquals(
                compoundTypeKey<String, Set<List<Int>>>(generics = true),
                compoundTypeKey<String, Set<List<Int>>>(generics = true))
    }

    @Test
    fun `GenericCompoundTypeKey should be equal to GenericCompoundTypeKey from same classes with same qualifier`() {
        assertSameHashAndEquals(
                compoundTypeKey<String, Set<List<Int>>>("test", true),
                compoundTypeKey<String, Set<List<Int>>>("test", true))
    }

    @Test
    fun `GenericCompoundTypeKey should not be equal to GenericCompoundTypeKey from different classes`() {
        assertNotSameHashAndEquals(
                compoundTypeKey<String, Set<List<String>>>(generics = true),
                compoundTypeKey<String, Set<List<Int>>>(generics = true))
    }

    @Test
    fun `GenericCompoundTypeKey should not be equal to GenericCompoundTypeKey from same classes but different qualifiers`() {
        assertNotSameHashAndEquals(
                compoundTypeKey<String, Set<List<Int>>>("test", true),
                compoundTypeKey<String, Set<List<Int>>>(generics = true))
    }

    @Test
    fun `GenericCompoundTypeKey should be type equal to GenericCompoundTypeKey from same classes`() {
        assertTrue(compoundTypeKey<String, Set<List<Int>>>(qualifier = Any(), generics = true)
                .typeEquals(compoundTypeKey<String, Set<List<Int>>>(generics = true)))
    }

    @Test
    fun `GenericCompoundTypeKey should not be type equal to GenericCompoundTypeKey from different classes`() {
        assertFalse(compoundTypeKey<String, Set<List<String>>>(qualifier = Any(), generics = true)
                .typeEquals(compoundTypeKey<String, Set<List<Int>>>(generics = true)))
    }

    @Test
    fun `CompoundTypeKey and GenericCompoundTypeKey should be equal when created from the same classes`() {
        assertSameHashAndEquals(
                compoundTypeKey<String, TestInterface>(),
                compoundTypeKey<String, TestInterface>(generics = true))
        assertSameHashAndEquals(
                compoundTypeKey<String, TestInterface>(generics = true),
                compoundTypeKey<String, TestInterface>())
    }

    @Test
    fun `CompoundTypeKey and GenericCompoundTypeKey should not be equal when created from the same classes but different qualifier`() {
        assertNotSameHashAndEquals(
                compoundTypeKey<String, TestInterface>(),
                compoundTypeKey<String, TestInterface>("test", true))
        assertNotSameHashAndEquals(
                compoundTypeKey<String, TestInterface>(generics = true),
                compoundTypeKey<String, TestInterface>("test"))
    }

    private fun assertSameHashAndEquals(left: Any, right: Any) {
        assertEquals("Should have same hash code", left.hashCode(), right.hashCode())
        assertEquals("Should be equal", left, right)
    }

    private fun assertNotSameHashAndEquals(left: Any, right: Any) {
        assertNotEquals("Should not have same hash code", left.hashCode(), right.hashCode())
        assertNotEquals("Should not be equal", left, right)
    }

}