package io.jentz.winter.evaluator

import io.jentz.winter.BoundService
import io.jentz.winter.Graph
import io.jentz.winter.TypeKey

/**
 * Like [DirectServiceEvaluator] but it also checks for cyclic dependencies.
 *
 * This is used when no plugin is registered and no service requires lifecycle callbacks and cyclic
 * dependency checks are enabled.
 */
internal class CyclicDependenciesCheckingDirectServiceEvaluator : ServiceEvaluator {

    private var stack = mutableListOf<TypeKey<*>>()

    override fun <R : Any> evaluate(service: BoundService<R>, graph: Graph): R {
        val key = service.key

        checkForCyclicDependencies(key, { stack.contains(key) }, { stack })

        stack.push(key)

        return try {
            service.newInstance(graph)
        } catch (t: Throwable) {
            handleException(key, t)
        } finally {
            stack.pop()
        }

    }

    private fun MutableList<TypeKey<*>>.push(key: TypeKey<*>) = add(key)

    private fun MutableList<TypeKey<*>>.pop() = removeAt(lastIndex)

}
