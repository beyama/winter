package io.jentz.winter.junit5

import io.jentz.winter.ComponentBuilderBlock
import io.jentz.winter.Winter
import io.jentz.winter.WinterApplication

class WinterJUnit5 {

    companion object {

        /**
         * Create an [ExtendGraphExtension].
         *
         * @see ExtendGraphExtension
         */
        fun extension(
            componentQualifier: Any? = null,
            application: WinterApplication = Winter,
            block: ComponentBuilderBlock
        ): ExtendGraphExtension = ExtendGraphExtension(componentQualifier, application, block)

    }

}
