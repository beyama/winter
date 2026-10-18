package io.jentz.winter

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GraphConcurrencyTest {

    private lateinit var executor: ExecutorService

    @BeforeAll
    fun beforeAll() {
        executor = Executors.newFixedThreadPool(8)
    }

    @AfterAll
    fun afterAll() {
        executor.shutdown()
    }

    @Test
    fun `prototype scope should be thread safe`() {
        val graph = graph {
            (0..100).forEach { value ->
                prototype(erased(qualifier(value.toString()))) { value }
            }
        }

        (0..100).map {
            executor.submit { graph.instance(erased<Int>(qualifier(it.toString()))).shouldBe(it) }
        }.forEach { it.get() }
    }

    @Test
    fun `singleton scope should be thread safe`() {
        val graph = graph {
            (0..100).forEach { value ->
                singleton(erased(qualifier(value.toString()))) { value }
            }
        }

        (0..100).map {
            executor.submit { graph.instance(erased<Int>(qualifier(it.toString()))).shouldBe(it) }
        }.forEach { it.get() }
    }

    @Test
    fun `should not deadlock when singletons with dependencies are resolved concurrently`() {
        // daemon threads so a deadlocked pair can't keep the test JVM alive
        val executor = Executors.newFixedThreadPool(2) { Thread(it).apply { isDaemon = true } }
        try {
            repeat(2000) { run ->
                val graph = graph {
                    singleton { Heater() }
                    singleton { Thermosiphon(instance()) }
                }
                val barrier = CyclicBarrier(2)
                val futures = listOf(
                    executor.submit { barrier.await(); graph.instance<Thermosiphon>() },
                    executor.submit { barrier.await(); graph.instance<Heater>() }
                )
                try {
                    futures.forEach { it.get(5, TimeUnit.SECONDS) }
                } catch (e: TimeoutException) {
                    throw AssertionError("Deadlock while resolving singletons in run $run", e)
                }
            }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `should resolve already bound services while the graph lock is held`() {
        val parent = graph {
            singleton { Heater() }
            subcomponent(QualifierSub) {
                singleton { Thermosiphon(instance()) }
            }
        }
        val child = parent.createSubgraph(QualifierSub)
        val heater = parent.instance<Heater>()
        val thermosiphon = child.instance<Thermosiphon>()

        val locked = CountDownLatch(2)
        val release = CountDownLatch(1)
        val lockHolders = listOf(parent, child).map { graph ->
            Thread {
                synchronized(graph) {
                    locked.countDown()
                    release.await()
                }
            }.apply { isDaemon = true; start() }
        }
        val executor = Executors.newSingleThreadExecutor { Thread(it).apply { isDaemon = true } }
        try {
            locked.await()
            val future = executor.submit<Pair<Heater, Thermosiphon>> {
                child.instance<Heater>() to child.instance<Thermosiphon>()
            }
            val (resolvedHeater, resolvedThermosiphon) = try {
                future.get(5, TimeUnit.SECONDS)
            } catch (e: TimeoutException) {
                throw AssertionError("Lookup of bound services blocked on graph lock", e)
            }
            resolvedHeater.shouldBeSameInstanceAs(heater)
            resolvedThermosiphon.shouldBeSameInstanceAs(thermosiphon)
        } finally {
            release.countDown()
            lockHolders.forEach { it.join() }
            executor.shutdownNow()
        }
    }

}
