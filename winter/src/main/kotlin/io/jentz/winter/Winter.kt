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
 * // install a Winter plugin
 * Winter.installMyPlugin()
 * // open the application dependency graph
 * Winter.openGraph()
 * ```
 *
 * @see WinterApplication for more details.
 */
object Winter : WinterApplication()
