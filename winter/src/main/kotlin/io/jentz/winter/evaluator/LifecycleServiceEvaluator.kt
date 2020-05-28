package io.jentz.winter.evaluator

import io.jentz.winter.BoundService
import io.jentz.winter.Graph
import io.jentz.winter.TypeKey
import io.jentz.winter.plugin.Plugins

private const val STACK_CAPACITY = 2 * 32

private const val INSTANCE_INDEX = 1

/**
 * A [ServiceEvaluator] implementation that can optionally check for cyclic dependencies and
 * that calls service and plugin lifecycle methods.
 *
 * This one is used when plugins are registered and/or a service needs lifecycle callbacks.
 */
internal class LifecycleServiceEvaluator(
    private val graph: Graph,
    private val plugins: Plugins,
    private val checkForCyclicDependencies: Boolean
) : ServiceEvaluator {

    private var stack = arrayOfNulls<Any>(STACK_CAPACITY)

    private var stackSize = 0

    private var isRequestPending = false

    private var currentRequestStartIndex = 0

    override fun <R : Any> evaluate(service: BoundService<R>, graph: Graph): R {
        val key = service.key

        if (checkForCyclicDependencies && isRequestPending) {
            checkForCyclicDependencies(key, { isKeyPending(currentRequestStartIndex, key) }, { pendingKeys() })
        }

        val serviceIndex = stackSize
        val instanceIndex = serviceIndex + INSTANCE_INDEX

        if (stack.lastIndex < instanceIndex) {
            val oldStack = stack
            stack = arrayOfNulls(oldStack.size + STACK_CAPACITY)
            oldStack.copyInto(stack, endIndex = oldStack.size)
        }

        // push service
        stack[serviceIndex] = service
        // nullify slot for instance
        stack[instanceIndex] = null

        stackSize += 2

        if (isRequestPending) {
            try {
                val instance = service.newInstance(graph)
                stack[instanceIndex] = instance
                return instance
            } catch (t: Throwable) {
                handleException(key, t)
            }
        }

        try {
            isRequestPending = true
            currentRequestStartIndex = serviceIndex
            val instance = service.newInstance(graph)
            stack[instanceIndex] = instance
            return instance
        } catch (t: Throwable) {
            handleException(key, t)
        } finally {
            isRequestPending = false
            drainSegment(serviceIndex, stackSize - 1)
        }
    }

    private fun drainSegment(start: Int, end: Int) {
        for (instanceIndex in end downTo start step 2) {
            val serviceIndex = instanceIndex - INSTANCE_INDEX

            @Suppress("UNCHECKED_CAST")
            val service = stack[serviceIndex] as BoundService<Any>
            val instance = stack[instanceIndex]

            stack[serviceIndex] = null
            stack[instanceIndex] = null

            if (instance != null) {
                service.onPostConstruct(instance)
                plugins.forEach { it.postConstruct(graph, service.scope, instance) }
            }
        }
    }

    private fun isKeyPending(startIndex: Int, key: TypeKey<*>): Boolean {
        for (i in startIndex until stackSize step 2) {
            if (stack[i + INSTANCE_INDEX] == null && (stack[i] as BoundService<*>).key == key) {
                return true
            }
        }
        return false
    }

    private fun pendingKeys(): List<TypeKey<*>> = (0 until stackSize step 2).mapNotNull { i ->
        if (stack[i + INSTANCE_INDEX] == null) (stack[i] as BoundService<*>).key else null
    }

}
