package io.jentz.winter.junit4

import io.jentz.winter.Graph
import io.jentz.winter.testing.WinterTestSession
import io.jentz.winter.testing.WinterTestSessionBlock
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

    val testGraph: Graph? get() = testSession?.testGraph

    val requireTestGraph: Graph get() = requireSession.requireTestGraph

    override fun before() {
        testSession = testSessionBuilder.build(testInstances)
        testSession?.start()
    }

    override fun after() {
        testSession?.stop()
    }

}
