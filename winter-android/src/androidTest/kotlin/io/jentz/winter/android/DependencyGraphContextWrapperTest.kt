package io.jentz.winter.android

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.view.LayoutInflater
import io.jentz.winter.emptyGraph
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import org.junit.Test

class DependencyGraphContextWrapperTest {

    private val graph = emptyGraph()

    private val context = InstrumentationRegistry.getContext()

    private val wrapper = DependencyGraphContextWrapper(context, graph)

    @Test
    fun getSystemService_should_return_cloned_layout_inflater() {
        wrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE).let {
            it.shouldBeInstanceOf<LayoutInflater>()
            (it as LayoutInflater).context.shouldBeSameInstanceAs(wrapper)
        }
    }

    @Test
    fun getSystemService_called_with_null_should_return_null() {
        wrapper.getSystemService(null).shouldBe(null)
    }

    @Test
    fun getSystemService_called_with_graph_constant_should_return_graph() {
        wrapper.getSystemService(DependencyGraphContextWrapper.WINTER_GRAPH)
            .shouldBeSameInstanceAs(graph)
    }

}
