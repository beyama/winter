package io.jentz.winter.dsl

import io.jentz.winter.Graph

/**
 * [Sequence] with all ancestors and this [Graph] starting with this up the parent hierarchy.
 */
val Graph.selfAndAncestors: Sequence<Graph> get() = generateSequence(this) { it.parent }

/**
 * A sequence of all ancestors. This returns a empty [Sequence] if this is a root [Graph].
 */
val Graph.ancestors get() = selfAndAncestors.drop(1)

/**
 * The root [Graph]. This returns itself if [Graph.parent] is null.
 */
val Graph.root: Graph get() = selfAndAncestors.last()

/**
 * Get parent [Graph].
 *
 * @throws IllegalStateException If parent [Graph] in null.
 */
val Graph.requireParent: Graph get() = checkNotNull(parent) {
    "Parent graph must not be null"
}