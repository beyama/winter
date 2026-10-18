package io.jentz.winter.plugin

import io.jentz.winter.Component
import io.jentz.winter.Graph
import io.jentz.winter.Qualifier
import io.jentz.winter.WinterApplication

typealias PluginBuilderBlock = PluginBuilder.() -> Unit
typealias PluginGraphExtender = Component.Builder.(parent: Graph?) -> Unit
typealias PluginGraphCallback = Graph.() -> Unit
typealias PluginPostConstruct<T> = Graph.(scope: Qualifier, instance: T) -> Unit

class PluginBuilder internal constructor() {

    private var graphExtenders = emptyList<Pair<Qualifier?, PluginGraphExtender>>()
    private var graphNew = emptyList<Pair<Qualifier?, PluginGraphCallback>>()
    private var graphClosing = emptyList<Pair<Qualifier?, PluginGraphCallback>>()
    private var newInstance = emptyList<Pair<Qualifier?, PluginPostConstruct<Any>>>()

    /**
     * Register a [PluginGraphExtender] that gets called for every opening [Graph] or every [Graph]
     * with matching [qualifier] if a [Qualifier] is given.
     */
    fun extend(
        qualifier: Qualifier? = null,
        extender: PluginGraphExtender
    ): PluginBuilder {
        graphExtenders += qualifier to extender
        return this
    }

    /**
     * Register a [PluginGraphCallback] that gets called for each freshly opened [Graph] or
     * every opened [Graph] with matching [qualifier] if a [Qualifier] is given.
     */
    fun withNewGraph(
        qualifier: Qualifier? = null,
        callback: PluginGraphCallback
    ): PluginBuilder {
        graphNew += qualifier to callback
        return this
    }

    /**
     * Register a [PluginGraphCallback] that gets called for each closing [Graph] or every closing
     * [Graph] with matching [qualifier] if a [Qualifier] is given.
     */
    fun withClosingGraph(
        qualifier: Qualifier? = null,
        callback: PluginGraphCallback
    ): PluginBuilder {
        graphClosing += qualifier to callback
        return this
    }

    /**
     * Register a [PluginPostConstruct] callback that gets called for ever newly constructed
     * instance of type [R].
     */
    inline fun <reified R: Any> withNewInstance(
        qualifier: Qualifier? = null,
        noinline callback: PluginPostConstruct<R>
    ) = internalWithNewInstance(qualifier) { scope, instance ->
        if (instance is R) callback(scope, instance)
    }

    @PublishedApi
    internal fun internalWithNewInstance(
        qualifier: Qualifier? = null,
        callback: PluginPostConstruct<Any>
    ): PluginBuilder {
        newInstance += qualifier to callback
        return this
    }

    internal fun build(): Plugin {
        val graphExtenders = this.graphExtenders
        val graphNew = this.graphNew
        val graphClosing = this.graphClosing
        val newInstance = this.newInstance

        return object : Plugin {
            override fun graphInitializing(parentGraph: Graph?, builder: Component.Builder) {
                for ((qualifier, extender) in graphExtenders) {
                    if (qualifier == null || builder.qualifier == qualifier) {
                        builder.extender(parentGraph)
                    }
                }
            }

            override fun graphInitialized(graph: Graph) {
                for ((qualifier, callback) in graphNew) {
                    if (qualifier == null || graph.component.qualifier == qualifier) {
                        graph.callback()
                    }
                }
            }

            override fun graphClose(graph: Graph) {
                for ((qualifier, callback) in graphClosing) {
                    if (qualifier == null || graph.component.qualifier == qualifier) {
                        graph.callback()
                    }
                }
            }

            override fun postConstruct(graph: Graph, scope: Qualifier, instance: Any) {
                for ((qualifier, callback) in newInstance) {
                    if (qualifier == null || graph.component.qualifier == qualifier) {
                        graph.callback(scope, instance)
                    }
                }
            }
        }
    }

}

fun WinterApplication.plugin(builder: PluginBuilderBlock): () -> Unit {
    val plugin = PluginBuilder().apply(builder).build()
    plugins += plugin
    return { plugins -= plugin }
}
