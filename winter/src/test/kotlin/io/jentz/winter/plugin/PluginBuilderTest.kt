package io.jentz.winter.plugin

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import io.jentz.winter.ApplicationScope
import io.jentz.winter.Graph
import io.jentz.winter.Heater
import io.jentz.winter.Qualifier
import io.jentz.winter.QualifierA
import io.jentz.winter.QualifierB
import io.jentz.winter.Thermosiphon
import io.jentz.winter.WinterApplication
import io.jentz.winter.dsl.requireParent
import io.jentz.winter.dsl.selfAndAncestors
import io.jentz.winter.dsl.singletonOf
import org.junit.jupiter.api.Test

class PluginBuilderTest {

    private val app = WinterApplication {
        singletonOf(::Heater)
        subcomponent(QualifierA) {
            singletonOf(::Thermosiphon)
            subcomponent(QualifierB) {}
        }
    }

    @Test
    fun `extender without qualifier should be called for each graph`() {
        val qualifiers = mutableListOf<Qualifier>()

        app.plugin {
            extend { qualifiers += qualifier }
        }
        app.createGraph().createSubgraph(QualifierA).createSubgraph(QualifierB)

        assertThat(qualifiers).isEqualTo(listOf(ApplicationScope, QualifierA, QualifierB))
    }

    @Test
    fun `extender with qualifier should only be called for builder with qualifier`() {
        val qualifiers = mutableListOf<Qualifier>()

        app.plugin {
            extend(QualifierA) { qualifiers += qualifier }
        }
        app.createGraph().createSubgraph(QualifierA).createSubgraph(QualifierB)

        assertThat(qualifiers).isEqualTo(listOf(QualifierA))
    }

    @Test
    fun `withNewGraph callback without qualifier should be called for each graph`() {
        val graphs = mutableListOf<Graph>()

        app.plugin {
            withNewGraph { graphs += this }
        }
        val graph = app.createGraph().createSubgraph(QualifierA).createSubgraph(QualifierB)

        assertThat(graphs).isEqualTo(graph.selfAndAncestors.toList().reversed())
    }

    @Test
    fun `withNewGraph callback with qualifier should only be called for graph with qualifier`() {
        val graphs = mutableListOf<Graph>()

        app.plugin {
            withNewGraph(QualifierA) { graphs += this }
        }
        val graph = app.createGraph().createSubgraph(QualifierA).createSubgraph(QualifierB)

        assertThat(graphs).containsExactly(graph.requireParent)
    }

    @Test
    fun `withClosingGraph callback without qualifier should be called for each closing graph`() {
        val qualifiers = mutableListOf<Qualifier>()

        app.plugin {
            withClosingGraph {
                assertThat(isClosed).isFalse()
                qualifiers += component.qualifier
            }
        }
        val graph = app.createGraph().createSubgraph(QualifierA).createSubgraph(QualifierB)
        graph.selfAndAncestors.toList().forEach { it.close() } // resolve sequence before closing

        assertThat(qualifiers).isEqualTo(listOf(QualifierB, QualifierA, ApplicationScope))
    }

    @Test
    fun `withClosingGraph callback with qualifier should only be called for closing graph with qualifier`() {
        val qualifiers = mutableListOf<Qualifier>()

        app.plugin {
            withClosingGraph(QualifierA) {
                assertThat(isClosed).isFalse()
                qualifiers += component.qualifier
            }
        }
        val graph = app.createGraph().createSubgraph(QualifierA).createSubgraph(QualifierB)
        graph.selfAndAncestors.toList().forEach { it.close() } // resolve sequence before closing

        assertThat(qualifiers).isEqualTo(listOf(QualifierA))
    }

    @Test
    fun `withNewInstance callback without qualifier should be called for each new instance`() {
        val instances = mutableListOf<Any>()

        app.plugin {
            withNewInstance<Any> { _, instance -> instances += instance }
        }
        val graph = app.createGraph().createSubgraph(QualifierA).createSubgraph(QualifierB)
        graph.instance<Thermosiphon>()

        assertThat(instances).containsExactly(graph.instance<Heater>(), graph.instance<Thermosiphon>())
    }

    @Test
    fun `withNewInstance callback with qualifier should only be called for new instances in graphs with qualifier`() {
        val instances = mutableListOf<Any>()

        app.plugin {
            withNewInstance<Any>(QualifierA) { _, instance -> instances += instance }
        }
        val graph = app.createGraph().createSubgraph(QualifierA).createSubgraph(QualifierB)
        graph.instance<Thermosiphon>()

        assertThat(instances).containsExactly(graph.instance<Thermosiphon>())
    }

    @Test
    fun `withNewInstance callback without qualifier but type should only be called for instances of type`() {
        val instances = mutableListOf<Thermosiphon>()

        app.plugin {
            withNewInstance<Thermosiphon> { _, i -> instances += i }
        }
        val graph = app.createGraph().createSubgraph(QualifierA).createSubgraph(QualifierB)
        graph.instance<Thermosiphon>()

        assertThat(instances).containsExactly(graph.instance<Thermosiphon>())
    }

    @Test
    fun `WinterApplication#plugin should register plugin and return an uninstaller`() {
        assertThat(app.plugins).isEmpty()
        val uninstaller = app.plugin {  }
        assertThat(app.plugins.size).isEqualTo(1)
        uninstaller()
        assertThat(app.plugins).isEmpty()
    }

}