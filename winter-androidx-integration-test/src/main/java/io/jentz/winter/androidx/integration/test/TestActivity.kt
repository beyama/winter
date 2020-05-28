package io.jentz.winter.androidx.integration.test

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import io.jentz.winter.Winter
import io.jentz.winter.androidx.viewmodel.injectViewModel
import io.jentz.winter.androidx.viewmodel.injectSavedStateViewModel

class TestActivity : FragmentActivity() {

    val viewModel: TestViewModel by injectViewModel {
        constant("Test ID")
    }

    val savedStatViewModel: TestSavedstateViewModel by injectSavedStateViewModel() {
        constant("Test ID")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Winter.inject(this)
        super.onCreate(savedInstanceState)
    }

}