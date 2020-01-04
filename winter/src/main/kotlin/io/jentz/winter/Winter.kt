package io.jentz.winter

/**
 * The default [WinterApplication] object.
 *
 * It is recommended for applications to use this directly and for libraries it is recommended to
 * create a library specific object based on [WinterApplication].
 *
 * Example:
 *
 * ```
 * // configure application component
 * Winter.component {
 *   // ... dependency declaration
 * }
 * // install RxJava 2 disposable plugin
 * Winter.installDisposablePlugin()
 * // configure injection adapter.
 * Winter.useAndroidPresentationScopeInjectionAdapter()
 * // create dependency graph
 * Winter.createGraph(myApplicationInstance)
 * ```
 *
 * @see WinterApplication for more details.
 */
object Winter : WinterApplication()
