package io.jentz.winter

import io.jentz.winter.plugin.Plugins

internal class ServiceEvaluator(
    private val graph: Graph,
    private val plugins: Plugins
) {

    private var isRequestPending = false

    // collection of all services with postConstructCallbacks and their instances
    private var postConstructCallbacks: MutableList<Any>? = null

    // type key of a detected cyclic dependency
    private var cyclicDependencyKey: TypeKey<*>? = null

    // list where we collect all pending keys from the stack down to the cyclic key
    private var cyclicDependenciesChain: MutableList<TypeKey<*>>? = null

    fun <R : Any> evaluate(service: BoundService<R>, graph: Graph): R {
        val key = service.key

        val isInitialServiceRequest = !isRequestPending

        isRequestPending = true

        // cyclic dependency detected
        if (service.isPending) {
            service.isPending = false
            cyclicDependencyKey = key
            cyclicDependenciesChain = mutableListOf()
            // unwind the stack until we are back to where this key was requested the first time
            throw CyclicDependencyException(key, "internal")
        }

        service.isPending = true

        try {
            val instance = service.newInstance(graph)

            if (service.requiresPostConstructCallback) {
                // collect services with their instances & graphs until we have no pending requests
                postConstructCallbacks = (postConstructCallbacks ?: mutableListOf()).apply {
                    add(service)
                    add(instance)
                    add(graph)
                }
            }

            plugins.forEach { it.postConstruct(this.graph, service.scope, instance) }

            return instance
        } catch (e: CyclicDependencyException) {
            handleCyclicDependencyException(key, e)
        } catch (e: EntryNotFoundException) {
            throw DependencyResolutionException(
                key,
                "Error while resolving dependency with key: $key " +
                        "reason: could not find dependency with key ${e.key}",
                e
            )
        } catch (e: WinterException) {
            throw e
        } catch (t: Throwable) {
            throw DependencyResolutionException(
                key, "Factory of dependency with key $key threw an exception on invocation.", t
            )
        } finally {
            service.isPending = false

            if (isInitialServiceRequest) {
                isRequestPending = false
                callPostConstructCallbacks()
            }
        }

    }

    private fun callPostConstructCallbacks() {
        val services = postConstructCallbacks ?: return
        postConstructCallbacks = null

        for (graphIndex in services.lastIndex downTo 0 step 3) {
            val instanceIndex = graphIndex - 1
            val serviceIndex = instanceIndex - 1

            @Suppress("UNCHECKED_CAST")
            val service = services[serviceIndex] as BoundService<Any>
            val instance = services[instanceIndex]
            val graph = services[graphIndex] as Graph

            service.onPostConstruct(graph, instance)
        }
    }

    private fun handleCyclicDependencyException(
        key: TypeKey<*>,
        e: CyclicDependencyException
    ): Nothing {
        // done unwinding stack to collect pending keys -> rethrow
        if (cyclicDependencyKey == null) {
            throw e
        }

        val chain = cyclicDependenciesChain!!

        chain.add(e.key)

        // not done unwinding the stack to the cyclic dependency
        if (cyclicDependencyKey != key) {
            throw CyclicDependencyException(key, "internal")
        }

        // we are done unwinding the stack so we reset this
        cyclicDependencyKey = null
        cyclicDependenciesChain = null

        if (chain.size == 1) {
            throw CyclicDependencyException(
                key,
                "Cyclic dependency found: `$key` is directly dependent of itself.\n" +
                        "Dependency chain: $key => $key"
            )
        }

        // add cyclic key
        chain.add(key)
        // remove cyclic key at the beginning for formatting purpose
        chain.removeAt(0)

        val dependencyChain = chain
            .reversed()
            .joinToString(separator = " -> ", postfix = " => $key")

        throw CyclicDependencyException(
            key, "Cyclic dependency found: `$key` is dependent of itself.\n" +
                    "Dependency chain: $dependencyChain"
        )
    }

}
