package io.jentz.winter.junit4

import io.jentz.winter.APPLICATION_COMPONENT_QUALIFIER
import io.jentz.winter.ComponentBuilderBlock
import io.jentz.winter.Winter
import io.jentz.winter.WinterApplication

class WinterJUnit4 {

    companion object {

        /**
         * Create an [ExtendGraphTestRule].
         *
         * @see ExtendGraphTestRule
         */
        fun rule(
            componentQualifier: Any = APPLICATION_COMPONENT_QUALIFIER,
            application: WinterApplication = Winter,
            block: ComponentBuilderBlock
        ): ExtendGraphTestRule = ExtendGraphTestRule(componentQualifier, application, block)

    }

}
