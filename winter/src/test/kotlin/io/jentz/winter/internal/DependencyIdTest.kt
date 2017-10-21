package io.jentz.winter.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DependencyIdTest {

    interface TestInterface
    class TestClass

    @Test
    fun `ProviderId should be equal for same class`() {
        assertSameHashAndEquals(providerId<TestInterface>(), providerId<TestInterface>())
    }

    @Test
    fun `ProviderId should be equal for same class with same qualifier`() {
        assertSameHashAndEquals(providerId<TestInterface>("test"), providerId<TestInterface>("test"))
    }

    @Test
    fun `ProviderId should not be equal for different classes`() {
        assertNotSameHashAndEquals(providerId<TestInterface>(), providerId<TestClass>())
    }

    @Test
    fun `ProviderId should not be equal with different qualifiers`() {
        assertNotSameHashAndEquals(providerId<TestInterface>(), providerId<TestInterface>("test"))
    }

    @Test
    fun `GenericProviderId should be equal for same class`() {
        assertSameHashAndEquals(genericProviderId<Map<String, List<Int>>>(), genericProviderId<Map<String, List<Int>>>())
    }

    @Test
    fun `GenericProviderId should be equal for same class with same qualifier`() {
        assertSameHashAndEquals(genericProviderId<Map<String, List<Int>>>("test"), genericProviderId<Map<String, List<Int>>>("test"))
    }

    @Test
    fun `GenericProviderId should not be equal for different classes`() {
        assertNotSameHashAndEquals(genericProviderId<Map<String, List<Int>>>(), genericProviderId<Set<String>>())
    }

    @Test
    fun `GenericProviderId should not be equal with same class but different qualifiers`() {
        assertNotSameHashAndEquals(genericProviderId<Map<String, List<Int>>>(), genericProviderId<Map<String, List<Int>>>("test"))
    }

    @Test
    fun `ProviderId and GenericProviderId should be equal when created for the same class`() {
        assertSameHashAndEquals(providerId<TestInterface>(), genericProviderId<TestInterface>())
        assertSameHashAndEquals(genericProviderId<TestInterface>(), providerId<TestInterface>())
    }

    @Test
    fun `ProviderId and GenericProviderId should not be equal when created for the same class but different qualifier`() {
        assertNotSameHashAndEquals(providerId<TestInterface>(), genericProviderId<TestInterface>("test"))
        assertNotSameHashAndEquals(genericProviderId<TestInterface>(), providerId<TestInterface>("test"))
    }

    @Test
    fun `FactoryId should be equal for same classes`() {
        assertSameHashAndEquals(factoryId<Int, String>(), factoryId<Int, String>())
    }

    @Test
    fun `FactoryId should be equal for same classes with same qualifier`() {
        assertSameHashAndEquals(factoryId<Int, String>("test"), factoryId<Int, String>("test"))
    }

    @Test
    fun `FactoryId should not be equal for different classes`() {
        assertNotSameHashAndEquals(factoryId<Int, String>(), factoryId<String, Int>())
    }

    @Test
    fun `FactoryId should not be equal with same classes but different qualifiers`() {
        assertNotSameHashAndEquals(factoryId<Int, String>(), factoryId<String, Int>("test"))
    }

    @Test
    fun `GenericFactoryId should be equal for same classes`() {
        assertSameHashAndEquals(genericFactoryId<String, Set<List<Int>>>(), genericFactoryId<String, Set<List<Int>>>())
    }

    @Test
    fun `GenericFactoryId should be equal for same class with same qualifier`() {
        assertSameHashAndEquals(genericFactoryId<String, Set<List<Int>>>("test"), genericFactoryId<String, Set<List<Int>>>("test"))
    }

    @Test
    fun `GenericFactoryId should not be equal for different classes`() {
        assertNotSameHashAndEquals(genericFactoryId<String, Set<List<String>>>(), genericFactoryId<String, Set<List<Int>>>())
    }

    @Test
    fun `GenericFactoryId should not be equal with same class but different qualifiers`() {
        assertNotSameHashAndEquals(genericFactoryId<String, Set<List<Int>>>("test"), genericFactoryId<String, Set<List<Int>>>())
    }

    @Test
    fun `FactoryId and GenericFactoryId should be equal when created for the same classes`() {
        assertSameHashAndEquals(factoryId<String, TestInterface>(), genericFactoryId<String, TestInterface>())
        assertSameHashAndEquals(genericFactoryId<String, TestInterface>(), factoryId<String, TestInterface>())
    }

    @Test
    fun `FactoryId and GenericFactoryId should not be equal when created for the same classes but different qualifier`() {
        assertNotSameHashAndEquals(factoryId<String, TestInterface>(), genericFactoryId<String, TestInterface>("test"))
        assertNotSameHashAndEquals(genericFactoryId<String, TestInterface>(), factoryId<String, TestInterface>("test"))
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