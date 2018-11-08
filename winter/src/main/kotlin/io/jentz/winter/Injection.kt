package io.jentz.winter

import io.jentz.winter.WinterInjection.Adapter

/**
 * Abstraction to create, get and dispose a dependency graph from a class that can't make use of
 * constructor injection. This takes the burden off of the class to know how exactly a graph
 * or parent graph is stored and how to create and store a new graph.
 *
 * An application specific graph creation and retrieval strategy can be provided by setting a
 * custom [Adapter].
 *
 * ```
 * Injection.adapter = MyCustomAdapter()
 * ```
 *
 * To use this abstraction in a library it is recommended to create a library specific object
 * from [WinterInjection].
 *
 * Example using the SimpleAndroidInjectionAdapter which is part of the winter-android module:
 *
 * ```
 * class MyApplication : Application() {
 *   override fun onCreate() {
 *     super.onCreate()
 *
 *     // register component
 *     Winter.component {
 *       singleton<GitHubApi> { GitHubApiImpl() }
 *
 *       singleton { RepoListViewModel(instance()) }
 *
 *       subcomponent("activity") {
 *          singleton { Glide.with(instance<Activity>()) }
 *       }
 *     }
 *
 *     // register adapter
 *     Injection.useSimpleAndroidAdapter()
 *     // create root graph
 *     Injection.createGraph(this)
 *   }
 * }
 *
 * class MyActivity : Activity() {
 *   private val injector = Injector()
 *   private val viewModel: RepoListViewModel by injector.instance()
 *
 *   override fun onCreate(savedInstanceState: Bundle?) {
 *     Injection.createGraphAndInject(this, injector)
 *     super.onCreate(savedInstanceState)
 *   }
 *
 *   override fun onDestroy() {
 *     Injection.disposeGraph(this)
 *     super.onDestroy()
 *   }
 *
 * }
 * ```
 *
 * @see WinterInjection
 */
object Injection : WinterInjection()
