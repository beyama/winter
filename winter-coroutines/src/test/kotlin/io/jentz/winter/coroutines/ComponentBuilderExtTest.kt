package io.jentz.winter.coroutines

import io.jentz.winter.graph
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.junit.jupiter.api.Test

class ComponentBuilderExtTest {

    @Test
    fun `should add SupervisorJob to context if no job is provided`() {
        val scope: CoroutineScope = graph { coroutineScope() }.instance()
        scope.coroutineContext[Job].shouldBeInstanceOf<CompletableJob>()
    }

    @Test
    fun `should not add SupervisorJob to context if job is provided`() {
        val job = Job()
        val scope: CoroutineScope = graph { coroutineScope(job) }.instance()
        scope.coroutineContext[Job].shouldBeSameInstanceAs(job)
    }

    @Test
    fun `should cancel job when the graph gets closed`() {
        val graph = graph { coroutineScope() }
        val scope: CoroutineScope = graph.instance()
        scope.coroutineContext[Job]!!.isCancelled.shouldBeFalse()
        graph.close()
        scope.coroutineContext[Job]!!.isCancelled.shouldBeTrue()
    }

}
