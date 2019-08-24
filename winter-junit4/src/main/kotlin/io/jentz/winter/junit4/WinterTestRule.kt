package io.jentz.winter.junit4

import io.jentz.winter.Winter
import io.jentz.winter.WinterApplication

@Deprecated(
    "Use GraphLifecycleTestRule or WinterJUnit4.rule() instead.",
    ReplaceWith(
        "GraphLifecycleTestRule(application)",
        "io.jentz.winter.junit4.GraphLifecycleTestRule"
    )
)
open class WinterTestRule(
    application: WinterApplication = Winter
) : GraphLifecycleTestRule(application)
