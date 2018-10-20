package io.jentz.winter

internal class DependenciesStack(private val graph: Graph) {

    private val stack: MutableList<Any?> = mutableListOf()

    private var stackSize = 0

    fun <A, R : Any> evaluate(service: BoundService<A, R>, argument: A): R {
        val key = service.key

        // check if key is already on the stack
        for (i in 0 until stack.size step 3) {
            if (stack[i + 2] == null && (stack[i] as BoundService<*, *>).key == key) {
                throw CyclicDependencyException("Cyclic dependency for key `$key`.")
            }
        }

        val serviceIndex = stack.size
        val argumentIndex = serviceIndex + 1
        val instanceIndex = argumentIndex + 1

        try {
            // push service
            stack.add(serviceIndex, service)
            // push argument
            stack.add(argumentIndex, argument)
            // add slot for instance
            stack.add(instanceIndex, null)
            stackSize += 1
            // create instance and add it to stack
            val instance = service.newInstance(argument)
            stack[instanceIndex] = instance
            return instance
        } catch (e: EntryNotFoundException) {
            drainStack()
            val stackInfo = stack.joinToString(" -> ")
            throw DependencyResolutionException(
                "Error while resolving dependencies of $key (dependency stack: $stackInfo)",
                e
            )
        } catch (e: WinterException) {
            throw e
        } catch (t: Throwable) {
            drainStack()
            val stackInfo = stack.joinToString(" -> ")
            throw DependencyResolutionException(
                "Error while invoking provider block of $key (dependency stack: $stackInfo)",
                t
            )
        } finally {
            // decrement stack size
            stackSize -= 1
            if (stackSize == 0) drainStack()
        }
    }

    private fun drainStack() {
        try {
            for (i in stack.size - 1 downTo 0 step 3) {
                val instance = stack[i]
                val argument = stack[i - 1]
                val service = stack[i - 2] as BoundService<*, *>

                if (instance != null && argument != null) {
                    service.postConstruct(argument, instance)
                    WinterPlugins.runPostConstructPlugins(graph, service.scope, argument, instance)
                }
            }
        } finally {
            stack.clear()
            stackSize = 0
        }
    }

}
