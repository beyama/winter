package io.jentz.winter.evaluator

import io.jentz.winter.BoundService
import io.jentz.winter.Graph

/**
 * The simplest [ServiceEvaluator] which only calls [BoundService.newInstance] and handles
 * exceptions.
 *
 * This is used when no plugin is registered and no service requires lifecycle callbacks and cyclic
 * dependency checks are disabled.
 */
internal class DirectServiceEvaluator : ServiceEvaluator {

    override fun <R : Any> evaluate(service: BoundService<R>, graph: Graph): R {
        return try {
            service.newInstance(graph)
        } catch (t: Throwable) {
            handleException(service.key, t)
        }
    }

}
