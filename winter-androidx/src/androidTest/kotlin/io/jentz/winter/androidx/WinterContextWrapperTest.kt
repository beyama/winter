package io.jentz.winter.androidx

import android.content.Context
import android.view.LayoutInflater
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isSameInstanceAs
import io.jentz.winter.emptyGraph
import org.junit.Test

class WinterContextWrapperTest {

    private val graph = emptyGraph()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val wrapper = WinterContextWrapper(context, graph)

    @Test
    fun getSystemService_should_return_cloned_layout_inflater() {
        wrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE).let { service ->
            assertThat(service)
                .isNotNull()
                .isInstanceOf<LayoutInflater>()
                .transform { it.context }
                .isSameInstanceAs(wrapper)
        }
    }

    @Test
    fun getSystemService_called_with_graph_constant_should_return_graph() {
        assertThat(wrapper.getSystemService(WinterContextWrapper.WINTER_GRAPH))
            .isSameInstanceAs(graph)
    }

}
