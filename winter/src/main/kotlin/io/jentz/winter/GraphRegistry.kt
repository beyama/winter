package io.jentz.winter

/**
 * The graph registry creates and holds dependency graphs in a tree (directed acyclic graph).
 *
 * For example consider the following application component of a basic Android application:
 *
 * ```
 * GraphRegistry.component = component { // the application component
 *   // A presentation subcomponent that survives orientation changes
 *   subcomponent("presentation") {
 *     // The activity subcomponent that gets recreated with every device rotation
 *     subcomponent("activity") {
 *     }
 *   }
 * }
 * ```
 *
 * Create the application dependency graph on application start:
 *
 * ```
 * class MyApplication : Application() {
 *   override fun onCreate() {
 *     super.onCreate()
 *
 *     GraphRegistry.open() {
 *       constant<Application> { this@MyApplication }
 *       constant<Context> { this@MyApplication }
 *     }
 *   }
 * }
 * ```
 *
 * Create the presenter and activity dependency graph by its path (of qualifiers):
 *
 * ```
 * class MyActivity : Activity() {
 *
 *   override fun onCreate(savedInstanceState: Bundle?) {
 *     // open presentation graph if not already open
 *     if (!GraphRegistry.has("presentation")) GraphRegistry.open("presentation")
 *     // open activity graph
 *     GraphRegistry.open("presentation", "activity") {
 *       constant<Activity>(theActivityInstance)
 *     }
 *     super.onCreate(savedInstanceState)
 *   }
 *
 *   override fun onDestroy() {
 *     // if we are leaving the scope of the activity then we close "presentation"
 *     if (isFinishing) {
 *       GraphRegistry.close("presentation")
 *     // and if this is just recreating then we just close "activity"
 *     } else {
 *       GraphRegistry.close("presentation", "activity")
 *     }
 *     super.onDestroy()
 *   }
 *
 * }
 * ```
 *
 * If you need multiple instances of the same subcomponent you can pass an `identifier` parameter
 * to the open method to register the graph instance under a different `identifier` than its
 * component qualifier:
 *
 * ```
 * GraphRegistry.open("presentation", "activity", identifier: theActivityInstance) {
 *   constant<Activity>(theActivityInstance)
 * }
 * ```
 *
 * Close the activity graph:
 *
 * ```
 * GraphRegistry.close("presentation", "activity")
 * ```
 *
 * Close the activity graph that was created with an identifier:
 *
 * ```
 * GraphRegistry.close("presentation", theActivityInstance)
 * ```
 *
 * [GraphRegistry.close] will remove and dispose all child dependency graphs from the registry.
 * So in our example above the call:
 *
 * ```
 * GraphRegistry.close("presentation")
 * ```
 *
 * will also close all activity dependency graphs.
 *
 * To get an instance of a dependency graph use [get][GraphRegistry.get]:
 * ```
 * GraphRegistry.get()               // Get the root dependency graph
 * GraphRegistry.get("presentation") // Get the presentation dependency graph
 * ```
 *
 * @see WinterTree
 */
object GraphRegistry : WinterTree()
