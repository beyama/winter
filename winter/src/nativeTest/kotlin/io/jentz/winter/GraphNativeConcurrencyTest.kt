package io.jentz.winter

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ObsoleteWorkersApi::class)
class GraphNativeConcurrencyTest {

    private val workers = List(4) { Worker.start() }

    @AfterTest
    fun afterEach() {
        workers.forEach { it.requestTermination().result }
    }

    @Test
    fun `singletons should only be created once when resolved concurrently`() {
        repeat(100) {
            val created = AtomicInt(0)
            val graph = graph {
                singleton { created.incrementAndGet(); Heater() }
                singleton { Thermosiphon(instance()) }
            }
            val heaters = workers
                .map { worker ->
                    worker.execute(TransferMode.SAFE, { graph }) { it.instance<Thermosiphon>().heater }
                }
                .map { it.result }

            heaters.forEach { it.shouldBeSameInstanceAs(heaters.first()) }
            created.value.shouldBe(1)
        }
    }

    @Test
    fun `should not deadlock when singletons with dependencies are resolved concurrently`() {
        repeat(500) {
            val graph = graph {
                singleton { Heater() }
                singleton { Thermosiphon(instance()) }
            }
            val thermosiphon = workers[0].execute(TransferMode.SAFE, { graph }) {
                it.instance<Thermosiphon>()
            }
            val heater = workers[1].execute(TransferMode.SAFE, { graph }) { it.instance<Heater>() }
            thermosiphon.result.heater.shouldBeSameInstanceAs(heater.result)
        }
    }

    @Test
    fun `subgraphs should resolve singletons of the parent graph concurrently`() {
        val parent = graph {
            singleton { Heater() }
            subcomponent(QualifierSub) {
                singleton { Thermosiphon(instance()) }
            }
        }
        repeat(100) {
            val children = List(workers.size) { parent.createSubgraph(QualifierSub) }
            val heaters = workers
                .mapIndexed { index, worker ->
                    worker.execute(TransferMode.SAFE, { children[index] }) {
                        it.instance<Thermosiphon>().heater
                    }
                }
                .map { it.result }
            heaters.forEach { it.shouldBeSameInstanceAs(parent.instance<Heater>()) }
        }
    }

}
