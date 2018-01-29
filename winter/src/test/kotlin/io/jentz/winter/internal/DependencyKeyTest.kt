package io.jentz.winter.internal

import io.jentz.winter.compoundTypeKey
import io.jentz.winter.typeKey
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
        assertSameHashAndEquals(typeKey<Map<String, List<Int>>>(generics = true), typeKey<Map<String, List<Int>>>(generics = true))
    }

    @Test
    fun `GenericTypeKey should be equal for same class with same qualifier`() {
        assertSameHashAndEquals(
                typeKey<Map<String, List<Int>>>("test", true),
                typeKey<Map<String, List<Int>>>("test", true))
    }

    @Test
    fun `GenericTypeKey should not be equal for different classes`() {
        assertNotSameHashAndEquals(
                typeKey<Map<String, List<Int>>>(generics = true),
                typeKey<Set<String>>(generics = true))
    }

    @Test
    fun `GenericTypeKey should not be equal with same class but different qualifiers`() {
        assertNotSameHashAndEquals(
                typeKey<Map<String, List<Int>>>(generics = true),
                typeKey<Map<String, List<Int>>>("test", true))
    }

    @Test
    fun `TypeKey and GenericTypeKey should be equal when created for the same class`() {
        assertSameHashAndEquals(typeKey<TestInterface>(), typeKey<TestInterface>(generics = true))
        assertSameHashAndEquals(typeKey<TestInterface>(generics = true), typeKey<TestInterface>())
    }

    @Test
    fun `TypeKey and GenericTypeKey should not be equal when created for the same class but different qualifier`() {
        assertNotSameHashAndEquals(typeKey<TestInterface>(), typeKey<TestInterface>("test", true))
        assertNotSameHashAndEquals(typeKey<TestInterface>(generics = true), typeKey<TestInterface>("test"))
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
        assertSameHashAndEquals(
                compoundTypeKey<String, Set<List<Int>>>(generics = true),
                compoundTypeKey<String, Set<List<Int>>>(generics = true))
    }

    @Test
    fun `GenericCompoundTypeKey should be equal for same class with same qualifier`() {
        assertSameHashAndEquals(
                compoundTypeKey<String, Set<List<Int>>>("test", true),
                compoundTypeKey<String, Set<List<Int>>>("test", true))
    }

    @Test
    fun `GenericCompoundTypeKey should not be equal for different classes`() {
        assertNotSameHashAndEquals(
                compoundTypeKey<String, Set<List<String>>>(generics = true),
                compoundTypeKey<String, Set<List<Int>>>(generics = true))
    }

    @Test
    fun `GenericCompoundTypeKey should not be equal with same class but different qualifiers`() {
        assertNotSameHashAndEquals(
                compoundTypeKey<String, Set<List<Int>>>("test", true),
                compoundTypeKey<String, Set<List<Int>>>(generics = true))
    }

    @Test
    fun `CompoundTypeKey and GenericCompoundTypeKey should be equal when created for the same classes`() {
        assertSameHashAndEquals(
                compoundTypeKey<String, TestInterface>(),
                compoundTypeKey<String, TestInterface>(generics = true))
        assertSameHashAndEquals(
                compoundTypeKey<String, TestInterface>(generics = true),
                compoundTypeKey<String, TestInterface>())
    }

    @Test
    fun `CompoundTypeKey and GenericCompoundTypeKey should not be equal when created for the same classes but different qualifier`() {
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