package io.jentz.winter.adapter

import io.jentz.winter.*
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ApplicationGraphOnlyInjectionAdapterTest {

    private val app = WinterApplication {}

    private val tree = app.tree

    private val adapter = ApplicationGraphOnlyInjectionAdapter(tree)

    @BeforeEach
    fun beforeEach() {
        tree.closeIfOpen()
    }

    @Test
    fun `#get should return null if no root graph is open`() {
        adapter.get(Any()).shouldBeNull()
    }

    @Test
    fun `#get should return root graph for any argument`() {
        val graph = tree.open()
        repeat(2) { adapter.get(Any()).shouldBeSameInstanceAs(graph) }
    }

    @Test
    fun `#open should open root graph`() {
        adapter.open(Any(), null)
        tree.isOpen().shouldBeTrue()
    }

    @Test
    fun `#open should apply builder block`() {
        adapter.open(Any()) { constant("") }
        tree.get().component.shouldContainService(typeKey<String>())
    }

    @Test
    fun `#open should throw an exception if root graph is already open`() {
        tree.open()
        shouldThrow<WinterException> { adapter.open(Any(), null) }
    }

    @Test
    fun `#close should close root graph`() {
        tree.open()
        expectValueToChange(true, false, { tree.isOpen() }) {
            adapter.close(Any())
        }
    }

    @Test
    fun `#close should throw an exception if root graph is not open`() {
        shouldThrow<WinterException> { adapter.close(Any()) }
    }

    @Test
    fun `#useApplicationGraphOnlyAdapter should register adapter`() {
        app.useApplicationGraphOnlyAdapter()
        app.injectionAdapter.shouldBeInstanceOf<ApplicationGraphOnlyInjectionAdapter>()
    }

}