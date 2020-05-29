package io.jentz.winter.androidx.integration.test

import android.content.Context
import androidx.fragment.app.Fragment
import io.jentz.winter.Winter
import io.jentz.winter.androidx.viewmodel.injectActivitySavedStateViewModel
import io.jentz.winter.androidx.viewmodel.injectActivityViewModel
import io.jentz.winter.androidx.viewmodel.injectSavedStateViewModel
import io.jentz.winter.androidx.viewmodel.injectViewModel

class TestFragment : Fragment() {

    val viewModel: TestViewModel by injectViewModel {
        constant("Fragment Test ID")
    }

    val activityViewModel: TestViewModel by injectActivityViewModel()

    val savedStateViewModel: TestSavedStateViewModel by injectSavedStateViewModel {
        constant("Fragment Test ID")
    }

    val activitySavedStateViewModel: TestSavedStateViewModel by injectActivitySavedStateViewModel()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Winter.inject(this)
    }

}
