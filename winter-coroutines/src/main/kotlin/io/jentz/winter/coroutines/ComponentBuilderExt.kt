package io.jentz.winter.coroutines

import io.jentz.winter.Component
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Register a [CoroutineScope] on this component.
 *
 * If the give [context] doesn't contain a [Job] a [SupervisorJob] is added to it.
 * The [contexts][context] job gets canceled when the graph gets closed.
 */
fun Component.Builder.coroutineScope(context: CoroutineContext = EmptyCoroutineContext) {

    singleton(onClose = { it.coroutineContext[Job]?.cancel() }) {
        CoroutineScope(if (context[Job] != null) context else context + SupervisorJob())
    }

}
