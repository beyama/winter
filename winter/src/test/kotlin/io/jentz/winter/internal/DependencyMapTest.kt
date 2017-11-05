package io.jentz.winter.internal

import io.jentz.winter.Graph
import io.jentz.winter.ServiceImpl
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DependencyMapTest {

    interface TestInterface

    private lateinit var map: DependencyMap<Any?>

    @Before
    fun before() {
        map = DependencyMap(16)
    }

    @Test
    fun `empty dependency map initialized with a capacity of 0 should return null on #get`() {
        map = DependencyMap(0)
        assertNull(map.get(TestInterface::class.java))
    }

    @Test
    fun `should be able to retrieve entry associated with type key by class`() {
        val v1 = object : TestInterface {}
        val v2 = "A string"

        map.put(typeKey<TestInterface>(), v1)
        map.put(typeKey<String>(), v2)

        assertTrue(v1 === map.get(TestInterface::class.java))
        assertTrue(v2 === map.get(String::class.java))
    }

    @Test
    fun `should be able to retrieve entry associated with type key by class and qualifier`() {
        val v1 = object : TestInterface {}
        val v2 = object : TestInterface {}

        map.put(typeKey<TestInterface>("a"), v1)
        map.put(typeKey<TestInterface>("b"), v2)

        assertTrue(v1 === map.get(TestInterface::class.java, "a"))
        assertTrue(v2 === map.get(TestInterface::class.java, "b"))
    }

    @Test
    fun `should be able to retrieve entry associated with compound key by classes`() {
        val instance = object : MembersInjector<ServiceImpl> {
            override fun injectMembers(graph: Graph, target: ServiceImpl) {
            }
        }
        map.put(membersInjectorKey<ServiceImpl>(), instance)

        assertEquals(instance, map.get(MembersInjector::class.java, ServiceImpl::class.java))
    }

}