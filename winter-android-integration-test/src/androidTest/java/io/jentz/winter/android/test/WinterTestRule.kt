package io.jentz.winter.android.test

import io.jentz.winter.*
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

open class WinterTestRule : TestRule {

    private val initializingComponentPlugin: InitializingComponentPlugin = ::onGraphInitialization

    private val postConstructPlugin: PostConstructPlugin = ::onPostConstruct

    private val graphDisposePlugin: GraphDisposePlugin = ::onDispose

    final override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                WinterPlugins.addInitializingComponentPlugin(initializingComponentPlugin)
                WinterPlugins.addPostConstructPlugin(postConstructPlugin)
                WinterPlugins.addGraphDisposePlugin(graphDisposePlugin)
                try {
                    base.evaluate()
                } finally {
                    WinterPlugins.removeInitializingComponentPlugin(initializingComponentPlugin)
                    WinterPlugins.removePostConstructPlugin(postConstructPlugin)
                    WinterPlugins.removeGraphDisposePlugin(graphDisposePlugin)
                }
            }
        }
    }

    open fun onGraphInitialization(parentGraph: Graph?, builder: ComponentBuilder) {
    }

    open fun onPostConstruct(graph: Graph, scope: Scope, argument: Any?, instance: Any) {
    }

    open fun onDispose(graph: Graph) {
    }

}