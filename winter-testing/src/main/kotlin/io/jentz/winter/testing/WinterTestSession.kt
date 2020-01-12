package io.jentz.winter.testing

import io.jentz.winter.*
import io.jentz.winter.inject.ApplicationScope
import io.jentz.winter.plugin.SimplePlugin

typealias WinterTestSessionBlock = WinterTestSession.Builder.() -> Unit

typealias OnGraphInitializedCallback = (Graph) -> Unit
typealias OnGraphCloseCallback = (Graph) -> Unit

/**
 * [WinterTestSession] helps to write cleaner tests that require `Winter` by eliminating
 * boilerplate code.
 *
 * This is used by the Winter JUnit4 and JUnit5 modules but can also be used directly with other
 * test frameworks.
 *
 * Example:
 *
 * ```
 * class ExampleTest {
 *
 *   private lateinit val session: WinterTestSession
 *
 *   @Mock service: Service = mock()
 *
 *   @Before fun setup() {
 *     session = WinterTestSession.session(this) {
 *       application = MyWinterApp
 *       bindAllMocks()
 *       testGraph("activity")
 *     }
 *     session.start()
 *   }
 *
 *   @After fun teardown() {
 *     session.stop()
 *   }
 *
 * }
 * ```
 *
 * @see WinterTestSession.Builder for more details about the possible configurations.
 */
