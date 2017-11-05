package io.jentz.winter.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DependencyKeyTest {

    interface TestInterface
    class TestClass

    @Test
    fun `TypeKey should be equal for same class`() {
        assertSameHashAndEquals(typeKey<TestInterface>(), typeKey<TestInterface>())
    }

    @Test
    fun `TypeKey should be equal for same class with same qualifier`() {
        assertSameHashAndEquals(typeKey<TestInterface>("test"), typeKey<TestInterface>("test"))
    }

    @Test
    fun `TypeKey should not be equal for different classes`() {
        assertNotSameHashAndEquals(typeKey<TestInterface>(), typeKey<TestClass>())
    }

    @Test
    fun `TypeKey should not be equal with different qualifiers`() {
        assertNotSameHashAndEquals(typeKey<TestInterface>(), typeKey<TestInterface>("test"))
    }

    @Test
    fun `GenericTypeKey should be equal for same class`() {
        assertSameHashAndEquals(genericTypeKey<Map<String, List<Int>>>(), genericTypeKey<Map<String, List<Int>>>())
    }

    @Test
    fun `GenericTypeKey should be equal for same class with same qualifier`() {
        assertSameHashAndEquals(genericTypeKey<Map<String, List<Int>>>("test"), genericTypeKey<Map<String, List<Int>>>("test"))
    }

    @Test
    fun `GenericTypeKey should not be equal for different classes`() {
        assertNotSameHashAndEquals(genericTypeKey<Map<String, List<Int>>>(), genericTypeKey<Set<String>>())
    }

    @Test
    fun `GenericTypeKey should not be equal with same class but different qualifiers`() {
        assertNotSameHashAndEquals(genericTypeKey<Map<String, List<Int>>>(), genericTypeKey<Map<String, List<Int>>>("test"))
    }

    @Test
    fun `TypeKey and GenericTypeKey should be equal when created for the same class`() {
        assertSameHashAndEquals(typeKey<TestInterface>(), genericTypeKey<TestInterface>())
        assertSameHashAndEquals(genericTypeKey<TestInterface>(), typeKey<TestInterface>())
    }

    @Test
    fun `TypeKey and GenericTypeKey should not be equal when created for the same class but different qualifier`() {
        assertNotSameHashAndEquals(typeKey<TestInterface>(), genericTypeKey<TestInterface>("test"))
        assertNotSameHashAndEquals(genericTypeKey<TestInterface>(), typeKey<TestInterface>("test"))
    }

    @Test
    fun `CompoundTypeKey should be equal for same classes`() {
        assertSameHashAndEquals(compoundTypeKey<Int, String>(), compoundTypeKey<Int, String>())
    }

    @Test
    fun `CompoundTypeKey should be equal for same classes with same qualifier`() {
        assertSameHashAndEquals(compoundTypeKey<Int, String>("test"), compoundTypeKey<Int, String>("test"))
    }

    @Test
    fun `CompoundTypeKey should not be equal for different classes`() {
        assertNotSameHashAndEquals(compoundTypeKey<Int, String>(), compoundTypeKey<String, Int>())
    }

    @Test
    fun `CompoundTypeKey should not be equal with same classes but different qualifiers`() {
        assertNotSameHashAndEquals(compoundTypeKey<Int, String>(), compoundTypeKey<String, Int>("test"))
    }

    @Test
    fun `GenericCompoundTypeKey should be equal for same classes`() {
        assertSameHashAndEquals(genericCompoundTypeKey<String, Set<List<Int>>>(), genericCompoundTypeKey<String, Set<List<Int>>>())
    }

    @Test
    fun `GenericCompoundTypeKey should be equal for same class with same qualifier`() {
        assertSameHashAndEquals(genericCompoundTypeKey<String, Set<List<Int>>>("test"), genericCompoundTypeKey<String, Set<List<Int>>>("test"))
    }

    @Test
    fun `GenericCompoundTypeKey should not be equal for different classes`() {
        assertNotSameHashAndEquals(genericCompoundTypeKey<String, Set<List<String>>>(), genericCompoundTypeKey<String, Set<List<Int>>>())
    }

    @Test
    fun `GenericCompoundTypeKey should not be equal with same class but different qualifiers`() {
        assertNotSameHashAndEquals(genericCompoundTypeKey<String, Set<List<Int>>>("test"), genericCompoundTypeKey<String, Set<List<Int>>>())
    }

    @Test
    fun `CompoundTypeKey and GenericCompoundTypeKey should be equal when created for the same classes`() {
        assertSameHashAndEquals(compoundTypeKey<String, TestInterface>(), genericCompoundTypeKey<String, TestInterface>())
        assertSameHashAndEquals(genericCompoundTypeKey<String, TestInterface>(), compoundTypeKey<String, TestInterface>())
    }

    @Test
    fun `CompoundTypeKey and GenericCompoundTypeKey should not be equal when created for the same classes but different qualifier`() {
        assertNotSameHashAndEquals(compoundTypeKey<String, TestInterface>(), genericCompoundTypeKey<String, TestInterface>("test"))
        assertNotSameHashAndEquals(genericCompoundTypeKey<String, TestInterface>(), compoundTypeKey<String, TestInterface>("test"))
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