package io.jentz.winter.evaluator

import io.jentz.winter.BoundService
import io.jentz.winter.Scope
import io.jentz.winter.TypeKey
import io.jentz.winter.typeKey

internal class BoundTestService(
    private val evaluator: ServiceEvaluator,
    override val key: TypeKey = typeKey<String>(),
    var dependency: BoundService<String, String>? = null,
    var throwOnNewInstance: ((String) -> Throwable)? = null,
    var instance: (String) -> String = { it }
) : BoundService<String, String> {

    var postConstructCalled = mutableListOf<Pair<Any, Any>>()

    override val scope: Scope get() = Scope.Prototype

    override fun instance(argument: String): String = throw Error()

    override fun newInstance(argument: String): String {
        dependency?.let { evaluator.evaluate(it, argument) }
        throwOnNewInstance?.let { throw it(argument) }
        return this.instance.invoke(argument)
    }

    override fun postConstruct(arg: Any, instance: Any) {
        postConstructCalled.add(arg to instance)
    }

    override fun dispose() {
        throw Error()
    }
}
