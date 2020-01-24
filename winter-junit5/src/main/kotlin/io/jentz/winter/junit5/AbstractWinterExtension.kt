package io.jentz.winter.junit5

import io.jentz.winter.testing.WinterTestSession
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolutionException
import org.junit.jupiter.api.extension.ParameterResolver

abstract class AbstractWinterExtension(
    private val namespace: ExtensionContext.Namespace,
    private val sessionBuilder: WinterTestSession.Builder
) : ParameterResolver {

    protected fun before(context: ExtensionContext) {
        val instances = context.testInstances.map { it.allInstances }.orElse(emptyList())
        sessionBuilder.build(instances).apply {
            context.session = this
            start()
        }
    }

    protected fun after(context: ExtensionContext) {
        context.session.stop()
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
            return extensionContext.session.resolve(type, qualifier)
        } catch (t: Throwable) {
            throw ParameterResolutionException("Error resolving parameter `${parameter}`", t)
        }
    }

    private var ExtensionContext.session: WinterTestSession
        get() = getStore(namespace).get(SESSION, WinterTestSession::class.java)
        set(value) = getStore(namespace).put(SESSION, value)

    companion object {
        private const val SESSION = "session"
    }

}
