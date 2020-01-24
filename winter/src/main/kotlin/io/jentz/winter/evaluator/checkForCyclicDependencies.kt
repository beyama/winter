package io.jentz.winter.evaluator

import io.jentz.winter.*

internal inline fun checkForCyclicDependencies(
    key: TypeKey<*>,
    check: () -> Boolean,
    buildPendingKeysList: () -> List<TypeKey<*>>
) {

    if (!check()) return

    val pendingKeysList = buildPendingKeysList()

    val index = pendingKeysList.indexOf(key)
    when {
        index == 0 && pendingKeysList.size == 1 -> {
            throw CyclicDependencyException(
                key,
                "Cyclic dependency found: `$key` is directly dependent of itself.\n" +
                        "Dependency chain: $key => $key")
        }
        index > -1 -> {
            val chain = pendingKeysList.listIterator(index)
                .asSequence()
                .joinToString(separator = " -> ", postfix = " => $key")

            throw CyclicDependencyException(
                key,
                "Cyclic dependency found: `$key` is dependent of itself.\n" +
                        "Dependency chain: $chain")
        }
    }

}

internal fun handleException(key: TypeKey<*>, t: Throwable): Nothing {
    when (t) {
        is EntryNotFoundException -> {
            throw DependencyResolutionException(
                key,
                "Error while resolving dependency with key: $key " +
                        "reason: could not find dependency with key ${t.key}",
                t
            )
        }
        is WinterException -> {
            throw t
        }
        else -> {
            throw DependencyResolutionException(
                key, "Factory of dependency with key $key threw an exception on invocation.", t
            )
        }
    }
}
