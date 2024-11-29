package io.jentz.winter

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import io.jentz.winter.dsl.ancestors
import io.jentz.winter.dsl.keys
import io.jentz.winter.dsl.lazyInstance
import io.jentz.winter.dsl.prototypeOf
import io.jentz.winter.dsl.provider
import io.jentz.winter.dsl.root
import io.jentz.winter.dsl.selfAndAncestors
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GraphExtTest {

    @Test
    fun `Graph#keys`() {
        val component = component {
            prototypeOf(::Heater)
            subcomponent(QualifierSub) { prototypeOf(::Thermosiphon) }
        }
        val graph = component.createGraph().createSubgraph(QualifierSub)
        assertThat(graph.keys()).containsOnly(
            erased<Component>(QualifierSub), erased<Heater>(), erased<Thermosiphon>()
        )
    }

    @Test
    fun `Graph#selfAndAncestors`() {
        val component = component {
            subcomponent(QualifierA) { subcomponent(QualifierB) {} }
        }
        val graph = component.createGraph().createSubgraph(QualifierA).createSubgraph(QualifierB)
        assertThat(graph.selfAndAncestors.toList()).isEqualTo(
            listOf(graph, graph.parent, graph.parent?.parent)
        )
    }

    @Test
    fun `Graph#ancestors`() {
        val component = component {
            subcomponent(QualifierA) { subcomponent(QualifierB) {} }
        }
        val graph = component.createGraph().createSubgraph(QualifierA).createSubgraph(QualifierB)
        assertThat(graph.ancestors.toList()).isEqualTo(
            listOf(graph.parent, graph.parent?.parent)
        )
    }

    @Test
    fun `Graph#root`() {
        val component = component {
            subcomponent(QualifierA) { subcomponent(QualifierB) {} }
        }
        val graph = component.createGraph().createSubgraph(QualifierA).createSubgraph(QualifierB)
        assertThat(graph.root).isEqualTo(graph.parent?.parent)
    }

    @Nested
    @DisplayName("#provider")
    inner class ProviderMethod {

        @Test
        fun `should resolve provider by class`() {
            assertThat(
                graph { prototype { "string" } }.provider<String>().invoke()
            ).isEqualTo("string")
        }

        @Test
        fun `provider should return null for non-existing optional type`() {
            assertThat(
                graph {}.provider<String?>().invoke()
            ).isNull()
        }

        @Test
        fun `provider should throw if service for non-optional type returns null`() {
            assertFailure {
                graph { prototype<String?> { null } }.provider<String>().invoke()
            }.isInstanceOf<DependencyResolutionException>()
        }

        @Test
        fun `should resolve provider by generic class`() {
            assertThat(
                graph {
                    prototype(generic()) { mapOf(1 to "1") }
                }.provider(generic<Map<Int, String>>()).invoke()
            ).isEqualTo(mapOf(1 to "1"))
        }

        @Test
        fun `should resolve provider with qualifier`() {
            assertThat(
                graph {
                    prototype(erased(QualifierA)) { "a" }
                    prototype(erased(QualifierB)) { "b" }
                }.provider(erased<String>(QualifierB)).invoke()
            ).isEqualTo("b")
        }

        @Test
        fun `should throw an exception if dependency doesn't exist`() {
            assertFailure {
                emptyGraph().provider<Any>()
            }.isInstanceOf<EntryNotFoundException>()
        }

        @Test
        fun `should throw an exception when graph is closed`() {
            assertFailure {
                graph { prototype { "string" } }.apply {
                    close()
                    provider<String>()
                }
            }.isInstanceOf<WinterException>()
        }

        @Test
        fun `should postpone evaluation until provider is called`() {
            var counter = 0
            val provider = graph { prototype { counter += 1; counter } }.provider<Int>()
            assertThat(counter).isEqualTo(0)
            assertThat(provider()).isEqualTo(1)
            assertThat(provider()).isEqualTo(2)
        }

        @Test
        fun `should pass builder block to factory`() {
            val heater = Heater()
            assertThat(
                graph {
                    singleton { Thermosiphon(instance()) }
                }.provider<Thermosiphon> {
                    constant(heater)
                }.invoke().heater
            ).isEqualTo(heater)
        }
    }

    @Nested
    @DisplayName("#lazyInstance")
    inner class LazyInstanceMethod {

        @Test
        fun `should resolve instance by class`() {
            assertThat(
                graph { prototype { "string" } }.lazyInstance<String>().value
            ).isEqualTo("string")
        }

        @Test
        fun `should return null for non-existing optional type`() {
            assertThat(
                graph {}.lazyInstance<String?>().value
            ).isNull()
        }

        @Test
        fun `should throw if service for non-optional type returns null`() {
            assertFailure {
                graph { prototype<String?> { null } }.lazyInstance<String>().value
            }.isInstanceOf<DependencyResolutionException>()
        }

        @Test
        fun `should resolve instance by generic class`() {
            assertThat(
                graph {
                    prototype(generic()) { mapOf(1 to "1") }
                }.lazyInstance(generic<Map<Int, String>>()).value
            ).isEqualTo(mapOf(1 to "1"))
        }

        @Test
        fun `should resolve instance with qualifier`() {
            assertThat(
                graph {
                    prototype(erased(QualifierA)) { "a" }
                    prototype(erased(QualifierB)) { "b" }
                }.lazyInstance(erased<String>(QualifierB)).value
            ).isEqualTo("b")
        }

        @Test
        fun `should throw an exception if dependency doesn't exist`() {
            assertFailure {
                emptyGraph().lazyInstance<Any>()
            }.isInstanceOf<EntryNotFoundException>()
        }

        @Test
        fun `should throw an exception when graph is closed`() {
            assertFailure {
                graph { prototype { "string" } }.apply {
                    close()
                    lazyInstance<String>()
                }
            }.isInstanceOf<WinterException>()
        }

        @Test
        fun `should postpone evaluation until lazy value is requested`() {
            var counter = 0
            graph { prototype { counter += 1; counter } }.lazyInstance<Int>()
            assertThat(counter).isEqualTo(0)
        }

        @Test
        fun `should pass builder block to factory`() {
            val heater = Heater()
            assertThat(
                graph {
                    singleton { Thermosiphon(instance()) }
                }.lazyInstance<Thermosiphon> {
                    constant(heater)
                }.value.heater
            ).isEqualTo(heater)
        }
    }

}