@Suppress("MaxLineLength") // maybe I should get rid of Detekt...
class WinterTestSession private constructor(
    private val application: WinterApplication,
    private val testInstances: List<Any>,
    private val graphExtenders: List<Pair<ComponentMatcher, ComponentBuilderBlock>>,
    private val onGraphInitializedCallbacks: List<Pair<ComponentMatcher, OnGraphInitializedCallback>>,
    private val onGraphCloseCallbacks: List<Pair<ComponentMatcher, OnGraphCloseCallback>>,
    private val testGraphComponentMatcher: ComponentMatcher,
    private val autoCloseMode: AutoCloseMode,
    private val bindAllMocksMatcher: ComponentMatcher?
) {

    /**
     * The test graph if open otherwise null.
     */
    var testGraph: Graph? = null
        private set

    /**
     * The test graph.
     *
     * @throws IllegalStateException If graph is not open.
     */
    val requireTestGraph: Graph
        get() = checkNotNull(testGraph) {
            "Test graph is not open."
        }

    private val _allGraphs = mutableListOf<Graph>()

    /**
     * A list of all graphs that where opened after calling [start].
     */
    val allGraphs: List<Graph> get() = _allGraphs.toList()

    private val plugin = object : SimplePlugin() {

        override fun graphInitializing(parentGraph: Graph?, builder: Component.Builder) {
            for ((matcher, block) in graphExtenders) {
                if (matcher.matches(builder)) {
                    builder.apply(block)
                }
            }
            if (bindAllMocksMatcher?.matches(builder) == true) {
                testInstances.forEach { builder.bindAllMocks(it) }
            }
        }

        override fun graphInitialized(graph: Graph) {
            _allGraphs += graph

            if (testGraphComponentMatcher.matches(graph)) {
                this@WinterTestSession.testGraph = graph
                testInstances.forEach { graph.injectWithReflection(it) }
            }

            for ((matcher, callback) in onGraphInitializedCallbacks) {
                if (matcher.matches(graph)) {
                    callback(graph)
                }
            }
        }

        override fun graphClose(graph: Graph) {
            _allGraphs -= graph

            for ((matcher, callback) in onGraphCloseCallbacks) {
                if (matcher.matches(graph)) {
                    callback(graph)
                }
            }

            if (testGraph == graph) {
                testGraph = null
            }
        }
    }

    /**
     * Call this to start the session.
     */
    fun start() {
        application.plugins += plugin
    }

    /**
     * Call this to stop the session.
     */
    fun stop() {
        application.plugins -= plugin

        testGraph?.let { graph ->
            this.testGraph = null

            if (graph.isClosed) {
                return
            }

            when (autoCloseMode) {
                AutoCloseMode.NoAutoClose -> {
                }
                AutoCloseMode.Graph -> {
                    graph.close()
                }
                AutoCloseMode.GraphAndAncestors -> {
                    var graphToClose: Graph? = graph
                    while (graphToClose != null && !graphToClose.isClosed) {
                        val parent = graphToClose.parent
                        graphToClose.close()
                        graphToClose = parent
                    }
                }
                AutoCloseMode.AllGraphs -> {
                    allGraphs.forEach(Graph::close)
                }
            }
        }
    }

    /**
     * Resolve an instance of [type] with optional [qualifier].
     *
     * @param type The Java type.
     * @param qualifier The optional qualifier.
     *
     * @return The requested type or null if not found.
     */
    fun resolve(type: Class<*>, qualifier: Any? = null): Any =
        requireTestGraph.instanceByKey(ClassTypeKey(type.kotlin.javaObjectType, qualifier))

    internal enum class AutoCloseMode { NoAutoClose, Graph, GraphAndAncestors, AllGraphs }

    internal class ComponentMatcher(private val qualifier: Any) {

        fun matches(graph: Graph): Boolean = qualifier == graph.component.qualifier

        fun matches(builder: Component.Builder): Boolean = qualifier == builder.qualifier
    }

    class Builder {
        var application: WinterApplication = Winter

        private var autoCloseMode: AutoCloseMode = AutoCloseMode.NoAutoClose

        private var testGraphComponentMatcher = ComponentMatcher(ApplicationScope::class)

        private var bindAllMocksMatcher: ComponentMatcher? = null

        private val graphExtenders = mutableListOf<Pair<ComponentMatcher, ComponentBuilderBlock>>()

        private val onGraphInitializedCallbacks =
            mutableListOf<Pair<ComponentMatcher, OnGraphInitializedCallback>>()

        private val onGraphCloseCallbacks =
            mutableListOf<Pair<ComponentMatcher, OnGraphCloseCallback>>()

        /**
         * Use the graph with component [qualifier] as test graph.
         *
         * Default: Uses the application graph.
         */
        fun testGraph(qualifier: Any) {
            testGraphComponentMatcher = ComponentMatcher(qualifier)
        }

        /**
         * Auto-close the test graph when [stop] is called.
         *
         * Default: No auto-close.
         */
        fun autoCloseTestGraph() {
            autoCloseMode = AutoCloseMode.Graph
        }

        /**
         * Auto-close the test graph and all its ancestors when [stop] is called.
         *
         * Default: No auto-close.
         */
        fun autoCloseTestGraphAndAncestors() {
            autoCloseMode = AutoCloseMode.GraphAndAncestors
        }

        /**
         * Auto-close all graphs that where opened after [start] was called when [stop] is
         * called.
         *
         * Default: No auto-close.
         */
        fun autoCloseAllGraphs() {
            autoCloseMode = AutoCloseMode.AllGraphs
        }

        /**
         * Extend the graph with the component qualifier [qualifier] with the given [block].
         *
         * @param qualifier The qualifier of the graph component.
         * @param block The block to apply to the graph component builder.
         */
        fun extend(qualifier: Any = ApplicationScope::class, block: ComponentBuilderBlock) {
            graphExtenders += ComponentMatcher(qualifier) to block
        }

        /**
         * Add callback that gets invoked when a graph with the component [qualifier] got created.
         *
         * @param qualifier The qualifier of the graph component.
         * @param callback The callback that gets invoked with the graph.
         */
        fun onGraphInitialized(
            qualifier: Any = ApplicationScope::class,
            callback: OnGraphInitializedCallback
        ) {
            onGraphInitializedCallbacks += ComponentMatcher(qualifier) to callback
        }

        /**
         * Add callback that gets invoked when a graph with the component [qualifier] gets closed.
         *
         * @param qualifier The qualifier of the graph component.
         * @param callback The callback that gets invoked with the graph.
         */
        fun onGraphClose(
            qualifier: Any = ApplicationScope::class,
            callback: OnGraphCloseCallback
        ) {
            onGraphCloseCallbacks += ComponentMatcher(qualifier) to callback
        }

        /**
         * Automatically bind all mocks found in the test instances to the graph with given
         * component [qualifier].
         *
         * @param qualifier The qualifier of the graph component.
         */
        fun bindAllMocks(qualifier: Any = ApplicationScope::class) {
            bindAllMocksMatcher = ComponentMatcher(qualifier)
        }

        fun build(testInstances: List<Any>) = WinterTestSession(
            application = application,
            testInstances = testInstances,
            graphExtenders = graphExtenders,
            onGraphInitializedCallbacks = onGraphInitializedCallbacks,
            onGraphCloseCallbacks = onGraphCloseCallbacks,
            testGraphComponentMatcher = testGraphComponentMatcher,
            autoCloseMode = autoCloseMode,
            bindAllMocksMatcher = bindAllMocksMatcher
        )

    }

    companion object {

        fun session(
            vararg testInstances: Any,
            block: WinterTestSessionBlock
        ): WinterTestSession = Builder().apply(block).build(testInstances.toList())

    }

}
