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

    override fun <R : Any> evaluate(service: BoundService<R>): R {
        return try {
            service.newInstance()
        } catch (t: Throwable) {
            handleException(service.key, t)
        }
    }

}
