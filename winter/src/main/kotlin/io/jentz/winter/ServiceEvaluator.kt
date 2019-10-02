package io.jentz.winter

private const val STACK_CAPACITY = 3 * 32

private const val SERVICE_INDEX = 0
private const val ARGUMENT_INDEX = SERVICE_INDEX + 1
private const val INSTANCE_INDEX = SERVICE_INDEX + 2

internal class ServiceEvaluator(private val graph: Graph) {

    private val plugins = graph.application.plugins

    private var stack: Array<Any?> = arrayOfNulls(STACK_CAPACITY)

    private var stackSize = 0

    private var pendingDependenciesCount = 0

    fun <A, R : Any> evaluate(service: BoundService<A, R>, argument: A): R {
        val key = service.key

        // check if key is already on the stack
        for (i in 0 until stackSize step 3) {
            if (stack[i + INSTANCE_INDEX] == null && (stack[i] as BoundService<*, *>).key == key) {
                throw CyclicDependencyException("Cyclic dependency for key `$key`.")
            }
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
        } catch (e: EntryNotFoundException) {
            drainStack()
            throw DependencyResolutionException(
                "Error while resolving dependency with key: $key " +
                        "reason: could not find dependency with key ${e.key}",
                e
            )
        } catch (e: WinterException) {
            throw e
        } catch (t: Throwable) {
            drainStack()
            throw DependencyResolutionException(
                "Factory of dependency with key $key threw an exception on invocation.",
                t
            )
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

                val service = stack[serviceIndex] as BoundService<*, *>
                val argument = stack[argumentIndex]
                val instance = stack[instanceIndex]

                stack[serviceIndex] = null
                stack[argumentIndex] = null
                stack[instanceIndex] = null

                if (instance != null && argument != null) {
                    service.postConstruct(argument, instance)
                    plugins.runPostConstruct(graph, service.scope, argument, instance)
                }
            }
        } finally {
            pendingDependenciesCount = 0
            stackSize = 0
        }
    }

}
