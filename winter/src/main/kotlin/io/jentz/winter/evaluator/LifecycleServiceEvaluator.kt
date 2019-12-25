package io.jentz.winter.evaluator

import io.jentz.winter.BoundService
import io.jentz.winter.Graph
import io.jentz.winter.TypeKey
import io.jentz.winter.plugin.Plugins

private const val STACK_CAPACITY = 3 * 32

private const val SERVICE_INDEX = 0
private const val ARGUMENT_INDEX = SERVICE_INDEX + 1
private const val INSTANCE_INDEX = SERVICE_INDEX + 2

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

    private var pendingDependenciesCount = 0

    override fun <A, R : Any> evaluate(service: BoundService<A, R>, argument: A): R {
        val key = service.key

        if (checkForCyclicDependencies) {
            checkForCyclicDependencies(key, { isKeyPending(key) }, { pendingKeys() })
        }

        val serviceIndex = stackSize
        val argumentIndex = serviceIndex + ARGUMENT_INDEX
        val instanceIndex = serviceIndex + INSTANCE_INDEX

        if (stack.lastIndex < instanceIndex) {
            val oldStack = stack
            stack = arrayOfNulls(oldStack.size + STACK_CAPACITY)
            oldStack.copyInto(stack, endIndex = oldStack.size)
        }

        try {
            // push service
            stack[serviceIndex] = service
            // push argument
            stack[argumentIndex] = argument
            // nullify slot for instance
            stack[instanceIndex] = null

            pendingDependenciesCount += 1

            stackSize += 3

            // create instance and add it to stack
            val instance = service.newInstance(argument)
            stack[instanceIndex] = instance
            return instance
        } catch (t: Throwable) {
            handleException(key, t)
        } finally {
            pendingDependenciesCount -= 1

            if (pendingDependenciesCount == 0) {
                drainStack()
            }
        }
    }

    private fun drainStack() {
        try {
            for (i in stackSize - 1 downTo 0 step 3) {
                val serviceIndex = i - 2
                val argumentIndex = serviceIndex + ARGUMENT_INDEX
                val instanceIndex = serviceIndex + INSTANCE_INDEX

                @Suppress("UNCHECKED_CAST")
                val service = stack[serviceIndex] as BoundService<Any, Any>
                val argument = stack[argumentIndex]
                val instance = stack[instanceIndex]

                stack[serviceIndex] = null
                stack[argumentIndex] = null
                stack[instanceIndex] = null

                if (instance != null && argument != null) {
                    service.postConstruct(argument, instance)
                    plugins.forEach { it.postConstruct(graph, service.scope, argument, instance) }
                }
            }
        } finally {
            pendingDependenciesCount = 0
            stackSize = 0
        }
    }

    private fun isKeyPending(key: TypeKey<*, *>): Boolean {
        for (i in 0 until stackSize step 3) {
            if (stack[i + INSTANCE_INDEX] == null && (stack[i] as BoundService<*, *>).key == key) {
                return true
            }
        }
        return false
    }

    private fun pendingKeys(): List<TypeKey<*, *>> = (0 until stackSize step 3).mapNotNull { i ->
        if (stack[i + INSTANCE_INDEX] == null) (stack[i] as BoundService<*, *>).key else null
    }

}
