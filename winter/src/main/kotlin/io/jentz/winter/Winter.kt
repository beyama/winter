package io.jentz.winter

/**
 * The default [WinterApplication] object that is used as default by [GraphRegistry] and
 * [Component.createGraph].
 *
 * A [WinterApplication] contains all configured plugins and and the application component.
 *
 * It is recommended for applications to use this directly and for libraries it is recommended to
 * create a library specific object based on [WinterApplication].
 *
 * This should be initialized early on application startup and usually not changed during runtime.
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
 */
object Winter : WinterApplication()
