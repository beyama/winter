package io.jentz.winter.androidx

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.testing.TestLifecycleOwner
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import io.jentz.winter.androidx.dsl.androidLifecycle
import io.jentz.winter.graph
import io.jentz.winter.qualifier
import org.junit.Test

class AndroidLifecycleTest {

    private val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)

    @Test
    fun should_call_all_lifecycle_methods() {
        val events = mutableListOf<String>()

        graph {
            constant<Lifecycle>(lifecycleOwner.lifecycle)
            singleton { "" }
                .eager()
                .androidLifecycle(
                    onCreate = { events.add("onCreate") },
                    onStart = { events.add("onStart") },
                    onResume = { events.add("onResume") },
                    onPause = { events.add("onPause") },
                    onStop = { events.add("onStop") },
                    onDestroy = { events.add("onDestroy") }
                )
        }

        assertThat(events).isEmpty()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        assertThat(events).containsExactly("onCreate")

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertThat(events).containsExactly("onCreate", "onStart")

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        assertThat(events).containsExactly("onCreate", "onStart", "onResume")

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        assertThat(events).containsExactly("onCreate", "onStart", "onResume", "onPause")

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertThat(events).containsExactly("onCreate", "onStart", "onResume", "onPause", "onStop")

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(events).containsExactly("onCreate", "onStart", "onResume", "onPause", "onStop",
            "onDestroy")
    }

    @Test
    fun should_unregister_observer_on_close() {
        val graph = graph {
            constant<Lifecycle>(lifecycleOwner.lifecycle)
            subcomponent(qualifier("sub")) {
                singleton { "" }
                    .eager()
                    .androidLifecycle()
            }
        }

        val sub = graph.createSubgraph(qualifier("sub"))
        assertThat(lifecycleOwner.observerCount).isEqualTo(1)
        sub.close()
        assertThat(lifecycleOwner.observerCount).isEqualTo(0)
    }

}