package io.jentz.winter.junit4

import io.jentz.winter.*
import io.jentz.winter.plugin.Plugin
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit 4 [test rule][TestRule] that allows to hook into the Component/Graph lifecycle.
 *
 * This registers itself as Winter plugin on [application] before executing a test and unregisters
 * itself afterwards.
 *
 * Sample where we replace a view model in a presentation graph with a test view model:
 * ```
 * val testViewModel = TestViewModel<ViewState>()
 *
 * @get:Rule
 * val winterTestRule = object : WinterTestRule() {
 *   override fun initializingComponent(parentGraph: Graph?, builder: ComponentBuilder) {
 *     if (builder.qualifier == "presentation") {
 *       builder.apply {
 *         singleton<ViewModel<QuotesViewState>>(generics = true, override = true) {
 *           viewModel
 *         }
 *       }
 *     }
 *   }
 * }
 * ```
 *
 */
open class WinterTestRule(
    private val application: WinterApplication = Winter
) : TestRule, Plugin {

    final override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                application.plugins.register(this@WinterTestRule)
                try {
                    base.evaluate()
                } finally {
                    application.plugins.unregister(this@WinterTestRule)
                }
            }
        }
    }

    override fun initializingComponent(parentGraph: Graph?, builder: ComponentBuilder) {
    }

    override fun postConstruct(graph: Graph, scope: Scope, argument: Any, instance: Any) {
    }

    override fun graphDispose(graph: Graph) {
    }
    
}
