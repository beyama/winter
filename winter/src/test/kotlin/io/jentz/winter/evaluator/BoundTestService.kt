package io.jentz.winter.evaluator

import io.jentz.winter.BoundService
import io.jentz.winter.Scope
import io.jentz.winter.TypeKey
import io.jentz.winter.typeKey

internal class BoundTestService(
    private val evaluator: ServiceEvaluator,
    override val key: TypeKey<String> = typeKey(),
    var dependency: BoundService<String>? = null,
    var throwOnNewInstance: (() -> Throwable)? = null,
    var instance: () -> String = { "" }
) : BoundService<String> {

    var postConstructCalled = mutableListOf<String>()

    override val scope: Scope get() = Scope.Prototype

    override fun instance(): String = throw Error()

    override fun newInstance(): String {
        dependency?.let { evaluator.evaluate(it) }
        throwOnNewInstance?.let { throw it() }
        return this.instance.invoke()
    }

    override fun postConstruct(instance: String) {
        postConstructCalled.add(instance)
    }

    override fun dispose() {
        throw Error()
    }
}
