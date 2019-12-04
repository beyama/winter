package io.jentz.winter.junit4

import io.jentz.winter.APPLICATION_COMPONENT_QUALIFIER
import io.jentz.winter.ComponentBuilderBlock
import io.jentz.winter.Winter
import io.jentz.winter.WinterApplication
import io.jentz.winter.testing.WinterTestSession
import io.jentz.winter.testing.WinterTestSessionBlock
import io.jentz.winter.testing.injectWithReflection
import org.junit.rules.MethodRule
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement

/**
 * JUnit4 rule that starts a [io.jentz.winter.testing.WinterTestSession] before each test
 * and stops the session after each test.
 *
 * For more details see [io.jentz.winter.testing.WinterTestSession].
 */
open class WinterRule(block: WinterTestSessionBlock) : MethodRule {

    private val testSessionBuilder = WinterTestSession.Builder().apply(block)

    private var testSession: WinterTestSession? = null

    private val requireSession: WinterTestSession get() = requireNotNull(testSession) {
        "Winter test session was not created."
    }

    final override fun apply(base: Statement, method: FrameworkMethod, target: Any): Statement {
        return object : Statement() {
            override fun evaluate() {
                val session = testSessionBuilder.build(listOf(target))

                testSession = session

                session.start()
                try {
                    base.evaluate()
                } finally {
                    session.stop()
                }
            }
        }

    }

    fun inject(target: Any) {
        requireSession.requireGraph.injectWithReflection(target)
    }

    companion object {

        /**
         * Sugar for calling:
         * ```
         *  WinterRule {
         *    application = MyWinterApp
         *    extend(qualifier) {
         *      // extend graph
         *    }
         *  }
         * ```
         *
         * @see WinterRule
         *
         * @param qualifier The component qualifier of the graph that should be extended.
         * @param application The application of the graph.
         * @param block The block that is applied to the component builder of the graph.
         */
        fun extend(
            qualifier: Any = APPLICATION_COMPONENT_QUALIFIER,
            application: WinterApplication = Winter,
            block: ComponentBuilderBlock
        ): WinterRule = WinterRule {
            this.application = application
            extend(qualifier, block)
        }

        /**
         * Sugar for calling:
         * ```
         *  WinterRule {
         *    application = MyWinterApp
         *    extend(parentQualifier, qualifier) {
         *      // extend graph
         *    }
         *  }
         * ```
         *
         * @see WinterRule
         *
         * @param parentQualifier The component qualifier of the parent graph.
         * @param qualifier The component qualifier of the graph that should be extended.
         * @param application The application of the graph.
         * @param block The block that is applied to the component builder of the graph.
         */
        fun extend(
            parentQualifier: Any,
            qualifier: Any,
            application: WinterApplication = Winter,
            block: ComponentBuilderBlock
        ): WinterRule = WinterRule {
            this.application = application
            extend(parentQualifier, qualifier, block)
        }

    }

}
