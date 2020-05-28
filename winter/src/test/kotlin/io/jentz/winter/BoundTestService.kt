package io.jentz.winter

internal class BoundTestService(
    private val evaluator: ServiceEvaluator,
    override val key: TypeKey<String> = typeKey(),
    var dependency: BoundService<String>? = null,
    var throwOnNewInstance: (() -> Throwable)? = null,
    var instance: () -> String = { "" }
) : BoundService<String>(), UnboundService<String> {

    var postConstructCalled = mutableListOf<String>()

    override val unboundService: UnboundService<String>
        get() = this

    override val scope: Scope get() = Scope.Prototype

    override val requiresPostConstructCallback: Boolean
        get() = true

    override fun bind(graph: Graph): BoundService<String> = this

    override fun instance(block: ComponentBuilderBlock?): String = throw Error()

    override fun newInstance(graph: Graph): String {
        dependency?.let { evaluator.evaluate(it, graph) }
        throwOnNewInstance?.let { throw it() }
        return this.instance.invoke()
    }

    override fun onPostConstruct(instance: String) {
        postConstructCalled.add(instance)
    }

    override fun onClose() {
        throw Error()
    }
}
