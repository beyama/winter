package io.jentz.winter.androidx

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import io.jentz.winter.androidx.dsl.androidLifecycle
import io.jentz.winter.androidx.dsl.androidProcessLifecycle
import io.jentz.winter.graph
import io.jentz.winter.inject.ApplicationScope
import org.junit.Test

class AndroidProcessLifecycleTest {

    private val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)

    @Test
    fun should_call_all_lifecycle_methods() {
        val events = mutableListOf<String>()

        graph {
            constant<Lifecycle>(lifecycleOwner.lifecycle, qualifier = ApplicationScope::class)
            singleton { "" }
                .eager()
                .androidProcessLifecycle(
                    onCreate = { events.add("onCreate") },
                    onStart = { events.add("onStart") },
                    onResume = { events.add("onResume") },
                    onPause = { events.add("onPause") },
                    onStop = { events.add("onStop") }
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
    }

    @Test
    fun should_unregister_observer_on_close() {
        val graph = graph {
            constant<Lifecycle>(lifecycleOwner.lifecycle, qualifier = ApplicationScope::class)
            subcomponent("sub") {
                singleton { "" }
                    .eager()
                    .androidProcessLifecycle()
            }
        }

        val sub = graph.createSubgraph("sub")
        assertThat(lifecycleOwner.observerCount).isEqualTo(1)
        sub.close()
        assertThat(lifecycleOwner.observerCount).isEqualTo(0)
    }

}