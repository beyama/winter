package io.jentz.winter

/**
 * The default [WinterApplication] object.
 *
 * It is recommended for applications to use this directly and for libraries it is recommended to
 * create a library specific object based on [WinterApplication].
 *
 * Usage example with winter-rxjava2 disposable plugin:
 *
 * ```
 * // configure application component
 * Winter.component {
 *   // ... dependency declaration
 * }
 * // install RxJava 2 disposable plugin
 * Winter.installDisposablePlugin()
 * // create dependency graph
 * val graph = Winter.createGraph()
 * ```
 *
 * @see WinterApplication for more details.
 */
object Winter : WinterApplication()
