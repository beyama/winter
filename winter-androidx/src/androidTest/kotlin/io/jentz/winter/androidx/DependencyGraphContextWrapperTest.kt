package io.jentz.winter.androidx

import android.content.Context
import android.view.LayoutInflater
import androidx.test.platform.app.InstrumentationRegistry
import io.jentz.winter.emptyGraph
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import org.junit.Test

class DependencyGraphContextWrapperTest {

    private val graph = emptyGraph()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

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
