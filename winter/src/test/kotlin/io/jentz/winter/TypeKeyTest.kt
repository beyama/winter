package io.jentz.winter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeKeyTest {

    interface TestInterface
    class TestClass

    @Test
    fun `TypeKey should be equal to TypeKey from same class`() {
        assertSameHashAndEquals(erased<TestInterface>(), erased<TestInterface>())
    }

    @Test
    fun `TypeKey should be equal to TypeKey from same class with same qualifier`() {
        assertSameHashAndEquals(erased<TestInterface>(QualifierTest), erased<TestInterface>(QualifierTest))
    }

    @Test
    fun `TypeKey should not be equal to TypeKey from different class`() {
        assertNotSameHashAndEquals(erased<TestInterface>(), erased<TestClass>())
    }

    @Test
    fun `TypeKey should not be equal to TypeKey from same class but with different qualifier`() {
        assertNotSameHashAndEquals(erased<TestInterface>(), erased<TestInterface>(QualifierTest))
    }

    @Test
    fun `TypeKey should be type equal to TypeKey from same class`() {
        assertTrue(erased<TestInterface>().typeEquals(erased<TestInterface>(QualifierTest)))
    }

    @Test
    fun `TypeKey should not be type equal to TypeKey from different class`() {
        assertFalse(erased<TestInterface>().typeEquals(erased<TestClass>()))
    }

    @Test
    fun `GenericTypeKey should be equal to GenericTypeKey from same class`() {
        assertSameHashAndEquals(generic<Map<String, List<Int>>>(), generic<Map<String, List<Int>>>())
    }

    @Test
    fun `GenericTypeKey should be equal to GenericTypeKey from same class with same qualifier`() {
        assertSameHashAndEquals(
            generic<Map<String, List<Int>>>(QualifierTest),
            generic<Map<String, List<Int>>>(QualifierTest))
    }

    @Test
    fun `GenericTypeKey should not be equal to GenericTypeKey from different class`() {
        assertNotSameHashAndEquals(
            generic<Map<String, List<Int>>>(),
            generic<Set<String>>())
    }

    @Test
    fun `GenericTypeKey should not be equal to GenericTypeKey from same class but different qualifier`() {
        assertNotSameHashAndEquals(
            generic<Map<String, List<Int>>>(),
            generic<Map<String, List<Int>>>(QualifierTest))
    }

    @Test
    fun `TypeKey and GenericTypeKey should be equal when created from the same class`() {
        assertSameHashAndEquals(erased<TestInterface>(), generic<TestInterface>())
        assertSameHashAndEquals(generic<TestInterface>(), erased<TestInterface>())
    }

    @Test
    fun `TypeKey and GenericTypeKey should not be equal when created from the same class but different qualifier`() {
        assertNotSameHashAndEquals(erased<TestInterface>(), generic<TestInterface>(QualifierTest))
        assertNotSameHashAndEquals(generic<TestInterface>(), erased<TestInterface>(QualifierTest))
    }

    @Test
    fun `TypeKey and GenericTypeKey should be type equal when created from the same class`() {
        assertTrue(erased<TestInterface>(QualifierTest).typeEquals(generic<TestInterface>()))
        assertTrue(generic<TestInterface>(QualifierTest).typeEquals(erased<TestInterface>()))
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