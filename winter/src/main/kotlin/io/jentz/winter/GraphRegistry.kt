package io.jentz.winter

/**
 * An object version of [WinterTree] that uses [Winter] as its [WinterApplication].
 *
 * Here an Android example where we use this to create a presentation graph that
 * survives configuration changes and an Activity graph that gets recreated every time.
 *
 * It is recommended to hide such details in a [WinterInjection.Adapter].
 *
 * Create the application object graph on application start:
 *
 * ```
 * class MyApplication : Application() {
 *   override fun onCreate() {
 *     super.onCreate()
 *
 *     // Register the application component
 *     Winter.component {
 *       // A presentation subcomponent that survives orientation changes
 *       subcomponent("presentation") {
 *         // The activity subcomponent that gets recreated with every device rotation
 *         subcomponent("activity") {
 *         }
 *       }
 *     }
 *
 *     // open the application graph
 *     GraphRegistry.open() {
 *       constant<Application> { this@MyApplication }
 *       constant<Context> { this@MyApplication }
 *     }
 *   }
 * }
 * ```
 *
 * Create the presenter and activity object subgraphs by their paths (of qualifiers):
 *
 * ```
 * class MyActivity : Activity() {
 *
 *   override fun onCreate(savedInstanceState: Bundle?) {
 *     // Open the presentation graph if not already open.
 *     // Since we could have multiple activity instances at the same time we use the Activity class
 *     // as an identifier for the presentation graph.
 *     if (!GraphRegistry.has(javaClass)) GraphRegistry.open("presentation", identifier = javaClass)
 *
 *     // Open the activity graph.
 *     // Here the same but we use the instance as identifier for the activity graph.
 *     GraphRegistry.open(javaClass, "activity", identifier = this) {
 *       constant<Context>(this@MyActivity)
 *       constant<Activity>(this@MyActivity)
 *     }
 *     super.onCreate(savedInstanceState)
 *   }
 *
 *   override fun onDestroy() {
 *     // If we are leaving the scope of the activity then we close the "presentation" graph.
 *     if (isFinishing) {
 *       GraphRegistry.close(javaClass)
 *     // And if this is just recreating then we just close the "activity" graph.
 *     } else {
 *       GraphRegistry.close(javaClass, this)
 *     }
 *     super.onDestroy()
 *   }
 *
 * }
 * ```
 *
 * @see WinterTree
 */
object GraphRegistry : WinterTree(Winter)
