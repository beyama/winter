package io.jentz.winter.aware

import io.jentz.winter.ComponentBuilderBlock
import io.jentz.winter.Graph

/**
 * Open dependency graph for [this].
 *
 * @see io.jentz.winter.WinterApplication.openGraph
 */
fun WinterAware.openGraph(block: ComponentBuilderBlock? = null): Graph =
    winterApplication.openGraph(this, block)

/**
 * Get or open graph for [this].
 *
 * @see io.jentz.winter.WinterApplication.getOrOpenGraph
 */
fun WinterAware.getOrOpenGraph(block: ComponentBuilderBlock? = null): Graph =
    winterApplication.getOrOpenGraph(this, block)

/**
 * Open dependency graph for [this] and inject all members into [this].
 *
 * @see io.jentz.winter.WinterApplication.openGraphAndInject
 */
fun WinterAware.openGraphAndInject(block: ComponentBuilderBlock? = null): Graph =
    winterApplication.openGraphAndInject(this, block)

/**
 * Check if dependency graph for [this] is open.
 *
 * @see io.jentz.winter.WinterApplication.isGraphOpen
 */
fun WinterAware.isGraphOpen(): Boolean =
    winterApplication.isGraphOpen(this)

/**
 * Close the dependency graph of [this].
 *
 * @see io.jentz.winter.WinterApplication.closeGraph
 */
fun WinterAware.closeGraph() {
    winterApplication.closeGraph(this)
}
