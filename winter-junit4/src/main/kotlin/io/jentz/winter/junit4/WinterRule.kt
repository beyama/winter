package io.jentz.winter.junit4

import io.jentz.winter.*
import io.jentz.winter.testing.WinterTestSession
import io.jentz.winter.testing.WinterTestSessionBlock
import io.jentz.winter.testing.injectWithReflection
import org.junit.rules.ExternalResource

/**
 * JUnit4 rule that starts a [io.jentz.winter.testing.WinterTestSession] before each test
 * and stops the session after each test.
 *
 * For more details see [io.jentz.winter.testing.WinterTestSession].
 */
open class WinterRule private constructor(
    private val testInstances: List<Any>,
    block: WinterTestSessionBlock
) : ExternalResource() {

    /**
     * Create an instance with [test] instance.
     *
     * @param test The test instance used by the [WinterTestSession].
     * @param block The [WinterTestSession] builder block.
     */
    constructor(test: Any, block: WinterTestSessionBlock) : this(listOf(test), block)

    /**
     * Creates an instance without test instance.
     *
     * @param block The [WinterTestSession] builder block.
     */
    constructor(block: WinterTestSessionBlock) : this(emptyList(), block)

    private val testSessionBuilder = WinterTestSession.Builder().apply(block)

    private var testSession: WinterTestSession? = null

    private val requireSession: WinterTestSession get() = requireNotNull(testSession) {
        "Winter test session was not created."
    }

    val testGraph: Graph? get() = testSession?.graph

    val requireTestGraph: Graph get() = requireSession.requireGraph

    override fun before() {
        testSession = testSessionBuilder.build(testInstances)
        testSession?.start()
    }

    override fun after() {
        testSession?.stop()
    }

    fun inject(target: Any) {
        requireSession.requireGraph.injectWithReflection(target)
    }

    companion object {

        /**
         * Sugar for calling:
         * ```
         *  WinterRule(testInstance) {
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
