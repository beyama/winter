package io.jentz.winter.junit5

import io.jentz.winter.ClassTypeKey
import io.jentz.winter.Graph
import io.jentz.winter.WinterApplication
import io.jentz.winter.plugin.PluginBuilderBlock
import io.jentz.winter.plugin.plugin
import io.jentz.winter.qualifier
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolutionException
import org.junit.jupiter.api.extension.ParameterResolver

abstract class AbstractWinterExtension(
    private val namespace: ExtensionContext.Namespace,
    private val application: WinterApplication,
    private val block: PluginBuilderBlock
) : ParameterResolver {

    private var uninstaller: (() -> Unit)? = null

    protected fun before(context: ExtensionContext) {
        uninstaller = application.plugin {
            block()

            withNewGraph {
                context.graph = this
            }
        }
    }

    protected fun after(context: ExtensionContext) {
        uninstaller?.invoke()
        uninstaller = null
    }

    final override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Boolean = parameterContext.isAnnotated(WInject::class.java)

    final override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Any {
        val parameter = parameterContext.parameter

        val type = parameter.type

        val qualifier: String? = parameterContext
            .findAnnotation(WInject::class.java)
            .map { it.qualifier }
            .filter { it.isNotBlank() }
            .orElse(null)

        try {
            val key = ClassTypeKey(
                type = type.kotlin.javaObjectType,
                qualifier = qualifier?.let { qualifier(it) }
            )
            return extensionContext.graph.instance(key)
        } catch (t: Throwable) {
            throw ParameterResolutionException("Error resolving parameter `${parameter}`", t)
        }
    }

    private var ExtensionContext.graph: Graph
        get() = getStore(namespace).get(GRAPH, Graph::class.java)
        set(value) = getStore(namespace).put(GRAPH, value)

    companion object {
        private const val GRAPH = "graph"
    }

}
