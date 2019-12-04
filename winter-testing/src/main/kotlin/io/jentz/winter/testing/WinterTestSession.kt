package io.jentz.winter.testing

import io.jentz.winter.*
import io.jentz.winter.plugin.SimplePlugin

typealias WinterTestSessionBlock = WinterTestSession.Builder.() -> Unit

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
class WinterTestSession private constructor(
    private val application: WinterApplication,
    private val testInstances: List<Any>,
    private val graphExtenders: List<GraphExtender>,
    private val testGraphComponentMatcher: ComponentMatcher,
    private val autoDisposeMode: AutoDisposeMode,
    private val bindAllMocksMatcher: ComponentMatcher?
) {

    /**
     * The test graph if open otherwise null.
     */
    var graph: Graph? = null
        private set

    /**
     * The test graph.
     *
     * @throws IllegalStateException If graph is not open.
     */
    val requireGraph: Graph
        get() = checkNotNull(graph) {
            "Test graph is not open."
        }

    private val _allGraphs = mutableListOf<Graph>()

    /**
     * A list of all graphs that where opened after calling [start].
     */
    val allGraphs: List<Graph> get() = _allGraphs.toList()

    private val plugin = object : SimplePlugin() {

        override fun graphInitializing(parentGraph: Graph?, builder: ComponentBuilder) {
            for ((matcher, block) in graphExtenders) {
                if (matcher.matches(parentGraph, builder)) {
                    builder.apply(block)
                }
            }
            if (bindAllMocksMatcher?.matches(parentGraph, builder) == true) {
                testInstances.forEach { builder.bindAllMocks(it) }
            }
        }

        override fun graphInitialized(graph: Graph) {
            _allGraphs += graph

            if (testGraphComponentMatcher.matches(graph)) {
                this@WinterTestSession.graph = graph
                testInstances.forEach { graph.injectWithReflection(it) }
            }
        }

    }

    /**
     * Call this to start the session.
     */
    fun start() {
        application.registerPlugin(plugin)
    }

    /**
     * Call this to stop the session.
     */
    fun stop() {
        application.unregisterPlugin(plugin)

        graph?.let { graph ->
            this.graph = null

            if (graph.isDisposed) {
                return
            }

            when (autoDisposeMode) {
                AutoDisposeMode.NoAutoDispose -> {
                }
                AutoDisposeMode.Graph -> {
                    graph.dispose()
                }
                AutoDisposeMode.GraphAndAncestors -> {
                    var graphToDispose: Graph? = graph
                    while (graphToDispose != null && !graphToDispose.isDisposed) {
                        val parent = graphToDispose.parent
                        graphToDispose.dispose()
                        graphToDispose = parent
                    }
                }
                AutoDisposeMode.AllGraphs -> {
                    _allGraphs.forEach(Graph::dispose)
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
        requireGraph.instanceByKey(ClassTypeKey(type, qualifier))

    internal enum class AutoDisposeMode { NoAutoDispose, Graph, GraphAndAncestors, AllGraphs }

    internal class ComponentMatcher(
        private val parentQualifier: Any?,
        private val qualifier: Any
    ) {

        private fun matches(parentQualifier: Any?, qualifier: Any): Boolean =
            this.parentQualifier != null
                    && this.parentQualifier == parentQualifier
                    && this.qualifier == qualifier
                    || this.parentQualifier == null
                    && this.qualifier == qualifier

        fun matches(graph: Graph): Boolean =
            matches(graph.parent?.component?.qualifier, graph.component.qualifier)

        fun matches(parentGraph: Graph?, builder: ComponentBuilder): Boolean =
            matches(parentGraph?.component?.qualifier, builder.qualifier)

    }

    internal data class GraphExtender(
        val componentMatcher: ComponentMatcher,
        val block: ComponentBuilderBlock
    )

    class Builder {
        var application: WinterApplication = Winter

        private var autoDisposeMode: AutoDisposeMode = AutoDisposeMode.NoAutoDispose

        private var testGraphComponentMatcher =
            ComponentMatcher(null, APPLICATION_COMPONENT_QUALIFIER)

        private var bindAllMocksMatcher: ComponentMatcher? = null

        private val graphExtenders = mutableListOf<GraphExtender>()

        /**
         * Use the graph with component [qualifier] and parent component qualifier [parentQualifier]
         * as test graph.
         *
         * Default: Uses the application graph.
         */
        fun testGraph(parentQualifier: Any, qualifier: Any) {
            testGraphComponentMatcher = ComponentMatcher(parentQualifier, qualifier)
        }

        /**
         * Use the graph with component [qualifier] as test graph.
         *
         * Default: Uses the application graph.
         */
        fun testGraph(qualifier: Any) {
            testGraphComponentMatcher = ComponentMatcher(null, qualifier)
        }

        /**
         * Auto-dispose the test graph when [stop] is called.
         *
         * Default: No auto-dispose.
         */
        fun autoDisposeTestGraph() {
            autoDisposeMode = AutoDisposeMode.Graph
        }

        /**
         * Auto-dispose the test graph and all its ancestors when [stop] is called.
         *
         * Default: No auto-dispose.
         */
        fun autoDisposeTestGraphAndAncestors() {
            autoDisposeMode = AutoDisposeMode.GraphAndAncestors
        }

        /**
         * Auto-dispose all graphs that where opened after [start] was called when [stop] is
         * called.
         *
         * Default: No auto-dispose.
         */
        fun autoDisposeAllGraphs() {
            autoDisposeMode = AutoDisposeMode.AllGraphs
        }

        /**
         * Extend the graph with the component [qualifier] and the parent graph component qualifier
         * [parentQualifier] with the given [block].
         *
         * @param parentQualifier The qualifier of the parent graph component.
         * @param qualifier The qualifier of the graph component.
         * @param block The block to apply to the graph component builder.
         */
        fun extend(parentQualifier: Any, qualifier: Any, block: ComponentBuilderBlock) {
            graphExtenders += GraphExtender(ComponentMatcher(parentQualifier, qualifier), block)
        }

        /**
         * Extend the graph with the component qualifier [qualifier] with the given [block].
         *
         * @param qualifier The qualifier of the graph component.
         * @param block The block to apply to the graph component builder.
         */
        fun extend(qualifier: Any = APPLICATION_COMPONENT_QUALIFIER, block: ComponentBuilderBlock) {
            graphExtenders += GraphExtender(ComponentMatcher(null, qualifier), block)
        }

        /**
         * Automatically bind all mocks found in the test instances to the graph with given
         * component [qualifier] and the parent graph component qualifier [parentQualifier].
         *
         * @param parentQualifier The qualifier of the parent graph component.
         * @param qualifier The qualifier of the graph component.
         */
        fun bindAllMocks(parentQualifier: Any, qualifier: Any) {
            bindAllMocksMatcher = ComponentMatcher(parentQualifier, qualifier)
        }

        /**
         * Automatically bind all mocks found in the test instances to the graph with given
         * component [qualifier].
         *
         * @param qualifier The qualifier of the graph component.
         */
        fun bindAllMocks(qualifier: Any = APPLICATION_COMPONENT_QUALIFIER) {
            bindAllMocksMatcher = ComponentMatcher(null, qualifier)
        }

        fun build(testInstances: List<Any>) = WinterTestSession(
            application = application,
            testInstances = testInstances,
            graphExtenders = graphExtenders,
            testGraphComponentMatcher = testGraphComponentMatcher,
            autoDisposeMode = autoDisposeMode,
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
