package io.jentz.winter.evaluator

import io.jentz.winter.BoundService
import io.jentz.winter.Graph

/**
 * Service evaluators are used to call [BoundService.newInstance] to retrieve a new instance from
 * a service.
 *
 * They are responsible for handling exceptions that may be thrown by the [BoundService],
 * calling plugin and service lifecycle methods and maybe performing cyclic dependency checks.
 */
internal interface ServiceEvaluator {
    fun <R : Any> evaluate(service: BoundService<R>, graph: Graph): R
}
