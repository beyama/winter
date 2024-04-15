package io.jentz.winter.androidx

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import io.jentz.winter.androidx.dsl.androidLifecycle
import io.jentz.winter.graph
import org.junit.Test

class AndroidLifecycleTest: LifecycleOwner {

    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle get() = registry

    @Test
    fun should_call_all_lifecycle_methods() = runOnMainSync {
        val events = mutableListOf<String>()

        graph {
            constant(lifecycle)
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
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        assertThat(events).containsExactly("onCreate")

        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertThat(events).containsExactly("onCreate", "onStart")

        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        assertThat(events).containsExactly("onCreate", "onStart", "onResume")

        registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        assertThat(events).containsExactly("onCreate", "onStart", "onResume", "onPause")

        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertThat(events).containsExactly("onCreate", "onStart", "onResume", "onPause", "onStop")

        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(events).containsExactly("onCreate", "onStart", "onResume", "onPause", "onStop",
            "onDestroy")
    }

    @Test
    fun should_unregister_observer_on_close() = runOnMainSync {
        val graph = graph {
            constant(lifecycle)
            subcomponent("sub") {
                singleton { "" }
                    .eager()
                    .androidLifecycle()
            }
        }

        val sub = graph.createSubgraph("sub")
        assertThat(registry.observerCount).isEqualTo(1)
        sub.close()
        assertThat(registry.observerCount).isEqualTo(0)
    }

}