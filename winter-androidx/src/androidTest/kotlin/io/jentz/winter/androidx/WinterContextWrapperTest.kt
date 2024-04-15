package io.jentz.winter.androidx

import android.content.Context
import android.view.LayoutInflater
import androidx.test.platform.app.InstrumentationRegistry
import io.jentz.winter.emptyGraph
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import org.junit.Test

class WinterContextWrapperTest {

    private val graph = emptyGraph()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val wrapper = WinterContextWrapper(context, graph)

    @Test
    fun getSystemService_should_return_cloned_layout_inflater() {
        wrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE).let {
            it.shouldBeInstanceOf<LayoutInflater>()
            (it as LayoutInflater).context.shouldBeSameInstanceAs(wrapper)
        }
    }

    @Test
    fun getSystemService_called_with_graph_constant_should_return_graph() {
        wrapper.getSystemService(WinterContextWrapper.WINTER_GRAPH)
            .shouldBeSameInstanceAs(graph)
    }

}
