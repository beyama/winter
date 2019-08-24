package io.jentz.winter.junit4

import io.jentz.winter.ComponentBuilderBlock
import io.jentz.winter.Winter
import io.jentz.winter.WinterApplication

class WinterJunit4 {

    companion object {

        /**
         * Create an [ExtendGraphTestRule].
         *
         * @see ExtendGraphTestRule
         */
        fun rule(
            componentQualifier: Any? = null,
            application: WinterApplication = Winter,
            block: ComponentBuilderBlock
        ): ExtendGraphTestRule = ExtendGraphTestRule(componentQualifier, application, block)

    }

}
