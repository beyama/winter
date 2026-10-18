package io.jentz.winter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

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

    @Test
    fun `GenericTypeKey should not be equal to TypeKey of the raw class`() {
        assertNotSameHashAndEquals(erased<List<*>>(), generic<List<String>>())
        assertNotSameHashAndEquals(generic<List<String>>(), erased<List<*>>())
    }

    @Test
    fun `GenericTypeKey should not be equal to GenericTypeKey with different type arguments`() {
        assertNotSameHashAndEquals(generic<List<String>>(), generic<List<Int>>())
        assertNotSameHashAndEquals(generic<Map<String, List<Int>>>(), generic<Map<String, List<String>>>())
    }

    @Test
    fun `GenericTypeKey should not be equal to GenericTypeKey with different variance`() {
        assertNotEquals<TypeKey<*>>(generic<MutableList<out CharSequence>>(), generic<MutableList<CharSequence>>())
        assertNotEquals<TypeKey<*>>(generic<List<*>>(), generic<List<Any?>>())
    }

    @Test
    fun `GenericTypeKey should ignore nullability`() {
        assertSameHashAndEquals(generic<List<String>>(), generic<List<String>?>())
        assertSameHashAndEquals(generic<List<String>>(), generic<List<String?>>())
        assertSameHashAndEquals(erased<String>(), generic<String?>())
    }

    @Test
    fun `optional keys should be equal to non optional keys`() {
        assertSameHashAndEquals(erased<TestClass>(), erased<TestClass?>())
        assertTrue(erased<TestClass?>().isOptional)
        assertTrue(generic<List<String>?>().isOptional)
        assertFalse(generic<List<String>>().isOptional)
    }

    @Test
    fun `KClass typeKey should be equal to erased TypeKey`() {
        assertSameHashAndEquals(erased<TestClass>(), TestClass::class.typeKey())
        assertSameHashAndEquals(erased<TestClass>(QualifierTest), TestClass::class.typeKey(QualifierTest))
    }

    @Test
    fun `ofType should return keys of the same type`() {
        val keys = setOf<TypeKey<*>>(erased<TestClass>(), erased<TestClass>(QualifierTest), erased<TestInterface>())
        assertEquals(setOf<TypeKey<*>>(erased<TestClass>(), erased<TestClass>(QualifierTest)), keys.ofType<TestClass>())
    }

    @Test
    fun `class qualifier should be equal for the same class and different for different classes`() {
        assertEquals(qualifier<TestClass>(), TestClass::class.qualifier())
        assertEquals(qualifier<TestClass>(), TestClass()::class.qualifier())
        assertNotEquals(qualifier<TestClass>(), qualifier<TestInterface>())
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