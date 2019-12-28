package io.jentz.winter.testing

import io.jentz.winter.*
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

    class Builder {
        var application: WinterApplication = Winter

        private var autoCloseMode: AutoCloseMode = AutoCloseMode.NoAutoClose

        private var testGraphComponentMatcher =
            ComponentMatcher(null, APPLICATION_COMPONENT_QUALIFIER)

        private var bindAllMocksMatcher: ComponentMatcher? = null

        private val graphExtenders = mutableListOf<Pair<ComponentMatcher, ComponentBuilderBlock>>()

        private val onGraphInitializedCallbacks =
            mutableListOf<Pair<ComponentMatcher, OnGraphInitializedCallback>>()

        private val onGraphCloseCallbacks =
            mutableListOf<Pair<ComponentMatcher, OnGraphCloseCallback>>()

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
         * Extend the graph with the component [qualifier] and the parent graph component qualifier
         * [parentQualifier] with the given [block].
         *
         * @param parentQualifier The qualifier of the parent graph component.
         * @param qualifier The qualifier of the graph component.
         * @param block The block to apply to the graph component builder.
         */
        fun extend(parentQualifier: Any, qualifier: Any, block: ComponentBuilderBlock) {
            graphExtenders += ComponentMatcher(parentQualifier, qualifier) to block
        }

        /**
         * Extend the graph with the component qualifier [qualifier] with the given [block].
         *
         * @param qualifier The qualifier of the graph component.
         * @param block The block to apply to the graph component builder.
         */
        fun extend(qualifier: Any = APPLICATION_COMPONENT_QUALIFIER, block: ComponentBuilderBlock) {
            graphExtenders += ComponentMatcher(null, qualifier) to block
        }

        /**
         * Add callback that gets invoked when a graph with the component [qualifier] and the
         * parent graph component qualifier [parentQualifier] got created.
         *
         * @param parentQualifier The qualifier of the parent graph component.
         * @param qualifier The qualifier of the graph component.
         * @param callback The callback that gets invoked with the graph.
         */
        fun onGraphInitialized(
            parentQualifier: Any,
            qualifier: Any,
            callback: OnGraphInitializedCallback
        ) {
            onGraphInitializedCallbacks += ComponentMatcher(parentQualifier, qualifier) to callback
        }

        /**
         * Add callback that gets invoked when a graph with the component [qualifier] got created.
         *
         * @param qualifier The qualifier of the graph component.
         * @param callback The callback that gets invoked with the graph.
         */
        fun onGraphInitialized(
            qualifier: Any = APPLICATION_COMPONENT_QUALIFIER,
            callback: OnGraphInitializedCallback
        ) {
            onGraphInitializedCallbacks += ComponentMatcher(null, qualifier) to callback
        }

        /**
         * Add callback that gets invoked when a graph with the component [qualifier] and the
         * parent graph component qualifier [parentQualifier] gets closed.
         *
         * @param parentQualifier The qualifier of the parent graph component.
         * @param qualifier The qualifier of the graph component.
         * @param callback The callback that gets invoked with the graph.
         */
        fun onGraphClose(
            parentQualifier: Any,
            qualifier: Any,
            callback: OnGraphCloseCallback
        ) {
            onGraphCloseCallbacks += ComponentMatcher(parentQualifier, qualifier) to callback
        }

        /**
         * Add callback that gets invoked when a graph with the component [qualifier] gets closed.
         *
         * @param qualifier The qualifier of the graph component.
         * @param callback The callback that gets invoked with the graph.
         */
        fun onGraphClose(
            qualifier: Any = APPLICATION_COMPONENT_QUALIFIER,
            callback: OnGraphCloseCallback
        ) {
            onGraphCloseCallbacks += ComponentMatcher(null, qualifier) to callback
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
