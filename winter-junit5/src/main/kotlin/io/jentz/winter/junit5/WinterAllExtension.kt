package io.jentz.winter.junit5

import io.jentz.winter.APPLICATION_COMPONENT_QUALIFIER
import io.jentz.winter.ComponentBuilderBlock
import io.jentz.winter.Winter
import io.jentz.winter.WinterApplication
import io.jentz.winter.testing.WinterTestSession.Builder
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace

/**
 * JUnit5 extension that starts a [io.jentz.winter.testing.WinterTestSession] before all tests
 * and stops the session after all tests.
 *
 * For more details see [io.jentz.winter.testing.WinterTestSession].
 */
open class WinterAllExtension(
    block: Builder.() -> Unit
) : AbstractWinterExtension(
    Namespace.create("io.jentz.winter.all"),
    Builder().apply(block)
), BeforeAllCallback, AfterAllCallback {

    /**
     * Default constructor to use this with [org.junit.jupiter.api.extension.RegisterExtension].
     *
     * The default configuration will operate on the application graph and will bind all `Mock`
     * annotated properties to it.
     *
     * This class is open and can be extended for other default configurations.
     */
    constructor(): this({ bindAllMocks() })

    final override fun beforeAll(context: ExtensionContext) {
        before(context)
    }

    final override fun afterAll(context: ExtensionContext) {
        after(context)
    }

    companion object {

        /**
         * Sugar for calling:
         * ```
         *  WinterAllExtension {
         *    application = MyWinterApp
         *    extend(qualifier) {
         *      // extend graph
         *    }
         *  }
         * ```
         *
         * @see WinterAllExtension
         *
         * @param qualifier The component qualifier of the graph that should be extended.
         * @param application The application of the graph.
         * @param block The block that is applied to the component builder of the graph.
         */
        fun extend(
            qualifier: Any = APPLICATION_COMPONENT_QUALIFIER,
            application: WinterApplication = Winter,
            block: ComponentBuilderBlock
        ): WinterAllExtension = WinterAllExtension {
            this.application = application
            extend(qualifier, block)
        }

        /**
         * Sugar for calling:
         * ```
         *  WinterAllExtension {
         *    application = MyWinterApp
         *    extend(parentQualifier, qualifier) {
         *      // extend graph
         *    }
         *  }
         * ```
         *
         * @see WinterAllExtension
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
        ): WinterAllExtension = WinterAllExtension {
            this.application = application
            extend(parentQualifier, qualifier, block)
        }

    }

}
