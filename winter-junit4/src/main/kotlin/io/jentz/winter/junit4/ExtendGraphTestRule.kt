package io.jentz.winter.junit4

import io.jentz.winter.*
import io.jentz.winter.testing.injectWithReflection

/**
 * [Test rule][org.junit.rules.TestRule] that takes a component qualifier and a
 * [ComponentBuilderBlock] and extends the [Graph] with [componentQualifier] by applying the
 * [ComponentBuilderBlock].
 *
 * It also provides a reflection based [inject] method that injects into all property annotated with
 * [javax.inject.Inject] by using the [Graph] with given [componentQualifier].
 *
 * Example:
 * ```
 * @get:Rule
 * val rule = WinterJUnit4.rule("presentation") {
 *   singleton<ListPresenter>(override = true) { MockListPresenter() }
 * }
 *
 * @Inject
 * lateinit var presenter: ListPresenter
 *
 *
 * @Before
 * fun beforeEach() {
 *   rule.inject(this)
 * }
 * ```
 *
 */
class ExtendGraphTestRule internal constructor(
    private val componentQualifier: Any,
    application: WinterApplication = Winter,
    private val componentBuilderBlock: ComponentBuilderBlock
) : GraphLifecycleTestRule(application) {

    var graph: Graph? = null
        private set

    override fun graphInitializing(parentGraph: Graph?, builder: ComponentBuilder) {
        if (builder.qualifier == componentQualifier) {
            componentBuilderBlock.invoke(builder)
        }
    }

    override fun graphInitialized(graph: Graph) {
        if (graph.component.qualifier == componentQualifier) {
            this.graph = graph
        }
    }

    override fun graphDispose(graph: Graph) {
        if (graph.component.qualifier == componentQualifier) {
            this.graph = null
        }
    }

    fun inject(target: Any) {
        val graph = graph ?: throw IllegalStateException("Graph is not present.")
        graph.injectWithReflection(target)
    }

}
