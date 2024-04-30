package io.jentz.winter.delegate

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import io.jentz.winter.component
import io.jentz.winter.emptyGraph
import io.jentz.winter.graph
import io.jentz.winter.qualifier
import io.jentz.winter.erased
import org.junit.jupiter.api.Test

class GraphResolverTest {

    class Target

    @Test
    fun `Component#Builder#graphResolver should register resolver`() {
        val component = component {
            graphResolver<Target> { root, _ -> root }
        }
        val graph = component.createGraph()

        assertThat(graph.keys()).contains(erased<GraphResolver<*>>(Target::class.qualifier()))
    }

    @Test
    fun `Graph#graphResolver should return null if resolver doesn't exist`() {
        val graph = graph {}
        assertThat(graph.graphResolver(Target())).isNull()
    }

    @Test
    fun `Graph#graphResolver should return registered resolver`() {
        val resolver = GraphResolver<Target> { _, _ -> emptyGraph() }
        val graph = graph { graphResolver(resolver = resolver) }
        assertThat(graph.graphResolver(Target())).isSameInstanceAs(resolver)
    }

    @Test
    fun `Graph#resolveGraph should return result from graph resolver`() {
        val graph = graph {
            graphResolver<Target> { root, _ -> root.createSubgraph(qualifier("sub")) }
            subcomponent(qualifier("sub")) {}
        }
        assertThat(graph.resolveGraph(Target()).component.qualifier)
            .isEqualTo(qualifier("sub"))
    }

    @Test
    fun `Graph#resolveGraph should return itself if no resolver was found`() {
        val graph = graph {}
        assertThat(graph.resolveGraph(Target())).isSameInstanceAs(graph)
    }

}