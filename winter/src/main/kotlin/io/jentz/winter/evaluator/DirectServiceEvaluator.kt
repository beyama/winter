package io.jentz.winter.evaluator

import io.jentz.winter.BoundService

/**
 * The simplest [ServiceEvaluator] which only calls [BoundService.newInstance] and handles
 * exceptions.
 *
 * This is used when no plugin is registered and no service requires lifecycle callbacks and cyclic
 * dependency checks are disabled.
 */
internal class DirectServiceEvaluator : ServiceEvaluator {

    override fun <A, R : Any> evaluate(service: BoundService<A, R>, argument: A): R {
        return try {
            service.newInstance(argument)
        } catch (t: Throwable) {
            handleException(service.key, t)
        }
    }

}
