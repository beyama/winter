package io.jentz.winter.junit4

import io.jentz.winter.testing.WinterTestSession
import io.jentz.winter.testing.WinterTestSessionBlock
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement

/**
 * JUnit4 runner that starts a [io.jentz.winter.testing.WinterTestSession] before each test
 * and stops the session after each test.
 *
 * For more details see [io.jentz.winter.testing.WinterTestSession].
 */
open class WinterRunner(
    klass: Class<*>,
    block: WinterTestSessionBlock
) : BlockJUnit4ClassRunner(klass) {

    constructor(klass: Class<*>) : this(klass, { bindAllMocks() })

    private val sessionBuilder = WinterTestSession.Builder().apply(block)

    private lateinit var session: WinterTestSession

    override fun withBefores(
        method: FrameworkMethod,
        target: Any,
        statement: Statement
    ): Statement {
        val base = super.withBefores(method, target, statement)

        return object : Statement() {
            override fun evaluate() {
                session = sessionBuilder.build(listOf(target))
                session.start()
                base.evaluate()
            }
        }
    }

    override fun withAfters(
        method: FrameworkMethod,
        target: Any,
        statement: Statement
    ): Statement {
        val base = super.withAfters(method, target, statement)

        return object : Statement() {
            override fun evaluate() {
                try {
                    base.evaluate()
                } finally {
                    session.stop()
                }
            }
        }
    }

}
